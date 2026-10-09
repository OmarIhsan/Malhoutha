package com.chethan616.clearpdf.medical.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp

/**
 * Lightweight, stacked auto-translation quick pill (Step 3 Unified Brand Aesthetic).
 * Mounts directly adjacent to the ClearPDF selection action bar for instant, zero-click comprehension.
 *
 * Displays exclusively:
 * 1. The translated Arabic text (MaterialTheme.colorScheme.onSurface).
 * 2. Optional clinical subspecialty badge (ClinicalDomainBadge in unified Clinical Teal).
 * 3. Audio icon (MaterialTheme.colorScheme.primary).
 * 4. "Details" / "التفاصيل" trigger (MaterialTheme.colorScheme.primary).
 */
@Composable
fun QuickTranslationPill(
    arabicTranslation: String,
    isLoading: Boolean = false,
    clinicalDomain: String? = null,
    onPlayAudio: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(16.dp)

    Surface(
        shape = pillShape,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp).copy(alpha = 0.94f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        tonalElevation = 6.dp,
        modifier = modifier
            .clip(pillShape)
            .wrapContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedContent(
                targetState = isLoading,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "QuickPillContentTransition"
            ) { loading ->
                if (loading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Translating...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Primary Arabic Term (RTL, bold, high-contrast onSurface)
                        Text(
                            text = arabicTranslation,
                            style = MaterialTheme.typography.titleSmall.copy(
                                textDirection = TextDirection.Rtl,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // 2. Optional Clinical Subspecialty Badge (if clinical context exists)
                        if (!clinicalDomain.isNullOrBlank() &&
                            !clinicalDomain.equals("ACADEMIC", ignoreCase = true) &&
                            !clinicalDomain.equals("GENERAL", ignoreCase = true)
                        ) {
                            ClinicalDomainBadge(domainText = clinicalDomain)
                        }

                        // 3. Audio Pronunciation Button
                        IconButton(
                            onClick = onPlayAudio,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Pronounce",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(14.dp))

                        // 4. Quick "Details / التفاصيل" Expansion Action
                        Row(
                            modifier = Modifier
                                .clickable(onClick = onOpenDetails)
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Details",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = "Open full details",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Backward-compatible overload without clinicalDomain.
 */
@Composable
fun QuickTranslationPill(
    arabicTranslation: String,
    isLoading: Boolean = false,
    onPlayAudio: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    QuickTranslationPill(
        arabicTranslation = arabicTranslation,
        isLoading = isLoading,
        clinicalDomain = null,
        onPlayAudio = onPlayAudio,
        onOpenDetails = onOpenDetails,
        modifier = modifier
    )
}
