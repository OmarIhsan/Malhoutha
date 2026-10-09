package com.malhoutha.ui.home.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malhoutha.ui.components.GlassCard
import com.malhoutha.ui.home.RecentDocumentItem
import com.malhoutha.ui.theme.GlassLevel
import com.malhoutha.ui.theme.LocalMalhouthaColors
import com.malhoutha.ui.theme.LocalMalhouthaTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentLecturesSection(
    lectures: List<RecentDocumentItem>,
    onOpenDocument: (Uri) -> Unit,
    onTogglePin: (Uri) -> Unit,
    onRemoveRecent: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMalhouthaColors.current
    val typography = LocalMalhouthaTypography.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Lectures & Slides • المحاضرات الأخيرة",
                style = typography.titleSection,
                color = colors.textPrimary
            )
            Text(
                text = "${lectures.size} Files",
                style = typography.bodySmall,
                color = colors.textMuted
            )
        }

        if (lectures.isEmpty()) {
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
                        text = "No lecture files found / لم يتم العثور على ملفات",
                        style = typography.bodyMedium,
                        color = colors.textMuted
                    )
                    Text(
                        text = "Tap 'Open Document' below to import your first slide deck or textbook",
                        style = typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                lectures.forEach { item ->
                    LectureItemCard(
                        item = item,
                        onClick = { onOpenDocument(item.uri) },
                        onTogglePin = { onTogglePin(item.uri) },
                        onRemove = { onRemoveRecent(item.uri) }
                    )
                }
            }
        }
    }
}

@Composable
fun LectureItemCard(
    item: RecentDocumentItem,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMalhouthaColors.current
    val typography = LocalMalhouthaTypography.current
    var menuExpanded by remember { mutableStateOf(false) }

    val formatIcon = when (item.formatTag) {
        "SLIDES" -> Icons.Rounded.Slideshow
        "IMAGE" -> Icons.Rounded.Image
        "SHEET" -> Icons.Rounded.TableChart
        else -> Icons.Rounded.PictureAsPdf
    }

    val formatColor = when (item.formatTag) {
        "SLIDES" -> Color(0xFFFF7043)
        "IMAGE" -> Color(0xFF26A69A)
        "SHEET" -> Color(0xFF66BB6A)
        else -> Color(0xFF42A5F5)
    }

    GlassCard(
        level = GlassLevel.ELEVATED_CARD,
        shape = RoundedCornerShape(20.dp),
        borderWidth = if (item.pinned) 1.5.dp else null,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Format icon box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(formatColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = formatIcon,
                    contentDescription = item.formatTag,
                    tint = formatColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            // Main Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.title,
                        style = typography.titleCard,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.pinned) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = "Pinned",
                            tint = colors.clinicalTeal,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Format Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(formatColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.formatTag,
                            style = typography.labelBadge.copy(fontSize = 9.sp),
                            color = formatColor
                        )
                    }

                    Text(
                        text = "${item.pageCount} pages",
                        style = typography.bodySmall,
                        color = colors.textMuted
                    )

                    if (item.stickyNoteCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.EditNote,
                                contentDescription = null,
                                tint = colors.warningAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "${item.stickyNoteCount} notes",
                                style = typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = colors.warningAmber
                            )
                        }
                    }

                    Text(
                        text = "• ${formatTimestamp(item.lastAccessedTimestamp)}",
                        style = typography.bodySmall,
                        color = colors.textMuted
                    )
                }
            }

            // More Options Dropdown
            Box {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { menuExpanded = true }
                        .padding(4.dp)
                )

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(colors.surfaceCardElevated)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (item.pinned) "Unpin Document" else "Pin to Top",
                                color = colors.textPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.PushPin,
                                contentDescription = null,
                                tint = colors.clinicalTeal
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onTogglePin()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove from Recents", color = colors.errorCoral) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = colors.errorCoral
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRemove()
                        }
                    )
                }
            }
        }
    }
}
}

private fun formatTimestamp(timestampMs: Long): String {
    if (timestampMs <= 0L) return "Recently"
    val now = System.currentTimeMillis()
    val diff = now - timestampMs
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 7 * 86400_000 -> "${diff / 86400_000}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestampMs))
    }
}
