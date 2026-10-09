package com.chethan616.clearpdf.medical.repository

import android.content.Context
import com.chethan616.clearpdf.medical.data.MedicalLexiconDatabase
import com.chethan616.clearpdf.medical.data.SavedVocabularyCardEntity
import com.chethan616.clearpdf.medical.data.VocabularyDeckDao
import com.chethan616.clearpdf.medical.data.VocabularyDeckEntity
import com.chethan616.clearpdf.medical.domain.TranslationResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository orchestrating clinical vocabulary history, student custom decks,
 * active recall flashcard bookmarks, and review telemetry.
 */
class VocabularyDeckRepository(
    private val deckDao: VocabularyDeckDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val allDecks: Flow<List<VocabularyDeckEntity>> = deckDao.observeAllDecks()
    val bookmarkedCards: Flow<List<SavedVocabularyCardEntity>> = deckDao.observeBookmarkedCards()

    fun observeCardsInDeck(deckId: Long): Flow<List<SavedVocabularyCardEntity>> =
        deckDao.observeCardsInDeck(deckId)

    /**
     * Resolves the primary default deck ("My Dental Lexicon"), creating it if it does not yet exist.
     */
    suspend fun getOrCreateDefaultDeck(deckName: String = "My Dental Lexicon"): VocabularyDeckEntity =
        withContext(ioDispatcher) {
            deckDao.getDeckByName(deckName) ?: run {
                val newDeckId = deckDao.createDeck(VocabularyDeckEntity(deckName = deckName))
                deckDao.getDeckById(newDeckId) ?: VocabularyDeckEntity(id = newDeckId, deckName = deckName)
            }
        }

    suspend fun createDeck(deckName: String): Long = withContext(ioDispatcher) {
        deckDao.createDeck(VocabularyDeckEntity(deckName = deckName.trim()))
    }

    suspend fun saveCard(card: SavedVocabularyCardEntity): Long = withContext(ioDispatcher) {
        deckDao.insertOrUpdateCard(card)
    }

    /**
     * Bookmarks a translated medical/dental result into a target deck (or the default deck),
     * automatically updating lookup count and review timestamps if already present.
     */
    suspend fun bookmarkTerm(
        result: TranslationResult,
        documentName: String? = null,
        pageIndex: Int = 0,
        deckId: Long? = null
    ): Long = withContext(ioDispatcher) {
        val targetDeck = if (deckId != null) {
            deckDao.getDeckById(deckId) ?: getOrCreateDefaultDeck()
        } else {
            getOrCreateDefaultDeck()
        }

        val sourceTermEn = when (result) {
            is TranslationResult.LexicalMatch -> result.matchedTerm ?: result.sourceText
            else -> result.sourceText
        }.trim()

        val existing = deckDao.findCardByTerm(targetDeck.id, sourceTermEn)
        if (existing != null) {
            val updated = existing.copy(
                isBookmarked = true,
                lookupCount = existing.lookupCount + 1,
                lastReviewedEpochMs = System.currentTimeMillis()
            )
            deckDao.insertOrUpdateCard(updated)
            existing.id
        } else {
            val latinRoot = (result as? TranslationResult.LexicalMatch)?.latinName
            val subspecialty = (result as? TranslationResult.LexicalMatch)?.subspecialty
            val definitionEn = (result as? TranslationResult.LexicalMatch)?.definitionEn
            val definitionAr = (result as? TranslationResult.LexicalMatch)?.definitionAr

            val card = SavedVocabularyCardEntity(
                deckId = targetDeck.id,
                sourceTermEn = sourceTermEn,
                targetTermAr = result.targetArabicText.trim(),
                latinRoot = latinRoot?.takeIf { it.isNotBlank() },
                domain = result.domain.name,
                subspecialty = subspecialty?.takeIf { it.isNotBlank() },
                definitionEn = definitionEn?.takeIf { it.isNotBlank() },
                definitionAr = definitionAr?.takeIf { it.isNotBlank() },
                sourceDocumentName = documentName,
                pageIndex = pageIndex,
                lookupCount = 1,
                isBookmarked = true,
                lastReviewedEpochMs = System.currentTimeMillis()
            )
            deckDao.insertOrUpdateCard(card)
        }
    }

    suspend fun removeCard(cardId: Long) = withContext(ioDispatcher) {
        deckDao.deleteCard(cardId)
    }

    suspend fun toggleBookmark(cardId: Long, isBookmarked: Boolean) = withContext(ioDispatcher) {
        deckDao.updateBookmark(cardId, isBookmarked)
    }

    suspend fun recordLookup(term: String, timestamp: Long = System.currentTimeMillis()) = withContext(ioDispatcher) {
        deckDao.incrementLookup(term.trim(), timestamp)
    }

    suspend fun getCardsForDeck(deckId: Long): List<SavedVocabularyCardEntity> = withContext(ioDispatcher) {
        deckDao.getCardsInDeckSync(deckId)
    }

    suspend fun getAllCards(): List<SavedVocabularyCardEntity> = withContext(ioDispatcher) {
        deckDao.getAllCardsSync()
    }

    suspend fun deleteDeck(deckId: Long) = withContext(ioDispatcher) {
        deckDao.deleteDeck(deckId)
    }

    companion object {
        @Volatile
        private var INSTANCE: VocabularyDeckRepository? = null

        fun getInstance(context: Context): VocabularyDeckRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildRepository(context: Context): VocabularyDeckRepository {
            val database = MedicalLexiconDatabase.getInstance(context)
            return VocabularyDeckRepository(database.vocabularyDeckDao())
        }
    }
}
