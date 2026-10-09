package com.chethan616.clearpdf.medical.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.chethan616.clearpdf.medical.model.MedicalDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Type converters for mapping domain enums between Kotlin types and SQLite column values.
 */
class MedicalTypeConverters {

    @TypeConverter
    fun fromDomain(domain: MedicalDomain?): String {
        return (domain ?: MedicalDomain.GENERAL_CLINICAL).name
    }

    @TypeConverter
    fun toDomain(value: String?): MedicalDomain {
        if (value.isNullOrBlank()) return MedicalDomain.GENERAL_CLINICAL
        return try {
            MedicalDomain.valueOf(value.uppercase())
        } catch (_: IllegalArgumentException) {
            MedicalDomain.GENERAL_CLINICAL
        }
    }
}

/**
 * Pre-packaged and embedded Room Database managing medical concepts, terms, definitions,
 * SQLite FTS4 virtual tables for sub-8ms offline medical lookups, and custom student vocabulary decks.
 */
@Database(
    entities = [
        MedicalConceptEntity::class,
        MedicalTermEntity::class,
        MedicalDefinitionEntity::class,
        MedicalTermFtsEntity::class,
        GeneralTermEntity::class,
        VocabularyDeckEntity::class,
        SavedVocabularyCardEntity::class,
        UnresolvedQueryEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(MedicalTypeConverters::class)
abstract class MedicalLexiconDatabase : RoomDatabase() {

    abstract fun medicalLexiconDao(): MedicalLexiconDao
    abstract fun generalVocabularyDao(): GeneralVocabularyDao
    abstract fun vocabularyDeckDao(): VocabularyDeckDao
    abstract fun unresolvedQueryDao(): UnresolvedQueryDao

    /**
     * Ensures both Tier 1 medical lexicon and isolated general academic vocabulary
     * are populated. When [.createFromAsset] is configured, Room clones the pre-populated
     * SQLite asset on initial access, making this routine a fast no-op.
     * Serves strictly as a lightweight in-memory fallback for isolated unit tests.
     */
    suspend fun ensureSeeded(forceRefresh: Boolean = false) {
        try {
            withTransaction {
                if (forceRefresh || medicalLexiconDao().getTermCount() == 0 || medicalLexiconDao().findExactMatch("biologic") == null) {
                    MedicalLexiconSeeder.seedDefaultLexicon(medicalLexiconDao(), forceRefresh = forceRefresh)
                }
                if (forceRefresh || generalVocabularyDao().countTerms() == 0 || generalVocabularyDao().findExactGeneralTerm("biologic") == null) {
                    GeneralVocabularySeeder.seedDefaultVocabulary(generalVocabularyDao(), forceRefresh = true)
                }
            }
        } catch (_: Exception) {
            // Non-fatal fallback
        }
    }

    companion object {
        const val DATABASE_NAME = "medical_lexicon.db"
        const val ASSET_PATH = "databases/medical_lexicon.db"

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createDeckTablesIfNotExist(db)
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createUnresolvedTableIfNotExist(db)
            }
        }

        private fun createDeckTablesIfNotExist(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `vocabulary_decks` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `deckName` TEXT NOT NULL,
                    `createdAtEpochMs` INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `saved_vocabulary_cards` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `deckId` INTEGER NOT NULL,
                    `sourceTermEn` TEXT NOT NULL,
                    `targetTermAr` TEXT NOT NULL,
                    `latinRoot` TEXT,
                    `domain` TEXT NOT NULL,
                    `subspecialty` TEXT,
                    `definitionEn` TEXT,
                    `definitionAr` TEXT,
                    `sourceDocumentName` TEXT,
                    `pageIndex` INTEGER NOT NULL,
                    `lookupCount` INTEGER NOT NULL,
                    `isBookmarked` INTEGER NOT NULL,
                    `lastReviewedEpochMs` INTEGER NOT NULL,
                    FOREIGN KEY(`deckId`) REFERENCES `vocabulary_decks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_vocabulary_cards_deckId` ON `saved_vocabulary_cards` (`deckId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_saved_vocabulary_cards_sourceTermEn` ON `saved_vocabulary_cards` (`sourceTermEn`)")
            db.execSQL("INSERT OR IGNORE INTO `vocabulary_decks` (`id`, `deckName`, `createdAtEpochMs`) VALUES (1, 'My Dental Lexicon', 0)")
        }

        private fun createUnresolvedTableIfNotExist(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `unresolved_queries` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `normalizedQuery` TEXT NOT NULL,
                    `originalSelection` TEXT NOT NULL,
                    `documentName` TEXT,
                    `pageIndex` INTEGER NOT NULL,
                    `hitCount` INTEGER NOT NULL,
                    `firstSeenEpochMs` INTEGER NOT NULL,
                    `lastSeenEpochMs` INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_unresolved_queries_normalizedQuery` ON `unresolved_queries` (`normalizedQuery`)")
        }

        @Volatile
        private var INSTANCE: MedicalLexiconDatabase? = null

        /**
         * Returns a thread-safe singleton instance of the medical lexicon database,
         * mounting directly from the pre-compiled SQLite asset with deck migrations.
         */
        fun getInstance(context: Context): MedicalLexiconDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): MedicalLexiconDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MedicalLexiconDatabase::class.java,
                DATABASE_NAME
            )
            .createFromAsset(ASSET_PATH)
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    createDeckTablesIfNotExist(db)
                    createUnresolvedTableIfNotExist(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            INSTANCE?.ensureSeeded()
                        } catch (_: Exception) {}
                    }
                }
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    createDeckTablesIfNotExist(db)
                    createUnresolvedTableIfNotExist(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            INSTANCE?.ensureSeeded()
                        } catch (_: Exception) {}
                    }
                }
            })
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
