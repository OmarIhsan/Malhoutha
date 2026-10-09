package com.chethan616.clearpdf.medical.export

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.FileProvider
import com.chethan616.clearpdf.medical.data.SavedVocabularyCardEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Random
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 100% Offline Native Anki (.apkg) Package Exporter.
 *
 * Compiles saved medical/dental cards into standard Anki SQLite schemas (`collection.anki2`),
 * formats bilingual HTML cards with RTL Arabic typography, Latin binomial roots, and clinical badges,
 * and packages them into a compressed `.apkg` ZIP bundle.
 */
object AnkiPackageExporter {

    private const val MODEL_ID = 1690000000000L
    private const val DEFAULT_DECK_CONF_ID = 1L
    private val RANDOM = Random()

    /**
     * Exports a list of [SavedVocabularyCardEntity] cards into an `.apkg` file in private cache storage.
     *
     * @param context Android context for cache dir resolution.
     * @param deckName Human-readable name of the target deck.
     * @param cards The list of cards to export.
     * @return The resulting `.apkg` file.
     */
    suspend fun exportDeckToApkg(
        context: Context,
        deckName: String,
        cards: List<SavedVocabularyCardEntity>
    ): File = withContext(Dispatchers.IO) {
        val sanitizedDeckName = deckName.replace(Regex("[^a-zA-Z0-9_\\-\\s]"), "")
            .trim()
            .replace("\\s+".toRegex(), "_")
            .ifBlank { "Medical_Vocabulary_Deck" }

        val tempDbFile = File.createTempFile("collection", ".anki2", context.cacheDir)
        val finalApkgFile = context.cacheDir.resolve("$sanitizedDeckName.apkg")

        try {
            buildAnkiDatabase(tempDbFile, deckName, cards)
            packageToZip(tempDbFile, finalApkgFile)
            finalApkgFile
        } finally {
            if (tempDbFile.exists()) {
                tempDbFile.delete()
            }
        }
    }

