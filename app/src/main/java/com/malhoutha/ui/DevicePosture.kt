package com.malhoutha.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Device Posture and Window Size Class classification for Malhoutha large-screen & adaptive tablet UX.
 *
 * - TabletLandscape: >= 840dp width in landscape orientation (primary university lecture studio layout)
 * - TabletPortrait: >= 600dp width in portrait orientation (textbook & syllabus reading mode)
 * - PhoneLandscape: < 600dp height or < 840dp width in landscape (split-screen multitasking or phone landscape)
 * - PhonePortrait: < 600dp width in portrait (compact mobile phone or narrow split-screen view)
 */
enum class DevicePosture {
    TabletLandscape,
    TabletPortrait,
    PhoneLandscape,
    PhonePortrait;

    val isTablet: Boolean
        get() = this == TabletLandscape || this == TabletPortrait

    val isLandscape: Boolean
        get() = this == TabletLandscape || this == PhoneLandscape

    val isCompact: Boolean
        get() = !isTablet

    /**
     * Whether the active layout warrants an ergonomic vertical Lateral Spine Dock
     * to avoid resting palm touch interference during stylus inking.
     */
    val useLateralDock: Boolean
        get() = isTablet

    /**
     * Whether the screen width supports an optional 28% margin note gutter lane
     * alongside the continuous PDF lecture slide feed.
     */
    val supportsSideGutter: Boolean
        get() = this == TabletLandscape
}

val LocalDevicePosture = staticCompositionLocalOf<DevicePosture> { DevicePosture.PhonePortrait }

fun calculateDevicePosture(widthDp: Float, heightDp: Float): DevicePosture {
    val isLandscape = widthDp >= heightDp
    return if (isLandscape) {
        if (widthDp >= 840f && heightDp >= 480f) {
            DevicePosture.TabletLandscape
        } else {
            DevicePosture.PhoneLandscape
        }
    } else {
        if (widthDp >= 600f) {
            DevicePosture.TabletPortrait
        } else {
            DevicePosture.PhonePortrait
        }
    }
}

@Composable
fun rememberDevicePosture(): DevicePosture {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp, configuration.screenHeightDp, configuration.orientation) {
        calculateDevicePosture(configuration.screenWidthDp.toFloat(), configuration.screenHeightDp.toFloat())
    }
}
