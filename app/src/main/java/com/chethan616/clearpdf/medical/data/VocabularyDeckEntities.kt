package com.chethan616.clearpdf.medical.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Custom student-created vocabulary deck (e.g. "Endodontics 5th Year", "Oral Pathology", "Periodontics").
 */
@Entity(tableName = "vocabulary_decks")
data class VocabularyDeckEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deckName: String,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Saved medical/dental flashcard preserving bilingual translations, Latin roots,
 * clinical domain badges, source PDF coordinates, and review statistics.
 */
@Entity(
    tableName = "saved_vocabulary_cards",
    foreignKeys = [
        ForeignKey(
            entity = VocabularyDeckEntity::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deckId"]),
        Index(value = ["sourceTermEn"], unique = false)
    ]
)
data class SavedVocabularyCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val deckId: Long,
    val sourceTermEn: String,
    val targetTermAr: String,
    val latinRoot: String? = null,
    val domain: String,
    val subspecialty: String? = null,
    val definitionEn: String? = null,
    val definitionAr: String? = null,
    val sourceDocumentName: String? = null,
    val pageIndex: Int = 0,
    val lookupCount: Int = 1,
    val isBookmarked: Boolean = true,
    val lastReviewedEpochMs: Long = System.currentTimeMillis()
)

/**
 * Room Data Access Object for custom decks, vocabulary history, and spaced repetition cards.
 */
@Dao
interface VocabularyDeckDao {

    @Query("SELECT * FROM vocabulary_decks ORDER BY createdAtEpochMs DESC")
    fun observeAllDecks(): Flow<List<VocabularyDeckEntity>>

    @Query("SELECT * FROM saved_vocabulary_cards WHERE deckId = :deckId ORDER BY lastReviewedEpochMs DESC")
    fun observeCardsInDeck(deckId: Long): Flow<List<SavedVocabularyCardEntity>>

    @Query("SELECT * FROM saved_vocabulary_cards WHERE isBookmarked = 1 ORDER BY lastReviewedEpochMs DESC")
    fun observeBookmarkedCards(): Flow<List<SavedVocabularyCardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdateCard(card: SavedVocabularyCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun createDeck(deck: VocabularyDeckEntity): Long

    @Query("DELETE FROM saved_vocabulary_cards WHERE id = :cardId")
    fun deleteCard(cardId: Long): Int

    @Query("UPDATE saved_vocabulary_cards SET lookupCount = lookupCount + 1, lastReviewedEpochMs = :timestamp WHERE sourceTermEn = :term")
    fun incrementLookup(term: String, timestamp: Long): Int

    @Query("SELECT * FROM vocabulary_decks WHERE deckName = :name LIMIT 1")
    fun getDeckByName(name: String): VocabularyDeckEntity?

    @Query("SELECT * FROM vocabulary_decks WHERE id = :deckId LIMIT 1")
    fun getDeckById(deckId: Long): VocabularyDeckEntity?

    @Query("SELECT * FROM saved_vocabulary_cards WHERE deckId = :deckId AND sourceTermEn = :term LIMIT 1")
    fun findCardByTerm(deckId: Long, term: String): SavedVocabularyCardEntity?

    @Query("SELECT * FROM saved_vocabulary_cards WHERE deckId = :deckId ORDER BY lastReviewedEpochMs DESC")
    fun getCardsInDeckSync(deckId: Long): List<SavedVocabularyCardEntity>

    @Query("SELECT * FROM saved_vocabulary_cards ORDER BY lastReviewedEpochMs DESC")
    fun getAllCardsSync(): List<SavedVocabularyCardEntity>

    @Query("UPDATE saved_vocabulary_cards SET isBookmarked = :bookmarked WHERE id = :cardId")
    fun updateBookmark(cardId: Long, bookmarked: Boolean): Int

    @Query("DELETE FROM vocabulary_decks WHERE id = :deckId")
    fun deleteDeck(deckId: Long): Int
}
