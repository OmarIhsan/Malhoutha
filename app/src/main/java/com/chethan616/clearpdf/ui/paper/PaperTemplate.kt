package com.chethan616.clearpdf.ui.paper

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

/**
 * Paper template types for synthetic vector notes.
 */
enum class PaperTemplateType {
    PLAIN,
    RULED_COLLEGE,   // 7.1mm / ~24dp standard college line spacing + left margin rule
    RULED_WIDE,      // 8.7mm / ~30dp wide line spacing
    GRID_5MM,        // 5mm x 5mm engineering/math grid
    DOT_MATRIX,      // 5mm spaced subtle dot grid
    CORNELL          // Cornell notes layout (cue column 30% width, summary footer 20% height)
}

/**
 * Specification and color configuration for procedural synthetic paper.
 */
data class PaperConfig(
    val type: PaperTemplateType,
    val baseColor: Color = Color(0xFFFCFDFD),
    val lineRuleColor: Color = Color(0xFFE2E8F0),
    val marginRuleColor: Color = Color(0xFFFFB4AB), // Subtle reddish-pink for vertical margins
    val gridSpacingDp: Float = 24f
)

/**
 * Curated tactile color substrate presets for note documents.
 */
enum class PaperColorPreset(
    val displayName: String,
    val baseColor: Color,
    val lineRuleColor: Color,
    val marginRuleColor: Color
) {
    WHITE(
        displayName = "White",
        baseColor = Color(0xFFFCFDFD),
        lineRuleColor = Color(0xFFE2E8F0),
        marginRuleColor = Color(0xFFFFB4AB)
    ),
    IVORY(
        displayName = "Ivory",
        baseColor = Color(0xFFFBF8EE),
        lineRuleColor = Color(0xFFE5DECE),
        marginRuleColor = Color(0xFFE8988E)
    ),
    LEGAL_YELLOW(
        displayName = "Legal Pad",
        baseColor = Color(0xFFFFFBE8),
        lineRuleColor = Color(0xFFE8DFB8),
        marginRuleColor = Color(0xFFE07A70)
    ),
    SAGE(
        displayName = "Sage",
        baseColor = Color(0xFFF2F7F4),
        lineRuleColor = Color(0xFFD4E2D8),
        marginRuleColor = Color(0xFFCC858A)
    ),
    DARK_SLATE(
        displayName = "Dark Slate",
        baseColor = Color(0xFF1E222A),
        lineRuleColor = Color(0xFF2C323E),
        marginRuleColor = Color(0xFF7E3B45)
    );

    fun applyTo(type: PaperTemplateType, spacingDp: Float? = null): PaperConfig {
        val defaultSpacing = when (type) {
            PaperTemplateType.PLAIN -> 24f
            PaperTemplateType.RULED_COLLEGE -> 24f
            PaperTemplateType.RULED_WIDE -> 30f
            PaperTemplateType.GRID_5MM -> 18f
            PaperTemplateType.DOT_MATRIX -> 18f
            PaperTemplateType.CORNELL -> 24f
        }
        return PaperConfig(
            type = type,
            baseColor = baseColor,
            lineRuleColor = lineRuleColor,
            marginRuleColor = marginRuleColor,
            gridSpacingDp = spacingDp ?: defaultSpacing
        )
    }
}

/**
 * Procedural synthetic paper rendering inside Compose DrawScope.
 * Infinitely sharp at any zoom level, drawn with subpixel vector precision.
 */
