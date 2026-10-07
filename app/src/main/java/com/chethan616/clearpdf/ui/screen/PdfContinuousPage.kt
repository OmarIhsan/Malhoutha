package com.chethan616.clearpdf.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.StrikethroughS
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.chethan616.clearpdf.ui.paper.PaperConfig
import com.chethan616.clearpdf.ui.paper.drawSyntheticPaper
import com.chethan616.clearpdf.ui.components.StickyNoteCard
import com.malhoutha.core.ink.models.StickyCardAnnotation
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.malhoutha.R
import com.chethan616.clearpdf.ui.selection.PdfTextSelectionState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import java.util.concurrent.atomic.AtomicReference
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.chethan616.clearpdf.ui.viewmodel.FindMatch
import com.chethan616.clearpdf.ui.viewmodel.OcrTextBlock
import com.chethan616.clearpdf.ui.viewmodel.OcrTextRange
import kotlin.math.max
import kotlin.math.min

/**
 * A single page inside the continuous (Adobe-style) vertical viewer.
 *
 * Zoom and pan are owned by the parent container (a [graphicsLayer] wrapping the whole
 * page column), so all pointer coordinates arrive already in this page's local, unscaled
 * space. Because the rendered bitmap fills the item width, local space == content space
 * and no manual zoom/pan projection is needed here — only tool gestures live on the page.
 */
