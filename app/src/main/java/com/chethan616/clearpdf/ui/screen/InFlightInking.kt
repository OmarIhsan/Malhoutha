package com.chethan616.clearpdf.ui.screen

import android.view.MotionEvent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.input.motionprediction.MotionEventPredictor
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance state holder for in-flight inking and shape drawing.
 *
 * Coordinates are held in an in-place mutable buffer to eliminate heap allocations
 * during 240Hz stylus movement. Visual updates are triggered via [drawInvalidationTick],
 * which is read exclusively inside the draw phase ([drawBehind]), completely bypassing
 * Compose recomposition and relayout passes of the page tree.
 *
 * Ground truth coordinates from the digitizer are stored in [points].
 * Sub-frame predicted coordinates from [MotionEventPredictor] are stored in [predictedPoints]
 * to extend the stroke tip directly under the moving stylus nib, and are discarded upon
 * stroke completion so only ground truth points are persisted into document markups.
 */
internal class InFlightInkState {
    val points = ArrayList<Offset>(1024)
    val predictedPoints = ArrayList<Offset>(16)
    val path = Path()

    var color: Color = Color.Black
    var strokeWidth: Float = 4f
    var isHighlight: Boolean = false
    var isActive: Boolean = false

    // Geometric shape drafting
    var shapeTool: PdfEditTool? = null
    var shapeStart: Offset? = null
    var shapeEnd: Offset? = null

    // Draw invalidation trigger: read ONLY in drawBehind to isolate invalidation to HWUI draw pass.
    var drawInvalidationTick by mutableIntStateOf(0)
        private set

    fun startFreehand(start: Offset, color: Color, width: Float, highlight: Boolean) {
        points.clear()
        predictedPoints.clear()
        points.add(start)
        this.color = color
        this.strokeWidth = width
        this.isHighlight = highlight
        this.shapeTool = null
        this.shapeStart = null
        this.shapeEnd = null
        this.isActive = true
        rebuildPath()
        drawInvalidationTick++
    }

    fun addPoints(pts: List<Offset>) {
        if (!isActive || pts.isEmpty()) return
        var added = false
        for (i in 0 until pts.size) {
            val pt = pts[i]
            val last = points.lastOrNull()
            if (last == null || pt.x != last.x || pt.y != last.y) {
                points.add(pt)
                added = true
            }
        }
        if (added) {
            rebuildPath()
            drawInvalidationTick++
        }
    }

    fun addPoint(pt: Offset) {
        if (!isActive) return
        val last = points.lastOrNull()
        if (last != null && pt.x == last.x && pt.y == last.y) return
        points.add(pt)
        rebuildPath()
        drawInvalidationTick++
    }

    fun setPredictedPoints(pts: List<Offset>) {
        if (!isActive) return
        predictedPoints.clear()
        if (pts.isNotEmpty()) {
            predictedPoints.addAll(pts)
        }
        rebuildPath()
        drawInvalidationTick++
    }

    fun clearPredictedPoints() {
        if (predictedPoints.isNotEmpty()) {
            predictedPoints.clear()
            rebuildPath()
            drawInvalidationTick++
        }
    }

    private fun rebuildPath() {
        path.reset()
        val totalCount = points.size + predictedPoints.size
        if (totalCount == 0) return

        val allPoints: (Int) -> Offset = { index ->
            if (index < points.size) points[index] else predictedPoints[index - points.size]
        }

        path.moveTo(allPoints(0).x, allPoints(0).y)
        if (totalCount == 1) return
        if (totalCount == 2) {
            val p1 = allPoints(1)
            path.lineTo(p1.x, p1.y)
            return
        }
        for (i in 1 until totalCount - 1) {
            val p0 = allPoints(i)
            val p1 = allPoints(i + 1)
            val midX = (p0.x + p1.x) / 2f
            val midY = (p0.y + p1.y) / 2f
            path.quadraticTo(p0.x, p0.y, midX, midY)
        }
        val last = allPoints(totalCount - 1)
        path.lineTo(last.x, last.y)
    }

