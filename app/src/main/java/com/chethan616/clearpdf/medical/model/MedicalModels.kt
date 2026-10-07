package com.chethan616.clearpdf.medical.model

/**
 * Domain classifications for specialized clinical, medical, and dental terminology.
 */
enum class MedicalDomain {
    ANATOMY,
    PATHOLOGY,
    PHARMACOLOGY,
    PROCEDURE,
    DIAGNOSTIC,
    GENERAL_CLINICAL
}

/**
 * High-performance lexical query result model returned by Room and FTS5 lookup pipelines.
 * Designed for offline medical translation and contextual academic terminology assistance.
 */
data class LexicalQueryResult(
    val conceptId: Long,
    val englishTerm: String,
    val arabicTerm: String,
    val latinName: String?,
    val domain: MedicalDomain,
    val subspecialty: String?,
    val definitionEn: String?,
    val definitionAr: String?,
    val source: String
)
