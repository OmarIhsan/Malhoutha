package com.malhoutha.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Malhoutha Semantic Color Tokens tailored for dental and medical study workstations.
 */
@Immutable
data class MalhouthaColors(
    val background: Color,
    val surface: Color,
    val surfaceCard: Color,
    val surfaceCardElevated: Color,
    val border: Color,
    val borderHighlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val clinicalTeal: Color,
    val royalIndigo: Color,
    val accentPurple: Color,
    val warningAmber: Color,
    val successMint: Color,
    val errorCoral: Color,
    val glassFill: Color,
    val glassBorder: Color,
    val cardGlowBrush: Brush
)

val DarkMalhouthaColors = MalhouthaColors(
    background = Color(0xFF0F1115),
    surface = Color(0xFF151820),
    surfaceCard = Color(0xFF1B202A),
    surfaceCardElevated = Color(0xFF222834),
    border = Color(0x1AFFFFFF),
    borderHighlight = Color(0x334DB6AC),
    textPrimary = Color(0xFFF5F6F8),
    textSecondary = Color(0xFFA2A7B5),
    textMuted = Color(0xFF6B7280),
    clinicalTeal = Color(0xFF26A69A),
    royalIndigo = Color(0xFF5C6BC0),
    accentPurple = Color(0xFF9575CD),
    warningAmber = Color(0xFFFFB74D),
    successMint = Color(0xFF81C784),
    errorCoral = Color(0xFFE57373),
    glassFill = Color(0xB81A202C),
    glassBorder = Color(0x2EFFFFFF),
    cardGlowBrush = Brush.horizontalGradient(
        listOf(Color(0x3300897B), Color(0x223949AB), Color(0x00000000))
    )
)

val LightMalhouthaColors = MalhouthaColors(
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceCard = Color(0xFFF0F3F7),
    surfaceCardElevated = Color(0xFFE6EAF0),
    border = Color(0x14000000),
    borderHighlight = Color(0x3300796B),
    textPrimary = Color(0xFF191D24),
    textSecondary = Color(0xFF5C6270),
    textMuted = Color(0xFF8C93A3),
    clinicalTeal = Color(0xFF00897B),
    royalIndigo = Color(0xFF3949AB),
    accentPurple = Color(0xFF7E57C2),
    warningAmber = Color(0xFFF57C00),
    successMint = Color(0xFF388E3C),
    errorCoral = Color(0xFFD32F2F),
    glassFill = Color(0xD9FFFFFF),
    glassBorder = Color(0x1F000000),
    cardGlowBrush = Brush.horizontalGradient(
        listOf(Color(0x2200897B), Color(0x153949AB), Color(0x00FFFFFF))
    )
)

val LocalMalhouthaColors = staticCompositionLocalOf { DarkMalhouthaColors }

/**
 * High-legibility typography optimized for bilingual Arabic-English clinical terms.
 */
@Immutable
data class MalhouthaTypography(
    val titleHero: TextStyle,
    val titleSection: TextStyle,
    val titleCard: TextStyle,
    val subtitle: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelBadge: TextStyle,
    val termLatin: TextStyle,
    val termArabic: TextStyle
)

private val defaultLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val DefaultMalhouthaTypography = MalhouthaTypography(
    titleHero = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        lineHeightStyle = defaultLineHeightStyle
    ),
    titleSection = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        lineHeightStyle = defaultLineHeightStyle
    ),
    titleCard = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        lineHeightStyle = defaultLineHeightStyle
    ),
    subtitle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        lineHeightStyle = defaultLineHeightStyle
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        lineHeightStyle = defaultLineHeightStyle
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        lineHeightStyle = defaultLineHeightStyle
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        lineHeightStyle = defaultLineHeightStyle
    ),
    labelBadge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.5.sp,
        lineHeightStyle = defaultLineHeightStyle
    ),
    termLatin = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        lineHeightStyle = defaultLineHeightStyle
    ),
    termArabic = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        lineHeightStyle = defaultLineHeightStyle
    )
)

val ClinicalTeal = Color(0xFF00897B)
val ClinicalTealDark = Color(0xFF26A69A)

val LocalMalhouthaTypography = staticCompositionLocalOf { DefaultMalhouthaTypography }

@Composable
fun MalhouthaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkMalhouthaColors else LightMalhouthaColors
    val m3Colors = if (darkTheme) {
        darkColorScheme(
            primary = colors.clinicalTeal,
            onPrimary = Color(0xFF003731),
            primaryContainer = Color(0xFF004D40),
            onPrimaryContainer = Color(0xFF80CBC4),
            secondary = colors.royalIndigo,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFF283593),
            onSecondaryContainer = Color(0xFFC5CAE9),
            tertiary = colors.accentPurple,
            background = colors.background,
            surface = colors.surface,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    } else {
        lightColorScheme(
            primary = colors.clinicalTeal,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE0F2F1),
            onPrimaryContainer = Color(0xFF004D40),
            secondary = colors.royalIndigo,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE8EAF6),
            onSecondaryContainer = Color(0xFF1A237E),
            tertiary = colors.accentPurple,
            background = colors.background,
            surface = colors.surface,
            onBackground = colors.textPrimary,
            onSurface = colors.textPrimary
        )
    }

    CompositionLocalProvider(
        LocalMalhouthaColors provides colors,
        LocalMalhouthaTypography provides DefaultMalhouthaTypography
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            content = content
        )
    }
}
