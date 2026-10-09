package com.malhoutha.ui.home

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.chethan616.clearpdf.data.repository.RecentFile
import com.chethan616.clearpdf.data.repository.RecentFilesManager
import com.chethan616.clearpdf.medical.data.SavedVocabularyCardEntity
import com.chethan616.clearpdf.medical.repository.VocabularyDeckRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

data class RecentDocumentItem(
    val uri: Uri,
    val title: String,
    val pageCount: Int,
    val lastOpenedPageIndex: Int,
    val lastAccessedTimestamp: Long,
    val thumbnailBitmap: Bitmap? = null,
    val stickyNoteCount: Int = 0,
    val formatTag: String = "PDF",
    val sizeBytes: Long = -1L,
    val pinned: Boolean = false,
    val subjectId: String? = null
)

data class SubjectCategory(
    val id: String,
    val titleEn: String,
    val titleAr: String,
    val icon: ImageVector,
    val documentCount: Int,
    val colorAccent: Color
)

data class HomeUiState(
    val recentLectures: List<RecentDocumentItem> = emptyList(),
    val filteredLectures: List<RecentDocumentItem> = emptyList(),
    val subjectCategories: List<SubjectCategory> = emptyList(),
    val selectedSubjectId: String? = null,
    val totalAnnotationsCount: Int = 0,
    val bookmarkedCardsCount: Int = 0,
    val dailyVocabularyWord: SavedVocabularyCardEntity? = null,
    val isLoading: Boolean = false,
    val searchQuery: String = ""
)

