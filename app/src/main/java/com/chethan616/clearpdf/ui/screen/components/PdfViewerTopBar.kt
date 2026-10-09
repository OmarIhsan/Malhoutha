package com.chethan616.clearpdf.ui.screen.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chethan616.clearpdf.ui.screen.PdfEditTool

/**
 * 6 Standard High-Contrast Clinical Study Colors.
 */
val ClinicalColorPalette = listOf(
    Color(0xFFE53935) to "Medical Red",
    Color(0xFF1E88E5) to "Margin Blue",
    Color(0xFFFB8C00) to "Dental Amber",
    Color(0xFF43A047) to "Histology Mint",
    Color(0xFF212121) to "Ink Black",
    Color(0xFFFFD600) to "Highlight Yellow"
)

/**
 * Stroke Width Presets (Thin, Regular, Bold, Marker).
 */
val StrokeWidthPresets = listOf(
    1.5f to "Thin",
    3.0f to "Regular",
    6.0f to "Bold",
    12.0f to "Marker"
)

/**
 * Ergonomic Stylus-Friendly Icon Button with enlarged 56dp × 56dp touch bounds,
 * high-contrast active state indicators, and 26-28dp vector assets.
 */
@Composable
fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    isSelected: Boolean = false,
    badgeText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val backgroundColor = if (isSelected) {
        if (isDark) Color(0x3800897B) else Color(0x2800897B)
    } else {
        Color.Transparent
    }
    val iconTint = if (isSelected) {
        if (isDark) Color(0xFF4DB6AC) else Color(0xFF00796B)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = if (isSelected) BorderStroke(1.dp, if (isDark) Color(0x6600897B) else Color(0x4000897B)) else null,
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
            if (badgeText != null) {
                Badge(
                    containerColor = Color(0xFF26A69A),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(badgeText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Production-Grade Expanded Top Command Bar for Tablet Study & Stylus Workflows.
 */
@Deprecated("Reverted in favor of original floating liquid header in PdfViewerScreen")
@Composable
internal fun PdfViewerTopBar(
    documentTitle: String,
    isTablet: Boolean,
    activeTool: PdfEditTool,
    onToolSelected: (PdfEditTool) -> Unit,
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    currentStrokeWidth: Float,
    onStrokeWidthSelected: (Float) -> Unit,
    currentPage: Int,
    totalPages: Int,
    onPagePillClick: () -> Unit,
    isLexiconDrawerOpen: Boolean,
    onToggleLexiconDrawer: () -> Unit,
    onSearchClick: () -> Unit,
    onShareClick: () -> Unit,
    onBackClick: () -> Unit,
    isDockDetached: Boolean = false,
    onToggleDockDetached: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showAttributesPopover by remember { mutableStateOf(false) }
    val barHeight = if (isTablet) 76.dp else 64.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        tonalElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Cluster A: Navigation & Document Context ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(if (isTablet) 0.30f else 0.45f, fill = false)
            ) {
                ToolbarIconButton(
                    icon = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Back",
                    onClick = onBackClick
                )

                Column {
                    Text(
                        text = documentTitle.ifBlank { "Medical Document" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Malhoutha Study Mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF26A69A)
                    )
                }
            }

            // ── Cluster B: Central Annotation Toolset (Desktop & Tablet) ──
            if (!isDockDetached) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(if (isTablet) 0.42f else 0.35f, fill = false)
                ) {
                    // Hand / Text Select
                    ToolbarIconButton(
                        icon = Icons.Rounded.TouchApp,
                        contentDescription = "Text Select / Paging",
                        isSelected = activeTool == PdfEditTool.None,
                        onClick = { onToolSelected(PdfEditTool.None) }
                    )

                    // Fountain Pen
                    ToolbarIconButton(
                        icon = Icons.Rounded.Edit,
                        contentDescription = "Pen Inking",
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
                        contentDescription = "Laser Pointer",
                        isSelected = activeTool == PdfEditTool.Laser,
                        onClick = { onToolSelected(PdfEditTool.Laser) }
                    )

                    // Sticky Card Dropper
                    ToolbarIconButton(
                        icon = Icons.Rounded.EditNote,
                        contentDescription = "Medical Sticky Note",
                        isSelected = activeTool == PdfEditTool.StickyNote,
                        onClick = { onToolSelected(PdfEditTool.StickyNote) }
                    )

                    // Quick Color & Stroke Pip
                    Box {
                        Surface(
                            onClick = { showAttributesPopover = !showAttributesPopover },
                            shape = CircleShape,
                            color = currentColor,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.size(32.dp)
                        ) {}

                        // Inline Fast Attributes Popover
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
                }
            } else {
                Spacer(Modifier.weight(0.1f))
            }

            // ── Cluster C: Navigation Pill & Utility Actions ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Interactive Page Indicator Pill
                Surface(
                    onClick = onPagePillClick,
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Page $currentPage / $totalPages",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Lexicon Drawer Toggle (Part 8)
                ToolbarIconButton(
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    contentDescription = "Medical Lexicon & Clinical Dictionary",
                    isSelected = isLexiconDrawerOpen,
                    onClick = onToggleLexiconDrawer
                )

                // Search
                ToolbarIconButton(
                    icon = Icons.Rounded.Search,
                    contentDescription = "Search Text",
                    onClick = onSearchClick
                )

                // Share / Export
                ToolbarIconButton(
                    icon = Icons.Rounded.Share,
                    contentDescription = "Share",
                    onClick = onShareClick
                )

                // Tablet Detachable Dock Toggle
                if (isTablet && onToggleDockDetached != null) {
                    ToolbarIconButton(
                        icon = Icons.Rounded.Dock,
                        contentDescription = "Toggle Tool Dock",
                        isSelected = isDockDetached,
                        onClick = onToggleDockDetached
                    )
                }
            }
        }
    }
}

/**
 * Fast Pen Attributes Content (Color Swatches & Stroke Width Pills).
 */
@Composable
fun PenAttributesPopoverContent(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    currentStrokeWidth: Float,
    onStrokeWidthSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier.width(260.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Stroke Thickness • سماكة الخط",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 4 Stroke Width Preset Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StrokeWidthPresets.forEach { (width, label) ->
                val isSelected = currentStrokeWidth == width
                Surface(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onStrokeWidthSelected(width)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) Color(0xFF26A69A) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF26A69A) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(54.dp, 36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }

        Text(
            text = "Clinical Palette • ألوان الدراسة",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 6 High-Contrast Clinical Study Color Swatches
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ClinicalColorPalette.forEach { (color, _) ->
                val isSelected = currentColor.value == color.value
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color.White else Color(0x33000000),
                            shape = CircleShape
                        )
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onColorSelected(color)
                        }
                )
            }
        }
    }
}
