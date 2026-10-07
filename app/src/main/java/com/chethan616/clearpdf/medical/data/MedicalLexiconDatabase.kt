package com.chethan616.clearpdf.medical.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
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
 * and SQLite FTS4 virtual tables for sub-8ms offline medical lookups.
 */
@Database(
    entities = [
        MedicalConceptEntity::class,
        MedicalTermEntity::class,
        MedicalDefinitionEntity::class,
        MedicalTermFtsEntity::class,
        GeneralTermEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(MedicalTypeConverters::class)
abstract class MedicalLexiconDatabase : RoomDatabase() {

    abstract fun medicalLexiconDao(): MedicalLexiconDao
    abstract fun generalVocabularyDao(): GeneralVocabularyDao

    /**
     * Ensures both Tier 1 medical lexicon and isolated general academic vocabulary
     * are populated in background storage.
     */
    suspend fun ensureSeeded() {
        try {
            if (medicalLexiconDao().getTermCount() == 0) {
                MedicalLexiconSeeder.seedDefaultLexicon(medicalLexiconDao())
            }
            GeneralVocabularySeeder.seedDefaultVocabulary(generalVocabularyDao())
        } catch (_: Exception) {
            // Non-fatal fallback
        }
    }

    companion object {
        const val DATABASE_NAME = "medical_lexicon.db"
        const val ASSET_PATH = "databases/medical_lexicon.db"

        @Volatile
        private var INSTANCE: MedicalLexiconDatabase? = null

        /**
         * Returns a thread-safe singleton instance of the medical lexicon database.
         * Gracefully falls back to dynamic in-code seeding if the pre-built asset is absent.
         */
        fun getInstance(context: Context): MedicalLexiconDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): MedicalLexiconDatabase {
            val builder = Room.databaseBuilder(
                context,
                MedicalLexiconDatabase::class.java,
                DATABASE_NAME
            ).fallbackToDestructiveMigration()

            // Check if pre-built SQLite asset is available in assets/databases/medical_lexicon.db
            val assetExists = runCatching {
                context.assets.open(ASSET_PATH).use { true }
            }.getOrDefault(false)

            if (assetExists) {
                builder.createFromAsset(ASSET_PATH)
            }

            // Fallback seed seeder for initial initialization
            builder.addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val database = getInstance(context)
                            database.ensureSeeded()
                        } catch (_: Exception) {
                            // Non-fatal logging/fallback
                        }
                    }
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val database = getInstance(context)
                            database.ensureSeeded()
                        } catch (_: Exception) {
                            // Non-fatal logging/fallback
                        }
                    }
                }
            })

            return builder.build()
        }
    }
}
