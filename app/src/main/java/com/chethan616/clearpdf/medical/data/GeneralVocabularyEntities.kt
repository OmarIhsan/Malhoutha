package com.chethan616.clearpdf.medical.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


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

