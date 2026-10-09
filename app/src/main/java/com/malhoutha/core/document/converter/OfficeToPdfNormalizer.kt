package com.malhoutha.core.document.converter

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Xml
import androidx.core.content.FileProvider
import com.chethan616.clearpdf.utils.PptxRenderer
import com.chethan616.clearpdf.utils.UniversalDocumentConverter
import com.malhoutha.core.document.DocumentFormatResolver
import com.malhoutha.core.document.SelectableWordSpan
import com.malhoutha.core.document.SupportedDocumentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.hwpf.extractor.WordExtractor
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Normalizes PowerPoint presentations (.pptx, .ppt) and Word documents (.docx, .doc)
 * into standardized PDF format while extracting vector text boxes with normalized coordinates [0..1].
 */
object OfficeToPdfNormalizer {

    data class NormalizationResult(
        val pdfUri: Uri,
        val pageCount: Int,
        val isPresentation: Boolean,
        val textSpansByPage: Map<Int, List<SelectableWordSpan>>
    )

    /**
     * Converts the office document or presentation at [sourceUri] into a clean PDF,
     * extracting vector text bounding spans where available.
     */
    suspend fun normalizeToPdf(
        context: Context,
        sourceUri: Uri
    ): NormalizationResult = withContext(Dispatchers.IO) {
        val docType = DocumentFormatResolver.resolveType(context, sourceUri)
        val ext = DocumentFormatResolver.getFileExtension(context, sourceUri)

        when {
            ext == "pptx" -> normalizePptx(context, sourceUri)
            ext == "ppt" -> normalizeLegacyPpt(context, sourceUri)
            ext == "docx" || ext == "doc" || ext == "odt" -> normalizeOfficeDoc(context, sourceUri)
            else -> {
                // Fallback via UniversalDocumentConverter
                val convertedPdfUri = UniversalDocumentConverter.convertToPdf(context, sourceUri)
                NormalizationResult(
                    pdfUri = convertedPdfUri,
                    pageCount = 1,
                    isPresentation = docType == SupportedDocumentType.OFFICE_PRESENTATION,
                    textSpansByPage = emptyMap()
                )
            }
        }
    }

    // ── Modern PPTX Processing ──────────────────────────────────────────────

