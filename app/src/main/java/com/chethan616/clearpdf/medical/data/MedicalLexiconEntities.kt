package com.chethan616.clearpdf.medical.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Root medical concept grouping multilingual terms, Latin designations, and domain classifications.
 */
@Entity(tableName = "medical_concepts")
data class MedicalConceptEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "conceptId")
    val conceptId: Long = 0,

    @ColumnInfo(name = "cui")
    val cui: String? = null,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "subspecialty")
    val subspecialty: String? = null,

    @ColumnInfo(name = "latinName")
    val latinName: String? = null
)

/**
 * Term entry representing a translation, synonym, or clinical name in a specific language (en, ar, la).
 */
@Entity(
    tableName = "medical_terms",
    foreignKeys = [
        ForeignKey(
            entity = MedicalConceptEntity::class,
            parentColumns = ["conceptId"],
            childColumns = ["conceptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conceptId"]),
        Index(value = ["termText"]),
        Index(value = ["langCode", "termText"])
    ]
)
data class MedicalTermEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "termId")
    val termId: Long = 0,

    @ColumnInfo(name = "conceptId")
    val conceptId: Long,

    @ColumnInfo(name = "langCode")
    val langCode: String,

    @ColumnInfo(name = "termText")
    val termText: String,

    @ColumnInfo(name = "isPreferred")
    val isPreferred: Boolean = true,

    @ColumnInfo(name = "source")
    val source: String = "MANUAL"
)

/**
 * Associated English and Arabic clinical definitions for a medical concept.
 */
@Entity(
    tableName = "medical_definitions",
    foreignKeys = [
        ForeignKey(
            entity = MedicalConceptEntity::class,
            parentColumns = ["conceptId"],
            childColumns = ["conceptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conceptId"])
    ]
)
data class MedicalDefinitionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "defId")
    val defId: Long = 0,

    @ColumnInfo(name = "conceptId")
    val conceptId: Long,

    @ColumnInfo(name = "definitionEn")
    val definitionEn: String? = null,

    @ColumnInfo(name = "definitionAr")
    val definitionAr: String? = null
)

/**
 * Full-Text Search (FTS4) virtual table backing fast sub-8ms prefix, compound phrase,
 * and exact search lookups using SQLite's unicode61 tokenizer.
 */
@Entity(tableName = "medical_terms_fts")
@Fts4(contentEntity = MedicalTermEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
data class MedicalTermFtsEntity(
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    val rowid: Long = 0,

    @ColumnInfo(name = "termText")
    val termText: String
)
