package com.chethan616.clearpdf.medical.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * Isolated entity for general academic and connective vocabulary (adverbs, transitions, prose terms).
 * Completely decoupled from clinical concept foreign keys to preserve Tier 1 medical pipeline determinism.
 */
@Entity(
    tableName = "general_terms",
    indices = [Index(value = ["termEn"], unique = true)]
)
data class GeneralTermEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    val termEn: String,
    val termAr: String,
    val partOfSpeech: String? = null,
    val shortDefinition: String? = null
)

/**
 * High-performance DAO for isolated general vocabulary fallback lookup with lowercase collation.
 */
@Dao
interface GeneralVocabularyDao {
    @Query("""
        SELECT * FROM general_terms 
        WHERE LOWER(termEn) = LOWER(:normalizedWord) 
        LIMIT 1
    """)
    fun findExactGeneralTerm(normalizedWord: String): GeneralTermEntity?

    @Query("""
        SELECT * FROM general_terms 
        WHERE LOWER(termEn) LIKE LOWER(:prefix) || '%' 
        LIMIT 3
    """)
    fun findPrefixMatches(prefix: String): List<GeneralTermEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTerms(terms: List<GeneralTermEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM general_terms")
    fun countTerms(): Int
}
