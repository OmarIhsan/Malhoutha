package com.chethan616.clearpdf.medical.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chethan616.clearpdf.medical.data.UnresolvedQueryEntity
import com.chethan616.clearpdf.medical.viewmodel.UnresolvedQueryViewModel
import com.chethan616.clearpdf.ui.components.viewerGlass
import com.chethan616.clearpdf.ui.theme.LocalIsDarkMode
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Slide-over / Modal diagnostic sheet providing visibility into unresolved query telemetry,
 * frequency analysis of study gaps, and the in-app self-healing lexicon patch bridge.
 */
@Composable
fun UnresolvedTermsSheet(
    viewModel: UnresolvedQueryViewModel,
    backdrop: Backdrop,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = LocalIsDarkMode.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.dismissToast()
        }
    }

    val glassTint = if (isDark) Color(0xFF14171F).copy(alpha = 0.94f) else Color(0xFFF7F9FC).copy(alpha = 0.96f)
    val fg = if (isDark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E)
    val fgSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF636366)
    val panelBorder = if (isDark) Color(0x33FFFFFF) else Color(0x1F000000)

    Box(
        modifier = modifier
            .fillMaxSize()
            .viewerGlass(backdrop, glassTint)
            .border(width = 1.dp, color = panelBorder)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header Bar ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9500).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoFixHigh,
                            contentDescription = null,
                            tint = Color(0xFFFF9500),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Vocabulary Gaps",
                                color = fg,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (uiState.totalMissCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFF9500).copy(alpha = 0.18f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${uiState.totalMissCount}",
                                        color = Color(0xFFFF9500),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Autonomous edge logging for missing terms",
                            color = fgSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Export TSV
                    if (uiState.items.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x20FFFFFF) else Color(0x10000000))
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                    coroutineScope.launch {
                                        val tsv = viewModel.exportAsTsv()
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Vocabulary Gaps TSV", tsv))
                                        Toast.makeText(context, "Export copied to clipboard", Toast.LENGTH_SHORT).show()
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, tsv)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Export Vocabulary Gaps"))
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Export TSV",
                                tint = fgSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Clear All
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x20FFFFFF) else Color(0x10000000))
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                    showClearConfirm = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Clear All Logs",
                                tint = fgSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
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
                            contentDescription = "Close",
                            tint = fgSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Search & Filter Input ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color(0x1FFFFFFF) else Color(0x0C000000))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = fgSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = fg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(Color(0xFFFF9500)),
                        decorationBox = { innerTextField ->
                            if (uiState.searchQuery.isEmpty()) {
                                Text(
                                    text = "Filter missed queries...",
                                    color = fgSecondary,
                                    fontSize = 13.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (uiState.searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = fgSecondary,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.setSearchQuery("") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ── Main List or Empty State ──
            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759).copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TaskAlt,
                                contentDescription = null,
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = if (uiState.searchQuery.isBlank()) "All Clinical Lookups Resolved" else "No matching queries found",
                            color = fg,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (uiState.searchQuery.isBlank())
                                "Offline lexicon is handling your lecture slide queries cleanly."
                            else
                                "Try adjusting your filter keyword.",
                            color = fgSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        UnresolvedQueryCard(
                            item = item,
                            fg = fg,
                            fgSecondary = fgSecondary,
                            isDark = isDark,
                            onPatch = {
                                haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                viewModel.setPendingPatchItem(item)
                            },
                            onDelete = {
                                haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                                viewModel.deleteMissedTerm(item.normalizedQuery)
                            }
                        )
                    }
                }
            }
        }

        // ── In-Sheet Patch Dialog ──
        uiState.pendingPatchItem?.let { itemToPatch ->
            PatchTermDialog(
                item = itemToPatch,
                isDark = isDark,
                onDismiss = { viewModel.setPendingPatchItem(null) },
                onConfirm = { translationAr, category, definition, addToDeck ->
                    viewModel.patchTermToLexicon(
                        item = itemToPatch,
                        arabicTranslation = translationAr,
                        partOfSpeechOrDomain = category,
                        definition = definition,
                        addToDeck = addToDeck
                    )
                }
            )
        }

        // ── Clear All Confirmation Dialog ──
        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text(text = "Clear All Unresolved Queries?", color = fg) },
                text = {
                    Text(
                        text = "This will delete all ${uiState.totalMissCount} logged missing terms telemetry. This action cannot be undone.",
                        color = fgSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showClearConfirm = false
                        viewModel.clearAllMissedTerms()
                    }) {
                        Text(text = "Clear All", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text(text = "Cancel", color = fgSecondary)
                    }
                },
                containerColor = if (isDark) Color(0xFF1C1E26) else Color.White
            )
        }
    }
}

