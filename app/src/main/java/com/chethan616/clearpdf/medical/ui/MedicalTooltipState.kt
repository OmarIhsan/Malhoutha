package com.chethan616.clearpdf.medical.ui

import androidx.compose.ui.geometry.Rect
import com.chethan616.clearpdf.medical.domain.TranslationResult

/**
 * UI state tracking the active medical translation hover card / tooltip,
 * its window position relative to the text selection bounds, and translation payload.
 */
data class MedicalTooltipUiState(
    val isVisible: Boolean = false,
    val isLoading: Boolean = false,
    val selectedText: String = "",
    val selectionBoundsInWindow: Rect = Rect.Zero,
    val selectionBoundsInPageNorm: Rect = Rect.Zero,
    val pageIndex: Int = 0,
    val result: TranslationResult? = null,
    val errorMessage: String? = null
)
