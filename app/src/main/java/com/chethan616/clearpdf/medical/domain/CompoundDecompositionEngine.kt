package com.chethan616.clearpdf.medical.domain

import com.chethan616.clearpdf.medical.data.GeneralVocabularyDao
import com.chethan616.clearpdf.medical.data.MedicalLexiconDao
import com.chethan616.clearpdf.medical.model.MedicalDomain

/**
 * Intelligent, deterministic offline compound decomposition engine.
 * Breaks down unregistered 2-to-4-word clinical compounds into constituent modifiers and head nouns,
 * resolves each sub-token against Tier 1/Tier 2 clinical dictionaries, synthesizes natural Arabic
 * grammatical constructs (Idafa / الإضافة or Noun-Adjective / صفة وموصوف), and outputs a structured
 * [TranslationResult.DecomposedCompoundMatch].
 */
class CompoundDecompositionEngine(
    private val lexiconDao: MedicalLexiconDao,
    private val generalVocabularyDao: GeneralVocabularyDao? = null
) {

    private val stopWords = setOf(
        "the", "a", "an", "of", "in", "on", "at", "to", "for", "with", "by", "from", "and", "or"
    )

    // Common dental/medical adjectives and their natural Arabic relational forms
    private val adjectivalArabicForms = mapOf(
        "dentinal" to "العاجي",
        "pulpal" to "اللبي",
        "gingival" to "اللثوي",
        "apical" to "الذروي",
        "coronal" to "التاجي",
        "cervical" to "العنقي",
        "periodontal" to "الداعم",
        "alveolar" to "السنخي",
        "bacterial" to "البكتيري",
        "composite" to "الكومبوزيت",
        "adhesive" to "اللاصق",
        "enamel" to "المينائي",
        "mucosal" to "المخاطي",
        "epithelial" to "الظهاري",
        "radicular" to "الجذري",
        "occlusal" to "الإطباقي"
    )

    /**
     * Attempts to decompose an unregistered multi-token phrase.
     *
     * @param rawPhrase Raw or sanitized multi-word expression (e.g. "enamel micro-hardness").
     * @return [TranslationResult.DecomposedCompoundMatch] if at least 2 tokens (or >=60%) resolve, null otherwise.
     */
    fun decompose(rawPhrase: String): TranslationResult.DecomposedCompoundMatch? {
        val trimmed = rawPhrase.trim()
        val rawTokens = extractTokens(trimmed)

        if (rawTokens.size !in 2..4) return null

        val nonStopTokens = rawTokens.filterNot { it.lowercase() in stopWords }
        if (nonStopTokens.size < 2) return null

        val resolvedSubTokens = mutableListOf<DecomposedSubToken>()
        var resolvedCount = 0

        for (token in nonStopTokens) {
            val subToken = resolveSubToken(token)
            if (subToken != null) {
                resolvedSubTokens.add(subToken)
                resolvedCount++
            } else {
                // Keep unresolved token as placeholder without translation if majority resolves
                resolvedSubTokens.add(
                    DecomposedSubToken(
                        tokenEn = token,
                        translationAr = token,
                        domain = MedicalDomain.GENERAL_CLINICAL
                    )
                )
            }
        }

        // Require at least 2 resolved tokens AND >= 60% resolution rate
        val resolutionRate = resolvedCount.toFloat() / nonStopTokens.size
        if (resolvedCount < 2 || resolutionRate < 0.60f) {
            return null
        }

        val primaryDomain = determinePrimaryDomain(resolvedSubTokens)
        val synthesizedArabic = synthesizeArabicCompound(resolvedSubTokens)

        return TranslationResult.DecomposedCompoundMatch(
            sourceText = trimmed,
            synthesizedArabicText = synthesizedArabic,
            subTokens = resolvedSubTokens,
            primaryDomain = primaryDomain,
            tierUsed = TranslationTier.TIER_1_LEXICON
        )
    }

    /**
     * Splits phrases respecting word boundaries and hyphens.
     */
    private fun extractTokens(phrase: String): List<String> {
        return phrase
            .replace(Regex("[,;:!?]"), " ")
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    /**
     * Resolves a single constituent token through the dictionary cascade:
     * Tier 1 Medical exact -> Biomedical Morphological Candidates -> Tier 2 General Vocabulary.
     */
    private fun resolveSubToken(token: String): DecomposedSubToken? {
        val clean = token.trim().lowercase()

        // 1. Direct Tier 1 Medical Exact
        val exactMedical = lexiconDao.findExactMatch(clean) ?: lexiconDao.findExactMatch(token.trim())
        if (exactMedical != null) {
            return DecomposedSubToken(
                tokenEn = token,
                translationAr = cleanArabicTerm(exactMedical.arabicTerm),
                domain = exactMedical.domain,
                latinRoot = exactMedical.latinName,
                definitionEn = exactMedical.definitionEn,
                definitionAr = exactMedical.definitionAr
            )
        }

        // 2. Hyphen variations (e.g. "micro-hardness" -> "microhardness" or "micro hardness")
        if (clean.contains("-")) {
            val unhyphenated = clean.replace("-", "")
            val unhyphenatedMatch = lexiconDao.findExactMatch(unhyphenated)
            if (unhyphenatedMatch != null) {
                return DecomposedSubToken(
                    tokenEn = token,
                    translationAr = cleanArabicTerm(unhyphenatedMatch.arabicTerm),
                    domain = unhyphenatedMatch.domain,
                    latinRoot = unhyphenatedMatch.latinName,
                    definitionEn = unhyphenatedMatch.definitionEn,
                    definitionAr = unhyphenatedMatch.definitionAr
                )
            }
            val spaced = clean.replace("-", " ")
            val spacedMatch = lexiconDao.findExactMatch(spaced)
            if (spacedMatch != null) {
                return DecomposedSubToken(
                    tokenEn = token,
                    translationAr = cleanArabicTerm(spacedMatch.arabicTerm),
                    domain = spacedMatch.domain,
                    latinRoot = spacedMatch.latinName,
                    definitionEn = spacedMatch.definitionEn,
                    definitionAr = spacedMatch.definitionAr
                )
            }
        }

        // 3. Biomedical Morphological Candidates (Plurals, Adjectival forms)
        val morphologicalCandidates = UniversalMorphologicalNormalizer.generateBiomedicalCandidates(clean)
        for (candidate in morphologicalCandidates) {
            if (candidate.equals(clean, ignoreCase = true)) continue
            val candMatch = lexiconDao.findExactMatch(candidate)
            if (candMatch != null) {
                // Check if we have a specialized adjectival form for this word
                val arTerm = adjectivalArabicForms[clean] ?: cleanArabicTerm(candMatch.arabicTerm)
                return DecomposedSubToken(
                    tokenEn = token,
                    translationAr = arTerm,
                    domain = candMatch.domain,
                    latinRoot = candMatch.latinName,
                    definitionEn = candMatch.definitionEn,
                    definitionAr = candMatch.definitionAr
                )
            }
        }

        // 4. Tier 2 General Academic Vocabulary Fallback
        if (generalVocabularyDao != null) {
            val general = generalVocabularyDao.findExactGeneralTerm(clean)
            if (general != null) {
                return DecomposedSubToken(
                    tokenEn = token,
                    translationAr = cleanArabicTerm(general.termAr),
                    domain = MedicalDomain.GENERAL_CLINICAL,
                    latinRoot = null,
                    definitionEn = general.shortDefinition,
                    definitionAr = null
                )
            }

            for (candidate in morphologicalCandidates) {
                val candGeneral = generalVocabularyDao.findExactGeneralTerm(candidate)
                if (candGeneral != null) {
                    return DecomposedSubToken(
                        tokenEn = token,
                        translationAr = cleanArabicTerm(candGeneral.termAr),
                        domain = MedicalDomain.GENERAL_CLINICAL,
                        latinRoot = null,
                        definitionEn = candGeneral.shortDefinition,
                        definitionAr = null
                    )
                }
            }
        }

        return null
    }

    /**
     * Synthesizes Arabic grammatical structure (Idafa / الإضافة or Noun-Adjective / صفة وموصوف).
     * In English, modifier precedes head: [Modifier: Enamel] [Head: Micro-hardness].
     * In Arabic, word order inverts: [Head: الصلادة المجهرية] [Modifier: للميناء].
     */
    private fun synthesizeArabicCompound(tokens: List<DecomposedSubToken>): String {
        return when (tokens.size) {
            2 -> synthesizeTwoWordCompound(tokens[0], tokens[1])
            3 -> synthesizeThreeWordCompound(tokens[0], tokens[1], tokens[2])
            4 -> synthesizeFourWordCompound(tokens)
            else -> tokens.joinToString(" ") { it.translationAr }
        }
    }

    /**
     * Synthesizes 2-word compounds: e.g. "enamel micro-hardness", "dentinal tubule", "bone loss".
     */
    private fun synthesizeTwoWordCompound(t1: DecomposedSubToken, t2: DecomposedSubToken): String {
        val w1Lower = t1.tokenEn.lowercase()
        val w2Lower = t2.tokenEn.lowercase()

        // Check if t1 is an adjective (e.g. "dentinal", "composite", "pulpal")
        val isT1Adjective = w1Lower.endsWith("al") || w1Lower.endsWith("ic") ||
            w1Lower.endsWith("ous") || w1Lower.endsWith("ar") || w1Lower == "composite"

        val t1Ar = adjectivalArabicForms[w1Lower] ?: t1.translationAr
        val t2Ar = t2.translationAr

        return if (isT1Adjective) {
            // Noun-Adjective structure: [Head: T2] [Adjective: T1]
            // e.g. "dentinal tubule" -> "نبيب عاجي" or "الأنابيب العاجية"
            val head = stripAl(t2Ar)
            val adj = if (head.startsWith("ال")) ensureAl(t1Ar) else stripAl(t1Ar)
            "$head $adj"
        } else {
            // Idafa construct: [Head: T2] [Genitive: T1]
            // e.g. "enamel micro-hardness" -> "صلادة الميناء المجهرية" or "الصلادة المجهرية للميناء"
            val head = t2Ar.trim()
            val modifier = ensureAl(t1Ar)

            if (head.contains(" ")) {
                // Compound head like "الصلادة المجهرية" -> "الصلادة المجهرية للميناء" or "صلادة الميناء المجهرية"
                val headWords = head.split(" ")
                if (headWords.size == 2) {
                    val firstNoun = stripAl(headWords[0])
                    val secondAdj = headWords[1]
                    "$firstNoun $modifier $secondAdj"
                } else {
                    "$head لـ$modifier"
                }
            } else {
                "${stripAl(head)} $modifier"
            }
        }
    }

    /**
     * Synthesizes 3-word compounds: e.g. "dentinal wall thickness", "pulpal blood flow", "composite restoration failure".
     */
    private fun synthesizeThreeWordCompound(
        t1: DecomposedSubToken,
        t2: DecomposedSubToken,
        t3: DecomposedSubToken
    ): String {
        val w1Lower = t1.tokenEn.lowercase()
        val t1Ar = adjectivalArabicForms[w1Lower] ?: t1.translationAr
        val t2Ar = t2.translationAr
        val t3Ar = t3.translationAr

        val isT1Adjective = w1Lower.endsWith("al") || w1Lower.endsWith("ic") ||
            w1Lower.endsWith("ous") || w1Lower.endsWith("ar") || w1Lower == "composite"

        // In 3-word compounds, T3 is usually the nominal head (thickness, flow, failure, response).
        // T2 is the secondary object/tissue (wall, blood, restoration, tissue).
        // T1 is the anatomical modifier (dentinal, pulpal, composite, gingival).
        val head = stripAl(t3Ar)
        val middle = if (isT1Adjective) {
            "${stripAl(t2Ar)} $t1Ar"
        } else {
            "${stripAl(t2Ar)} ${ensureAl(t1Ar)}"
        }

        return "$head $middle"
    }

    /**
     * Synthesizes 4-word compounds by joining inverted sub-structures cleanly.
     */
    private fun synthesizeFourWordCompound(tokens: List<DecomposedSubToken>): String {
        val headPair = synthesizeTwoWordCompound(tokens[2], tokens[3])
        val modifierPair = synthesizeTwoWordCompound(tokens[0], tokens[1])
        return "$headPair لـ$modifierPair"
    }

    /**
     * Determines primary clinical domain based on clinical hierarchy.
     */
    private fun determinePrimaryDomain(tokens: List<DecomposedSubToken>): MedicalDomain {
        val domains = tokens.map { it.domain }
        return when {
            domains.contains(MedicalDomain.PATHOLOGY) -> MedicalDomain.PATHOLOGY
            domains.contains(MedicalDomain.PROCEDURE) -> MedicalDomain.PROCEDURE
            domains.contains(MedicalDomain.ANATOMY) -> MedicalDomain.ANATOMY
            domains.contains(MedicalDomain.DIAGNOSTIC) -> MedicalDomain.DIAGNOSTIC
            domains.contains(MedicalDomain.PHARMACOLOGY) -> MedicalDomain.PHARMACOLOGY
            else -> MedicalDomain.GENERAL_CLINICAL
        }
    }

    private fun cleanArabicTerm(ar: String): String {
        return ar.split("/").first().split("،").first().trim()
    }

    private fun stripAl(ar: String): String {
        val trimmed = ar.trim()
        return when {
            trimmed.startsWith("الـ") -> trimmed.removePrefix("الـ").trim()
            trimmed.startsWith("ال") -> trimmed.removePrefix("ال").trim()
            else -> trimmed
        }
    }

    private fun ensureAl(ar: String): String {
        val trimmed = ar.trim()
        return when {
            trimmed.startsWith("الـ") || trimmed.startsWith("ال") -> trimmed
            else -> "ال$trimmed"
        }
    }
}
