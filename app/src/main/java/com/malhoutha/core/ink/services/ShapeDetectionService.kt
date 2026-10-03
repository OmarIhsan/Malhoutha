package com.malhoutha.core.ink.services

import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

enum class RecognizedShapeType {
    NONE,
    LINE,
    CIRCLE,
    OVAL,
    RECTANGLE,
    SQUARE,
    TRIANGLE,
    ARROW
}

data class ShapeRecognitionResult(
    val type: RecognizedShapeType,
    val points: List<PointF>,
    val anchorPoint: PointF? = null,
    val bounds: RectF? = null,
    val lineStart: PointF? = null,
    val lineEnd: PointF? = null,
    val confidence: Float = 0f
) {
    /**
     * Converts the recognized geometric shape into a Skia / Android Path
     * suitable for drawing directly onto the front-buffer Canvas.
     */
    fun toPath(): Path {
        val path = Path()
        when (type) {
            RecognizedShapeType.LINE -> {
                val s = lineStart ?: points.firstOrNull()
                val e = lineEnd ?: points.lastOrNull()
                if (s != null && e != null) {
                    path.moveTo(s.x, s.y)
                    path.lineTo(e.x, e.y)
                }
            }
            RecognizedShapeType.ARROW -> {
                if (points.isNotEmpty()) {
                    path.moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        path.lineTo(points[i].x, points[i].y)
                    }
                }
            }
            RecognizedShapeType.RECTANGLE, RecognizedShapeType.SQUARE -> {
                val b = bounds
                if (b != null) {
                    path.addRect(b, Path.Direction.CW)
                } else if (points.size >= 4) {
                    path.moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        path.lineTo(points[i].x, points[i].y)
                    }
                    path.close()
                }
            }
            RecognizedShapeType.CIRCLE, RecognizedShapeType.OVAL -> {
                val b = bounds
                if (b != null) {
                    path.addOval(b, Path.Direction.CW)
                } else if (points.isNotEmpty()) {
                    path.moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        path.lineTo(points[i].x, points[i].y)
                    }
                    path.close()
                }
            }
            RecognizedShapeType.TRIANGLE -> {
                if (points.isNotEmpty()) {
                    path.moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        path.lineTo(points[i].x, points[i].y)
                    }
                    path.close()
                }
            }
            RecognizedShapeType.NONE -> {
                if (points.isNotEmpty()) {
                    path.moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        path.lineTo(points[i].x, points[i].y)
                    }
                }
            }
        }
        return path
    }
}

/**
 * Service to analyze raw ink strokes and perfectly snap them into geometric vector shapes
 * (Lines, Directed Arrows, Circles, Ovals, Rectangles, Squares, Triangles) for Hold-to-Snap.
 *
 * Employs Douglas-Peucker polygon reduction, circularity ratio tests, and tip-vector calculations.
 * Pure Android graphics with zero external third-party dependencies.
 */
class ShapeDetectionService {

    /**
     * Overload for Compose [Offset] lists.
     */
    @JvmName("detectShapeOffsets")
    fun detectShape(offsets: List<Offset>): ShapeRecognitionResult {
        if (offsets.size < 4) return ShapeRecognitionResult(RecognizedShapeType.NONE, emptyList())
        val points = offsets.map { PointF(it.x, it.y) }
        return detectShape(points)
    }