fun DrawScope.drawSyntheticPaper(config: PaperConfig) {
    // 1. Base paper substrate fill
    drawRect(color = config.baseColor, topLeft = Offset.Zero, size = size)

    when (config.type) {
        PaperTemplateType.PLAIN -> {
            // Pure clean sheet
        }

        PaperTemplateType.RULED_COLLEGE -> {
            val spacingPx = (if (config.gridSpacingDp > 0f) config.gridSpacingDp else 24f).dp.toPx()
            val strokeW = 1.dp.toPx()
            val marginStrokeW = 1.25.dp.toPx()
            val marginX = (size.width * 0.12f).coerceIn(32.dp.toPx(), 72.dp.toPx())
            val headerY = (size.height * 0.08f).coerceIn(40.dp.toPx(), 70.dp.toPx())

            // Left vertical margin rule
            drawLine(
                color = config.marginRuleColor,
                start = Offset(marginX, 0f),
                end = Offset(marginX, size.height),
                strokeWidth = marginStrokeW
            )

            // Horizontal rules
            var y = headerY
            while (y < size.height - spacingPx * 0.25f) {
                drawLine(
                    color = config.lineRuleColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeW
                )
                y += spacingPx
            }
        }

        PaperTemplateType.RULED_WIDE -> {
            val spacingPx = (if (config.gridSpacingDp > 0f) config.gridSpacingDp else 30f).dp.toPx()
            val strokeW = 1.dp.toPx()
            val marginStrokeW = 1.25.dp.toPx()
            val marginX = (size.width * 0.12f).coerceIn(32.dp.toPx(), 72.dp.toPx())
            val headerY = (size.height * 0.08f).coerceIn(44.dp.toPx(), 76.dp.toPx())

            // Left vertical margin rule
            drawLine(
                color = config.marginRuleColor,
                start = Offset(marginX, 0f),
                end = Offset(marginX, size.height),
                strokeWidth = marginStrokeW
            )

            // Horizontal rules
            var y = headerY
            while (y < size.height - spacingPx * 0.25f) {
                drawLine(
                    color = config.lineRuleColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeW
                )
                y += spacingPx
            }
        }

        PaperTemplateType.GRID_5MM -> {
            val spacingPx = (if (config.gridSpacingDp > 0f) config.gridSpacingDp else 18f).dp.toPx()
            val strokeW = 0.85.dp.toPx()

            // Vertical grid lines
            var x = spacingPx
            while (x < size.width) {
                drawLine(
                    color = config.lineRuleColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = strokeW
                )
                x += spacingPx
            }

            // Horizontal grid lines
            var y = spacingPx
            while (y < size.height) {
                drawLine(
                    color = config.lineRuleColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeW
                )
                y += spacingPx
            }
        }

        PaperTemplateType.DOT_MATRIX -> {
            val spacingPx = (if (config.gridSpacingDp > 0f) config.gridSpacingDp else 18f).dp.toPx()
            val dotRadius = 1.25.dp.toPx()

            var x = spacingPx
            while (x < size.width) {
                var y = spacingPx
                while (y < size.height) {
                    drawCircle(
                        color = config.lineRuleColor,
                        radius = dotRadius,
                        center = Offset(x, y)
                    )
                    y += spacingPx
                }
                x += spacingPx
            }
        }

        PaperTemplateType.CORNELL -> {
            val spacingPx = (if (config.gridSpacingDp > 0f) config.gridSpacingDp else 24f).dp.toPx()
            val strokeW = 1.dp.toPx()
            val dividerStrokeW = 1.5.dp.toPx()

            val headerY = (size.height * 0.08f).coerceIn(44.dp.toPx(), 72.dp.toPx())
            val summaryY = size.height * 0.80f
            val cueX = size.width * 0.30f

            // Cornell Dividers
            // Top header line
            drawLine(
                color = config.marginRuleColor,
                start = Offset(0f, headerY),
                end = Offset(size.width, headerY),
                strokeWidth = dividerStrokeW
            )
            // Vertical cue separator
            drawLine(
                color = config.marginRuleColor,
                start = Offset(cueX, headerY),
                end = Offset(cueX, summaryY),
                strokeWidth = dividerStrokeW
            )
            // Bottom summary separator
            drawLine(
                color = config.marginRuleColor,
                start = Offset(0f, summaryY),
                end = Offset(size.width, summaryY),
                strokeWidth = dividerStrokeW
            )

            // Notes area horizontal rules (right of cue column, between header and summary)
            var y = headerY + spacingPx
            while (y < summaryY - spacingPx * 0.25f) {
                drawLine(
                    color = config.lineRuleColor,
                    start = Offset(cueX, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeW
                )
                y += spacingPx
            }

            // Summary area horizontal rules
            y = summaryY + spacingPx
            while (y < size.height - spacingPx * 0.25f) {
                drawLine(
                    color = config.lineRuleColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeW
                )
                y += spacingPx
            }
        }
    }
}

/**
 * Scaled mini procedural preview for template selector cards and thumbnails.
 */
fun DrawScope.drawPaperPreview(config: PaperConfig) {
    drawRect(color = config.baseColor, topLeft = Offset.Zero, size = size)

    when (config.type) {
        PaperTemplateType.PLAIN -> {}

        PaperTemplateType.RULED_COLLEGE, PaperTemplateType.RULED_WIDE -> {
            val spacing = size.height / if (config.type == PaperTemplateType.RULED_COLLEGE) 10f else 8f
            val marginX = size.width * 0.16f
            val headerY = spacing * 1.5f

            drawLine(
                color = config.marginRuleColor,
                start = Offset(marginX, 0f),
                end = Offset(marginX, size.height),
                strokeWidth = 1.2f
            )

            var y = headerY
            while (y < size.height - 2f) {
                drawLine(
                    color = config.lineRuleColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += spacing
            }
        }

        PaperTemplateType.GRID_5MM -> {
            val spacing = size.width / 6f
            var x = spacing
            while (x < size.width) {
                drawLine(
                    color = config.lineRuleColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 0.8f
                )
                x += spacing
            }
            var y = spacing
            while (y < size.height) {
                drawLine(
                    color = config.lineRuleColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 0.8f
                )
                y += spacing
            }
        }

        PaperTemplateType.DOT_MATRIX -> {
            val spacing = size.width / 6f
            var x = spacing
            while (x < size.width) {
                var y = spacing
                while (y < size.height) {
                    drawCircle(
                        color = config.lineRuleColor,
                        radius = 1.2f,
                        center = Offset(x, y)
                    )
                    y += spacing
                }
                x += spacing
            }
        }

        PaperTemplateType.CORNELL -> {
            val headerY = size.height * 0.15f
            val summaryY = size.height * 0.75f
            val cueX = size.width * 0.32f

            drawLine(config.marginRuleColor, Offset(0f, headerY), Offset(size.width, headerY), 1.2f)
            drawLine(config.marginRuleColor, Offset(cueX, headerY), Offset(cueX, summaryY), 1.2f)
            drawLine(config.marginRuleColor, Offset(0f, summaryY), Offset(size.width, summaryY), 1.2f)

            val spacing = (summaryY - headerY) / 5f
            var y = headerY + spacing
            while (y < summaryY - 2f) {
                drawLine(config.lineRuleColor, Offset(cueX, y), Offset(size.width, y), 0.9f)
                y += spacing
            }
        }
    }
}

/**
 * Procedural synthetic paper rendering on an Android Graphics Canvas.
 * Used when generating standalone PDF pages so that the PDF contains native vector lines.
 */
fun android.graphics.Canvas.drawSyntheticPaper(
    config: PaperConfig,
    width: Float,
    height: Float,
    density: Float = 1f
) {
    // 1. Draw base color
    drawColor(config.baseColor.toArgb())

    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.lineRuleColor.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }

    val marginPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.marginRuleColor.toArgb()
        style = Paint.Style.STROKE
        strokeWidth = 1.35f * density
    }

    when (config.type) {
        PaperTemplateType.PLAIN -> {}

        PaperTemplateType.RULED_COLLEGE -> {
            val spacing = (if (config.gridSpacingDp > 0f) config.gridSpacingDp else 24f) * density
            val marginX = (width * 0.12f).coerceIn(32f * density, 72f * density)
            val headerY = (height * 0.08f).coerceIn(40f * density, 70f * density)

            drawLine(marginX, 0f, marginX, height, marginPaint)

            var y = headerY
            while (y < height - spacing * 0.25f) {
                drawLine(0f, y, width, y, linePaint)
                y += spacing
            }
        }

        PaperTemplateType.RULED_WIDE -> {
            val spacing = (if (config.gridSpacingDp > 0f) config.gridSpacingDp else 30f) * density
            val marginX = (width * 0.12f).coerceIn(32f * density, 72f * density)
            val headerY = (height * 0.08f).coerceIn(44f * density, 76f * density)

            drawLine(marginX, 0f, marginX, height, marginPaint)

            var y = headerY
            while (y < height - spacing * 0.25f) {
                drawLine(0f, y, width, y, linePaint)
                y += spacing
            }
        }

        PaperTemplateType.GRID_5MM -> {
            val spacing = (if (config.gridSpacingDp > 0f) config.gridSpacingDp else 18f) * density

            var x = spacing
            while (x < width) {
                drawLine(x, 0f, x, height, linePaint)
                x += spacing
            }

            var y = spacing
            while (y < height) {
                drawLine(0f, y, width, y, linePaint)
                y += spacing
            }
        }

        PaperTemplateType.DOT_MATRIX -> {
            val spacing = (if (config.gridSpacingDp > 0f) config.gridSpacingDp else 18f) * density
            val dotRadius = 1.25f * density
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = config.lineRuleColor.toArgb()
                style = Paint.Style.FILL
            }

            var x = spacing
            while (x < width) {
                var y = spacing
                while (y < height) {
                    drawCircle(x, y, dotRadius, dotPaint)
                    y += spacing
                }
                x += spacing
            }
        }

        PaperTemplateType.CORNELL -> {
            val spacing = (if (config.gridSpacingDp > 0f) config.gridSpacingDp else 24f) * density
            val headerY = (height * 0.08f).coerceIn(44f * density, 72f * density)
            val summaryY = height * 0.80f
            val cueX = width * 0.30f

            drawLine(0f, headerY, width, headerY, marginPaint)
            drawLine(cueX, headerY, cueX, summaryY, marginPaint)
            drawLine(0f, summaryY, width, summaryY, marginPaint)

            var y = headerY + spacing
            while (y < summaryY - spacing * 0.25f) {
                drawLine(cueX, y, width, y, linePaint)
                y += spacing
            }

            y = summaryY + spacing
            while (y < height - spacing * 0.25f) {
                drawLine(0f, y, width, y, linePaint)
                y += spacing
            }
        }
    }
}
