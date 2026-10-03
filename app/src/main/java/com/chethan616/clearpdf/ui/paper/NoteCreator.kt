package com.chethan616.clearpdf.ui.paper

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.chethan616.clearpdf.data.repository.RecentFile
import com.chethan616.clearpdf.data.repository.RecentFilesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates vector-backed synthetic paper note documents in Malhoutha.
 */
object NoteCreator {

    suspend fun createNote(
        context: Context,
        title: String,
        config: PaperConfig,
        pageCount: Int = 3
    ): Uri = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val cleanTitle = title.trim().replace(Regex("[^a-zA-Z0-9._ -]"), "_").ifEmpty { "Note_$timeStamp" }
        val fileName = if (cleanTitle.endsWith(".pdf", ignoreCase = true)) cleanTitle else "$cleanTitle.pdf"

        val notesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Notes").apply {
            if (!exists()) mkdirs()
        }
        val file = File(notesDir, fileName)

        val doc = android.graphics.pdf.PdfDocument()
        val pageWidth = 595 // Standard A4 in PDF points (72 pt / inch)
        val pageHeight = 842

        try {
            for (i in 0 until pageCount) {
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i).create()
                val page = doc.startPage(pageInfo)
                // Paint background color and procedural rules
                page.canvas.drawColor(config.baseColor.toArgb())
                page.canvas.drawSyntheticPaper(config, pageWidth.toFloat(), pageHeight.toFloat(), density = 1f)
                doc.finishPage(page)
            }

            FileOutputStream(file).use { out ->
                doc.writeTo(out)
                out.flush()
            }
        } finally {
            doc.close()
        }

        val outputUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        // Persist the synthetic paper config for Malhoutha's procedural viewer
        NotePaperManager.savePaperConfig(context, outputUri, config)

        // Add to Recent Files so it immediately appears in Malhoutha's substrate
        RecentFilesManager.addRecent(
            context,
            RecentFile(
                name = fileName,
                uriString = outputUri.toString(),
                timestamp = System.currentTimeMillis(),
                pageCount = pageCount
            )
        )

        outputUri
    }
}
