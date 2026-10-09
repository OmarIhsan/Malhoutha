package com.chethan616.clearpdf.medical.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * High-performance DAO for isolated general vocabulary fallback lookup with lowercase collation.
 */
@Dao
interface GeneralVocabularyDao {
    @Query("""
        SELECT * FROM general_terms 
        WHERE LOWER(TRIM(termEn)) = LOWER(TRIM(:term)) 
        LIMIT 1
    """)
    fun findExactGeneralTerm(term: String): GeneralTermEntity?

    @Query("""
        SELECT * FROM general_terms 
        WHERE LOWER(TRIM(termEn)) IN (:terms)
    """)
    fun findBatchGeneralTerms(terms: List<String>): List<GeneralTermEntity>

    @Query("""
        SELECT * FROM general_terms 
        WHERE LOWER(termEn) LIKE LOWER(:prefix) || '%' 
        LIMIT 3
    """)
    fun findPrefixMatches(prefix: String): List<GeneralTermEntity>

    @Query("""
        SELECT * FROM general_terms 
        WHERE LOWER(termEn) LIKE '%' || LOWER(:query) || '%' 
        ORDER BY termEn ASC
        LIMIT :limit
    """)
    fun searchGeneralTerms(query: String, limit: Int): List<GeneralTermEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTerms(terms: List<GeneralTermEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTerm(term: GeneralTermEntity): Long

    @Query("SELECT COUNT(*) FROM general_terms")
    fun countTerms(): Int

    @Query("SELECT COUNT(*) FROM general_terms WHERE LOWER(termEn) = LOWER(:term)")
    fun countTerm(term: String): Int
}
