package com.chethan616.clearpdf.medical.engine

import android.content.Context
import android.util.Log
import com.chethan616.clearpdf.medical.domain.TranslationResult
import com.chethan616.clearpdf.medical.domain.TranslationTier
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
import java.util.concurrent.Executors

/**
 * Concrete on-device Small Language Model (SLM) contextual translation engine
 * executing INT4 quantized models via MediaPipe GenAI / LiteRT LLM runtime.
 *
 * Implements thread-safe reference tracking and aggressive idle memory eviction (180s)
 * to prevent Android Low Memory Killer (LMK) eviction while reading or inking.
 */
class LiteRtMedicalContextualEngine(
    private val context: Context,
    private val fallbackEngine: OnDeviceContextualEngine? = null,
    private val idleTimeoutMs: Long = 180_000L
) : OnDeviceContextualEngine {

    companion object {
        private const val TAG = "LiteRtMedicalEngine"
    }

    private val mutex = Mutex()
    private val inferenceDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "LiteRt-Medical-Inference").apply {
            priority = Thread.NORM_PRIORITY
        }
    }.asCoroutineDispatcher()

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var llmInstance: LlmInference? = null
    private var idleEvictionJob: Job? = null

    override suspend fun isModelAvailable(): Boolean {
        return ModelWeightManager.isModelAvailable(context) || fallbackEngine?.isModelAvailable() == true
    }

    override suspend fun translateContextual(
        sourceText: String,
        surroundingContext: String?
    ): TranslationResult.ContextualSentence {
        val modelFile = ModelWeightManager.getActiveModelFile(context)

        // If no model file is on disk, immediately route to fallback engine
        if (modelFile == null || !modelFile.exists()) {
            Log.d(TAG, "No local model weights found on disk; falling back to secondary engine")
            return fallbackEngine?.translateContextual(sourceText, surroundingContext)
                ?: createFallbackResponse(sourceText)
        }

        return withContext(inferenceDispatcher) {
            val inference = getOrLoadModel(modelFile.absolutePath)
            if (inference == null) {
                Log.w(TAG, "Failed to initialize LLM inference runtime; using fallback engine")
                return@withContext fallbackEngine?.translateContextual(sourceText, surroundingContext)
                    ?: createFallbackResponse(sourceText)
            }

            try {
                // Determine token format based on model name
                val prompt = if (modelFile.name.contains("gemma", ignoreCase = true)) {
                    ClinicalPromptFormatter.buildGemmaPrompt(sourceText, surroundingContext)
                } else {
                    ClinicalPromptFormatter.buildChatMlPrompt(sourceText, surroundingContext)
                }

                val rawResponse = inference.generateResponse(prompt)
                val parsed = ClinicalPromptFormatter.parseResponse(rawResponse, sourceText)

                // Schedule memory eviction after idle window
                scheduleIdleEviction()

                TranslationResult.ContextualSentence(
                    sourceText = sourceText,
                    targetArabicText = parsed.translationAr,
                    domain = parsed.domain,
                    clinicalNotes = parsed.clinicalNotes,
                    highlightedEntities = parsed.entities,
                    tierUsed = TranslationTier.TIER_2_ON_DEVICE_SLM
                )
            } catch (e: Exception) {
                Log.e(TAG, "On-device inference execution failed", e)
                fallbackEngine?.translateContextual(sourceText, surroundingContext)
                    ?: createFallbackResponse(sourceText)
            }
        }
    }

    /**
     * Unloads model weights immediately, releasing 1.2GB–1.5GB of RAM to the operating system.
     */
    suspend fun evictModelWeights(reason: String = "Manual eviction") {
        mutex.withLock {
            idleEvictionJob?.cancel()
            idleEvictionJob = null
            if (llmInstance != null) {
                Log.i(TAG, "Evicting on-device SLM weights: $reason")
                try {
                    llmInstance?.close()
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing LLM instance", e)
                } finally {
                    llmInstance = null
                    System.gc()
                }
            }
        }
    }

    private suspend fun getOrLoadModel(modelPath: String): LlmInference? {
        mutex.withLock {
            idleEvictionJob?.cancel()
            idleEvictionJob = null

            if (llmInstance != null) {
                return llmInstance
            }

            Log.i(TAG, "Loading on-device SLM weights from: $modelPath")
            return try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(512)
                    .setTemperature(0.1f)
                    .setTopK(40)
                    .build()

                val instance = LlmInference.createFromOptions(context, options)
                llmInstance = instance
                instance
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model from $modelPath", e)
                null
            }
        }
    }

    private fun scheduleIdleEviction() {
        idleEvictionJob?.cancel()
        idleEvictionJob = engineScope.launch {
            delay(idleTimeoutMs)
            evictModelWeights("Idle timeout ($idleTimeoutMs ms reached)")
        }
    }

    private fun createFallbackResponse(sourceText: String): TranslationResult.ContextualSentence {
        return TranslationResult.ContextualSentence(
            sourceText = sourceText,
            targetArabicText = sourceText,
            domain = com.chethan616.clearpdf.medical.model.MedicalDomain.GENERAL_CLINICAL,
            clinicalNotes = null,
            highlightedEntities = emptyList(),
            tierUsed = TranslationTier.NONE
        )
    }
}