    /**
     * Builds standard Android share intent for the generated `.apkg` archive using FileProvider.
     */
    fun createShareIntent(context: Context, apkgFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkgFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, apkgFile.nameWithoutExtension)
            putExtra(Intent.EXTRA_TEXT, "Exported vocabulary flashcards: ${apkgFile.nameWithoutExtension}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun buildAnkiDatabase(
        dbFile: File,
        deckName: String,
        cards: List<SavedVocabularyCardEntity>
    ) {
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        try {
            db.beginTransaction()

            // 1. Create standard Anki schemas
            db.execSQL("""
                CREATE TABLE col (
                    id INTEGER PRIMARY KEY,
                    crt INTEGER,
                    mod INTEGER,
                    scm INTEGER,
                    ver INTEGER,
                    dty INTEGER,
                    usn INTEGER,
                    ls INTEGER,
                    conf TEXT,
                    models TEXT,
                    decks TEXT,
                    dconf TEXT,
                    tags TEXT
                );
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE notes (
                    id INTEGER PRIMARY KEY,
                    guid TEXT,
                    mid INTEGER,
                    mod INTEGER,
                    usn INTEGER,
                    tags TEXT,
                    flds TEXT,
                    sfld TEXT,
                    csum INTEGER,
                    flags INTEGER,
                    data TEXT
                );
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE cards (
                    id INTEGER PRIMARY KEY,
                    nid INTEGER,
                    did INTEGER,
                    ord INTEGER,
                    mod INTEGER,
                    usn INTEGER,
                    type INTEGER,
                    queue INTEGER,
                    due INTEGER,
                    ivl INTEGER,
                    factor INTEGER,
                    reps INTEGER,
                    lapses INTEGER,
                    left INTEGER,
                    odue INTEGER,
                    odid INTEGER,
                    flags INTEGER,
                    data TEXT
                );
            """.trimIndent())

            db.execSQL("CREATE INDEX ix_notes_usn ON notes (usn);")
            db.execSQL("CREATE INDEX ix_cards_usn ON cards (usn);")
            db.execSQL("CREATE INDEX ix_cards_nid ON cards (nid);")
            db.execSQL("CREATE INDEX ix_cards_sched ON cards (did, queue, due);")

            val nowMs = System.currentTimeMillis()
            val nowSec = nowMs / 1000
            val targetDeckId = (nowMs % 1_000_000_000L) + 100_000L

            // 2. Build Models JSON
            val modelsJson = JSONObject().apply {
                put(MODEL_ID.toString(), JSONObject().apply {
                    put("id", MODEL_ID)
                    put("name", "Medical and Dental Standard")
                    put("type", 0)
                    put("mod", nowSec)
                    put("usn", -1)
                    put("sortf", 0)
                    put("did", targetDeckId)
                    put("tmpls", JSONArray().apply {
                        put(JSONObject().apply {
                            put("name", "Card 1")
                            put("ord", 0)
                            put("qfmt", "{{Front}}")
                            put("afmt", "{{FrontSide}}\n\n<hr id=answer>\n\n{{Back}}")
                            put("bqfmt", "")
                            put("bafmt", "")
                            put("did", JSONObject.NULL)
                        })
                    })
                    put("flds", JSONArray().apply {
                        put(JSONObject().apply {
                            put("name", "Front")
                            put("ord", 0)
                            put("sticky", false)
                            put("rtl", false)
                            put("font", "Arial")
                            put("size", 20)
                        })
                        put(JSONObject().apply {
                            put("name", "Back")
                            put("ord", 1)
                            put("sticky", false)
                            put("rtl", false)
                            put("font", "Arial")
                            put("size", 20)
                        })
                    })
                    put("css", """
                        .card {
                            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                            font-size: 18px;
                            text-align: center;
                            color: #1E293B;
                            background-color: #FFFFFF;
                            padding: 16px;
                        }
                    """.trimIndent())
                    put("latexPre", "")
                    put("latexPost", "")
                    put("latexsvg", false)
                    put("req", JSONArray().apply {
                        put(JSONArray().apply {
                            put(0)
                            put("all")
                            put(JSONArray().apply { put(0) })
                        })
                    })
                })
            }

            // 3. Build Decks JSON
            val decksJson = JSONObject().apply {
                put("1", JSONObject().apply {
                    put("id", 1L)
                    put("mod", nowSec)
                    put("name", "Default")
                    put("usn", 0)
                    put("maxTaken", 60)
                    put("collapsed", false)
                    put("browserCollapsed", false)
                    put("desc", "")
                    put("dyn", 0)
                    put("conf", DEFAULT_DECK_CONF_ID)
                    put("extendNew", 10)
                    put("extendRev", 50)
                })
                put(targetDeckId.toString(), JSONObject().apply {
                    put("id", targetDeckId)
                    put("mod", nowSec)
                    put("name", deckName)
                    put("usn", -1)
                    put("maxTaken", 60)
                    put("collapsed", false)
                    put("browserCollapsed", false)
                    put("desc", "Exported from Malhoutha Medical PDF Reader.")
                    put("dyn", 0)
                    put("conf", DEFAULT_DECK_CONF_ID)
                    put("extendNew", 10)
                    put("extendRev", 50)
                })
            }

            // 4. Build DConf JSON
            val dconfJson = JSONObject().apply {
                put("1", JSONObject().apply {
                    put("id", 1L)
                    put("mod", nowSec)
                    put("name", "Default")
                    put("usn", 0)
                    put("maxTaken", 60)
                    put("autoplay", true)
                    put("timer", 0)
                    put("replayq", true)
                    put("new", JSONObject().apply {
                        put("delays", JSONArray().apply { put(1); put(10) })
                        put("ints", JSONArray().apply { put(1); put(4); put(7) })
                        put("initialFactor", 2500)
                        put("separate", true)
                        put("order", 1)
                        put("perDay", 20)
                    })
                    put("rev", JSONObject().apply {
                        put("perDay", 200)
                        put("ease4", 1.3)
                        put("fuzz", 0.05)
                        put("minSpace", 1)
                        put("ivlFctr", 1.0)
                        put("maxIvl", 36500)
                    })
                    put("lapse", JSONObject().apply {
                        put("delays", JSONArray().apply { put(10) })
                        put("mult", 0.0)
                        put("minInt", 1)
                        put("leechFails", 8)
                        put("leechAction", 0)
                    })
                })
            }

            // 5. Build Conf JSON
            val confJson = JSONObject().apply {
                put("nextPos", 1)
                put("estTimes", true)
                put("activeDecks", JSONArray().apply { put(targetDeckId) })
                put("sortType", "noteFld")
                put("timeLim", 0)
                put("sortBackwards", false)
                put("addToCur", true)
                put("curDeck", targetDeckId)
                put("curModel", MODEL_ID)
                put("collapseTime", 1200)
            }

            // Insert single row into `col`
            val insertCol = db.compileStatement("""
                INSERT INTO col (id, crt, mod, scm, ver, dty, usn, ls, conf, models, decks, dconf, tags)
                VALUES (1, ?, ?, ?, 11, 0, 0, 0, ?, ?, ?, ?, '{}')
            """.trimIndent())
            insertCol.bindLong(1, nowSec)
            insertCol.bindLong(2, nowMs)
            insertCol.bindLong(3, nowMs)
            insertCol.bindString(4, confJson.toString())
            insertCol.bindString(5, modelsJson.toString())
            insertCol.bindString(6, decksJson.toString())
            insertCol.bindString(7, dconfJson.toString())
            insertCol.executeInsert()

            // 6. Insert Notes & Cards
            val noteStmt = db.compileStatement("""
                INSERT INTO notes (id, guid, mid, mod, usn, tags, flds, sfld, csum, flags, data)
                VALUES (?, ?, ?, ?, -1, ?, ?, ?, ?, 0, '')
            """.trimIndent())

            val cardStmt = db.compileStatement("""
                INSERT INTO cards (id, nid, did, ord, mod, usn, type, queue, due, ivl, factor, reps, lapses, left, odue, odid, flags, data)
                VALUES (?, ?, ?, 0, ?, -1, 0, 0, ?, 0, 2500, 0, 0, 0, 0, 0, 0, '')
            """.trimIndent())

            cards.forEachIndexed { index, card ->
                val noteId = nowMs + (index * 2L)
                val cardId = noteId + 1L
                val guid = generateGuid()

                val frontHtml = formatFrontField(card)
                val backHtml = formatBackField(card)
                val flds = "$frontHtml\u001f$backHtml"
                val sfld = card.sourceTermEn.trim()
                val csum = calculateChecksum(sfld)

                val tagString = buildString {
                    append(" Medical Malhoutha ")
                    if (card.domain.isNotBlank()) append("${card.domain} ")
                    if (!card.subspecialty.isNullOrBlank()) append("${card.subspecialty.replace(' ', '_')} ")
                }.trim()

                // Insert Note
                noteStmt.clearBindings()
                noteStmt.bindLong(1, noteId)
                noteStmt.bindString(2, guid)
                noteStmt.bindLong(3, MODEL_ID)
                noteStmt.bindLong(4, nowSec)
                noteStmt.bindString(5, " $tagString ")
                noteStmt.bindString(6, flds)
                noteStmt.bindString(7, sfld)
                noteStmt.bindLong(8, csum)
                noteStmt.executeInsert()

                // Insert Card
                cardStmt.clearBindings()
                cardStmt.bindLong(1, cardId)
                cardStmt.bindLong(2, noteId)
                cardStmt.bindLong(3, targetDeckId)
                cardStmt.bindLong(4, nowSec)
                cardStmt.bindLong(5, (index + 1).toLong())
                cardStmt.executeInsert()
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
            db.close()
        }
    }

    private fun packageToZip(dbFile: File, outputFile: File) {
        if (outputFile.exists()) {
            outputFile.delete()
        }
        ZipOutputStream(FileOutputStream(outputFile).buffered()).use { zos ->
            // Entry 1: collection.anki2
            val ankiEntry = ZipEntry("collection.anki2")
            zos.putNextEntry(ankiEntry)
            dbFile.inputStream().buffered().use { input ->
                input.copyTo(zos)
            }
            zos.closeEntry()

            // Entry 2: media (Empty JSON dictionary for text-only package)
            val mediaEntry = ZipEntry("media")
            zos.putNextEntry(mediaEntry)
            zos.write("{}".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
    }

    private fun formatFrontField(card: SavedVocabularyCardEntity): String {
        val term = escapeHtml(card.sourceTermEn.trim())
        val domain = escapeHtml(card.domain.replace('_', ' '))
        val subspecialty = card.subspecialty?.takeIf { it.isNotBlank() }?.let { " • " + escapeHtml(it) }.orEmpty()

        return """
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; text-align: center; padding: 12px;">
                <div style="font-size: 24px; font-weight: bold; color: #1E293B; letter-spacing: 0.3px;">
                    $term
                </div>
                <div style="display: inline-block; font-size: 11px; font-weight: 700; color: #2563EB; background: rgba(37,99,235,0.08); border-radius: 6px; padding: 3px 8px; margin-top: 8px; text-transform: uppercase;">
                    [$domain]$subspecialty
                </div>
            </div>
        """.trimIndent()
    }

    private fun formatBackField(card: SavedVocabularyCardEntity): String {
        val arabicTerm = escapeHtml(card.targetTermAr.trim())
        val latin = card.latinRoot?.takeIf { it.isNotBlank() }?.let {
            """<div style="font-style: italic; color: #64748B; font-size: 14px; margin-top: 4px;">Latin: ${escapeHtml(it)}</div>"""
        }.orEmpty()

        val defAr = card.definitionAr?.takeIf { it.isNotBlank() }?.let {
            """<div style="font-size: 15px; color: #1F2937; direction: rtl; text-align: right; line-height: 1.5; margin-bottom: 6px;">${escapeHtml(it)}</div>"""
        }.orEmpty()

        val defEn = card.definitionEn?.takeIf { it.isNotBlank() }?.let {
            """<div style="font-size: 14px; color: #334155; text-align: left; line-height: 1.4;">${escapeHtml(it)}</div>"""
        }.orEmpty()

        val divider = if (defAr.isNotBlank() || defEn.isNotBlank()) {
            """<hr style="border: 0; border-top: 1px solid #E2E8F0; margin: 12px 0;" />"""
        } else ""

        return """
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 8px;">
                <div style="font-size: 26px; font-weight: bold; color: #166534; direction: rtl; text-align: right; line-height: 1.4;">
                    $arabicTerm
                </div>
                $latin
                $divider
                $defAr
                $defEn
            </div>
        """.trimIndent()
    }

    private fun calculateChecksum(text: String): Long {
        return try {
            val md = MessageDigest.getInstance("SHA-1")
            val digest = md.digest(text.toByteArray(Charsets.UTF_8))
            val hex = digest.take(4).joinToString("") { "%02x".format(it) }
            hex.toLong(16)
        } catch (_: Exception) {
            0L
        }
    }

    private fun generateGuid(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..10).map { chars[RANDOM.nextInt(chars.length)] }.joinToString("")
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
