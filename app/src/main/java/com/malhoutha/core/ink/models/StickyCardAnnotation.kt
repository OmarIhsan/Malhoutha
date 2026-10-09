package com.malhoutha.core.ink.models

import java.util.UUID

/**
 * Curated pastel color palette for Sticky Card annotations (Post-it style).
 */
object StickyCardPalette {
    const val YELLOW = 0xFFFFF9C4L  // Classic Pastel Post-it
    const val TEAL   = 0xFFE0F2F1L  // Soft Clinical Teal Post-it (Malhoutha Brand)
    const val GREEN  = 0xFFC8E6C9L  // Mint Green
    const val BLUE   = 0xFFBBDEFBL  // Sky Blue
    const val PEACH  = 0xFFFFE0B2L  // Warm Peach
    const val PURPLE = 0xFFE1BEE7L  // Soft Lavender
    const val ROSE   = 0xFFF8BBD0L  // Pastel Rose
    val ALL = listOf(YELLOW, TEAL, GREEN, BLUE, PEACH, PURPLE, ROSE)
}

/**
 * Rich, interactive Sticky Note / Sticky Card annotation bounded in normalized page space ([0.0..1.0]).
 *
 * Provides a floating, foldable pastel card note featuring dynamic vertical expansion,
 * content editing (title + content body), pinning, fold toggle, and draggable spatial placement over PDF pages.
 */
data class StickyCardAnnotation(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int = 0,
    val xNorm: Float = 0.1f,               // Normalized [0.0..1.0] relative to page width
    val yNorm: Float = 0.1f,               // Normalized [0.0..1.0] relative to page height
    val widthNorm: Float = 0.35f,          // Default proportional card width
    val heightNorm: Float = 0f,            // 0f indicates intrinsic wrap_content
    val title: String = "",
    val content: String = "",
    val colorHex: Long = StickyCardPalette.YELLOW,
    val isFolded: Boolean = false,         // Minimized to small icon vs. expanded card
    val isPinned: Boolean = false,
    val alignment: String = "START"
) {
    val body: String get() = content
}
