package com.chethan616.clearpdf.ui.screen.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chethan616.clearpdf.ui.screen.PdfEditTool

/**
 * Vertical Floating / Detachable Annotation Tool Dock for Tablet Ergonomics.
 *
 * Places primary inking and note-taking triggers along the left or right margin,
 * keeping the top edge completely unobstructed and aligning directly with the student's thumb.
 */
@Composable
internal fun AnnotationToolDock(
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
    modifier: Modifier = Modifier
) {
    var showAttributesPopover by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 8.dp,
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp), spotColor = Color(0x66000000))
            .width(68.dp)
            .padding(vertical = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 6.dp)
        ) {
            // Hand / Text Selection
            ToolbarIconButton(
                icon = Icons.Rounded.TouchApp,
                contentDescription = "Select / Hand",
                isSelected = activeTool == PdfEditTool.None,
                onClick = { onToolSelected(PdfEditTool.None) }
            )

            // Pen
            ToolbarIconButton(
                icon = Icons.Rounded.Edit,
                contentDescription = "Pen",
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
                contentDescription = "Eraser",
                isSelected = activeTool == PdfEditTool.Eraser,
                onClick = { onToolSelected(PdfEditTool.Eraser) }
            )

            // Laser Pointer
            ToolbarIconButton(
                icon = Icons.Rounded.NearMe,
                contentDescription = "Laser",
                isSelected = activeTool == PdfEditTool.Laser,
                onClick = { onToolSelected(PdfEditTool.Laser) }
            )

            // Sticky Note Dropper
            ToolbarIconButton(
                icon = Icons.Rounded.EditNote,
                contentDescription = "Sticky Note",
                isSelected = activeTool == PdfEditTool.StickyNote,
                onClick = { onToolSelected(PdfEditTool.StickyNote) }
            )

            Spacer(Modifier.height(4.dp))

            // Quick Color Pip
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

            Spacer(Modifier.height(4.dp))

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
        }
    }
}
