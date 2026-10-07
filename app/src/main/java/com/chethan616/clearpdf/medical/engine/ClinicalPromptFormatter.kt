package com.chethan616.clearpdf.medical.engine

import com.chethan616.clearpdf.medical.domain.ExtractedMedicalEntity
import com.chethan616.clearpdf.medical.model.MedicalDomain
import org.json.JSONObject
import java.util.Locale

/**
 * Structured clinical prompt engineering and deterministic JSON response parser
 * tailored for quantized on-device SLMs (e.g., Qwen2.5-1.5B-Instruct, Gemma-2-2B-IT).
 *
 * Enforces WHO Unified Medical Dictionary (UMD) Arabization conventions and extracts
 * semantic clinical entities from LLM responses.
 */
object ClinicalPromptFormatter {

    private const val SYSTEM_PROMPT = """You are an academic medical translator and lexicographer specializing in clinical, dental, and biomedical sciences.
Translate the medical text into precise academic Arabic according to WHO Unified Medical Dictionary (UMD) standards.
Maintain all anatomical landmarks, pathology classifications, and pharmaceutical names accurately.
Respond ONLY with a valid JSON object matching this schema:
{
  "translation": "<Arabic medical translation>",
  "domain": "ANATOMY|PATHOLOGY|PHARMACOLOGY|PROCEDURE|DIAGNOSTIC|GENERAL_CLINICAL",
  "entities": [
    {"en": "<english term>", "ar": "<arabic equivalent>", "domain": "<ANATOMY|PATHOLOGY|PHARMACOLOGY|PROCEDURE|DIAGNOSTIC|GENERAL_CLINICAL>"}
  ],
  "clinical_notes": "<concise clinical guidance or diagnostic note in Arabic, or null>"
}
Do not include any conversational preamble, commentary, or markdown fences outside the JSON object."""

    /**
     * Formats the prompt using ChatML tokens (<|im_start|>, <|im_end|>).
     */
    fun buildChatMlPrompt(sourceText: String, surroundingContext: String? = null): String {
        val userContent = buildString {
            if (!surroundingContext.isNullOrBlank()) {
                appendLine("Context: \"${surroundingContext.trim().take(400)}\"")
            }
            append("Text to translate: \"${sourceText.trim()}\"")
        }

        return """<|im_start|>system
$SYSTEM_PROMPT<|im_end|>
<|im_start|>user
$userContent<|im_end|>
<|im_start|>assistant
""".trimIndent()
    }

    /**
     * Formats the prompt using Gemma instruction tokens (<start_of_turn>, <end_of_turn>).
     */
    fun buildGemmaPrompt(sourceText: String, surroundingContext: String? = null): String {
        val userContent = buildString {
            appendLine(SYSTEM_PROMPT)
            appendLine()
            if (!surroundingContext.isNullOrBlank()) {
                appendLine("Context: \"${surroundingContext.trim().take(400)}\"")
            }
            append("Text to translate: \"${sourceText.trim()}\"")
        }

        return """<start_of_turn>user
$userContent<end_of_turn>
<start_of_turn>model
""".trimIndent()
    }

    /**
     * Resiliently parses the raw LLM output string into [ParsedClinicalResponse].
     * Strips potential markdown code blocks, extracts the JSON object envelope,
     * and normalizes entities and clinical domains.
     */
    fun parseResponse(rawOutput: String, fallbackSourceText: String): ParsedClinicalResponse {
        val cleaned = extractJsonPayload(rawOutput)
        if (cleaned.isBlank()) {
            return ParsedClinicalResponse(
                translationAr = rawOutput.trim(),
                domain = MedicalDomain.GENERAL_CLINICAL,
                entities = emptyList(),
                clinicalNotes = null
            )
        }

        return runCatching {
            val json = JSONObject(cleaned)
            val translation = json.optString("translation", "").trim()
                .ifEmpty { rawOutput.trim() }

            val rawDomain = json.optString("domain", "GENERAL_CLINICAL").uppercase(Locale.ROOT)
            val domain = parseDomain(rawDomain)

            val notes = json.optString("clinical_notes", "")
                .takeIf { it.isNotBlank() && it != "null" }

            val entitiesList = mutableListOf<ExtractedMedicalEntity>()
            val entitiesArray = json.optJSONArray("entities")
            if (entitiesArray != null) {
                for (i in 0 until entitiesArray.length()) {
                    val obj = entitiesArray.optJSONObject(i) ?: continue
                    val en = obj.optString("en", "").trim()
                    val ar = obj.optString("ar", "").trim()
                    val entityDomain = parseDomain(obj.optString("domain", rawDomain))
                    if (en.isNotBlank() && ar.isNotBlank()) {
                        entitiesList.add(
                            ExtractedMedicalEntity(
                                englishTerm = en,
                                arabicEquivalent = ar,
                                domain = entityDomain
                            )
                        )
                    }
                }
            }

            ParsedClinicalResponse(
                translationAr = translation,
                domain = domain,
                entities = entitiesList,
                clinicalNotes = notes
            )
        }.getOrElse {
            ParsedClinicalResponse(
                translationAr = rawOutput.trim(),
                domain = MedicalDomain.GENERAL_CLINICAL,
                entities = emptyList(),
                clinicalNotes = null
            )
        }
    }

    private fun extractJsonPayload(raw: String): String {
        var text = raw.trim()
        // Strip markdown fences ```json ... ``` or ``` ... ```
        if (text.startsWith("```")) {
            val startIdx = text.indexOf('\n')
            if (startIdx != -1) {
                text = text.substring(startIdx + 1)
            }
            if (text.endsWith("```")) {
                text = text.substring(0, text.length - 3)
            }
        }

        // Locate first '{' and last '}'
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        return if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            text.substring(firstBrace, lastBrace + 1).trim()
        } else {
            text.trim()
        }
    }

    private fun parseDomain(domainStr: String): MedicalDomain {
        return runCatching {
            MedicalDomain.valueOf(domainStr.trim().uppercase(Locale.ROOT))
        }.getOrDefault(MedicalDomain.GENERAL_CLINICAL)
    }

    data class ParsedClinicalResponse(
        val translationAr: String,
        val domain: MedicalDomain,
        val entities: List<ExtractedMedicalEntity>,
        val clinicalNotes: String?
    )
}
