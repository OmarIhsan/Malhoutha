package com.malhoutha.ui.gutter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chethan616.clearpdf.ui.components.LiquidButton
import com.chethan616.clearpdf.ui.components.LiquidIconButton
import com.chethan616.clearpdf.ui.components.viewerGlass
import com.chethan616.clearpdf.ui.theme.LiquidGlassColors
import com.chethan616.clearpdf.ui.theme.LocalIsDarkMode
import com.kyant.backdrop.backdrops.LayerBackdrop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GutterNoteItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val timestamp: String,
    val colorHex: Long = 0xFFFFF9C4L,
    val pinned: Boolean = false
)

object GutterPastelColors {
    val Yellow = 0xFFFFF9C4L
    val Green = 0xFFC8E6C9L
    val Blue = 0xFFBBDEFBL
    val Peach = 0xFFFFE0B2L
    val Purple = 0xFFE1BEE7L
    val Rose = 0xFFF8BBD0L

    val all = listOf(Yellow, Green, Blue, Peach, Purple, Rose)
}

@Composable
fun GutterNotePanel(
    backdrop: LayerBackdrop,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkMode.current
    val fg = if (isDark) Color(0xFFF0F0F5) else Color(0xFF1C1D22)
    val sub = if (isDark) Color(0xFF9E9EA7) else Color(0xFF6B6E77)
    val panelGlass = if (isDark) Color(0xFF181B22).copy(alpha = 0.88f) else Color(0xFFFFFFFF).copy(alpha = 0.82f)

    var draftText by rememberSaveable { mutableStateOf("") }
    var selectedColor by rememberSaveable { mutableLongStateOf(GutterPastelColors.Yellow) }
    val notes = remember {
        mutableStateListOf(
            GutterNoteItem(
                text = "Theorem 3.2: Verify boundary condition at x=0 for exam prep.",
                timestamp = "Lecture Note · 10:15",
                colorHex = GutterPastelColors.Yellow,
                pinned = true
            ),
            GutterNoteItem(
                text = "Key definition: Thevenin resistance R_th = V_oc / I_sc.",
                timestamp = "Lecture Note · 10:28",
                colorHex = GutterPastelColors.Blue,
                pinned = false
            )
        )
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .viewerGlass(backdrop, panelGlass, shape = { RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp) })
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Header Row ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.EditNote,
                    contentDescription = null,
                    tint = LiquidGlassColors.Blue,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    BasicText(
                        "Margin Gutter",
                        style = TextStyle(color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )
                    BasicText(
                        "${notes.size} synchronized notes",
                        style = TextStyle(color = sub, fontSize = 11.sp)
                    )
                }
            }

            LiquidIconButton(
                onClick = onClose,
                backdrop = backdrop,
                tint = LiquidGlassColors.Red,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Rounded.Close, "Close Gutter", Modifier.size(16.dp), Color.White)
            }
        }

        // ── Fast Note Composer ──────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color(0xFF242831) else Color(0xFFF1F3F9))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BasicTextField(
                value = draftText,
                onValueChange = { draftText = it },
                textStyle = TextStyle(color = fg, fontSize = 13.sp),
                cursorBrush = SolidColor(LiquidGlassColors.Blue),
                decorationBox = { innerTextField ->
                    if (draftText.isEmpty()) {
                        BasicText("Type margin note or scratchpad equation...", style = TextStyle(color = sub.copy(0.7f), fontSize = 13.sp))
                    }
                    innerTextField()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = 100.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color swatches
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GutterPastelColors.all.take(4).forEach { colorHex ->
                        val isSelected = selectedColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 22.dp else 18.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .clickable { selectedColor = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF333333)))
                            }
                        }
                    }
                }

                // Add button
                LiquidButton(
                    onClick = {
                        if (draftText.isNotBlank()) {
                            notes.add(
                                0,
                                GutterNoteItem(
                                    text = draftText.trim(),
                                    timestamp = "Note · ${timeFormatter.format(Date())}",
                                    colorHex = selectedColor
                                )
                            )
                            draftText = ""
                        }
                    },
                    backdrop = backdrop,
                    tint = LiquidGlassColors.Blue
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Add, null, Modifier.size(14.dp), Color.White)
                        BasicText("Add", style = TextStyle(Color.White, 12.sp, FontWeight.SemiBold))
                    }
                }
            }
        }

        // ── Notes Stream ────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(notes, key = { _, item -> item.id }) { index, item ->
                GutterStickyCard(
                    item = item,
                    onDelete = { notes.removeAt(index) },
                    onTogglePin = {
                        notes[index] = item.copy(pinned = !item.pinned)
                    }
                )
            }
        }
    }
}

@Composable
private fun GutterStickyCard(
    item: GutterNoteItem,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit
) {
    val cardColor = Color(item.colorHex)
    val textDark = Color(0xFF1E2124)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardColor)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.pinned) {
                    Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(13.dp),
                        tint = textDark.copy(0.75f)
                    )
                }
                BasicText(
                    item.timestamp,
                    style = TextStyle(color = textDark.copy(0.65f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    Icons.Rounded.PushPin,
                    contentDescription = "Pin Note",
                    tint = if (item.pinned) Color(0xFFD32F2F) else textDark.copy(0.35f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onTogglePin)
                )
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete Note",
                    tint = textDark.copy(0.45f),
                    modifier = Modifier
                        .size(16.dp)
                        .clickable(onClick = onDelete)
                )
            }
        }

        BasicText(
            item.text,
            style = TextStyle(color = textDark, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
        )
    }
}
