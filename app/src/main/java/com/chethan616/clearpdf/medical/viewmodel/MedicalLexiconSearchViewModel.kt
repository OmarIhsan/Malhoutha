package com.chethan616.clearpdf.medical.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chethan616.clearpdf.medical.data.SavedVocabularyCardEntity
import com.chethan616.clearpdf.medical.domain.TranslationResult
import com.chethan616.clearpdf.medical.model.MedicalDomain
import com.chethan616.clearpdf.medical.repository.MedicalTranslationRepository
import com.chethan616.clearpdf.medical.repository.VocabularyDeckRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Reactive ViewModel coordinating the tablet reference panel and slide-over lexicon search drawer.
 * Supports instant debounced FTS5/SQLite queries, domain category filtering, and recent lookup browsing.
 */
class MedicalLexiconSearchViewModel(
    private val translationRepository: MedicalTranslationRepository,
    private val deckRepository: VocabularyDeckRepository
) : ViewModel() {

    val searchQuery = MutableStateFlow("")

    val selectedDomain = MutableStateFlow<MedicalDomain?>(null)

    val selectedSubspecialty = MutableStateFlow<String?>(null)

    private val _isOpen = MutableStateFlow(false)
    val isOpen: StateFlow<Boolean> = _isOpen.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    val recentLookups: StateFlow<List<SavedVocabularyCardEntity>> = deckRepository.bookmarkedCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<TranslationResult.LexicalMatch>> = combine(
        searchQuery.debounce(150L).distinctUntilChanged(),
        selectedDomain
    ) { query, domain ->
        query to domain
    }.mapLatest { (query, domain) ->
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            if (domain != null) {
                _isSearching.value = true
                val results = translationRepository.browseTermsByDomain(domain, limit = 40)
                _isSearching.value = false
                results
            } else {
                emptyList()
            }
        } else {
            _isSearching.value = true
            val results = translationRepository.searchLexicon(trimmed, domain, limit = 40)
            _isSearching.value = false
            results
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(query: String) {
        searchQuery.value = query
    }

    fun selectDomain(domain: MedicalDomain?) {
        selectedDomain.value = if (selectedDomain.value == domain) null else domain
    }

    fun selectSubspecialty(subspecialty: String?) {
        selectedSubspecialty.value = if (selectedSubspecialty.value == subspecialty) null else subspecialty
    }

    fun openDrawer() {
        _isOpen.value = true
    }

    fun closeDrawer() {
        _isOpen.value = false
    }

    fun toggleDrawer() {
        _isOpen.value = !_isOpen.value
    }

    fun clearQuery() {
        searchQuery.value = ""
    }

    class Factory(
        private val translationRepository: MedicalTranslationRepository,
        private val deckRepository: VocabularyDeckRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MedicalLexiconSearchViewModel(translationRepository, deckRepository) as T
        }
    }
}
