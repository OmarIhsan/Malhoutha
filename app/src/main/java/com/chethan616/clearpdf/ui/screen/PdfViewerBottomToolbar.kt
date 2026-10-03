package com.chethan616.clearpdf.ui.screen

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chethan616.clearpdf.ui.components.LiquidButton
import com.chethan616.clearpdf.ui.components.LiquidIconButton
import com.chethan616.clearpdf.ui.components.carouselEdges
import com.chethan616.clearpdf.ui.components.viewerGlass
import com.chethan616.clearpdf.ui.utils.UISensor
import com.chethan616.clearpdf.utils.DocKind
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.malhoutha.R
import com.malhoutha.ui.LocalDevicePosture
import kotlin.math.roundToInt

/**
 * Curated 3-slot Academic Inking Palette for Zero-Tap color switching:
 * - Carbon Black: Primary lecture notation and formulas (#1A1A1E)
 * - Academic Blue: Sub-headers, theorem definitions, diagrams (#1976D2)
 * - Alert Crimson: Exam alerts, urgent corrections, deadlines (#D32F2F)
 */
object AcademicPalette {
    const val CarbonBlackHex = 0xFF1A1A1EL
    const val AcademicBlueHex = 0xFF1976D2L
    const val AlertCrimsonHex = 0xFFD32F2FL

    val CarbonBlack = Color(CarbonBlackHex)
    val AcademicBlue = Color(AcademicBlueHex)
    val AlertCrimson = Color(AlertCrimsonHex)

    val Swatches = listOf(
        Triple(CarbonBlackHex, CarbonBlack, "Carbon Black"),
        Triple(AcademicBlueHex, AcademicBlue, "Academic Blue"),
        Triple(AlertCrimsonHex, AlertCrimson, "Alert Crimson")
    )
}

/**
 * Native Floating Liquid Glass Toolbar for Malhoutha:
 *
 * Implements the exact layout, ergonomics, and tool progression of the legacy native Android
 * ToolCapsule (from notes_app_native_android) rendered with ClearPDF's :backdrop Liquid Glass
 * shader system.
 *
 * Exact Visual Topology:
 * 1. Drag Handle: Subtle pill indicator at the lead edge for repositioning.
 * 2. Navigation / Pointer: Hand / Selection icon (PdfEditTool.None).
 * 3. Primary Inking Tools:
 *    - Pen (PdfEditTool.Draw) with live color tip indicator.
 *    - Highlighter (PdfEditTool.Highlight).
 *    - Eraser (PdfEditTool.Eraser).
 * 4. Hairline Divider.
 * 5. Zero-Tap Academic Color Well:
 *    - Exactly 3 exposed circular swatches (#1A1A1E, #1976D2, #D32F2F).
 *    - Spring scale animation and high-contrast active ring.
 * 6. Hairline Divider.
 * 7. History Controls:
 *    - Undo (Icons.AutoMirrored.Rounded.Undo).
 *    - Redo (Icons.AutoMirrored.Rounded.Redo).
 * 8. Trailing Shelf Flyout:
 *    - Hairline divider and More Tools button (Icons.Rounded.Apps) for stroke presets,
 *      clear page, signature, image import, search/find, and export.
 *
 * Surface & Ergonomics:
 * - 28.dp corner radius with 0.75.dp specular highlight border.
 * - Dynamic Luminance: Frost automatically adapts to dark or light document substrates.
 * - Responsive Dual-Mode Docking:
 *   - Tablet mode (TabletLandscape/Portrait): 56dp vertical capsule anchored at CenterStart.
 *   - Mobile/Split mode (PhonePortrait/Landscape): 56dp horizontal capsule anchored at BottomCenter.
 */
