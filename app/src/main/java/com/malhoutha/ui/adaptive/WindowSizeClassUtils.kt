package com.malhoutha.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard Material 3 Window Width Size Class representations.
 */
enum class WindowWidthSizeClass {
    Compact,   // < 600dp (standard handheld phone portrait)
    Medium,    // 600dp - 839dp (tablet portrait, unfolded foldable, multi-window split)
    Expanded   // >= 840dp (tablet landscape, desktop mode, workstation display)
}

/**
 * Standard Material 3 Window Height Size Class representations.
 */
enum class WindowHeightSizeClass {
    Compact,   // < 480dp (phone landscape, compact height)
    Medium,    // 480dp - 899dp (phone portrait, standard tablet)
    Expanded   // >= 900dp (large tablet portrait, desktop display)
}

/**
 * Represents window size categorization adhering strictly to Material 3 responsive specifications.
 */
data class WindowSizeClass(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass
) {
    companion object {
        fun calculateFromSize(widthDp: Float, heightDp: Float): WindowSizeClass {
            val width = when {
                widthDp < 600f -> WindowWidthSizeClass.Compact
                widthDp < 840f -> WindowWidthSizeClass.Medium
                else -> WindowWidthSizeClass.Expanded
            }
            val height = when {
                heightDp < 480f -> WindowHeightSizeClass.Compact
                heightDp < 900f -> WindowHeightSizeClass.Medium
                else -> WindowHeightSizeClass.Expanded
            }
            return WindowSizeClass(width, height)
        }
    }
}

/**
 * Calculates current [WindowSizeClass] reactively from the active viewport configuration.
 */
@Composable
fun calculateWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp, configuration.screenHeightDp) {
        WindowSizeClass.calculateFromSize(
            configuration.screenWidthDp.toFloat(),
            configuration.screenHeightDp.toFloat()
        )
    }
}

/**
 * Adaptive Device Posture classification for Malhoutha large-screen & foldables UX.
 */
enum class DevicePosture {
    PHONE_PORTRAIT,
    PHONE_LANDSCAPE,
    TABLET_PORTRAIT,
    TABLET_LANDSCAPE,
    DESKTOP_EXPANDED;

    val isTablet: Boolean
        get() = this == TABLET_PORTRAIT || this == TABLET_LANDSCAPE || this == DESKTOP_EXPANDED

    val isLandscape: Boolean
        get() = this == TABLET_LANDSCAPE || this == PHONE_LANDSCAPE || this == DESKTOP_EXPANDED
}

/**
 * Ergonomic Handedness setting for stylus writing:
 * - RIGHT_HANDED_WRITER: Writer rests palm on the right; inking tool dock placed on Left Margin.
 * - LEFT_HANDED_WRITER: Writer rests palm on the left; inking tool dock placed on Right Margin.
 */
enum class Handedness {
    RIGHT_HANDED_WRITER,
    LEFT_HANDED_WRITER;

    fun toggle(): Handedness =
        if (this == RIGHT_HANDED_WRITER) LEFT_HANDED_WRITER else RIGHT_HANDED_WRITER
}

/**
 * Specification tokens for adaptive layout scaling, touch target boundaries,
 * and dual-pane workspace allocations.
 */
data class AdaptiveLayoutConfig(
    val posture: DevicePosture,
    val showPermanentNavRail: Boolean,
    val isDualPaneEnabled: Boolean,
    val companionPanelWidth: Dp,
    val touchTargetMinSize: Dp,
    val toolbarHeight: Dp,
    val navigationBarHeight: Dp = 80.dp,
    val handedness: Handedness = Handedness.RIGHT_HANDED_WRITER
)

val LocalAdaptiveLayoutConfig = staticCompositionLocalOf {
    AdaptiveLayoutConfig(
        posture = DevicePosture.PHONE_PORTRAIT,
        showPermanentNavRail = false,
        isDualPaneEnabled = false,
        companionPanelWidth = 0.dp,
        touchTargetMinSize = 48.dp,
        toolbarHeight = 60.dp
    )
}

/**
 * Remembers an [AdaptiveLayoutConfig] based on the [WindowSizeClass].
 */
@Composable
fun rememberAdaptiveLayoutConfig(
    windowSizeClass: WindowSizeClass,
    handedness: Handedness = Handedness.RIGHT_HANDED_WRITER
): AdaptiveLayoutConfig {
    val widthClass = windowSizeClass.widthSizeClass
    val heightClass = windowSizeClass.heightSizeClass

    return remember(widthClass, heightClass, handedness) {
        when {
            widthClass == WindowWidthSizeClass.Expanded && heightClass != WindowHeightSizeClass.Compact -> {
                AdaptiveLayoutConfig(
                    posture = DevicePosture.TABLET_LANDSCAPE,
                    showPermanentNavRail = true,
                    isDualPaneEnabled = true,
                    companionPanelWidth = 360.dp,
                    touchTargetMinSize = 56.dp, // Enlarged stylus targets
                    toolbarHeight = 72.dp,
                    handedness = handedness
                )
            }
            widthClass == WindowWidthSizeClass.Expanded && heightClass == WindowHeightSizeClass.Compact -> {
                AdaptiveLayoutConfig(
                    posture = DevicePosture.PHONE_LANDSCAPE,
                    showPermanentNavRail = true,
                    isDualPaneEnabled = false,
                    companionPanelWidth = 300.dp,
                    touchTargetMinSize = 48.dp,
                    toolbarHeight = 56.dp,
                    handedness = handedness
                )
            }
            widthClass == WindowWidthSizeClass.Medium -> {
                AdaptiveLayoutConfig(
                    posture = DevicePosture.TABLET_PORTRAIT,
                    showPermanentNavRail = true,
                    isDualPaneEnabled = false, // Overlay sheets on portrait tablets
                    companionPanelWidth = 320.dp,
                    touchTargetMinSize = 52.dp,
                    toolbarHeight = 68.dp,
                    handedness = handedness
                )
            }
            else -> {
                AdaptiveLayoutConfig(
                    posture = DevicePosture.PHONE_PORTRAIT,
                    showPermanentNavRail = false,
                    isDualPaneEnabled = false,
                    companionPanelWidth = 0.dp,
                    touchTargetMinSize = 48.dp, // Compact mobile touch targets
                    toolbarHeight = 60.dp,
                    handedness = handedness
                )
            }
        }
    }
}

/**
 * Overload of [rememberAdaptiveLayoutConfig] that resolves [WindowSizeClass] automatically.
 */
@Composable
fun rememberAdaptiveLayoutConfig(
    handedness: Handedness = Handedness.RIGHT_HANDED_WRITER
): AdaptiveLayoutConfig {
    val windowSizeClass = calculateWindowSizeClass()
    return rememberAdaptiveLayoutConfig(windowSizeClass, handedness)
}
