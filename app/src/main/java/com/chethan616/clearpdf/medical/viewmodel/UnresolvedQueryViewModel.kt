package com.chethan616.clearpdf.medical.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chethan616.clearpdf.medical.data.GeneralTermEntity
import com.chethan616.clearpdf.medical.data.GeneralVocabularyDao
import com.chethan616.clearpdf.medical.data.MedicalLexiconDatabase
import com.chethan616.clearpdf.medical.data.SavedVocabularyCardEntity
import com.chethan616.clearpdf.medical.data.UnresolvedQueryDao
import com.chethan616.clearpdf.medical.data.UnresolvedQueryEntity
import com.chethan616.clearpdf.medical.repository.VocabularyDeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UnresolvedQueryUiState(
    val items: List<UnresolvedQueryEntity> = emptyList(),
    val searchQuery: String = "",
    val totalMissCount: Int = 0,
    val isLoading: Boolean = false,
    val pendingPatchItem: UnresolvedQueryEntity? = null,
    val toastMessage: String? = null
)

/**
 * ViewModel coordinating the self-healing missing vocabulary logger,
 * frequency analysis, and in-app lexicon patch application.
 */
class UnresolvedQueryViewModel(
    private val unresolvedDao: UnresolvedQueryDao,
    private val generalDao: GeneralVocabularyDao,
    private val deckRepository: VocabularyDeckRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _pendingPatchItem = MutableStateFlow<UnresolvedQueryEntity?>(null)
    val pendingPatchItem: StateFlow<UnresolvedQueryEntity?> = _pendingPatchItem.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val uiState: StateFlow<UnresolvedQueryUiState> = combine(
        unresolvedDao.observeTopMissedTerms(limit = 200),
        unresolvedDao.observeMissedCount(),
        _searchQuery,
        _pendingPatchItem,
        _toastMessage
    ) { terms, count, query, pendingPatch, message ->
        val filtered = if (query.isBlank()) {
            terms
        } else {
            val q = query.trim().lowercase()
            terms.filter {
                it.normalizedQuery.contains(q, ignoreCase = true) ||
                it.originalSelection.contains(q, ignoreCase = true) ||
                (it.documentName?.contains(q, ignoreCase = true) == true)
            }
        }
        UnresolvedQueryUiState(
            items = filtered,
            searchQuery = query,
            totalMissCount = count,
            isLoading = false,
            pendingPatchItem = pendingPatch,
            toastMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UnresolvedQueryUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setPendingPatchItem(item: UnresolvedQueryEntity?) {
        _pendingPatchItem.value = item
    }

    fun dismissToast() {
        _toastMessage.value = null
    }

    fun deleteMissedTerm(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            unresolvedDao.deleteTerm(query)
        }
    }

    fun clearAllMissedTerms() {
        viewModelScope.launch(Dispatchers.IO) {
            unresolvedDao.clearAll()
            _toastMessage.value = "All unresolved telemetry logs cleared."
        }
    }

    /**
     * Patches an unknown term into the local general/custom lexicon so future lookups
     * resolve instantly without app rebuilds, and optionally saves it into student flashcards.
     */
    fun patchTermToLexicon(
        item: UnresolvedQueryEntity,
        arabicTranslation: String,
        partOfSpeechOrDomain: String? = "Dental Vocabulary",
        definition: String? = null,
        addToDeck: Boolean = true
    ) {
        if (arabicTranslation.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Direct patch into local SQLite general vocabulary table
                val generalEntity = GeneralTermEntity(
                    termEn = item.normalizedQuery,
                    termAr = arabicTranslation.trim(),
                    partOfSpeech = partOfSpeechOrDomain?.takeIf { it.isNotBlank() },
                    shortDefinition = definition?.takeIf { it.isNotBlank() }
                )
                generalDao.insertTerm(generalEntity)

                // 2. Optionally inject into custom deck for spaced repetition
                if (addToDeck) {
                    val defaultDeck = deckRepository.getOrCreateDefaultDeck("My Dental Lexicon")
                    val card = SavedVocabularyCardEntity(
                        deckId = defaultDeck.id,
                        sourceTermEn = item.normalizedQuery,
                        targetTermAr = arabicTranslation.trim(),
                        latinRoot = null,
                        domain = "GENERAL_CLINICAL",
                        subspecialty = partOfSpeechOrDomain,
                        definitionEn = definition,
                        definitionAr = null,
                        sourceDocumentName = item.documentName,
                        pageIndex = item.pageIndex,
                        lookupCount = item.hitCount,
                        isBookmarked = true,
                        lastReviewedEpochMs = System.currentTimeMillis()
                    )
                    deckRepository.saveCard(card)
                }

                // 3. Remove from unresolved queue
                unresolvedDao.deleteTerm(item.normalizedQuery)
                _pendingPatchItem.value = null
                _toastMessage.value = "Patched '${item.normalizedQuery}' into offline lexicon."
            } catch (e: Exception) {
                _toastMessage.value = "Failed to patch term: ${e.message}"
            }
        }
    }

    /**
     * Generates a TSV export of high-frequency gaps for clinical faculty curriculum review.
     */
    suspend fun exportAsTsv(): String = withContext(Dispatchers.IO) {
        val list = unresolvedDao.getTopMissedTerms()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val sb = StringBuilder()
        sb.append("Normalized Query\tOriginal Selection\tHits\tDocument\tPage\tLast Seen\n")
        for (item in list) {
            val dateStr = dateFormat.format(Date(item.lastSeenEpochMs))
            sb.append("${item.normalizedQuery}\t")
            sb.append("${item.originalSelection}\t")
            sb.append("${item.hitCount}\t")
            sb.append("${item.documentName ?: "N/A"}\t")
            sb.append("${item.pageIndex + 1}\t")
            sb.append("$dateStr\n")
        }
        sb.toString()
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val db = MedicalLexiconDatabase.getInstance(context)
            val deckDao = db.vocabularyDeckDao()
            val deckRepo = VocabularyDeckRepository(deckDao)
            return UnresolvedQueryViewModel(
                unresolvedDao = db.unresolvedQueryDao(),
                generalDao = db.generalVocabularyDao(),
                deckRepository = deckRepo
            ) as T
        }
    }
}
