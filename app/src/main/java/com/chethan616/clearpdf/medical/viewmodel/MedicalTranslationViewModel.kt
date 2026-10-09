package com.chethan616.clearpdf.medical.viewmodel

import android.content.Context
import android.graphics.RectF
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chethan616.clearpdf.medical.audio.MedicalPronunciationEngine
import com.chethan616.clearpdf.medical.domain.MedicalTextSanitizer
import com.chethan616.clearpdf.medical.domain.TranslationResult
import com.chethan616.clearpdf.medical.repository.MedicalTranslationRepository
import com.chethan616.clearpdf.medical.repository.VocabularyDeckRepository
import com.chethan616.clearpdf.medical.ui.MedicalTooltipUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Two-phase state machine coordinating text selection overlays:
 * - Phase 1: Text highlighted -> show compact action strip with intentional Translate button
 * - Phase 2: Translate tapped -> smoothly morphs into active translation pill
 */
sealed class SelectionOverlayState {
    object Idle : SelectionOverlayState()

    // Phase 1: Text highlighted -> show compact action strip with Translate button
    data class ActionStripVisible(
        val selectedText: String,
        val anchorCoordinates: RectF // screen coordinates
    ) : SelectionOverlayState()

    // Phase 2: Translate tapped -> show active translation pill
    data class TranslationPillVisible(
        val selectedText: String,
        val result: TranslationResult? = null,
        val anchorCoordinates: RectF,
        val isDetailsExpanded: Boolean = false,
        val isLoading: Boolean = false
    ) : SelectionOverlayState()
}

fun Rect.toRectF(): RectF = RectF(left, top, right, bottom)
fun RectF.toComposeRect(): Rect = Rect(left, top, right, bottom)

/**
 * Reactive state holding the instant quick translation (Selection Redesign Step 2 & 3).
 */
data class QuickTranslationState(
    val text: String = "",
    val arabicText: String = "",
    val clinicalDomain: String? = null,
    val isLoading: Boolean = false,
    val hasTranslation: Boolean = false
)

/**
 * Reactive state holding the unified detailed translation modal / anchored sheet (Step 4).
 */
data class DetailedTranslationState(
    val isVisible: Boolean = false,
    val sourceText: String = "",
    val result: TranslationResult? = null,
    val anchorBounds: RectF? = null,
    val pageIndex: Int = 0
)

/**
 * ViewModel orchestrating intentional selection action strips, smooth morphing
 * translation pills, clinical domain classification, offline audio, and deep-dive sheet expansions.
 */
