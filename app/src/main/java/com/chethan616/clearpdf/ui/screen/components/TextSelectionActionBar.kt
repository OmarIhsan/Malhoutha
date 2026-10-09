package com.chethan616.clearpdf.ui.screen.components

import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.chethan616.clearpdf.medical.ui.QuickTranslationPill
import com.chethan616.clearpdf.medical.viewmodel.QuickTranslationState

/**
 * Standardized ClearPDF contextual selection menu item.
 * Preserves exact icon sizing (20dp), horizontal padding, and unified onSurface theme tint.
 */
@Composable
fun SelectionMenuItem(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * ClearPDF contextual text selection action bar with unified native styling.
 * Retains ClearPDF's exact action bar architecture, layout dimensions, elevation,
 * and icon button styling, while seamlessly integrating a first-class "Translate" action button.
 */
@Composable
fun TextSelectionActionBar(
    onCopyClick: () -> Unit,
    onHighlightClick: () -> Unit,
    onTranslateClick: () -> Unit,
    onAddNoteClick: () -> Unit,
    onShareClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier.wrapContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SelectionMenuItem(
                icon = Icons.Rounded.ContentCopy,
                label = "Copy",
                contentDescription = "Copy selected text",
                onClick = onCopyClick
            )
            SelectionMenuItem(
                icon = Icons.Rounded.Highlight,
                label = "Highlight",
                contentDescription = "Highlight selected text",
                onClick = onHighlightClick
            )
            SelectionMenuItem(
                icon = Icons.Rounded.Translate,
                label = "Translate",
                contentDescription = "Translate selected text",
                onClick = onTranslateClick
            )
            SelectionMenuItem(
                icon = Icons.AutoMirrored.Rounded.NoteAdd,
                label = "Note",
                contentDescription = "Add note for selected text",
                onClick = onAddNoteClick
            )
            if (onShareClick != null) {
                SelectionMenuItem(
                    icon = Icons.Rounded.Share,
                    label = "Share",
                    contentDescription = "Share selected text",
                    onClick = onShareClick
                )
            }
        }
    }
}

/**
 * Overload matching the Step 2 selection container contract.
 */
@Composable
fun TextSelectionActionBar(
    bounds: RectF?,
    onCopy: () -> Unit,
    onHighlight: () -> Unit,
    onTranslate: () -> Unit,
    onAddNote: () -> Unit,
    onShare: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TextSelectionActionBar(
        onCopyClick = onCopy,
        onHighlightClick = onHighlight,
        onTranslateClick = onTranslate,
        onAddNoteClick = onAddNote,
        onShareClick = onShare,
        modifier = modifier
    )
}

/**
 * Calculates synchronized dual-layer coordinates for Stacked Quick Translation Pill
 * and ClearPDF Selection Menu (Step 2 Selection Redesign).
 *
 * Invariants:
 * - When placed ABOVE: Menu is directly above selection; Pill sits directly above Menu.
 * - When flipped BELOW: Entire stack flips together (Pill on top, Menu underneath) without
 *   overlapping each other or obscuring text/selection handles.
 * - Horizontal clamping prevents either element from clipping outside viewport margins.
 */
fun calculateStackedSelectionCoordinates(
    selectionBounds: RectF,
    viewportWidth: Float,
    viewportHeight: Float,
    density: Density,
    menuWidthDp: Dp = 290.dp,
    menuHeightDp: Dp = 48.dp,
    pillWidthDp: Dp = 240.dp,
    pillHeightDp: Dp = 44.dp,
    edgePaddingDp: Dp = 16.dp,
    selectionGapDp: Dp = 8.dp,
    stackGapDp: Dp = 8.dp,
    handleDropDp: Dp = 26.dp
): Pair<IntOffset, IntOffset> {
    val menuWidthPx = with(density) { menuWidthDp.toPx() }
    val menuHeightPx = with(density) { menuHeightDp.toPx() }
    val pillWidthPx = with(density) { pillWidthDp.toPx() }
    val pillHeightPx = with(density) { pillHeightDp.toPx() }
    val edgePaddingPx = with(density) { edgePaddingDp.toPx() }
    val selectionGapPx = with(density) { selectionGapDp.toPx() }
    val stackGapPx = with(density) { stackGapDp.toPx() }
    val handleDropPx = with(density) { handleDropDp.toPx() }

    val totalStackHeightPx = pillHeightPx + stackGapPx + menuHeightPx

    // Check if entire stack fits above the text selection
    val fitsAbove = (selectionBounds.top - selectionGapPx - totalStackHeightPx) >= edgePaddingPx

    val pillY: Float
    val menuY: Float

    if (fitsAbove) {
        // Above: Menu directly above selection, Pill stacked directly above Menu
        menuY = selectionBounds.top - selectionGapPx - menuHeightPx
        pillY = menuY - stackGapPx - pillHeightPx
    } else {
        // Below: Entire stack flips below text & handles. Pill on top, Menu underneath.
        val topOfStackBelow = selectionBounds.bottom + handleDropPx + selectionGapPx
        pillY = topOfStackBelow.coerceIn(
            edgePaddingPx,
            (viewportHeight - totalStackHeightPx - edgePaddingPx).coerceAtLeast(edgePaddingPx)
        )
        menuY = pillY + pillHeightPx + stackGapPx
    }

    // Horizontal clamping within [edgePadding, viewportWidth - width - edgePadding]
    val pillTargetX = selectionBounds.centerX() - (pillWidthPx / 2f)
    val pillX = pillTargetX.coerceIn(
        edgePaddingPx,
        (viewportWidth - pillWidthPx - edgePaddingPx).coerceAtLeast(edgePaddingPx)
    )

    val menuTargetX = selectionBounds.centerX() - (menuWidthPx / 2f)
    val menuX = menuTargetX.coerceIn(
        edgePaddingPx,
        (viewportWidth - menuWidthPx - edgePaddingPx).coerceAtLeast(edgePaddingPx)
    )

    val pillOffset = IntOffset(pillX.roundToInt(), pillY.roundToInt())
    val menuOffset = IntOffset(menuX.roundToInt(), menuY.roundToInt())

    return Pair(pillOffset, menuOffset)
}

