package com.chethan616.clearpdf.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.malhoutha.ui.adaptive.AdaptiveLayoutConfig
import com.malhoutha.ui.adaptive.Handedness
import com.malhoutha.ui.adaptive.rememberAdaptiveLayoutConfig

/**
 * Companion panel tabs for side-by-side study workflows.
 */
enum class ReaderCompanionTab(val titleEn: String, val titleAr: String) {
    LEXICON("Lexicon", "المعجم"),
    DECKS("Vocabulary Decks", "البطاقات"),
    NOTES("Sticky Notes", "الملاحظات"),
    OUTLINE("Outline", "الفهرس")
}

/**
 * Adaptive Dual-Pane Reader Scaffold for Tablet Ergonomics & Zero Content Overlap.
 *
 * Coordinates:
 * 1. Dedicated, unoccluded PDF reading & inking canvas ([canvasContent]).
 * 2. Handedness-aware stylus dock placement ([toolDockContent]).
 * 3. Anchored companion pane for Medical Lexicon, Decks, and Notes ([companionContent]).
 */
@Composable
fun AdaptiveReaderScaffold(
    isCompanionOpen: Boolean,
    onCloseCompanion: () -> Unit,
    modifier: Modifier = Modifier,
    config: AdaptiveLayoutConfig = rememberAdaptiveLayoutConfig(),
    handedness: Handedness = config.handedness,
    isDockDetached: Boolean = false,
    toolDockContent: @Composable () -> Unit = {},
    topBarContent: @Composable () -> Unit = {},
    bottomBarContent: @Composable () -> Unit = {},
    companionContent: @Composable () -> Unit = {},
    canvasContent: @Composable (canvasModifier: Modifier) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (config.isDualPaneEnabled) {
            // ── Tablet Landscape True Dual-Pane Row ──
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Dock for Right-Handed Writers (Keeps palm away from tools)
                if (isDockDetached && handedness == Handedness.RIGHT_HANDED_WRITER) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(start = 12.dp, top = config.toolbarHeight + 8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        toolDockContent()
                    }
                }

                // Center Primary Canvas (Dedicated weight so companion never occludes PDF)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    canvasContent(Modifier.fillMaxSize())
                }

                // Anchored Right Companion Panel (Medical Lexicon / Decks)
                AnimatedVisibility(
                    visible = isCompanionOpen,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .width(config.companionPanelWidth)
                            .fillMaxHeight(),
                        tonalElevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        companionContent()
                    }
                }

                // Right Dock for Left-Handed Writers
                if (isDockDetached && handedness == Handedness.LEFT_HANDED_WRITER) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 12.dp, top = config.toolbarHeight + 8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        toolDockContent()
                    }
                }
            }
        } else {
            // ── Compact Phone / Portrait Single-Pane Layout ──
            Box(modifier = Modifier.fillMaxSize()) {
                canvasContent(Modifier.fillMaxSize())

                // Overlay sheet / drawer on compact devices
                AnimatedVisibility(
                    visible = isCompanionOpen,
                    enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                    exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Surface(
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight(),
                        tonalElevation = 8.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        companionContent()
                    }
                }
            }
        }

        // Overlay Chrome (Top Command Bar)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
        ) {
            topBarContent()
        }

        // Bottom Bar (Phone / Compact Viewports)
        if (!config.isDualPaneEnabled || !isDockDetached) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxSize()
            ) {
                bottomBarContent()
            }
        }
    }
}
