package com.chethan616.clearpdf.medical.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chethan616.clearpdf.medical.export.AnkiPackageExporter
import com.chethan616.clearpdf.medical.repository.VocabularyDeckRepository
import com.chethan616.clearpdf.medical.viewmodel.UnresolvedQueryViewModel
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chethan616.clearpdf.medical.data.SavedVocabularyCardEntity
import com.chethan616.clearpdf.medical.domain.TranslationResult
import com.chethan616.clearpdf.medical.domain.TranslationTier
import com.chethan616.clearpdf.medical.model.MedicalDomain
import com.chethan616.clearpdf.medical.viewmodel.MedicalLexiconSearchViewModel
import com.chethan616.clearpdf.ui.components.viewerChromeGlass
import com.chethan616.clearpdf.ui.components.viewerGlass
import com.chethan616.clearpdf.ui.theme.LocalIsDarkMode
import com.kyant.backdrop.Backdrop

import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.navigationBarsPadding

/**
 * Tablet-optimized dual-pane reference panel & slide-over medical lexicon search drawer.
 * Supports instant offline dictionary queries, clinical category filtering, and one-tap
 * note injection directly into the active PDF page.
 */
@Composable
fun MedicalLexiconDrawer(
    viewModel: MedicalLexiconSearchViewModel,
    backdrop: Backdrop,
    onClose: () -> Unit,
    onDropToPage: (TranslationResult.LexicalMatch) -> Unit,
    onBookmarkTerm: (TranslationResult.LexicalMatch) -> Unit,
    onSpeakTerm: (text: String, isLatin: Boolean) -> Unit,
    currentlyPlayingAudioText: String? = null,
    isDualPane: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isOpen by viewModel.isOpen.collectAsState()
    val isDark = LocalIsDarkMode.current

    AnimatedVisibility(
        visible = isOpen,
        enter = if (isDualPane) {
            expandHorizontally(animationSpec = tween(250), expandFrom = Alignment.End) + fadeIn(tween(200))
        } else {
            slideInHorizontally(animationSpec = tween(250), initialOffsetX = { it }) + fadeIn(tween(200))
        },
        exit = if (isDualPane) {
            shrinkHorizontally(animationSpec = tween(200), shrinkTowards = Alignment.End) + fadeOut(tween(150))
        } else {
            slideOutHorizontally(animationSpec = tween(200), targetOffsetX = { it }) + fadeOut(tween(150))
        },
        modifier = modifier
    ) {
        val glassTint = viewerChromeGlass(isDark)
        val fg = if (isDark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E)
        val fgSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF636366)
        val panelBorder = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)
        val context = LocalContext.current
        var showUnresolvedSheet by remember { mutableStateOf(false) }

        if (showUnresolvedSheet) {
            val unresolvedVm: UnresolvedQueryViewModel = viewModel(
                factory = UnresolvedQueryViewModel.Factory(context)
            )
            UnresolvedTermsSheet(
                viewModel = unresolvedVm,
                backdrop = backdrop,
                onClose = { showUnresolvedSheet = false },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .viewerGlass(backdrop, glassTint)
                    .border(width = 1.dp, color = panelBorder)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // ── Drawer Header ──
                LexiconDrawerHeader(
                    onClose = onClose,
                    onOpenUnresolved = { showUnresolvedSheet = true },
                    fg = fg,
                    fgSecondary = fgSecondary,
                    isDark = isDark
                )

                Spacer(Modifier.height(12.dp))

                // ── Search Input Box ──
                LexiconSearchBox(
                    viewModel = viewModel,
                    fg = fg,
                    fgSecondary = fgSecondary,
                    isDark = isDark
                )

                Spacer(Modifier.height(10.dp))

                // ── Category Filter Chips ──
                LexiconDomainChips(
                    viewModel = viewModel,
                    isDark = isDark
                )

                Spacer(Modifier.height(12.dp))

                // ── Main Content: Search Results, Recent Lookups, or Quick Browse ──
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LexiconDrawerBody(
                        viewModel = viewModel,
                        fg = fg,
                        fgSecondary = fgSecondary,
                        isDark = isDark,
                        onDropToPage = onDropToPage,
                        onBookmarkTerm = onBookmarkTerm,
                        onSpeakTerm = onSpeakTerm,
                        currentlyPlayingAudioText = currentlyPlayingAudioText
                    )
                }
            }
        }
    }
}