    private fun normalizePptx(context: Context, sourceUri: Uri): NormalizationResult {
        val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Cannot read presentation bytes")

        val spansByPage = mutableMapOf<Int, List<SelectableWordSpan>>()
        val slideTexts = extractPptxSlideSpans(bytes)
        slideTexts.forEachIndexed { index, spans ->
            spansByPage[index] = spans
        }

        val renderedPdf = runCatching { PptxRenderer.render(bytes) }.getOrNull()
        val pdfFile = if (renderedPdf != null) {
            writePdfDoc(context, renderedPdf, "presentation_norm")
        } else {
            val fallbackUri = UniversalDocumentConverter.convertToPdf(context, sourceUri)
            return NormalizationResult(
                pdfUri = fallbackUri,
                pageCount = slideTexts.size.coerceAtLeast(1),
                isPresentation = true,
                textSpansByPage = spansByPage
            )
        }

        return NormalizationResult(
            pdfUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile),
            pageCount = renderedPdf.pages.size.coerceAtLeast(1),
            isPresentation = true,
            textSpansByPage = spansByPage
        )
    }

    private fun extractPptxSlideSpans(bytes: ByteArray): List<List<SelectableWordSpan>> {
        val slideXmlMap = sortedMapOf<Int, ByteArray>()
        var slideWidthPt = 960f
        var slideHeightPt = 540f // Default 16:9

        ZipInputStream(bytes.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (name == "ppt/presentation.xml") {
                    val xmlBytes = zip.readBytes()
                    val (w, h) = parseSlideDimensions(xmlBytes)
                    if (w > 0f && h > 0f) {
                        slideWidthPt = w
                        slideHeightPt = h
                    }
                } else if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) {
                    val num = Regex("slide(\\d+)\\.xml").find(name)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE
                    slideXmlMap[num] = zip.readBytes()
                }
            }
        }

        val allSlides = mutableListOf<List<SelectableWordSpan>>()
        var pageIdx = 0
        for ((_, xml) in slideXmlMap) {
            val spans = parsePptxSlideTextShapes(xml, pageIdx, slideWidthPt, slideHeightPt)
            allSlides.add(spans)
            pageIdx++
        }
        return allSlides
    }

    private fun parseSlideDimensions(bytes: ByteArray): Pair<Float, Float> {
        return runCatching {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(bytes.inputStream(), "UTF-8")
            }
            var w = 0f
            var h = 0f
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && (parser.name == "p:sldSz" || parser.name == "sldSz")) {
                    val cx = parser.getAttributeValue(null, "cx")?.toFloatOrNull() ?: 0f
                    val cy = parser.getAttributeValue(null, "cy")?.toFloatOrNull() ?: 0f
                    if (cx > 0f && cy > 0f) {
                        w = cx / 12700f // EMU to points
                        h = cy / 12700f
                    }
                    break
                }
                event = parser.next()
            }
            w to h
        }.getOrDefault(0f to 0f)
    }

    private fun parsePptxSlideTextShapes(
        xmlBytes: ByteArray,
        pageIndex: Int,
        slideW: Float,
        slideH: Float
    ): List<SelectableWordSpan> {
        val spans = mutableListOf<SelectableWordSpan>()
        runCatching {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(xmlBytes.inputStream(), "UTF-8")
            }
            var event = parser.eventType
            var inShape = false
            var currentXNorm = 0.05f
            var currentYNorm = 0.05f
            var currentWNorm = 0.90f
            var currentHNorm = 0.10f
            val textBuf = StringBuilder()

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name) {
                            "p:sp", "sp" -> inShape = true
                            "a:off", "off" -> {
                                val x = parser.getAttributeValue(null, "x")?.toFloatOrNull() ?: 0f
                                val y = parser.getAttributeValue(null, "y")?.toFloatOrNull() ?: 0f
                                if (slideW > 0f && slideH > 0f) {
                                    currentXNorm = (x / 12700f / slideW).coerceIn(0f, 1f)
                                    currentYNorm = (y / 12700f / slideH).coerceIn(0f, 1f)
                                }
                            }
                            "a:ext", "ext" -> {
                                val cx = parser.getAttributeValue(null, "cx")?.toFloatOrNull() ?: 0f
                                val cy = parser.getAttributeValue(null, "cy")?.toFloatOrNull() ?: 0f
                                if (slideW > 0f && slideH > 0f) {
                                    currentWNorm = (cx / 12700f / slideW).coerceIn(0.01f, 1f)
                                    currentHNorm = (cy / 12700f / slideH).coerceIn(0.01f, 1f)
                                }
                            }
                            "a:t", "t" -> textBuf.clear()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        parser.text?.let { textBuf.append(it) }
                    }
                    XmlPullParser.END_TAG -> {
                        when (parser.name) {
                            "a:t", "t" -> {
                                val raw = textBuf.toString().trim()
                                if (raw.isNotEmpty()) {
                                    val words = raw.split("\\s+".toRegex())
                                    val count = words.size.coerceAtLeast(1)
                                    val stepX = currentWNorm / count
                                    words.forEachIndexed { wordIdx, word ->
                                        if (word.isNotBlank()) {
                                            val wLeft = (currentXNorm + (wordIdx * stepX)).coerceIn(0f, 1f)
                                            val wRight = (wLeft + stepX).coerceIn(0f, 1f)
                                            spans.add(
                                                SelectableWordSpan(
                                                    text = word,
                                                    normalizedBounds = RectF(
                                                        wLeft,
                                                        currentYNorm,
                                                        wRight,
                                                        (currentYNorm + currentHNorm).coerceIn(0f, 1f)
                                                    ),
                                                    pageIndex = pageIndex
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            "p:sp", "sp" -> inShape = false
                        }
                    }
                }
                event = parser.next()
            }
        }
        return spans
    }

    // ── Legacy PPT (Apache POI HSLF) ────────────────────────────────────────

    private fun normalizeLegacyPpt(context: Context, sourceUri: Uri): NormalizationResult {
        val stream = context.contentResolver.openInputStream(sourceUri)
            ?: throw IllegalStateException("Cannot read .ppt")

        val slideShow = HSLFSlideShow(stream)
        val slides = slideShow.slides
        val pgSize = slideShow.pageSize
        val slideW = pgSize.width.toFloat().coerceAtLeast(720f)
        val slideH = pgSize.height.toFloat().coerceAtLeast(540f)

        val doc = PdfDocument()
        val spansByPage = mutableMapOf<Int, List<SelectableWordSpan>>()

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 24f
            color = Color.BLACK
            typeface = Typeface.DEFAULT_BOLD
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            color = Color.DKGRAY
        }

        slides.forEachIndexed { index, slide ->
            val pageInfo = PdfDocument.PageInfo.Builder(slideW.toInt(), slideH.toInt(), index + 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            val pageSpans = mutableListOf<SelectableWordSpan>()
            var curY = 48f
            val marginX = 48f
            val maxW = (slideW - 2 * marginX).toInt().coerceAtLeast(100)

            val title = slide.title
            if (!title.isNullOrBlank()) {
                val layout = StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, maxW)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()
                canvas.save()
                canvas.translate(marginX, curY)
                layout.draw(canvas)
                canvas.restore()

                val yNorm = curY / slideH
                val hNorm = layout.height / slideH
                title.split("\\s+".toRegex()).forEachIndexed { wIdx, word ->
                    pageSpans.add(
                        SelectableWordSpan(
                            text = word,
                            normalizedBounds = RectF(marginX / slideW, yNorm, (marginX + maxW) / slideW, yNorm + hNorm),
                            pageIndex = index
                        )
                    )
                }
                curY += layout.height + 24f
            }

            for (textRun in slide.textParagraphs) {
                val text = textRun.joinToString(" ") { it.textRuns.joinToString("") { r -> r.rawText } }.trim()
                if (text.isNotBlank() && text != title) {
                    val layout = StaticLayout.Builder.obtain(text, 0, text.length, bodyPaint, maxW)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .build()
                    canvas.save()
                    canvas.translate(marginX, curY)
                    layout.draw(canvas)
                    canvas.restore()

                    val yNorm = curY / slideH
                    val hNorm = layout.height / slideH
                    text.split("\\s+".toRegex()).forEach { word ->
                        pageSpans.add(
                            SelectableWordSpan(
                                text = word,
                                normalizedBounds = RectF(marginX / slideW, yNorm, (marginX + maxW) / slideW, yNorm + hNorm),
                                pageIndex = index
                            )
                        )
                    }
                    curY += layout.height + 12f
                }
            }

            doc.finishPage(page)
            spansByPage[index] = pageSpans
        }

        val pdfFile = writePdfDoc(context, doc, "legacy_ppt_norm")
        return NormalizationResult(
            pdfUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile),
            pageCount = slides.size.coerceAtLeast(1),
            isPresentation = true,
            textSpansByPage = spansByPage
        )
    }

    // ── Word Document Processing ────────────────────────────────────────────

    private fun normalizeOfficeDoc(context: Context, sourceUri: Uri): NormalizationResult {
        val convertedPdfUri = UniversalDocumentConverter.convertToPdf(context, sourceUri)
        return NormalizationResult(
            pdfUri = convertedPdfUri,
            pageCount = 1,
            isPresentation = false,
            textSpansByPage = emptyMap()
        )
    }

    private fun writePdfDoc(context: Context, doc: PdfDocument, prefix: String): File {
        val dir = File(context.cacheDir, "normalized_pdf").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
