package com.chethan616.clearpdf.medical.interop

import androidx.compose.ui.geometry.Rect
import com.chethan616.clearpdf.medical.domain.TranslationResult
import com.chethan616.clearpdf.medical.model.MedicalDomain
import com.malhoutha.core.ink.models.StickyCardAnnotation
import com.malhoutha.core.ink.models.StickyCardPalette
import java.util.UUID

/**
 * Transforms medical translation results into structured, persistent [StickyCardAnnotation]s
 * with auto-marginal placement, collision avoidance, domain-specific color coding, and
 * rich markdown formatting.
 */
object MedicalStickyCardMapper {

    /** Default width of auto-generated marginal medical note cards (proportional to page width). */
    const val DEFAULT_CARD_WIDTH_NORM = 0.28f

    /** Default initial height of auto-generated marginal medical note cards (proportional to page height). */
    const val DEFAULT_CARD_HEIGHT_NORM = 0.16f

    /**
     * Resolves the curated pastel color palette according to medical/clinical domain.
     * - Anatomy: Sky Blue
     * - Pathology: Soft Rose
     * - Pharmacology: Mint Green
     * - Procedure: Warm Yellow
     * - Diagnostic: Warm Peach
     * - General Clinical: Soft Lavender
     */
    fun resolvePaletteColor(domain: MedicalDomain): Long = when (domain) {
        MedicalDomain.ANATOMY -> StickyCardPalette.BLUE
        MedicalDomain.PATHOLOGY -> StickyCardPalette.ROSE
        MedicalDomain.PHARMACOLOGY -> StickyCardPalette.GREEN
        MedicalDomain.PROCEDURE -> StickyCardPalette.YELLOW
        MedicalDomain.DIAGNOSTIC -> StickyCardPalette.PEACH
        MedicalDomain.GENERAL_CLINICAL -> StickyCardPalette.PURPLE
    }

    /**
     * Formats bilingual title and markdown content for a [TranslationResult].
     *
     * Returns a pair of:
     * - `first`: Card title (English term or truncated clinical query)
     * - `second`: Card body in structured markdown
     */
    fun formatCardContent(result: TranslationResult): Pair<String, String> {
        return when (result) {
            is TranslationResult.LexicalMatch -> {
                val isGeneralVocab = result.sourceLexicon == "GENERAL_ACADEMIC_VOCAB"
                val title = result.sourceText.trim()
                val metaParts = mutableListOf<String>()
                if (!isGeneralVocab) {
                    result.latinName?.takeIf { it.isNotBlank() }?.let {
                        metaParts.add("*Latin: $it*")
                    }
                    val domainLabel = result.domain.name
                        .replace("_", " ")
                        .lowercase()
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    metaParts.add("• $domainLabel${result.subspecialty?.let { " ($it)" }.orEmpty()}")
                } else {
                    val partOfSpeech = result.subspecialty?.takeIf { it.isNotBlank() }?.let { " (${it.lowercase()})" }.orEmpty()
                    metaParts.add("• General Academic$partOfSpeech")
                }
                val metaLine = metaParts.joinToString(" ")

                val defParts = mutableListOf<String>()
                result.definitionAr?.takeIf { it.isNotBlank() }?.let { defParts.add(it.trim()) }
                result.definitionEn?.takeIf { it.isNotBlank() }?.let { defParts.add(it.trim()) }
                val defSection = if (defParts.isNotEmpty()) {
                    "---\n" + defParts.joinToString("\n\n")
                } else ""

                val body = buildString {
                    appendLine("**العربية:** ${result.targetArabicText.trim()}")
                    if (metaLine.isNotBlank()) {
                        appendLine(metaLine)
                    }
                    if (defSection.isNotBlank()) {
                        appendLine(defSection)
                    }
                }.trim()

                title to body
            }
            is TranslationResult.ContextualSentence -> {
                val title = if (result.sourceText.length > 35) {
                    result.sourceText.take(35).trim() + "..."
                } else {
                    result.sourceText.trim()
                }

                val body = buildString {
                    appendLine("**العربية:** ${result.targetArabicText.trim()}")
                    if (result.highlightedEntities.isNotEmpty()) {
                        appendLine()
                        appendLine("**المصطلحات الرئيسية:**")
                        result.highlightedEntities.forEach { entity ->
                            appendLine("• ${entity.englishTerm} -> ${entity.arabicEquivalent} (${entity.domain.name})")
                        }
                    }
                    result.clinicalNotes?.takeIf { it.isNotBlank() }?.let {
                        appendLine()
                        appendLine("*إرشادات سريرية:* $it")
                    }
                }.trim()

                title to body
            }
            is TranslationResult.NotFound -> {
                val title = result.sourceText.take(40).trim()
                val body = "**لم يتم العثور على ترجمة معتمدة**\n\n${result.reason}"
                title to body
            }
        }
    }