@Composable
private fun UnresolvedQueryCard(
    item: UnresolvedQueryEntity,
    fg: Color,
    fgSecondary: Color,
    isDark: Boolean,
    onPatch: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = if (isDark) Color(0x14FFFFFF) else Color(0x08000000)
    val cardBorder = if (isDark) Color(0x22FFFFFF) else Color(0x12000000)
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }
    val isFrequent = item.hitCount >= 3

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(width = 1.dp, color = cardBorder, shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Top Row: Term + Hit Counter Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.normalizedQuery,
                        color = fg,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.originalSelection.isNotBlank() && !item.originalSelection.equals(item.normalizedQuery, ignoreCase = true)) {
                        Text(
                            text = "(\"${item.originalSelection}\")",
                            color = fgSecondary,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Hit badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isFrequent) Color(0xFFFF9500).copy(alpha = 0.20f)
                            else if (isDark) Color(0x28FFFFFF)
                            else Color(0x14000000)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        if (isFrequent) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFF9500),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = "${item.hitCount}x",
                            color = if (isFrequent) Color(0xFFFF9500) else fgSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Context Reference Metadata
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!item.documentName.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = fgSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${item.documentName} • p. ${item.pageIndex + 1}",
                            color = fgSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = fgSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = dateFormat.format(Date(item.lastSeenEpochMs)),
                        color = fgSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Patch Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0A84FF).copy(alpha = 0.16f))
                        .clickable { onPatch() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoFixHigh,
                            contentDescription = null,
                            tint = Color(0xFF0A84FF),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Patch into Lexicon",
                            color = Color(0xFF0A84FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Delete Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Dismiss",
                        tint = fgSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PatchTermDialog(
    item: UnresolvedQueryEntity,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (translationAr: String, category: String?, definition: String?, addToDeck: Boolean) -> Unit
) {
    var arabicTranslation by remember { mutableStateOf("") }
    var definition by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Dental Vocabulary") }
    var addToDeck by remember { mutableStateOf(true) }

    val fg = if (isDark) Color(0xFFF2F2F7) else Color(0xFF1C1C1E)
    val fgSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF636366)
    val containerBg = if (isDark) Color(0xFF1E212B) else Color(0xFFFFFFFF)
    val inputBg = if (isDark) Color(0x1AFFFFFF) else Color(0x0A000000)

    val categories = listOf(
        "Dental Vocabulary",
        "Endodontics",
        "Periodontics",
        "Prosthodontics",
        "Oral Surgery",
        "Orthodontics",
        "Oral Pathology",
        "General Academic"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Patch '${item.normalizedQuery}'",
                    color = fg,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Instantly resolve offline across PDF viewports",
                    color = fgSecondary,
                    fontSize = 11.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Arabic Translation Input (RTL)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Arabic Translation (الترجمة العربية):",
                        color = fg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(inputBg)
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            BasicTextField(
                                value = arabicTranslation,
                                onValueChange = { arabicTranslation = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = fg,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    textDirection = TextDirection.Rtl,
                                    textAlign = TextAlign.Right
                                ),
                                cursorBrush = SolidColor(Color(0xFF0A84FF)),
                                decorationBox = { inner ->
                                    if (arabicTranslation.isEmpty()) {
                                        Text(
                                            text = "أدخل المصطلح بالعربية...",
                                            color = fgSecondary,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                    }
                }

                // Category Chips
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Department / Category:",
                        color = fg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val selected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (selected) Color(0xFF0A84FF)
                                        else if (isDark) Color(0x1EFFFFFF)
                                        else Color(0x0F000000)
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (selected) Color.White else fg,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Short English Definition
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Short Definition (Optional):",
                        color = fg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(inputBg)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = definition,
                            onValueChange = { definition = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = fg,
                                fontSize = 13.sp
                            ),
                            cursorBrush = SolidColor(Color(0xFF0A84FF)),
                            decorationBox = { inner ->
                                if (definition.isEmpty()) {
                                    Text(
                                        text = "Brief clinical context...",
                                        color = fgSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                                inner()
                            }
                        )
                    }
                }

                // Add to Custom Deck Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { addToDeck = !addToDeck },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = addToDeck,
                        onCheckedChange = { addToDeck = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF0A84FF),
                            uncheckedColor = fgSecondary
                        )
                    )
                    Text(
                        text = "Save to 'My Dental Lexicon' flashcard deck",
                        color = fg,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (arabicTranslation.isNotBlank()) {
                        onConfirm(
                            arabicTranslation.trim(),
                            selectedCategory,
                            definition.takeIf { it.isNotBlank() },
                            addToDeck
                        )
                    }
                },
                enabled = arabicTranslation.isNotBlank()
            ) {
                Text(
                    text = "Save & Patch",
                    color = if (arabicTranslation.isNotBlank()) Color(0xFF0A84FF) else fgSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = fgSecondary)
            }
        },
        containerColor = containerBg
    )
}
