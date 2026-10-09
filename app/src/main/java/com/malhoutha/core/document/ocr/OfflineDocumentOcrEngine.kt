package com.malhoutha.core.document.ocr

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import com.chethan616.clearpdf.ui.viewmodel.OcrTextBlock
import com.kyant.ocrcore.OcrService
import com.kyant.ocrcore.OcrServiceImpl
import com.kyant.ocrcore.OcrWord
import com.malhoutha.core.document.SelectableWordSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.math.abs

/**
 * 100% Offline On-Demand OCR Engine with persistent SQLite caching.
 *
 * Automatically triggers when a document page or photograph lacks a digital text stream,
 * extracting normalized [0.0, 1.0] bounding boxes and caching them under `page_ocr_cache`.
 */
object OfflineDocumentOcrEngine {

    private val ocrService: OcrService by lazy { OcrServiceImpl() }
    private var dbHelper: OcrCacheDbHelper? = null

    @Synchronized
    private fun getDb(context: Context): SQLiteDatabase {
        if (dbHelper == null) {
            dbHelper = OcrCacheDbHelper(context.applicationContext)
        }
        return dbHelper!!.writableDatabase
    }

    /**
     * Computes a deterministic SHA-256 fingerprint for document identity caching.
     */
    fun computeContentHash(uri: Uri, sizeBytes: Long = 0L): String {
        val raw = "${uri}|${sizeBytes}"
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Reads cached OCR spans from the internal SQLite cache. Returns null if not cached.
     */
    suspend fun getCachedSpans(
        context: Context,
        docHash: String,
        pageIndex: Int
    ): List<SelectableWordSpan>? = withContext(Dispatchers.IO) {
        runCatching {
            val db = getDb(context)
            db.query(
                OcrCacheDbHelper.TABLE_NAME,
                arrayOf("data_json"),
                "doc_hash = ? AND page_index = ?",
                arrayOf(docHash, pageIndex.toString()),
                null,
                null,
                null
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    val jsonStr = cursor.getString(0)
                    parseSpansFromJson(jsonStr, pageIndex)
                } else null
            }
        }.getOrNull()
    }

    /**
     * Executes OCR on [bitmap] or returns previously cached results in 0ms.
     */
    suspend fun recognizePage(
        context: Context,
        docHash: String,
        pageIndex: Int,
        bitmap: Bitmap
    ): List<SelectableWordSpan> = withContext(Dispatchers.Default) {
        // 1. Check disk cache
        val cached = getCachedSpans(context, docHash, pageIndex)
        if (cached != null) return@withContext cached

        // 2. Perform 100% on-device OCR recognition
        val ocrResult = runCatching {
            ocrService.recognize(context, bitmap)
        }.getOrNull() ?: return@withContext emptyList()

        // 3. Map into normalized SelectableWordSpan objects [0..1]
        val spans = ocrResult.words.mapNotNull { word ->
            val cleanText = word.text.trim()
            if (cleanText.isEmpty()) return@mapNotNull null
            SelectableWordSpan(
                text = cleanText,
                normalizedBounds = RectF(
                    word.left.coerceIn(0f, 1f),
                    word.top.coerceIn(0f, 1f),
                    word.right.coerceIn(0f, 1f),
                    word.bottom.coerceIn(0f, 1f)
                ),
                pageIndex = pageIndex
            )
        }

        // 4. Persist to SQLite cache asynchronously
        withContext(Dispatchers.IO) {
            saveSpansToCache(context, docHash, pageIndex, spans)
        }

        spans
    }

