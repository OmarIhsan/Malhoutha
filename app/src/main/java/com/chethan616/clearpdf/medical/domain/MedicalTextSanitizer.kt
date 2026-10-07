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

    enum class SelectionType {
        /**
         * Concise term or compound noun phrase (≤ 4 tokens), dispatched to Tier 1 Lexicon.
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
        if (tokens.size <= 4) {
            // Only classify as sentence if there are multiple clauses with punctuation on both sides
            if (hasInternalSentenceBoundary(sanitizedText)) {
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
