package com.chethan616.clearpdf.medical.repository

import android.content.Context
import com.chethan616.clearpdf.medical.data.GeneralVocabularyDao
import com.chethan616.clearpdf.medical.data.GeneralVocabularySeeder
import com.chethan616.clearpdf.medical.data.MedicalLexiconDao
import com.chethan616.clearpdf.medical.data.MedicalLexiconDatabase
import com.chethan616.clearpdf.medical.domain.MedicalTextSanitizer
import com.chethan616.clearpdf.medical.domain.TranslationResult
import com.chethan616.clearpdf.medical.domain.TranslationTier
import com.chethan616.clearpdf.medical.engine.LiteRtMedicalContextualEngine
import com.chethan616.clearpdf.medical.engine.MockOnDeviceContextualEngine
import com.chethan616.clearpdf.medical.engine.OnDeviceContextualEngine
import com.chethan616.clearpdf.medical.model.LexicalQueryResult
import com.chethan616.clearpdf.medical.model.MedicalDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Unified repository managing the multi-tier offline medical translation pipeline.
 * Coordinates deterministic Tier 1 Lexicon lookups (sub-8ms), isolated general academic
 * vocabulary fallback (<5ms), and Tier 2 on-device SLM contextual translation.
 */
class MedicalTranslationRepository(
    private val lexiconDao: MedicalLexiconDao,
    private val contextualEngine: OnDeviceContextualEngine,
    private val generalVocabularyDao: GeneralVocabularyDao? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Translates highlighted or selected text from a PDF page with microsecond dispatch.
     * Sanitizes raw text, resolves hyphenated line wraps, removes citation artifacts,
     * and routes to the optimal tier.
     *
     * @param selectedText Raw text highlighted by user or detected by OCR.
     * @param surroundingContext Contextual text from the page/paragraph for disambiguation.
     * @return [TranslationResult] containing either a LexicalMatch, ContextualSentence, or NotFound.
     */
    suspend fun translate(
        selectedText: String,
        surroundingContext: String? = null
    ): TranslationResult = withContext(ioDispatcher) {
        val sanitized = MedicalTextSanitizer.sanitize(selectedText)
        if (sanitized.isBlank()) {
            return@withContext TranslationResult.NotFound(
                sourceText = selectedText,
                reason = "Selection is empty or contains only whitespace/artifacts."
            )
        }

        // Ensure database has default seed data
        if (lexiconDao.getTermCount() == 0 || lexiconDao.findExactMatch("Enamel") == null) {
            com.chethan616.clearpdf.medical.data.MedicalLexiconSeeder.seedDefaultLexicon(lexiconDao)
        }
        if (generalVocabularyDao != null) {
            GeneralVocabularySeeder.seedDefaultVocabulary(generalVocabularyDao)
        }

        val classification = MedicalTextSanitizer.classifySelection(sanitized)

        when (classification) {
            MedicalTextSanitizer.SelectionType.CONCISE_PHRASE -> {
                // 1. PRIMARY MEDICAL PASS (Strict, Deterministic, <8ms)
                val lexicalMatch = findLexicalEntry(sanitized)
                if (lexicalMatch != null) {
                    return@withContext mapToLexicalMatch(sanitized, lexicalMatch)
                }

                // 2. TIER 2 SENTENCE / CONTEXTUAL FALLBACK (If model is available)
                val tokenCount = MedicalTextSanitizer.tokenize(sanitized).size
                if (tokenCount in 1..4 && contextualEngine.isModelAvailable()) {
                    val contextual = contextualEngine.translateContextual(sanitized, surroundingContext)
                    if (contextual.targetArabicText.isNotBlank() && !contextual.targetArabicText.startsWith("الترجمة السياقية: $sanitized")) {
                        return@withContext contextual
                    }
                }

                // 3. SECONDARY GENERAL FALLBACK (Isolated, Sub-5ms)
                if (generalVocabularyDao != null) {
                    val candidates = generateLemmaCandidates(sanitized)
                    var generalMatch: com.chethan616.clearpdf.medical.data.GeneralTermEntity? = null
                    for (candidate in candidates) {
                        val match = generalVocabularyDao.findExactGeneralTerm(candidate)
                        if (match != null) {
                            generalMatch = match
                            break
                        }
                    }
                    
                    if (generalMatch != null) {
                        return@withContext TranslationResult.LexicalMatch(
                            sourceText = sanitized,
                            targetArabicText = generalMatch.termAr,
                            domain = MedicalDomain.GENERAL_CLINICAL,
                            latinName = null,
                            subspecialty = generalMatch.partOfSpeech,
                            definitionEn = generalMatch.shortDefinition,
                            definitionAr = null,
                            sourceLexicon = "GENERAL_ACADEMIC_VOCAB",
                            synonyms = emptyList(),
                            tierUsed = TranslationTier.TIER_1_LEXICON
                        )
                    }
                }

                // 4. FINAL NOT FOUND FALLBACK
                TranslationResult.NotFound(
                    sourceText = sanitized,
                    reason = "Term not found in offline medical or general academic lexicon."
                )
            }

            MedicalTextSanitizer.SelectionType.SENTENCE_OR_PASSAGE -> {
                // Tier 2: On-Device Contextual SLM Translation
                val contextual = contextualEngine.translateContextual(sanitized, surroundingContext)

                // Enrich extracted entities with lexical database definitions
                val enrichedEntities = contextual.highlightedEntities.map { entity ->
                    val exact = lexiconDao.findExactMatch(entity.englishTerm)
                    if (exact != null) {
                        entity.copy(
                            arabicEquivalent = exact.arabicTerm,
                            domain = exact.domain
                        )
                    } else {
                        val generalCandidates = generateLemmaCandidates(entity.englishTerm)
                        var general: com.chethan616.clearpdf.medical.data.GeneralTermEntity? = null
                        for (c in generalCandidates) {
                            val match = generalVocabularyDao?.findExactGeneralTerm(c)
                            if (match != null) {
                                general = match
                                break
                            }
                        }
                        if (general != null) {
                            entity.copy(
                                arabicEquivalent = general.termAr,
                                domain = MedicalDomain.GENERAL_CLINICAL
                            )
                        } else {
                            entity
                        }
                    }
                }

                contextual.copy(highlightedEntities = enrichedEntities)
            }
        }
    }

    private fun findLexicalEntry(query: String): LexicalQueryResult? {
        // 1. Direct exact match
        lexiconDao.findExactMatch(query)?.let { return it }
        val lower = query.lowercase()
        lexiconDao.findExactMatch(lower)?.let { return it }
        lexiconDao.findExactMatchArabic(query)?.let { return it }

        // 2. Normalize possessives: "Nasmyth's" -> "Nasmyth"
        if (query.endsWith("'s", ignoreCase = true) || query.endsWith("’s", ignoreCase = true)) {
            val noPossessive = query.dropLast(2).trim()
            lexiconDao.findExactMatch(noPossessive)?.let { return it }
            lexiconDao.findExactMatch(noPossessive.lowercase())?.let { return it }
        }

        // 3. Normalize plurals and inflections
        val singularCandidates = getSingularCandidates(query)
        for (candidate in singularCandidates) {
            lexiconDao.findExactMatch(candidate)?.let { return it }
            lexiconDao.findExactMatch(candidate.lowercase())?.let { return it }
        }

        // 4. FTS phrase / prefix search
        val ftsQuery = MedicalTextSanitizer.toSafeFtsQuery(query)
        if (ftsQuery.isNotBlank()) {
            val ftsResults = lexiconDao.searchFtsMatches(ftsQuery)
            if (ftsResults.isNotEmpty()) {
                return ftsResults.first()
            }
        }

        // 5. FTS search on singular candidate if plural
        for (candidate in singularCandidates) {
            val ftsSingular = MedicalTextSanitizer.toSafeFtsQuery(candidate)
            if (ftsSingular.isNotBlank()) {
                val ftsResults = lexiconDao.searchFtsMatches(ftsSingular)
                if (ftsResults.isNotEmpty()) {
                    return ftsResults.first()
                }
            }
        }

        return null
    }

    private fun getSingularCandidates(word: String): List<String> {
        val trimmed = word.trim()
        val candidates = mutableListOf<String>()

        if (trimmed.equals("teeth", ignoreCase = true)) {
            candidates.add("tooth")
        }

        if (trimmed.length > 3) {
            if (trimmed.endsWith("ies", ignoreCase = true)) {
                candidates.add(trimmed.dropLast(3) + "y")
            }
            if (trimmed.endsWith("es", ignoreCase = true) && !trimmed.endsWith("ses", ignoreCase = true)) {
                candidates.add(trimmed.dropLast(2))
            }
            if (trimmed.endsWith("s", ignoreCase = true) && !trimmed.endsWith("ss", ignoreCase = true)) {
                candidates.add(trimmed.dropLast(1))
            }
        }
        return candidates
    }

    private fun generateLemmaCandidates(word: String): List<String> {
        val lower = word.trim().lowercase()
        val candidates = linkedSetOf(lower)

        // 1. Possessives
        if (lower.endsWith("'s") || lower.endsWith("’s")) {
            candidates.add(lower.dropLast(2))
        }

        // 2. Participle / Gerund (-ing)
        if (lower.endsWith("ing") && lower.length > 4) {
            val stem = lower.dropLast(3)
            candidates.add(stem)               // repairing -> repair
            candidates.add(stem + "e")          // providing -> provide
            if (stem.length > 2 && stem.last() == stem[stem.length - 2]) {
                candidates.add(stem.dropLast(1)) // occurring -> occur
            }
        }

        // 3. Past tense / Passive (-ed)
        if (lower.endsWith("ed") && lower.length > 3) {
            val stem = lower.dropLast(2)
            candidates.add(stem)               // repaired -> repair
            candidates.add(lower.dropLast(1))   // provided -> provide
            if (stem.length > 2 && stem.last() == stem[stem.length - 2]) {
                candidates.add(stem.dropLast(1)) // stopped -> stop
            }
        }

        // 4. Plural / 3rd person (-s, -es, -ies)
        if (lower.endsWith("ies") && lower.length > 4) {
            candidates.add(lower.dropLast(3) + "y") // therapies -> therapy
        } else if (lower.endsWith("es") && lower.length > 3) {
            candidates.add(lower.dropLast(2))      // processes -> process
            candidates.add(lower.dropLast(1))      // provides -> provide
        } else if (lower.endsWith("s") && lower.length > 2 && !lower.endsWith("ss")) {
            candidates.add(lower.dropLast(1))      // repairs -> repair
        }

        // 5. Adverbs (-ly, -ily)
        if (lower.endsWith("ily") && lower.length > 4) {
            candidates.add(lower.dropLast(3) + "y") // primarily -> primary
        } else if (lower.endsWith("ly") && lower.length > 4) {
            candidates.add(lower.dropLast(2))      // markedly -> marked
        }

        return candidates.toList()
    }

    private fun mapToLexicalMatch(sourceText: String, result: LexicalQueryResult): TranslationResult.LexicalMatch {
        return TranslationResult.LexicalMatch(
            sourceText = sourceText,
            targetArabicText = result.arabicTerm,
            domain = result.domain,
            latinName = result.latinName,
            subspecialty = result.subspecialty,
            definitionEn = result.definitionEn,
            definitionAr = result.definitionAr,
            sourceLexicon = result.source,
            tierUsed = TranslationTier.TIER_1_LEXICON
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: MedicalTranslationRepository? = null

        /**
         * Returns a thread-safe singleton instance of the [MedicalTranslationRepository].
         */
        fun getInstance(context: Context): MedicalTranslationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildRepository(context: Context): MedicalTranslationRepository {
            val database = MedicalLexiconDatabase.getInstance(context)
            val dao = database.medicalLexiconDao()
            val generalDao = database.generalVocabularyDao()
            val fallbackEngine = MockOnDeviceContextualEngine(dao)
            val engine = LiteRtMedicalContextualEngine(
                context = context,
                fallbackEngine = fallbackEngine
            )
            return MedicalTranslationRepository(
                lexiconDao = dao,
                contextualEngine = engine,
                generalVocabularyDao = generalDao
            )
        }
    }
}
