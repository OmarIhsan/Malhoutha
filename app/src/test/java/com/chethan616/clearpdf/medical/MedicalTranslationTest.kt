package com.chethan616.clearpdf.medical

import com.chethan616.clearpdf.medical.data.GeneralTermEntity
import com.chethan616.clearpdf.medical.data.GeneralVocabularyDao
import com.chethan616.clearpdf.medical.data.GeneralVocabularySeeder
import com.chethan616.clearpdf.medical.data.MedicalConceptEntity
import com.chethan616.clearpdf.medical.data.MedicalDefinitionEntity
import com.chethan616.clearpdf.medical.data.MedicalLexiconDao
import com.chethan616.clearpdf.medical.data.MedicalTermEntity
import com.chethan616.clearpdf.medical.domain.TranslationResult
import com.chethan616.clearpdf.medical.engine.MockOnDeviceContextualEngine
import com.chethan616.clearpdf.medical.interop.MedicalStickyCardMapper
import com.chethan616.clearpdf.medical.model.LexicalQueryResult
import com.chethan616.clearpdf.medical.model.MedicalDomain
import com.chethan616.clearpdf.medical.repository.MedicalTranslationRepository
import com.malhoutha.core.ink.models.StickyCardPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MedicalTranslationTest {

    private val testDispatcher = Dispatchers.Unconfined
    private lateinit var fakeDao: FakeMedicalLexiconDao
    private lateinit var fakeGeneralDao: FakeGeneralVocabularyDao
    private lateinit var repository: MedicalTranslationRepository

    class FakeGeneralVocabularyDao : GeneralVocabularyDao {
        val generalTerms = mutableListOf<GeneralTermEntity>()
        private var idGen = 1L

        override fun findExactGeneralTerm(normalizedWord: String): GeneralTermEntity? {
            return generalTerms.firstOrNull { it.termEn.equals(normalizedWord.trim(), ignoreCase = true) }
        }

        override fun findPrefixMatches(prefix: String): List<GeneralTermEntity> {
            val clean = prefix.trim().lowercase()
            return generalTerms.filter { it.termEn.lowercase().startsWith(clean) }.take(3)
        }

        override fun insertTerms(terms: List<GeneralTermEntity>): List<Long> {
            val ids = mutableListOf<Long>()
            terms.forEach { term ->
                if (generalTerms.none { it.termEn.equals(term.termEn, ignoreCase = true) }) {
                    val id = idGen++
                    generalTerms.add(term.copy(id = id))
                    ids.add(id)
                }
            }
            return ids
        }

        override fun countTerms(): Int = generalTerms.size
    }

    class FakeMedicalLexiconDao : MedicalLexiconDao {
        val concepts = mutableListOf<MedicalConceptEntity>()
        val terms = mutableListOf<MedicalTermEntity>()
        val definitions = mutableListOf<MedicalDefinitionEntity>()
        private var conceptIdGen = 1L
        private var termIdGen = 1L

        override fun findExactMatch(normalizedText: String): LexicalQueryResult? {
            val term = terms.firstOrNull { it.langCode == "en" && it.termText.equals(normalizedText, ignoreCase = true) }
                ?: return null
            val concept = concepts.firstOrNull { it.conceptId == term.conceptId } ?: return null
            val arTerm = terms.firstOrNull { it.conceptId == concept.conceptId && it.langCode == "ar" } ?: return null
            val def = definitions.firstOrNull { it.conceptId == concept.conceptId }

            return LexicalQueryResult(
                conceptId = concept.conceptId,
                englishTerm = term.termText,
                arabicTerm = arTerm.termText,
                latinName = concept.latinName,
                domain = MedicalDomain.valueOf(concept.category),
                subspecialty = concept.subspecialty,
                definitionEn = def?.definitionEn,
                definitionAr = def?.definitionAr,
                source = term.source
            )
        }

        override fun findExactMatchArabic(normalizedArabicText: String): LexicalQueryResult? {
            val term = terms.firstOrNull { it.langCode == "ar" && it.termText.equals(normalizedArabicText, ignoreCase = true) }
                ?: return null
            val concept = concepts.firstOrNull { it.conceptId == term.conceptId } ?: return null
            val enTerm = terms.firstOrNull { it.conceptId == concept.conceptId && it.langCode == "en" } ?: return null
            val def = definitions.firstOrNull { it.conceptId == concept.conceptId }

            return LexicalQueryResult(
                conceptId = concept.conceptId,
                englishTerm = enTerm.termText,
                arabicTerm = term.termText,
                latinName = concept.latinName,
                domain = MedicalDomain.valueOf(concept.category),
                subspecialty = concept.subspecialty,
                definitionEn = def?.definitionEn,
                definitionAr = def?.definitionAr,
                source = term.source
            )
        }

        override fun searchFtsMatches(ftsQuery: String): List<LexicalQueryResult> {
            val clean = ftsQuery.replace("*", "").replace("\"", "").trim().lowercase()
            return terms.filter { it.langCode == "en" && it.termText.lowercase().contains(clean) }
                .mapNotNull { findExactMatch(it.termText) }
                .take(3)
        }

        override fun insertConcept(concept: MedicalConceptEntity): Long {
            val id = conceptIdGen++
            concepts.add(concept.copy(conceptId = id))
            return id
        }

        override fun insertConcepts(conceptsList: List<MedicalConceptEntity>): List<Long> {
            return conceptsList.map { insertConcept(it) }
        }

        override fun insertTerm(term: MedicalTermEntity): Long {
            val id = termIdGen++
            terms.add(term.copy(termId = id))
            return id
        }

        override fun insertTerms(termsList: List<MedicalTermEntity>): List<Long> {
            return termsList.map { insertTerm(it) }
        }

        override fun insertDefinition(definition: MedicalDefinitionEntity): Long {
            definitions.add(definition)
            return definition.defId
        }

        override fun insertDefinitions(definitionsList: List<MedicalDefinitionEntity>): List<Long> {
            return definitionsList.map { insertDefinition(it) }
        }

        override fun getConceptCount(): Int = concepts.size
        override fun getTermCount(): Int = terms.size
    }

    @Before
    fun setUp() {
        fakeDao = FakeMedicalLexiconDao()
        fakeGeneralDao = FakeGeneralVocabularyDao()
        val mockEngine = MockOnDeviceContextualEngine(fakeDao)
        repository = MedicalTranslationRepository(
            lexiconDao = fakeDao,
            contextualEngine = mockEngine,
            generalVocabularyDao = fakeGeneralDao,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testEnamelExactMatch() = runBlocking {
        val result = repository.translate("Enamel")
        assertTrue("Expected LexicalMatch but got: $result", result is TranslationResult.LexicalMatch)
        val match = result as TranslationResult.LexicalMatch
        assertEquals("Enamel", match.sourceText)
        assertTrue(match.targetArabicText.contains("ميناء"))
        assertEquals(MedicalDomain.ANATOMY, match.domain)
        assertNotNull(match.latinName)
    }

    @Test
    fun testPluralAmeloblastsResolvesToSingular() = runBlocking {
        val result = repository.translate("ameloblasts")
        assertTrue("Expected LexicalMatch for ameloblasts but got: $result", result is TranslationResult.LexicalMatch)
        val match = result as TranslationResult.LexicalMatch
        assertTrue(match.targetArabicText.contains("أرومة الميناء") || match.targetArabicText.contains("مصورة للميناء"))
    }

    @Test
    fun testDentinAndOdontoblastsMatch() = runBlocking {
        val dentinResult = repository.translate("Dentin")
        assertTrue(dentinResult is TranslationResult.LexicalMatch)
        val dentinMatch = dentinResult as TranslationResult.LexicalMatch
        assertTrue(dentinMatch.targetArabicText.contains("عاج"))

        val odontoblastsResult = repository.translate("odontoblasts")
        assertTrue(odontoblastsResult is TranslationResult.LexicalMatch)
        val odontoMatch = odontoblastsResult as TranslationResult.LexicalMatch
        assertTrue(odontoMatch.targetArabicText.contains("أرومة العاج") || odontoMatch.targetArabicText.contains("مصورة للعاج"))
    }

    @Test
    fun testPossessiveNasmythMembrane() = runBlocking {
        val result = repository.translate("Nasmyth's membrane")
        assertTrue(result is TranslationResult.LexicalMatch)
        val match = result as TranslationResult.LexicalMatch
        assertTrue(match.targetArabicText.contains("ناسميث"))
    }

    @Test
    fun testPrimaryEnamelCuticleMatch() = runBlocking {
        val result = repository.translate("primary enamel cuticle")
        assertTrue(result is TranslationResult.LexicalMatch)
        val match = result as TranslationResult.LexicalMatch
        assertTrue(match.targetArabicText.contains("جليدة الميناء الأولية"))
    }

    @Test
    fun testGeneralVocabularyFallbackWords() = runBlocking {
        // "adjacent", "furthermore", "underlying", "prevalent", "bilateral", "compromise"
        val testTerms = listOf(
            "adjacent" to "مجاور / ملاصق",
            "furthermore" to "علاوة على ذلك",
            "underlying" to "أساسي / باطني / كامن",
            "prevalent" to "شائع / منتشر",
            "bilateral" to "ثنائي الجانب",
            "compromise" to "يُعرض للخطر / يضر"
        )

        for ((termEn, expectedAr) in testTerms) {
            val result = repository.translate(termEn)
            assertTrue("Expected LexicalMatch for general word '$termEn' but got $result", result is TranslationResult.LexicalMatch)
            val match = result as TranslationResult.LexicalMatch
            assertEquals(termEn, match.sourceText)
            assertEquals(expectedAr, match.targetArabicText)
            assertEquals("GENERAL_ACADEMIC_VOCAB", match.sourceLexicon)
            assertEquals(MedicalDomain.GENERAL_CLINICAL, match.domain)
            assertNull("General academic terms must omit Latin roots", match.latinName)
            assertNotNull(match.subspecialty)
        }
    }

    @Test
    fun testGeneralVocabularyInflectionFallback() = runBlocking {
        val result = repository.translate("bilaterally")
        assertTrue("Expected LexicalMatch for 'bilaterally'", result is TranslationResult.LexicalMatch)
        val match = result as TranslationResult.LexicalMatch
        assertTrue(match.targetArabicText.contains("الجانبين") || match.targetArabicText.contains("جانب"))
    }

    @Test
    fun testMedicalTermTakesPrecedenceOverGeneral() = runBlocking {
        val result = repository.translate("Enamel")
        assertTrue(result is TranslationResult.LexicalMatch)
        val match = result as TranslationResult.LexicalMatch
        assertEquals(MedicalDomain.ANATOMY, match.domain)
        assertNotNull(match.latinName)
        assertEquals("Substantia adamantina", match.latinName)
        assertTrue(match.sourceLexicon != "GENERAL_ACADEMIC_VOCAB")
    }

    @Test
    fun testGeneralVocabularyStickyCardMapping() = runBlocking {
        val result = repository.translate("adjacent")
        assertTrue(result is TranslationResult.LexicalMatch)
        val card = MedicalStickyCardMapper.mapToStickyCard(result, pageIndex = 0)
        assertEquals(StickyCardPalette.PURPLE, card.colorHex)
        assertTrue("Body should contain General Academic tag", card.content.contains("General Academic"))
        assertTrue("Body should not contain Latin tag", !card.content.contains("*Latin:"))
    }

    @Test
    fun testProvideConjugationsResolve() = runBlocking {
        val conjugations = listOf("provide", "provides", "provided", "providing")
        for (word in conjugations) {
            val result = repository.translate(word)
            assertTrue("Expected LexicalMatch for '$word' but got $result", result is TranslationResult.LexicalMatch)
            val match = result as TranslationResult.LexicalMatch
            assertEquals("GENERAL_ACADEMIC_VOCAB", match.sourceLexicon)
            assertEquals("يُوفّر / يُقدّم / يُزوّد", match.targetArabicText)
        }
    }

    @Test
    fun testAfterWithPunctuationAndUnicodeSpaces() = runBlocking {
        val variations = listOf("after", "after.", "after\u00A0.")
        for (word in variations) {
            val result = repository.translate(word)
            assertTrue("Expected LexicalMatch for '$word' but got $result", result is TranslationResult.LexicalMatch)
            val match = result as TranslationResult.LexicalMatch
            assertEquals("GENERAL_ACADEMIC_VOCAB", match.sourceLexicon)
            assertEquals("بعد / عقب / في أعقاب", match.targetArabicText)
        }
    }

    @Test
    fun testRepairAndParticiplesResolve() = runBlocking {
        val forms = listOf("repair", "repaired", "repairing")
        for (word in forms) {
            val result = repository.translate(word)
            assertTrue("Expected LexicalMatch for '$word' but got $result", result is TranslationResult.LexicalMatch)
            val match = result as TranslationResult.LexicalMatch
            assertEquals("GENERAL_ACADEMIC_VOCAB", match.sourceLexicon)
            assertEquals("يرمّم / يُصلح / ترميم", match.targetArabicText)
        }
    }
}
