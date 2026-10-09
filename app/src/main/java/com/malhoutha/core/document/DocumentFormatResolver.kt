package com.malhoutha.core.document

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.util.Locale

/**
 * High-performance format inspector and MIME classifier determining the optimal ingestion pipeline.
 */
object DocumentFormatResolver {

    private val PRESENTATION_EXTENSIONS = setOf("pptx", "ppt")
    private val DOCUMENT_EXTENSIONS = setOf("docx", "doc", "xlsx", "xls", "odt")
    private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "webp", "bmp", "heic", "gif")
    private val EPUB_TEXT_EXTENSIONS = setOf("epub", "txt", "md", "csv", "log", "rtf")

    /**
     * Resolves the [SupportedDocumentType] from Android [Uri] by examining MIME type and file extension.
     */
    fun resolveType(context: Context, uri: Uri): SupportedDocumentType {
        val mimeType = context.contentResolver.getType(uri)?.lowercase(Locale.ROOT) ?: ""
        val fileName = getFileName(context, uri).lowercase(Locale.ROOT)
        val ext = fileName.substringAfterLast('.', "")

        return when {
            // 1. Office Presentation Decks
            ext in PRESENTATION_EXTENSIONS || mimeType.contains("presentation") || mimeType.contains("powerpoint") ->
                SupportedDocumentType.OFFICE_PRESENTATION

            // 2. Word & Spreadsheet Summaries
            ext in DOCUMENT_EXTENSIONS || mimeType.contains("wordprocessing") || mimeType.contains("msword") ||
                mimeType.contains("spreadsheet") || mimeType.contains("ms-excel") ->
                SupportedDocumentType.OFFICE_DOCUMENT

            // 3. Standalone Images / Clinical Snaps / Board Photos
            ext in IMAGE_EXTENSIONS || mimeType.startsWith("image/") ->
                SupportedDocumentType.STANDALONE_IMAGE

            // 4. EPUB & Markdown Summaries
            ext in EPUB_TEXT_EXTENSIONS || mimeType.contains("epub") || mimeType.startsWith("text/") || mimeType.contains("markdown") ->
                SupportedDocumentType.EPUB_MARKDOWN

            // 5. Standard PDF (Vector or Scanned)
            ext == "pdf" || mimeType.contains("pdf") ->
                SupportedDocumentType.VECTOR_PDF

            else -> SupportedDocumentType.VECTOR_PDF
        }
    }

    fun isPresentation(context: Context, uri: Uri): Boolean =
        resolveType(context, uri) == SupportedDocumentType.OFFICE_PRESENTATION

    fun isOfficeDocument(context: Context, uri: Uri): Boolean =
        resolveType(context, uri) == SupportedDocumentType.OFFICE_DOCUMENT

    fun isStandaloneImage(context: Context, uri: Uri): Boolean =
        resolveType(context, uri) == SupportedDocumentType.STANDALONE_IMAGE

    fun getFileName(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "document"
    }

    fun getFileExtension(context: Context, uri: Uri): String {
        val name = getFileName(context, uri)
        return name.substringAfterLast('.', "").lowercase(Locale.ROOT)
    }
}
