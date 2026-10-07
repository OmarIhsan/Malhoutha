package com.chethan616.clearpdf.medical.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chethan616.clearpdf.medical.domain.ExtractedMedicalEntity
import com.chethan616.clearpdf.medical.domain.TranslationResult
import com.chethan616.clearpdf.medical.domain.TranslationTier
import com.chethan616.clearpdf.medical.model.MedicalDomain
import com.chethan616.clearpdf.ui.components.viewerChromeGlass
import com.chethan616.clearpdf.ui.components.viewerGlass
import com.chethan616.clearpdf.ui.theme.LocalIsDarkMode
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.RoundedRectangle
import com.malhoutha.R
import kotlin.math.roundToInt

/**
 * Luminous frosted-glass hover card positioned dynamically relative to the highlighted text selection bounds.
 * Displays validated WHO UMD Arabic translations, Latin terminology roots, clinical domain badges,
 * and quick actions (Copy and "Insert as Sticky Note").
 */
@Composable
fun MedicalTranslationTooltip(
    state: MedicalTooltipUiState,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onCopyTranslation: (String) -> Unit,
    onInsertStickyNote: (TranslationResult) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    val isDark = LocalIsDarkMode.current
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val screenW = maxWidth
        val screenH = maxHeight
        val marginPx = with(density) { 12.dp.toPx() }
        val gapPx = with(density) { 8.dp.toPx() }
        val topSafe = WindowInsets.statusBars.getTop(density) + marginPx
        val bottomSafe = screenH.value * density.density - WindowInsets.navigationBars.getBottom(density) - marginPx

        val bounds = state.selectionBoundsInWindow
        val cardWidthDp = 350.dp.coerceAtMost(screenW - 24.dp)
        val cardWidthPx = with(density) { cardWidthDp.toPx() }
        val estimatedCardHeightPx = with(density) { 340.dp.toPx() }

        // Determine vertical placement: above selection if space permits, otherwise below
        val placeAbove = (bounds.top - gapPx - estimatedCardHeightPx) >= topSafe
        val rawY = if (placeAbove) {
            bounds.top - gapPx - estimatedCardHeightPx
        } else {
            bounds.bottom + gapPx
        }
        val clampedY = rawY.coerceIn(topSafe, (bottomSafe - estimatedCardHeightPx).coerceAtLeast(topSafe))

        // Center horizontally on selection bounds and clamp within screen margins
        val centerX = if (bounds.width > 0f) bounds.center.x else screenW.value * density.density / 2f
        val rawX = centerX - cardWidthPx / 2f
        val clampedX = rawX.coerceIn(marginPx, (screenW.value * density.density - cardWidthPx - marginPx).coerceAtLeast(marginPx))

        val originY = if (placeAbove) 1f else 0f
        val originX = if (cardWidthPx > 0f) ((centerX - clampedX) / cardWidthPx).coerceIn(0f, 1f) else 0.5f

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
            modifier = Modifier.offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
        ) {
            val glassTint = viewerChromeGlass(isDark)
            val fg = if (isDark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E)
            val fgSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF636366)

            Column(
                Modifier
                    .width(cardWidthDp)
                    .heightIn(max = 440.dp)
                    .viewerGlass(backdrop, glassTint, shape = { RoundedRectangle(24.dp) })
                    .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1F000000), RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                // ── Header: Domain Badge, Subspecialty & Dismiss ─────────────────
                val result = state.result
                val isGeneralVocab = (result as? TranslationResult.LexicalMatch)?.sourceLexicon == "GENERAL_ACADEMIC_VOCAB"
                val domain = result?.domain ?: MedicalDomain.GENERAL_CLINICAL
                val domainColor = if (isGeneralVocab) {
                    if (isDark) Color(0xFFB388FF) else Color(0xFF7C4DFF)
                } else {
                    getDomainColor(domain)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Domain Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(domainColor.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isGeneralVocab) Icons.Rounded.Translate else getDomainIcon(domain),
                                contentDescription = null,
                                tint = domainColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isGeneralVocab) "GENERAL" else domain.name.replace('_', ' '),
                                color = domainColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Tier Pill
                        val tierText = when {
                            isGeneralVocab -> "GENERAL • VOCAB"
                            result?.tierUsed == TranslationTier.TIER_1_LEXICON -> "TIER 1 • LEXICON"
                            result?.tierUsed == TranslationTier.TIER_2_ON_DEVICE_SLM -> "TIER 2 • ON-DEVICE SLM"
                            else -> null
                        }
                        if (tierText != null) {
                            Text(
                                text = tierText,
                                color = fgSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isDark) Color(0x22FFFFFF) else Color(0x10000000))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0x20FFFFFF) else Color(0x10000000))
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss",
                            tint = fgSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Body: Loading / Content / NotFound ────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    when {
                        state.isLoading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = domainColor,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = "Translating offline...",
                                    color = fgSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        result is TranslationResult.LexicalMatch -> {
                            LexicalMatchContent(
                                match = result,
                                fg = fg,
                                fgSecondary = fgSecondary,
                                domainColor = domainColor,
                                isDark = isDark
                            )
                        }

                        result is TranslationResult.ContextualSentence -> {
                            ContextualSentenceContent(
                                sentence = result,
                                fg = fg,
                                fgSecondary = fgSecondary,
                                domainColor = domainColor,
                                isDark = isDark,
                                onCopyEntity = onCopyTranslation
                            )
                        }

                        result is TranslationResult.NotFound || state.errorMessage != null -> {
                            NotFoundContent(
                                text = state.selectedText,
                                reason = state.errorMessage ?: (result as? TranslationResult.NotFound)?.reason ?: "Term not found in lexicon.",
                                fg = fg,
                                fgSecondary = fgSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Footer: Action Buttons & Attribution ──────────────────────────
                if (result != null && result !is TranslationResult.NotFound && !state.isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Copy Action
                        TooltipActionButton(
                            label = "Copy",
                            icon = Icons.Rounded.ContentCopy,
                            fg = fg,
                            tint = if (isDark) Color(0x30FFFFFF) else Color(0x18000000),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val textToCopy = when (result) {
                                    is TranslationResult.LexicalMatch -> "${result.targetArabicText} (${result.sourceText})"
                                    is TranslationResult.ContextualSentence -> result.targetArabicText
                                    else -> result.targetArabicText
                                }
                                onCopyTranslation(textToCopy)
                            }
                        )

                        // Insert as Sticky Note Action
                        TooltipActionButton(
                            label = "Sticky Note",
                            icon = Icons.Rounded.StickyNote2,
                            fg = Color.White,
                            tint = domainColor,
                            modifier = Modifier.weight(1.3f),
                            onClick = {
                                onInsertStickyNote(result)
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Source attribution footer
                    val sourceText = when (result) {
                        is TranslationResult.LexicalMatch -> if (result.sourceLexicon == "GENERAL_ACADEMIC_VOCAB") {
                            "Source: General Academic Vocabulary"
                        } else {
                            "Source: ${result.sourceLexicon}"
                        }
                        is TranslationResult.ContextualSentence -> "Engine: On-Device Contextual SLM"
                        else -> "Malhoutha Medical Engine"
                    }
                    Text(
                        text = sourceText,
                        color = fgSecondary.copy(alpha = 0.7f),
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun LexicalMatchContent(
    match: TranslationResult.LexicalMatch,
    fg: Color,
    fgSecondary: Color,
    domainColor: Color,
    isDark: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // English Source Term
        Text(
            text = match.sourceText,
            color = fg,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(4.dp))

        // Arabic Primary Translation (prominent, RTL)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = match.targetArabicText,
                color = domainColor,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Latin Name root (if distinct and not general vocabulary)
        if (!match.latinName.isNullOrBlank() && match.sourceLexicon != "GENERAL_ACADEMIC_VOCAB") {
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Latin:",
                    color = fgSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = match.latinName,
                    color = fg,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Subspecialty Tag / Part of Speech
        if (!match.subspecialty.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            val subLabel = if (match.sourceLexicon == "GENERAL_ACADEMIC_VOCAB") {
                "• ${match.subspecialty.lowercase()}"
            } else {
                "• ${match.subspecialty.replace('_', ' ')}"
            }
            Text(
                text = subLabel,
                color = fgSecondary,
                fontSize = 11.sp
            )
        }

        // Definitions
        if (!match.definitionAr.isNullOrBlank() || !match.definitionEn.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0x18FFFFFF) else Color(0x0C000000))
                    .padding(10.dp)
            ) {
                if (!match.definitionAr.isNullOrBlank()) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = match.definitionAr,
                            color = fg,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            textAlign = TextAlign.Right
                        )
                    }
                }
                if (!match.definitionEn.isNullOrBlank()) {
                    if (!match.definitionAr.isNullOrBlank()) Spacer(Modifier.height(6.dp))
                    Text(
                        text = match.definitionEn,
                        color = fgSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContextualSentenceContent(
    sentence: TranslationResult.ContextualSentence,
    fg: Color,
    fgSecondary: Color,
    domainColor: Color,
    isDark: Boolean,
    onCopyEntity: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Arabic Translation (prominent)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = sentence.targetArabicText,
                color = fg,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 23.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(8.dp))

        // Clinical Notes callout
        if (!sentence.clinicalNotes.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(domainColor.copy(alpha = 0.12f))
                    .padding(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = domainColor,
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(Modifier.width(6.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = sentence.clinicalNotes,
                        color = fg,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Highlighted entities
        if (sentence.highlightedEntities.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Identified Medical Terms:",
                color = fgSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sentence.highlightedEntities.forEach { entity ->
                    EntityChip(entity = entity, isDark = isDark, onClick = {
                        onCopyEntity("${entity.arabicEquivalent} (${entity.englishTerm})")
                    })
                }
            }
        }
    }
}

@Composable
private fun EntityChip(
    entity: ExtractedMedicalEntity,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val domainColor = getDomainColor(entity.domain)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) Color(0x22FFFFFF) else Color(0x10000000))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onClick()
            }
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(domainColor)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "${entity.englishTerm}: ${entity.arabicEquivalent}",
            color = if (isDark) Color(0xFFECECEC) else Color(0xFF2C2C2E),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NotFoundContent(
    text: String,
    reason: String,
    fg: Color,
    fgSecondary: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = fgSecondary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "\"$text\"",
            color = fg,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = reason,
            color = fgSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TooltipActionButton(
    label: String,
    icon: ImageVector,
    fg: Color,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onClick()
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun getDomainColor(domain: MedicalDomain): Color {
    return when (domain) {
        MedicalDomain.PATHOLOGY -> Color(0xFFFF375F)
        MedicalDomain.ANATOMY -> Color(0xFF30D158)
        MedicalDomain.PHARMACOLOGY -> Color(0xFFFF9F0A)
        MedicalDomain.PROCEDURE -> Color(0xFF5E5CE6)
        MedicalDomain.DIAGNOSTIC -> Color(0xFFBF5AF2)
        MedicalDomain.GENERAL_CLINICAL -> Color(0xFF0A84FF)
    }
}

private fun getDomainIcon(domain: MedicalDomain): ImageVector {
    return when (domain) {
        MedicalDomain.PATHOLOGY -> Icons.Rounded.MedicalServices
        MedicalDomain.ANATOMY -> Icons.Rounded.LocalHospital
        MedicalDomain.PHARMACOLOGY -> Icons.Rounded.Medication
        MedicalDomain.PROCEDURE -> Icons.Rounded.MedicalServices
        MedicalDomain.DIAGNOSTIC -> Icons.Rounded.Psychology
        MedicalDomain.GENERAL_CLINICAL -> Icons.Rounded.LocalHospital
    }
}