@Composable
private fun LexiconDrawerHeader(
    onClose: () -> Unit,
    onOpenUnresolved: () -> Unit,
    fg: Color,
    fgSecondary: Color,
    isDark: Boolean
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0A84FF).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                    contentDescription = null,
                    tint = Color(0xFF0A84FF),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "Medical Lexicon",
                    color = fg,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Offline UMD & Dental Reference",
                    color = fgSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9500).copy(alpha = 0.16f))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        onOpenUnresolved()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoFixHigh,
                    contentDescription = "Vocabulary Gaps & Telemetry",
                    tint = Color(0xFFFF9500),
                    modifier = Modifier.size(15.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x20FFFFFF) else Color(0x10000000))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        onClose()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close Reference Panel",
                    tint = fgSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun LexiconSearchBox(
    viewModel: MedicalLexiconSearchViewModel,
    fg: Color,
    fgSecondary: Color,
    isDark: Boolean
) {
    val query by viewModel.searchQuery.collectAsState()
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0x22FFFFFF) else Color(0x0C000000))
            .border(1.dp, if (isDark) Color(0x28FFFFFF) else Color(0x15000000), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = fgSecondary,
            modifier = Modifier.size(18.dp)
        )

        Spacer(Modifier.width(8.dp))

        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = "Search terms, roots, anatomy...",
                    color = fgSecondary.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
            BasicTextField(
                value = query,
                onValueChange = { viewModel.setQuery(it) },
                singleLine = true,
                textStyle = TextStyle(
                    color = fg,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(Color(0xFF0A84FF)),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x30FFFFFF) else Color(0x20000000))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        viewModel.clearQuery()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear",
                    tint = fg,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@Composable
