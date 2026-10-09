package com.malhoutha.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malhoutha.ui.components.GlassCard
import com.malhoutha.ui.theme.GlassLevel
import com.malhoutha.ui.theme.LocalMalhouthaColors
import com.malhoutha.ui.theme.LocalMalhouthaTypography

@Composable
fun HomeHeaderSection(
    totalAnnotations: Int,
    bookmarkedCardsCount: Int,
    totalDocuments: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMalhouthaColors.current
    val typography = LocalMalhouthaTypography.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Brand Wordmark & Offline Indicator ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Malhoutha",
                        style = typography.titleHero,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "|",
                        style = typography.titleHero.copy(fontWeight = FontWeight.Light),
                        color = colors.clinicalTeal.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ملحوظة",
                        style = typography.titleHero.copy(fontWeight = FontWeight.Bold),
                        color = colors.clinicalTeal
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Medical & Dental Academic Workstation • محطة الاستذكار السريري",
                    style = typography.bodySmall,
                    color = colors.textMuted
                )
            }

            // Offline Verified Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceCard)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.successMint)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "100% Offline",
                    style = typography.labelBadge,
                    color = colors.textSecondary
                )
            }
        }

        // ── Quick Telemetry Stats Row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HeaderStatChip(
                icon = Icons.Rounded.Description,
                value = "$totalDocuments",
                label = "Lectures",
                accentColor = colors.clinicalTeal,
                modifier = Modifier.weight(1f)
            )
            HeaderStatChip(
                icon = Icons.Rounded.EditNote,
                value = "$totalAnnotations",
                label = "Notes Dropped",
                accentColor = colors.warningAmber,
                modifier = Modifier.weight(1f)
            )
            HeaderStatChip(
                icon = Icons.Rounded.Bookmark,
                value = "$bookmarkedCardsCount",
                label = "Flashcards",
                accentColor = colors.royalIndigo,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Unified Search Field ──
        GlassCard(
            level = GlassLevel.BASE_RAIL,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search lectures, summaries, or slides... / ابحث في المراجع والمحاضرات",
                            style = typography.bodyMedium,
                            color = colors.textMuted
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        textStyle = typography.bodyMedium.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.clinicalTeal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = colors.textSecondary,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable { onSearchQueryChange("") }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderStatChip(
    icon: ImageVector,
    value: String,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalMalhouthaColors.current
    val typography = LocalMalhouthaTypography.current

    GlassCard(
        level = GlassLevel.BASE_RAIL,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    style = typography.titleCard.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    color = colors.textPrimary
                )
                Text(
                    text = label,
                    style = typography.bodySmall.copy(fontSize = 10.sp),
                    color = colors.textMuted
                )
            }
        }
    }
}
