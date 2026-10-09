package com.chethan616.clearpdf.medical.ui

import android.graphics.RectF
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.chethan616.clearpdf.medical.domain.ExtractedMedicalEntity
import com.chethan616.clearpdf.medical.domain.TranslationResult
import com.chethan616.clearpdf.medical.viewmodel.DetailedTranslationState
import com.chethan616.clearpdf.ui.theme.LocalIsDarkMode
import com.malhoutha.ui.components.GlassCard
import com.malhoutha.ui.theme.GlassLevel

val TranslationResult.subspecialty: String?
    get() = when (this) {
        is TranslationResult.LexicalMatch -> subspecialty
        else -> null
    }

val TranslationResult.latinName: String?
    get() = when (this) {
        is TranslationResult.LexicalMatch -> latinName
        is TranslationResult.DecomposedCompoundMatch -> subTokens.firstOrNull { !it.latinRoot.isNullOrBlank() }?.latinRoot
        else -> null
    }

val TranslationResult.definitionEn: String?
    get() = when (this) {
        is TranslationResult.LexicalMatch -> definitionEn
        is TranslationResult.DecomposedCompoundMatch -> subTokens.mapNotNull { it.definitionEn }.joinToString("\n").ifBlank { null }
        else -> null
    }

val TranslationResult.definitionAr: String?
    get() = when (this) {
        is TranslationResult.LexicalMatch -> definitionAr
        is TranslationResult.DecomposedCompoundMatch -> subTokens.mapNotNull { it.definitionAr }.joinToString("\n").ifBlank { null }
        else -> null
    }

/**
 * Step 4: Unified Rich Detailed Translation Content.
 *
 * Single destination triggered from either entry point:
 * 1. "Translate" action in ClearPDF selection menu.
 * 2. "Details" / "التفاصيل" chip inside stacked quick translation pill.
 */