/**
 * Reactive dashboard state coordinator for Malhoutha dental & medical workstation.
 */
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val deckRepository = VocabularyDeckRepository.getInstance(application)
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
        observeVocabularyDecks()
    }

    private fun observeVocabularyDecks() {
        deckRepository.bookmarkedCards
            .onEach { cards ->
                val current = _uiState.value
                val dayIndex = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                val dailyWord = if (cards.isNotEmpty()) {
                    cards[dayIndex % cards.size]
                } else {
                    fallbackDailyTerms[dayIndex % fallbackDailyTerms.size]
                }
                _uiState.value = current.copy(
                    bookmarkedCardsCount = cards.size,
                    dailyVocabularyWord = dailyWord
                )
            }
            .launchIn(viewModelScope)
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val context = getApplication<Application>()

            val recents: List<RecentFile> = withContext(Dispatchers.IO) {
                RecentFilesManager.getRecents(context)
            }

            val docItems = recents.map { rf ->
                val nameLower = rf.name.lowercase(Locale.ROOT)
                val formatTag = when {
                    nameLower.endsWith(".pptx") || nameLower.endsWith(".ppt") -> "SLIDES"
                    nameLower.endsWith(".png") || nameLower.endsWith(".jpg") || nameLower.endsWith(".jpeg") || nameLower.endsWith(".webp") -> "IMAGE"
                    nameLower.endsWith(".docx") || nameLower.endsWith(".doc") || nameLower.endsWith(".odt") -> "DOC"
                    nameLower.endsWith(".xlsx") || nameLower.endsWith(".xls") || nameLower.endsWith(".csv") -> "SHEET"
                    else -> "PDF"
                }

                val detectedSubject = detectSubjectFromTitle(rf.name)
                // Simulated or stored note count estimation
                val notesCount = if (rf.pageCount > 10) (rf.pageCount / 8).coerceAtLeast(1) else 0

                RecentDocumentItem(
                    uri = rf.uri,
                    title = rf.name.substringBeforeLast('.').ifBlank { rf.name },
                    pageCount = rf.pageCount.coerceAtLeast(1),
                    lastOpenedPageIndex = 0,
                    lastAccessedTimestamp = rf.timestamp,
                    stickyNoteCount = notesCount,
                    formatTag = formatTag,
                    sizeBytes = rf.sizeBytes,
                    pinned = rf.pinned,
                    subjectId = detectedSubject
                )
            }

            val subjectCounts = mutableMapOf<String, Int>()
            docItems.forEach { item ->
                item.subjectId?.let { sId ->
                    subjectCounts[sId] = (subjectCounts[sId] ?: 0) + 1
                }
            }

            val subjectCategories = baseClinicalSubjects.map { cat ->
                cat.copy(documentCount = subjectCounts[cat.id] ?: 0)
            }

            val totalNotes = docItems.sumOf { it.stickyNoteCount }

            val dayIndex = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val defaultDailyWord = fallbackDailyTerms[dayIndex % fallbackDailyTerms.size]

            val currentSelected = _uiState.value.selectedSubjectId
            val currentQuery = _uiState.value.searchQuery

            val filtered = filterDocuments(docItems, currentSelected, currentQuery)

            _uiState.value = _uiState.value.copy(
                recentLectures = docItems,
                filteredLectures = filtered,
                subjectCategories = subjectCategories,
                totalAnnotationsCount = totalNotes,
                dailyVocabularyWord = _uiState.value.dailyVocabularyWord ?: defaultDailyWord,
                isLoading = false
            )
        }
    }

    fun selectSubject(subjectId: String?) {
        val nextSubject = if (_uiState.value.selectedSubjectId == subjectId) null else subjectId
        val filtered = filterDocuments(
            _uiState.value.recentLectures,
            nextSubject,
            _uiState.value.searchQuery
        )
        _uiState.value = _uiState.value.copy(
            selectedSubjectId = nextSubject,
            filteredLectures = filtered
        )
    }

    fun updateSearchQuery(query: String) {
        val filtered = filterDocuments(
            _uiState.value.recentLectures,
            _uiState.value.selectedSubjectId,
            query
        )
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredLectures = filtered
        )
    }

    fun togglePin(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            RecentFilesManager.togglePin(getApplication(), uri)
            loadDashboardData()
        }
    }

    fun removeRecent(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            RecentFilesManager.removeRecent(getApplication(), uri)
            loadDashboardData()
        }
    }

    private fun filterDocuments(
        items: List<RecentDocumentItem>,
        subjectId: String?,
        query: String
    ): List<RecentDocumentItem> {
        return items.filter { item ->
            val matchesSubject = subjectId == null || item.subjectId == subjectId
            val matchesQuery = query.isBlank() || item.title.contains(query, ignoreCase = true)
            matchesSubject && matchesQuery
        }
    }

    private fun detectSubjectFromTitle(title: String): String? {
        val t = title.lowercase(Locale.ROOT)
        return when {
            t.contains("endo") || t.contains("pulp") || t.contains("canal") || t.contains("apex") -> "endo"
            t.contains("perio") || t.contains("gingiv") || t.contains("pocket") || t.contains("calculus") -> "perio"
            t.contains("prosth") || t.contains("crown") || t.contains("bridge") || t.contains("denture") || t.contains("implant") -> "prosth"
            t.contains("operat") || t.contains("restor") || t.contains("caries") || t.contains("composite") || t.contains("amalgam") -> "operative"
            t.contains("surg") || t.contains("extract") || t.contains("fracture") || t.contains("biopsy") -> "surgery"
            t.contains("ortho") || t.contains("bracket") || t.contains("cephalom") || t.contains("malocclusion") -> "ortho"
            t.contains("patho") || t.contains("lesion") || t.contains("tumor") || t.contains("cyst") -> "pathology"
            else -> null
        }
    }

    companion object {
        val baseClinicalSubjects = listOf(
            SubjectCategory(
                id = "endo",
                titleEn = "Endodontics",
                titleAr = "معالجة لب الأسنان",
                icon = Icons.Rounded.Biotech,
                documentCount = 0,
                colorAccent = Color(0xFFEF5350)
            ),
            SubjectCategory(
                id = "perio",
                titleEn = "Periodontics",
                titleAr = "أمراض وجراحة اللثة",
                icon = Icons.Rounded.Healing,
                documentCount = 0,
                colorAccent = Color(0xFF66BB6A)
            ),
            SubjectCategory(
                id = "prosth",
                titleEn = "Prosthodontics",
                titleAr = "الاستعاضة السنية",
                icon = Icons.Rounded.MedicalServices,
                documentCount = 0,
                colorAccent = Color(0xFF42A5F5)
            ),
            SubjectCategory(
                id = "operative",
                titleEn = "Operative",
                titleAr = "مداواة وترميم الأسنان",
                icon = Icons.Rounded.CleaningServices,
                documentCount = 0,
                colorAccent = Color(0xFFFFA726)
            ),
            SubjectCategory(
                id = "surgery",
                titleEn = "Oral Surgery",
                titleAr = "جراحة الفم والفكين",
                icon = Icons.Rounded.Vaccines,
                documentCount = 0,
                colorAccent = Color(0xFFAB47BC)
            ),
            SubjectCategory(
                id = "ortho",
                titleEn = "Orthodontics",
                titleAr = "تقويم الأسنان",
                icon = Icons.Rounded.Visibility,
                documentCount = 0,
                colorAccent = Color(0xFF26A69A)
            ),
            SubjectCategory(
                id = "pathology",
                titleEn = "Oral Pathology",
                titleAr = "أمراض الفم والأنسجة",
                icon = Icons.Rounded.Science,
                documentCount = 0,
                colorAccent = Color(0xFFFF7043)
            )
        )

        val fallbackDailyTerms = listOf(
            SavedVocabularyCardEntity(
                id = -1L,
                deckId = 0L,
                sourceTermEn = "Biologic Width",
                targetTermAr = "العرض الحيوي",
                latinRoot = "Spatium biologicum",
                domain = "Periodontics",
                subspecialty = "Periodontal-Restorative Interrelationship",
                definitionEn = "The physiological dimension of junctional epithelium and connective tissue attachment (approx. 2.04 mm) above alveolar crest.",
                definitionAr = "البعد الفسيولوجي الطبيعي للنسيج الضام والظهارة الموصلية فوق الحافة السنخية المحيطة بالسن.",
                sourceDocumentName = "Periodontal Principles.pdf"
            ),
            SavedVocabularyCardEntity(
                id = -2L,
                deckId = 0L,
                sourceTermEn = "Smear Layer",
                targetTermAr = "طبقة اللطاخة",
                latinRoot = "Stratum amorphum",
                domain = "Endodontics",
                subspecialty = "Root Canal Debridement",
                definitionEn = "Amorphous microcrystalline debris layer created on dentin walls during rotary or manual canal instrumentation.",
                definitionAr = "طبقة غير متبلورة من الحطام العضوي وغير العضوي تتكون على جدران العاج أثناء تحضير القناة الجذرية.",
                sourceDocumentName = "Endodontic Instrumentation.pptx"
            ),
            SavedVocabularyCardEntity(
                id = -3L,
                deckId = 0L,
                sourceTermEn = "Amelogenesis Imperfecta",
                targetTermAr = "تخلق الميناء الناقص",
                latinRoot = "Amelogenesis imperfecta",
                domain = "Oral Pathology",
                subspecialty = "Developmental Dental Anomalies",
                definitionEn = "A group of hereditary conditions affecting the structural formation and mineralization of dental enamel in both dentitions.",
                definitionAr = "مجموعة من الاضطرابات الوراثية التي تؤثر على تكوين وتمعدن ميناء الأسنان اللبنية والدائمة.",
                sourceDocumentName = "Oral Pathology Atlas.pdf"
            ),
            SavedVocabularyCardEntity(
                id = -4L,
                deckId = 0L,
                sourceTermEn = "Ferrule Effect",
                targetTermAr = "تأثير الطوق المعدني الحامي",
                latinRoot = "Virga coronalis",
                domain = "Prosthodontics",
                subspecialty = "Post & Core Restorations",
                definitionEn = "A 1.5 to 2.0 mm vertical band of sound dentin collar surrounding the tooth, providing resistance against fracture under crowns.",
                definitionAr = "طوق بارتفاع ١٫٥ إلى ٢ مم من العاج السليم يحيط بمحيط السن ويوفر مقاومة ضد الكسر تحت التيجان الصناعية.",
                sourceDocumentName = "Fixed Prosthodontics.pdf"
            )
        )
    }
}
