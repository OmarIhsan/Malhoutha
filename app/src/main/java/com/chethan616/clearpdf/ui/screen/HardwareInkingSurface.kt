package com.chethan616.clearpdf.ui.screen

import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.MotionEvent
import android.view.SurfaceView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.graphics.lowlatency.CanvasFrontBufferedRenderer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Data segment passed to the hardware front-buffered renderer callback.
 */
internal data class InkingRenderSegment(
    val tool: PdfEditTool = PdfEditTool.None,
    val path: android.graphics.Path? = null,
    val shapeMode: InkShapeMode? = null,
    val shapeTool: PdfEditTool? = null,
    val shapeStart: Offset? = null,
    val shapeEnd: Offset? = null,
    val color: Int = 0,
    val strokeWidth: Float = 0f,
    val isHighlight: Boolean = false,
    val isClear: Boolean = false,
    val isLaser: Boolean = false,
    val laserStrokes: List<CompletedLaserStroke>? = null,
    val laserTip: Offset? = null,
    val laserScale: Float = 1.0f
)

internal typealias InkingParameter = InkingRenderSegment

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
    page: Int = -1,
    activeTool: PdfEditTool,
    shapeMode: InkShapeMode = InkShapeMode.Free,
    currentColor: Color,
    currentStrokeWidth: Float,
    pageScale: Float = 1.0f,
    onInteraction: () -> Unit,
    onStrokeCommitted: (PdfMarkup, onCommittedDrawn: () -> Unit) -> Unit,
    onTransformGesture: ((centroid: Offset, pan: Offset, zoom: Float) -> Unit)? = null
) {
    val hostView = LocalView.current

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

    // Jetpack Low-Latency Laser Pointer Photonic Skia pipeline (100% GPU accelerated, 1:1 Viewport Screen Space)
    val density = LocalDensity.current
    val laserGlowRadiusPx = with(density) { 10.dp.toPx() } // Baseline glow = 10dp
    val laserCoreRadiusPx = with(density) { 4.5.dp.toPx() } // Baseline core = 4.5dp
    val laserHotRadiusPx = with(density) { 1.8.dp.toPx() } // Radiant white center core
    val laserFlareRadiusPx = with(density) { 14.dp.toPx() } // Lens flare radius: 14dp

    val laserScreenXfermode = remember { PorterDuffXfermode(PorterDuff.Mode.SCREEN) }

    // Outer soft ambient corona (GPU multi-ring scattering replacement for BlurMaskFilter)
    val laserOuterGlowPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = 0x28FF1744.toInt() // 16% soft ambient corona
            strokeWidth = laserGlowRadiusPx * 1.5f
            xfermode = laserScreenXfermode
        }
    }

    val laserGlowPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = 0x66FF1744.toInt() // 40% luminous neon red
            strokeWidth = laserGlowRadiusPx
            xfermode = laserScreenXfermode
        }
    }

    val laserCorePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = 0xFFFF2A55.toInt() // 100% solid vivid crimson
            strokeWidth = laserCoreRadiusPx
            xfermode = laserScreenXfermode
        }
    }

    val laserCenterHotPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = 0xFFFFFFFF.toInt() // White-hot radiant center
            strokeWidth = laserHotRadiusPx
            xfermode = laserScreenXfermode
        }
    }

    val flareColors = remember {
        intArrayOf(
            0xFFFFFFFF.toInt(),
            0xFFFF2A55.toInt(),
            0x66FF1744.toInt(),
            0x00FF1744.toInt()
        )
    }
    val flareStops = remember { floatArrayOf(0.0f, 0.25f, 0.55f, 1.0f) }

    val laserFlarePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            style = Paint.Style.FILL
            xfermode = laserScreenXfermode
        }
    }

    val laserBeadHotPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            isAntiAlias = true
            isDither = true
            isFilterBitmap = true
            style = Paint.Style.FILL
            color = 0xFFFFFFFF.toInt()
            xfermode = laserScreenXfermode
        }
    }

    val completedStrokesLock = remember { Any() }
    val completedLaserStrokes = remember { mutableListOf<CompletedLaserStroke>() }
    val activeLaserPoints = remember { ArrayList<Offset>(512) }
    val activeLaserPath = remember { android.graphics.Path() }
    val isLaserPressedRef = remember { booleanArrayOf(false) }
    val currentLaserTipRef = remember { arrayOfNulls<Offset>(1) }
    val isDecayLoopRunning = remember { booleanArrayOf(false) }
    val laserVelocityRef = remember { floatArrayOf(0f) }
    val laserScaleRef = remember { floatArrayOf(1.0f) }
    val lastEventTimeRef = remember { longArrayOf(0L) }
    val lastEventPosRef = remember { arrayOfNulls<Offset>(1) }

    fun getLaserScale(velocity: Float): Float {
        // v in px/ms: slow pointing <= 0.4 px/ms (scale = 1.0x); fast slash >= 2.4 px/ms (scale = 1.35x)
        val normalized = ((velocity - 0.4f) / 2.0f).coerceIn(0f, 1f)
        return 1.0f + 0.35f * normalized
    }

    fun updateFrontBufferPath(points: List<Offset>, targetPath: android.graphics.Path = frontBufferPath) {
        targetPath.reset()
        val totalCount = points.size
        if (totalCount == 0) return

        val p0 = points[0]
        targetPath.moveTo(p0.x, p0.y)
        if (totalCount == 1) {
            targetPath.lineTo(p0.x + 0.1f, p0.y + 0.1f)
            return
        }
        if (totalCount == 2) {
            val p1 = points[1]
            targetPath.lineTo(p1.x, p1.y)
            return
        }
        for (i in 1 until totalCount - 1) {
            val pt0 = points[i]
            val pt1 = points[i + 1]
            val midX = (pt0.x + pt1.x) / 2f
            val midY = (pt0.y + pt1.y) / 2f
            targetPath.quadTo(pt0.x, pt0.y, midX, midY)
        }
        val pLast = points[totalCount - 1]
        targetPath.lineTo(pLast.x, pLast.y)
    }

    fun drawCrispScreenSpaceLaser(
        canvas: Canvas,
        activePath: android.graphics.Path?,
        beadPoint: Offset?,
        completedStrokes: List<CompletedLaserStroke>?,
        activeScale: Float = 1.0f
    ) {
        val now = SystemClock.uptimeMillis()
        val invScale = 1.0f / pageScale.coerceAtLeast(0.01f)

        // 1. Render completed decaying strokes with Thermal Contraction Decay (Cooling Light Effect)
        if (!completedStrokes.isNullOrEmpty()) {
            for (i in 0 until completedStrokes.size) {
                val stroke = completedStrokes[i]
                val elapsed = now - stroke.birthTime

                val baseGlow = (if (stroke.glowWidth > 0f) stroke.glowWidth else laserGlowRadiusPx) * invScale
                val baseCore = (if (stroke.coreWidth > 0f) stroke.coreWidth else laserCoreRadiusPx) * invScale
                val baseHot = (if (stroke.hotWidth > 0f) stroke.hotWidth else laserHotRadiusPx) * invScale

                if (elapsed < LASER_STROKE_HOLD_DURATION_MS) {
                    // Phase 1: High-Energy Hold (0ms to 1200ms) - 100% solid, fully luminous, anchored
                    laserOuterGlowPaint.strokeWidth = baseGlow * 1.5f
                    laserOuterGlowPaint.alpha = 0x28
                    canvas.drawPath(stroke.path, laserOuterGlowPaint)

                    laserGlowPaint.strokeWidth = baseGlow
                    laserGlowPaint.alpha = 0x66
                    canvas.drawPath(stroke.path, laserGlowPaint)

                    laserCorePaint.strokeWidth = baseCore
                    laserCorePaint.alpha = 255
                    canvas.drawPath(stroke.path, laserCorePaint)

                    laserCenterHotPaint.strokeWidth = baseHot
                    laserCenterHotPaint.alpha = 255
                    canvas.drawPath(stroke.path, laserCenterHotPaint)
                } else if (elapsed < LASER_STROKE_HOLD_DURATION_MS + LASER_STROKE_FADE_DURATION_MS) {
                    // Phase 2: Thermal Evaporation (1200ms to 1600ms)
                    val p = ((elapsed - LASER_STROKE_HOLD_DURATION_MS).toFloat() / LASER_STROKE_FADE_DURATION_MS).coerceIn(0f, 1f)
                    val alphaProgress = (1.0f - p) * (1.0f - p)
                    val widthFactor = 1.0f - 0.45f * p

                    val curGlowW = baseGlow * widthFactor
                    val curCoreW = baseCore * widthFactor
                    val curHotW = baseHot * widthFactor

                    laserOuterGlowPaint.strokeWidth = curGlowW * 1.5f
                    laserOuterGlowPaint.alpha = (0x28 * alphaProgress).toInt()
                    canvas.drawPath(stroke.path, laserOuterGlowPaint)

                    laserGlowPaint.strokeWidth = curGlowW
                    laserGlowPaint.alpha = (0x66 * alphaProgress).toInt()
                    canvas.drawPath(stroke.path, laserGlowPaint)

                    laserCorePaint.strokeWidth = curCoreW
                    laserCorePaint.alpha = (255 * alphaProgress).toInt()
                    canvas.drawPath(stroke.path, laserCorePaint)

                    laserCenterHotPaint.strokeWidth = curHotW
                    laserCenterHotPaint.alpha = (255 * alphaProgress * (1.0f - 0.2f * p)).toInt()
                    canvas.drawPath(stroke.path, laserCenterHotPaint)
                }
            }
        }

        // 2. Render In-Flight Active Laser Stroke (Solid Neon Filament)
        if (activePath != null && !activePath.isEmpty) {
            val curGlowW = laserGlowRadiusPx * activeScale * invScale
            val curCoreW = laserCoreRadiusPx * activeScale * invScale
            val curHotW = laserHotRadiusPx * activeScale * invScale

            laserOuterGlowPaint.strokeWidth = curGlowW * 1.5f
            laserOuterGlowPaint.alpha = 0x28
            canvas.drawPath(activePath, laserOuterGlowPaint)

            laserGlowPaint.strokeWidth = curGlowW
            laserGlowPaint.alpha = 0x66
            canvas.drawPath(activePath, laserGlowPaint)

            laserCorePaint.strokeWidth = curCoreW
            laserCorePaint.alpha = 255
            canvas.drawPath(activePath, laserCorePaint)

            laserCenterHotPaint.strokeWidth = curHotW
            laserCenterHotPaint.alpha = 255
            canvas.drawPath(activePath, laserCenterHotPaint)
        }

        // 3. Render Radial Lens Flare / Nib Glint & Radiant Hot Bead directly at pointer head
        if (beadPoint != null) {
            val flareRadius = laserFlareRadiusPx * invScale
            laserFlarePaint.shader = RadialGradient(
                beadPoint.x,
                beadPoint.y,
                flareRadius,
                flareColors,
                flareStops,
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(beadPoint.x, beadPoint.y, flareRadius, laserFlarePaint)

            // Radiant white-hot core
            laserBeadHotPaint.alpha = 255
            canvas.drawCircle(beadPoint.x, beadPoint.y, laserHotRadiusPx * activeScale * 1.25f * invScale, laserBeadHotPaint)
        }
    }

    fun drawNativeLaserStroke(
        canvas: Canvas,
        activePath: android.graphics.Path?,
        beadPoint: Offset?,
        completedStrokes: List<CompletedLaserStroke>?,
        activeScale: Float = 1.0f
    ) = drawCrispScreenSpaceLaser(canvas, activePath, beadPoint, completedStrokes, activeScale)

    val laserDecayCallback = remember {
        object : android.view.Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                val now = SystemClock.uptimeMillis()
                var hasActive = false

                synchronized(completedStrokesLock) {
                    val iterator = completedLaserStrokes.iterator()
                    while (iterator.hasNext()) {
                        val stroke = iterator.next()
                        val elapsed = now - stroke.birthTime
                        if (elapsed < LASER_STROKE_HOLD_DURATION_MS + LASER_STROKE_FADE_DURATION_MS) {
                            hasActive = true
                        } else {
                            iterator.remove()
                        }
                    }
                }

                val isPressed = isLaserPressedRef[0]
                if (hasActive || isPressed) {
                    if (!isPressed) {
                        val snapshot = synchronized(completedStrokesLock) { ArrayList(completedLaserStrokes) }
                        rendererRef[0]?.renderFrontBufferedLayer(
                            InkingRenderSegment(
                                tool = PdfEditTool.Laser,
                                isLaser = true,
                                path = null,
                                laserTip = null,
                                laserStrokes = snapshot,
                                laserScale = 1.0f
                            )
                        )
                    }
                    android.view.Choreographer.getInstance().postFrameCallback(this)
                    isDecayLoopRunning[0] = true
                } else {
                    isDecayLoopRunning[0] = false
                    rendererRef[0]?.clear()
                }
            }
        }
    }

    DisposableEffect(activeTool) {
        if (activeTool != PdfEditTool.Laser) {
            if (isDecayLoopRunning[0]) {
                android.view.Choreographer.getInstance().removeFrameCallback(laserDecayCallback)
                isDecayLoopRunning[0] = false
            }
            synchronized(completedStrokesLock) { completedLaserStrokes.clear() }
            activeLaserPoints.clear()
            activeLaserPath.reset()
            isLaserPressedRef[0] = false
            currentLaserTipRef[0] = null
            laserVelocityRef[0] = 0f
            laserScaleRef[0] = 1.0f
            lastEventTimeRef[0] = 0L
            lastEventPosRef[0] = null
            rendererRef[0]?.clear()
        }
        onDispose {
            if (isDecayLoopRunning[0]) {
                android.view.Choreographer.getInstance().removeFrameCallback(laserDecayCallback)
                isDecayLoopRunning[0] = false
            }
            synchronized(completedStrokesLock) { completedLaserStrokes.clear() }
            activeLaserPoints.clear()
            activeLaserPath.reset()
            isLaserPressedRef[0] = false
            currentLaserTipRef[0] = null
            laserVelocityRef[0] = 0f
            laserScaleRef[0] = 1.0f
            lastEventTimeRef[0] = 0L
            lastEventPosRef[0] = null
            rendererRef[0]?.clear()
        }
    }

    fun drawAndroidArrow(canvas: Canvas, start: Offset, end: Offset, arrowPaint: Paint) {
        canvas.drawLine(start.x, start.y, end.x, end.y, arrowPaint)
        val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
        val arrowLen = (arrowPaint.strokeWidth * 4.5f).coerceIn(24f, 72f)
        val angle1 = angle + Math.PI - (Math.PI / 7.2)
        val angle2 = angle + Math.PI + (Math.PI / 7.2)
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
        val fillPaint = Paint(arrowPaint).apply {
            style = Paint.Style.FILL
            xfermode = arrowPaint.xfermode
        }
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

                            // ── Unified Jetpack Low-Latency Scanout ──────────────────────────
                            // Ephemeral laser strokes exist strictly in active viewport screen space (1:1 native pixels).
                            // They do NOT inherit document magnification / pageTransformMatrix.
                            if (param.tool == PdfEditTool.Laser || param.isLaser) {
                                drawCrispScreenSpaceLaser(
                                    canvas = canvas,
                                    activePath = param.path,
                                    beadPoint = param.laserTip,
                                    completedStrokes = param.laserStrokes,
                                    activeScale = param.laserScale
                                )
                                return
                            }

                            paint.color = param.color
                            paint.strokeWidth = param.strokeWidth

                            if (param.isHighlight) {
                                paint.alpha = (255 * 0.38f).toInt()
                                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                            } else {
                                paint.alpha = AndroidColor.alpha(param.color)
                                paint.xfermode = null
                            }

                            val sMode = param.shapeMode
                            val s = param.shapeStart
                            val e = param.shapeEnd

                            if (sMode != null && sMode != InkShapeMode.Free && s != null && e != null) {
                                val left = min(s.x, e.x)
                                val top = min(s.y, e.y)
                                val right = max(s.x, e.x)
                                val bottom = max(s.y, e.y)
                                when (sMode) {
                                    InkShapeMode.Rectangle -> canvas.drawRect(left, top, right, bottom, paint)
                                    InkShapeMode.Circle    -> canvas.drawOval(left, top, right, bottom, paint)
                                    InkShapeMode.Arrow     -> drawAndroidArrow(canvas, s, e, paint)
                                    InkShapeMode.Triangle  -> {
                                        val midX = s.x + (e.x - s.x) / 2f
                                        val triPath = android.graphics.Path().apply {
                                            moveTo(midX, s.y)
                                            lineTo(s.x, e.y)
                                            lineTo(e.x, e.y)
                                            close()
                                        }
                                        canvas.drawPath(triPath, paint)
                                    }
                                    InkShapeMode.Free -> Unit
                                }
                            } else if (param.shapeTool != null && s != null && e != null) {
                                val left = min(s.x, e.x)
                                val top = min(s.y, e.y)
                                val right = max(s.x, e.x)
                                val bottom = max(s.y, e.y)
                                when (param.shapeTool) {
                                    PdfEditTool.Rect     -> canvas.drawRect(left, top, right, bottom, paint)
                                    PdfEditTool.Ellipse  -> canvas.drawOval(left, top, right, bottom, paint)
                                    PdfEditTool.Line     -> canvas.drawLine(s.x, s.y, e.x, e.y, paint)
                                    PdfEditTool.Arrow    -> drawAndroidArrow(canvas, s, e, paint)
                                    PdfEditTool.Triangle -> {
                                        val midX = s.x + (e.x - s.x) / 2f
                                        val triPath = android.graphics.Path().apply {
                                            moveTo(midX, s.y)
                                            lineTo(s.x, e.y)
                                            lineTo(e.x, e.y)
                                            close()
                                        }
                                        canvas.drawPath(triPath, paint)
                                    }
                                    else -> Unit
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
                            // When committed, draw ONLY the finalized stroke or shape into the multi-buffered layer
                            // so it remains frozen on glass until Compose renders the persistent mark.
                            canvas.drawColor(AndroidColor.TRANSPARENT, PorterDuff.Mode.CLEAR)
                            val finalParam = params.lastOrNull { !it.isClear } ?: return
                            if (finalParam.isLaser) return

                            paint.color = finalParam.color
                            paint.strokeWidth = finalParam.strokeWidth

                            if (finalParam.isHighlight) {
                                paint.alpha = (255 * 0.38f).toInt()
                                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                            } else {
                                paint.alpha = AndroidColor.alpha(finalParam.color)
                                paint.xfermode = null
                            }

                            val sMode = finalParam.shapeMode
                            val s = finalParam.shapeStart
                            val e = finalParam.shapeEnd

                            if (sMode != null && sMode != InkShapeMode.Free && s != null && e != null) {
                                val left = min(s.x, e.x)
                                val top = min(s.y, e.y)
                                val right = max(s.x, e.x)
                                val bottom = max(s.y, e.y)
                                when (sMode) {
                                    InkShapeMode.Rectangle -> canvas.drawRect(left, top, right, bottom, paint)
                                    InkShapeMode.Circle    -> canvas.drawOval(left, top, right, bottom, paint)
                                    InkShapeMode.Arrow     -> drawAndroidArrow(canvas, s, e, paint)
                                    InkShapeMode.Triangle  -> {
                                        val midX = s.x + (e.x - s.x) / 2f
                                        val triPath = android.graphics.Path().apply {
                                            moveTo(midX, s.y)
                                            lineTo(s.x, e.y)
                                            lineTo(e.x, e.y)
                                            close()
                                        }
                                        canvas.drawPath(triPath, paint)
                                    }
                                    InkShapeMode.Free -> Unit
                                }
                            } else if (finalParam.shapeTool != null && s != null && e != null) {
                                val left = min(s.x, e.x)
                                val top = min(s.y, e.y)
                                val right = max(s.x, e.x)
                                val bottom = max(s.y, e.y)
                                when (finalParam.shapeTool) {
                                    PdfEditTool.Rect     -> canvas.drawRect(left, top, right, bottom, paint)
                                    PdfEditTool.Ellipse  -> canvas.drawOval(left, top, right, bottom, paint)
                                    PdfEditTool.Line     -> canvas.drawLine(s.x, s.y, e.x, e.y, paint)
                                    PdfEditTool.Arrow    -> drawAndroidArrow(canvas, s, e, paint)
                                    PdfEditTool.Triangle -> {
                                        val midX = s.x + (e.x - s.x) / 2f
                                        val triPath = android.graphics.Path().apply {
                                            moveTo(midX, s.y)
                                            lineTo(s.x, e.y)
                                            lineTo(e.x, e.y)
                                            close()
                                        }
                                        canvas.drawPath(triPath, paint)
                                    }
                                    else -> Unit
                                }
                            } else if (finalParam.path != null) {
                                canvas.drawPath(finalParam.path, paint)
                            }
                        }
                    }

                    val renderer = CanvasFrontBufferedRenderer(this, callback)
                    rendererRef[0] = renderer
                }
            }
        )

        val currentOnInteraction by rememberUpdatedState(onInteraction)
        val currentOnStrokeCommitted by rememberUpdatedState(onStrokeCommitted)
        val currentOnTransformGesture by rememberUpdatedState(onTransformGesture)

        // Low-latency gesture detection driving the front-buffer surface
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(page, activeTool, shapeMode, currentColor, currentStrokeWidth) {
                    val isHl = activeTool == PdfEditTool.Highlight
                    val isLaser = activeTool == PdfEditTool.Laser
                    val isFreehand = shapeMode == InkShapeMode.Free && (activeTool == PdfEditTool.Draw || isHl)
                    val effectiveShapeMode = if (isFreehand) {
                        InkShapeMode.Free
                    } else when (activeTool) {
                        PdfEditTool.Rect     -> InkShapeMode.Rectangle
                        PdfEditTool.Ellipse  -> InkShapeMode.Circle
                        PdfEditTool.Arrow    -> InkShapeMode.Arrow
                        PdfEditTool.Line     -> InkShapeMode.Arrow
                        PdfEditTool.Triangle -> InkShapeMode.Triangle
                        else -> shapeMode
                    }
                    val isShape = effectiveShapeMode != InkShapeMode.Free && (activeTool == PdfEditTool.Draw || isHl)
                    if (!isFreehand && !isShape && !isLaser) return@pointerInput

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val initialEvent = currentEvent
                        val initialChanges = initialEvent.changes
                        val initialHasStylus = initialChanges.any { it.pressed && (it.type == PointerType.Stylus || it.type == PointerType.Eraser) } ||
                            (initialEvent.motionEvent?.let { me ->
                                (0 until me.pointerCount).any { idx ->
                                    me.getToolType(idx) == MotionEvent.TOOL_TYPE_STYLUS || me.getToolType(idx) == MotionEvent.TOOL_TYPE_ERASER
                                }
                            } ?: false)

                        var isTwoFingerTransforming = false
                        var strokeCancelled = false

                        val freezeActiveLaserStroke = {
                            if (isLaserPressedRef[0]) {
                                isLaserPressedRef[0] = false
                                currentLaserTipRef[0] = null
                                if (activeLaserPoints.size > 1) {
                                    val finalizedPath = android.graphics.Path(activeLaserPath)
                                    val strokeScale = laserScaleRef[0]
                                    synchronized(completedStrokesLock) {
                                        completedLaserStrokes.add(
                                            CompletedLaserStroke(
                                                path = finalizedPath,
                                                birthTime = SystemClock.uptimeMillis(),
                                                coreWidth = laserCoreRadiusPx * strokeScale,
                                                glowWidth = laserGlowRadiusPx * strokeScale,
                                                hotWidth = laserHotRadiusPx * strokeScale
                                            )
                                        )
                                    }
                                }
                                activeLaserPoints.clear()
                                activeLaserPath.reset()
                                laserVelocityRef[0] = 0f
                                laserScaleRef[0] = 1.0f
                                lastEventTimeRef[0] = 0L
                                lastEventPosRef[0] = null
                                if (!isDecayLoopRunning[0]) {
                                    isDecayLoopRunning[0] = true
                                    android.view.Choreographer.getInstance().postFrameCallback(laserDecayCallback)
                                }
                            }
                        }

                        // Check if multi-touch finger gesture is already active on initial touch
                        if (!initialHasStylus && initialChanges.count { it.pressed } >= 2) {
                            isTwoFingerTransforming = true
                            strokeCancelled = true
                            hostView.parent?.requestDisallowInterceptTouchEvent(false)
                            val zoomChange = initialEvent.calculateZoom()
                            val panChange = initialEvent.calculatePan()
                            val centroid = initialEvent.calculateCentroid(useCurrent = true)
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                currentOnTransformGesture?.invoke(centroid, panChange, zoomChange)
                            }
                            initialChanges.forEach { it.consume() }
                        } else {
                            // Scenario A: Single pointer (stylus or finger drawing)
                            down.consume()
                            if (!isLaser) {
                                currentOnInteraction()
                            }
                            hostView.parent?.requestDisallowInterceptTouchEvent(true)

                            if (isLaser) {
                                isLaserPressedRef[0] = true
                                currentLaserTipRef[0] = down.position
                                activeLaserPoints.clear()
                                activeLaserPoints.add(down.position)
                                laserVelocityRef[0] = 0f
                                laserScaleRef[0] = 1.0f
                                lastEventTimeRef[0] = initialEvent.motionEvent?.eventTime ?: SystemClock.uptimeMillis()
                                lastEventPosRef[0] = down.position
                                updateFrontBufferPath(activeLaserPoints, activeLaserPath)

                                val snapshot = synchronized(completedStrokesLock) { ArrayList(completedLaserStrokes) }
                                rendererRef[0]?.renderFrontBufferedLayer(
                                    InkingRenderSegment(
                                        tool = PdfEditTool.Laser,
                                        isLaser = true,
                                        path = activeLaserPath,
                                        laserTip = down.position,
                                        laserStrokes = snapshot,
                                        laserScale = 1.0f
                                    )
                                )
                            } else {
                                // Clear any previous residual stroke before starting the new one
                                rendererRef[0]?.clear()
                            }
                        }

                        val alpha = if (isHl) 0.38f else 1.0f
                        val sw = if (isHl) currentStrokeWidth * 2.8f else currentStrokeWidth
                        val renderColor = currentColor.copy(alpha = alpha).toArgb()

                        var shapeStart = down.position
                        var shapeEnd = down.position

                        if (!isLaser && !isTwoFingerTransforming) {
                            if (isFreehand) {
                                state.startFreehand(
                                    start = down.position,
                                    color = currentColor,
                                    width = currentStrokeWidth,
                                    highlight = isHl
                                )
                                updateFrontBufferPath(state.points)
                                rendererRef[0]?.renderFrontBufferedLayer(
                                    InkingRenderSegment(
                                        path = frontBufferPath,
                                        color = renderColor,
                                        strokeWidth = sw,
                                        isHighlight = isHl
                                    )
                                )
                            } else {
                                rendererRef[0]?.renderFrontBufferedLayer(
                                    InkingRenderSegment(
                                        shapeMode = effectiveShapeMode,
                                        shapeStart = shapeStart,
                                        shapeEnd = shapeEnd,
                                        color = renderColor,
                                        strokeWidth = sw,
                                        isHighlight = isHl
                                    )
                                )
                            }
                        }

                        var activePointerId = down.id

                        while (true) {
                            val event = awaitPointerEvent()
                            val motionEvent = event.motionEvent
                            val pressedChanges = event.changes.filter { it.pressed }

                            if (pressedChanges.isEmpty()) {
                                if (isLaser) {
                                    freezeActiveLaserStroke()
                                }
                                break
                            }

                            // If already in two-finger transform mode, route all pan/zoom deltas and suppress inking
                            if (isTwoFingerTransforming) {
                                val zoomChange = event.calculateZoom()
                                val panChange = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = true)
                                if (zoomChange != 1f || panChange != Offset.Zero) {
                                    currentOnTransformGesture?.invoke(centroid, panChange, zoomChange)
                                }
                                event.changes.forEach { it.consume() }
                                continue
                            }

                            // Inspect pointer types for stylus vs finger palm rejection
                            val hasStylus = event.changes.any { it.pressed && (it.type == PointerType.Stylus || it.type == PointerType.Eraser) } ||
                                (motionEvent != null && (0 until motionEvent.pointerCount).any { idx ->
                                    motionEvent.getToolType(idx) == MotionEvent.TOOL_TYPE_STYLUS || motionEvent.getToolType(idx) == MotionEvent.TOOL_TYPE_ERASER
                                })

                            val activeChange: PointerInputChange
                            if (hasStylus) {
                                // Stylus takes absolute priority: palm rejection on any simultaneous finger contacts
                                val stylusChange = event.changes.firstOrNull { it.id == activePointerId && (it.type == PointerType.Stylus || it.type == PointerType.Eraser) }
                                    ?: event.changes.firstOrNull { it.pressed && (it.type == PointerType.Stylus || it.type == PointerType.Eraser) }

                                if (stylusChange == null || !stylusChange.pressed) {
                                    if (isLaser) {
                                        freezeActiveLaserStroke()
                                    }
                                    break
                                }

                                activePointerId = stylusChange.id
                                // Palm rejection: consume any resting palm touches so they don't trigger gestures
                                event.changes.filter { it.id != stylusChange.id }.forEach { it.consume() }
                                activeChange = stylusChange
                            } else {
                                // Multi-Touch Detection: 2 or more finger pointers without stylus
                                if (pressedChanges.size >= 2) {
                                    // Multi-finger gesture detected
                                    strokeCancelled = true
                                    isTwoFingerTransforming = true

                                    if (isLaser) {
                                        // Freeze the in-flight laser stroke into a decaying stroke so it remains
                                        // stationary on screen glass and naturally decays during pan/zoom
                                        freezeActiveLaserStroke()
                                    } else {
                                        // Cancel in-flight pen ink stroke immediately & clear Skia front buffer
                                        state.cancel()
                                        frontBufferPath.reset()
                                        rendererRef[0]?.clear()
                                    }

                                    // Release parent interception for smooth viewport panning/scaling
                                    hostView.parent?.requestDisallowInterceptTouchEvent(false)

                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()
                                    val centroid = event.calculateCentroid(useCurrent = true)
                                    if (zoomChange != 1f || panChange != Offset.Zero) {
                                        currentOnTransformGesture?.invoke(centroid, panChange, zoomChange)
                                    }
                                    event.changes.forEach { it.consume() }
                                    continue
                                }

                                // Single finger inking
                                val fingerChange = event.changes.firstOrNull { it.id == activePointerId }
                                if (fingerChange == null || !fingerChange.pressed) {
                                    if (isLaser) {
                                        freezeActiveLaserStroke()
                                    }
                                    break
                                }
                                activeChange = fingerChange
                            }

                            hostView.parent?.requestDisallowInterceptTouchEvent(true)
                            activeChange.consume()

                            if (!isFreehand && !isLaser) {
                                shapeEnd = activeChange.position
                            }

                            if (isLaser) {
                                // Velocity tracking for dynamic organic beam width
                                val eventTime = motionEvent?.eventTime ?: SystemClock.uptimeMillis()
                                val lastTime = lastEventTimeRef[0]
                                val lastPos = lastEventPosRef[0]
                                if (lastPos != null && lastTime > 0L && eventTime > lastTime) {
                                    val dt = (eventTime - lastTime).toFloat()
                                    val dist = hypot((activeChange.position.x - lastPos.x).toDouble(), (activeChange.position.y - lastPos.y).toDouble()).toFloat()
                                    val instantV = (dist / dt).coerceIn(0f, 10f)
                                    laserVelocityRef[0] = 0.35f * instantV + 0.65f * laserVelocityRef[0]
                                }
                                lastEventTimeRef[0] = eventTime
                                lastEventPosRef[0] = activeChange.position

                                laserScaleRef[0] = getLaserScale(laserVelocityRef[0])

                                // Batch hardware 240Hz digitizer points (identical to Pen)
                                val batched = extractDigitizerBatchPoints(activeChange, event)
                                for (i in 0 until batched.size) {
                                    val pt = batched[i]
                                    val last = activeLaserPoints.lastOrNull()
                                    if (last == null || pt.x != last.x || pt.y != last.y) {
                                        activeLaserPoints.add(pt)
                                    }
                                }
                                val last = activeLaserPoints.lastOrNull()
                                if (last == null || activeChange.position.x != last.x || activeChange.position.y != last.y) {
                                    activeLaserPoints.add(activeChange.position)
                                }

                                // Pointer tip coordinate is strictly the actual stylus contact point (no prediction / extrapolation)
                                currentLaserTipRef[0] = activeChange.position

                                // Rebuild active path using the identical midpoint quadratic spline formula as Pen
                                updateFrontBufferPath(activeLaserPoints, activeLaserPath)

                                // Synchronous immediate front-buffer dispatch: push directly to front buffer!
                                val snapshot = synchronized(completedStrokesLock) { ArrayList(completedLaserStrokes) }
                                rendererRef[0]?.renderFrontBufferedLayer(
                                    InkingRenderSegment(
                                        tool = PdfEditTool.Laser,
                                        isLaser = true,
                                        path = activeLaserPath,
                                        laserTip = activeChange.position,
                                        laserStrokes = snapshot,
                                        laserScale = laserScaleRef[0]
                                    )
                                )
                            } else if (isFreehand) {
                                // Batch hardware 240Hz digitizer points
                                val batched = extractDigitizerBatchPoints(activeChange, event)
                                if (batched.isNotEmpty()) {
                                    state.addPoints(batched)
                                }
                                state.addPoint(activeChange.position)

                                // Update Skia path and render directly to hardware front-buffer
                                updateFrontBufferPath(state.points)
                                rendererRef[0]?.renderFrontBufferedLayer(
                                    InkingRenderSegment(
                                        path = frontBufferPath,
                                        color = renderColor,
                                        strokeWidth = sw,
                                        isHighlight = isHl
                                    )
                                )
                            } else {
                                shapeEnd = activeChange.position
                                rendererRef[0]?.renderFrontBufferedLayer(
                                    InkingRenderSegment(
                                        shapeMode = effectiveShapeMode,
                                        shapeStart = shapeStart,
                                        shapeEnd = shapeEnd,
                                        color = renderColor,
                                        strokeWidth = sw,
                                        isHighlight = isHl
                                    )
                                )
                            }
                        }

                        // Release disallow intercept on gesture completion
                        hostView.parent?.requestDisallowInterceptTouchEvent(false)

                        if (strokeCancelled || isTwoFingerTransforming) {
                            rendererRef[0]?.clear()
                            state.cancel()
                            if (isLaser) {
                                isLaserPressedRef[0] = false
                                currentLaserTipRef[0] = null
                                activeLaserPoints.clear()
                                activeLaserPath.reset()
                                synchronized(completedStrokesLock) { completedLaserStrokes.clear() }
                                laserVelocityRef[0] = 0f
                                laserScaleRef[0] = 1.0f
                                lastEventTimeRef[0] = 0L
                                lastEventPosRef[0] = null
                                if (isDecayLoopRunning[0]) {
                                    android.view.Choreographer.getInstance().removeFrameCallback(laserDecayCallback)
                                    isDecayLoopRunning[0] = false
                                }
                                rendererRef[0]?.clear()
                            }
                        } else {
                            // ── Two-Phase Handoff Synchronization (Zero-Flicker) ─────────────────
                            // 1. DO NOT call renderer.clear() synchronously on touch up!
                            // 2. Commit the completed stroke to the multi-buffered layer to freeze it on screen.
                            // 3. Dispatch onStrokeCommitted and pass a clear callback that executes ONLY
                            //    after Compose has drawn the persistent markup into the document display list.
                            if (isLaser) {
                                freezeActiveLaserStroke()
                            } else if (isFreehand) {
                                val strokePoints = state.finishFreehand()
                                if (strokePoints.size > 1) {
                                    val finalizedPath = android.graphics.Path(frontBufferPath)
                                    val finalSegment = InkingRenderSegment(
                                        path = finalizedPath,
                                        color = renderColor,
                                        strokeWidth = sw,
                                        isHighlight = isHl
                                    )
                                    rendererRef[0]?.renderFrontBufferedLayer(finalSegment)
                                    rendererRef[0]?.commit()

                                    currentOnStrokeCommitted(PdfMarkup.StrokeMarkup(strokePoints, currentColor, sw, alpha, isClosed = false, isHighlight = isHl)) {
                                        rendererRef[0]?.clear()
                                    }
                                } else {
                                    rendererRef[0]?.clear()
                                }
                            } else {
                                val dist = (shapeEnd - shapeStart).getDistance()
                                if (dist >= 4f) {
                                    val finalSegment = InkingRenderSegment(
                                        shapeMode = effectiveShapeMode,
                                        shapeStart = shapeStart,
                                        shapeEnd = shapeEnd,
                                        color = renderColor,
                                        strokeWidth = sw,
                                        isHighlight = isHl
                                    )
                                    rendererRef[0]?.renderFrontBufferedLayer(finalSegment)
                                    rendererRef[0]?.commit()

                                    val minX = min(shapeStart.x, shapeEnd.x)
                                    val minY = min(shapeStart.y, shapeEnd.y)
                                    val maxX = max(shapeStart.x, shapeEnd.x)
                                    val maxY = max(shapeStart.y, shapeEnd.y)

                                    val markup: PdfMarkup = when (effectiveShapeMode) {
                                        InkShapeMode.Rectangle -> PdfMarkup.RectMarkup(
                                            start = Offset(minX, minY),
                                            end = Offset(maxX, maxY),
                                            color = currentColor,
                                            alpha = alpha,
                                            filled = false,
                                            width = sw,
                                            isHighlight = isHl
                                        )
                                        InkShapeMode.Circle -> PdfMarkup.OvalMarkup(
                                            start = Offset(minX, minY),
                                            end = Offset(maxX, maxY),
                                            color = currentColor,
                                            alpha = alpha,
                                            filled = false,
                                            width = sw,
                                            isHighlight = isHl
                                        )
                                        InkShapeMode.Arrow -> PdfMarkup.LineMarkup(
                                            start = shapeStart,
                                            end = shapeEnd,
                                            color = currentColor,
                                            width = sw,
                                            alpha = alpha,
                                            arrowHead = true,
                                            isHighlight = isHl
                                        )
                                        InkShapeMode.Triangle -> {
                                            val midX = shapeStart.x + (shapeEnd.x - shapeStart.x) / 2f
                                            PdfMarkup.StrokeMarkup(
                                                points = listOf(
                                                    Offset(midX, shapeStart.y),
                                                    Offset(shapeStart.x, shapeEnd.y),
                                                    Offset(shapeEnd.x, shapeEnd.y)
                                                ),
                                                color = currentColor,
                                                width = sw,
                                                alpha = alpha,
                                                isClosed = true,
                                                isHighlight = isHl
                                            )
                                        }
                                        InkShapeMode.Free -> {
                                            val pts = state.finishFreehand()
                                            PdfMarkup.StrokeMarkup(pts, currentColor, sw, alpha, isClosed = false, isHighlight = isHl)
                                        }
                                    }
                                    state.cancel()
                                    currentOnStrokeCommitted(markup) {
                                        rendererRef[0]?.clear()
                                    }
                                } else {
                                    state.cancel()
                                    rendererRef[0]?.clear()
                                }
                            }
                        }
                    }
                }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isDecayLoopRunning[0]) {
                android.view.Choreographer.getInstance().removeFrameCallback(laserDecayCallback)
                isDecayLoopRunning[0] = false
            }
            synchronized(completedStrokesLock) { completedLaserStrokes.clear() }
            activeLaserPoints.clear()
            activeLaserPath.reset()
            isLaserPressedRef[0] = false
            currentLaserTipRef[0] = null
            laserVelocityRef[0] = 0f
            laserScaleRef[0] = 1.0f
            lastEventTimeRef[0] = 0L
            lastEventPosRef[0] = null
            rendererRef[0]?.release(true)
            rendererRef[0] = null
        }
    }
}