@Composable
fun MedicalDetailedTranslationContent(
    result: TranslationResult,
    sourceText: String,
    onPlayAudio: () -> Unit,
    onAddStickyNote: () -> Unit,
    onSaveToDeck: () -> Unit,
    onCopyText: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header Row: Source Term + Pronunciation + Close Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(
                    text = sourceText.ifBlank { result.sourceText },
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onPlayAudio, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Pronounce",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 2. Primary Arabic Translation Block
        if (result.targetArabicText.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = result.targetArabicText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textDirection = TextDirection.Rtl
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val spec = result.subspecialty
                    if (!spec.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = spec.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        } else if (result is TranslationResult.NotFound) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "لم يتم العثور على ترجمة معتمدة",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDirection = TextDirection.Rtl
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = result.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Anatomical Latin Nomenclature (if present)
        val latin = result.latinName
        if (!latin.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Latin:",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = latin,
                    style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 4. Clinical & Academic Definitions
        val defEn = result.definitionEn
        val defAr = result.definitionAr
        if (!defEn.isNullOrBlank() || !defAr.isNullOrBlank()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            if (!defEn.isNullOrBlank()) {
                Text(
                    text = defEn,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (!defAr.isNullOrBlank()) {
                Text(
                    text = defAr,
                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Rtl),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 5. Action Row: Sticky Note, Flashcard, Copy
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onAddStickyNote,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Icon(Icons.AutoMirrored.Rounded.NoteAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Note", style = MaterialTheme.typography.labelLarge)
            }
            OutlinedButton(
                onClick = onSaveToDeck,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Deck", style = MaterialTheme.typography.labelLarge)
            }
            IconButton(
                onClick = onCopyText,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Translation")
            }
        }
    }
}

/**
 * Step 4: Adaptive Form-Factor Modal / Sheet Presentation.
 *
 * - Tablets (>=840dp): Renders as an anchored, floating frosted glass card (width = 420.dp,
 *   clamped to screen margins) positioned adjacent to the selection bounds via Popup.
 * - Phones & Compact Screens (<840dp): Renders as an ergonomic ModalBottomSheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalDetailedTranslationModal(
    state: DetailedTranslationState,
    isTablet: Boolean,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    onDismiss: () -> Unit,
    onPlayAudio: () -> Unit,
    onAddStickyNote: () -> Unit,
    onSaveToDeck: () -> Unit,
    onCopyText: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return

    val isDark = LocalIsDarkMode.current
    val density = LocalDensity.current

    if (isTablet) {
        val cardWidthDp = 420.dp
        val cardWidthPx = with(density) { cardWidthDp.toPx() }
        val cardHeightPx = with(density) { 380.dp.toPx() }
        val anchorBounds = state.anchorBounds
        val anchorX = anchorBounds?.centerX() ?: (viewportWidthPx / 2f)
        val anchorTopY = anchorBounds?.top ?: (viewportHeightPx / 2f)
        val anchorBottomY = anchorBounds?.bottom ?: anchorTopY

        val clampedOffset = calculateClampedTooltipOffset(
            anchorScreenX = anchorX,
            anchorScreenY = anchorTopY,
            cardWidthPx = cardWidthPx,
            cardHeightPx = cardHeightPx,
            viewportWidthPx = viewportWidthPx,
            viewportHeightPx = viewportHeightPx,
            verticalGapPx = with(density) { 12.dp.toPx() },
            edgePaddingPx = with(density) { 24.dp.toPx() },
            anchorBottomY = anchorBottomY
        )

        Popup(
            offset = clampedOffset,
            onDismissRequest = onDismiss,
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            GlassCard(
                level = GlassLevel.MODAL_TOOLTIP,
                shape = RoundedCornerShape(24.dp),
                isDark = isDark,
                modifier = modifier
                    .width(cardWidthDp)
                    .heightIn(max = 520.dp)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.40f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                val result = state.result
                if (result != null) {
                    MedicalDetailedTranslationContent(
                        result = result,
                        sourceText = state.sourceText,
                        onPlayAudio = onPlayAudio,
                        onAddStickyNote = onAddStickyNote,
                        onSaveToDeck = onSaveToDeck,
                        onCopyText = onCopyText,
                        onClose = onDismiss
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF1E2430) else Color(0xFFFBFBFD),
            scrimColor = Color.Black.copy(alpha = 0.35f),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            val result = state.result
            if (result != null) {
                MedicalDetailedTranslationContent(
                    result = result,
                    sourceText = state.sourceText,
                    onPlayAudio = onPlayAudio,
                    onAddStickyNote = onAddStickyNote,
                    onSaveToDeck = onSaveToDeck,
                    onCopyText = onCopyText,
                    onClose = onDismiss,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

/**
 * Tier B: The Detailed Translation Sheet.
 *
 * Renders as an anchored glass sheet / popover containing:
 * - Full English clinical definition & Arabic academic explanation
 * - Latin binomial root with dedicated Latin pronunciation
 * - Subspecialty classification tag (e.g., Endodontics, Oral Pathology)
 * - Complete Action Row: Add as Sticky Note, Bookmark to Flashcard Deck, Copy Translation.
 */
@Composable
fun MedicalDetailedTranslationSheet(
    state: MedicalTooltipUiState,
    onDismiss: () -> Unit,
    onCollapse: () -> Unit,
    onCopyTranslation: (String) -> Unit,
    onInsertStickyNote: (TranslationResult) -> Unit,
    onBookmarkCard: ((TranslationResult) -> Unit)? = null,
    onDownloadModel: (() -> Unit)? = null,
    onSpeakTerm: ((text: String, isLatin: Boolean) -> Unit)? = null,
    currentlyPlayingAudioText: String? = null,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkMode.current
    val haptics = LocalHapticFeedback.current
    val fg = if (isDark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E)
    val fgSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF636366)

    val result = state.result
    val brandAccent = MaterialTheme.colorScheme.primary
    val sheetShape = RoundedCornerShape(24.dp)

    GlassCard(
        level = GlassLevel.MODAL_TOOLTIP,
        shape = sheetShape,
        isDark = isDark,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 480.dp)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        brandAccent.copy(alpha = 0.40f),
                        brandAccent.copy(alpha = 0.10f)
                    )
                ),
                shape = sheetShape
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ── Top Header: Domain Badge, Subspecialty & Collapse Button ──────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Unified Frosted Domain Badge
                    MedicalDomainBadge(result = result, isDark = isDark)

                    // Subspecialty Tag if present
                    val subspecialty = when (result) {
                        is TranslationResult.LexicalMatch -> result.subspecialty
                        else -> null
                    }
                    if (!subspecialty.isNullOrBlank()) {
                        Text(
                            text = "•  $subspecialty",
                            color = fgSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Collapse / Close Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x18FFFFFF) else Color(0x0C000000))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCollapse()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Collapse translation sheet",
                        tint = fgSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Scrollable Body: Definitions, Latin Root, Synonyms ──────────
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = brandAccent,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "جاري استرجاع التفاصيل السريرية...",
                                    color = fgSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    result is TranslationResult.LexicalMatch -> {
                        DetailedLexicalContent(
                            match = result,
                            fg = fg,
                            fgSecondary = fgSecondary,
                            brandAccent = brandAccent,
                            isDark = isDark,
                            onSpeakTerm = onSpeakTerm,
                            currentlyPlayingAudioText = currentlyPlayingAudioText
                        )
                    }

                    result is TranslationResult.DecomposedCompoundMatch -> {
                        DetailedCompoundContent(
                            match = result,
                            fg = fg,
                            fgSecondary = fgSecondary,
                            brandAccent = brandAccent,
                            isDark = isDark,
                            onSpeakTerm = onSpeakTerm,
                            currentlyPlayingAudioText = currentlyPlayingAudioText
                        )
                    }

                    result is TranslationResult.ContextualSentence -> {
                        DetailedContextualContent(
                            targetArabicText = result.targetArabicText,
                            entities = result.highlightedEntities,
                            fg = fg,
                            fgSecondary = fgSecondary,
                            brandAccent = brandAccent,
                            isDark = isDark
                        )
                    }

                    result is TranslationResult.ContextualMatch -> {
                        DetailedContextualContent(
                            targetArabicText = result.targetArabicText,
                            entities = result.highlightedEntities,
                            fg = fg,
                            fgSecondary = fgSecondary,
                            brandAccent = brandAccent,
                            isDark = isDark
                        )
                    }

                    result is TranslationResult.NotFound -> {
                        Text(
                            text = result.sourceText,
                            color = fg,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "لم يتم العثور على مصطلح طبي مباشر في المعجم الطبي.",
                            color = fgSecondary,
                            fontSize = 13.sp
                        )
                    }

                    state.errorMessage != null -> {
                        Text(
                            text = state.errorMessage,
                            color = Color(0xFFFF453A),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Bottom Action Row: Sticky Note, Bookmark, Copy ────────────
            if (result != null && result !is TranslationResult.NotFound) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Add as Margin Sticky Note
                    DetailedActionButton(
                        icon = Icons.Rounded.StickyNote2,
                        label = "ملاحظة لاصقة",
                        tint = brandAccent,
                        isDark = isDark,
                        modifier = Modifier.weight(1.2f),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            onInsertStickyNote(result)
                        }
                    )

                    // 2. Save to Flashcard Deck
                    if (onBookmarkCard != null) {
                        DetailedActionButton(
                            icon = if (state.isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            label = if (state.isBookmarked) "محفوظ" else "حفظ للبطاقات",
                            tint = brandAccent,
                            isDark = isDark,
                            modifier = Modifier.weight(1.1f),
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                onBookmarkCard(result)
                            }
                        )
                    }

                    // 3. Copy Translation
                    val textToCopy = when (result) {
                        is TranslationResult.LexicalMatch -> "${result.sourceText} - ${result.targetArabicText}"
                        is TranslationResult.DecomposedCompoundMatch -> "${result.sourceText} - ${result.synthesizedArabicText}"
                        is TranslationResult.ContextualSentence -> "${result.sourceText} - ${result.targetArabicText}"
                        is TranslationResult.ContextualMatch -> "${result.sourceText} - ${result.targetArabicText}"
                        else -> result.sourceText
                    }
                    DetailedActionButton(
                        icon = Icons.Rounded.ContentCopy,
                        label = "نسخ",
                        tint = brandAccent,
                        isDark = isDark,
                        modifier = Modifier.weight(0.7f),
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCopyTranslation(textToCopy)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailedLexicalContent(
    match: TranslationResult.LexicalMatch,
    fg: Color,
    fgSecondary: Color,
    brandAccent: Color,
    isDark: Boolean,
    onSpeakTerm: ((text: String, isLatin: Boolean) -> Unit)? = null,
    currentlyPlayingAudioText: String? = null
) {
    val haptics = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // English Term & TTS Speaker
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = match.sourceText,
                color = fg,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (onSpeakTerm != null) {
                val isPlaying = currentlyPlayingAudioText?.equals(match.sourceText.trim(), ignoreCase = true) == true
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpeakTerm(match.sourceText, false)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Pronounce English Term",
                        tint = if (isPlaying) brandAccent else fgSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Primary Arabic Translation (bold, RTL, high-contrast)
        Text(
            text = match.targetArabicText,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.copy(
                textDirection = TextDirection.Rtl,
                textAlign = TextAlign.Right
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Latin Binomial Root (if present)
        if (!match.latinName.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Latin:",
                    color = fgSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = match.latinName,
                    color = fg,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )
                if (onSpeakTerm != null) {
                    val isLatinPlaying = currentlyPlayingAudioText?.equals(match.latinName.trim(), ignoreCase = true) == true
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Pronounce Latin root",
                        tint = if (isLatinPlaying) brandAccent else fgSecondary.copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable {
                                onSpeakTerm(match.latinName, true)
                            }
                    )
                }
            }
        }

        // English Clinical Definition
        if (!match.definitionEn.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = match.definitionEn,
                color = fg.copy(alpha = 0.88f),
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )
        }

        // Arabic Clinical Definition
        if (!match.definitionAr.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = match.definitionAr,
                color = fg.copy(alpha = 0.82f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                style = MaterialTheme.typography.bodySmall.copy(
                    textDirection = TextDirection.Rtl,
                    textAlign = TextAlign.Right
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Synonyms / Aliases
        if (match.synonyms.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Synonyms:",
                    color = fgSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = match.synonyms.joinToString(", "),
                    color = fgSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailedCompoundContent(
    match: TranslationResult.DecomposedCompoundMatch,
    fg: Color,
    fgSecondary: Color,
    brandAccent: Color,
    isDark: Boolean,
    onSpeakTerm: ((text: String, isLatin: Boolean) -> Unit)? = null,
    currentlyPlayingAudioText: String? = null
) {
    val haptics = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = match.sourceText,
                color = fg,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (onSpeakTerm != null) {
                val isPlaying = currentlyPlayingAudioText?.equals(match.sourceText.trim(), ignoreCase = true) == true
                IconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpeakTerm(match.sourceText, false)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Pronounce Compound",
                        tint = if (isPlaying) brandAccent else fgSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Primary Synthesized Arabic Translation (bold, RTL, high-contrast)
        Text(
            text = match.synthesizedArabicText,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium.copy(
                textDirection = TextDirection.Rtl,
                textAlign = TextAlign.Right
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))
        Text(
            text = "Morphological Breakdown:",
            color = fgSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            match.subTokens.forEach { token ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0x18FFFFFF) else Color(0x0C000000))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = token.tokenEn,
                        color = fg,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "→  ${token.translationAr}",
                        color = brandAccent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailedContextualContent(
    targetArabicText: String,
    entities: List<ExtractedMedicalEntity>,
    fg: Color,
    fgSecondary: Color,
    brandAccent: Color,
    isDark: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = targetArabicText,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 24.sp,
            style = MaterialTheme.typography.bodyMedium.copy(
                textDirection = TextDirection.Rtl,
                textAlign = TextAlign.Right
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (entities.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Key Clinical Entities:",
                color = fgSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                entities.forEach { entity ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0x18FFFFFF) else Color(0x0C000000))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = entity.englishTerm,
                            color = fg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (entity.arabicEquivalent.isNotBlank()) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "(${entity.arabicEquivalent})",
                                color = brandAccent,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedActionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0E000000))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = if (isDark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
