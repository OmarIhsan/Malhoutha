package com.chethan616.clearpdf.ui.paper

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chethan616.clearpdf.ui.components.GlassDialog
import com.chethan616.clearpdf.ui.components.GlassDialogAction
import com.chethan616.clearpdf.ui.theme.LiquidGlassColors
import com.chethan616.clearpdf.ui.theme.LocalIsDarkMode
import com.kyant.backdrop.Backdrop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StartNoteDialog(
    backdrop: Backdrop,
    onDismissRequest: () -> Unit,
    onCreateNote: (title: String, config: PaperConfig, pageCount: Int) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true
) {
    val context = LocalContext.current
    val isDark = LocalIsDarkMode.current
    val textPrimary = LiquidGlassColors.text(isDark)
    val textSecondary = LiquidGlassColors.secondary(isDark)
    val accent = LiquidGlassColors.Teal

    var title by remember {
        val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date())
        mutableStateOf("Note · $dateStr")
    }

    var selectedType by remember { mutableStateOf(PaperTemplateType.RULED_COLLEGE) }
    var selectedPreset by remember { mutableStateOf(PaperColorPreset.WHITE) }
    var pageCount by remember { mutableIntStateOf(3) }

    val templateOptions = remember {
        listOf(
            PaperTemplateType.PLAIN to "Plain",
            PaperTemplateType.RULED_COLLEGE to "College",
            PaperTemplateType.RULED_WIDE to "Wide",
            PaperTemplateType.GRID_5MM to "5mm Grid",
            PaperTemplateType.DOT_MATRIX to "Dot Matrix",
            PaperTemplateType.CORNELL to "Cornell"
        )
    }

    val pageCountOptions = remember { listOf(1, 3, 5, 10) }

    GlassDialog(
        visible = visible,
        onDismiss = onDismissRequest,
        backdrop = backdrop,
        title = "Start New Note",
        modifier = modifier,
        actions = {
            GlassDialogAction(
                text = "Cancel",
                onClick = onDismissRequest
            )
            GlassDialogAction(
                text = "Create Note",
                primary = true,
                tint = accent,
                onClick = {
                    val finalConfig = selectedPreset.applyTo(selectedType)
                    onCreateNote(title, finalConfig, pageCount)
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Document Title Input
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BasicText(
                    "TITLE",
                    style = TextStyle(
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xFF242730) else Color(0xFFEAECEF))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            cursorBrush = SolidColor(accent),
                            modifier = Modifier.weight(1f)
                        )
                        if (title.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear title",
                                tint = textSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { title = "" }
                            )
                        }
                    }
                }
            }

            // Paper Template Carousel
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(
                    "TEMPLATE",
                    style = TextStyle(
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    templateOptions.forEach { (type, label) ->
                        val isSelected = selectedType == type
                        val previewConfig = remember(type, selectedPreset) {
                            selectedPreset.applyTo(type)
                        }
                        val borderColor by animateColorAsState(
                            if (isSelected) accent else (if (isDark) Color(0xFF333742) else Color(0xFFD2D5DC)),
                            label = "templateBorder"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .width(70.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedType = type }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 66.dp, height = 86.dp)
                                    .shadow(if (isSelected) 6.dp else 2.dp, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(10.dp))
                            ) {
                                Canvas(Modifier.matchParentSize()) {
                                    drawPaperPreview(previewConfig)
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(accent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                            BasicText(
                                label,
                                style = TextStyle(
                                    color = if (isSelected) accent else textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }

            // Paper Substrate Color Swatches
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(
                    "PAPER SUBSTRATE",
                    style = TextStyle(
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PaperColorPreset.values().forEach { preset ->
                        val isSelected = selectedPreset == preset
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedPreset = preset }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .shadow(if (isSelected) 4.dp else 1.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(preset.baseColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) accent else (if (isDark) Color(0xFF444956) else Color(0xFFC7CBD6)),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(accent)
                                    )
                                }
                            }
                            BasicText(
                                preset.displayName,
                                style = TextStyle(
                                    color = if (isSelected) accent else textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }

            // Page Count Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(
                    "PAGES",
                    style = TextStyle(
                        color = textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pageCountOptions.forEach { count ->
                        val isSelected = pageCount == count
                        val pillBg by animateColorAsState(
                            if (isSelected) accent.copy(alpha = 0.18f) else (if (isDark) Color(0xFF242730) else Color(0xFFECEFF3)),
                            label = "pagePillBg"
                        )
                        val pillBorder by animateColorAsState(
                            if (isSelected) accent else Color.Transparent,
                            label = "pagePillBorder"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(pillBg)
                                .border(1.dp, pillBorder, RoundedCornerShape(12.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { pageCount = count }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                "$count ${if (count == 1) "Page" else "Pages"}",
                                style = TextStyle(
                                    color = if (isSelected) accent else textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StartNoteDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    backdrop: Backdrop,
    onConfirm: (title: String, config: PaperConfig, pageCount: Int) -> Unit
) = StartNoteDialog(
    backdrop = backdrop,
    onDismissRequest = onDismiss,
    onCreateNote = onConfirm,
    visible = visible
)
