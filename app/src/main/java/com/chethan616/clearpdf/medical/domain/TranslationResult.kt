package com.chethan616.clearpdf.medical.domain

import com.chethan616.clearpdf.medical.model.MedicalDomain

/**
 * Unified translation output consumed by the PDF hover tooltip, dictionary sheet,
 * and academic sticky note generator.
 */
sealed interface TranslationResult {
    val sourceText: String
    val targetArabicText: String
    val domain: MedicalDomain
    val tierUsed: TranslationTier

    /**
     * Deterministic Tier 1 match returned from the embedded SQLite/FTS lexical database.
     * Contains exact clinical terms, Latin terminology, definitions, and domain classification.
     */
    data class LexicalMatch(
        override val sourceText: String,
        override val targetArabicText: String,
        override val domain: MedicalDomain,
        val latinName: String?,
        val subspecialty: String?,
        val definitionEn: String?,
        val definitionAr: String?,
        val sourceLexicon: String,
        val synonyms: List<String> = emptyList(),
        override val tierUsed: TranslationTier = TranslationTier.TIER_1_LEXICON
    ) : TranslationResult

    /**
     * Contextual Tier 2 translation for complex sentences or paragraphs,
     * generated via an on-device Small Language Model (SLM).
     */
    data class ContextualSentence(
        override val sourceText: String,
        override val targetArabicText: String,
        override val domain: MedicalDomain,
        val clinicalNotes: String? = null,
        val highlightedEntities: List<ExtractedMedicalEntity> = emptyList(),
        override val tierUsed: TranslationTier = TranslationTier.TIER_2_ON_DEVICE_SLM
    ) : TranslationResult

    /**
     * Fallback when neither lexicon nor contextual engine can resolve a translation.
     */
    data class NotFound(
        override val sourceText: String,
        val reason: String = "No medical definition or contextual translation available."
    ) : TranslationResult {
        override val targetArabicText: String = ""
        override val domain: MedicalDomain = MedicalDomain.GENERAL_CLINICAL
        override val tierUsed: TranslationTier = TranslationTier.NONE
    }
}

/**
 * Execution tier that serviced the translation query.
 */
enum class TranslationTier {
    TIER_1_LEXICON,
    TIER_2_ON_DEVICE_SLM,
    NONE
}

/**
 * Medical entity identified and extracted within a translated sentence or paragraph.
 */
data class ExtractedMedicalEntity(
    val englishTerm: String,
    val arabicEquivalent: String,
    val domain: MedicalDomain
)
