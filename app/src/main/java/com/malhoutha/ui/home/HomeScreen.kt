package com.malhoutha.ui.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chethan616.clearpdf.medical.audio.MedicalPronunciationEngine
import com.malhoutha.ui.home.components.HomeHeaderSection
import com.malhoutha.ui.home.components.LectureItemCard
import com.malhoutha.ui.home.components.QuickActionDock
import com.malhoutha.ui.home.components.RecentLecturesSection
import com.malhoutha.ui.home.components.SubjectFolderGrid
import com.malhoutha.ui.home.components.VocabularySnippetCard
import com.malhoutha.ui.theme.LocalMalhouthaColors
import com.malhoutha.ui.theme.LocalMalhouthaTypography
import com.malhoutha.ui.theme.MalhouthaTheme

private val SUPPORTED_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "image/*",
    "text/*"
)

/**
 * Malhoutha Production-Grade Homepage Dashboard.
 *
 * Automatically adapts between single-column fluid mobile layout and
 * master-detail multi-pane tablet workstation (e.g. Xiaomi Pad 6).
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onOpenDocument: (Uri) -> Unit,
    onOpenLexiconDrawer: () -> Unit,
    onOpenDecks: () -> Unit,
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val pronunciationEngine = remember { MedicalPronunciationEngine(context) }

    DisposableEffect(Unit) {
        onDispose {
            pronunciationEngine.shutdown()
        }
    }

    // System SAF File Picker Launcher
    val openDocLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onOpenDocument(uri)
        }
    }

    // Photo/Snapshot Capture Launcher
    val snapLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onOpenDocument(uri)
        }
    }

    MalhouthaTheme {
        val colors = LocalMalhouthaColors.current
        val typography = LocalMalhouthaTypography.current

        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            val isTabletLandscape = maxWidth > 840.dp

            if (isTabletLandscape) {
                // ── Tablet Landscape Master-Detail Architecture (>840dp) ──
                Row(modifier = Modifier.fillMaxSize()) {
                    // 1. Left Lateral Navigation Rail
                    MalhouthaNavRail(
                        onOpenPicker = { openDocLauncher.launch(SUPPORTED_MIME_TYPES) },
                        onSnapSlide = { snapLauncher.launch("image/*") },
                        onOpenLexicon = onOpenLexiconDrawer,
                        onOpenDecks = onOpenDecks,
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(88.dp)
                    )

                    // 2. Central Document & Subject Workstation
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 24.dp)
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(22.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                        ) {
                            item {
                                HomeHeaderSection(
                                    totalAnnotations = state.totalAnnotationsCount,
                                    bookmarkedCardsCount = state.bookmarkedCardsCount,
                                    totalDocuments = state.recentLectures.size,
                                    searchQuery = state.searchQuery,
                                    onSearchQueryChange = { viewModel.updateSearchQuery(it) }
                                )
                            }

                            item {
                                SubjectFolderGrid(
                                    categories = state.subjectCategories,
                                    selectedSubjectId = state.selectedSubjectId,
                                    onSelectSubject = { viewModel.selectSubject(it) }
                                )
                            }

                            item {
                                Text(
                                    text = "Recent Lectures & Slides • المحاضرات الأخيرة",
                                    style = typography.titleSection,
                                    color = colors.textPrimary
                                )
                            }

                            if (state.filteredLectures.isEmpty()) {
                                item {
                                    EmptyLecturesPlaceholder()
                                }
                            } else {
                                items(state.filteredLectures.size) { index ->
                                    val item = state.filteredLectures[index]
                                    LectureItemCard(
                                        item = item,
                                        onClick = { onOpenDocument(item.uri) },
                                        onTogglePin = { viewModel.togglePin(item.uri) },
                                        onRemove = { viewModel.removeRecent(item.uri) }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Right Clinical Companion Sidebar (~320dp)
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                            .background(colors.surface)
                            .border(1.dp, colors.border)
                            .padding(20.dp)
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "Clinical Digest • ملخص سريري",
                            style = typography.titleSection,
                            color = colors.textPrimary
                        )

                        // Daily Clinical Vocabulary Card
                        VocabularySnippetCard(
                            card = state.dailyVocabularyWord,
                            onSpeak = { text, isLatin -> pronunciationEngine.speakTerm(text, isLatin) },
                            onOpenDecks = onOpenDecks
                        )

                        // Clinical Memory & Stats Card
                        ClinicalStatsCard(
                            notesCount = state.totalAnnotationsCount,
                            cardsCount = state.bookmarkedCardsCount
                        )

                        Spacer(Modifier.weight(1f))

                        // Quick Import Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.clinicalTeal.copy(alpha = 0.12f))
                                .border(1.dp, colors.clinicalTeal.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .clickable { openDocLauncher.launch(SUPPORTED_MIME_TYPES) }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.FileOpen,
                                    contentDescription = null,
                                    tint = colors.clinicalTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Import Lecture / إضافة محاضرة",
                                    style = typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colors.clinicalTeal
                                )
                            }
                        }
                    }
                }
            } else {
                // ── Phone & Portrait Tablet Layout (<840dp) ──
                Scaffold(
                    containerColor = colors.background,
                    bottomBar = {
                        QuickActionDock(
                            onOpenDocument = { openDocLauncher.launch(SUPPORTED_MIME_TYPES) },
                            onCaptureSlide = { snapLauncher.launch("image/*") },
                            onOpenLexiconDrawer = onOpenLexiconDrawer,
                            onOpenDecks = onOpenDecks
                        )
                    }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp)
                    ) {
                        item {
                            HomeHeaderSection(
                                totalAnnotations = state.totalAnnotationsCount,
                                bookmarkedCardsCount = state.bookmarkedCardsCount,
                                totalDocuments = state.recentLectures.size,
                                searchQuery = state.searchQuery,
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) }
                            )
                        }

                        item {
                            VocabularySnippetCard(
                                card = state.dailyVocabularyWord,
                                onSpeak = { text, isLatin -> pronunciationEngine.speakTerm(text, isLatin) },
                                onOpenDecks = onOpenDecks
                            )
                        }

                        item {
                            SubjectFolderGrid(
                                categories = state.subjectCategories,
                                selectedSubjectId = state.selectedSubjectId,
                                onSelectSubject = { viewModel.selectSubject(it) }
                            )
                        }

                        item {
                            RecentLecturesSection(
                                lectures = state.filteredLectures,
                                onOpenDocument = onOpenDocument,
                                onTogglePin = { viewModel.togglePin(it) },
                                onRemoveRecent = { viewModel.removeRecent(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MalhouthaNavRail(
    onOpenPicker: () -> Unit,
    onSnapSlide: () -> Unit,
    onOpenLexicon: () -> Unit,
    onOpenDecks: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMalhouthaColors.current
    val typography = LocalMalhouthaTypography.current

    Column(
        modifier = modifier
            .background(colors.surface)
            .border(1.dp, colors.border)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Logo / Symbol
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.clinicalTeal),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalHospital,
                contentDescription = "Malhoutha",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        // Center Nav Items
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NavRailItem(
                icon = Icons.Rounded.FileOpen,
                label = "Import",
                onClick = onOpenPicker
            )
            NavRailItem(
                icon = Icons.Rounded.CameraAlt,
                label = "Snap",
                onClick = onSnapSlide
            )
            NavRailItem(
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                label = "Lexicon",
                onClick = onOpenLexicon
            )
            NavRailItem(
                icon = Icons.Rounded.School,
                label = "Decks",
                onClick = onOpenDecks
            )
        }

        // Bottom Settings
        NavRailItem(
            icon = Icons.Rounded.Settings,
            label = "Settings",
            onClick = onOpenSettings
        )
    }
}

@Composable
private fun NavRailItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = LocalMalhouthaColors.current
    val typography = LocalMalhouthaTypography.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = typography.bodySmall.copy(fontSize = 10.sp),
            color = colors.textMuted
        )
    }
}

@Composable
private fun ClinicalStatsCard(
    notesCount: Int,
    cardsCount: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalMalhouthaColors.current
    val typography = LocalMalhouthaTypography.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceCard)
            .border(1.dp, colors.border, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Academic Progress • التقدم الدراسي",
                style = typography.titleCard.copy(fontSize = 14.sp),
                color = colors.textPrimary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Clinical Sticky Notes",
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
                Text(
                    text = "$notesCount",
                    style = typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.warningAmber
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spaced Repetition Cards",
                    style = typography.bodySmall,
                    color = colors.textSecondary
                )
                Text(
                    text = "$cardsCount",
                    style = typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = colors.royalIndigo
                )
            }
        }
    }
}

@Composable
private fun EmptyLecturesPlaceholder() {
    val colors = LocalMalhouthaColors.current
    val typography = LocalMalhouthaTypography.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceCard)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Description,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = "No lecture files found / لا توجد محاضرات مطابقة",
                style = typography.bodyMedium,
                color = colors.textMuted
            )
        }
    }
}
