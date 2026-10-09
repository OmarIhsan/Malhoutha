package com.chethan616.clearpdf.medical.engine

import android.content.Context
import android.util.Log
import com.chethan616.clearpdf.medical.domain.ExtractedMedicalEntity
import com.chethan616.clearpdf.medical.domain.TranslationResult
import com.chethan616.clearpdf.medical.domain.TranslationTier
import com.chethan616.clearpdf.medical.model.MedicalDomain
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * Concrete on-device Small Language Model (SLM) contextual translation engine
 * executing INT4 quantized models via MediaPipe GenAI / LiteRT LLM runtime.
 *
 * Implements thread-safe reference tracking and aggressive autonomous memory eviction
 * (180s idle window) to prevent Android Low Memory Killer (LMK) eviction while reading or inking.
 */
class LiteRtMedicalContextualEngine(
    private val context: Context,
    private val weightManager: ModelWeightManager,
    private val fallbackEngine: OnDeviceContextualEngine? = null,
    private val idleTimeoutMillis: Long = 180_000L // 3 minutes
) : MedicalContextualEngine {

    /**
     * Backward-compatible convenience constructor resolving the singleton [ModelWeightManager].
     */
    constructor(
        context: Context,
        fallbackEngine: OnDeviceContextualEngine? = null,
        idleTimeoutMillis: Long = 180_000L
    ) : this(
        context = context,
        weightManager = ModelWeightManager.getInstance(context),
        fallbackEngine = fallbackEngine,
        idleTimeoutMillis = idleTimeoutMillis
    )

    companion object {
        private const val TAG = "LiteRtMedicalEngine"

        private const val SYSTEM_PROMPT =
            "You are an expert medical and dental translator. Translate the following English passage into precise, academic Arabic. Preserve medical terminology using standard Unified Medical Dictionary (UMD) standards. Output ONLY the Arabic translation without introductory chatter."
    }

    private val engineMutex = Mutex()
    private var llmInference: LlmInference? = null
    private var evictionJob: Job? = null
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val inferenceDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "LiteRt-Medical-Inference").apply {
            priority = Thread.NORM_PRIORITY
        }
    }.asCoroutineDispatcher()

    override suspend fun isModelAvailable(): Boolean {
        return weightManager.isModelAvailable() || fallbackEngine?.isModelAvailable() == true
    }

    override suspend fun translateContextual(
        sourceText: String,
        surroundingContext: String?
    ): TranslationResult.ContextualSentence {
        // If no model weights are available on disk, immediately route to fallback seeder
        if (!weightManager.isModelAvailable()) {
            Log.d(TAG, "No local model weights available; delegating to fallback contextual engine")
            return fallbackEngine?.translateContextual(sourceText, surroundingContext)
                ?: createFallbackResponse(sourceText)
        }

        return withContext(inferenceDispatcher) {
            val session = getOrInitializeInferenceSession()
            if (session == null) {
                Log.w(TAG, "Failed to initialize LiteRT LLM session; using fallback engine")
                return@withContext fallbackEngine?.translateContextual(sourceText, surroundingContext)
                    ?: createFallbackResponse(sourceText)
            }

            try {
                val prompt = formatMedicalPrompt(sourceText, surroundingContext)
                val rawResponse = session.generateResponse(prompt)

                // Clean response and extract Arabic content
                val parsed = parseModelOutput(rawResponse, sourceText)

                // Reset 180s idle eviction timer on successful inference
                resetIdleEvictionTimer()

                TranslationResult.ContextualSentence(
                    sourceText = sourceText,
                    targetArabicText = parsed.translationAr,
                    domain = parsed.domain,
                    clinicalNotes = parsed.clinicalNotes,
                    highlightedEntities = parsed.entities,
                    tierUsed = TranslationTier.TIER_2_ON_DEVICE_SLM
                )
            } catch (e: Throwable) {
                Log.e(TAG, "LiteRT on-device inference execution encountered error", e)
                fallbackEngine?.translateContextual(sourceText, surroundingContext)
                    ?: createFallbackResponse(sourceText)
            } finally {
                resetIdleEvictionTimer()
            }
        }
    }

    /**
     * Initializes or returns the cached active LiteRT inference session, resetting the eviction timer.
     */
    private suspend fun getOrInitializeInferenceSession(): LlmInference? {
        engineMutex.withLock {
            if (llmInference != null) {
                resetIdleEvictionTimerLocked()
                return llmInference
            }

            val modelPath = weightManager.getModelPath()
            val file = File(modelPath)
            if (!file.exists()) {
                Log.w(TAG, "Model weight path does not exist on storage: $modelPath")
                return null
            }

            return try {
                Log.i(TAG, "Initializing LiteRT/MediaPipe LlmInference with weights from: $modelPath")
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(512)
                    .setTopK(40)
                    .setTemperature(0.2f) // Low temperature for deterministic, factual medical translation
                    .build()

                val session = LlmInference.createFromOptions(context, options)
                llmInference = session
                resetIdleEvictionTimerLocked()
                session
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to instantiate LlmInference runtime from options", e)
                null
            }
        }
    }

    /**
     * Schedules or resets the autonomous 180-second idle memory eviction job.
     */
    private fun resetIdleEvictionTimer() {
        engineScope.launch {
            engineMutex.withLock {
                resetIdleEvictionTimerLocked()
            }
        }
    }

    private fun resetIdleEvictionTimerLocked() {
        evictionJob?.cancel()
        evictionJob = engineScope.launch {
            delay(idleTimeoutMillis)
            engineMutex.withLock {
                Log.i(TAG, "180s idle window expired. Evicting on-device SLM weights to free physical RAM.")
                try {
                    llmInference?.close()
                } catch (e: Throwable) {
                    Log.w(TAG, "Error closing LlmInference instance during idle eviction", e)
                } finally {
                    llmInference = null
                    // Explicit hint to runtime GC to reclaim native heap allocations
                    System.gc()
                }
            }
        }
    }

    /**
     * Forces immediate weight eviction and native heap release.
     */
    suspend fun evictModelWeights(reason: String = "Manual eviction") {
        engineMutex.withLock {
            evictionJob?.cancel()
            evictionJob = null
            if (llmInference != null) {
                Log.i(TAG, "Evicting on-device SLM weights: $reason")
                try {
                    llmInference?.close()
                } catch (e: Throwable) {
                    Log.w(TAG, "Error closing LlmInference during manual eviction", e)
                } finally {
                    llmInference = null
                    System.gc()
                }
            }
        }
    }

    /**
     * Formats the prompt specifically for academic medical and dental translation.
     */
    private fun formatMedicalPrompt(passage: String, contextText: String?): String {
        val modelPath = weightManager.getModelPath()
        val isGemma = modelPath.contains("gemma", ignoreCase = true)

        if (isGemma) {
            val userContent = buildString {
                appendLine(SYSTEM_PROMPT)
                if (!contextText.isNullOrBlank()) {
                    appendLine("Context: \"${contextText.trim().take(300)}\"")
                }
                append("Translate: \"${passage.trim()}\"")
            }
            return "<start_of_turn>user\n$userContent<end_of_turn>\n<start_of_turn>model\n"
        }

        // Standard ChatML format
        val userContent = buildString {
            if (!contextText.isNullOrBlank()) {
                appendLine("Context: \"${contextText.trim().take(300)}\"")
            }
            append("Translate: \"${passage.trim()}\"")
        }

        return """<|im_start|>system
$SYSTEM_PROMPT<|im_end|>
<|im_start|>user
$userContent<|im_end|>
<|im_start|>assistant
""".trimIndent()
    }

    private fun parseModelOutput(rawOutput: String, sourceText: String): ParsedResponse {
        var cleaned = rawOutput.trim()

        // Strip ChatML / instruction token tails
        val stopTokens = listOf("<|im_end|>", "<|endoftext|>", "<end_of_turn>", "</s>")
        for (token in stopTokens) {
            val idx = cleaned.indexOf(token)
            if (idx >= 0) {
                cleaned = cleaned.substring(0, idx).trim()
            }
        }

        // Check if output is wrapped in JSON payload
        val parsedJson = ClinicalPromptFormatter.parseResponse(cleaned, sourceText)
        if (parsedJson.translationAr.isNotBlank() && parsedJson.translationAr != sourceText) {
            return ParsedResponse(
                translationAr = parsedJson.translationAr,
                domain = parsedJson.domain,
                clinicalNotes = parsedJson.clinicalNotes,
                entities = parsedJson.entities
            )
        }

        return ParsedResponse(
            translationAr = cleaned.ifBlank { sourceText },
            domain = MedicalDomain.GENERAL_CLINICAL,
            clinicalNotes = null,
            entities = emptyList()
        )
    }

    private fun createFallbackResponse(sourceText: String): TranslationResult.ContextualSentence {
        return TranslationResult.ContextualSentence(
            sourceText = sourceText,
            targetArabicText = sourceText,
            domain = MedicalDomain.GENERAL_CLINICAL,
            clinicalNotes = null,
            highlightedEntities = emptyList(),
            tierUsed = TranslationTier.NONE
        )
    }

    private data class ParsedResponse(
        val translationAr: String,
        val domain: MedicalDomain,
        val clinicalNotes: String?,
        val entities: List<ExtractedMedicalEntity>
    )
}
