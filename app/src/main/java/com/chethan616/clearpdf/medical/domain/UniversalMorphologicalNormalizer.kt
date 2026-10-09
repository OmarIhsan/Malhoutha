package com.chethan616.clearpdf.medical.domain

/**
 * Universal Biomedical & Trilingual Morphological Normalizer.
 *
 * Provides deterministic stemming, lemma expansion, and affix resolution across:
 * 1. English scientific & biomedical morphology (derivational suffixes, nominalizations, participles, plurals).
 * 2. Latin & Greek anatomical declensions (nominative plural <-> nominative singular).
 * 3. Arabic orthography, diacritics (Tashkeel, Tatweel), clitics, and feminine plurals.
 *
 * Designed for offline, sub-millisecond execution within Malhoutha's Tier 1 Medical Lexicon,
 * Tier 2 General Academic Vocabulary fallback, and interactive dictionary drawer searches.
 */
object UniversalMorphologicalNormalizer {

    private val ARABIC_TASHKEEL_REGEX = Regex("""[\u064B-\u065F\u0670]""")
    private const val ARABIC_TATWEEL = '\u0640'

    // Arabic clitic prefixes ordered from longest to shortest to prevent partial matching
    private val ARABIC_CLITIC_PREFIXES = listOf(
        "وبال", "فكال", "فبال", "ولل",
        "بال", "كال", "فال", "وال", "لل",
        "ال",
        "و", "ف", "ب", "ل"
    )

    /**
     * Generates an ordered list of candidate lemmas, stems, and morphological variations
     * for a given English, Latin, or Greek biomedical term or concise compound.
     *
     * The input word in lowercase is always guaranteed to be the first candidate in the list.
     */
    fun generateBiomedicalCandidates(rawText: String): List<String> {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) return emptyList()

        val lower = trimmed.lowercase()
        val candidates = linkedSetOf(lower)

        // If the query contains multiple words (compound noun phrase), normalize head noun & modifiers
        if (lower.contains(' ')) {
            val words = lower.split(Regex("""\s+""")).filter { it.isNotBlank() }
            if (words.size in 2..6) {
                // 1. Normalize the head noun (last word in English biomedical compounds, e.g. "enamel rods" -> "enamel rod")
                val lastWord = words.last()
                val lastWordCandidates = generateSingleWordCandidates(lastWord)
                for (lastCand in lastWordCandidates) {
                    if (lastCand != lastWord) {
                        val newPhrase = (words.dropLast(1) + lastCand).joinToString(" ")
                        candidates.add(newPhrase)
                    }
                }

                // 2. Normalize the initial modifier (e.g. "hexagonal enamel rods" -> "hexagon enamel rod")
                val firstWord = words.first()
                val firstWordCandidates = generateSingleWordCandidates(firstWord)
                for (firstCand in firstWordCandidates) {
                    if (firstCand != firstWord) {
                        val newPhrase = (listOf(firstCand) + words.drop(1)).joinToString(" ")
                        candidates.add(newPhrase)
                        // Also combine with normalized head noun
                        for (lastCand in lastWordCandidates) {
                            if (lastCand != lastWord) {
                                val combinedPhrase = (listOf(firstCand) + words.subList(1, words.size - 1) + lastCand).joinToString(" ")
                                candidates.add(combinedPhrase)
                            }
                        }
                    }
                }
            }
            return candidates.toList()
        }

