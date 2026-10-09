package com.chethan616.clearpdf.medical.nlp

/**
 * Algorithmic Lemmatizer and Morphological Stemmer for biomedical and academic text.
 * Deterministically strips grammatical inflections (-ing, -ed, -es, -s, -tion, -ical <-> -ic)
 * before database queries.
 */
object MorphologicalStemmer {

    /**
     * Generates an ordered set of candidate root stems for a given word token or concise phrase.
     * Guaranteed deterministic order: exact normalized token first, then common inflections.
     */
    fun extractCandidates(rawWord: String): List<String> {
        val clean = rawWord.trim().lowercase().replace(Regex("[^a-z\\-\\s]"), "")
        if (clean.isBlank()) return emptyList()

        val candidates = linkedSetOf(clean)

        // Multi-word phrase support: normalize head noun (e.g. "treatment considerations" -> "treatment consideration")
        if (clean.contains(' ')) {
            val words = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.isNotEmpty()) {
                val headNoun = words.last()
                val headCandidates = extractCandidates(headNoun)
                for (headCand in headCandidates) {
                    if (headCand != headNoun) {
                        candidates.add((words.dropLast(1) + headCand).joinToString(" "))
                    }
                }
            }
            return candidates.toList()
        }

        // Scientific / Academic Adjective Alternations
        if (clean.endsWith("ical") && clean.length > 6) {
            candidates.add(clean.dropLast(2))         // biological -> biologic
            candidates.add(clean.dropLast(4) + "y")    // biological -> biology
        } else if (clean.endsWith("ic") && clean.length > 4) {
            candidates.add(clean + "al")              // biologic -> biological
            candidates.add(clean.dropLast(2) + "y")   // biologic -> biology
        }

        // Participle & Verb Inflections
        if (clean.endsWith("ing") && clean.length > 5) {
            candidates.add(clean.dropLast(3))         // repairing -> repair
            candidates.add(clean.dropLast(3) + "e")   // degenerating -> degenerate
        }
        if (clean.endsWith("ed") && clean.length > 4) {
            candidates.add(clean.dropLast(2))         // destroyed -> destroy
            candidates.add(clean.dropLast(1))         // degenerated -> degenerate
        }

        // Plural / Third Person Suffixes
        if (clean.endsWith("ies") && clean.length > 4) {
            candidates.add(clean.dropLast(3) + "y")   // cavities -> cavity
        } else if (clean.endsWith("es") && clean.length > 4) {
            candidates.add(clean.dropLast(2))         // processes -> process
            candidates.add(clean.dropLast(1))         // provides -> provide
        } else if (clean.endsWith("s") && clean.length > 3) {
            candidates.add(clean.dropLast(1))         // considerations -> consideration
        }

        // Noun Derivatives
        if (clean.endsWith("tion") && clean.length > 6) {
            candidates.add(clean.dropLast(4) + "te")  // formation -> formate
            candidates.add(clean.dropLast(4))         // consideration -> consider
        }

        return candidates.toList()
    }
}
