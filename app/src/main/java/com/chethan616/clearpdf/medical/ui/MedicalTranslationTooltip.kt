package com.chethan616.clearpdf.medical.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chethan616.clearpdf.medical.domain.TranslationResult
import com.chethan616.clearpdf.medical.model.MedicalDomain
import com.chethan616.clearpdf.medical.viewmodel.SelectionOverlayState
import com.chethan616.clearpdf.ui.theme.LocalIsDarkMode
import com.chethan616.clearpdf.ui.screen.components.SelectionMenuItem
import com.chethan616.clearpdf.ui.screen.components.TextSelectionActionBar
import com.kyant.backdrop.Backdrop
import com.malhoutha.ui.components.GlassCard
import com.malhoutha.ui.theme.GlassLevel
import kotlin.math.roundToInt

/**
 * Calculates a viewport-safe offset for the floating translation hover card.
 * Centers horizontally over [anchorScreenX], clamped strictly within [edgePaddingPx] and screen bounds.
 * Smartly flips vertically: prefers displaying above [anchorScreenY] ([anchorTopY]), but flips below
 * [anchorBottomY] if upper space is insufficient.
 */
fun calculateClampedTooltipOffset(
    anchorScreenX: Float,
    anchorScreenY: Float,
    cardWidthPx: Float,
    cardHeightPx: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    verticalGapPx: Float = 16f,
    edgePaddingPx: Float = 24f,
    anchorBottomY: Float = anchorScreenY
): IntOffset {
    val targetX = anchorScreenX - (cardWidthPx / 2f)
    val maxClampedX = (viewportWidthPx - cardWidthPx - edgePaddingPx).coerceAtLeast(edgePaddingPx)
    val finalX = targetX.coerceIn(edgePaddingPx, maxClampedX)

    val targetYAbove = anchorScreenY - cardHeightPx - verticalGapPx
    val rawY = if (targetYAbove < edgePaddingPx) {
        anchorBottomY + verticalGapPx
    } else {
        targetYAbove
    }
    val maxClampedY = (viewportHeightPx - cardHeightPx - edgePaddingPx).coerceAtLeast(edgePaddingPx)
    val finalY = rawY.coerceIn(edgePaddingPx, maxClampedY)

    return IntOffset(finalX.roundToInt(), finalY.roundToInt())
}

/** Overload adhering to the strict 8-parameter anchorScreenY contract. */
fun calculateClampedTooltipOffset(
    anchorScreenX: Float,
    anchorScreenY: Float,
    cardWidthPx: Float,
    cardHeightPx: Float,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    verticalGapPx: Float = 16f,
    edgePaddingPx: Float = 24f
): IntOffset {
    return calculateClampedTooltipOffset(
        anchorScreenX = anchorScreenX,
        anchorScreenY = anchorScreenY,
        cardWidthPx = cardWidthPx,
        cardHeightPx = cardHeightPx,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        verticalGapPx = verticalGapPx,
        edgePaddingPx = edgePaddingPx,
        anchorBottomY = anchorScreenY
    )
}

/**
 * Two-tier floating medical selection action strip and translation UI.
 * - Selection Phase: Compact frosted glass action strip with prominent Translate button, Highlight, Copy & Note.
 * - Translation Phase: Tapping Translate smoothly morphs into [CompactHoverPill] (RTL term, domain badge, audio, expand).
 * - Detailed Sheet: [MedicalDetailedTranslationSheet] (Full clinical definition, Latin binomial root, academic breakdown).
 */