@Composable
internal fun PdfViewerBottomToolbar(
    // Display state
    activeTool: PdfEditTool,
    drawingToolActive: Boolean,
    showFindBar: Boolean,
    showSignaturePad: Boolean,
    activeImageId: Long?,
    currentColor: Color,
    currentColorLong: Long,
    currentStrokeWidth: Float,
    zoomScale: Float,
    hasEdits: Boolean,
    isExporting: Boolean,
    exportError: String?,
    exportMessage: String?,
    lastExportedUri: Uri?,
    activeIsSignature: Boolean,
    canUndo: Boolean,
    canRedo: Boolean = false,
    // Callbacks
    onUndo: () -> Unit,
    onRedo: () -> Unit = {},
    onClearPage: () -> Unit,
    onSetActiveTool: (PdfEditTool) -> Unit,
    onToggleFindBar: () -> Unit,
    onShowSignaturePad: () -> Unit,
    onPickImage: () -> Unit,
    onResetZoom: () -> Unit,
    onShowSaveDialog: () -> Unit,
    onImageDone: () -> Unit,
    onReplaceImage: () -> Unit,
    onDeleteImage: () -> Unit,
    onSetColorLong: (Long) -> Unit,
    onSetStrokeWidth: (Float) -> Unit,
    onDismissExportFeedback: () -> Unit,
    onOpenExportedFile: () -> Unit,
    onOpenAnotherPdf: () -> Unit,
    onShareDocument: () -> Unit,
    onEditorOpenChanged: (Boolean) -> Unit = {},
    onShareHoldChanged: (Boolean) -> Unit = {},
    onRecolorSignature: (Long) -> Unit,
    backdrop: LayerBackdrop,
    uiSensor: UISensor,
    fg: Color,
    fgSoft: Color,
    glass: Color,
    chip: Color,
    docKind: DocKind = DocKind.Pdf
) {
    val posture = LocalDevicePosture.current
    val isVertical = posture.useLateralDock
    val haptic = LocalHapticFeedback.current

    // Spring physics configuration
    val springSpec = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    }

    // Dynamic luminance: Frosted glass adapting to document background
    val isDarkSubstrate = fg.luminance() > 0.5f
    val dockGlassTint = if (isDarkSubstrate) {
        Color(0xFF1E2124).copy(alpha = 0.65f)
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.60f)
    }

    // Specular highlight border: 0.75.dp semi-transparent white gradient
    val specularHighlight = remember(isDarkSubstrate) {
        Brush.linearGradient(
            colors = if (isDarkSubstrate) {
                listOf(
                    Color.White.copy(alpha = 0.38f),
                    Color.White.copy(alpha = 0.12f),
                    Color.White.copy(alpha = 0.04f)
                )
            } else {
                listOf(
                    Color.White.copy(alpha = 0.85f),
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.15f)
                )
            }
        )
    }

    val accent = Color(0xFF1976D2)
    var toolsFlyoutOpen by rememberSaveable { mutableStateOf(false) }

    // Repositioning drag offsets (confined exclusively to the drag handle)
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    // Active tool states
    val pointerActive = activeTool == PdfEditTool.None && !drawingToolActive
    val penActive = drawingToolActive && activeTool == PdfEditTool.Draw
    val hlActive = activeTool == PdfEditTool.Highlight
    val eraserActive = activeTool == PdfEditTool.Eraser

    // Animated scales for spring selection physics
    val pointerScale by animateFloatAsState(if (pointerActive) 1.10f else 1.0f, springSpec, label = "pointerScale")
    val penScale by animateFloatAsState(if (penActive) 1.10f else 1.0f, springSpec, label = "penScale")
    val hlScale by animateFloatAsState(if (hlActive) 1.10f else 1.0f, springSpec, label = "hlScale")
    val eraserScale by animateFloatAsState(if (eraserActive) 1.10f else 1.0f, springSpec, label = "eraserScale")
    val moreScale by animateFloatAsState(if (toolsFlyoutOpen) 1.10f else 1.0f, springSpec, label = "moreScale")

    val buttonSize = if (isVertical) 40.dp else 36.dp
    val iconSize = if (isVertical) 20.dp else 18.dp
    val swatchTouchSize = if (isVertical) 36.dp else 32.dp
    val swatchDiscSize = if (isVertical) 22.dp else 19.dp

    // ── Primary Tool Capsule Content ───────────────────────────────────────────
    val capsuleContent: @Composable () -> Unit = {
        // 1. Drag Handle
        DockDragHandle(
            isVertical = isVertical,
            isDark = isDarkSubstrate,
            modifier = Modifier.pointerInput(isVertical) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    if (isVertical) {
                        dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(-180f, 180f)
                    } else {
                        dragOffsetX = (dragOffsetX + dragAmount.x).coerceIn(-160f, 160f)
                    }
                }
            }
        )

        // 2. Navigation / Pointer (Hand icon)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(pointerScale)) {
            LiquidIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSetActiveTool(PdfEditTool.None)
                },
                backdrop = backdrop,
                surfaceColor = if (pointerActive) accent.copy(alpha = 0.95f) else chip,
                tint = if (pointerActive) accent else Color.Unspecified,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PanTool,
                    contentDescription = "Navigate / Select",
                    tint = if (pointerActive) Color.White else fg,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // 3. Primary Inking Tools:
        // A. Pen with live color tip indicator
        Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(penScale)) {
            LiquidIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSetActiveTool(if (penActive) PdfEditTool.None else PdfEditTool.Draw)
                },
                backdrop = backdrop,
                surfaceColor = if (penActive) currentColor.copy(alpha = 0.95f) else chip,
                tint = if (penActive) currentColor else Color.Unspecified,
                modifier = Modifier.size(buttonSize)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Pen Tool",
                        tint = if (penActive) (if (currentColor.luminance() > 0.65f) Color.Black else Color.White) else fg,
                        modifier = Modifier.size(iconSize)
                    )
                    // Live color tip indicator at bottom-right corner of pen icon
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(currentColor)
                            .border(1.dp, if (penActive) Color.White else fg.copy(alpha = 0.4f), CircleShape)
                    )
                }
            }
        }

        // B. Highlighter
        val hlColor = Color(0xFFF9A825)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(hlScale)) {
            LiquidIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSetActiveTool(if (hlActive) PdfEditTool.None else PdfEditTool.Highlight)
                },
                backdrop = backdrop,
                surfaceColor = if (hlActive) hlColor.copy(alpha = 0.95f) else chip,
                tint = if (hlActive) hlColor else Color.Unspecified,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Brush,
                    contentDescription = "Highlighter",
                    tint = if (hlActive) Color.White else fg,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // C. Eraser
        val eraserColor = Color(0xFFC62828)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(eraserScale)) {
            LiquidIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSetActiveTool(if (eraserActive) PdfEditTool.None else PdfEditTool.Eraser)
                },
                backdrop = backdrop,
                surfaceColor = if (eraserActive) eraserColor.copy(alpha = 0.95f) else chip,
                tint = if (eraserActive) eraserColor else Color.Unspecified,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Eraser",
                    tint = if (eraserActive) Color.White else fg,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // 4. Hairline Divider
        DockHairlineDivider(isVertical = isVertical, fg = fg)

        // 5. Zero-Tap Academic Color Well (Black, Blue, Crimson)
        val colorWellContent = @Composable {
            AcademicPalette.Swatches.forEach { (colorHex, colorVal, name) ->
                val isSelected = currentColorLong == colorHex
                val swatchScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.20f else 1.0f,
                    animationSpec = springSpec,
                    label = "swatchScale_$name"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(swatchTouchSize)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSetColorLong(colorHex)
                            if (!penActive) {
                                onSetActiveTool(PdfEditTool.Draw)
                            }
                        }
                ) {
                    // High-contrast outer selection ring
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(swatchTouchSize - 4.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (isDarkSubstrate) Color.White else Color(0xFF1976D2),
                                    shape = CircleShape
                                )
                        )
                    }

                    // Visual color disk
                    val checkmarkTint = if (colorVal.luminance() > 0.5f) Color.Black else Color.White
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .scale(swatchScale)
                            .size(swatchDiscSize)
                            .clip(CircleShape)
                            .background(colorVal)
                            .border(
                                width = 1.dp,
                                color = if (colorVal.luminance() > 0.85f) Color.Gray.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Active color $name",
                                tint = checkmarkTint,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isVertical) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(fg.copy(alpha = 0.06f))
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                colorWellContent()
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(fg.copy(alpha = 0.06f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                colorWellContent()
            }
        }

        // 6. Hairline Divider
        DockHairlineDivider(isVertical = isVertical, fg = fg)

        // 7. History Controls: Undo & Redo
        LiquidIconButton(
            onClick = {
                if (canUndo) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onUndo()
                }
            },
            backdrop = backdrop,
            surfaceColor = chip,
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Undo,
                contentDescription = stringResource(R.string.viewer_undo),
                tint = fg.copy(alpha = if (canUndo) 1.0f else 0.35f),
                modifier = Modifier.size(iconSize)
            )
        }

        LiquidIconButton(
            onClick = {
                if (canRedo) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRedo()
                }
            },
            backdrop = backdrop,
            surfaceColor = chip,
            modifier = Modifier.size(buttonSize)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Redo,
                contentDescription = "Redo",
                tint = fg.copy(alpha = if (canRedo) 1.0f else 0.35f),
                modifier = Modifier.size(iconSize)
            )
        }

        // 8. Hairline Divider & Secondary Tools Toggle
        DockHairlineDivider(isVertical = isVertical, fg = fg)

        Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(moreScale)) {
            LiquidIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    toolsFlyoutOpen = !toolsFlyoutOpen
                },
                backdrop = backdrop,
                surfaceColor = if (toolsFlyoutOpen) accent.copy(alpha = 0.90f) else chip,
                tint = if (toolsFlyoutOpen) accent else Color.Unspecified,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = "More Tools",
                    tint = if (toolsFlyoutOpen) Color.White else fg,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }

    // ── Secondary Flyout Shelf Content (Stroke Presets, Clear, Shapes/Image/Sign/Find) ────
    val secondaryShelfContent: @Composable (isVerticalLayout: Boolean) -> Unit = { isVert ->
        if (isVert) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
            ) {
                if (drawingToolActive) {
                    listOf(3f to "Fine", 6f to "Medium", 11f to "Bold").forEach { (width, label) ->
                        val isSelected = (currentStrokeWidth - width).let { it >= -0.5f && it <= 0.5f }
                        LiquidButton(
                            onClick = { onSetStrokeWidth(width) },
                            backdrop = backdrop,
                            surfaceColor = if (isSelected) accent.copy(alpha = 0.9f) else chip,
                            modifier = Modifier.height(28.dp)
                        ) {
                            BasicText(
                                label,
                                style = TextStyle(
                                    color = if (isSelected) Color.White else fg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Box(Modifier.width(24.dp).height(1.dp).background(fg.copy(0.12f)))
                    LiquidIconButton(
                        onClick = onClearPage,
                        backdrop = backdrop,
                        surfaceColor = chip,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Rounded.Delete, "Clear Page", Modifier.size(16.dp), fg)
                    }
                }

                LiquidIconButton(
                    onClick = onShowSignaturePad,
                    backdrop = backdrop,
                    surfaceColor = chip,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.Gesture, "Signature", Modifier.size(16.dp), fg)
                }

                LiquidIconButton(
                    onClick = onPickImage,
                    backdrop = backdrop,
                    surfaceColor = chip,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.CropSquare, "Insert Image", Modifier.size(16.dp), fg)
                }

                LiquidIconButton(
                    onClick = onToggleFindBar,
                    backdrop = backdrop,
                    surfaceColor = chip,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.Search, "Find", Modifier.size(16.dp), fg)
                }

                if (hasEdits && !isExporting) {
                    LiquidIconButton(
                        onClick = onShowSaveDialog,
                        backdrop = backdrop,
                        tint = Color(0xFF1976D2),
                        surfaceColor = Color(0xFF1976D2).copy(alpha = 0.9f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Rounded.Check, "Save", Modifier.size(16.dp), Color.White)
                    }
                }

                LiquidIconButton(
                    onClick = onShareDocument,
                    backdrop = backdrop,
                    surfaceColor = chip,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Rounded.IosShare, "Share", Modifier.size(16.dp), fg)
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (drawingToolActive) {
                    listOf(3f to "Fine", 6f to "Med", 11f to "Bold").forEach { (width, label) ->
                        val isSelected = (currentStrokeWidth - width).let { it >= -0.5f && it <= 0.5f }
                        LiquidButton(
                            onClick = { onSetStrokeWidth(width) },
                            backdrop = backdrop,
                            surfaceColor = if (isSelected) accent.copy(alpha = 0.9f) else chip,
                            modifier = Modifier.height(28.dp)
                        ) {
                            BasicText(
                                label,
                                style = TextStyle(
                                    color = if (isSelected) Color.White else fg,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Box(Modifier.width(1.dp).height(20.dp).background(fg.copy(0.12f)))
                    LiquidIconButton(
                        onClick = onClearPage,
                        backdrop = backdrop,
                        surfaceColor = chip,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.Delete, "Clear Page", Modifier.size(15.dp), fg)
                    }
                }

                LiquidIconButton(
                    onClick = onShowSignaturePad,
                    backdrop = backdrop,
                    surfaceColor = chip,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.Gesture, "Signature", Modifier.size(15.dp), fg)
                }

                LiquidIconButton(
                    onClick = onPickImage,
                    backdrop = backdrop,
                    surfaceColor = chip,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.CropSquare, "Insert Image", Modifier.size(15.dp), fg)
                }

                LiquidIconButton(
                    onClick = onToggleFindBar,
                    backdrop = backdrop,
                    surfaceColor = chip,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.Search, "Find", Modifier.size(15.dp), fg)
                }

                if (hasEdits && !isExporting) {
                    LiquidIconButton(
                        onClick = onShowSaveDialog,
                        backdrop = backdrop,
                        tint = Color(0xFF1976D2),
                        surfaceColor = Color(0xFF1976D2).copy(alpha = 0.9f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Rounded.Check, "Save", Modifier.size(15.dp), Color.White)
                    }
                }

                LiquidIconButton(
                    onClick = onShareDocument,
                    backdrop = backdrop,
                    surfaceColor = chip,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.IosShare, "Share", Modifier.size(15.dp), fg)
                }
            }
        }
    }

    // ── Export Feedback Banner ─────────────────────────────────────────────────
    if (exportError != null || exportMessage != null || isExporting) {
        Row(
            Modifier
                .wrapContentSize()
                .viewerGlass(backdrop, dockGlassTint, shape = { RoundedCornerShape(18.dp) })
                .clip(RoundedCornerShape(18.dp))
                .border(0.75.dp, specularHighlight, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                isExporting -> BasicText(stringResource(R.string.viewer_saving), style = TextStyle(fgSoft, 12.sp))
                exportError != null -> {
                    BasicText(exportError, style = TextStyle(Color(0xFFE53935), 12.sp))
                    LiquidButton(onClick = onDismissExportFeedback, backdrop = backdrop, surfaceColor = chip) {
                        BasicText(stringResource(R.string.dismiss), style = TextStyle(fg, 11.sp, FontWeight.Medium))
                    }
                }
                exportMessage != null -> {
                    BasicText(exportMessage, style = TextStyle(Color(0xFFB9F6CA), 12.sp))
                    if (lastExportedUri != null) {
                        LiquidButton(onClick = onOpenExportedFile, backdrop = backdrop, tint = Color(0xFF1976D2)) {
                            BasicText(stringResource(R.string.open), style = TextStyle(Color.White, 11.sp, FontWeight.Medium))
                        }
                    }
                }
            }
        }
    }

    // ── Tablet Mode (Vertical Spine Dock) ──────────────────────────────────────
    if (isVertical) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.offset { IntOffset(0, dragOffsetY.roundToInt()) }
        ) {
            // Main Lateral Dock (56dp width, 28dp radius)
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .wrapContentHeight()
                    .viewerGlass(backdrop, dockGlassTint, shape = { RoundedCornerShape(28.dp) })
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        width = 0.75.dp,
                        brush = specularHighlight,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(vertical = 12.dp, horizontal = 6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    capsuleContent()
                }
            }

            // Secondary Flyout Shelf (Adjacent lateral glass card)
            AnimatedVisibility(
                visible = toolsFlyoutOpen || drawingToolActive,
                enter = fadeIn(tween(180)) + expandHorizontally(),
                exit = fadeOut(tween(150)) + shrinkHorizontally()
            ) {
                Box(
                    modifier = Modifier
                        .viewerGlass(backdrop, dockGlassTint, shape = { RoundedCornerShape(22.dp) })
                        .clip(RoundedCornerShape(22.dp))
                        .border(0.75.dp, specularHighlight, RoundedCornerShape(22.dp))
                ) {
                    secondaryShelfContent(true)
                }
            }
        }
    } else {
        // ── Mobile / Split-Screen Mode (Horizontal Floating Capsule) ────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.offset { IntOffset(dragOffsetX.roundToInt(), 0) }
        ) {
            // Secondary Flyout Shelf (Floats directly above dock)
            AnimatedVisibility(
                visible = toolsFlyoutOpen || drawingToolActive,
                enter = fadeIn(tween(180)) + expandVertically(),
                exit = fadeOut(tween(150)) + shrinkVertically()
            ) {
                val shelfScroll = rememberScrollState()
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .viewerGlass(backdrop, dockGlassTint, shape = { RoundedCornerShape(22.dp) })
                        .clip(RoundedCornerShape(22.dp))
                        .border(0.75.dp, specularHighlight, RoundedCornerShape(22.dp))
                        .carouselEdges(shelfScroll, clipContent = false)
                        .horizontalScroll(shelfScroll)
                ) {
                    secondaryShelfContent(false)
                }
            }

            // Main Bottom Floating Capsule (56dp height, 28dp radius)
            val capsuleScroll = rememberScrollState()
            Box(
                modifier = Modifier
                    .height(56.dp)
                    .wrapContentWidth()
                    .viewerGlass(backdrop, dockGlassTint, shape = { RoundedCornerShape(28.dp) })
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        width = 0.75.dp,
                        brush = specularHighlight,
                        shape = RoundedCornerShape(28.dp)
                    )
                    .carouselEdges(capsuleScroll, clipContent = false)
                    .horizontalScroll(capsuleScroll)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    capsuleContent()
                }
            }
        }
    }
}

/**
 * Subtle hairline separator inside the floating dock.
 */
@Composable
private fun DockHairlineDivider(
    isVertical: Boolean,
    fg: Color,
    modifier: Modifier = Modifier
) {
    if (isVertical) {
        Box(
            modifier = modifier
                .padding(vertical = 2.dp)
                .width(22.dp)
                .height(1.dp)
                .background(fg.copy(alpha = 0.16f))
        )
    } else {
        Box(
            modifier = modifier
                .padding(horizontal = 2.dp)
                .width(1.dp)
                .height(22.dp)
                .background(fg.copy(alpha = 0.16f))
        )
    }
}

/**
 * Subtle pill indicator at the lead edge for dock repositioning.
 */
@Composable
private fun DockDragHandle(
    isVertical: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val handleColor = if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.25f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(if (isVertical) 4.dp else 4.dp)
    ) {
        if (isVertical) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(handleColor)
            )
        } else {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(20.dp)
                    .clip(CircleShape)
                    .background(handleColor)
            )
        }
    }
}
