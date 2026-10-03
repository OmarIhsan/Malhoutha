package com.chethan616.clearpdf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malhoutha.core.ink.math.TextAnnotationTransformSolver
import com.malhoutha.core.ink.models.StickyCardAnnotation
import com.malhoutha.core.ink.models.StickyCardPalette
import kotlin.math.roundToInt

private val AccentBlue = Color(0xFF0A84FF)

/**
 * Pre-styled, lightweight "Sticky Card" / Sticky Note Composable.
 *
 * Features:
 * - Distinct Header (Title) and Body text area.
 * - Folded vs. Expanded state: folds down into a compact floating Post-it chip.
 * - Rounded container with pastel background and subtle drop shadow.
 * - Integrated stationary opposite-anchor horizontal resizing & page translation handles.
 * - Dynamic vertical height auto-expansion downwards.
 * - Pin/Unpin support to lock placement against accidental drags.
 * - 6-Color pastel swatch switcher (`StickyCardPalette`).
 * - Fluid gesture responses and high-contrast pill handles.
 */
@Composable
fun StickyNoteCard(
    annotation: StickyCardAnnotation,
    pageWidthPx: Float,
    pageHeightPx: Float,
    isToolActive: Boolean = false,
    isReadOnly: Boolean = false,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onDeselect: () -> Unit = {},
    onPositionChanged: (Rect) -> Unit = {},
    onAnnotationChanged: (StickyCardAnnotation) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val titleFocusRequester = remember { FocusRequester() }

    var isEditing by remember(isSelected) { mutableStateOf(isSelected) }

    // Synchronize editing state with external selection or tool change
    if (isReadOnly) {
        if (isEditing) {
            isEditing = false
            onDeselect()
        }
    }

    var isDraggingHandle by remember { mutableStateOf(false) }

    var normX by remember(annotation.id) { mutableFloatStateOf(annotation.xNorm) }
    var normY by remember(annotation.id) { mutableFloatStateOf(annotation.yNorm) }
    var normW by remember(annotation.id) { mutableFloatStateOf(annotation.widthNorm.coerceAtLeast(0.18f)) }
    var normH by remember(annotation.id) { mutableFloatStateOf(annotation.heightNorm) }

    var cardColorHex by remember(annotation.id) { mutableLongStateOf(annotation.colorHex) }
    var isPinned by remember(annotation.id) { mutableStateOf(annotation.isPinned) }
    var isFolded by remember(annotation.id) { mutableStateOf(annotation.isFolded) }
    var textAlign by remember(annotation.id) {
        mutableStateOf(
            when (annotation.alignment) {
                "CENTER" -> TextAlign.Center
                "END" -> TextAlign.End
                else -> TextAlign.Start
            }
        )
    }
    var showColorPalette by remember { mutableStateOf(false) }

    // Decoupled Title state keyed strictly by annotation.id
    var titleValue by remember(annotation.id) {
        mutableStateOf(
            TextFieldValue(
                text = annotation.title,
                selection = TextRange(annotation.title.length)
            )
        )
    }

    // Decoupled Body state keyed strictly by annotation.id
    var bodyValue by remember(annotation.id) {
        mutableStateOf(
            TextFieldValue(
                text = annotation.content,
                selection = TextRange(annotation.content.length)
            )
        )
    }

    // Synchronize external updates
    LaunchedEffect(annotation.title) {
        if (annotation.title != titleValue.text) {
            titleValue = titleValue.copy(
                text = annotation.title,
                selection = TextRange(annotation.title.length)
            )
        }
    }

    LaunchedEffect(annotation.content) {
        if (annotation.content != bodyValue.text) {
            bodyValue = bodyValue.copy(
                text = annotation.content,
                selection = TextRange(annotation.content.length)
            )
        }
    }

    LaunchedEffect(annotation.colorHex, annotation.isPinned, annotation.isFolded, annotation.alignment) {
        cardColorHex = annotation.colorHex
        isPinned = annotation.isPinned
        isFolded = annotation.isFolded
        textAlign = when (annotation.alignment) {
            "CENTER" -> TextAlign.Center
            "END" -> TextAlign.End
            else -> TextAlign.Start
        }
    }

    // Synchronize layout coordinates when not dragging
    LaunchedEffect(annotation.xNorm, annotation.yNorm, annotation.widthNorm, annotation.heightNorm) {
        if (!isDraggingHandle) {
            normX = annotation.xNorm
            normY = annotation.yNorm
            normW = annotation.widthNorm.coerceAtLeast(0.18f)
            normH = annotation.heightNorm
        }
    }

    val pixelX = normX * pageWidthPx
    val pixelY = normY * pageHeightPx
    val pixelW = normW * pageWidthPx
    val boxWidthDp = with(density) { pixelW.toDp() }

    // Commit helper
    fun commitChanges(
        newX: Float = normX,
        newY: Float = normY,
        newW: Float = normW,
        newH: Float = normH,
        newTitle: String = titleValue.text,
        newContent: String = bodyValue.text,
        newColorHex: Long = cardColorHex,
        newPinned: Boolean = isPinned,
        newFolded: Boolean = isFolded,
        newTextAlign: TextAlign = textAlign
    ) {
        val alignmentStr = when (newTextAlign) {
            TextAlign.Center -> "CENTER"
            TextAlign.End, TextAlign.Right -> "END"
            else -> "START"
        }
        onAnnotationChanged(
            annotation.copy(
                xNorm = newX,
                yNorm = newY,
                widthNorm = newW,
                heightNorm = newH,
                title = newTitle,
                content = newContent,
                colorHex = newColorHex,
                isPinned = newPinned,
                isFolded = newFolded,
                alignment = alignmentStr
            )
        )
    }

    // ─── Folded Mode: Minimized floating Post-it chip ─────────────────────────
    if (isFolded) {
        Box(
            modifier = modifier
                .offset { IntOffset(pixelX.roundToInt(), pixelY.roundToInt()) }
                .size(38.dp)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(10.dp),
                    spotColor = Color.Black.copy(alpha = 0.25f)
                )
                .clip(RoundedCornerShape(10.dp))
                .background(Color(cardColorHex))
                .border(
                    width = 1.dp,
                    color = Color.Black.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp)
                )
                .then(
                    if (!isReadOnly && !isPinned) {
                        Modifier.pointerInput(annotation.id) {
                            detectDragGestures(
                                onDragStart = { isDraggingHandle = true },
                                onDragEnd = {
                                    isDraggingHandle = false
                                    commitChanges()
                                },
                                onDragCancel = { isDraggingHandle = false }
                            ) { change, dragAmount ->
                                change.consume()
                                val deltaXNorm = dragAmount.x / pageWidthPx
                                val deltaYNorm = dragAmount.y / pageHeightPx
                                val (newX, newY) = TextAnnotationTransformSolver.translate(
                                    currentX = normX,
                                    currentY = normY,
                                    width = 38.dp.toPx() / pageWidthPx,
                                    height = 38.dp.toPx() / pageHeightPx,
                                    deltaXNorm = deltaXNorm,
                                    deltaYNorm = deltaYNorm
                                )
                                normX = newX
                                normY = newY
                            }
                        }
                    } else Modifier
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isFolded = false
                    isEditing = true
                    onSelect()
                    commitChanges(newFolded = false)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.StickyNote2,
                contentDescription = "Expand Sticky Card",
                tint = Color(0xFF1C1B1F).copy(alpha = 0.75f),
                modifier = Modifier.size(20.dp)
            )
            if (isPinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = AccentBlue,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
        return
    }

    // ─── Expanded Mode: Full Post-it Card ─────────────────────────────────────
    val titleStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1C1B1F),
        lineHeight = 20.sp,
        textAlign = textAlign
    )

    val bodyStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = Color(0xFF2C2C2C),
        lineHeight = 19.sp,
        textAlign = textAlign
    )

    Box(
        modifier = modifier
            .offset { IntOffset(pixelX.roundToInt(), pixelY.roundToInt()) }
            .width(boxWidthDp.coerceAtLeast(160.dp))
            .wrapContentHeight(align = Alignment.Top)
            .onGloballyPositioned { coordinates ->
                if (isEditing) {
                    val pos = coordinates.positionInRoot()
                    val sz = coordinates.size
                    onPositionChanged(
                        Rect(
                            pos,
                            Size(sz.width.toFloat(), sz.height.toFloat())
                        )
                    )
                }
            }
    ) {
        // ─── Main Card Surface ───────────────────────────────────────────────
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top)
                .shadow(
                    elevation = if (isEditing) 6.dp else 2.dp,
                    shape = RoundedCornerShape(14.dp),
                    spotColor = Color.Black.copy(alpha = 0.22f)
                )
                .border(
                    width = if (isEditing) 1.5.dp else 1.dp,
                    color = if (isEditing) AccentBlue else Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp)
                )
                .onSizeChanged { size ->
                    val measuredH = size.height.toFloat()
                    val computedNormH = TextAnnotationTransformSolver.computeNormalizedHeight(measuredH, pageHeightPx)
                    if (kotlin.math.abs(computedNormH - normH) > 0.002f) {
                        normH = computedNormH
                        if (!isDraggingHandle) {
                            commitChanges(newH = computedNormH)
                        }
                    }
                }
                .then(
                    if (!isReadOnly) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isEditing) {
                                isEditing = true
                                onSelect()
                            }
                        }
                    } else Modifier
                ),
            shape = RoundedCornerShape(14.dp),
            color = Color(cardColorHex)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // ─── Header Bar: Title + Action Controls ─────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Title Input / Display
                    Box(modifier = Modifier.weight(1f)) {
                        if (isEditing && !isReadOnly && !isPinned) {
                            BasicTextField(
                                value = titleValue,
                                onValueChange = { newValue ->
                                    titleValue = newValue
                                    commitChanges(newTitle = newValue.text)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(titleFocusRequester),
                                textStyle = titleStyle,
                                singleLine = true,
                                cursorBrush = SolidColor(AccentBlue),
                                decorationBox = { innerTextField ->
                                    if (titleValue.text.isEmpty()) {
                                        Text(
                                            text = "Title...",
                                            style = titleStyle.copy(color = Color.Black.copy(alpha = 0.35f)),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        } else {
                            Text(
                                text = titleValue.text.ifEmpty { if (isToolActive) "Title" else "" },
                                style = if (titleValue.text.isEmpty()) titleStyle.copy(color = Color.Black.copy(alpha = 0.35f)) else titleStyle,
                                maxLines = 1,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Action Controls in Header (Editing Mode)
                    if (isEditing && !isReadOnly) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pin Toggle
                            IconButton(
                                onClick = {
                                    val newPinned = !isPinned
                                    isPinned = newPinned
                                    commitChanges(newPinned = newPinned)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = if (isPinned) "Unpin Sticky Card" else "Pin Sticky Card",
                                    tint = if (isPinned) AccentBlue else Color.Black.copy(alpha = 0.55f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Fold / Minimize Toggle
                            IconButton(
                                onClick = {
                                    isFolded = true
                                    isEditing = false
                                    onDeselect()
                                    commitChanges(newFolded = true)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.UnfoldLess,
                                    contentDescription = "Fold Sticky Card",
                                    tint = Color.Black.copy(alpha = 0.55f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Color Palette Trigger
                            Box {
                                IconButton(
                                    onClick = { showColorPalette = !showColorPalette },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Change Pastel Color",
                                        tint = Color.Black.copy(alpha = 0.55f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showColorPalette,
                                    onDismissRequest = { showColorPalette = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                StickyCardPalette.ALL.forEach { hex ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(26.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(hex))
                                                            .border(
                                                                width = if (cardColorHex == hex) 2.dp else 1.dp,
                                                                color = if (cardColorHex == hex) AccentBlue else Color.Black.copy(alpha = 0.15f),
                                                                shape = CircleShape
                                                            )
                                                            .clickable {
                                                                cardColorHex = hex
                                                                commitChanges(newColorHex = hex)
                                                                showColorPalette = false
                                                            }
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {}
                                    )
                                }
                            }

                            // Text Alignment Cycle Button (Start -> Center -> End)
                            IconButton(
                                onClick = {
                                    val nextAlign = when (textAlign) {
                                        TextAlign.Start, TextAlign.Left -> TextAlign.Center
                                        TextAlign.Center -> TextAlign.End
                                        else -> TextAlign.Start
                                    }
                                    textAlign = nextAlign
                                    commitChanges(newTextAlign = nextAlign)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = when (textAlign) {
                                        TextAlign.Center -> Icons.Default.FormatAlignCenter
                                        TextAlign.End, TextAlign.Right -> Icons.Default.FormatAlignRight
                                        else -> Icons.Default.FormatAlignLeft
                                    },
                                    contentDescription = "Cycle Text Alignment",
                                    tint = Color.Black.copy(alpha = 0.55f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Delete Button
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete Sticky Card",
                                    tint = Color.Black.copy(alpha = 0.55f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Done Button
                            IconButton(
                                onClick = {
                                    isEditing = false
                                    keyboardController?.hide()
                                    onDeselect()
                                    commitChanges()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done Editing",
                                    tint = AccentBlue,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    } else if (isPinned) {
                        // Pinned subtle badge when not editing
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = Color.Black.copy(alpha = 0.35f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ─── Subtle Hairline Divider ─────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(Color.Black.copy(alpha = 0.08f))
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ─── Body Text Area ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                ) {
                    if (isEditing && !isReadOnly && !isPinned) {
                        BasicTextField(
                            value = bodyValue,
                            onValueChange = { newValue ->
                                bodyValue = newValue
                                commitChanges(newContent = newValue.text)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = bodyStyle,
                            cursorBrush = SolidColor(AccentBlue),
                            decorationBox = { innerTextField ->
                                if (bodyValue.text.isEmpty()) {
                                    Text(
                                        text = "Take a note...",
                                        style = bodyStyle.copy(color = Color.Black.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                innerTextField()
                            }
                        )
                    } else {
                        Text(
                            text = bodyValue.text.ifEmpty { if (isToolActive) "Tap to write..." else "" },
                            style = if (bodyValue.text.isEmpty()) bodyStyle.copy(color = Color.Black.copy(alpha = 0.35f)) else bodyStyle,
                            softWrap = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // ─── Transform Handles (Editing Mode, when not pinned) ───────────────
        if (isEditing && !isReadOnly && !isPinned) {
            // Top Drag Handle (Move Card)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-14).dp)
                    .size(width = 56.dp, height = 36.dp)
                    .pointerInput(annotation.id) {
                        detectDragGestures(
                            onDragStart = { isDraggingHandle = true },
                            onDragEnd = {
                                isDraggingHandle = false
                                commitChanges()
                            },
                            onDragCancel = { isDraggingHandle = false }
                        ) { change, dragAmount ->
                            change.consume()
                            val deltaXNorm = dragAmount.x / pageWidthPx
                            val deltaYNorm = dragAmount.y / pageHeightPx
                            val (newX, newY) = TextAnnotationTransformSolver.translate(
                                currentX = normX,
                                currentY = normY,
                                width = normW,
                                height = normH,
                                deltaXNorm = deltaXNorm,
                                deltaYNorm = deltaYNorm
                            )
                            normX = newX
                            normY = newY
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Visual Pill Handle (48dp x 18dp)
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(AccentBlue),
                    contentAlignment = Alignment.Center
                ) {
                    // Grip indicator dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            // Left Pill Handle (Opposite Right Anchor Remains Stationary)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-5).dp)
                    .width(8.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentBlue)
                    .pointerInput(annotation.id) {
                        detectDragGestures(
                            onDragStart = { isDraggingHandle = true },
                            onDragEnd = {
                                isDraggingHandle = false
                                commitChanges()
                            },
                            onDragCancel = { isDraggingHandle = false }
                        ) { change, dragAmount ->
                            change.consume()
                            val deltaXNorm = dragAmount.x / pageWidthPx
                            val (newX, newW) = TextAnnotationTransformSolver.resizeHorizontal(
                                currentX = normX,
                                currentWidth = normW,
                                deltaXNorm = deltaXNorm,
                                isLeftHandle = true,
                                minWidthNorm = 0.18f
                            )
                            normX = newX
                            normW = newW
                        }
                    }
            )

            // Right Pill Handle (Opposite Left Anchor Remains Stationary)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 5.dp)
                    .width(8.dp)
                    .height(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentBlue)
                    .pointerInput(annotation.id) {
                        detectDragGestures(
                            onDragStart = { isDraggingHandle = true },
                            onDragEnd = {
                                isDraggingHandle = false
                                commitChanges()
                            },
                            onDragCancel = { isDraggingHandle = false }
                        ) { change, dragAmount ->
                            change.consume()
                            val deltaXNorm = dragAmount.x / pageWidthPx
                            val (newX, newW) = TextAnnotationTransformSolver.resizeHorizontal(
                                currentX = normX,
                                currentWidth = normW,
                                deltaXNorm = deltaXNorm,
                                isLeftHandle = false,
                                minWidthNorm = 0.18f
                            )
                            normX = newX
                            normW = newW
                        }
                    }
            )
        }
    }

    // Auto-focus title and deploy IME when entering editing mode (if not pinned)
    LaunchedEffect(isEditing) {
        if (isEditing && !isReadOnly && !isPinned) {
            titleFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }
}