    private fun saveSpansToCache(
        context: Context,
        docHash: String,
        pageIndex: Int,
        spans: List<SelectableWordSpan>
    ) {
        runCatching {
            val jsonArray = JSONArray()
            spans.forEach { span ->
                val obj = JSONObject().apply {
                    put("t", span.text)
                    put("l", span.normalizedBounds.left.toDouble())
                    put("t_y", span.normalizedBounds.top.toDouble())
                    put("r", span.normalizedBounds.right.toDouble())
                    put("b", span.normalizedBounds.bottom.toDouble())
                }
                jsonArray.put(obj)
            }
            val db = getDb(context)
            val cv = ContentValues().apply {
                put("doc_hash", docHash)
                put("page_index", pageIndex)
                put("data_json", jsonArray.toString())
                put("timestamp", System.currentTimeMillis())
            }
            db.insertWithOnConflict(
                OcrCacheDbHelper.TABLE_NAME,
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
    }

    private fun parseSpansFromJson(jsonStr: String, pageIndex: Int): List<SelectableWordSpan> {
        val spans = mutableListOf<SelectableWordSpan>()
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            spans.add(
                SelectableWordSpan(
                    text = obj.getString("t"),
                    normalizedBounds = RectF(
                        obj.getDouble("l").toFloat(),
                        obj.getDouble("t_y").toFloat(),
                        obj.getDouble("r").toFloat(),
                        obj.getDouble("b").toFloat()
                    ),
                    pageIndex = pageIndex
                )
            )
        }
        return spans
    }

    /**
     * Bridges [SelectableWordSpan] list directly into the viewer's native [OcrTextBlock] format,
     * grouping horizontal runs into paragraph lines with character-precise offsets.
     */
    fun spansToOcrTextBlocks(spans: List<SelectableWordSpan>, pageIndex: Int): List<OcrTextBlock> {
        if (spans.isEmpty()) return emptyList()

        val avgH = spans.map { it.normalizedBounds.height() }.average().toFloat().coerceAtLeast(0.005f)
        val lineGap = avgH * 0.6f

        val sorted = spans.sortedBy { it.normalizedBounds.centerY() }
        val lines = mutableListOf<MutableList<SelectableWordSpan>>()
        for (span in sorted) {
            val cy = span.normalizedBounds.centerY()
            val last = lines.lastOrNull()
            val lastCy = last?.let { l -> l.map { it.normalizedBounds.centerY() }.average().toFloat() }
            if (last == null || lastCy == null || abs(cy - lastCy) > lineGap) {
                lines.add(mutableListOf(span))
            } else {
                last.add(span)
            }
        }

        return lines.mapIndexedNotNull { lineIdx, lineSpans ->
            val byX = lineSpans.sortedBy { it.normalizedBounds.left }
            val sb = StringBuilder()
            val cl = ArrayList<Float>()
            val cr = ArrayList<Float>()
            var lastRight = -1f
            byX.forEach { s ->
                if (lastRight >= 0f) {
                    sb.append(' '); cl.add(lastRight); cr.add(s.normalizedBounds.left)
                }
                val n = s.text.length.coerceAtLeast(1)
                for (i in s.text.indices) {
                    sb.append(s.text[i])
                    val w = s.normalizedBounds.width()
                    cl.add(s.normalizedBounds.left + w * i / n)
                    cr.add(s.normalizedBounds.left + w * (i + 1) / n)
                }
                lastRight = s.normalizedBounds.right
            }
            if (sb.isEmpty()) return@mapIndexedNotNull null
            OcrTextBlock(
                id = "$pageIndex-ocr-$lineIdx",
                text = sb.toString(),
                left = byX.minOf { it.normalizedBounds.left },
                top = byX.minOf { it.normalizedBounds.top },
                right = byX.maxOf { it.normalizedBounds.right },
                bottom = byX.maxOf { it.normalizedBounds.bottom },
                charLefts = cl.toFloatArray(),
                charRights = cr.toFloatArray()
            )
        }
    }

    private class OcrCacheDbHelper(context: Context) : SQLiteOpenHelper(
        context,
        "document_ocr_cache.db",
        null,
        1
    ) {
        companion object {
            const val TABLE_NAME = "page_ocr_cache"
        }

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $TABLE_NAME (
                    doc_hash TEXT NOT NULL,
                    page_index INTEGER NOT NULL,
                    data_json TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    PRIMARY KEY (doc_hash, page_index)
                )
            """.trimIndent())
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
            onCreate(db)
        }
    }
}
