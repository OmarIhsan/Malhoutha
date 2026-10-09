package com.malhoutha.ui.home.components

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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chethan616.clearpdf.medical.data.SavedVocabularyCardEntity
import com.malhoutha.ui.components.GlassCard
import com.malhoutha.ui.theme.GlassLevel
import com.malhoutha.ui.theme.LocalMalhouthaColors
import com.malhoutha.ui.theme.LocalMalhouthaTypography

@Composable
fun VocabularySnippetCard(
    card: SavedVocabularyCardEntity?,
    onSpeak: (text: String, isLatin: Boolean) -> Unit,
    onOpenDecks: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (card == null) return

    val colors = LocalMalhouthaColors.current
    val typography = LocalMalhouthaTypography.current

    GlassCard(
        level = GlassLevel.ELEVATED_CARD,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenDecks() }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            // Header Row: Domain Chip + High-Yield Daily Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.clinicalTeal.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = colors.clinicalTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "DAILY CLINICAL RECALL • استذكار سريري",
                        style = typography.labelBadge,
                        color = colors.clinicalTeal
                    )
                }

                Text(
                    text = card.domain,
                    style = typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = colors.royalIndigo
                )
            }

            // Term Presentation (English + Arabic + Audio Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.sourceTermEn,
                        style = typography.titleSection.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary
                    )
                    if (!card.latinRoot.isNullOrBlank()) {
                        Text(
                            text = card.latinRoot,
                            style = typography.termLatin,
                            color = colors.textSecondary
                        )
                    }
                }

                // Audio Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceCardElevated)
                        .clickable { onSpeak(card.sourceTermEn, false) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Speak Term",
                        tint = colors.clinicalTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Arabic Translation Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceCardElevated.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.targetTermAr,
                    style = typography.termArabic.copy(fontSize = 15.sp),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Clinical Definition
            val def = card.definitionEn ?: card.definitionAr
            if (!def.isNullOrBlank()) {
                Text(
                    text = def,
                    style = typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 3
                )
            }

            // Bottom CTA Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Review in Flashcard Decks",
                    style = typography.bodySmall.copy(color = colors.clinicalTeal, fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = colors.clinicalTeal,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