    /**
     * Analyzes raw digitizer points and recognizes geometric shape candidates.
     */
    fun detectShape(points: List<PointF>): ShapeRecognitionResult {
        if (points.size < 4) {
            return ShapeRecognitionResult(RecognizedShapeType.NONE, points)
        }

        val bounds = calculateBounds(points)
        val width = bounds.width()
        val height = bounds.height()
        val pathLength = computePathLength(points)

        // Ignore micro-taps and jitter
        if (pathLength < 30f) {
            return ShapeRecognitionResult(RecognizedShapeType.NONE, points)
        }

        // 1. Check for Directed Arrow (Open shape with arrow head at tip)
        val arrowCandidate = checkArrow(points, pathLength)
        if (arrowCandidate != null) {
            return arrowCandidate
        }

        // 2. Check for Straight Line
        if (isLine(points, pathLength)) {
            val start = points.first()
            val end = points.last()
            val perfected = applyLinePerfection(start, end)
            val chord = hypot(end.x - start.x, end.y - start.y)
            val maxDev = maxDeviationFromSegment(points, start, end)
            val allowed = max(28f, chord * 0.12f)
            val confidence = (1f - (maxDev / allowed) * 0.25f).coerceIn(0.78f, 0.98f)
            return ShapeRecognitionResult(
                type = RecognizedShapeType.LINE,
                points = perfected,
                anchorPoint = start,
                bounds = bounds,
                lineStart = start,
                lineEnd = end,
                confidence = confidence
            )
        }

        // 3. Closed Loop Shapes (Circle, Oval, Triangle, Rectangle, Square)
        val startPoint = points.first()
        val endPoint = points.last()
        val closureDistance = hypot(startPoint.x - endPoint.x, startPoint.y - endPoint.y)
        val diagonal = hypot(width, height)

        if (closureDistance < diagonal * 0.40f || closureDistance < 85f) {
            // Douglas-Peucker simplification for polygon analysis
            val epsilon = max(width, height) * 0.12f
            val simplified = douglasPeucker(points, epsilon)

            // Triangle: 3 dominant corners (+ 1 closure point)
            if (simplified.size in 4..5 && isPolygonClosed(simplified)) {
                val corners = getDistinctCorners(simplified, 3)
                if (corners.size == 3) {
                    val perfected = applyTrianglePerfection(corners)
                    return ShapeRecognitionResult(
                        type = RecognizedShapeType.TRIANGLE,
                        points = perfected,
                        anchorPoint = PointF(bounds.centerX(), bounds.centerY()),
                        bounds = bounds,
                        confidence = 0.88f
                    )
                }
            }

            // Rectangle / Square: 4 dominant corners (+ 1 closure point)
            if (simplified.size in 5..7 && isPolygonClosed(simplified)) {
                val corners = getDistinctCorners(simplified, 4)
                if (corners.size == 4) {
                    val ratio = width / height.coerceAtLeast(1f)
                    val isSquare = ratio in 0.82f..1.22f
                    val perfectedBounds: RectF
                    val type: RecognizedShapeType
                    if (isSquare) {
                        val cx = bounds.centerX()
                        val cy = bounds.centerY()
                        val side = max(width, height) / 2f
                        perfectedBounds = RectF(cx - side, cy - side, cx + side, cy + side)
                        type = RecognizedShapeType.SQUARE
                    } else {
                        perfectedBounds = bounds
                        type = RecognizedShapeType.RECTANGLE
                    }
                    val perfected = applyRectanglePerfection(perfectedBounds)
                    return ShapeRecognitionResult(
                        type = type,
                        points = perfected,
                        anchorPoint = PointF(bounds.centerX(), bounds.centerY()),
                        bounds = perfectedBounds,
                        confidence = 0.90f
                    )
                }
            }

            // Circle / Oval check via circularity quotient and aspect ratio
            val circularity = calculateCircularity(points, pathLength)
            if (circularity > 0.45f) {
                val ratio = width / height.coerceAtLeast(1f)
                val isCircle = ratio in 0.80f..1.25f
                val perfectedBounds: RectF
                val type: RecognizedShapeType
                if (isCircle) {
                    val cx = bounds.centerX()
                    val cy = bounds.centerY()
                    val r = (width + height) / 4f
                    perfectedBounds = RectF(cx - r, cy - r, cx + r, cy + r)
                    type = RecognizedShapeType.CIRCLE
                } else {
                    perfectedBounds = bounds
                    type = RecognizedShapeType.OVAL
                }
                val perfected = applyOvalPerfection(perfectedBounds)
                val conf = (0.80f + (circularity * 0.15f)).coerceIn(0.78f, 0.95f)
                return ShapeRecognitionResult(
                    type = type,
                    points = perfected,
                    anchorPoint = PointF(bounds.centerX(), bounds.centerY()),
                    bounds = perfectedBounds,
                    confidence = conf
                )
            }
        }

        return ShapeRecognitionResult(RecognizedShapeType.NONE, points, confidence = 0f)
    }

