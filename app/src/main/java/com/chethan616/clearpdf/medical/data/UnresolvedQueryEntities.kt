package com.chethan616.clearpdf.medical.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Isolated entity for tracking unresolved vocabulary queries (Tier 1 & Tier 2 lookup misses).
 * Completely decoupled from foreign keys to guarantee non-blocking, fail-safe edge logging.
 */
@Entity(
    tableName = "unresolved_queries",
    indices = [Index(value = ["normalizedQuery"], unique = true)]
)
data class UnresolvedQueryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val normalizedQuery: String,
    val originalSelection: String,
    val documentName: String? = null,
    val pageIndex: Int = 0,
    val hitCount: Int = 1,
    val firstSeenEpochMs: Long = System.currentTimeMillis(),
    val lastSeenEpochMs: Long = System.currentTimeMillis()
)

/**
 * Data Access Object for managing missed query telemetry, frequency aggregation,
 * and self-healing lexicon patch resolution.
 */
@Dao
interface UnresolvedQueryDao {

    @Query("SELECT * FROM unresolved_queries ORDER BY hitCount DESC, lastSeenEpochMs DESC LIMIT :limit")
    fun observeTopMissedTerms(limit: Int): Flow<List<UnresolvedQueryEntity>>

    @Query("SELECT * FROM unresolved_queries ORDER BY hitCount DESC, lastSeenEpochMs DESC")
    fun getTopMissedTerms(): List<UnresolvedQueryEntity>

    @Query("""
        SELECT * FROM unresolved_queries 
        WHERE normalizedQuery LIKE '%' || :query || '%' OR originalSelection LIKE '%' || :query || '%'
        ORDER BY hitCount DESC, lastSeenEpochMs DESC 
        LIMIT :limit
    """)
    fun searchMissedTerms(query: String, limit: Int): Flow<List<UnresolvedQueryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertNewMiss(entry: UnresolvedQueryEntity): Long

    @Query("""
        UPDATE unresolved_queries 
        SET hitCount = hitCount + 1, 
            lastSeenEpochMs = :timestamp,
            documentName = COALESCE(:docName, documentName),
            pageIndex = :page
        WHERE normalizedQuery = :query
    """)
    fun incrementMissCount(
        query: String,
        timestamp: Long,
        docName: String?,
        page: Int
    ): Int

    @Query("DELETE FROM unresolved_queries WHERE normalizedQuery = :query")
    fun deleteTerm(query: String): Int

    @Query("DELETE FROM unresolved_queries")
    fun clearAll(): Int

    @Query("SELECT COUNT(*) FROM unresolved_queries")
    fun getMissedCount(): Int

    @Query("SELECT COUNT(*) FROM unresolved_queries")
    fun observeMissedCount(): Flow<Int>
}

/**
 * Atomically records an unresolved query miss: increments existing hit counter
 * or inserts a new telemetry row.
 */
fun UnresolvedQueryDao.recordMiss(
    normalizedQuery: String,
    originalSelection: String,
    documentName: String? = null,
    pageIndex: Int = 0
) {
    val now = System.currentTimeMillis()
    val rowsUpdated = incrementMissCount(
        query = normalizedQuery,
        timestamp = now,
        docName = documentName,
        page = pageIndex
    )
    if (rowsUpdated == 0) {
        insertNewMiss(
            UnresolvedQueryEntity(
                normalizedQuery = normalizedQuery,
                originalSelection = originalSelection,
                documentName = documentName,
                pageIndex = pageIndex,
                hitCount = 1,
                firstSeenEpochMs = now,
                lastSeenEpochMs = now
            )
        )
    }
}
