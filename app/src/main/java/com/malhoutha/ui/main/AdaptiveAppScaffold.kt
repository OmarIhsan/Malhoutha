package com.malhoutha.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malhoutha.ui.adaptive.AdaptiveLayoutConfig
import com.malhoutha.ui.adaptive.LocalAdaptiveLayoutConfig
import com.malhoutha.ui.adaptive.rememberAdaptiveLayoutConfig
import com.malhoutha.ui.navigation.AdaptiveBottomNavigationBar
import com.malhoutha.ui.navigation.AdaptiveNavigationRail
import com.malhoutha.ui.navigation.MalhouthaNavDestination
import com.malhoutha.ui.navigation.NavigationBadge

/**
 * Root Adaptive Shell for Malhoutha study workstation.
 *
 * Implements "Tablet-First, Mobile-Friendly" responsive architecture:
 * - On Expanded/Medium screens (Tablets & Landscape Foldables):
 *   Deploys an ergonomic left [AdaptiveNavigationRail], leaving the entire display unobstructed
 *   for multi-pane study workflows and wide slide reading.
 * - On Compact screens (Phones):
 *   Gracefully collapses into a bottom [AdaptiveBottomNavigationBar] optimized for one-handed thumb interaction.
 */
@Composable
fun AdaptiveAppScaffold(
    currentDestination: MalhouthaNavDestination,
    onNavigate: (MalhouthaNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    badges: Map<MalhouthaNavDestination, NavigationBadge> = emptyMap(),
    config: AdaptiveLayoutConfig = rememberAdaptiveLayoutConfig(),
    topBar: @Composable (() -> Unit)? = null,
    content: @Composable (AdaptiveLayoutConfig) -> Unit
) {
    CompositionLocalProvider(LocalAdaptiveLayoutConfig provides config) {
        if (config.showPermanentNavRail) {
            // ── Tablet / Large Screen Layout ──
            Row(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AdaptiveNavigationRail(
                    currentDestination = currentDestination,
                    onNavigate = onNavigate,
                    config = config,
                    badges = badges
                )

                VerticalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    content(config)
                }
            }
        } else {
            // ── Mobile / Compact Phone Layout ──
            Scaffold(
                topBar = { topBar?.invoke() },
                bottomBar = {
                    AdaptiveBottomNavigationBar(
                        currentDestination = currentDestination,
                        onNavigate = onNavigate,
                        badges = badges
                    )
                },
                modifier = modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    content(config)
                }
            }
        }
    }
}
