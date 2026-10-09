package com.malhoutha.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malhoutha.ui.components.GlassCard
import com.malhoutha.ui.home.SubjectCategory
import com.malhoutha.ui.theme.GlassLevel
import com.malhoutha.ui.theme.LocalMalhouthaColors
import com.malhoutha.ui.theme.LocalMalhouthaTypography

@Composable
fun SubjectFolderGrid(
    categories: List<SubjectCategory>,
    selectedSubjectId: String?,
    onSelectSubject: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMalhouthaColors.current
    val typography = LocalMalhouthaTypography.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Clinical Subjects & Specialties • الأقسام التخصصية",
                style = typography.titleSection,
                color = colors.textPrimary
            )
            if (selectedSubjectId != null) {
                Text(
                    text = "Clear Filter",
                    style = typography.bodySmall.copy(color = colors.clinicalTeal, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.clickable { onSelectSubject(null) }
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories, key = { it.id }) { cat ->
                val isSelected = cat.id == selectedSubjectId
                val targetBorder = if (isSelected) cat.colorAccent else colors.border
                val animatedBorder by animateColorAsState(targetValue = targetBorder, label = "subjectBorder")
                val animatedBg by animateColorAsState(
                    targetValue = if (isSelected) cat.colorAccent.copy(alpha = 0.16f) else colors.surfaceCard,
                    label = "subjectBg"
                )

                GlassCard(
                    level = GlassLevel.ELEVATED_CARD,
                    shape = RoundedCornerShape(18.dp),
                    borderWidth = if (isSelected) 1.5.dp else null,
                    modifier = Modifier
                        .width(160.dp)
                        .clickable { onSelectSubject(cat.id) }
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cat.colorAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = cat.titleEn,
                                    tint = cat.colorAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Doc Count Badge
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.surfaceCardElevated)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${cat.documentCount}",
                                    style = typography.labelBadge,
                                    color = if (cat.documentCount > 0) cat.colorAccent else colors.textMuted
                                )
                            }
                        }

                        Column {
                            Text(
                                text = cat.titleEn,
                                style = typography.titleCard.copy(fontSize = 14.sp),
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = cat.titleAr,
                                style = typography.bodySmall.copy(fontSize = 11.sp),
                                color = colors.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