    fun finishFreehand(): List<Offset> {
        if (!isActive) return emptyList()
        isActive = false
        predictedPoints.clear()
        val result = if (points.size == 1) {
            // For a single-point tap (dotting an 'i' or writing a period / accent),
            // synthesize a micro-segment so StrokeCap.Round renders a clean round dot.
            val p = points[0]
            listOf(p, Offset(p.x + 0.1f, p.y + 0.1f))
        } else {
            points.toList()
        }
        points.clear()
        path.reset()
        drawInvalidationTick++
        return result
    }

    fun startShape(tool: PdfEditTool, start: Offset, color: Color, width: Float, highlight: Boolean = false) {
        this.shapeTool = tool
        this.shapeStart = start
        this.shapeEnd = start
        this.color = color
        this.strokeWidth = width
        this.isHighlight = highlight
        this.isActive = true
        this.points.clear()
        this.predictedPoints.clear()
        this.path.reset()
        drawInvalidationTick++
    }

    fun updateShapeEnd(end: Offset) {
        if (!isActive) return
        this.shapeEnd = end
        drawInvalidationTick++
    }

    fun finishShape(): PdfMarkup? {
        if (!isActive) return null
        val tool = shapeTool
        val s = shapeStart
        val e = shapeEnd
        val isHl = isHighlight
        cancel()
        if (tool == null || s == null || e == null) return null
        val alpha = if (isHl) 0.38f else 1f
        val sw = if (isHl) strokeWidth * 2.8f else strokeWidth
        val minX = min(s.x, e.x)
        val minY = min(s.y, e.y)
        val maxX = max(s.x, e.x)
        val maxY = max(s.y, e.y)
        return when (tool) {
            PdfEditTool.Rect    -> PdfMarkup.RectMarkup(Offset(minX, minY), Offset(maxX, maxY), color, alpha, false, width = sw, isHighlight = isHl)
            PdfEditTool.Ellipse -> PdfMarkup.OvalMarkup(Offset(minX, minY), Offset(maxX, maxY), color, alpha, false, width = sw, isHighlight = isHl)
            PdfEditTool.Line    -> PdfMarkup.LineMarkup(s, e, color, sw, alpha, false, isHighlight = isHl)
            PdfEditTool.Arrow   -> PdfMarkup.LineMarkup(s, e, color, sw, alpha, true, isHighlight = isHl)
            PdfEditTool.Triangle -> {
                val midX = s.x + (e.x - s.x) / 2f
                PdfMarkup.StrokeMarkup(
                    points = listOf(Offset(midX, s.y), Offset(s.x, e.y), Offset(e.x, e.y)),
                    color = color,
                    width = sw,
                    alpha = alpha,
                    isClosed = true,
                    isHighlight = isHl
                )
            }
            else -> null
        }
    }

    fun cancel() {
        isActive = false
        points.clear()
        predictedPoints.clear()
        path.reset()
        shapeTool = null
        shapeStart = null
        shapeEnd = null
        drawInvalidationTick++
    }
}

/**
 * Extracts all batched sub-frame historical coordinates from hardware digitizer events.
 *
 * Modern Android active styluses (e.g. Xiaomi Smart Pen on Xiaomi Pad 6) sample at 240Hz,
 * while display refreshes at 60Hz/120Hz/144Hz. Android batches the intermediate samples into
 * the [MotionEvent]. This function retrieves all intermediate points so no corner-cutting
 * occurs and high-frequency stylus strokes remain completely fluid.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun extractDigitizerBatchPoints(
    change: PointerInputChange,
    event: PointerEvent
): List<Offset> {
    // 1. Compose's PointerInputChange historical records (already transformed to local space)
    val composeHist = change.historical
    if (composeHist.isNotEmpty()) {
        val list = ArrayList<Offset>(composeHist.size)
        for (i in 0 until composeHist.size) {
            list.add(composeHist[i].position)
        }
        return list
    }

    // 2. Direct Android MotionEvent fallback if available
    val motionEvent = event.motionEvent
    if (motionEvent != null && motionEvent.historySize > 0) {
        val pIdx = if (motionEvent.pointerCount > 0) {
            val id = change.id.value.toInt()
            val found = motionEvent.findPointerIndex(id)
            if (found >= 0) found else 0
        } else 0

        if (pIdx in 0 until motionEvent.pointerCount) {
            val curMotionX = motionEvent.getX(pIdx)
            val curMotionY = motionEvent.getY(pIdx)
            val dx = change.position.x - curMotionX
            val dy = change.position.y - curMotionY
            val historySize = motionEvent.historySize
            val list = ArrayList<Offset>(historySize)
            for (h in 0 until historySize) {
                list.add(Offset(motionEvent.getHistoricalX(pIdx, h) + dx, motionEvent.getHistoricalY(pIdx, h) + dy))
            }
            return list
        }
    }

    return emptyList()
}

/**
 * Coordinate node for ephemeral laser pointer strokes.
 */