    /**
     * Manipulates an existing snapped shape dynamically when user drags before lifting.
     */
    @JvmName("manipulateSnappedShapeOffset")
    fun manipulateSnappedShape(
        current: ShapeRecognitionResult,
        cursor: Offset
    ): ShapeRecognitionResult {
        return manipulateSnappedShape(current, PointF(cursor.x, cursor.y))
    }

    /**
     * Manipulates an existing snapped shape dynamically when user drags before lifting.
     */
    fun manipulateSnappedShape(
        current: ShapeRecognitionResult,
        cursor: PointF
    ): ShapeRecognitionResult {
        val anchor = current.anchorPoint ?: current.points.firstOrNull() ?: cursor
        return when (current.type) {
            RecognizedShapeType.LINE -> {
                val start = current.lineStart ?: anchor
                val perfected = applyLinePerfection(start, cursor)
                current.copy(
                    points = perfected,
                    lineStart = start,
                    lineEnd = cursor,
                    bounds = calculateBounds(perfected)
                )
            }
            RecognizedShapeType.ARROW -> {
                val start = current.lineStart ?: anchor
                val perfected = applyArrowPerfection(start, cursor)
                current.copy(
                    points = perfected,
                    lineStart = start,
                    lineEnd = cursor,
                    bounds = calculateBounds(perfected)
                )
            }
            RecognizedShapeType.CIRCLE -> {
                val radius = hypot(cursor.x - anchor.x, cursor.y - anchor.y).coerceAtLeast(10f)
                val bounds = RectF(anchor.x - radius, anchor.y - radius, anchor.x + radius, anchor.y + radius)
                val perfected = applyOvalPerfection(bounds)
                current.copy(points = perfected, bounds = bounds)
            }
            RecognizedShapeType.OVAL -> {
                val rx = abs(cursor.x - anchor.x).coerceAtLeast(10f)
                val ry = abs(cursor.y - anchor.y).coerceAtLeast(10f)
                val bounds = RectF(anchor.x - rx, anchor.y - ry, anchor.x + rx, anchor.y + ry)
                val perfected = applyOvalPerfection(bounds)
                current.copy(points = perfected, bounds = bounds)
            }
            RecognizedShapeType.RECTANGLE -> {
                val rx = abs(cursor.x - anchor.x).coerceAtLeast(10f)
                val ry = abs(cursor.y - anchor.y).coerceAtLeast(10f)
                val bounds = RectF(anchor.x - rx, anchor.y - ry, anchor.x + rx, anchor.y + ry)
                val perfected = applyRectanglePerfection(bounds)
                current.copy(points = perfected, bounds = bounds)
            }
            RecognizedShapeType.SQUARE -> {
                val side = max(abs(cursor.x - anchor.x), abs(cursor.y - anchor.y)).coerceAtLeast(10f)
                val bounds = RectF(anchor.x - side, anchor.y - side, anchor.x + side, anchor.y + side)
                val perfected = applyRectanglePerfection(bounds)
                current.copy(points = perfected, bounds = bounds)
            }
            RecognizedShapeType.TRIANGLE -> {
                val rx = abs(cursor.x - anchor.x).coerceAtLeast(10f)
                val ry = abs(cursor.y - anchor.y).coerceAtLeast(10f)
                val apex = PointF(anchor.x, anchor.y - ry)
                val baseRight = PointF(anchor.x + rx, anchor.y + ry)
                val baseLeft = PointF(anchor.x - rx, anchor.y + ry)
                val perfected = applyTrianglePerfection(listOf(apex, baseRight, baseLeft))
                val bounds = RectF(anchor.x - rx, anchor.y - ry, anchor.x + rx, anchor.y + ry)
                current.copy(points = perfected, bounds = bounds)
            }
            RecognizedShapeType.NONE -> current
        }
    }