@Composable
fun MedicalTranslationTooltip(
    state: MedicalTooltipUiState,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onCopyTranslation: (String) -> Unit,
    onInsertStickyNote: (TranslationResult) -> Unit,
    onDownloadModel: (() -> Unit)? = null,
    onBookmarkCard: ((TranslationResult) -> Unit)? = null,
    onSpeakTerm: ((text: String, isLatin: Boolean) -> Unit)? = null,
    onExpandDetails: () -> Unit = {},
    onCollapseDetails: () -> Unit = {},
    currentlyPlayingAudioText: String? = null,
    onTranslateAction: (() -> Unit)? = null,
    onHighlightAction: (() -> Unit)? = null,
    onCopySelectedText: (() -> Unit)? = null,
    onAddNoteAction: (() -> Unit)? = null,
    highlightColor: Color = Color(0xFFFFD60A),
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    val isDark = LocalIsDarkMode.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val screenW = maxWidth
        val screenH = maxHeight
        val viewportWPx = screenW.value * density.density
        val viewportHPx = screenH.value * density.density
        val edgePaddingPx = with(density) { 24.dp.toPx() }

        val isExpanded = state.isExpanded
        val bounds = state.selectionBoundsInWindow

        val cardWidthDp = if (isExpanded) {
            360.dp.coerceAtMost(screenW - 32.dp)
        } else {
            340.dp.coerceAtMost(screenW - 32.dp)
        }
        val cardWidthPx = with(density) { cardWidthDp.toPx() }
        val estimatedCardHeightPx = with(density) {
            if (isExpanded) 420.dp.toPx() else 54.dp.toPx()
        }
        val verticalGapPx = with(density) { if (isExpanded) 12.dp.toPx() else 8.dp.toPx() }

        val anchorX = if (bounds.width > 0f) bounds.center.x else viewportWPx / 2f
        val anchorTopY = if (bounds.height > 0f) bounds.top else bounds.center.y
        val anchorBottomY = if (bounds.height > 0f) bounds.bottom else bounds.center.y

        val clampedOffset = calculateClampedTooltipOffset(
            anchorScreenX = anchorX,
            anchorScreenY = anchorTopY,
            cardWidthPx = cardWidthPx,
            cardHeightPx = estimatedCardHeightPx,
            viewportWidthPx = viewportWPx,
            viewportHeightPx = viewportHPx,
            verticalGapPx = verticalGapPx,
            edgePaddingPx = edgePaddingPx,
            anchorBottomY = anchorBottomY
        )

        val placeAbove = (anchorTopY - estimatedCardHeightPx - verticalGapPx) >= edgePaddingPx
        val originY = if (placeAbove) 1f else 0f
        val originX = if (cardWidthPx > 0f) ((anchorX - clampedOffset.x) / cardWidthPx).coerceIn(0f, 1f) else 0.5f

        AnimatedVisibility(
            visible = state.isVisible,
            enter = fadeIn() + scaleIn(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                initialScale = 0.90f,
                transformOrigin = TransformOrigin(originX, originY)
            ),
            exit = fadeOut() + scaleOut(
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                targetScale = 0.94f,
                transformOrigin = TransformOrigin(originX, originY)
            ),
            modifier = Modifier.offset { clampedOffset }
        ) {
            if (isExpanded) {
                MedicalDetailedTranslationSheet(
                    state = state,
                    onDismiss = onDismiss,
                    onCollapse = onCollapseDetails,
                    onCopyTranslation = onCopyTranslation,
                    onInsertStickyNote = onInsertStickyNote,
                    onBookmarkCard = onBookmarkCard,
                    onDownloadModel = onDownloadModel,
                    onSpeakTerm = onSpeakTerm,
                    currentlyPlayingAudioText = currentlyPlayingAudioText,
                    modifier = Modifier.width(cardWidthDp)
                )
            } else {
                AnimatedContent(
                    targetState = state.overlayState,
                    transitionSpec = {
                        (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                                scaleIn(
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                    initialScale = 0.92f
                                )) togetherWith
                                (fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                                        scaleOut(
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            targetScale = 0.95f
                                        )) using
                                SizeTransform(clip = false) { _, _ ->
                                    spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                                }
                    },
                    label = "selectionOverlayMorph"
                ) { overlayState ->
                    when (overlayState) {
                        is SelectionOverlayState.ActionStripVisible -> {
                            SelectionActionToolbar(
                                selectedText = state.selectedText,
                                onTranslateClick = onTranslateAction ?: onExpandDetails,
                                onHighlightClick = onHighlightAction,
                                onCopyClick = onCopySelectedText ?: { onCopyTranslation(state.selectedText) },
                                onAddNoteClick = onAddNoteAction ?: {
                                    val res = state.result ?: TranslationResult.NotFound(state.selectedText, "")
                                    onInsertStickyNote(res)
                                },
                                highlightColor = highlightColor,
                                isDark = isDark
                            )
                        }

                        is SelectionOverlayState.TranslationPillVisible -> {
                            CompactHoverPill(
                                state = state,
                                onExpandDetails = onExpandDetails,
                                onDismiss = onDismiss,
                                onSpeakTerm = onSpeakTerm,
                                currentlyPlayingAudioText = currentlyPlayingAudioText,
                                isDark = isDark,
                                modifier = Modifier.wrapContentSize()
                            )
                        }

                        else -> {
                            // Fallback if overlayState is Idle but isVisible was explicitly set
                            if (state.result != null || state.isLoading) {
                                CompactHoverPill(
                                    state = state,
                                    onExpandDetails = onExpandDetails,
                                    onDismiss = onDismiss,
                                    onSpeakTerm = onSpeakTerm,
                                    currentlyPlayingAudioText = currentlyPlayingAudioText,
                                    isDark = isDark,
                                    modifier = Modifier.wrapContentSize()
                                )
                            } else {
                                SelectionActionToolbar(
                                    selectedText = state.selectedText,
                                    onTranslateClick = onTranslateAction ?: onExpandDetails,
                                    onHighlightClick = onHighlightAction,
                                    onCopyClick = onCopySelectedText ?: { onCopyTranslation(state.selectedText) },
                                    onAddNoteClick = onAddNoteAction ?: {
                                        val res = state.result ?: TranslationResult.NotFound(state.selectedText, "")
                                        onInsertStickyNote(res)
                                    },
                                    highlightColor = highlightColor,
                                    isDark = isDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Restored floating text selection action bar providing explicit, high-visibility actions
 * (Translate, Highlight, Copy, Add Note) anchored directly at selection coordinates.
 * Seamlessly morphs into [CompactHoverPill] upon tapping "Translate".
 */
@Composable
fun SelectionActionToolbar(
    selectedText: String,
    onTranslateClick: () -> Unit,
    onCopyClick: () -> Unit,
    onAddNoteClick: () -> Unit,
    onHighlightClick: (() -> Unit)? = null,
    highlightColor: Color = Color(0xFFFFD60A),
    isDark: Boolean = LocalIsDarkMode.current,
    modifier: Modifier = Modifier
) {
    TextSelectionActionBar(
        onCopyClick = onCopyClick,
        onHighlightClick = onHighlightClick ?: {},
        onTranslateClick = onTranslateClick,
        onAddNoteClick = onAddNoteClick,
        modifier = modifier
    )
}


/**
 * Tier A: Sleek compact floating glass pill displaying zero-click instant translation.
 * Lightweight frosted glass pill (height ~38dp-44dp) showing RTL primary Arabic translation,
 * clinical subspecialty badge, audio pronunciation, and expand chevron trigger.
 */
@Composable
private fun CompactHoverPill(
    state: MedicalTooltipUiState,
    onExpandDetails: () -> Unit,
    onDismiss: () -> Unit,
    onSpeakTerm: ((text: String, isLatin: Boolean) -> Unit)? = null,
    currentlyPlayingAudioText: String? = null,
    isDark: Boolean = LocalIsDarkMode.current,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val result = state.result
    val pillShape = RoundedCornerShape(22.dp)

    GlassCard(
        level = GlassLevel.FLOATING_DOCK,
        shape = pillShape,
        isDark = isDark,
        modifier = modifier
            .wrapContentSize()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    )
                ),
                shape = pillShape
            )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "جاري الترجمة...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }

                result is TranslationResult.NotFound -> {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "لم يُعثر على ترجمة",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDirection = TextDirection.Rtl,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    VerticalDivider(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onExpandDetails()
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "بحث",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                result is TranslationResult.ModelDownloadRequired -> {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "يلزم تنزيل النموذج",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDirection = TextDirection.Rtl,
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                    VerticalDivider(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onExpandDetails()
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تنزيل",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Download model",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                else -> {
                    // 1. Primary Arabic Term (RTL, bold, high-contrast)
                    val arabicText = when (result) {
                        is TranslationResult.DecomposedCompoundMatch -> result.synthesizedArabicText
                        else -> result?.targetArabicText.orEmpty()
                    }
                    if (arabicText.isNotBlank()) {
                        Text(
                            text = arabicText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDirection = TextDirection.Rtl,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 2. Subspecialty Badge or Latin Root (if compact space permits)
                    MedicalDomainBadge(result = result, isDark = isDark)

                    // 3. Quick Audio Pronunciation Button
                    if (onSpeakTerm != null) {
                        val speechText = when (result) {
                            is TranslationResult.LexicalMatch -> result.matchedTerm ?: result.sourceText
                            else -> result?.sourceText.orEmpty()
                        }
                        if (speechText.isNotBlank()) {
                            val isPlaying = currentlyPlayingAudioText?.equals(speechText.trim(), ignoreCase = true) == true
                            IconButton(
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSpeakTerm(speechText, false)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = "Pronounce",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    VerticalDivider(modifier = Modifier.height(16.dp))

                    // 4. "More Details" / Expand Trigger
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onExpandDetails()
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "Expand details",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // 5. Subtle close trigger
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDismiss()
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