private fun LexiconDomainChips(
    viewModel: MedicalLexiconSearchViewModel,
    isDark: Boolean
) {
    val selectedDomain by viewModel.selectedDomain.collectAsState()
    val domains = listOf(
        null to "All",
        MedicalDomain.ANATOMY to "Anatomy",
        MedicalDomain.PATHOLOGY to "Pathology",
        MedicalDomain.PHARMACOLOGY to "Pharmacology",
        MedicalDomain.PROCEDURE to "Procedure",
        MedicalDomain.DIAGNOSTIC to "Diagnostic",
        MedicalDomain.GENERAL_CLINICAL to "General"
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(domains) { (domain, label) ->
            val isSelected = selectedDomain == domain
            val chipColor = if (domain != null) resolveDomainColor(domain) else Color(0xFF00897B)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) chipColor.copy(alpha = 0.25f)
                        else if (isDark) Color(0x18FFFFFF) else Color(0x0C000000)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) chipColor else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { viewModel.selectDomain(domain) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = label,
                    color = if (isSelected) chipColor else if (isDark) Color(0xFFCCCCCC) else Color(0xFF555555),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LexiconDrawerBody(
    viewModel: MedicalLexiconSearchViewModel,
    fg: Color,
    fgSecondary: Color,
    isDark: Boolean,
    onDropToPage: (TranslationResult.LexicalMatch) -> Unit,
    onBookmarkTerm: (TranslationResult.LexicalMatch) -> Unit,
    onSpeakTerm: (text: String, isLatin: Boolean) -> Unit,
    currentlyPlayingAudioText: String?
) {
    val query by viewModel.searchQuery.collectAsState()
    val selectedDomain by viewModel.selectedDomain.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val recentCards by viewModel.recentLookups.collectAsState()

    val isSearchActive = query.isNotBlank() || selectedDomain != null

    if (isSearchActive) {
        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF00897B),
                    strokeWidth = 2.dp
                )
            }
        } else if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No matching terms found",
                        color = fg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Try alternative spellings or switch to 'All' category.",
                        color = fgSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(
                    items = searchResults,
                    key = { "${it.matchedTerm ?: it.sourceText}_${it.domain}" }
                ) { match ->
                    LexiconDrawerResultCard(
                        match = match,
                        fg = fg,
                        fgSecondary = fgSecondary,
                        isDark = isDark,
                        onDropToPage = onDropToPage,
                        onBookmarkTerm = onBookmarkTerm,
                        onSpeakTerm = onSpeakTerm,
                        currentlyPlayingAudioText = currentlyPlayingAudioText
                    )
                }
            }
        }
    } else {
        // Idle State: Quick Browse & Recent Lookups
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                Text(
                    text = "Quick Topics",
                    color = fgSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(6.dp))
                QuickTopicsRow(
                    onTopicClick = { viewModel.setQuery(it) },
                    isDark = isDark
                )
            }

            if (recentCards.isNotEmpty()) {
                item {
                    val context = LocalContext.current
                    val coroutineScope = rememberCoroutineScope()
                    var isExporting by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = null,
                                tint = fgSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Saved & Recent Terms",
                                color = fgSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Anki Export Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color(0x33007AFF) else Color(0x1F007AFF))
                                .clickable(enabled = !isExporting) {
                                    isExporting = true
                                    coroutineScope.launch {
                                        try {
                                            val deckRepo = VocabularyDeckRepository.getInstance(context)
                                            val allCards = deckRepo.getAllCards()
                                            if (allCards.isNotEmpty()) {
                                                val apkgFile = AnkiPackageExporter.exportDeckToApkg(
                                                    context = context,
                                                    deckName = "Malhoutha_Medical_Lexicon",
                                                    cards = allCards
                                                )
                                                val shareIntent = AnkiPackageExporter.createShareIntent(context, apkgFile)
                                                context.startActivity(Intent.createChooser(shareIntent, "Export Anki Deck (.apkg)"))
                                            } else {
                                                Toast.makeText(context, "No saved cards to export", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isExporting = false
                                        }
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Export to Anki",
                                    tint = if (isDark) Color(0xFF64B5F6) else Color(0xFF007AFF),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = if (isExporting) "Exporting..." else "Anki (.apkg)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color(0xFF64B5F6) else Color(0xFF007AFF)
                                )
                            }
                        }
                    }
                }

                items(
                    items = recentCards.take(15),
                    key = { it.id }
                ) { card ->
                    SavedCardDrawerItem(
                        card = card,
                        fg = fg,
                        fgSecondary = fgSecondary,
                        isDark = isDark,
                        onDropToPage = {
                            val match = mapSavedCardToMatch(card)
                            onDropToPage(match)
                        },
                        onSpeakTerm = onSpeakTerm,
                        currentlyPlayingAudioText = currentlyPlayingAudioText
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0x15FFFFFF) else Color(0x08000000))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = "Clinical Knowledge Hub",
                                color = fg,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Search over 35,000 WHO UMD dental concepts, anatomical Latin roots, and clinical definitions without internet.",
                                color = fgSecondary,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickTopicsRow(
    onTopicClick: (String) -> Unit,
    isDark: Boolean
) {
    val topics = listOf(
        "Enamel", "Dentin", "Pulpitis", "Periodontitis", "Gingiva",
        "Amelogenesis", "Cementum", "Crown", "Root canal", "Caries"
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        topics.forEach { topic ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isDark) Color(0x20FFFFFF) else Color(0x0F000000))
                    .clickable { onTopicClick(topic) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = topic,
                    color = if (isDark) Color(0xFFDDDDDD) else Color(0xFF444444),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LexiconDrawerResultCard(
    match: TranslationResult.LexicalMatch,
    fg: Color,
    fgSecondary: Color,
    isDark: Boolean,
    onDropToPage: (TranslationResult.LexicalMatch) -> Unit,
    onBookmarkTerm: (TranslationResult.LexicalMatch) -> Unit,
    onSpeakTerm: (text: String, isLatin: Boolean) -> Unit,
    currentlyPlayingAudioText: String?
) {
    val haptics = LocalHapticFeedback.current
    val domainColor = resolveDomainColor(match.domain)
    val termEn = match.matchedTerm ?: match.sourceText

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDark) Color(0x1CFFFFFF) else Color(0x0E000000))
            .border(1.dp, if (isDark) Color(0x24FFFFFF) else Color(0x14000000), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // English Term & Domain
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = termEn,
                color = fg,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false)
            )

            MedicalDomainBadge(result = match, isDark = isDark)
        }

        Spacer(Modifier.height(4.dp))

        // Arabic Primary Translation
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = match.targetArabicText,
                color = fg,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                style = TextStyle(
                    textDirection = TextDirection.Rtl,
                    lineHeight = 26.sp,
                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Latin Anatomical Root
        if (!match.latinName.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Latin: ${match.latinName}",
                color = fgSecondary,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic
            )
        }

        // English Definition
        if (!match.definitionEn.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = match.definitionEn,
                color = fgSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        // Bottom Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Action 1: Pronounce English
            val isPlayingEn = currentlyPlayingAudioText?.equals(termEn.trim(), ignoreCase = true) == true
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isPlayingEn) domainColor.copy(alpha = 0.25f) else if (isDark) Color(0x18FFFFFF) else Color(0x10000000))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpeakTerm(termEn, false)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "Speak",
                    tint = if (isPlayingEn) domainColor else fgSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Action 2: Bookmark
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0x18FFFFFF) else Color(0x10000000))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        onBookmarkTerm(match)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(15.dp)
                )
            }

            // Action 3: "Drop to Page" (Add as Note)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(domainColor.copy(alpha = 0.15f))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDropToPage(match)
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.NoteAdd,
                    contentDescription = null,
                    tint = domainColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Drop to Page",
                    color = domainColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SavedCardDrawerItem(
    card: SavedVocabularyCardEntity,
    fg: Color,
    fgSecondary: Color,
    isDark: Boolean,
    onDropToPage: () -> Unit,
    onSpeakTerm: (text: String, isLatin: Boolean) -> Unit,
    currentlyPlayingAudioText: String?
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDark) Color(0x16FFFFFF) else Color(0x0A000000))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.sourceTermEn,
                color = fg,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = card.targetTermAr,
                    color = fg,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val isPlaying = currentlyPlayingAudioText?.equals(card.sourceTermEn.trim(), ignoreCase = true) == true
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) Color(0xFF00897B).copy(alpha = 0.25f) else if (isDark) Color(0x18FFFFFF) else Color(0x10000000))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSpeakTerm(card.sourceTermEn, false)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "Speak",
                    tint = if (isPlaying) Color(0xFF00897B) else fgSecondary,
                    modifier = Modifier.size(13.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00897B).copy(alpha = 0.15f))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDropToPage()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.NoteAdd,
                    contentDescription = "Drop to Page",
                    tint = Color(0xFF00897B),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

private fun resolveDomainColor(domain: MedicalDomain): Color = Color(0xFF00897B)

private fun mapSavedCardToMatch(card: SavedVocabularyCardEntity): TranslationResult.LexicalMatch {
    val domain = try {
        MedicalDomain.valueOf(card.domain)
    } catch (_: Exception) {
        MedicalDomain.GENERAL_CLINICAL
    }
    return TranslationResult.LexicalMatch(
        sourceText = card.sourceTermEn,
        targetArabicText = card.targetTermAr,
        domain = domain,
        latinName = card.latinRoot,
        subspecialty = card.subspecialty,
        definitionEn = card.definitionEn,
        definitionAr = card.definitionAr,
        sourceLexicon = "SAVED_DECK",
        tierUsed = TranslationTier.TIER_1_LEXICON,
        matchedTerm = card.sourceTermEn
    )
}