    private fun checkArrow(points: List<PointF>, pathLength: Float): ShapeRecognitionResult? {
        if (points.size < 6) return null
        val start = points.first()

        // Find the index of the point farthest from start
        var maxDist = 0f
        var tipIdx = 0
        for (i in points.indices) {
            val d = hypot(points[i].x - start.x, points[i].y - start.y)
            if (d > maxDist) {
                maxDist = d
                tipIdx = i
            }
        }

        // Tip must be located in the latter 40% of the stroke, but not the very end
        if (tipIdx < points.size * 0.55f || tipIdx >= points.size - 2) return null
        if (maxDist < 40f) return null

        val tip = points[tipIdx]
        val shaftLength = maxDist

        // Shaft (from start to tip) must be reasonably straight
        val shaftPoints = points.subList(0, tipIdx + 1)
        val shaftDev = maxDeviationFromSegment(shaftPoints, start, tip)
        if (shaftDev > max(28f, shaftLength * 0.16f)) return null

        // Tail (barbs) must stay near the tip and reverse direction relative to shaft
        val tailPoints = points.subList(tipIdx, points.size)
        var tailMaxDistFromTip = 0f
        for (p in tailPoints) {
            val dist = hypot(p.x - tip.x, p.y - tip.y)
            if (dist > tailMaxDistFromTip) tailMaxDistFromTip = dist
        }

        // Tail length shouldn't exceed 45% of shaft length
        if (tailMaxDistFromTip < 12f || tailMaxDistFromTip > shaftLength * 0.45f) return null

        val perfected = applyArrowPerfection(start, tip)
        return ShapeRecognitionResult(
            type = RecognizedShapeType.ARROW,
            points = perfected,
            anchorPoint = start,
            bounds = calculateBounds(perfected),
            lineStart = start,
            lineEnd = tip,
            confidence = 0.88f
        )
    }

    private fun isLine(points: List<PointF>, pathLength: Float): Boolean {
        if (points.size < 3) return true
        val start = points.first()
        val end = points.last()
        val chord = hypot(end.x - start.x, end.y - start.y)
        if (chord < 30f) return false

        // Linearity ratio: chord should be close to total path length
        if (chord < pathLength * 0.78f) return false

        val maxDeviation = maxDeviationFromSegment(points, start, end)
        return maxDeviation < max(28f, chord * 0.12f)
    }

    private fun maxDeviationFromSegment(points: List<PointF>, a: PointF, b: PointF): Float {
        var maxDev = 0f
        for (p in points) {
            val dist = pointLineDistance(p, a, b)
            if (dist > maxDev) maxDev = dist
        }
        return maxDev
    }

    fun applyLinePerfection(start: PointF, end: PointF): List<PointF> {
        val count = 10
        val list = ArrayList<PointF>(count + 1)
        for (i in 0..count) {
            val t = i.toFloat() / count
            list.add(PointF(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t))
        }
        return list
    }

    fun applyArrowPerfection(start: PointF, end: PointF): List<PointF> {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = hypot(dx, dy)
        if (length < 0.0001f) return listOf(start, end)

        val headLen = (length * 0.28f).coerceIn(24f, 72f)
        val angle = atan2(dy, dx)
        val barb1Angle = angle + PI - (PI / 7.2)
        val barb2Angle = angle + PI + (PI / 7.2)

        val barb1X = end.x + (headLen * cos(barb1Angle)).toFloat()
        val barb1Y = end.y + (headLen * sin(barb1Angle)).toFloat()
        val barb2X = end.x + (headLen * cos(barb2Angle)).toFloat()
        val barb2Y = end.y + (headLen * sin(barb2Angle)).toFloat()

        val points = mutableListOf<PointF>()

        // 1. Shaft line (multiple segments for stable rendering)
        val shaftSegments = 8
        for (i in 0..shaftSegments) {
            val t = i.toFloat() / shaftSegments
            points.add(PointF(start.x + dx * t, start.y + dy * t))
        }

        // 2. Barb 1
        points.add(PointF((end.x + barb1X) / 2f, (end.y + barb1Y) / 2f))
        points.add(PointF(barb1X, barb1Y))
        points.add(PointF((end.x + barb1X) / 2f, (end.y + barb1Y) / 2f))

        // 3. Retrace to arrow tip
        points.add(PointF(end.x, end.y))

        // 4. Barb 2
        points.add(PointF((end.x + barb2X) / 2f, (end.y + barb2Y) / 2f))
        points.add(PointF(barb2X, barb2Y))

        return points
    }

