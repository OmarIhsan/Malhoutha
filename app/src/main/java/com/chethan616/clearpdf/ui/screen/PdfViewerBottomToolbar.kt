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
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoFixNormal
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.ChangeHistory
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CropSquare
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PanTool
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.chethan616.clearpdf.ui.components.GlassColorPicker
import com.chethan616.clearpdf.ui.components.LiquidButton
import com.chethan616.clearpdf.ui.components.LiquidIconButton
import com.chethan616.clearpdf.ui.components.carouselEdges
import com.chethan616.clearpdf.ui.components.viewerGlass
import com.chethan616.clearpdf.ui.utils.UISensor
import com.chethan616.clearpdf.utils.DocKind
import com.chethan616.clearpdf.data.repository.ToolbarOrientation
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.malhoutha.R
import com.malhoutha.ui.LocalDevicePosture
import kotlin.math.roundToInt

internal data class QuickPickSwatch(
    val hex: Long,
    val color: Color,
    val label: String
)

internal object QuickPalette {
    val Swatches = listOf(
        QuickPickSwatch(0xFF1A1A1EL, Color(0xFF1A1A1E), "Charcoal Black"),
        QuickPickSwatch(0xFF1E88E5L, Color(0xFF1E88E5), "Royal Blue"),
        QuickPickSwatch(0xFFE53935L, Color(0xFFE53935), "Vibrant Red"),
        QuickPickSwatch(0xFF43A047L, Color(0xFF43A047), "Emerald Green"),
        QuickPickSwatch(0xFFFDD835L, Color(0xFFFDD835), "Amber Yellow")
    )
}

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
    activeShapeMode: InkShapeMode = InkShapeMode.Free,
    drawingToolActive: Boolean,
    showFindBar: Boolean,
    showSignaturePad: Boolean,
    activeImageId: Long?,
    currentColor: Color,
    currentColorLong: Long,
    recentColors: List<Color> = DefaultRecentColors,
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
    onSetShapeMode: (InkShapeMode) -> Unit = {},
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
    docKind: DocKind = DocKind.Pdf,
    toolbarOrientation: ToolbarOrientation = ToolbarOrientation.Horizontal,
    onToggleToolbarOrientation: (() -> Unit)? = null
) {
    val posture = LocalDevicePosture.current
    val isVertical = toolbarOrientation == ToolbarOrientation.Vertical
    val haptic = LocalHapticFeedback.current

    // Repositioning drag offsets (confined exclusively to the drag handle)
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isVertical) {
        dragOffsetY = 0f
        dragOffsetX = 0f
    }

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
    var showColorPicker by rememberSaveable { mutableStateOf(false) }

    val shapeTools = remember {
        setOf(PdfEditTool.Rect, PdfEditTool.Ellipse, PdfEditTool.Line, PdfEditTool.Arrow, PdfEditTool.Triangle)
    }

    // Active tool states
    val pointerActive = activeTool == PdfEditTool.None && !drawingToolActive
    val penActive = drawingToolActive && (activeTool == PdfEditTool.Draw || activeTool in shapeTools)
    val hlActive = activeTool == PdfEditTool.Highlight
    val eraserActive = activeTool == PdfEditTool.Eraser
    val laserActive = activeTool == PdfEditTool.Laser
    val textActive = activeTool == PdfEditTool.Text
    val stickyNoteActive = activeTool == PdfEditTool.StickyNote
    val imageActive = activeTool == PdfEditTool.Image
    val signatureActive = activeTool == PdfEditTool.Signature || showSignaturePad

    // Animated scales for spring selection physics
    val pointerScale by animateFloatAsState(if (pointerActive) 1.10f else 1.0f, springSpec, label = "pointerScale")
    val penScale by animateFloatAsState(if (penActive) 1.10f else 1.0f, springSpec, label = "penScale")
    val hlScale by animateFloatAsState(if (hlActive) 1.10f else 1.0f, springSpec, label = "hlScale")
    val eraserScale by animateFloatAsState(if (eraserActive) 1.10f else 1.0f, springSpec, label = "eraserScale")
    val laserScale by animateFloatAsState(if (laserActive) 1.10f else 1.0f, springSpec, label = "laserScale")
    val textScale by animateFloatAsState(if (textActive) 1.10f else 1.0f, springSpec, label = "textScale")
    val stickyNoteScale by animateFloatAsState(if (stickyNoteActive) 1.10f else 1.0f, springSpec, label = "stickyNoteScale")
    val imageScale by animateFloatAsState(if (imageActive) 1.10f else 1.0f, springSpec, label = "imageScale")
    val signatureScale by animateFloatAsState(if (signatureActive) 1.10f else 1.0f, springSpec, label = "signatureScale")

    val buttonSize = if (isVertical) 40.dp else 36.dp
    val iconSize = if (isVertical) 20.dp else 18.dp

    // ── Primary Tool Capsule Content ───────────────────────────────────────────
    val capsuleContent: @Composable () -> Unit = {
        // Drag Handle
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

        // 1. Browse / Hand
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

        // 2. Draw / Pen (with live color tip indicator)
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

        // 3. Highlighter
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
                    imageVector = Icons.Rounded.BorderColor,
                    contentDescription = "Highlighter",
                    tint = if (hlActive) Color.White else fg,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // 4. Eraser
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
                    imageVector = Icons.Rounded.AutoFixNormal,
                    contentDescription = "Eraser",
                    tint = if (eraserActive) Color.White else fg,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // 5. Laser Pointer (Ephemeral Presentation Tool)
        val laserColor = Color(0xFFFF1744)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(laserScale)) {
            LiquidIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSetActiveTool(if (laserActive) PdfEditTool.None else PdfEditTool.Laser)
                },
                backdrop = backdrop,
                surfaceColor = if (laserActive) laserColor.copy(alpha = 0.95f) else chip,
                tint = if (laserActive) laserColor else Color.Unspecified,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Flare,
                    contentDescription = "Laser Pointer",
                    tint = if (laserActive) Color.White else fg,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // 6. Text Box
        Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(textScale)) {
            LiquidIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSetActiveTool(if (textActive) PdfEditTool.None else PdfEditTool.Text)
                },
                backdrop = backdrop,
                surfaceColor = if (textActive) accent.copy(alpha = 0.95f) else chip,
                tint = if (textActive) accent else Color.Unspecified,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.TextFields,
                    contentDescription = "Text Box",
                    tint = if (textActive) Color.White else fg,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // 6b. Sticky Note (Post-it Card Tool)
        val noteColor = Color(0xFFFBC02D)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(stickyNoteScale)) {
            LiquidIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSetActiveTool(if (stickyNoteActive) PdfEditTool.None else PdfEditTool.StickyNote)
                },
                backdrop = backdrop,
                surfaceColor = if (stickyNoteActive) noteColor.copy(alpha = 0.95f) else chip,
                tint = if (stickyNoteActive) noteColor else Color.Unspecified,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.StickyNote2,
                    contentDescription = "Sticky Note",
                    tint = if (stickyNoteActive) Color.Black else fg,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // 6. Image / Photo
        Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(imageScale)) {
            LiquidIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (imageActive) {
                        onSetActiveTool(PdfEditTool.None)
                    } else {
                        onPickImage()
                    }
                },
                backdrop = backdrop,
                surfaceColor = if (imageActive) accent.copy(alpha = 0.95f) else chip,
                tint = if (imageActive) accent else Color.Unspecified,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Image,
                    contentDescription = "Insert Image",
                    tint = if (imageActive) Color.White else fg,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // 7. Signature
        Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(signatureScale)) {
            LiquidIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onShowSignaturePad()
                },
                backdrop = backdrop,
                surfaceColor = if (signatureActive) accent.copy(alpha = 0.95f) else chip,
                tint = if (signatureActive) accent else Color.Unspecified,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Draw,
                    contentDescription = "Signature",
                    tint = if (signatureActive) Color.White else fg,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        // 8. Separator
        DockHairlineDivider(isVertical = isVertical, fg = fg)

        // 9. Undo
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

        // 10. Redo
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

        // 11. Dock Orientation Toggle (Horizontal Bottom Bar <-> Vertical Lateral Dock)
        if (onToggleToolbarOrientation != null) {
            DockHairlineDivider(isVertical = isVertical, fg = fg)

            LiquidIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleToolbarOrientation()
                },
                backdrop = backdrop,
                surfaceColor = chip,
                modifier = Modifier.size(buttonSize)
            ) {
                Icon(
                    imageVector = if (isVertical) Icons.Rounded.ViewAgenda else Icons.AutoMirrored.Rounded.ViewSidebar,
                    contentDescription = if (isVertical) "Switch to horizontal bottom dock" else "Switch to vertical side dock",
                    tint = fg.copy(alpha = 0.9f),
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }

    // ── Secondary Flyout Shelf Content (Shapes Cluster | Thickness Cluster | Color Swatches Cluster) ────
    val shapeItems = remember {
        listOf(
            InkShapeMode.Free to (Icons.Rounded.Gesture to "Free Write"),
            InkShapeMode.Arrow to (Icons.AutoMirrored.Rounded.TrendingFlat to "Line / Arrow"),
            InkShapeMode.Rectangle to (Icons.Rounded.CropSquare to "Rectangle"),
            InkShapeMode.Circle to (Icons.Rounded.RadioButtonUnchecked to "Circle / Oval"),
            InkShapeMode.Triangle to (Icons.Rounded.ChangeHistory to "Triangle")
        )
    }

    val thicknessItems = remember {
        listOf(
            Triple(3f, 3.dp, "Fine (1.5dp)"),
            Triple(6f, 6.dp, "Medium (3.0dp)"),
            Triple(11f, 10.dp, "Bold (6.0dp)")
        )
    }

    val rainbowBrush = remember {
        Brush.sweepGradient(
            listOf(
                Color(0xFFFF0000),
                Color(0xFFFFEE00),
                Color(0xFF00FF00),
                Color(0xFF00EEFF),
                Color(0xFF0000FF),
                Color(0xFFFF00FF),
                Color(0xFFFF0000)
            )
        )
    }

    val secondaryShelfContent: @Composable (isVerticalLayout: Boolean) -> Unit = { isVert ->
        val isRecentColorSelected = recentColors.any { it.toArgb() == currentColor.toArgb() }

        val colorClusterContent: @Composable () -> Unit = {
            recentColors.forEachIndexed { index, swatchColor ->
                val isSelected = currentColor.toArgb() == swatchColor.toArgb()
                val swatchScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = springSpec,
                    label = "recentSwatchScale_$index"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (isVert) 30.dp else 28.dp)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val hex = swatchColor.toArgb().toLong() and 0xFFFFFFFFL
                            onSetColorLong(hex)
                            if (!penActive && !hlActive) {
                                onSetActiveTool(PdfEditTool.Draw)
                            }
                        }
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(if (isVert) 28.dp else 26.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (isDarkSubstrate) Color.White else Color(0xFF1976D2),
                                    shape = CircleShape
                                )
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .scale(swatchScale)
                            .size(if (isVert) 20.dp else 18.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = 1.dp,
                                color = if (swatchColor.luminance() > 0.85f) Color.Gray.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Active color $index",
                                tint = if (swatchColor.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

            // Custom color picker circle button
            val isCustomActive = !isRecentColorSelected
            val customScale by animateFloatAsState(
                targetValue = if (isCustomActive) 1.15f else 1.0f,
                animationSpec = springSpec,
                label = "customColorScale"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (isVert) 30.dp else 28.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showColorPicker = true
                    }
            ) {
                if (isCustomActive) {
                    Box(
                        modifier = Modifier
                            .size(if (isVert) 28.dp else 26.dp)
                            .border(
                                width = 2.dp,
                                color = if (isDarkSubstrate) Color.White else Color(0xFF1976D2),
                                shape = CircleShape
                            )
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .scale(customScale)
                        .size(if (isVert) 20.dp else 18.dp)
                        .clip(CircleShape)
                        .background(if (isCustomActive) currentColor else Color.Transparent)
                        .border(
                            width = 2.dp,
                            brush = rainbowBrush,
                            shape = CircleShape
                        )
                ) {
                    if (isCustomActive) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Custom Color Active",
                            tint = if (currentColor.luminance() > 0.5f) Color.Black else Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }

        if (isVert) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 5.dp)
            ) {
                // ── Cluster 1: Shape Geometries (InkShapeMode) ──
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(fg.copy(alpha = 0.06f))
                        .padding(2.dp)
                ) {
                    shapeItems.forEach { (mode, pair) ->
                        val (icon, label) = pair
                        val isSelected = activeShapeMode == mode && (penActive || hlActive)
                        val btnScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1.0f,
                            animationSpec = springSpec,
                            label = "shapeScale_vert_${mode.name}"
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .scale(btnScale)
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .background(accent.copy(alpha = 0.92f))
                                            .border(0.75.dp, specularHighlight, RoundedCornerShape(9.dp))
                                    } else Modifier
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSetShapeMode(mode)
                                }
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) Color.White else fg.copy(alpha = 0.78f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // ── Divider: Subtle horizontal separator (16dp width, 1dp height) ──
                Box(
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .width(16.dp)
                        .height(1.dp)
                        .background(fg.copy(alpha = 0.20f))
                )

                // ── Cluster 2: Stroke Thickness Selector ──
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(fg.copy(alpha = 0.06f))
                        .padding(2.dp)
                ) {
                    thicknessItems.forEach { (width, dotSize, label) ->
                        val isSelected = (currentStrokeWidth - width).let { it >= -0.5f && it <= 0.5f }
                        val btnScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1.0f,
                            animationSpec = springSpec,
                            label = "strokeScale_vert_$width"
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .scale(btnScale)
                                .size(32.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .background(accent.copy(alpha = 0.92f))
                                            .border(0.75.dp, specularHighlight, RoundedCornerShape(9.dp))
                                    } else Modifier
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSetStrokeWidth(width)
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else fg.copy(alpha = 0.78f))
                            )
                        }
                    }
                }

                // ── Divider: Subtle horizontal separator (16dp width, 1dp height) ──
                Box(
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .width(16.dp)
                        .height(1.dp)
                        .background(fg.copy(alpha = 0.20f))
                )

                // ── Cluster 3: Color Palette Swatches Cluster ──
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(fg.copy(alpha = 0.06f))
                        .padding(horizontal = 2.dp, vertical = 3.dp)
                ) {
                    colorClusterContent()
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                // ── Cluster 1: Shape Geometries (InkShapeMode) ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(fg.copy(alpha = 0.06f))
                        .padding(2.dp)
                ) {
                    shapeItems.forEach { (mode, pair) ->
                        val (icon, label) = pair
                        val isSelected = activeShapeMode == mode && (penActive || hlActive)
                        val btnScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1.0f,
                            animationSpec = springSpec,
                            label = "shapeScale_horiz_${mode.name}"
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .scale(btnScale)
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .background(accent.copy(alpha = 0.92f))
                                            .border(0.75.dp, specularHighlight, RoundedCornerShape(9.dp))
                                    } else Modifier
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSetShapeMode(mode)
                                }
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) Color.White else fg.copy(alpha = 0.78f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // ── Divider: Subtle vertical glass separator (1dp width, 16dp height) ──
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .width(1.dp)
                        .height(16.dp)
                        .background(fg.copy(alpha = 0.20f))
                )

                // ── Cluster 2: Stroke Thickness Selector ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(fg.copy(alpha = 0.06f))
                        .padding(2.dp)
                ) {
                    thicknessItems.forEach { (width, dotSize, label) ->
                        val isSelected = (currentStrokeWidth - width).let { it >= -0.5f && it <= 0.5f }
                        val btnScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1.0f,
                            animationSpec = springSpec,
                            label = "strokeScale_horiz_$width"
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .scale(btnScale)
                                .size(30.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .background(accent.copy(alpha = 0.92f))
                                            .border(0.75.dp, specularHighlight, RoundedCornerShape(9.dp))
                                    } else Modifier
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSetStrokeWidth(width)
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(dotSize)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else fg.copy(alpha = 0.78f))
                            )
                        }
                    }
                }

                // ── Divider: Subtle vertical glass separator (1dp width, 16dp height) ──
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .width(1.dp)
                        .height(16.dp)
                        .background(fg.copy(alpha = 0.20f))
                )

                // ── Cluster 3: Color Palette Swatches Cluster ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(fg.copy(alpha = 0.06f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    colorClusterContent()
                }
            }
        }
    }

    // ── Color Picker Dialog (GlassColorPicker) ─────────────────────────────────
    if (showColorPicker) {
        Dialog(
            onDismissRequest = { showColorPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(20.dp)
                    .viewerGlass(backdrop, dockGlassTint, shape = { RoundedCornerShape(24.dp) })
                    .clip(RoundedCornerShape(24.dp))
                    .border(0.75.dp, specularHighlight, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText(
                            "Color Palette",
                            style = TextStyle(
                                color = fg,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        LiquidIconButton(
                            onClick = { showColorPicker = false },
                            backdrop = backdrop,
                            surfaceColor = chip,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = fg,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    var pickerDraftColor by remember(showColorPicker, currentColor) { mutableStateOf(currentColor) }

                    GlassColorPicker(
                        color = pickerDraftColor,
                        onColorChange = { c ->
                            pickerDraftColor = c
                        },
                        backdrop = backdrop,
                        showAlpha = false
                    )

                    LiquidButton(
                        onClick = {
                            val argbLong = pickerDraftColor.toArgb().toLong() and 0xFFFFFFFFL
                            onSetColorLong(argbLong)
                            if (!penActive && !hlActive) {
                                onSetActiveTool(PdfEditTool.Draw)
                            }
                            showColorPicker = false
                        },
                        backdrop = backdrop,
                        surfaceColor = accent.copy(alpha = 0.92f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        BasicText(
                            "Apply",
                            style = TextStyle(Color.White, 14.sp, FontWeight.SemiBold)
                        )
                    }
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
                visible = drawingToolActive,
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
                visible = drawingToolActive,
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
