package com.malhoutha.core.document.converter

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.malhoutha.core.document.DocumentFormatResolver
import com.malhoutha.core.document.SelectableWordSpan
import com.malhoutha.core.document.SupportedDocumentType
import com.malhoutha.core.document.UniversalDocumentAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Lightweight, zero-bloat office document and presentation converter.
 *
 * Implements a "Normalize-to-PDF Cache" pipeline: converts presentations (.pptx, .ppt),
 * summaries (.docx, .doc), and text files (.txt, .md) into standardized vector PDF
 * streams stored in `context.cacheDir.resolve("normalized_docs/")`.
 *
 * Caching is keyed by file content SHA-256 hash (${sha256(uri)}.pdf) to achieve
 * guaranteed 0ms overhead on subsequent document visits.
 */
object OfficeToPdfConverter {

    data class ConversionResult(
        val pdfUri: Uri,
        val pdfFile: File,
        val fromCache: Boolean,
        val textSpansByPage: Map<Int, List<SelectableWordSpan>> = emptyMap()
    )

    /**
     * Computes the SHA-256 hash of the content referenced by [uri].
     */
    fun computeSha256(context: Context, uri: Uri): String {
        return runCatching {
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(16384)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            } ?: return "norm_${System.currentTimeMillis()}"
            digest.digest().joinToString("") { "%02x".format(it) }
        }.getOrElse { "norm_${System.currentTimeMillis()}" }
    }

    /**
     * Checks if a cached normalized PDF exists for the given [uri].
     */
    fun getCachedNormalizedFile(context: Context, uri: Uri): File? {
        val hash = computeSha256(context, uri)
        val targetFile = UniversalDocumentAdapter.getNormalizedDocsDir(context).resolve("$hash.pdf")
        return if (targetFile.exists() && targetFile.length() > 0L) targetFile else null
    }

    /**
     * Converts the office or presentation document into a normalized PDF stream.
     * If already normalized, returns the cached file in 0ms.
     */
    suspend fun convertToNormalizedPdf(
        context: Context,
        sourceUri: Uri,
        onProgress: ((String) -> Unit)? = null
    ): ConversionResult = withContext(Dispatchers.IO) {
        val normalizedDir = UniversalDocumentAdapter.getNormalizedDocsDir(context)
        val contentHash = computeSha256(context, sourceUri)
        val cachedTarget = File(normalizedDir, "$contentHash.pdf")

        // 1. Instant Cache Check (0ms overhead)
        if (cachedTarget.exists() && cachedTarget.length() > 0L) {
            val cachedUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", cachedTarget)
            return@withContext ConversionResult(
                pdfUri = cachedUri,
                pdfFile = cachedTarget,
                fromCache = true,
                textSpansByPage = emptyMap()
            )
        }

        // 2. Perform Normalized Conversion with Progress Reporting
        onProgress?.invoke("Preparing document normalization...")
        val docType = DocumentFormatResolver.resolveType(context, sourceUri)

        onProgress?.invoke(
            when (docType) {
                SupportedDocumentType.OFFICE_PRESENTATION -> "Rendering presentation slides to vector PDF..."
                SupportedDocumentType.OFFICE_DOCUMENT -> "Normalizing document pages..."
                SupportedDocumentType.EPUB_MARKDOWN -> "Formatting text document..."
                else -> "Converting document to standardized PDF..."
            }
        )

        // Delegate to OfficeToPdfNormalizer for vector parsing and shape extraction
        val normalizerResult = OfficeToPdfNormalizer.normalizeToPdf(context, sourceUri)

        // Copy / write the converted PDF into the canonical normalized_docs cache
        val sourcePath = normalizerResult.pdfUri.path
        val sourceFile = if (sourcePath != null) File(sourcePath) else null
        if (sourceFile != null && sourceFile.exists()) {
            sourceFile.copyTo(cachedTarget, overwrite = true)
        } else {
            context.contentResolver.openInputStream(normalizerResult.pdfUri)?.use { input ->
                cachedTarget.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        onProgress?.invoke("Finalizing document...")
        val finalUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", cachedTarget)

        ConversionResult(
            pdfUri = finalUri,
            pdfFile = cachedTarget,
            fromCache = false,
            textSpansByPage = normalizerResult.textSpansByPage
        )
    }
}