internal data class LaserPoint(
    val x: Float,
    val y: Float
)

/**
 * Frozen completed laser vector stroke awaiting time-decay dissolution via CanvasFrontBufferedRenderer.
 */
internal data class CompletedLaserStroke(
    val path: android.graphics.Path,
    val birthTime: Long = android.os.SystemClock.uptimeMillis()
)

internal typealias LaserStroke = CompletedLaserStroke

/**
 * Smoothly constructs a unified Skia Path through the provided laser coordinates
 * using midpoint quadratic Bezier interpolation.
 */
internal fun rebuildLaserPath(points: List<LaserPoint>, path: android.graphics.Path) {
    path.reset()
    if (points.isEmpty()) return
    val p0 = points[0]
    path.moveTo(p0.x, p0.y)
    if (points.size == 1) {
        // Micro-offset allows Paint.Cap.ROUND to stamp a circular dot
        path.lineTo(p0.x + 0.1f, p0.y + 0.1f)
        return
    }
    if (points.size == 2) {
        val p1 = points[1]
        path.lineTo(p1.x, p1.y)
        return
    }
    for (i in 1 until points.size - 1) {
        val pt0 = points[i]
        val pt1 = points[i + 1]
        val midX = (pt0.x + pt1.x) / 2f
        val midY = (pt0.y + pt1.y) / 2f
        path.quadTo(pt0.x, pt0.y, midX, midY)
    }
    val last = points.last()
    path.lineTo(last.x, last.y)
}

/**
 * Extracts historical batch points from the 240Hz active digitizer for the real-time laser pointer ribbon.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal fun extractLaserBatchPoints(
    change: PointerInputChange,
    event: PointerEvent
): List<LaserPoint> {
    val composeHist = change.historical
    if (composeHist.isNotEmpty()) {
        val list = ArrayList<LaserPoint>(composeHist.size)
        for (i in 0 until composeHist.size) {
            val h = composeHist[i]
            list.add(LaserPoint(h.position.x, h.position.y))
        }
        return list
    }

    val motionEvent = event.motionEvent
    if (motionEvent != null && motionEvent.historySize > 0) {
        val pIdx = if (motionEvent.pointerCount > 0) {
            val id = change.id.value.toInt()
            val found = motionEvent.findPointerIndex(id)
            if (found >= 0) found else 0
        } else 0

        if (pIdx in 0 until motionEvent.pointerCount) {
            val curMotionX = motionEvent.getX(pIdx)
            val curMotionY = motionEvent.getY(pIdx)
            val dx = change.position.x - curMotionX
            val dy = change.position.y - curMotionY
            val historySize = motionEvent.historySize
            val list = ArrayList<LaserPoint>(historySize)
            for (h in 0 until historySize) {
                list.add(
                    LaserPoint(
                        motionEvent.getHistoricalX(pIdx, h) + dx,
                        motionEvent.getHistoricalY(pIdx, h) + dy
                    )
                )
            }
            return list
        }
    }

    return emptyList()
}

/**
 * Predicts sub-frame stylus tip coordinates using Google's [MotionEventPredictor].
 *
 * Predicts the future physical coordinate of the stylus tip for the upcoming display scanout /
 * vsync swap, closing the remaining 15-25ms air gap between physical nib and rendered ink.
 * Coordinates are mapped to local page space using the offset delta and validated for smoothness.
 */
