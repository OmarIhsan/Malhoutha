package com.malhoutha.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Depth hierarchy levels for the unified Malhoutha Frosted Glassmorphism design system.
 */
enum class GlassLevel {
    BASE_RAIL,      // Bottom navigation bars, permanent side rails
    ELEVATED_CARD,  // Lecture preview cards, folder items, deck snippets
    FLOATING_DOCK,  // Stylus inking dock, floating capsules, page jumper
    MODAL_TOOLTIP   // Medical translation tooltip, dialog sheets, popovers
}

/**
 * Resolved visual tokens for a frosted glass surface.
 */
@Immutable
data class GlassStyleTokens(
    val blurRadius: Dp,
    val containerColor: Color,
    val borderColor: Color,
    val borderBrush: Brush? = null,
    val borderWidth: Dp = 1.dp,
    val elevation: Dp = 0.dp,
    val ambientShadowColor: Color = Color.Transparent
)

/**
 * Global Glass Tokens calibrated for clinical contrast, readability, and hardware acceleration.
 */
object GlassTokens {

    /**
     * Whether the current runtime supports hardware-accelerated RenderEffect blurs
     * without dropping frames during 120fps stylus inking (Android 12+ / API 31+).
     */
    val supportsHardwareBlur: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // ── Dark Mode Tokens (Deep obsidian glass with specular white rim highlights) ──
    object Dark {
        val baseRail = GlassStyleTokens(
            blurRadius = 12.dp,
            containerColor = Color(0xCC12161F), // ~80% opacity dark obsidian
            borderColor = Color(0x1FFFFFFF),
            borderBrush = Brush.verticalGradient(
                listOf(Color(0x33FFFFFF), Color(0x14FFFFFF))
            ),
            elevation = 2.dp
        )

        val elevatedCard = GlassStyleTokens(
            blurRadius = 16.dp,
            containerColor = Color(0xD6171B24), // ~84% opacity
            borderColor = Color(0x2EFFFFFF),
            borderBrush = Brush.verticalGradient(
                listOf(Color(0x3DFFFFFF), Color(0x18FFFFFF))
            ),
            elevation = 6.dp,
            ambientShadowColor = Color(0x40000000)
        )

        val floatingDock = GlassStyleTokens(
            blurRadius = 20.dp,
            containerColor = Color(0xE0141822), // ~88% opacity
            borderColor = Color(0x38FFFFFF),
            borderBrush = Brush.verticalGradient(
                listOf(Color(0x47FFFFFF), Color(0x1FFFFFFF))
            ),
            elevation = 10.dp,
            ambientShadowColor = Color(0x66000000)
        )

        val modalTooltip = GlassStyleTokens(
            blurRadius = 24.dp,
            containerColor = Color(0xEB11151E), // ~92% opacity
            borderColor = Color(0x4D00897B),   // Luminous clinical teal rim
            borderBrush = Brush.verticalGradient(
                listOf(Color(0x8000897B), Color(0x2600897B))
            ),
            elevation = 16.dp,
            ambientShadowColor = Color(0x80000000)
        )
    }

    // ── Light Mode Tokens (Frosted crystal white glass with subtle shadow borders) ──
    object Light {
        val baseRail = GlassStyleTokens(
            blurRadius = 12.dp,
            containerColor = Color(0xD9F8FAFC), // ~85% opacity
            borderColor = Color(0x14000000),
            borderBrush = Brush.verticalGradient(
                listOf(Color(0x80FFFFFF), Color(0x14000000))
            ),
            elevation = 2.dp
        )

        val elevatedCard = GlassStyleTokens(
            blurRadius = 16.dp,
            containerColor = Color(0xEEFFFFFF), // ~93% opacity
            borderColor = Color(0x1F000000),
            borderBrush = Brush.verticalGradient(
                listOf(Color(0xE6FFFFFF), Color(0x1F000000))
            ),
            elevation = 4.dp,
            ambientShadowColor = Color(0x1A000000)
        )

        val floatingDock = GlassStyleTokens(
            blurRadius = 20.dp,
            containerColor = Color(0xF2F6F8FA), // ~95% opacity
            borderColor = Color(0x24000000),
            borderBrush = Brush.verticalGradient(
                listOf(Color(0xFFFFFFFF), Color(0x24000000))
            ),
            elevation = 8.dp,
            ambientShadowColor = Color(0x24000000)
        )

        val modalTooltip = GlassStyleTokens(
            blurRadius = 24.dp,
            containerColor = Color(0xF8FFFFFF), // ~97% opacity
            borderColor = Color(0x4D00897B),
            borderBrush = Brush.verticalGradient(
                listOf(Color(0x6600897B), Color(0x2600897B))
            ),
            elevation = 14.dp,
            ambientShadowColor = Color(0x2E000000)
        )
    }

    /**
     * Resolves the token set for a given [level] and dark/light configuration.
     */
    fun resolve(level: GlassLevel, isDark: Boolean): GlassStyleTokens {
        return if (isDark) {
            when (level) {
                GlassLevel.BASE_RAIL -> Dark.baseRail
                GlassLevel.ELEVATED_CARD -> Dark.elevatedCard
                GlassLevel.FLOATING_DOCK -> Dark.floatingDock
                GlassLevel.MODAL_TOOLTIP -> Dark.modalTooltip
            }
        } else {
            when (level) {
                GlassLevel.BASE_RAIL -> Light.baseRail
                GlassLevel.ELEVATED_CARD -> Light.elevatedCard
                GlassLevel.FLOATING_DOCK -> Light.floatingDock
                GlassLevel.MODAL_TOOLTIP -> Light.modalTooltip
            }
        }
    }
}
