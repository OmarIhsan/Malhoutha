package com.chethan616.clearpdf.ui.screen

import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.graphics.lowlatency.CanvasFrontBufferedRenderer
import androidx.input.motionprediction.MotionEventPredictor
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Data segment passed to the hardware front-buffered renderer callback.
 */
internal data class InkingRenderSegment(
    val path: android.graphics.Path? = null,
    val shapeTool: PdfEditTool? = null,
    val shapeStart: Offset? = null,
    val shapeEnd: Offset? = null,
    val color: Int = 0,
    val strokeWidth: Float = 0f,
    val isClear: Boolean = false
)

/**
 * Low-latency hardware front-buffered inking surface using [CanvasFrontBufferedRenderer].
 *
 * This composable mounts a translucent [SurfaceView] with front-buffered hardware rendering.
 * During active freehand stylus strokes and shape drafts, ink is drawn directly to the display
 * scanout front-buffer, bypassing SurfaceFlinger's multi-buffering pipeline (<4ms latency on Xiaomi Pad 6).
 *
 * On stroke completion ([MotionEvent.ACTION_UP]), a two-phase handoff is executed:
 * 1. The front-buffer is committed to the multi-buffered layer to freeze the exact ink image on glass.
 * 2. The front buffer is NOT cleared synchronously; instead, the completed markup is dispatched via [onStrokeCommitted].
 * 3. The provided [onCommittedDrawn] callback is triggered ONLY AFTER Compose's persistent document canvas
 *    has drawn the markup into its display list, completely eliminating any "refresh" blink or empty-frame window.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun HardwareInkingSurface(
    modifier: Modifier = Modifier,
    state: InFlightInkState,
    page: Int,
    activeTool: PdfEditTool,
    currentColor: Color,
    currentStrokeWidth: Float,
    onInteraction: () -> Unit,
    onStrokeCommitted: (PdfMarkup, onCommittedDrawn: () -> Unit) -> Unit
) {
    val hostView = LocalView.current
    val predictor = remember(hostView) {
        runCatching { MotionEventPredictor.newInstance(hostView) }.getOrNull()
    }

    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    // Android graphics path for Skia front-buffer drawing
    val frontBufferPath = remember { android.graphics.Path() }

    val rendererRef = remember { arrayOfNulls<CanvasFrontBufferedRenderer<InkingRenderSegment>>(1) }

    fun updateFrontBufferPath(points: List<Offset>, predicted: List<Offset>) {
        frontBufferPath.reset()
        val totalCount = points.size + predicted.size
        if (totalCount == 0) return

        val getPt: (Int) -> Offset = { idx ->
            if (idx < points.size) points[idx] else predicted[idx - points.size]
        }

        val p0 = getPt(0)
        frontBufferPath.moveTo(p0.x, p0.y)
        if (totalCount == 1) {
            frontBufferPath.lineTo(p0.x + 0.1f, p0.y + 0.1f)
            return
        }
        if (totalCount == 2) {
            val p1 = getPt(1)
            frontBufferPath.lineTo(p1.x, p1.y)
            return
        }
        for (i in 1 until totalCount - 1) {
            val pt0 = getPt(i)
            val pt1 = getPt(i + 1)
            val midX = (pt0.x + pt1.x) / 2f
            val midY = (pt0.y + pt1.y) / 2f
            frontBufferPath.quadTo(pt0.x, pt0.y, midX, midY)
        }
        val pLast = getPt(totalCount - 1)
        frontBufferPath.lineTo(pLast.x, pLast.y)
    }

    fun drawAndroidArrow(canvas: Canvas, start: Offset, end: Offset, arrowPaint: Paint) {
        canvas.drawLine(start.x, start.y, end.x, end.y, arrowPaint)
        val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
        val arrowLen = (arrowPaint.strokeWidth * 3.5f).coerceAtLeast(18f)
        val angle1 = angle + Math.PI - (Math.PI / 6)
        val angle2 = angle + Math.PI + (Math.PI / 6)
        val p1x = (end.x + arrowLen * cos(angle1)).toFloat()
        val p1y = (end.y + arrowLen * sin(angle1)).toFloat()
        val p2x = (end.x + arrowLen * cos(angle2)).toFloat()
        val p2y = (end.y + arrowLen * sin(angle2)).toFloat()
        val arrowPath = android.graphics.Path().apply {
            moveTo(end.x, end.y)
            lineTo(p1x, p1y)
            lineTo(p2x, p2y)
            close()
        }
        val fillPaint = Paint(arrowPaint).apply { style = Paint.Style.FILL }
        canvas.drawPath(arrowPath, fillPaint)
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.matchParentSize(),
            factory = { ctx ->
                SurfaceView(ctx).apply {
                    setZOrderOnTop(true)
                    holder.setFormat(PixelFormat.TRANSLUCENT)

                    val callback = object : CanvasFrontBufferedRenderer.Callback<InkingRenderSegment> {
                        override fun onDrawFrontBufferedLayer(
                            canvas: Canvas,
                            bufferWidth: Int,
                            bufferHeight: Int,
                            param: InkingRenderSegment
                        ) {
                            // Clear front buffer to avoid ghosting from previous predicted tip
                            canvas.drawColor(AndroidColor.TRANSPARENT, PorterDuff.Mode.CLEAR)
                            if (param.isClear) return

                            paint.color = param.color
                            paint.strokeWidth = param.strokeWidth

                            val sTool = param.shapeTool
                            if (sTool != null) {
                                val s = param.shapeStart
                                val e = param.shapeEnd
                                if (s != null && e != null) {
                                    val left = min(s.x, e.x)
                                    val top = min(s.y, e.y)
                                    val right = max(s.x, e.x)
                                    val bottom = max(s.y, e.y)
                                    when (sTool) {
                                        PdfEditTool.Rect    -> canvas.drawRect(left, top, right, bottom, paint)
                                        PdfEditTool.Ellipse -> canvas.drawOval(left, top, right, bottom, paint)
                                        PdfEditTool.Line    -> canvas.drawLine(s.x, s.y, e.x, e.y, paint)
                                        PdfEditTool.Arrow   -> drawAndroidArrow(canvas, s, e, paint)
                                        else -> Unit
                                    }
                                }
                            } else if (param.path != null) {
                                canvas.drawPath(param.path, paint)
                            }
                        }

                        override fun onDrawMultiBufferedLayer(
                            canvas: Canvas,
                            bufferWidth: Int,
                            bufferHeight: Int,
                            params: Collection<InkingRenderSegment>
                        ) {
                            // When committed, draw the completed stroke into the multi-buffered layer
                            // so it remains frozen on glass until Compose renders the persistent mark.
                            canvas.drawColor(AndroidColor.TRANSPARENT, PorterDuff.Mode.CLEAR)
                            for (param in params) {
                                if (param.isClear) continue
                                paint.color = param.color
                                paint.strokeWidth = param.strokeWidth

                                val sTool = param.shapeTool
                                if (sTool != null) {
                                    val s = param.shapeStart
                                    val e = param.shapeEnd
                                    if (s != null && e != null) {
                                        val left = min(s.x, e.x)
                                        val top = min(s.y, e.y)
                                        val right = max(s.x, e.x)
                                        val bottom = max(s.y, e.y)
                                        when (sTool) {
                                            PdfEditTool.Rect    -> canvas.drawRect(left, top, right, bottom, paint)
                                            PdfEditTool.Ellipse -> canvas.drawOval(left, top, right, bottom, paint)
                                            PdfEditTool.Line    -> canvas.drawLine(s.x, s.y, e.x, e.y, paint)
                                            PdfEditTool.Arrow   -> drawAndroidArrow(canvas, s, e, paint)
                                            else -> Unit
                                        }
                                    }
                                } else if (param.path != null) {
                                    canvas.drawPath(param.path, paint)
                                }
                            }
                        }
                    }

                    val renderer = CanvasFrontBufferedRenderer(this, callback)
                    rendererRef[0] = renderer
                }
            }
        )

        // Low-latency gesture detection driving the front-buffer surface
        Box(
            modifier = Modifier
                .matchParentSize()
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

                        // Clear any previous residual stroke before starting the new one
                        rendererRef[0]?.clear()

                        val isHl = activeTool == PdfEditTool.Highlight
                        val alpha = if (isHl) 0.32f else 0.95f
                        val sw = if (isHl) currentStrokeWidth * 3.5f else currentStrokeWidth
                        val renderColor = currentColor.copy(alpha = alpha).toArgb()

                        if (isFreehand) {
                            state.startFreehand(
                                start = down.position,
                                color = currentColor,
                                width = currentStrokeWidth,
                                highlight = activeTool == PdfEditTool.Highlight
                            )
                            updateFrontBufferPath(state.points, emptyList())
                            rendererRef[0]?.renderFrontBufferedLayer(
                                InkingRenderSegment(
                                    path = frontBufferPath,
                                    color = renderColor,
                                    strokeWidth = sw
                                )
                            )
                        } else {
                            state.startShape(
                                tool = activeTool,
                                start = down.position,
                                color = currentColor,
                                width = currentStrokeWidth
                            )
                            val shapeColor = when (activeTool) {
                                PdfEditTool.Rect    -> AndroidColor.parseColor("#42A5F5")
                                PdfEditTool.Ellipse -> AndroidColor.parseColor("#26A69A")
                                PdfEditTool.Line    -> AndroidColor.parseColor("#66BB6A")
                                PdfEditTool.Arrow   -> AndroidColor.parseColor("#EF5350")
                                else -> renderColor
                            }
                            rendererRef[0]?.renderFrontBufferedLayer(
                                InkingRenderSegment(
                                    shapeTool = activeTool,
                                    shapeStart = down.position,
                                    shapeEnd = down.position,
                                    color = shapeColor,
                                    strokeWidth = if (activeTool == PdfEditTool.Line || activeTool == PdfEditTool.Arrow) 4f else 3f
                                )
                            )
                        }

                        val pointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val motionEvent = event.motionEvent
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null || !change.pressed) {
                                motionEvent?.let { runCatching { predictor?.record(it) } }
                                state.clearPredictedPoints()
                                break
                            }
                            change.consume()

                            if (isFreehand) {
                                if (motionEvent != null) {
                                    runCatching { predictor?.record(motionEvent) }
                                }

                                // Batch hardware 240Hz digitizer points
                                val batched = extractDigitizerBatchPoints(change, event)
                                if (batched.isNotEmpty()) {
                                    state.addPoints(batched)
                                }
                                state.addPoint(change.position)

                                // Sub-frame motion prediction
                                val predicted = if (predictor != null && motionEvent != null) {
                                    predictSubFramePoints(predictor, change, motionEvent)
                                } else emptyList()

                                state.setPredictedPoints(predicted)

                                // Update Skia path and render directly to hardware front-buffer
                                updateFrontBufferPath(state.points, predicted)
                                rendererRef[0]?.renderFrontBufferedLayer(
                                    InkingRenderSegment(
                                        path = frontBufferPath,
                                        color = renderColor,
                                        strokeWidth = sw
                                    )
                                )
                            } else {
                                state.updateShapeEnd(change.position)
                                val shapeColor = when (activeTool) {
                                    PdfEditTool.Rect    -> AndroidColor.parseColor("#42A5F5")
                                    PdfEditTool.Ellipse -> AndroidColor.parseColor("#26A69A")
                                    PdfEditTool.Line    -> AndroidColor.parseColor("#66BB6A")
                                    PdfEditTool.Arrow   -> AndroidColor.parseColor("#EF5350")
                                    else -> renderColor
                                }
                                rendererRef[0]?.renderFrontBufferedLayer(
                                    InkingRenderSegment(
                                        shapeTool = activeTool,
                                        shapeStart = state.shapeStart,
                                        shapeEnd = change.position,
                                        color = shapeColor,
                                        strokeWidth = if (activeTool == PdfEditTool.Line || activeTool == PdfEditTool.Arrow) 4f else 3f
                                    )
                                )
                            }
                        }

                        // ── Two-Phase Handoff Synchronization (Zero-Flicker) ─────────────────
                        // 1. DO NOT call renderer.clear() synchronously on touch up!
                        // 2. Commit the completed stroke to the multi-buffered layer to freeze it on screen.
                        // 3. Dispatch onStrokeCommitted and pass a clear callback that executes ONLY
                        //    after Compose has drawn the persistent markup into the document display list.
                        if (isFreehand) {
                            val strokePoints = state.finishFreehand()
                            if (strokePoints.size > 1) {
                                val a = if (isHl) 0.32f else 0.95f
                                val finalizedPath = android.graphics.Path(frontBufferPath)
                                val finalSegment = InkingRenderSegment(
                                    path = finalizedPath,
                                    color = renderColor,
                                    strokeWidth = sw
                                )
                                rendererRef[0]?.renderFrontBufferedLayer(finalSegment)
                                rendererRef[0]?.commit()

                                onStrokeCommitted(PdfMarkup.StrokeMarkup(strokePoints, currentColor, sw, a)) {
                                    rendererRef[0]?.clear()
                                }
                            } else {
                                rendererRef[0]?.clear()
                            }
                        } else {
                            val shape = state.finishShape()
                            if (shape != null) {
                                val shapeColor = when (activeTool) {
                                    PdfEditTool.Rect    -> AndroidColor.parseColor("#42A5F5")
                                    PdfEditTool.Ellipse -> AndroidColor.parseColor("#26A69A")
                                    PdfEditTool.Line    -> AndroidColor.parseColor("#66BB6A")
                                    PdfEditTool.Arrow   -> AndroidColor.parseColor("#EF5350")
                                    else -> renderColor
                                }
                                val finalSegment = InkingRenderSegment(
                                    shapeTool = activeTool,
                                    shapeStart = state.shapeStart,
                                    shapeEnd = state.shapeEnd,
                                    color = shapeColor,
                                    strokeWidth = if (activeTool == PdfEditTool.Line || activeTool == PdfEditTool.Arrow) 4f else 3f
                                )
                                rendererRef[0]?.renderFrontBufferedLayer(finalSegment)
                                rendererRef[0]?.commit()

                                onStrokeCommitted(shape) {
                                    rendererRef[0]?.clear()
                                }
                            } else {
                                rendererRef[0]?.clear()
                            }
                        }
                    }
                }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            rendererRef[0]?.release(true)
            rendererRef[0] = null
        }
    }
}