internal fun predictSubFramePoints(
    predictor: MotionEventPredictor,
    change: PointerInputChange,
    motionEvent: MotionEvent
): List<Offset> {
    val predictedEvent = runCatching { predictor.predict() }.getOrNull() ?: return emptyList()
    return try {
        val pIdx = if (motionEvent.pointerCount > 0) {
            val id = change.id.value.toInt()
            val found = motionEvent.findPointerIndex(id)
            if (found >= 0) found else 0
        } else 0

        val curMotionX = motionEvent.getX(pIdx)
        val curMotionY = motionEvent.getY(pIdx)
        val deltaX = change.position.x - curMotionX
        val deltaY = change.position.y - curMotionY

        val predPIdx = if (predictedEvent.pointerCount > 0) {
            val id = change.id.value.toInt()
            val found = predictedEvent.findPointerIndex(id)
            if (found >= 0) found else 0
        } else 0

        if (predPIdx in 0 until predictedEvent.pointerCount) {
            val historySize = predictedEvent.historySize
            val list = ArrayList<Offset>(historySize + 1)
            var lastPt = change.position
            for (h in 0 until historySize) {
                val pt = Offset(
                    predictedEvent.getHistoricalX(predPIdx, h) + deltaX,
                    predictedEvent.getHistoricalY(predPIdx, h) + deltaY
                )
                val dx = pt.x - lastPt.x
                val dy = pt.y - lastPt.y
                val distSq = dx * dx + dy * dy
                // Sanity check: keep predictions within smooth velocity envelope (< 120px per sub-frame)
                if (distSq in 1f..14400f) {
                    list.add(pt)
                    lastPt = pt
                }
            }
            val finalPt = Offset(
                predictedEvent.getX(predPIdx) + deltaX,
                predictedEvent.getY(predPIdx) + deltaY
            )
            val fdx = finalPt.x - lastPt.x
            val fdy = finalPt.y - lastPt.y
            val fDistSq = fdx * fdx + fdy * fdy
            if (fDistSq in 1f..14400f) {
                list.add(finalPt)
            }
            list
        } else {
            emptyList()
        }
    } finally {
        predictedEvent.recycle()
    }
}