fun calculateStackedPillOffset(
    selectionBounds: RectF,
    viewportWidth: Float,
    viewportHeight: Float,
    density: Density,
    menuWidthDp: Dp = 290.dp,
    menuHeightDp: Dp = 48.dp,
    pillWidthDp: Dp = 240.dp,
    pillHeightDp: Dp = 44.dp,
    edgePaddingDp: Dp = 16.dp,
    selectionGapDp: Dp = 8.dp,
    stackGapDp: Dp = 8.dp,
    handleDropDp: Dp = 26.dp
): IntOffset {
    return calculateStackedSelectionCoordinates(
        selectionBounds = selectionBounds,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        density = density,
        menuWidthDp = menuWidthDp,
        menuHeightDp = menuHeightDp,
        pillWidthDp = pillWidthDp,
        pillHeightDp = pillHeightDp,
        edgePaddingDp = edgePaddingDp,
        selectionGapDp = selectionGapDp,
        stackGapDp = stackGapDp,
        handleDropDp = handleDropDp
    ).first
}

fun calculateMenuOffset(
    selectionBounds: RectF,
    viewportWidth: Float,
    viewportHeight: Float,
    density: Density,
    menuWidthDp: Dp = 290.dp,
    menuHeightDp: Dp = 48.dp,
    pillWidthDp: Dp = 240.dp,
    pillHeightDp: Dp = 44.dp,
    edgePaddingDp: Dp = 16.dp,
    selectionGapDp: Dp = 8.dp,
    stackGapDp: Dp = 8.dp,
    handleDropDp: Dp = 26.dp
): IntOffset {
    return calculateStackedSelectionCoordinates(
        selectionBounds = selectionBounds,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        density = density,
        menuWidthDp = menuWidthDp,
        menuHeightDp = menuHeightDp,
        pillWidthDp = pillWidthDp,
        pillHeightDp = pillHeightDp,
        edgePaddingDp = edgePaddingDp,
        selectionGapDp = selectionGapDp,
        stackGapDp = stackGapDp,
        handleDropDp = handleDropDp
    ).second
}

/**
 * Deterministically placed and clamped Selection Action Bar overlay.
 * Clamps within viewport margins (16.dp edge padding) and flips vertically
 * above/below the selection handles without occluding the highlighted text stream.
 * Supports dual-layer stacked QuickTranslationPill directly above the menu.
 */
@Composable
fun TextSelectionActionBarOverlay(
    visible: Boolean,
    selectionBounds: RectF?,
    onCopyClick: () -> Unit,
    onHighlightClick: () -> Unit,
    onTranslateClick: () -> Unit,
    onAddNoteClick: () -> Unit,
    onShareClick: (() -> Unit)? = null,
    quickTranslationState: QuickTranslationState = QuickTranslationState(),
    onPlayAudio: (() -> Unit)? = null,
    onOpenDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (!visible || selectionBounds == null) return

    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val screenW = maxWidth.value * density.density
        val screenH = maxHeight.value * density.density

        val (pillOffset, menuOffset) = remember(selectionBounds, screenW, screenH, density) {
            calculateStackedSelectionCoordinates(
                selectionBounds = selectionBounds,
                viewportWidth = screenW,
                viewportHeight = screenH,
                density = density,
                menuWidthDp = if (onShareClick != null) 360.dp else 290.dp
            )
        }

        val estimatedMenuWidthPx = with(density) { (if (onShareClick != null) 360.dp else 290.dp).toPx() }
        val originX = if (estimatedMenuWidthPx > 0f) {
            ((selectionBounds.centerX() - menuOffset.x) / estimatedMenuWidthPx).coerceIn(0f, 1f)
        } else 0.5f
        val originY = if (menuOffset.y < selectionBounds.top) 1f else 0f

        // Layer 1: Stacked Quick Translation Pill
        if (quickTranslationState.hasTranslation || quickTranslationState.isLoading) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn() + scaleIn(
                    initialScale = 0.92f,
                    transformOrigin = TransformOrigin(originX, originY)
                ),
                exit = fadeOut() + scaleOut(
                    targetScale = 0.92f,
                    transformOrigin = TransformOrigin(originX, originY)
                ),
                modifier = Modifier.offset { pillOffset }
            ) {
                QuickTranslationPill(
                    arabicTranslation = quickTranslationState.arabicText,
                    isLoading = quickTranslationState.isLoading,
                    clinicalDomain = quickTranslationState.clinicalDomain,
                    onPlayAudio = { onPlayAudio?.invoke() },
                    onOpenDetails = { onOpenDetails?.invoke() ?: onTranslateClick() }
                )
            }
        }

        // Layer 2: ClearPDF Selection Action Bar
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(
                initialScale = 0.92f,
                transformOrigin = TransformOrigin(originX, originY)
            ),
            exit = fadeOut() + scaleOut(
                targetScale = 0.92f,
                transformOrigin = TransformOrigin(originX, originY)
            ),
            modifier = Modifier.offset { menuOffset }
        ) {
            TextSelectionActionBar(
                onCopyClick = onCopyClick,
                onHighlightClick = onHighlightClick,
                onTranslateClick = onTranslateClick,
                onAddNoteClick = onAddNoteClick,
                onShareClick = onShareClick
            )
        }
    }
}