    /**
     * Calculates the nearest page margin coordinates $[xNorm, yNorm]$ in normalized page space $[0.0, 1.0]$.
     *
     * Snaps to either the right margin (standard reading flow for Arabic glosses) or left margin
     * depending on text center X, and executes collision avoidance downward to prevent overlapping
     * existing notes on the same margin.
     */
    fun calculateMarginalCoordinates(
        selectionBoundsPageNorm: Rect,
        existingNotes: List<StickyCardAnnotation>,
        cardWidthNorm: Float = DEFAULT_CARD_WIDTH_NORM,
        cardHeightNorm: Float = DEFAULT_CARD_HEIGHT_NORM
    ): Pair<Float, Float> {
        val hasValidBounds = selectionBoundsPageNorm != Rect.Zero &&
            selectionBoundsPageNorm.width > 0f &&
            selectionBoundsPageNorm.height > 0f

        val textCenterNormX = if (hasValidBounds) {
            (selectionBoundsPageNorm.left + selectionBoundsPageNorm.right) / 2f
        } else {
            0.60f
        }

        val targetNormX = if (textCenterNormX > 0.5f) {
            // Right-hand margin placement (standard reading alignment for Arabic glosses)
            (1.0f - cardWidthNorm - 0.02f).coerceIn(0f, 1f)
        } else {
            // Left-hand margin placement (e.g. left margin in multi-column layout)
            0.02f
        }

        var targetNormY = if (hasValidBounds) {
            selectionBoundsPageNorm.top.coerceIn(0.02f, 1.0f - cardHeightNorm - 0.02f)
        } else {
            0.20f
        }

        // Avoid overlap with existing notes on the same margin side (within vertical threshold ± 0.04f)
        val isRightSide = targetNormX > 0.5f
        val sameMarginNotes = existingNotes.filter { note ->
            (note.xNorm > 0.5f) == isRightSide
        }

        var collision = true
        var attempts = 0
        while (collision && attempts < 12) {
            val hit = sameMarginNotes.any { note ->
                kotlin.math.abs(note.yNorm - targetNormY) < 0.04f
            }
            if (hit) {
                targetNormY = (targetNormY + 0.05f).coerceAtMost(1.0f - cardHeightNorm - 0.02f)
                attempts++
            } else {
                collision = false
            }
        }

        return targetNormX to targetNormY
    }

    /**
     * Constructs a complete [StickyCardAnnotation] ready for insertion into the document's active page.
     */
    fun mapToStickyCard(
        result: TranslationResult,
        pageIndex: Int,
        selectionBoundsPageNorm: Rect = Rect.Zero,
        existingNotes: List<StickyCardAnnotation> = emptyList(),
        cardWidthNorm: Float = DEFAULT_CARD_WIDTH_NORM,
        cardHeightNorm: Float = DEFAULT_CARD_HEIGHT_NORM
    ): StickyCardAnnotation {
        val (targetNormX, targetNormY) = calculateMarginalCoordinates(
            selectionBoundsPageNorm = selectionBoundsPageNorm,
            existingNotes = existingNotes,
            cardWidthNorm = cardWidthNorm,
            cardHeightNorm = cardHeightNorm
        )

        val isGeneralVocab = (result as? TranslationResult.LexicalMatch)?.sourceLexicon == "GENERAL_ACADEMIC_VOCAB"
        val colorLong = if (isGeneralVocab) {
            StickyCardPalette.PURPLE
        } else {
            resolvePaletteColor(result.domain)
        }
        val (titleText, bodyContent) = formatCardContent(result)

        return StickyCardAnnotation(
            id = UUID.randomUUID().toString(),
            pageIndex = pageIndex,
            xNorm = targetNormX,
            yNorm = targetNormY,
            widthNorm = cardWidthNorm,
            heightNorm = cardHeightNorm,
            title = titleText,
            content = bodyContent,
            colorHex = colorLong,
            isFolded = false
        )
    }
}
