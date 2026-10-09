package com.chethan616.clearpdf.medical.engine

import com.chethan616.clearpdf.medical.domain.TranslationResult

/**
 * On-device Small Language Model (SLM) contextual translation contract.
 * Designed for offline execution via MediaPipe GenAI / LiteRT LLM runtimes.
 */
interface OnDeviceContextualEngine {

    /**
     * Checks if the on-device model weights are loaded and ready for execution.
     */
    suspend fun isModelAvailable(): Boolean

    /**
     * Translates a complex medical sentence or clinical paragraph into Arabic,
     * maintaining terminological precision, clinical grammar, and entity annotations.
     *
     * @param sourceText The sanitized medical sentence or passage.
     * @param surroundingContext Optional contextual text from the surrounding PDF page or paragraph.
     * @return [TranslationResult.ContextualSentence] with target Arabic, domain, notes, and entities.
     */
    suspend fun translateContextual(
        sourceText: String,
        surroundingContext: String? = null
    ): TranslationResult.ContextualSentence
}

/**
 * Architectural alias for [OnDeviceContextualEngine].
 */
typealias MedicalContextualEngine = OnDeviceContextualEngine
