package com.chethan616.clearpdf.medical.repository

import android.content.Context
import com.chethan616.clearpdf.medical.data.GeneralVocabularyDao
import com.chethan616.clearpdf.medical.data.GeneralVocabularySeeder
import com.chethan616.clearpdf.medical.data.MedicalLexiconDao
import com.chethan616.clearpdf.medical.data.MedicalLexiconDatabase
import com.chethan616.clearpdf.medical.data.recordMiss
import com.chethan616.clearpdf.medical.domain.CompoundDecompositionEngine
import com.chethan616.clearpdf.medical.domain.MedicalTextSanitizer
import com.chethan616.clearpdf.medical.domain.TranslationResult
import com.chethan616.clearpdf.medical.domain.TranslationTier
import com.chethan616.clearpdf.medical.domain.UniversalMorphologicalNormalizer
import com.chethan616.clearpdf.medical.engine.LiteRtMedicalContextualEngine
import com.chethan616.clearpdf.medical.engine.MockOnDeviceContextualEngine
import com.chethan616.clearpdf.medical.engine.ModelWeightManager
import com.chethan616.clearpdf.medical.engine.OnDeviceContextualEngine
import com.chethan616.clearpdf.medical.model.LexicalQueryResult
import com.chethan616.clearpdf.medical.model.MedicalDomain
import com.chethan616.clearpdf.medical.nlp.MorphologicalStemmer
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
    private val unresolvedQueryDao: com.chethan616.clearpdf.medical.data.UnresolvedQueryDao? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val compoundDecompositionEngine = CompoundDecompositionEngine(
        lexiconDao = lexiconDao,
        generalVocabularyDao = generalVocabularyDao
    )

    /**
     * Translates highlighted or selected text from a PDF page with microsecond dispatch.
     * Sanitizes raw text, resolves hyphenated line wraps, removes citation artifacts,
     * and routes to the optimal tier.
     *
     * @param selectedText Raw text highlighted by user or detected by OCR.
     * @param surroundingContext Contextual text from the page/paragraph for disambiguation.
     * @param documentName Optional document filename for diagnostic telemetry.
     * @param pageIndex Zero-based page index where the term was queried.
     * @return [TranslationResult] containing either a LexicalMatch, ContextualSentence, or NotFound.
     */
    suspend fun translate(
        selectedText: String,
        surroundingContext: String? = null,
        documentName: String? = null,
        pageIndex: Int = 0
    ): TranslationResult = withContext(ioDispatcher) {
        val sanitized = MedicalTextSanitizer.sanitize(selectedText)
        if (sanitized.isBlank()) {
            return@withContext TranslationResult.NotFound(
                sourceText = selectedText,
                reason = "Selection is empty or contains only whitespace/artifacts."
            )
        }

        // Ensure database has default seed data and valid clinical disambiguation
        val clinicalEntry = lexiconDao.findExactMatch("clinical")
        val biologicEntry = lexiconDao.findExactMatch("biologic")
        if (lexiconDao.getTermCount() == 0 ||
            lexiconDao.findExactMatch("Enamel") == null ||
            clinicalEntry == null ||
            clinicalEntry.domain != MedicalDomain.GENERAL_CLINICAL ||
            biologicEntry == null) {
            com.chethan616.clearpdf.medical.data.MedicalLexiconSeeder.seedDefaultLexicon(lexiconDao, forceRefresh = true)
        }
        if (generalVocabularyDao != null) {
            GeneralVocabularySeeder.seedDefaultVocabulary(generalVocabularyDao)
        }

        val classification = MedicalTextSanitizer.classifySelection(sanitized)

        when (classification) {
            MedicalTextSanitizer.SelectionType.CONCISE_PHRASE -> {
                translateConcisePhrase(sanitized, surroundingContext, documentName, pageIndex)
            }

            MedicalTextSanitizer.SelectionType.SENTENCE_OR_PASSAGE -> {
                // Tier 2: On-Device Contextual SLM Translation
                if (contextualEngine.isModelAvailable()) {
                    try {
                        val contextual = contextualEngine.translateContextual(sanitized, surroundingContext)
                        if (contextual.targetArabicText.isNotBlank() && contextual.targetArabicText != sanitized) {
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

                            TranslationResult.ContextualMatch(
                                sourceText = sanitized,
                                targetArabicText = contextual.targetArabicText,
                                domain = contextual.domain,
                                confidenceScore = 0.95f,
                                tierUsed = TranslationTier.TIER_2_CONTEXTUAL_SLM,
                                clinicalNotes = contextual.clinicalNotes,
                                highlightedEntities = enrichedEntities
                            )
                        } else {
                            // Fallback to sliding N-grams
                            degradeSentenceToSlidingNgrams(sanitized, surroundingContext, documentName, pageIndex, showModelDownloadIfNotFound = false)
                        }
                    } catch (e: Throwable) {
                        // Gracefully degrade to sliding N-grams on inference failure
                        degradeSentenceToSlidingNgrams(sanitized, surroundingContext, documentName, pageIndex, showModelDownloadIfNotFound = false)
                    }
                } else {
                    // SLM model unavailable: gracefully degrade to sliding N-grams
                    degradeSentenceToSlidingNgrams(sanitized, surroundingContext, documentName, pageIndex, showModelDownloadIfNotFound = true)
                }
            }
        }
    }

    /**
     * Gracefully degrades a sentence or passage to sliding N-grams (windows 4, 3, 2, 1)
     * resolving multi-token clinical compounds, lexical terms, and academic vocabulary.
     */
    private suspend fun degradeSentenceToSlidingNgrams(
        sanitized: String,
        surroundingContext: String? = null,
        documentName: String? = null,
        pageIndex: Int = 0,
        showModelDownloadIfNotFound: Boolean = false
    ): TranslationResult {
        // 1. Try concise phrase translation if under token limit
        val tokens = MedicalTextSanitizer.tokenize(sanitized)
        if (tokens.size in 1..6) {
            val conciseResult = translateConcisePhrase(sanitized, surroundingContext, documentName, pageIndex)
            if (conciseResult !is TranslationResult.NotFound) {
                return conciseResult
            }
        }

        // 2. Sliding multi-token N-grams across entire sentence (longest window first: 4..1)
        val ngrams = MedicalTextSanitizer.generateSlidingNgrams(tokens, minN = 1, maxN = 4)
        val candidatePool = mutableListOf<String>()
        for (ngram in ngrams) {
            candidatePool.add(ngram)
            stripCompoundTailPlural(ngram)?.let { candidatePool.add(it) }
            candidatePool.addAll(generateLemmaCandidates(ngram))
        }

        val candidateList = candidatePool.map { it.trim().lowercase() }.distinct()
        if (candidateList.isNotEmpty()) {
            val batchResults = lexiconDao.findExactMatches(candidateList)
            if (batchResults.isNotEmpty()) {
                for (candidate in candidatePool) {
                    val match = batchResults.firstOrNull {
                        it.englishTerm.trim().equals(candidate.trim(), ignoreCase = true)
                    }
                    if (match != null) {
                        return mapToLexicalMatch(sanitized, match, matchedTerm = match.englishTerm)
                    }
                }
            }

            for (candidate in candidatePool.distinct()) {
                val lexicalMatch = findLexicalEntry(candidate)
                if (lexicalMatch != null) {
                    return mapToLexicalMatch(sanitized, lexicalMatch, matchedTerm = lexicalMatch.englishTerm)
                }
            }
        }

        // 3. Dynamic compound decomposition across 2..4 token windows
        for (ngram in ngrams) {
            val ngramTokens = MedicalTextSanitizer.tokenize(ngram)
            if (ngramTokens.size in 2..4) {
                val compoundMatch = compoundDecompositionEngine.decompose(ngram)
                if (compoundMatch != null) {
                    return compoundMatch
                }
            }
        }

        // 4. Secondary academic vocabulary fallback across sliding N-grams
        if (generalVocabularyDao != null) {
            for (ngram in ngrams) {
                val generalCandidates = generateLemmaCandidates(ngram)
                for (cand in generalCandidates) {
                    try {
                        val generalMatch = generalVocabularyDao.findExactGeneralTerm(cand)
                        if (generalMatch != null) {
                            return mapGeneralTermToLexicalMatch(sanitized, generalMatch)
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // 5. If no sliding N-gram resolves and model is not yet available, prompt for model download
        if (showModelDownloadIfNotFound && !contextualEngine.isModelAvailable()) {
            return TranslationResult.ModelDownloadRequired(
                sourceText = sanitized
            )
        }

        return translateConcisePhrase(sanitized, surroundingContext, documentName, pageIndex)
    }

    private suspend fun translateConcisePhrase(
        sanitized: String,
        surroundingContext: String? = null,
        documentName: String? = null,
        pageIndex: Int = 0
    ): TranslationResult {
        val candidates = generateLemmaCandidates(sanitized)

        // PASS 1: Specialized Clinical / Dental Lexicon (Exact & Stemmed)
        for (candidate in candidates) {
            val medicalMatch = findLexicalEntry(candidate)
            if (medicalMatch != null) {
                return mapToLexicalMatch(sanitized, medicalMatch, matchedTerm = medicalMatch.englishTerm)
            }
        }

        stripCompoundTailPlural(sanitized)?.let { singularTail ->
            val match = findLexicalEntry(singularTail)
            if (match != null) {
                return mapToLexicalMatch(sanitized, match, matchedTerm = match.englishTerm)
            }
        }

        // PASS 2: General Academic Lexicon (Exact & Stemmed via DAO)
        if (generalVocabularyDao != null) {
            for (candidate in candidates) {
                try {
                    val generalMatch = generalVocabularyDao.findExactGeneralTerm(candidate)
                    if (generalMatch != null) {
                        return mapGeneralTermToLexicalMatch(sanitized, generalMatch)
                    }
                } catch (_: Exception) {}
            }
        }

        val tokens = MedicalTextSanitizer.tokenize(sanitized)

        // Multi-token selections (2..6 tokens): sliding sub-phrases (4, 3, 2, 1) & compound morphology
        var strippedTokens: List<String>? = null
        if (tokens.size in 2..6) {
            val stripped = MedicalTextSanitizer.stripLeadingTrailingStopWords(tokens)
            if (stripped.isNotEmpty() && stripped != tokens) {
                strippedTokens = stripped
                val strippedPhrase = stripped.joinToString(" ")
                val strippedMatch = findLexicalEntry(strippedPhrase)
                if (strippedMatch != null) {
                    return mapToLexicalMatch(sanitized, strippedMatch, matchedTerm = strippedMatch.englishTerm)
                }

                stripCompoundTailPlural(strippedPhrase)?.let { singularTail ->
                    val tailMatch = findLexicalEntry(singularTail)
                    if (tailMatch != null) {
                        return mapToLexicalMatch(sanitized, tailMatch, matchedTerm = tailMatch.englishTerm)
                    }
                }

                val strippedCandidates = generateLemmaCandidates(strippedPhrase)
                for (cand in strippedCandidates) {
                    val match = findLexicalEntry(cand)
                    if (match != null) {
                        return mapToLexicalMatch(sanitized, match, matchedTerm = match.englishTerm)
                    }
                }

                if (generalVocabularyDao != null) {
                    for (cand in strippedCandidates) {
                        val generalMatch = generalVocabularyDao.findExactGeneralTerm(cand)
                        if (generalMatch != null) {
                            return mapGeneralTermToLexicalMatch(sanitized, generalMatch)
                        }
                    }
                }
            }

            // Sliding sub-phrases prioritizing longest windows first (lengths 4, 3, 2, 1)
            val baseTokens = strippedTokens ?: tokens
            val ngrams = MedicalTextSanitizer.generateSlidingNgrams(baseTokens, minN = 1, maxN = 4)
            val candidatePool = mutableListOf<String>()
            for (ngram in ngrams) {
                candidatePool.add(ngram)
                stripCompoundTailPlural(ngram)?.let { candidatePool.add(it) }
                candidatePool.addAll(generateLemmaCandidates(ngram))
            }

            val candidateList = candidatePool.map { it.trim().lowercase() }.distinct()
            if (candidateList.isNotEmpty()) {
                val batchResults = lexiconDao.findExactMatches(candidateList)
                if (batchResults.isNotEmpty()) {
                    for (candidate in candidatePool) {
                        val match = batchResults.firstOrNull {
                            it.englishTerm.trim().equals(candidate.trim(), ignoreCase = true)
                        }
                        if (match != null) {
                            return mapToLexicalMatch(sanitized, match, matchedTerm = match.englishTerm)
                        }
                    }
                }
            }

            for (candidate in candidatePool.distinct()) {
                val lexicalMatch = findLexicalEntry(candidate)
                if (lexicalMatch != null) {
                    return mapToLexicalMatch(sanitized, lexicalMatch, matchedTerm = lexicalMatch.englishTerm)
                }
            }

            if (generalVocabularyDao != null) {
                for (candidate in candidatePool.distinct()) {
                    try {
                        val generalMatch = generalVocabularyDao.findExactGeneralTerm(candidate)
                        if (generalMatch != null) {
                            return mapGeneralTermToLexicalMatch(sanitized, generalMatch)
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        // PASS 3: Compound Decomposition (2..4 tokens)
        if (tokens.size in 2..4 || (strippedTokens != null && strippedTokens.size in 2..4)) {
            val candidatePhrase = if (strippedTokens != null && strippedTokens.size in 2..4) {
                strippedTokens.joinToString(" ")
            } else {
                sanitized
            }
            val compoundMatch = compoundDecompositionEngine.decompose(candidatePhrase)
                ?: compoundDecompositionEngine.decompose(sanitized)
            if (compoundMatch != null) {
                return compoundMatch
            }
        }

        // PASS 4: Contextual Engine Fallback (if on-device SLM model is active)
        val tokenCount = tokens.size
        if (tokenCount in 1..4 && contextualEngine.isModelAvailable()) {
            val contextual = contextualEngine.translateContextual(sanitized, surroundingContext)
            if (contextual.targetArabicText.isNotBlank() && !contextual.targetArabicText.startsWith("الترجمة السياقية: $sanitized")) {
                return contextual
            }
        }

        // Unresolved
        logUnresolvedQuery(
            sanitizedQuery = sanitized,
            originalText = sanitized,
            documentName = documentName,
            pageIndex = pageIndex
        )
        return TranslationResult.NotFound(
            sourceText = sanitized,
            reason = "Term not found in clinical or academic offline database."
        )
    }

    private suspend fun logUnresolvedQuery(
        sanitizedQuery: String,
        originalText: String,
        documentName: String?,
        pageIndex: Int
    ) {
        if (unresolvedQueryDao == null) return
        try {
            val normalized = sanitizedQuery.trim().lowercase()
            if (normalized.length < 2) return // Skip 1-character artifacts
            unresolvedQueryDao.recordMiss(
                normalizedQuery = normalized,
                originalSelection = originalText.trim(),
                documentName = documentName,
                pageIndex = pageIndex
            )
        } catch (_: Exception) {
            // Autonomous edge telemetry: non-blocking, fail-safe
        }
    }

    /**
     * Interactive search across clinical and general lexicons with domain filtering.
     * Used by the tablet side-drawer and dual-pane reference layout.
     */
    suspend fun searchLexicon(
        query: String,
        domain: MedicalDomain? = null,
        limit: Int = 30
    ): List<TranslationResult.LexicalMatch> = withContext(ioDispatcher) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val results = mutableListOf<TranslationResult.LexicalMatch>()

        // 1. Direct exact lookup
        val exact = findLexicalEntry(trimmed)
        if (exact != null && (domain == null || exact.domain == domain)) {
            results.add(mapToLexicalMatch(exact.englishTerm, exact))
        }

        // 1b. Morphological candidate lookup (English biomedical or Arabic clitics/tashkeel)
        val candidates = if (trimmed.any { it in '\u0600'..'\u06FF' }) {
            UniversalMorphologicalNormalizer.normalizeArabic(trimmed)
        } else {
            generateLemmaCandidates(trimmed)
        }
        for (cand in candidates) {
            if (cand.equals(trimmed, ignoreCase = true)) continue
            val match = findLexicalEntry(cand)
            if (match != null && (domain == null || match.domain == domain)) {
                if (results.none { it.matchedTerm.equals(match.englishTerm, ignoreCase = true) }) {
                    results.add(mapToLexicalMatch(match.englishTerm, match))
                }
            }
        }

        // 2. FTS virtual table compound lookup
        val ftsQueries = candidates.take(3).map { MedicalTextSanitizer.toSafeFtsQuery(it) }.filter { it.isNotBlank() }.distinct()
        for (ftsQuery in ftsQueries) {
            if (results.size >= limit) break
            val ftsList = lexiconDao.searchFtsMatchesWithLimit(ftsQuery, limit - results.size)
            for (item in ftsList) {
                if (domain != null && item.domain != domain) continue
                if (results.none { it.matchedTerm.equals(item.englishTerm, ignoreCase = true) }) {
                    results.add(mapToLexicalMatch(item.englishTerm, item))
                }
            }
        }

        // 3. Fallback to general academic vocabulary
        if ((domain == null || domain == MedicalDomain.GENERAL_CLINICAL) && results.size < limit) {
            for (cand in candidates) {
                if (results.size >= limit) break
                val generalList = generalVocabularyDao?.searchGeneralTerms(cand, limit - results.size).orEmpty()
                for (gen in generalList) {
                    if (results.none { it.matchedTerm.equals(gen.termEn, ignoreCase = true) }) {
                        results.add(mapGeneralTermToLexicalMatch(gen.termEn, gen))
                    }
                }
            }
        }

        results
    }

    /**
     * Browses terminology categorized by clinical domain.
     */
    suspend fun browseTermsByDomain(
        domain: MedicalDomain?,
        limit: Int = 30
    ): List<TranslationResult.LexicalMatch> = withContext(ioDispatcher) {
        val list = lexiconDao.getTermsByDomain(domain?.name, limit)
        list.map { mapToLexicalMatch(it.englishTerm, it) }
    }

    private fun findLexicalEntry(query: String): LexicalQueryResult? {
        val trimmed = query.trim()
        val tokens = MedicalTextSanitizer.tokenize(trimmed)
        val isSingleToken = tokens.size == 1

        // 1. Direct exact match (Priority 1)
        lexiconDao.findExactMatch(trimmed)?.let { return it }
        val lower = trimmed.lowercase()
        lexiconDao.findExactMatch(lower)?.let { return it }
        lexiconDao.findExactMatchArabic(trimmed)?.let { return it }

        // 2. Normalize possessives: "Nasmyth's" -> "Nasmyth"
        if (trimmed.endsWith("'s", ignoreCase = true) || trimmed.endsWith("’s", ignoreCase = true)) {
            val noPossessive = trimmed.dropLast(2).trim()
            lexiconDao.findExactMatch(noPossessive)?.let { return it }
            lexiconDao.findExactMatch(noPossessive.lowercase())?.let { return it }
        }

        // 3. Normalize plurals and inflections (including compound phrase tail plural stripping)
        val singularCandidates = getSingularCandidates(trimmed)
        for (candidate in singularCandidates) {
            lexiconDao.findExactMatch(candidate)?.let { return it }
            lexiconDao.findExactMatch(candidate.lowercase())?.let { return it }
        }

        stripCompoundTailPlural(trimmed)?.let { singularTail ->
            lexiconDao.findExactMatch(singularTail)?.let { return it }
            lexiconDao.findExactMatch(singularTail.lowercase())?.let { return it }
        }

        // 4. FTS phrase / prefix search
        // Multi-word phrase search vs Single-word queries:
        // A single-word query must NEVER greedily match compound multi-word phrases
        // (e.g. querying "clinical" must never match "clinical crown" or "clinical trial").
        if (!isSingleToken) {
            val ftsQuery = MedicalTextSanitizer.toSafeFtsQuery(trimmed)
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
        } else {
            // For single-token queries, only permit FTS matches where the matched English term
            // is strictly a single token (e.g. stem match), never a multi-word compound phrase!
            val ftsQuery = MedicalTextSanitizer.toSafeFtsQuery(trimmed)
            if (ftsQuery.isNotBlank()) {
                val ftsResults = lexiconDao.searchFtsMatches(ftsQuery)
                val singleTokenMatch = ftsResults.firstOrNull { result ->
                    MedicalTextSanitizer.tokenize(result.englishTerm).size == 1
                }
                if (singleTokenMatch != null) {
                    return singleTokenMatch
                }
            }
        }

        return null
    }

    /**
     * Applies morphological plural stripping specifically to the tail (head noun) of a compound phrase.
     * E.g., "enamel rods" -> "enamel rod", "dentinal tubules" -> "dentinal tubule",
     * "apical foramina" -> "apical foramen", "dental radices" -> "dental radix".
     */
    fun stripCompoundTailPlural(phrase: String): String? {
        val tokens = MedicalTextSanitizer.tokenize(phrase)
        if (tokens.size < 2) return null
        val headNoun = tokens.last()
        val singularCandidates = MorphologicalStemmer.extractCandidates(headNoun)
            .filter { !it.equals(headNoun, ignoreCase = true) }
        val singularHead = singularCandidates.firstOrNull() ?: return null
        return (tokens.dropLast(1) + singularHead).joinToString(" ")
    }

    private fun getSingularCandidates(phrase: String): List<String> {
        return generateLemmaCandidates(phrase)
    }

    fun generateLemmaCandidates(rawWord: String): List<String> {
        val candidates = linkedSetOf<String>()
        candidates.addAll(MorphologicalStemmer.extractCandidates(rawWord))
        val clean = rawWord.trim().lowercase().replace(Regex("[^a-z\\-\\s]"), "")
        if (clean.isNotBlank()) {
            candidates.addAll(UniversalMorphologicalNormalizer.generateBiomedicalCandidates(clean))
        }
        return candidates.toList()
    }

    private fun mapToLexicalMatch(
        sourceText: String,
        result: LexicalQueryResult,
        matchedTerm: String? = null
    ): TranslationResult.LexicalMatch {
        return TranslationResult.LexicalMatch(
            sourceText = sourceText,
            targetArabicText = result.arabicTerm,
            domain = result.domain,
            latinName = result.latinName,
            subspecialty = result.subspecialty,
            definitionEn = result.definitionEn,
            definitionAr = result.definitionAr,
            sourceLexicon = result.source,
            tierUsed = TranslationTier.TIER_1_LEXICON,
            matchedTerm = matchedTerm ?: result.englishTerm
        )
    }

    private fun mapGeneralTermToLexicalMatch(
        sourceText: String,
        generalMatch: com.chethan616.clearpdf.medical.data.GeneralTermEntity
    ): TranslationResult.LexicalMatch {
        return TranslationResult.LexicalMatch(
            sourceText = sourceText,
            targetArabicText = generalMatch.termAr,
            domain = MedicalDomain.GENERAL_CLINICAL,
            latinName = null,
            subspecialty = generalMatch.partOfSpeech,
            definitionEn = generalMatch.shortDefinition,
            definitionAr = null,
            sourceLexicon = "GENERAL_ACADEMIC_VOCAB",
            synonyms = emptyList(),
            tierUsed = TranslationTier.TIER_1_LEXICON,
            matchedTerm = generalMatch.termEn
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
            val unresolvedDao = database.unresolvedQueryDao()
            val fallbackEngine = MockOnDeviceContextualEngine(dao)
            val weightManager = ModelWeightManager.getInstance(context)
            val engine = LiteRtMedicalContextualEngine(
                context = context,
                weightManager = weightManager,
                fallbackEngine = fallbackEngine
            )
            return MedicalTranslationRepository(
                lexiconDao = dao,
                contextualEngine = engine,
                generalVocabularyDao = generalDao,
                unresolvedQueryDao = unresolvedDao
            )
        }
    }
}