/**
 * Dedicated, low-latency in-flight inking overlay composable with motion prediction.
 *
 * This overlay renders active freehand strokes and shape drafts in real-time.
 * Because all points are drawn via [drawBehind] observing [InFlightInkState.drawInvalidationTick],
 * visual invalidation is isolated to the draw phase of this overlay node, bypassing Compose
 * recomposition and relayout passes of the page tree entirely.
 *
 * Sub-frame prediction from [MotionEventPredictor] extends the in-flight stroke tip directly
 * under the active stylus nib, eliminating visual dragging during rapid cursive writing.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun InFlightInkingOverlay(
    modifier: Modifier = Modifier,
    state: InFlightInkState,
    page: Int,
    activeTool: PdfEditTool,
    currentColor: Color,
    currentStrokeWidth: Float,
    onInteraction: () -> Unit,
    onStrokeCommitted: (PdfMarkup) -> Unit
) {
    val view = LocalView.current
    val predictor = remember(view) {
        runCatching { MotionEventPredictor.newInstance(view) }.getOrNull()
    }

    Box(
        modifier = modifier
            .drawBehind {
                if (state.drawInvalidationTick > 0 && state.isActive) {
                    val sTool = state.shapeTool
                    if (sTool != null) {
                        val s = state.shapeStart
                        val e = state.shapeEnd
                        if (s != null && e != null) {
                            val pr = Rect(min(s.x, e.x), min(s.y, e.y), max(s.x, e.x), max(s.y, e.y))
                            val isHl = state.isHighlight
                            val sw = if (isHl) state.strokeWidth * 2.8f else state.strokeWidth
                            val alpha = if (isHl) 0.38f else 1f
                            val shapeColor = state.color.copy(alpha = alpha)
                            when (sTool) {
                                PdfEditTool.Rect    -> drawRect(shapeColor, pr.topLeft, pr.size, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                PdfEditTool.Ellipse -> drawOval(shapeColor, pr.topLeft, pr.size, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                PdfEditTool.Line    -> drawLine(shapeColor, s, e, sw, cap = StrokeCap.Round)
                                PdfEditTool.Arrow   -> drawArrow(s, e, shapeColor, sw)
                                PdfEditTool.Triangle -> {
                                    val midX = s.x + (e.x - s.x) / 2f
                                    val tri = Path().apply {
                                        moveTo(midX, s.y)
                                        lineTo(s.x, e.y)
                                        lineTo(e.x, e.y)
                                        close()
                                    }
                                    drawPath(tri, shapeColor, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                }
                                else -> Unit
                            }
                        }
                    } else if (state.points.isNotEmpty()) {
                        val isHl = state.isHighlight
                        val sw = if (isHl) state.strokeWidth * 3.5f else state.strokeWidth
                        val alpha = if (isHl) 0.32f else 0.95f
                        drawPath(
                            path = state.path,
                            color = state.color.copy(alpha = alpha),
                            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
            .pointerInput(page, activeTool, currentColor, currentStrokeWidth) {
                val isFreehand = activeTool == PdfEditTool.Draw || activeTool == PdfEditTool.Highlight
                val isShape = activeTool in setOf(
                    PdfEditTool.Rect, PdfEditTool.Ellipse, PdfEditTool.Line, PdfEditTool.Arrow
                )
                if (!isFreehand && !isShape) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    onInteraction()

                    if (isFreehand) {
                        state.startFreehand(
                            start = down.position,
                            color = currentColor,
                            width = currentStrokeWidth,
                            highlight = activeTool == PdfEditTool.Highlight
                        )
                    } else {
                        state.startShape(
                            tool = activeTool,
                            start = down.position,
                            color = currentColor,
                            width = currentStrokeWidth
                        )
                    }

                    val pointerId = down.id

                    while (true) {
                        val event = awaitPointerEvent()
                        val motionEvent = event.motionEvent
                        val change = event.changes.firstOrNull { it.id == pointerId }
                        if (change == null || !change.pressed) {
                            // Touch up or gesture cancelled: record up into predictor
                            motionEvent?.let { runCatching { predictor?.record(it) } }
                            state.clearPredictedPoints()
                            break
                        }
                        change.consume()

                        if (isFreehand) {
                            if (motionEvent != null) {
                                runCatching { predictor?.record(motionEvent) }
                            }

                            // 1. Digitizer point batching via getHistorical* (240Hz hardware samples)
                            val batched = extractDigitizerBatchPoints(change, event)
                            if (batched.isNotEmpty()) {
                                state.addPoints(batched)
                            }
                            // 2. Latest touch sample
                            state.addPoint(change.position)

                            // 3. Sub-frame motion prediction (extends in-flight tip directly under stylus nib)
                            if (predictor != null && motionEvent != null) {
                                val predicted = predictSubFramePoints(predictor, change, motionEvent)
                                state.setPredictedPoints(predicted)
                            } else {
                                state.clearPredictedPoints()
                            }
                        } else {
                            state.updateShapeEnd(change.position)
                        }
                    }

                    // Commit markup on gesture completion (UP)
                    if (isFreehand) {
                        val strokePoints = state.finishFreehand()
                        if (strokePoints.size > 1) {
                            val isHl = activeTool == PdfEditTool.Highlight
                            val w = if (isHl) currentStrokeWidth * 3.5f else currentStrokeWidth
                            val a = if (isHl) 0.32f else 0.95f
                            onStrokeCommitted(PdfMarkup.StrokeMarkup(strokePoints, currentColor, w, a))
                        }
                    } else {
                        val shape = state.finishShape()
                        if (shape != null) {
                            onStrokeCommitted(shape)
                        }
                    }
                }
            }
    )
}
