package com.chethan616.clearpdf.medical.domain

/**
 * Intelligent text sanitization and routing classifier for academic PDF reading.
 * Strips OCR artifacts, resolves hyphenated line wraps, removes academic citations,
 * and routes queries between Tier 1 Lexicon (≤ 4 tokens) and Tier 2 SLM (> 4 tokens).
 */
object MedicalTextSanitizer {

    private val LINE_WRAP_HYPHEN_REGEX = Regex("""(\p{L}+)-\s*[\r\n]+\s*(\p{L}+)""")
    private val BRACKETED_CITATION_REGEX = Regex("""\[\s*\d+(?:\s*[,–-]\s*\d+)*\s*]""")
    private val PARENTHETICAL_CITATION_REGEX = Regex("""\(\s*[\p{L}\s]+et\s+al\.?,?\s*\d{4}\s*\)""", RegexOption.IGNORE_CASE)
    private val SUPERSCRIPT_NUMBERS_REGEX = Regex("""[\u00B2\u00B3\u00B9\u2070-\u2079]+""")
    private val MULTI_WHITESPACE_REGEX = Regex("""\s+""")
    private val INVISIBLE_UNICODE_REGEX = Regex("""[\u00AD\u200B-\u200D\uFEFF]""")
    private val INTERNAL_SENTENCE_BOUNDARY_REGEX = Regex("""\p{L}+[.?!]\s+\p{L}+""")
    private const val PUNCTUATION_TRIM_CHARS = ".,;:()[]{}<>\"'`~•·|—–-_،؛؟«»“”‘’"

    val LEADING_STOP_WORDS = setOf(
        "the", "a", "an", "this", "that", "these", "those",
        "its", "their", "any", "some", "each", "every"
    )

    val TRAILING_STOP_WORDS = setOf(
        "the", "a", "an", "this", "that", "these", "those",
        "its", "their", "any", "some", "each", "every",
        "and", "or", "in", "on", "at", "to", "for", "with", "by", "of",
        "is", "are", "was", "were", "be", "been", "being"
    )

    val GENERIC_STOP_WORDS = setOf(
        "the", "a", "an", "this", "that", "these", "those",
        "its", "their", "any", "some", "each", "every",
        "and", "or", "nor", "but", "so", "yet",
        "in", "on", "at", "to", "for", "with", "by", "of", "from", "as", "into", "onto",
        "is", "are", "was", "were", "be", "been", "being",
        "it", "he", "she", "they", "we", "you", "i"
    )

    enum class SelectionType {
        /**
         * Concise term or compound noun phrase (≤ 6 tokens), dispatched to Tier 1 Lexicon.
         */
        CONCISE_PHRASE,

        /**
         * Clinical sentence, multi-clause statement, or paragraph dispatched to Tier 2 SLM.
         */
        SENTENCE_OR_PASSAGE
    }

    /**
     * Sanitizes raw text selected from a PDF document or extracted via OCR.
     */
    fun sanitize(rawText: String): String {
        if (rawText.isBlank()) return ""

        var text = rawText

        // 1. Strip soft hyphens, zero-width spaces, and BOM characters
        text = INVISIBLE_UNICODE_REGEX.replace(text, "")

        // 2. Normalize non-breaking spaces and Unicode whitespace to standard space
        text = text.replace('\u00A0', ' ')
            .replace('\u202F', ' ')
            .replace('\u3000', ' ')

        // 3. Normalize smart quotes to standard quotes
        text = text.replace('“', '"')
            .replace('”', '"')
            .replace('‘', '\'')
            .replace('’', '\'')
            .replace('«', '"')
            .replace('»', '"')

        // 4. Resolve hyphenated word wrapping: "amelo-\n  blastoma" -> "ameloblastoma"
        text = LINE_WRAP_HYPHEN_REGEX.replace(text) { matchResult ->
            "${matchResult.groupValues[1]}${matchResult.groupValues[2]}"
        }

        // 5. Strip numeric bracketed citations: "[1]", "[2, 5]", "[1-4]"
        text = BRACKETED_CITATION_REGEX.replace(text, " ")

        // 6. Strip parenthetical author citations: "(Smith et al., 2020)"
        text = PARENTHETICAL_CITATION_REGEX.replace(text, " ")

        // 7. Strip unicode superscript footnote numbers: "nerve¹" -> "nerve"
        text = SUPERSCRIPT_NUMBERS_REGEX.replace(text, "")

        // 8. Normalize newlines, carriage returns, and tabs to whitespace
        text = text.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ')

        // 9. Collapse excessive whitespace
        text = MULTI_WHITESPACE_REGEX.replace(text, " ").trim()

        // 10. Strip surrounding punctuation artifacts if this is not a full sentence
        if (!isFullSentence(text)) {
            text = text.trim { it <= ' ' || it in PUNCTUATION_TRIM_CHARS }
        }

        return text
    }