        // Single word candidate expansion
        candidates.addAll(generateSingleWordCandidates(lower))
        return candidates.toList()
    }

    /**
     * Internal morphological generator for a single English, Latin, or Greek token.
     */
    private fun generateSingleWordCandidates(word: String): List<String> {
        val candidates = linkedSetOf(word)

        // 1. Possessives ('s, ’s)
        if (word.endsWith("'s") || word.endsWith("’s")) {
            candidates.add(word.dropLast(2))
        }

        // 2. Latin & Greek Anatomical Declensions (Plural -> Singular)
        // -ices -> -ix, -ex (e.g., radices -> radix, apices -> apex, matrices -> matrix)
        if (word.endsWith("ices") && word.length > 4) {
            val stem = word.dropLast(4)
            candidates.add(stem + "ix")
            candidates.add(stem + "ex")
        }

        // -ina -> -en (e.g., foramina -> foramen, lumina -> lumen)
        if (word.endsWith("ina") && word.length > 4) {
            candidates.add(word.dropLast(3) + "en")
        }

        // -ae -> -a (e.g., striae -> stria, gingivae -> gingiva, cristae -> crista, papillae -> papilla)
        if (word.endsWith("ae") && word.length > 3) {
            candidates.add(word.dropLast(1))
        }

        // -i -> -us / -um (e.g., tubuli -> tubulus, alveoli -> alveolus, rami -> ramus, periostei -> periosteum)
        if (word.endsWith("i") && word.length > 3 && !word.endsWith("ii")) {
            val stem = word.dropLast(1)
            candidates.add(stem + "us")
            candidates.add(stem + "um")
        }

        // -a -> -um / -on / root (e.g., strata -> stratum, epithelia -> epithelium, septa -> septum, criteria -> criterion)
        if (word.endsWith("a") && word.length > 3) {
            val stem = word.dropLast(1)
            candidates.add(stem + "um")
            candidates.add(stem + "on")
            // Special Greek neuter plural: -mata -> -ma (e.g., granulomata -> granuloma, stromata -> stroma)
            if (word.endsWith("mata") && word.length > 5) {
                candidates.add(word.dropLast(2))
            }
        }

        // -es -> -is (e.g., canales -> canalis, axes -> axis, bases -> basis, diagnoses -> diagnosis)
        if (word.endsWith("es") && word.length > 4) {
            val stem = word.dropLast(2)
            candidates.add(stem + "is")
        }

        // 3. English Scientific Suffix Alternation (-ic <-> -ical <-> -y)
        // biological -> biologic, biology
        if (word.endsWith("ical") && word.length > 5) {
            val stem = word.dropLast(4) // e.g. biolog
            candidates.add(stem + "ic") // biologic
            candidates.add(stem + "y")  // biology
            candidates.add(word.dropLast(2)) // biologic
        } else if (word.endsWith("ic") && word.length > 4) {
            // biologic -> biological, biology
            candidates.add(word + "al") // biological
            val stem = word.dropLast(2)
            candidates.add(stem + "y")  // biology, microscopic -> microscopy
        } else if (word.endsWith("y") && word.length > 4) {
            // biology -> biologic, biological
            val stem = word.dropLast(1)
            candidates.add(stem + "ic")
            candidates.add(stem + "ical")
        }

        // 4. Geometric Shapes (-agonal <-> -agon)
        // hexagonal -> hexagon, polygonal -> polygon
        if (word.endsWith("agonal") && word.length > 6) {
            candidates.add(word.dropLast(2)) // hexagon
        } else if (word.endsWith("agon") && word.length > 5) {
            candidates.add(word + "al")      // hexagonal
        }

        // 5. Structural Adjectives (-ular -> -ule / root, -al -> -e / root)
        // tubular -> tubule, globular -> globule, radicular -> radicle
        if (word.endsWith("ular") && word.length > 5) {
            val stem = word.dropLast(4)
            candidates.add(stem + "ule")
            candidates.add(stem + "icle")
            candidates.add(stem)
        }
        // structural -> structure, procedural -> procedure, developmental -> development
        if (word.endsWith("al") && word.length > 4 && !word.endsWith("ical") && !word.endsWith("agonal")) {
            val stem = word.dropLast(2)
            candidates.add(stem)
            candidates.add(stem + "e")
        }

        // 6. Nominalizations (-tions / -tion -> -e / -er / root)
        // considerations -> consideration -> consider
        if (word.endsWith("tions") && word.length > 6) {
            candidates.add(word.dropLast(1)) // consideration
            candidates.add(word.dropLast(4)) // consider
            candidates.add(word.dropLast(5) + "e") // examine, prepare
        } else if (word.endsWith("tion") && word.length > 5) {
            candidates.add(word.dropLast(3)) // consider, alter
            candidates.add(word.dropLast(4) + "e") // prepare, examine, restore, dilate
            candidates.add(word.dropLast(4)) // form, adapt
            // -fication -> -fy (calcification -> calcify, stratification -> stratify)
            if (word.endsWith("fication") && word.length > 8) {
                candidates.add(word.dropLast(7) + "fy")
            }
        }

        // 7. State & Condition (-ments / -ment -> root)
        // attachments -> attachment -> attach
        if (word.endsWith("ments") && word.length > 6) {
            candidates.add(word.dropLast(1)) // attachment
            candidates.add(word.dropLast(5)) // attach
        } else if (word.endsWith("ment") && word.length > 5) {
            candidates.add(word.dropLast(4)) // attach, develop, treat
            candidates.add(word.dropLast(4) + "e") // enlarge, manage
        }

        // 8. Participles & Gerunds (-ing, -ed with silent-e restoration & consonant de-duplication)
        if (word.endsWith("ing") && word.length > 4) {
            val stem = word.dropLast(3)
            candidates.add(stem)               // repair, develop
            candidates.add(stem + "e")          // provide, cure, live
            if (stem.length > 2 && stem.last() == stem[stem.length - 2]) {
                candidates.add(stem.dropLast(1)) // occurring -> occur, stopping -> stop
            }
        }

        if (word.endsWith("ed") && word.length > 4) {
            // -ied -> -y (calcified -> calcify, stratified -> stratify, applied -> apply)
            if (word.endsWith("ied") && word.length > 4) {
                candidates.add(word.dropLast(3) + "y")
            }
            val stem = word.dropLast(2)
            candidates.add(stem)               // treat, stain
            candidates.add(stem + "e")          // striate, prepare, provide, enlarge
            candidates.add(word.dropLast(1))   // provide
            if (stem.length > 2 && stem.last() == stem[stem.length - 2]) {
                candidates.add(stem.dropLast(1)) // stopped -> stop, pinned -> pin
            }
        }

        // 9. Standard Plurals (-ies, -es, -s)
        if (word.endsWith("ies") && word.length > 4) {
            candidates.add(word.dropLast(3) + "y") // therapies -> therapy, cavities -> cavity
        } else if (word.endsWith("es") && word.length > 3 && !word.endsWith("ses") && !word.endsWith("tes")) {
            candidates.add(word.dropLast(2))      // processes -> process, branches -> branch
            candidates.add(word.dropLast(1))      // tubules -> tubule, capsules -> capsule
        } else if (word.endsWith("s") && word.length > 3 && !word.endsWith("ss") && !word.endsWith("us") && !word.endsWith("is") && !word.endsWith("as")) {
            candidates.add(word.dropLast(1))      // rods -> rod, cells -> cell, horns -> horn
        }

        // 10. Adverbs (-ily, -ly)
        if (word.endsWith("ily") && word.length > 4) {
            candidates.add(word.dropLast(3) + "y") // primarily -> primary
        } else if (word.endsWith("ly") && word.length > 4) {
            candidates.add(word.dropLast(2))      // markedly -> marked, broadly -> broad
            candidates.add(word.dropLast(2) + "e") // subtly -> subtle
        }

        return candidates.toList()
    }

    /**
     * Normalizes Arabic text by stripping Tashkeel diacritics, Tatweel elongation,
     * unifying Alef variants, stripping clitic prepositions/articles, and reducing feminine plurals.
     *
     * Returns an ordered list of candidate forms starting with the cleaned base form.
     */
    fun normalizeArabic(rawArabic: String): List<String> {
        val trimmed = rawArabic.trim()
        if (trimmed.isBlank()) return emptyList()

        val candidates = linkedSetOf<String>()

        // 1. Strip Tashkeel & Tatweel
        val strippedDiacritics = stripArabicDiacritics(trimmed)
        candidates.add(strippedDiacritics)

        // 2. Unify Alef variants (إ, أ, آ, ٱ -> ا)
        val unifiedAlef = unifyAlef(strippedDiacritics)
        candidates.add(unifiedAlef)

        // 3. Unify common interchangeable letter endings (ى -> ي, ة -> ه)
        val unifiedLetters = unifyArabicLetters(unifiedAlef)
        candidates.add(unifiedLetters)

        // 4. Clitic Prefix Stripping (الـ, بالـ, كالـ, فالـ, ولـ, للـ, وـ, فـ, بـ, لـ)
        val baseForms = listOf(strippedDiacritics, unifiedAlef, unifiedLetters).distinct()
        for (base in baseForms) {
            for (prefix in ARABIC_CLITIC_PREFIXES) {
                if (base.startsWith(prefix)) {
                    val minStemLength = if (prefix == "ال") 2 else 3
                    if (base.length >= prefix.length + minStemLength) {
                        val stripped = base.removePrefix(prefix)
                        candidates.add(stripped)
                        candidates.add(unifyAlef(stripped))
                        candidates.add(unifyArabicLetters(stripped))
                    }
                }
            }
        }

        // 5. Feminine Plural Reduction (ـات -> ـة / root)
        val currentSnapshot = candidates.toList()
        for (cand in currentSnapshot) {
            if (cand.endsWith("ات") && cand.length >= 4) {
                val stem = cand.dropLast(2)
                candidates.add(stem + "ة")
                candidates.add(stem + "ه")
                candidates.add(stem)
            }
        }

        return candidates.filter { it.isNotBlank() }.distinct()
    }

    /**
     * Removes all Arabic Tashkeel (harakat: Fathah, Dammah, Kasrah, Sukun, Shaddah, Tanwin)
     * and Tatweel (Kashida elongation).
     */
    fun stripArabicDiacritics(text: String): String {
        return text.replace(ARABIC_TASHKEEL_REGEX, "")
            .replace(ARABIC_TATWEEL.toString(), "")
    }

    /**
     * Unifies all forms of Alef (أ, إ, آ, ٱ) into a plain Alef (ا).
     */
    fun unifyAlef(text: String): String {
        return text.replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
    }

    /**
     * Unifies interchangeable Arabic letters (Alef Maksura ى -> ي, Taa Marbutah ة -> ه).
     */
    fun unifyArabicLetters(text: String): String {
        return text.replace('ى', 'ي')
            .replace('ة', 'ه')
    }
}
