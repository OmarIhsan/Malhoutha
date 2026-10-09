package com.malhoutha.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malhoutha.ui.adaptive.AdaptiveLayoutConfig
import com.malhoutha.ui.components.GlassSurface
import com.malhoutha.ui.theme.GlassLevel

/**
 * Standard Navigation Destinations in Malhoutha study workstation.
 */
enum class MalhouthaNavDestination(
    val route: String,
    val titleEn: String,
    val titleAr: String,
    val icon: ImageVector
) {
    Lectures("lectures", "Lectures", "المحاضرات", Icons.Rounded.School),
    Subjects("subjects", "Subjects", "المواد", Icons.Rounded.Folder),
    Lexicon("lexicon", "Lexicon", "المعجم", Icons.AutoMirrored.Rounded.MenuBook),
    Decks("decks", "Decks", "البطاقات", Icons.Rounded.Style),
    Settings("settings", "Settings", "الإعدادات", Icons.Rounded.Settings)
}

data class NavigationBadge(
    val count: Int? = null,
    val hasUpdate: Boolean = false
)

/**
 * Adaptive Navigation Rail optimized for Tablet Landscape & Portrait viewports.
 * Pinned along the left edge, keeping the reading canvas wide and palm-resting friendly.
 */
@Composable
fun AdaptiveNavigationRail(
    currentDestination: MalhouthaNavDestination,
    onNavigate: (MalhouthaNavDestination) -> Unit,
    config: AdaptiveLayoutConfig,
    badges: Map<MalhouthaNavDestination, NavigationBadge> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    GlassSurface(
        level = GlassLevel.BASE_RAIL,
        shape = RoundedCornerShape(0.dp),
        modifier = modifier
            .fillMaxHeight()
            .width(88.dp)
    ) {
        NavigationRail(
            containerColor = Color.Transparent,
            header = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
                ) {
                    // Malhoutha Brand Monogram Pill
                    Box(
                        modifier = Modifier
                            .size(config.touchTargetMinSize)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF00695C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "م",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "ملحوظة",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            modifier = Modifier.fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary Destinations (Top / Center)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MalhouthaNavDestination.entries.filter { it != MalhouthaNavDestination.Settings }.forEach { destination ->
                        val selected = destination == currentDestination
                        val badge = badges[destination]

                        NavigationRailItem(
                            selected = selected,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigate(destination)
                            },
                            icon = {
                                Box {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.titleEn,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    if (badge?.count != null && badge.count > 0) {
                                        Badge(
                                            containerColor = Color(0xFFE53935),
                                            contentColor = Color.White,
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            Text(badge.count.toString(), fontSize = 9.sp)
                                        }
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = destination.titleEn,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color(0xFF00695C),
                                selectedTextColor = Color(0xFF00695C),
                                indicatorColor = Color(0xFF00695C).copy(alpha = 0.16f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Settings pinned at bottom
                NavigationRailItem(
                    selected = currentDestination == MalhouthaNavDestination.Settings,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigate(MalhouthaNavDestination.Settings)
                    },
                    icon = {
                        Icon(
                            imageVector = MalhouthaNavDestination.Settings.icon,
                            contentDescription = MalhouthaNavDestination.Settings.titleEn,
                            modifier = Modifier.size(26.dp)
                        )
                    },
                    label = {
                        Text(
                            text = MalhouthaNavDestination.Settings.titleEn,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.padding(bottom = 16.dp),
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = Color(0xFF00695C),
                        selectedTextColor = Color(0xFF00695C),
                        indicatorColor = Color(0xFF00695C).copy(alpha = 0.16f)
                    )
                )
            }
        }
    }
}

/**
 * Bottom Navigation Bar for Compact Phone Viewports.
 * Places primary actions directly beneath thumb resting zones.
 */
@Composable
fun AdaptiveBottomNavigationBar(
    currentDestination: MalhouthaNavDestination,
    onNavigate: (MalhouthaNavDestination) -> Unit,
    badges: Map<MalhouthaNavDestination, NavigationBadge> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current

    GlassSurface(
        level = GlassLevel.BASE_RAIL,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            MalhouthaNavDestination.entries.forEach { destination ->
                val selected = destination == currentDestination
                val badge = badges[destination]

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigate(destination)
                    },
                    icon = {
                        Box {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.titleEn,
                                modifier = Modifier.size(24.dp)
                            )
                            if (badge?.count != null && badge.count > 0) {
                                Badge(
                                    containerColor = Color(0xFFE53935),
                                    contentColor = Color.White,
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Text(badge.count.toString(), fontSize = 9.sp)
                                }
                            }
                        }
                    },
                    label = {
                        Text(
                            text = destination.titleEn,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00695C),
                        selectedTextColor = Color(0xFF00695C),
                        indicatorColor = Color(0xFF00695C).copy(alpha = 0.16f)
                    )
                )
            }
        }
    }
}