    /**
     * Classifies a sanitized text string based on token length and grammatical markers.
     */
    fun classifySelection(sanitizedText: String): SelectionType {
        if (sanitizedText.isBlank()) return SelectionType.CONCISE_PHRASE

        val tokens = tokenize(sanitizedText)
        if (tokens.size <= 6) {
            // Only classify as sentence if there are multiple clauses with punctuation on both sides
            // or if it's a full sentence with terminal punctuation
            if (hasInternalSentenceBoundary(sanitizedText) || (tokens.size > 4 && containsTerminalSentencePunctuation(sanitizedText))) {
                return SelectionType.SENTENCE_OR_PASSAGE
            }
            return SelectionType.CONCISE_PHRASE
        }
        return SelectionType.SENTENCE_OR_PASSAGE
    }

    /**
     * Splits text into individual word tokens.
     */
    fun tokenize(text: String): List<String> {
        return text.trim()
            .split(MULTI_WHITESPACE_REGEX)
            .filter { it.isNotBlank() }
    }

    /**
     * Strips recognized leading determiners and trailing stop words from token boundaries
     * while preserving internal prepositions (e.g. "of" in "Striae of Retzius").
     */
    fun stripLeadingTrailingStopWords(tokens: List<String>): List<String> {
        if (tokens.isEmpty()) return emptyList()

        var start = 0
        var end = tokens.size - 1

        while (start <= end && LEADING_STOP_WORDS.contains(cleanToken(tokens[start]).lowercase())) {
            start++
        }
        while (end >= start && TRAILING_STOP_WORDS.contains(cleanToken(tokens[end]).lowercase())) {
            end--
        }

        return if (start <= end) {
            tokens.subList(start, end + 1)
        } else {
            emptyList()
        }
    }

    /**
     * Generates sliding sub-phrase N-grams from tokens in greedy descending window order.
     * Prioritizes longest window matches first (from min(tokens.size, maxN) down to minN).
     * Preserves token order and filters out solitary stop words and invalid candidates.
     */
    fun generateSlidingNgrams(tokens: List<String>, minN: Int = 1, maxN: Int = 4): List<String> {
        if (tokens.isEmpty()) return emptyList()
        val result = mutableListOf<String>()
        val effectiveMaxN = minOf(tokens.size, maxN)

        for (windowSize in effectiveMaxN downTo minN) {
            for (startIndex in 0..(tokens.size - windowSize)) {
                val windowTokens = tokens.subList(startIndex, startIndex + windowSize)
                val strippedWindow = stripLeadingTrailingStopWords(windowTokens)
                if (strippedWindow.isNotEmpty() && isValidCandidatePhrase(strippedWindow)) {
                    val strippedCandidate = strippedWindow.map { cleanToken(it) }.filter { it.isNotBlank() }.joinToString(" ")
                    if (strippedCandidate.isNotBlank()) {
                        result.add(strippedCandidate)
                    }
                }
                if (isValidCandidatePhrase(windowTokens)) {
                    val candidate = windowTokens.map { cleanToken(it) }.filter { it.isNotBlank() }.joinToString(" ")
                    if (candidate.isNotBlank()) {
                        result.add(candidate)
                    }
                }
            }
        }
        return result.distinct()
    }

    /**
     * Verifies that a candidate token list is valid for medical translation.
     * Guards against false positives: single generic words (e.g., "of", "and", "in", "the")
     * never resolve as solitary matches, and candidates must contain at least one non-stopword
     * token with length >= 3.
     */
    fun isValidCandidatePhrase(tokens: List<String>): Boolean {
        if (tokens.isEmpty()) return false
        // Solitary stop word check
        if (tokens.size == 1) {
            val clean = cleanToken(tokens.first()).lowercase()
            if (clean.length < 3 || GENERIC_STOP_WORDS.contains(clean)) {
                return false
            }
        }
        // Must contain at least one non-stopword token with length >= 3
        return tokens.any { token ->
            val clean = cleanToken(token).lowercase()
            clean.length >= 3 && !GENERIC_STOP_WORDS.contains(clean)
        }
    }

    private fun cleanToken(token: String): String {
        return token.trim { it <= ' ' || it in PUNCTUATION_TRIM_CHARS }
    }

    private fun isFullSentence(text: String): Boolean {
        val tokens = tokenize(text)
        return tokens.size > 4 && containsTerminalSentencePunctuation(text)
    }

    private fun hasInternalSentenceBoundary(text: String): Boolean {
        return INTERNAL_SENTENCE_BOUNDARY_REGEX.containsMatchIn(text)
    }

    private fun containsTerminalSentencePunctuation(text: String): Boolean {
        return text.contains('.') || text.contains('?') || text.contains('!')
    }

    /**
     * Converts a term into an SQLite FTS-safe prefix or phrase query.
     */
    fun toSafeFtsQuery(term: String): String {
        // Strip illegal SQLite FTS characters: quotes, asterisks, carets, colons
        val clean = term.replace(Regex("""["*^:]"""), "").trim()
        if (clean.isBlank()) return ""

        val tokens = tokenize(clean)
        return if (tokens.size == 1) {
            "${tokens.first()}*"
        } else {
            "\"$clean\"*"
        }
    }
}
