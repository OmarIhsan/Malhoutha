package com.malhoutha.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.malhoutha.ui.theme.GlassLevel
import com.malhoutha.ui.theme.GlassTokens

/**
 * Universal Frosted Glass Card for lecture cards, document summaries, and list items.
 *
 * Provides calibrated luminance, specular rim highlighting, and isolated background
 * styling without blurring child content or leaking blur to parent surfaces.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.ELEVATED_CARD,
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp? = null,
    isDark: Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = GlassTokens.resolve(level, isDark)
    val actualBorderWidth = borderWidth ?: tokens.borderWidth

    val baseModifier = if (tokens.elevation > 0.dp && tokens.ambientShadowColor != Color.Transparent) {
        modifier.shadow(
            elevation = tokens.elevation,
            shape = shape,
            ambientColor = tokens.ambientShadowColor,
            spotColor = tokens.ambientShadowColor
        )
    } else {
        modifier
    }

    val borderModifier = if (actualBorderWidth > 0.dp) {
        if (tokens.borderBrush != null) {
            Modifier.border(
                BorderStroke(actualBorderWidth, tokens.borderBrush),
                shape = shape
            )
        } else if (tokens.borderColor != Color.Transparent) {
            Modifier.border(
                BorderStroke(actualBorderWidth, tokens.borderColor),
                shape = shape
            )
        } else {
            Modifier
        }
    } else {
        Modifier
    }

    Box(
        modifier = baseModifier
            .clip(shape)
            .then(borderModifier)
    ) {
        // Isolated Background Plane: provides calibrated translucent tint without blurring children or bleeding
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(tokens.containerColor, shape)
        )

        // Foreground Content: rendered crisp and unblurred
        content()
    }
}

/**
 * Low-level Frosted Glass Surface for toolbars, docked rails, and custom layout panels.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    level: GlassLevel = GlassLevel.BASE_RAIL,
    shape: Shape = RoundedCornerShape(16.dp),
    isDark: Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
    content: @Composable () -> Unit
) {
    val tokens = GlassTokens.resolve(level, isDark)

    val baseModifier = if (tokens.elevation > 0.dp && tokens.ambientShadowColor != Color.Transparent) {
        modifier.shadow(
            elevation = tokens.elevation,
            shape = shape,
            ambientColor = tokens.ambientShadowColor,
            spotColor = tokens.ambientShadowColor
        )
    } else {
        modifier
    }

    val borderModifier = if (tokens.borderWidth > 0.dp) {
        if (tokens.borderBrush != null) {
            Modifier.border(
                BorderStroke(tokens.borderWidth, tokens.borderBrush),
                shape = shape
            )
        } else if (tokens.borderColor != Color.Transparent) {
            Modifier.border(
                BorderStroke(tokens.borderWidth, tokens.borderColor),
                shape = shape
            )
        } else {
            Modifier
        }
    } else {
        Modifier
    }

    Box(
        modifier = baseModifier
            .clip(shape)
            .then(borderModifier)
    ) {
        // Isolated Background Plane
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .background(tokens.containerColor, shape)
        )

        // Foreground Content: rendered crisp and unblurred
        content()
    }
}

/**
 * Frosted Glass Floating Bar for docked stylus controls, page jumper capsules,
 * and persistent contextual action bars.
 */
@Composable
fun GlassFloatingBar(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    isDark: Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
    content: @Composable BoxScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        level = GlassLevel.FLOATING_DOCK,
        shape = shape,
        isDark = isDark,
        content = content
    )
}

/**
 * Frosted Glass Dialog Sheet for modal tooltips, lexicon definitions,
 * and slide-over study reference panels.
 */
@Composable
fun GlassDialogSheet(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    isDark: Boolean = MaterialTheme.colorScheme.surface.luminance() < 0.5f,
    content: @Composable BoxScope.() -> Unit
) {
    GlassCard(
        modifier = modifier,
        level = GlassLevel.MODAL_TOOLTIP,
        shape = shape,
        isDark = isDark,
        content = content
    )
}
