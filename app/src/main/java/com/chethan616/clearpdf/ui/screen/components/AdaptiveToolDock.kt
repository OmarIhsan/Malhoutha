package com.chethan616.clearpdf.ui.screen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chethan616.clearpdf.ui.screen.PdfEditTool
import com.malhoutha.ui.adaptive.Handedness
import com.malhoutha.ui.components.GlassCard
import com.malhoutha.ui.theme.GlassLevel

/**
 * Ergonomic Handedness-Aware Floating & Detachable Annotation Dock for Tablets.
 *
 * Prevents palm-rejection failures:
 * - If user is a Right-Handed writer, dock sits along the Left screen margin.
 * - If user is a Left-Handed writer, dock sits along the Right screen margin.
 * - Includes one-tap handedness flipping and pen attributes popover.
 */
@Composable
internal fun AdaptiveToolDock(
    activeTool: PdfEditTool,
    onToolSelected: (PdfEditTool) -> Unit,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    currentStrokeWidth: Float,
    onStrokeWidthSelected: (Float) -> Unit,
    canUndo: Boolean = false,
    onUndo: () -> Unit = {},
    canRedo: Boolean = false,
    onRedo: () -> Unit = {},
    handedness: Handedness = Handedness.RIGHT_HANDED_WRITER,
    onToggleHandedness: (() -> Unit)? = null,
    onCloseDock: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showAttributesPopover by remember { mutableStateOf(false) }

    GlassCard(
        level = GlassLevel.FLOATING_DOCK,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier
            .width(68.dp)
            .padding(vertical = 10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            // Hand / Text Selection
            ToolbarIconButton(
                icon = Icons.Rounded.TouchApp,
                contentDescription = "Select / Text Hand",
                isSelected = activeTool == PdfEditTool.None,
                onClick = { onToolSelected(PdfEditTool.None) }
            )

            // Pen Inking
            ToolbarIconButton(
                icon = Icons.Rounded.Edit,
                contentDescription = "Fountain Pen",
                isSelected = activeTool == PdfEditTool.Draw,
                onClick = {
                    if (activeTool == PdfEditTool.Draw) {
                        showAttributesPopover = !showAttributesPopover
                    } else {
                        onToolSelected(PdfEditTool.Draw)
                    }
                }
            )

            // Highlighter
            ToolbarIconButton(
                icon = Icons.Rounded.Highlight,
                contentDescription = "Highlighter",
                isSelected = activeTool == PdfEditTool.Highlight,
                onClick = {
                    if (activeTool == PdfEditTool.Highlight) {
                        showAttributesPopover = !showAttributesPopover
                    } else {
                        onToolSelected(PdfEditTool.Highlight)
                    }
                }
            )

            // Eraser
            ToolbarIconButton(
                icon = Icons.Rounded.CleaningServices,
                contentDescription = "Precision Eraser",
                isSelected = activeTool == PdfEditTool.Eraser,
                onClick = { onToolSelected(PdfEditTool.Eraser) }
            )

            // Laser Pointer
            ToolbarIconButton(
                icon = Icons.Rounded.NearMe,
                contentDescription = "Laser Pointer",
                isSelected = activeTool == PdfEditTool.Laser,
                onClick = { onToolSelected(PdfEditTool.Laser) }
            )

            // Medical Sticky Note
            ToolbarIconButton(
                icon = Icons.Rounded.EditNote,
                contentDescription = "Medical Sticky Note",
                isSelected = activeTool == PdfEditTool.StickyNote,
                onClick = { onToolSelected(PdfEditTool.StickyNote) }
            )

            Spacer(Modifier.height(2.dp))

            // Quick Color & Stroke Pip with inline attributes popover
            Box {
                Surface(
                    onClick = { showAttributesPopover = !showAttributesPopover },
                    shape = CircleShape,
                    color = currentColor,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(34.dp)
                ) {}

                DropdownMenu(
                    expanded = showAttributesPopover,
                    onDismissRequest = { showAttributesPopover = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    PenAttributesPopoverContent(
                        currentColor = currentColor,
                        onColorSelected = {
                            onColorSelected(it)
                            showAttributesPopover = false
                        },
                        currentStrokeWidth = currentStrokeWidth,
                        onStrokeWidthSelected = {
                            onStrokeWidthSelected(it)
                            showAttributesPopover = false
                        }
                    )
                }
            }

            Spacer(Modifier.height(2.dp))
            HorizontalDivider(
                modifier = Modifier.width(36.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(2.dp))

            // Undo
            ToolbarIconButton(
                icon = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = "Undo",
                onClick = onUndo
            )

            // Redo
            ToolbarIconButton(
                icon = Icons.AutoMirrored.Rounded.Redo,
                contentDescription = "Redo",
                onClick = onRedo
            )

            // Handedness Switcher (Swap Left / Right Screen Edge)
            if (onToggleHandedness != null) {
                ToolbarIconButton(
                    icon = Icons.Rounded.SwapHoriz,
                    contentDescription = if (handedness == Handedness.RIGHT_HANDED_WRITER)
                        "Right-Handed Mode (Dock Left)" else "Left-Handed Mode (Dock Right)",
                    badgeText = if (handedness == Handedness.RIGHT_HANDED_WRITER) "R" else "L",
                    onClick = onToggleHandedness
                )
            }

            // Close / Repin to Top Bar
            if (onCloseDock != null) {
                ToolbarIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Pin Dock to Top Bar",
                    onClick = onCloseDock
                )
            }
        }
    }
}