    fun applyOvalPerfection(bounds: RectF): List<PointF> {
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        val rx = bounds.width() / 2f
        val ry = bounds.height() / 2f

        val segments = 48
        val list = ArrayList<PointF>(segments + 1)
        for (i in 0..segments) {
            val theta = (i.toFloat() / segments) * 2f * PI
            list.add(PointF(cx + rx * cos(theta).toFloat(), cy + ry * sin(theta).toFloat()))
        }
        return list
    }

    fun applyRectanglePerfection(bounds: RectF): List<PointF> {
        val list = ArrayList<PointF>(5)
        list.add(PointF(bounds.left, bounds.top))
        list.add(PointF(bounds.right, bounds.top))
        list.add(PointF(bounds.right, bounds.bottom))
        list.add(PointF(bounds.left, bounds.bottom))
        list.add(PointF(bounds.left, bounds.top))
        return list
    }

    fun applyTrianglePerfection(corners: List<PointF>): List<PointF> {
        val list = mutableListOf<PointF>()
        for (i in corners.indices) {
            val p1 = corners[i]
            val p2 = corners[(i + 1) % corners.size]
            for (step in 0..6) {
                val t = step / 6f
                list.add(PointF(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t))
            }
        }
        return list
    }

    fun calculateBounds(points: List<PointF>): RectF {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (p in points) {
            minX = min(minX, p.x)
            minY = min(minY, p.y)
            maxX = max(maxX, p.x)
            maxY = max(maxY, p.y)
        }
        return RectF(minX, minY, maxX, maxY)
    }

    private fun computePathLength(points: List<PointF>): Float {
        var total = 0f
        for (i in 0 until points.size - 1) {
            total += hypot(points[i + 1].x - points[i].x, points[i + 1].y - points[i].y)
        }
        return total
    }

    private fun calculateCircularity(points: List<PointF>, perimeter: Float): Float {
        if (perimeter <= 0f || points.size < 4) return 0f
        // Shoelace formula for closed area
        var area2 = 0.0
        val n = points.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area2 += (points[i].x.toDouble() * points[j].y.toDouble()) - (points[j].x.toDouble() * points[i].y.toDouble())
        }
        val area = abs(area2) / 2.0
        // Circularity quotient: 4 * PI * Area / Perimeter^2
        val quotient = (4.0 * PI * area) / (perimeter.toDouble() * perimeter.toDouble())
        return quotient.toFloat()
    }

    private fun pointLineDistance(p: PointF, a: PointF, b: PointF): Float {
        val normalLength = hypot(b.x - a.x, b.y - a.y)
        if (normalLength < 1e-5f) {
            return hypot(p.x - a.x, p.y - a.y)
        }
        return abs((p.x - a.x) * (b.y - a.y) - (p.y - a.y) * (b.x - a.x)) / normalLength
    }

    private fun isPolygonClosed(points: List<PointF>): Boolean {
        if (points.size < 3) return false
        val first = points.first()
        val last = points.last()
        return hypot(first.x - last.x, first.y - last.y) < 80f
    }

    private fun getDistinctCorners(points: List<PointF>, targetCount: Int): List<PointF> {
        val distinct = mutableListOf<PointF>()
        for (p in points) {
            if (distinct.none { hypot(it.x - p.x, it.y - p.y) < 25f }) {
                distinct.add(p)
            }
        }
        return distinct.take(targetCount)
    }

    private fun douglasPeucker(points: List<PointF>, epsilon: Float): List<PointF> {
        if (points.size < 3) return points

        var dmax = 0f
        var index = 0
        val end = points.size - 1

        for (i in 1 until end) {
            val d = pointLineDistance(points[i], points[0], points[end])
            if (d > dmax) {
                index = i
                dmax = d
            }
        }

        return if (dmax > epsilon) {
            val recResults1 = douglasPeucker(points.subList(0, index + 1), epsilon)
            val recResults2 = douglasPeucker(points.subList(index, end + 1), epsilon)
            val result = mutableListOf<PointF>()
            result.addAll(recResults1.dropLast(1))
            result.addAll(recResults2)
            result
        } else {
            listOf(points[0], points[end])
        }
    }
}