class MedicalTranslationViewModel(
    private val repository: MedicalTranslationRepository,
    private val deckRepository: VocabularyDeckRepository,
    private val pronunciationEngine: MedicalPronunciationEngine? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicalTooltipUiState())
    val uiState: StateFlow<MedicalTooltipUiState> = _uiState.asStateFlow()

    private val _overlayState = MutableStateFlow<SelectionOverlayState>(SelectionOverlayState.Idle)
    val overlayState: StateFlow<SelectionOverlayState> = _overlayState.asStateFlow()

    private val _isDetailedTranslationVisible = MutableStateFlow(false)
    val isDetailedTranslationVisible: StateFlow<Boolean> = _isDetailedTranslationVisible.asStateFlow()

    private val _detailedTranslationState = MutableStateFlow(DetailedTranslationState())
    val detailedTranslationState: StateFlow<DetailedTranslationState> = _detailedTranslationState.asStateFlow()

    private val _quickTranslationState = MutableStateFlow(QuickTranslationState())
    val quickTranslationState: StateFlow<QuickTranslationState> = _quickTranslationState.asStateFlow()

    private var lookupJob: Job? = null
    private var quickLookupJob: Job? = null

    /**
     * Auto quick lookup triggered when selection changes (Step 2).
     * For concise terms (1 to 4 words), performs debounced background lookup (50-75ms)
     * and publishes results to [quickTranslationState] without blocking main/inking threads.
     */
    fun onTextSelectionChanged(selectedText: String, bounds: RectF?) {
        quickLookupJob?.cancel()
        val sanitized = MedicalTextSanitizer.sanitize(selectedText)
        if (sanitized.isBlank() || bounds == null) {
            _quickTranslationState.value = QuickTranslationState()
            return
        }

        val tokens = sanitized.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.size in 1..4) {
            quickLookupJob = viewModelScope.launch(Dispatchers.IO) {
                delay(60) // 50ms–75ms debounce
                _quickTranslationState.value = QuickTranslationState(text = sanitized, isLoading = true)
                try {
                    val result = repository.translate(sanitized)
                    val arabic = when {
                        result is TranslationResult.LexicalMatch -> result.targetArabicText
                        result is TranslationResult.DecomposedCompoundMatch -> result.synthesizedArabicText
                        result.targetArabicText.isNotBlank() -> result.targetArabicText
                        else -> "No translation found"
                    }
                    val clinicalDomain = when (result) {
                        is TranslationResult.LexicalMatch -> {
                            if (result.sourceLexicon == "GENERAL_ACADEMIC_VOCAB") null
                            else result.subspecialty ?: result.domain.takeIf { it != com.chethan616.clearpdf.medical.model.MedicalDomain.GENERAL_CLINICAL }?.name
                        }
                        else -> null
                    }
                    _quickTranslationState.value = QuickTranslationState(
                        text = sanitized,
                        arabicText = arabic,
                        clinicalDomain = clinicalDomain,
                        isLoading = false,
                        hasTranslation = true
                    )
                } catch (e: Exception) {
                    _quickTranslationState.value = QuickTranslationState(
                        text = sanitized,
                        arabicText = "No translation found",
                        isLoading = false,
                        hasTranslation = false
                    )
                }
            }
        } else {
            _quickTranslationState.value = QuickTranslationState()
        }
    }

    /**
     * Triggers audio pronunciation for the selected medical term.
     */
    fun playPronunciation(text: String, isLatin: Boolean = false) {
        pronunciationEngine?.speakTerm(text, isLatin)
    }

    fun playPronunciation(context: Context, text: String, isLatin: Boolean = false) {
        MedicalPronunciationEngine.getInstance(context).speakTerm(text, isLatin)
    }

    /**
     * Dispatches a direct translation query for the given text.
     */
    fun translate(text: String, surroundingContext: String? = null, documentName: String? = null) {
        translateSelection(
            selectedText = text,
            surroundingContext = surroundingContext,
            documentName = documentName
        )
    }

    /**
     * Opens the unified detailed translation sheet / anchored modal (Step 4).
     * Single destination triggered by either the selection action bar or quick pill details.
     */
    fun openDetailedTranslation(
        text: String,
        bounds: RectF? = null,
        pageIndex: Int = 0,
        surroundingContext: String? = null,
        documentName: String? = null
    ) {
        val sanitized = MedicalTextSanitizer.sanitize(text)
        if (sanitized.isBlank()) return
        viewModelScope.launch {
            _detailedTranslationState.value = DetailedTranslationState(
                isVisible = true,
                sourceText = sanitized,
                anchorBounds = bounds,
                pageIndex = pageIndex
            )
            val result = try {
                withContext(Dispatchers.IO) {
                    repository.translate(
                        selectedText = sanitized,
                        surroundingContext = surroundingContext,
                        documentName = documentName,
                        pageIndex = pageIndex
                    )
                }
            } catch (e: Exception) {
                TranslationResult.NotFound(sanitized, e.localizedMessage ?: "Translation failed")
            }
            _detailedTranslationState.value = _detailedTranslationState.value.copy(result = result)
        }
    }

    /**
     * Dismisses the detailed translation sheet / modal.
     */
    fun dismissDetailedTranslation() {
        _detailedTranslationState.value = DetailedTranslationState(isVisible = false)
    }

    /**
     * Dispatches translation request and prepares state for detailed sheet (Step 4).
     * Explicit selection trigger passing selected text and anchor coordinates.
     */
    fun onExplicitTranslateRequested(
        text: String,
        bounds: RectF = RectF(),
        pageIndex: Int = _uiState.value.pageIndex,
        surroundingContext: String? = null,
        documentName: String? = null
    ) {
        openDetailedTranslation(
            text = text,
            bounds = bounds,
            pageIndex = pageIndex,
            surroundingContext = surroundingContext,
            documentName = documentName
        )
        val anchorRect = bounds.toComposeRect()
        translateSelection(
            selectedText = text,
            boundsInWindow = anchorRect,
            pageIndex = pageIndex,
            surroundingContext = surroundingContext,
            documentName = documentName
        )
        _isDetailedTranslationVisible.value = true
    }

    /**
     * Phase 1: Activates the unobtrusive, compact selection action strip
     * anchored at the selection coordinates. Does NOT fire premature translation queries,
     * ensuring student gesture freedom while dragging handles across lines or words.
     */
    fun showActionStrip(
        selectedText: String,
        boundsInWindow: Rect,
        boundsInPageNorm: Rect = Rect.Zero,
        pageIndex: Int = 0
    ) {
        val sanitized = MedicalTextSanitizer.sanitize(selectedText)
        if (sanitized.isBlank()) {
            dismiss()
            return
        }

        lookupJob?.cancel()
        val anchorF = boundsInWindow.toRectF()
        val nextOverlay = SelectionOverlayState.ActionStripVisible(
            selectedText = sanitized,
            anchorCoordinates = anchorF
        )

        _overlayState.value = nextOverlay
        _uiState.update {
            it.copy(
                isVisible = true,
                isLoading = false,
                isExpanded = false,
                selectedText = sanitized,
                selectionBoundsInWindow = boundsInWindow,
                selectionBoundsInPageNorm = boundsInPageNorm,
                pageIndex = pageIndex,
                result = null,
                errorMessage = null,
                overlayState = nextOverlay
            )
        }
    }

    /**
     * Phase 2: Explicit "Translate" action trigger.
     * Smoothly morphs the action strip button into the frosted glass translation pill
     * directly at the selection anchor coordinates, executing the medical translation lookup.
     */
    fun translateSelection(
        selectedText: String = _uiState.value.selectedText,
        boundsInWindow: Rect = _uiState.value.selectionBoundsInWindow,
        boundsInPageNorm: Rect = _uiState.value.selectionBoundsInPageNorm,
        pageIndex: Int = _uiState.value.pageIndex,
        surroundingContext: String? = null,
        documentName: String? = null,
        autoExpandOnResolve: Boolean = false
    ) {
        val current = _uiState.value
        val sanitized = MedicalTextSanitizer.sanitize(selectedText.ifBlank { current.selectedText })
        if (sanitized.isBlank()) {
            dismiss()
            return
        }

        if (!current.isLoading && current.result != null && current.selectedText == sanitized) {
            expandDetails()
            return
        }

        val anchorRect = if (boundsInWindow != Rect.Zero) boundsInWindow else current.selectionBoundsInWindow
        val anchorF = anchorRect.toRectF()
        val resolvingOverlay = SelectionOverlayState.TranslationPillVisible(
            selectedText = sanitized,
            result = null,
            anchorCoordinates = anchorF,
            isDetailsExpanded = autoExpandOnResolve,
            isLoading = true
        )

        _overlayState.value = resolvingOverlay
        _uiState.update {
            it.copy(
                isVisible = true,
                isLoading = true,
                isExpanded = autoExpandOnResolve,
                selectedText = sanitized,
                selectionBoundsInWindow = anchorRect,
                selectionBoundsInPageNorm = if (boundsInPageNorm != Rect.Zero) boundsInPageNorm else current.selectionBoundsInPageNorm,
                pageIndex = pageIndex,
                errorMessage = null,
                overlayState = resolvingOverlay
            )
        }

        lookupJob?.cancel()
        lookupJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.translate(
                        selectedText = sanitized,
                        surroundingContext = surroundingContext,
                        documentName = documentName,
                        pageIndex = pageIndex
                    )
                }

                val termEn = when (result) {
                    is TranslationResult.LexicalMatch -> result.matchedTerm ?: result.sourceText
                    else -> result.sourceText
                }.trim()

                val isBookmarked = withContext(Dispatchers.IO) {
                    val defaultDeck = deckRepository.getOrCreateDefaultDeck()
                    deckRepository.getCardsForDeck(defaultDeck.id)
                        .any { it.sourceTermEn.equals(termEn, ignoreCase = true) && it.isBookmarked }
                }

                val targetExpanded = if (autoExpandOnResolve) true else _uiState.value.isExpanded
                val resolvedOverlay = SelectionOverlayState.TranslationPillVisible(
                    selectedText = sanitized,
                    result = result,
                    anchorCoordinates = anchorF,
                    isDetailsExpanded = targetExpanded,
                    isLoading = false
                )

                _overlayState.value = resolvedOverlay
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        result = result,
                        isModelDownloadRequired = result is TranslationResult.ModelDownloadRequired,
                        isBookmarked = isBookmarked,
                        isExpanded = targetExpanded,
                        overlayState = resolvedOverlay
                    )
                }
            } catch (e: Exception) {
                val fallbackResult = TranslationResult.NotFound(sanitized, e.localizedMessage ?: "Translation failed")
                val targetExpanded = if (autoExpandOnResolve) true else _uiState.value.isExpanded
                val errorOverlay = SelectionOverlayState.TranslationPillVisible(
                    selectedText = sanitized,
                    result = fallbackResult,
                    anchorCoordinates = anchorF,
                    isDetailsExpanded = targetExpanded,
                    isLoading = false
                )
                _overlayState.value = errorOverlay
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Translation failed",
                        result = fallbackResult,
                        isExpanded = targetExpanded,
                        overlayState = errorOverlay
                    )
                }
            }
        }
    }

    /**
     * Backward-compatible direct lookup for existing markup or programmatic translation clicks.
     */
    fun lookupInstant(
        selectedText: String,
        boundsInWindow: Rect,
        boundsInPageNorm: Rect = Rect.Zero,
        pageIndex: Int = 0,
        surroundingContext: String? = null,
        documentName: String? = null,
        debounceMs: Long = 0L,
        autoExpandOnResolve: Boolean = false
    ) {
        if (debounceMs <= 0L) {
            translateSelection(
                selectedText = selectedText,
                boundsInWindow = boundsInWindow,
                boundsInPageNorm = boundsInPageNorm,
                pageIndex = pageIndex,
                surroundingContext = surroundingContext,
                documentName = documentName,
                autoExpandOnResolve = autoExpandOnResolve
            )
        } else {
            val sanitized = MedicalTextSanitizer.sanitize(selectedText)
            if (sanitized.isBlank()) {
                dismiss()
                return
            }
            lookupJob?.cancel()
            lookupJob = viewModelScope.launch {
                delay(debounceMs)
                translateSelection(
                    selectedText = sanitized,
                    boundsInWindow = boundsInWindow,
                    boundsInPageNorm = boundsInPageNorm,
                    pageIndex = pageIndex,
                    surroundingContext = surroundingContext,
                    documentName = documentName,
                    autoExpandOnResolve = autoExpandOnResolve
                )
            }
        }
    }

    /**
     * Backward-compatible trigger mapping directly to [translateSelection].
     */
    fun forceTranslateOrExpand(
        selectedText: String = _uiState.value.selectedText,
        boundsInWindow: Rect = _uiState.value.selectionBoundsInWindow,
        boundsInPageNorm: Rect = _uiState.value.selectionBoundsInPageNorm,
        pageIndex: Int = _uiState.value.pageIndex,
        surroundingContext: String? = null,
        documentName: String? = null
    ) {
        translateSelection(
            selectedText = selectedText,
            boundsInWindow = boundsInWindow,
            boundsInPageNorm = boundsInPageNorm,
            pageIndex = pageIndex,
            surroundingContext = surroundingContext,
            documentName = documentName,
            autoExpandOnResolve = false
        )
    }

    /**
     * Expands from Tier A (compact pill) into Tier B (detailed translation sheet).
     */
    fun expandDetails() {
        _uiState.update { current ->
            val nextOverlay = when (val ov = current.overlayState) {
                is SelectionOverlayState.TranslationPillVisible -> ov.copy(isDetailsExpanded = true)
                else -> ov
            }
            _overlayState.value = nextOverlay
            current.copy(isExpanded = true, overlayState = nextOverlay)
        }
    }

    /**
     * Collapses back to Tier A (compact pill).
     */
    fun collapseDetails() {
        _isDetailedTranslationVisible.value = false
        _uiState.update { current ->
            val nextOverlay = when (val ov = current.overlayState) {
                is SelectionOverlayState.TranslationPillVisible -> ov.copy(isDetailsExpanded = false)
                else -> ov
            }
            _overlayState.value = nextOverlay
            current.copy(isExpanded = false, overlayState = nextOverlay)
        }
    }

    /**
     * Sets expansion state directly.
     */
    fun setExpanded(expanded: Boolean) {
        if (expanded) expandDetails() else collapseDetails()
    }

    /**
     * Deterministically dismisses selection action strip, hover pill and expanded sheet.
     */
    fun dismiss() {
        lookupJob?.cancel()
        quickLookupJob?.cancel()
        _quickTranslationState.value = QuickTranslationState()
        _isDetailedTranslationVisible.value = false
        _detailedTranslationState.value = DetailedTranslationState(isVisible = false)
        _overlayState.value = SelectionOverlayState.Idle
        _uiState.update {
            it.copy(
                isVisible = false,
                isExpanded = false,
                isLoading = false,
                overlayState = SelectionOverlayState.Idle
            )
        }
    }

    /**
     * Toggles flashcard deck bookmark for the currently translated term.
     */
    fun toggleBookmark(result: TranslationResult, docName: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val defaultDeck = deckRepository.getOrCreateDefaultDeck()
            val termEn = when (result) {
                is TranslationResult.LexicalMatch -> result.matchedTerm ?: result.sourceText
                else -> result.sourceText
            }.trim()

            val isCurrentlyBookmarked = _uiState.value.isBookmarked
            if (isCurrentlyBookmarked) {
                val existing = deckRepository.getCardsForDeck(defaultDeck.id)
                    .firstOrNull { it.sourceTermEn.equals(termEn, ignoreCase = true) }
                if (existing != null) {
                    deckRepository.toggleBookmark(existing.id, false)
                }
                _uiState.update { it.copy(isBookmarked = false) }
            } else {
                deckRepository.bookmarkTerm(
                    result = result,
                    documentName = docName,
                    pageIndex = _uiState.value.pageIndex,
                    deckId = defaultDeck.id
                )
                _uiState.update { it.copy(isBookmarked = true) }
            }
        }
    }

    companion object {
        fun create(context: Context): MedicalTranslationViewModel {
            val appCtx = context.applicationContext
            val repo = MedicalTranslationRepository.getInstance(appCtx)
            val deckRepo = VocabularyDeckRepository.getInstance(appCtx)
            val engine = MedicalPronunciationEngine.getInstance(appCtx)
            return MedicalTranslationViewModel(repo, deckRepo, engine)
        }
    }
}