@Composable
internal fun PdfContinuousPage(
    page: Int,
    bitmap: Bitmap?,
    marks: MutableList<PdfMarkup>,
    ocrBlocks: List<OcrTextBlock>,
    findMatches: List<FindMatch>,
    currentMatchIndex: Int,
    showFindBar: Boolean,
    activeTool: PdfEditTool,
    shapeMode: InkShapeMode = InkShapeMode.Free,
    currentColor: Color,
    currentStrokeWidth: Float,
    activeImageId: Long?,
    pageCanvasSizes: SnapshotStateMap<Int, Size>,
    pageBitmapSizes: SnapshotStateMap<Int, Size>,
    onInteraction: () -> Unit,
    /** A stroke/shape just landed on this page. Feeds the viewer's undo history. */
    onMarkAdded: () -> Unit = {},
    onToggleControls: () -> Unit,
    onShowControls: () -> Unit,
    onActiveToolChanged: (PdfEditTool) -> Unit,
    onActiveImageIdChanged: (Long?) -> Unit,
    onPlaceText: (Offset) -> Unit,
    onPlaceNote: (Offset) -> Unit,
    onEditAnnotation: (Long) -> Unit,
    onEditShape: (Int) -> Unit = {},
    // Generic markup selection (shapes / text / notes) for move + resize.
    selectedMarkupIndex: Int = -1,
    onSelectMarkup: (Int) -> Unit = {},
    onDeleteMarkup: (Int) -> Unit = {},
    onTranslateMarkup: ((Int, Rect) -> Unit)? = null,
    /** The viewer's text selection: this page draws its slice of the highlight and registers its coordinates. */
    textSelection: PdfTextSelectionState,
    /** Procedural synthetic paper template (Ruled, Grid, Dot-Matrix, Cornell, Plain). Infinitely sharp at any zoom. */
    paperConfig: PaperConfig? = null,
    // Sticky Card Notes (Post-it style)
    stickyNotes: List<StickyCardAnnotation> = emptyList(),
    selectedStickyNoteId: String? = null,
    onSelectStickyNote: (String?) -> Unit = {},
    onStickyNoteChanged: (StickyCardAnnotation) -> Unit = {},
    onDeleteStickyNote: (String) -> Unit = {},
    onPlaceStickyNote: (Offset) -> Unit = {},
    onPlaceImage: ((Offset) -> Unit)? = null,
    onTransformGesture: ((centroid: Offset, pan: Offset, zoom: Float) -> Unit)? = null
) {
    val inFlightState = remember(page) { InFlightInkState() }
    val hostView = LocalView.current
    val pendingHandoffClear = remember(page) { AtomicReference<(() -> Unit)?>(null) }
    val selectionColors = LocalTextSelectionColors.current
    val localDensity = LocalDensity.current
    var localPageSize by remember { mutableStateOf(Size.Zero) }
    // Generous 44dp touch-slop hit target (22dp radial threshold) for accessible stylus & finger interaction
    val handleHitRadiusPx = with(localDensity) { 22.dp.toPx() }
    val handleHitRadiusSq = handleHitRadiusPx * handleHitRadiusPx
    val transformPadPx = with(localDensity) { 32.dp.toPx() }
    DisposableEffect(page, textSelection) { onDispose { textSelection.unregisterPage(page, null) } }
    // Accessibility: expose the page's extracted text to TalkBack, plus a "select page text" action.
    val selectPageLabel = stringResource(R.string.selection_select_page_text)
    val pageText = remember(ocrBlocks) { textSelection.layout(page)?.text.orEmpty() }
    val pageTextSemantics = if (pageText.isEmpty()) Modifier else Modifier.semantics {
        text = AnnotatedString(pageText)
        customActions = listOf(CustomAccessibilityAction(selectPageLabel) { textSelection.selectPages(page, page); true })
    }

    // Page layout (rebuilt on the Pdf_Tools model): the image is drawn at its TRUE
    // aspect via ContentScale.FillWidth, so the box height follows the bitmap. There
    // is no forced-aspect placeholder jump, and single / landscape pages lay out
    // correctly. Every overlay uses matchParentSize() so its coordinate frame is
    // exactly the image frame (0,0 → box size).
    // Height to reserve while this page has no bitmap — either it has not rendered yet, or it
    // rendered once and was evicted from the cache while staying composed.
    //
    // This used to be hardcoded to A4 portrait. On a landscape document (a converted .pptx is 16:9)
    // that reserved a box ~2.5x taller than the page, and the box collapsed the instant the bitmap
    // arrived. On the last page that collapse is a feedback loop: the list is centred
    // (`Arrangement.Center`), so the shrink shifts content, which can push the page out of the
    // viewport, which disposes it, which restores the tall placeholder, which shifts it back in —
    // visible as the last page flickering. Reserving the page's real aspect removes the size change
    // entirely, so there is nothing left to oscillate.
    //
    // `pageBitmapSizes` survives eviction (it is only cleared when the document changes), so a page
    // that has ever rendered knows its own shape; anything else borrows the first page that does,
    // since documents are near enough uniform. Only the very first page of a fresh document falls
    // through to the A4 guess.
    val placeholderAspect = if (bitmap != null) null else {
        (pageBitmapSizes[page] ?: pageBitmapSizes.values.firstOrNull { it.width > 0f && it.height > 0f })
            ?.takeIf { it.width > 0f && it.height > 0f }
            ?.let { it.width / it.height }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .then(if (bitmap == null) Modifier.aspectRatio(placeholderAspect ?: (1f / 1.414f)) else Modifier)
            .background(Color(0xFF15181E))
            // Page-local coordinates for the text selection's screen mapping (see PdfViewportTransform).
            .onGloballyPositioned { textSelection.registerPage(page, it) }
            .then(pageTextSemantics)
            .onSizeChanged { sz ->
                val fSize = Size(sz.width.toFloat(), sz.height.toFloat())
                localPageSize = fSize
                pageCanvasSizes[page] = fSize
                if (bitmap != null) pageBitmapSizes[page] = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
            }
    ) {
        if (bitmap == null) {
            if (paperConfig != null) {
                // For synthetic note documents, immediately render the procedural paper
                // without waiting for rasterizer or showing a loading indicator.
                Canvas(Modifier.matchParentSize()) {
                    drawSyntheticPaper(paperConfig)
                }
            } else {
                Box(Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1976D2), strokeWidth = 2.dp)
                }
                return@Box
            }
        } else {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // The image fills the box width and the box height follows it, so the content
        // frame is the full box.
        Canvas(Modifier.matchParentSize()) {
            val frame = Rect(0f, 0f, size.width, size.height)

            // Render procedural synthetic paper template if configured
            if (paperConfig != null) {
                drawSyntheticPaper(paperConfig)
            }

            // Text selection highlight — the platform selection colour, one band per line fragment.
            // Drawn inside the zoom layer so it scales with the page, exactly like the glyphs.
            textSelection.pageRange(page)?.let { (from, to) ->
                textSelection.layout(page)?.selectionRects(from, to, size, density)?.forEach { r ->
                    drawRect(selectionColors.backgroundColor, r.topLeft, r.size)
                }
            }

            marks.forEach { markup ->
                when (markup) {
                    is PdfMarkup.StrokeMarkup -> if (markup.points.size > 1) {
                        val path = if (markup.isClosed) {
                            Path().apply {
                                moveTo(markup.points[0].x, markup.points[0].y)
                                for (i in 1 until markup.points.size) {
                                    lineTo(markup.points[i].x, markup.points[i].y)
                                }
                                close()
                            }
                        } else {
                            smoothPath(markup.points)
                        }
                        val blend = if (markup.isHighlight) androidx.compose.ui.graphics.BlendMode.SrcOver else androidx.compose.ui.graphics.drawscope.DrawScope.DefaultBlendMode
                        drawPath(
                            path = path,
                            color = markup.color.copy(markup.alpha),
                            style = Stroke(markup.width, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            blendMode = blend
                        )
                    }
                    is PdfMarkup.RectMarkup -> {
                        val r = Rect(min(markup.start.x, markup.end.x), min(markup.start.y, markup.end.y), max(markup.start.x, markup.end.x), max(markup.start.y, markup.end.y))
                        val blend = if (markup.isHighlight) androidx.compose.ui.graphics.BlendMode.SrcOver else androidx.compose.ui.graphics.drawscope.DrawScope.DefaultBlendMode
                        if (markup.filled) drawRect(markup.color.copy(markup.alpha), r.topLeft, r.size, blendMode = blend)
                        else drawRect(markup.color.copy(markup.alpha), r.topLeft, r.size, style = Stroke(markup.width, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend)
                    }
                    is PdfMarkup.OvalMarkup -> {
                        val r = Rect(min(markup.start.x, markup.end.x), min(markup.start.y, markup.end.y), max(markup.start.x, markup.end.x), max(markup.start.y, markup.end.y))
                        val blend = if (markup.isHighlight) androidx.compose.ui.graphics.BlendMode.SrcOver else androidx.compose.ui.graphics.drawscope.DrawScope.DefaultBlendMode
                        if (markup.filled) drawOval(markup.color.copy(markup.alpha), r.topLeft, r.size, blendMode = blend)
                        else drawOval(markup.color.copy(markup.alpha), r.topLeft, r.size, style = Stroke(markup.width, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = blend)
                    }
                    is PdfMarkup.LineMarkup -> {
                        val blend = if (markup.isHighlight) androidx.compose.ui.graphics.BlendMode.SrcOver else androidx.compose.ui.graphics.drawscope.DrawScope.DefaultBlendMode
                        if (markup.arrowHead) drawArrow(markup.start, markup.end, markup.color.copy(markup.alpha), markup.width, blendMode = blend)
                        else drawLine(markup.color.copy(markup.alpha), markup.start, markup.end, markup.width, cap = StrokeCap.Round, blendMode = blend)
                    }
                    is PdfMarkup.TextBlockHighlightMarkup -> ocrBlocks.firstOrNull { it.id == markup.blockId }?.let { b ->
                        val range = OcrTextRange(markup.blockId, markup.start, markup.end)
                        val r = expandedTextHighlightRect(ocrTextRangeToRect(b, range, frame))
                        drawRoundRect(markup.color.copy(markup.alpha), r.topLeft, r.size, CornerRadius((r.height * 0.14f).coerceIn(2f, 5f), (r.height * 0.14f).coerceIn(2f, 5f)))
                    }
                    is PdfMarkup.TextBlockLineMarkup -> ocrBlocks.firstOrNull { it.id == markup.blockId }?.let { b ->
                        val r = ocrTextRangeToRect(b, OcrTextRange(markup.blockId, markup.start, markup.end), frame)
                        val y = markup.textMarkupLineY(r) ?: return@let
                        drawLine(
                            color = markup.color.copy(markup.alpha),
                            start = Offset(r.left, y),
                            end = Offset(r.right, y),
                            strokeWidth = markup.width.coerceIn(2f, 4f),
                            cap = StrokeCap.Round
                        )
                    }
                    is PdfMarkup.ImageMarkup -> {
                        val r = Rect(min(markup.start.x, markup.end.x), min(markup.start.y, markup.end.y), max(markup.start.x, markup.end.x), max(markup.start.y, markup.end.y))
                        if (!markup.bitmap.isRecycled && markup.bitmap.width > 0) runCatching {
                            drawImage(
                                image = markup.bitmap.asImageBitmap(),
                                srcOffset = androidx.compose.ui.unit.IntOffset.Zero,
                                srcSize = androidx.compose.ui.unit.IntSize(markup.bitmap.width, markup.bitmap.height),
                                dstOffset = androidx.compose.ui.unit.IntOffset(r.left.toInt(), r.top.toInt()),
                                dstSize = androidx.compose.ui.unit.IntSize(r.width.toInt().coerceAtLeast(1), r.height.toInt().coerceAtLeast(1))
                            )
                        }
                        if (markup.id == activeImageId && (activeTool == PdfEditTool.None || activeTool == PdfEditTool.Image || activeTool == PdfEditTool.Signature)) {
                            // Unified prominent boundary frame + high-contrast 17dp corner anchors & 23dp resize handle
                            drawSelectionGizmo(bounds = r, isResizable = true)
                        }
                    }
                    is PdfMarkup.TextBoxMarkup -> {
                        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            color = markup.color.toArgb(); textSize = markup.fontSize
                        }
                        val linesT = if (markup.text.isEmpty()) listOf("") else markup.text.split("\n")
                        drawIntoCanvas { c ->
                            var yy = markup.position.y + markup.fontSize
                            linesT.forEach { ln -> c.nativeCanvas.drawText(ln, markup.position.x, yy, paint); yy += markup.fontSize * 1.2f }
                        }
                        if (markup.text.isEmpty()) drawRect(Color(0xFF1976D2).copy(0.5f),
                            Offset(markup.position.x - 4f, markup.position.y - 4f), Size(markup.fontSize * 5f, markup.fontSize * 1.4f), style = Stroke(2f))
                    }
                    is PdfMarkup.NoteMarkup -> {
                        val sz = 30f; val tl = markup.anchor
                        drawRoundRect(markup.color, tl, Size(sz, sz), CornerRadius(6f, 6f))
                        val fold = Path().apply { moveTo(tl.x + sz * 0.62f, tl.y); lineTo(tl.x + sz, tl.y + sz * 0.38f); lineTo(tl.x + sz * 0.62f, tl.y + sz * 0.38f); close() }
                        drawPath(fold, Color.White.copy(0.55f))
                        val lc = Color.White.copy(0.75f)
                        drawLine(lc, Offset(tl.x + 6f, tl.y + sz * 0.56f), Offset(tl.x + sz - 6f, tl.y + sz * 0.56f), 2f)
                        drawLine(lc, Offset(tl.x + 6f, tl.y + sz * 0.74f), Offset(tl.x + sz - 9f, tl.y + sz * 0.74f), 2f)
                    }
                }
            }

            // Selection frame + handles for the selected shape / text / note.
            if (activeTool == PdfEditTool.None || activeTool == PdfEditTool.Text) {
                marks.getOrNull(selectedMarkupIndex)?.takeIf {
                    (activeTool == PdfEditTool.None || (it is PdfMarkup.TextBoxMarkup || it is PdfMarkup.NoteMarkup)) && it.isTransformable()
                }?.let { selM ->
                    selM.movableBounds()?.let { b ->
                        val framePad = 6.dp.toPx()
                        val fr = Rect(b.left - framePad, b.top - framePad, b.right + framePad, b.bottom + framePad)
                        drawSelectionGizmo(bounds = fr, isResizable = selM.isResizable())
                    }
                }

                // OCR markups are anchored to extracted text rather than freely movable
                // geometry, so they get their own subtle focus ring instead of the generic
                // resize frame. The precise range remains visible and the action bubble below
                // supplies the delete affordance.
                marks.getOrNull(selectedMarkupIndex)
                    ?.takeIf { it is PdfMarkup.TextBlockHighlightMarkup || it is PdfMarkup.TextBlockLineMarkup }
                    ?.textMarkupRangeRect(ocrBlocks, frame)
                    ?.let { r ->
                        val focus = Color(0xFF0A84FF).copy(0.9f)
                        val bounds = if (marks[selectedMarkupIndex] is PdfMarkup.TextBlockLineMarkup) {
                            val y = marks[selectedMarkupIndex].textMarkupLineY(r) ?: r.bottom
                            Rect(r.left - 5f, y - 5f, r.right + 5f, y + 5f)
                        } else {
                            r.inflate(3f)
                        }
                        drawRoundRect(
                            focus,
                            bounds.topLeft,
                            bounds.size,
                            CornerRadius(6f, 6f),
                            style = Stroke(2f)
                        )
                    }
            }

            if (showFindBar && findMatches.isNotEmpty()) {
                val activeMatch = findMatches.getOrNull(currentMatchIndex)
                findMatches.filter { it.pageIndex == page }.forEach { match ->
                    // Highlight the exact matched WORD (normalized rect), not the whole line.
                    val r = Rect(
                        frame.left + match.left * frame.width,
                        frame.top + match.top * frame.height,
                        frame.left + match.right * frame.width,
                        frame.top + match.bottom * frame.height
                    )
                    // The rect carries the word's EXACT bounds (per-glyph width × glyph box height).
                    // Extend it a touch VERTICALLY so ascenders (the dot on "i") and descenders (the
                    // tail on "p"/"g") fall INSIDE the selection — the OCR/text box hugs the x-height,
                    // so without this those strokes poke out above/below the fill. A hair of horizontal
                    // padding keeps it snug across font sizes.
                    val padX = (r.height * 0.05f).coerceIn(0.75f, 3f)
                    val padTop = r.height * 0.13f
                    val padBottom = r.height * 0.11f
                    val hl = Rect(r.left - padX, r.top - padTop, r.right + padX, r.bottom + padBottom)
                    val cr = (hl.height * 0.14f).coerceIn(2f, 5f)
                    // A TRUE text-selection overlay: one uniform blue fill covering the whole glyph
                    // area (drawn over the page, so the glyphs read through it as a darker shape —
                    // Google-Docs / Acrobat style), not a faint tint sitting behind the ink.
                    if (match == activeMatch) {
                        val base = Color(0xFF3B82F6)
                        drawRoundRect(base.copy(0.52f), hl.topLeft, hl.size, CornerRadius(cr, cr))
                        // Focused-result border (unchanged style) so the current hit stands out.
                        drawRoundRect(base.copy(0.9f), hl.topLeft, hl.size, CornerRadius(cr, cr), style = Stroke(1.25f))
                    } else {
                        // Secondary results: same uniform coverage, lower emphasis, no border.
                        drawRoundRect(Color(0xFF60A5FA).copy(0.42f), hl.topLeft, hl.size, CornerRadius(cr, cr))
                    }
                }
            }

            // Two-Phase Handoff Synchronization:
            // Compose has now completed drawing the committed mark into its display list.
            // Post the front-buffer clearance to the host view animation queue so it executes
            // on the next animation frame immediately after this RenderNode is presented.
            val clearAction = pendingHandoffClear.getAndSet(null)
            if (clearAction != null) {
                hostView.postOnAnimation { clearAction.invoke() }
            }
        }

        // ── Tool gesture layers (local coordinates == content coordinates) ──────
        val drawingToolActive = activeTool in setOf(
            PdfEditTool.Draw, PdfEditTool.Highlight, PdfEditTool.Rect, PdfEditTool.Ellipse, PdfEditTool.Line, PdfEditTool.Arrow, PdfEditTool.Triangle
        )

        // Reading mode: a plain tap on a placed markup selects it. Images jump to their
        // dedicated toolbar (replace/recolour/delete); shapes/text/notes get a selection
        // frame with move + resize handles and a small Edit/Delete bar. A tap that misses
        // every markup deselects and falls through to the container (chrome toggle / zoom).
        if (activeTool == PdfEditTool.None) {
            Box(Modifier.matchParentSize().pointerInput(page, marks.size) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // If a markup is already selected, let its transform layer handle touches
                    // inside its frame (don't steal them here).
                    val sel = marks.getOrNull(selectedMarkupIndex)?.takeIf { it.isTransformable() }
                    val selBounds = sel?.movableBounds()
                    if (selBounds != null && selBounds.inflate(transformPadPx).contains(down.position)) return@awaitEachGesture
                    val selImg = marks.firstOrNull { it is PdfMarkup.ImageMarkup && it.id == activeImageId } as? PdfMarkup.ImageMarkup
                    val selImgBounds = selImg?.let { Rect(min(it.start.x, it.end.x), min(it.start.y, it.end.y), max(it.start.x, it.end.x), max(it.start.y, it.end.y)) }
                    if (selImgBounds != null && selImgBounds.inflate(transformPadPx).contains(down.position)) return@awaitEachGesture

                    val frame = Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())
                    val idx = marks.indexOfLast { it.hitTest(down.position, ocrBlocks, frame) }
                    if (idx >= 0) {
                        val hit = marks[idx]
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            up.consume()
                            when (hit) {
                                is PdfMarkup.ImageMarkup -> {
                                    onSelectMarkup(-1)
                                    onActiveImageIdChanged(hit.id)
                                }
                                is PdfMarkup.TextBoxMarkup,
                                is PdfMarkup.NoteMarkup,
                                is PdfMarkup.RectMarkup,
                                is PdfMarkup.OvalMarkup,
                                is PdfMarkup.LineMarkup,
                                is PdfMarkup.StrokeMarkup,
                                is PdfMarkup.TextBlockHighlightMarkup,
                                is PdfMarkup.TextBlockLineMarkup -> {
                                    onActiveImageIdChanged(null)
                                    onSelectMarkup(idx)
                                }
                                else -> Unit
                            }
                            onShowControls()
                            onInteraction()
                        }
                    } else {
                        // Missed everything → clear any selection (tap propagates to container).
                        if (selectedMarkupIndex >= 0) onSelectMarkup(-1)
                        if (activeImageId != null) onActiveImageIdChanged(null)
                        if (selectedStickyNoteId != null) onSelectStickyNote(null)
                    }
                }
            })
        }



        if (drawingToolActive) {
            HardwareInkingSurface(
                modifier = Modifier.matchParentSize(),
                state = inFlightState,
                page = page,
                activeTool = activeTool,
                shapeMode = shapeMode,
                currentColor = currentColor,
                currentStrokeWidth = currentStrokeWidth,
                onInteraction = onInteraction,
                onStrokeCommitted = { newMarkup, onDrawn ->
                    val previousClear = pendingHandoffClear.getAndSet(onDrawn)
                    previousClear?.invoke()
                    marks.add(newMarkup)
                    onMarkAdded()
                },
                onTransformGesture = onTransformGesture
            )
        }

        if (activeTool == PdfEditTool.Eraser) {
            Box(Modifier.matchParentSize().pointerInput(page) {
                detectTapGestures { p ->
                    val frame = Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())
                    val idx = marks.indexOfLast { it.hitTest(p, ocrBlocks, frame) }
                    if (idx >= 0) marks.removeAt(idx)
                    onInteraction()
                }
            }.pointerInput(page) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val initialCount = currentEvent.changes.count { it.pressed }
                    if (initialCount >= 2) {
                        do {
                            val event = awaitPointerEvent()
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                onTransformGesture?.invoke(centroid, panChange, zoomChange)
                            }
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                        return@awaitEachGesture
                    }
                    down.consume()
                    val frame = Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())
                    val idx = marks.indexOfLast { it.hitTest(down.position, ocrBlocks, frame) }
                    if (idx >= 0) marks.removeAt(idx)
                    onInteraction()
                    val pointerId = down.id
                    var isTwoFingerTransform = false
                    while (true) {
                        val event = awaitPointerEvent()
                        if (isTwoFingerTransform) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                onTransformGesture?.invoke(centroid, panChange, zoomChange)
                            }
                            event.changes.forEach { it.consume() }
                            if (event.changes.none { it.pressed }) break
                            continue
                        }
                        if (event.changes.count { it.pressed } >= 2) {
                            isTwoFingerTransform = true
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                onTransformGesture?.invoke(centroid, panChange, zoomChange)
                            }
                            event.changes.forEach { it.consume() }
                            continue
                        }
                        val change = event.changes.firstOrNull { it.id == pointerId }
                        if (change == null || !change.pressed) break
                        change.consume()
                        val batched = extractDigitizerBatchPoints(change, event)
                        for (i in 0 until batched.size) {
                            val bIdx = marks.indexOfLast { it.hitTest(batched[i], ocrBlocks, frame) }
                            if (bIdx >= 0) marks.removeAt(bIdx)
                        }
                        val cIdx = marks.indexOfLast { it.hitTest(change.position, ocrBlocks, frame) }
                        if (cIdx >= 0) marks.removeAt(cIdx)
                        onInteraction()
                    }
                }
            })
        }

        val pageW = if (localPageSize.width > 0f) localPageSize.width else (pageCanvasSizes[page]?.width ?: 1f)
        val pageH = if (localPageSize.height > 0f) localPageSize.height else (pageCanvasSizes[page]?.height ?: 1f)

        // ── Intentional Spatial Placement Layer for Insertion Tools (Text, StickyNote, Image) ──
        val insertionToolActive = activeTool in setOf(
            PdfEditTool.Text, PdfEditTool.StickyNote, PdfEditTool.Image
        )
        if (insertionToolActive) {
            Box(Modifier.matchParentSize().pointerInput(page, activeTool, marks.size, stickyNotes.size, selectedMarkupIndex, activeImageId, selectedStickyNoteId) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // 1. If an existing element is already selected, let its transform / editor layer handle touches within its bounds
                    if (activeTool == PdfEditTool.Text) {
                        val sel = marks.getOrNull(selectedMarkupIndex)?.takeIf { (it is PdfMarkup.TextBoxMarkup || it is PdfMarkup.NoteMarkup) && it.isTransformable() }
                        val selBounds = sel?.movableBounds()
                        if (selBounds != null && selBounds.inflate(transformPadPx).contains(down.position)) return@awaitEachGesture
                    } else if (activeTool == PdfEditTool.Image) {
                        val selImg = marks.firstOrNull { it is PdfMarkup.ImageMarkup && it.id == activeImageId } as? PdfMarkup.ImageMarkup
                        val selImgBounds = selImg?.let { Rect(min(it.start.x, it.end.x), min(it.start.y, it.end.y), max(it.start.x, it.end.x), max(it.start.y, it.end.y)) }
                        if (selImgBounds != null && selImgBounds.inflate(transformPadPx).contains(down.position)) return@awaitEachGesture
                    }

                    // 2. Prevent collision with existing Sticky Notes: if down lands on a note, focus/unfold it without placing a new one
                    if (activeTool == PdfEditTool.StickyNote) {
                        val hitNote = stickyNotes.lastOrNull { note ->
                            val px = note.xNorm * pageW
                            val py = note.yNorm * pageH
                            val pw = if (note.isFolded) with(localDensity) { 38.dp.toPx() } else (note.widthNorm * pageW).coerceAtLeast(with(localDensity) { 160.dp.toPx() })
                            val ph = if (note.isFolded) with(localDensity) { 38.dp.toPx() } else (note.heightNorm * pageH).coerceAtLeast(with(localDensity) { 120.dp.toPx() })
                            Rect(px, py, px + pw, py + ph).contains(down.position)
                        }
                        if (hitNote != null) {
                            onSelectMarkup(-1)
                            onActiveImageIdChanged(null)
                            onSelectStickyNote(hitNote.id)
                            onShowControls()
                            onInteraction()
                            return@awaitEachGesture
                        }
                    }

                    // 3. Multi-touch handling: Two-finger pan & pinch-to-zoom delegation
                    if (currentEvent.changes.count { it.pressed } >= 2) {
                        do {
                            val event = awaitPointerEvent()
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                onTransformGesture?.invoke(centroid, panChange, zoomChange)
                            }
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                        return@awaitEachGesture
                    }

                    // 4. Track single-pointer movement vs intentional tap
                    var movedPastSlop = false
                    var isTwoFingerTransform = false
                    val startPos = down.position
                    val touchSlop = viewConfiguration.touchSlop
                    var upOrCancel: PointerInputChange? = null

                    while (true) {
                        val event = awaitPointerEvent()
                        if (isTwoFingerTransform) {
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                onTransformGesture?.invoke(centroid, panChange, zoomChange)
                            }
                            event.changes.forEach { it.consume() }
                            if (event.changes.none { it.pressed }) break
                            continue
                        }

                        if (event.changes.count { it.pressed } >= 2) {
                            isTwoFingerTransform = true
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                onTransformGesture?.invoke(centroid, panChange, zoomChange)
                            }
                            event.changes.forEach { it.consume() }
                            continue
                        }

                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            upOrCancel = change
                            break
                        }

                        if ((change.position - startPos).getDistance() > touchSlop) {
                            movedPastSlop = true
                        }
                    }

                    if (isTwoFingerTransform || movedPastSlop) {
                        return@awaitEachGesture
                    }

                    // 5. Intentional discrete tap completed!
                    val tapPos = upOrCancel?.position ?: startPos
                    val frame = Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())

                    when (activeTool) {
                        PdfEditTool.Text -> {
                            val hitIdx = marks.indexOfLast { (it is PdfMarkup.TextBoxMarkup || it is PdfMarkup.NoteMarkup) && it.hitTest(tapPos, ocrBlocks, frame) }
                            if (hitIdx >= 0) {
                                val hit = marks[hitIdx]
                                onActiveImageIdChanged(null)
                                onSelectStickyNote(null)
                                onSelectMarkup(hitIdx)
                                val hitId = when (hit) {
                                    is PdfMarkup.TextBoxMarkup -> hit.id
                                    is PdfMarkup.NoteMarkup -> hit.id
                                    else -> null
                                }
                                if (hitId != null) onEditAnnotation(hitId)
                                onShowControls()
                                onInteraction()
                            } else {
                                onSelectMarkup(-1)
                                onActiveImageIdChanged(null)
                                onSelectStickyNote(null)
                                onPlaceText(tapPos)
                                onInteraction()
                            }
                        }
                        PdfEditTool.StickyNote -> {
                            val hitNote = stickyNotes.lastOrNull { note ->
                                val px = note.xNorm * pageW
                                val py = note.yNorm * pageH
                                val pw = if (note.isFolded) with(localDensity) { 38.dp.toPx() } else (note.widthNorm * pageW).coerceAtLeast(with(localDensity) { 160.dp.toPx() })
                                val ph = if (note.isFolded) with(localDensity) { 38.dp.toPx() } else (note.heightNorm * pageH).coerceAtLeast(with(localDensity) { 120.dp.toPx() })
                                Rect(px, py, px + pw, py + ph).contains(tapPos)
                            }
                            if (hitNote != null) {
                                onSelectMarkup(-1)
                                onActiveImageIdChanged(null)
                                onSelectStickyNote(hitNote.id)
                                onShowControls()
                                onInteraction()
                            } else {
                                onSelectMarkup(-1)
                                onActiveImageIdChanged(null)
                                onSelectStickyNote(null)
                                onPlaceStickyNote(tapPos)
                                onInteraction()
                            }
                        }
                        PdfEditTool.Image -> {
                            val hitImg = marks.lastOrNull { it is PdfMarkup.ImageMarkup && it.hitTest(tapPos, ocrBlocks, frame) } as? PdfMarkup.ImageMarkup
                            if (hitImg != null) {
                                onSelectMarkup(-1)
                                onSelectStickyNote(null)
                                onActiveImageIdChanged(hitImg.id)
                                onShowControls()
                                onInteraction()
                            } else {
                                onSelectMarkup(-1)
                                onActiveImageIdChanged(null)
                                onSelectStickyNote(null)
                                onPlaceImage?.invoke(tapPos)
                                onInteraction()
                            }
                        }
                        else -> Unit
                    }
                }
            })
        }

        // ── Transform layer: move + resize the selected shape / text / note ──────
        val selForXf = marks.getOrNull(selectedMarkupIndex)?.takeIf {
            (activeTool == PdfEditTool.None || (activeTool == PdfEditTool.Text && (it is PdfMarkup.TextBoxMarkup || it is PdfMarkup.NoteMarkup))) && it.isTransformable()
        }
        val selXfBounds = selForXf?.movableBounds()
        if (selForXf != null && selXfBounds != null) {
            val boxL = selXfBounds.left - transformPadPx
            val boxT = selXfBounds.top - transformPadPx
            val boxW = selXfBounds.width + transformPadPx * 2
            val boxH = selXfBounds.height + transformPadPx * 2
            val framePadPx = with(localDensity) { 6.dp.toPx() }
            Box(
                Modifier
                    .offset { IntOffset(boxL.roundToInt(), boxT.roundToInt()) }
                    .size(with(localDensity) { boxW.toDp() }, with(localDensity) { boxH.toDp() })
                    .pointerInput(page, selectedMarkupIndex) {
                        var mode = 0 // 1 = move, 2 = resize
                        detectDragGestures(
                            onDragStart = { local ->
                                val cur = marks.getOrNull(selectedMarkupIndex)
                                val bb = cur?.movableBounds()
                                // Convert the box-local touch back to page space.
                                val pPage = Offset(local.x + boxL, local.y + boxT)
                                val brGizmo = bb?.let { Offset(it.right + framePadPx, it.bottom + framePadPx) }
                                val isNearResize = if (bb != null && brGizmo != null) {
                                    val d1 = (pPage.x - brGizmo.x) * (pPage.x - brGizmo.x) + (pPage.y - brGizmo.y) * (pPage.y - brGizmo.y)
                                    val d2 = (pPage.x - bb.bottomRight.x) * (pPage.x - bb.bottomRight.x) + (pPage.y - bb.bottomRight.y) * (pPage.y - bb.bottomRight.y)
                                    d1 <= handleHitRadiusSq || d2 <= handleHitRadiusSq
                                } else false
                                mode = if (bb != null && cur.isResizable() && isNearResize) 2 else 1
                                onInteraction()
                            },
                            onDrag = { ch, drag ->
                                if (mode == 0) return@detectDragGestures
                                ch.consume()
                                val cur = marks.getOrNull(selectedMarkupIndex) ?: return@detectDragGestures
                                val bb = cur.movableBounds() ?: return@detectDragGestures
                                marks[selectedMarkupIndex] = if (mode == 2) cur.resizedBy(drag, bb) else cur.translated(drag)
                                onInteraction()
                            },
                            onDragEnd = { mode = 0 },
                            onDragCancel = { mode = 0 }
                        )
                    }
            )
        }

        // ── Sticky Note Cards Overlay ──────────────────────────────────────────
        if (pageW > 10f && pageH > 10f) {
            stickyNotes.forEach { note ->
                val isSelectedNote = selectedStickyNoteId == note.id
                StickyNoteCard(
                    annotation = note,
                    pageWidthPx = pageW,
                    pageHeightPx = pageH,
                    isToolActive = activeTool == PdfEditTool.StickyNote,
                    isSelected = isSelectedNote,
                    onSelect = {
                        onSelectStickyNote(note.id)
                        onSelectMarkup(-1)
                        onActiveImageIdChanged(null)
                        onInteraction()
                    },
                    onDeselect = {
                        if (selectedStickyNoteId == note.id) {
                            onSelectStickyNote(null)
                        }
                    },
                    onAnnotationChanged = { updated ->
                        onStickyNoteChanged(updated)
                        onInteraction()
                    },
                    onDelete = {
                        onDeleteStickyNote(note.id)
                        if (selectedStickyNoteId == note.id) {
                            onSelectStickyNote(null)
                        }
                        onInteraction()
                    }
                )
            }
        }

        // ── Transform layer: move + resize the selected image / signature ──────
        val activeImg = marks.firstOrNull { it is PdfMarkup.ImageMarkup && it.id == activeImageId } as? PdfMarkup.ImageMarkup
        if (activeImg != null && (activeTool == PdfEditTool.None || activeTool == PdfEditTool.Image || activeTool == PdfEditTool.Signature)) {
            val imgBounds = Rect(
                min(activeImg.start.x, activeImg.end.x),
                min(activeImg.start.y, activeImg.end.y),
                max(activeImg.start.x, activeImg.end.x),
                max(activeImg.start.y, activeImg.end.y)
            )
            val boxL = imgBounds.left - transformPadPx
            val boxT = imgBounds.top - transformPadPx
            val boxW = imgBounds.width + transformPadPx * 2
            val boxH = imgBounds.height + transformPadPx * 2
            Box(
                Modifier
                    .offset { IntOffset(boxL.roundToInt(), boxT.roundToInt()) }
                    .size(with(localDensity) { boxW.toDp() }, with(localDensity) { boxH.toDp() })
                    .pointerInput(page, activeImageId) {
                        var resizing = false
                        detectDragGestures(
                            onDragStart = { local ->
                                val pPage = Offset(local.x + boxL, local.y + boxT)
                                val idx = marks.indexOfLast { it is PdfMarkup.ImageMarkup && it.id == activeImageId }
                                val img = marks.getOrNull(idx) as? PdfMarkup.ImageMarkup
                                if (img != null) {
                                    val br = Offset(max(img.start.x, img.end.x), max(img.start.y, img.end.y))
                                    val dSq = (pPage.x - br.x) * (pPage.x - br.x) + (pPage.y - br.y) * (pPage.y - br.y)
                                    resizing = dSq <= handleHitRadiusSq
                                } else {
                                    resizing = false
                                }
                                onInteraction()
                            },
                            onDrag = { ch, drag ->
                                ch.consume()
                                val idx = marks.indexOfLast { it is PdfMarkup.ImageMarkup && it.id == activeImageId }
                                val img = marks.getOrNull(idx) as? PdfMarkup.ImageMarkup ?: return@detectDragGestures
                                val pw = (pageCanvasSizes[page]?.width ?: size.width.toFloat()).coerceAtLeast(100f)
                                val ph = (pageCanvasSizes[page]?.height ?: size.height.toFloat()).coerceAtLeast(100f)
                                val minSize = with(localDensity) { 32.dp.toPx() }
                                marks[idx] = if (resizing) {
                                    img.copy(end = Offset(
                                        (img.end.x + drag.x).coerceIn(img.start.x + minSize, pw),
                                        (img.end.y + drag.y).coerceIn(img.start.y + minSize, ph)
                                    ))
                                } else {
                                    val iw = img.end.x - img.start.x
                                    val ih = img.end.y - img.start.y
                                    val nx = (img.start.x + drag.x).coerceIn(0f, (pw - iw).coerceAtLeast(0f))
                                    val ny = (img.start.y + drag.y).coerceIn(0f, (ph - ih).coerceAtLeast(0f))
                                    img.copy(start = Offset(nx, ny), end = Offset(nx + iw, ny + ih))
                                }
                                onInteraction()
                            }
                        )
                    }
            )
        }

        val csz = pageCanvasSizes[page]

        // ── Contextual Edit / Delete bar for the selected shape / text / note ──────
        val allowContextualBar = (activeTool == PdfEditTool.None || activeTool == PdfEditTool.Text) && csz != null && csz.width > 0f
        if (allowContextualBar) {
            marks.getOrNull(selectedMarkupIndex)?.takeIf {
                (activeTool == PdfEditTool.None || (it is PdfMarkup.TextBoxMarkup || it is PdfMarkup.NoteMarkup)) && it.isTransformable()
            }?.let { selM ->
                selM.movableBounds()?.let { b ->
                    val density = LocalDensity.current
                    val gapPx = with(density) { 12.dp.toPx() }
                    val barHpx = with(density) { 44.dp.toPx() }
                    val barWpx = with(density) { 132.dp.toPx() }
                    val placeBelow = b.top < barHpx + gapPx
                    val by = (if (placeBelow) b.bottom + gapPx else b.top - barHpx - gapPx)
                        .coerceIn(0f, (csz.height - barHpx).coerceAtLeast(0f))
                    val bx = ((b.left + b.right) / 2f - barWpx / 2f)
                        .coerceIn(0f, (csz.width - barWpx).coerceAtLeast(0f))

                    Row(
                        Modifier
                            .offset { IntOffset(bx.roundToInt(), by.roundToInt()) }
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF1C1F26).copy(0.97f))
                            .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(22.dp))
                            .padding(horizontal = 4.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            "Edit",
                            style = TextStyle(Color.White, 13.sp, FontWeight.SemiBold),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    when (selM) {
                                        is PdfMarkup.TextBoxMarkup -> onEditAnnotation(selM.id)
                                        is PdfMarkup.NoteMarkup    -> onEditAnnotation(selM.id)
                                        else -> onEditShape(selectedMarkupIndex)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        )
                        Box(Modifier.width(1.dp).height(20.dp).background(Color.White.copy(0.14f)))
                        BasicText(
                            "Delete",
                            style = TextStyle(Color(0xFFFF6B6B), 13.sp, FontWeight.SemiBold),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onDeleteMarkup(selectedMarkupIndex) }
                                .padding(horizontal = 16.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            val activeImgForBar = marks.firstOrNull { it is PdfMarkup.ImageMarkup && it.id == activeImageId } as? PdfMarkup.ImageMarkup
            val activeImgIdx = if (activeImgForBar != null) marks.indexOfLast { it is PdfMarkup.ImageMarkup && it.id == activeImageId } else -1
            if (activeImgForBar != null && activeImgIdx >= 0) {
                val b = Rect(
                    min(activeImgForBar.start.x, activeImgForBar.end.x),
                    min(activeImgForBar.start.y, activeImgForBar.end.y),
                    max(activeImgForBar.start.x, activeImgForBar.end.x),
                    max(activeImgForBar.start.y, activeImgForBar.end.y)
                )
                val density = LocalDensity.current
                val gapPx = with(density) { 12.dp.toPx() }
                val barHpx = with(density) { 44.dp.toPx() }
                val barWpx = with(density) { 88.dp.toPx() }
                val placeBelow = b.top < barHpx + gapPx
                val by = (if (placeBelow) b.bottom + gapPx else b.top - barHpx - gapPx)
                    .coerceIn(0f, (csz.height - barHpx).coerceAtLeast(0f))
                val bx = ((b.left + b.right) / 2f - barWpx / 2f)
                    .coerceIn(0f, (csz.width - barWpx).coerceAtLeast(0f))

                Row(
                    Modifier
                        .offset { IntOffset(bx.roundToInt(), by.roundToInt()) }
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF1C1F26).copy(0.97f))
                        .border(1.dp, Color.White.copy(0.14f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "Delete",
                        style = TextStyle(Color(0xFFFF6B6B), 13.sp, FontWeight.SemiBold),
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                onDeleteMarkup(activeImgIdx)
                                onActiveImageIdChanged(null)
                            }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }

            // Text highlights, underlines, and strike-throughs are precise OCR ranges, not
            // transformable shapes. When one is tapped, expose the same clear destructive action
            // used by the other professional markup tools.
            marks.getOrNull(selectedMarkupIndex)
                ?.takeIf { it is PdfMarkup.TextBlockHighlightMarkup || it is PdfMarkup.TextBlockLineMarkup }
                ?.textMarkupRangeRect(ocrBlocks, Rect(0f, 0f, csz.width, csz.height))
                ?.let { rangeRect ->
                    val selected = marks[selectedMarkupIndex]
                    val anchorRect = if (selected is PdfMarkup.TextBlockLineMarkup) {
                        val y = selected.textMarkupLineY(rangeRect) ?: rangeRect.bottom
                        Rect(rangeRect.left, y - 4f, rangeRect.right, y + 4f)
                    } else rangeRect
                    val density = LocalDensity.current
                    val gapPx = with(density) { 10.dp.toPx() }
                    val barHpx = with(density) { 44.dp.toPx() }
                    val barWpx = with(density) { 176.dp.toPx() }
                    val placeBelow = anchorRect.top < barHpx + gapPx
                    val by = (if (placeBelow) anchorRect.bottom + gapPx else anchorRect.top - barHpx - gapPx)
                        .coerceIn(0f, (csz.height - barHpx).coerceAtLeast(0f))
                    val bx = ((anchorRect.left + anchorRect.right) / 2f - barWpx / 2f)
                        .coerceIn(0f, (csz.width - barWpx).coerceAtLeast(0f))

                    Row(
                        Modifier
                            .offset { IntOffset(bx.roundToInt(), by.roundToInt()) }
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF232629))
                            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Same Edit affordance as shapes/text/notes — opens the shared recolor
                        // popover (ShapeEditorPopup handles highlight/underline/strike too).
                        BasicText(
                            "Edit",
                            style = TextStyle(Color(0xFFECECEC), 13.sp, FontWeight.Medium),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onEditShape(selectedMarkupIndex) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                        if (onTranslateMarkup != null && (selected is PdfMarkup.TextBlockHighlightMarkup || selected is PdfMarkup.TextBlockLineMarkup)) {
                            val normRect = if (csz.width > 0f && csz.height > 0f) {
                                Rect(
                                    (anchorRect.left / csz.width).coerceIn(0f, 1f),
                                    (anchorRect.top / csz.height).coerceIn(0f, 1f),
                                    (anchorRect.right / csz.width).coerceIn(0f, 1f),
                                    (anchorRect.bottom / csz.height).coerceIn(0f, 1f)
                                )
                            } else Rect.Zero
                            BasicText(
                                "Translate",
                                style = TextStyle(Color(0xFF64D2FF), 13.sp, FontWeight.Medium),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onTranslateMarkup(selectedMarkupIndex, normRect) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                        BasicText(
                            "Delete",
                            style = TextStyle(Color(0xFFEF5350), 13.sp, FontWeight.Medium),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onDeleteMarkup(selectedMarkupIndex) }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
        }
    }
}

/**
 * Renders high-contrast, scalable selection boundary handles (visual anchors and action resize handle)
 * for placed annotations (Images, Signatures, Text Boxes, Shapes).
 *
 * Sizing:
 * - Corner scale / anchor circles: 17dp diameter (8.5dp radius)
 * - Action / resize anchor handle: 23dp diameter (11.5dp radius)
 * Styling:
 * - Pure white interior fill
 * - 2dp - 2.5dp high-contrast accent stroke
 * - Outer dark rim + subtle drop shadow for crisp visibility on both pure white paper and dark PDF backgrounds
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSelectionGizmo(
    bounds: Rect,
    isResizable: Boolean,
    accentColor: Color = Color(0xFF0A84FF)
) {
    val cornerRadius = 8.5.dp.toPx()
    val resizeRadius = 11.5.dp.toPx()

    val cornerStrokeWidth = 2.dp.toPx()
    val resizeStrokeWidth = 2.5.dp.toPx()
    val frameStrokeWidth = 2.dp.toPx()
    val frameCornerRadius = 8.dp.toPx()

    val shadowDropColor = Color(0x38000000)
    val outerRimColor = Color(0xFF1A1A1E)

    // Bounding selection frame: outer contrast shadow + accent frame line
    drawRoundRect(
        color = shadowDropColor,
        topLeft = bounds.topLeft,
        size = bounds.size,
        cornerRadius = CornerRadius(frameCornerRadius, frameCornerRadius),
        style = Stroke(frameStrokeWidth + 1.5.dp.toPx())
    )
    drawRoundRect(
        color = accentColor,
        topLeft = bounds.topLeft,
        size = bounds.size,
        cornerRadius = CornerRadius(frameCornerRadius, frameCornerRadius),
        style = Stroke(frameStrokeWidth)
    )

    // Passive corner anchor circles (Top-Left, Top-Right, Bottom-Left)
    val corners = listOf(
        bounds.topLeft,
        Offset(bounds.right, bounds.top),
        Offset(bounds.left, bounds.bottom)
    )
    corners.forEach { c ->
        drawCircle(shadowDropColor, cornerRadius + 1.5.dp.toPx(), c)
        drawCircle(outerRimColor, cornerRadius + 0.5.dp.toPx(), c, style = Stroke(1.dp.toPx()))
        drawCircle(Color.White, cornerRadius, c)
        drawCircle(accentColor, cornerRadius, c, style = Stroke(cornerStrokeWidth))
    }

    // Prominent Bottom-Right Action / Resize Anchor
    if (isResizable) {
        val br = Offset(bounds.right, bounds.bottom)
        drawCircle(shadowDropColor, resizeRadius + 2.dp.toPx(), br)
        drawCircle(outerRimColor, resizeRadius + 0.5.dp.toPx(), br, style = Stroke(1.2.dp.toPx()))
        drawCircle(Color.White, resizeRadius, br)
        drawCircle(accentColor, resizeRadius, br, style = Stroke(resizeStrokeWidth))

        // Diagonal resize grip glyph with rounded stroke caps
        val glyphSpan = 4.2.dp.toPx()
        val glyphSep = 1.2.dp.toPx()
        val glyphStroke = 2.2.dp.toPx()
        drawLine(
            accentColor,
            Offset(br.x - glyphSpan, br.y + glyphSep),
            Offset(br.x + glyphSep, br.y - glyphSpan),
            strokeWidth = glyphStroke,
            cap = StrokeCap.Round
        )
        drawLine(
            accentColor,
            Offset(br.x - glyphSep, br.y + glyphSpan),
            Offset(br.x + glyphSpan, br.y - glyphSep),
            strokeWidth = glyphStroke,
            cap = StrokeCap.Round
        )
    }
}
