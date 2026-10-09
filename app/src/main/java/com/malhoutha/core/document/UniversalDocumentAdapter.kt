package com.malhoutha.core.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import java.io.File

/**
 * Universal multi-format document classification for medical school educational resources.
 */
enum class SupportedDocumentType {
    VECTOR_PDF,
    SCANNED_PDF,
    OFFICE_PRESENTATION, // PPTX, PPT
    OFFICE_DOCUMENT,     // DOCX, DOC, XLSX, ODT
    STANDALONE_IMAGE,    // PNG, JPG, JPEG, WEBP, BMP, HEIC
    EPUB_MARKDOWN        // EPUB, TXT, MD, CSV
}

/**
 * Geometric and text-stream metadata for a single document page or slide.
 */
data class DocumentPageMetadata(
    val pageIndex: Int,
    val widthPx: Int,
    val heightPx: Int,
    val aspectRatio: Float,
    val hasSelectableTextStream: Boolean
)

/**
 * Normalized selectable word or token span with bounding box relative to page bounds [0.0, 1.0].
 */
data class SelectableWordSpan(
    val text: String,
    val normalizedBounds: RectF, // [0..1] relative to page dimensions
    val pageIndex: Int
)

/**
 * Common format-agnostic abstraction layer across PDF, presentations, scanned sheets, and images.
 */
interface UniversalDocumentAdapter {
    val documentType: SupportedDocumentType
    val pageCount: Int
    suspend fun loadDocument(uri: Uri): Result<Unit>
    suspend fun renderPageBitmap(pageIndex: Int, targetWidth: Int, targetHeight: Int): Bitmap
    suspend fun getPageMetadata(pageIndex: Int): DocumentPageMetadata
    suspend fun extractSelectableSpans(pageIndex: Int): List<SelectableWordSpan>
    suspend fun close()

    companion object {
        const val NORMALIZED_DOCS_DIR = "normalized_docs"

        fun getNormalizedDocsDir(context: Context): File {
            return context.cacheDir.resolve(NORMALIZED_DOCS_DIR).apply {
                if (!exists()) mkdirs()
            }
        }
    }
}
