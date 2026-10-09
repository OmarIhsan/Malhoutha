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

    /** Compact height of a collapsed/folded sticky note badge (proportional to page height). */
    const val FOLDED_CARD_HEIGHT_NORM = 0.04f

    /** Normalized vertical spacing gap between stacked marginal sticky note cards. */
    const val DEFAULT_VERTICAL_SPACING_NORM = 0.015f

    /** Normalized top page margin boundary for sticky note placement. */
    const val DEFAULT_PAGE_MARGIN_TOP_NORM = 0.02f

    /** Normalized bottom page margin boundary for sticky note placement. */
    const val DEFAULT_PAGE_MARGIN_BOTTOM_NORM = 0.04f


    /**
     * Resolves the canonical brand palette color (Clinical Teal) for marginal sticky cards.
     */
    fun resolvePaletteColor(domain: MedicalDomain? = null): Long = StickyCardPalette.TEAL

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
                    result.subspecialty?.takeIf { it.isNotBlank() }?.let {
                        metaParts.add("• ${it.lowercase()}")
                    }
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
            is TranslationResult.ContextualMatch -> {
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
            is TranslationResult.ModelDownloadRequired -> {
                val title = "Model Download Required"
                val body = "**يتطلب تنزيل النموذج الموضعي**\n\nيرجى تنزيل نموذج الذكاء الاصطناعي الموضعي (1.1 GB) لترجمة الجمل الكاملة في وضع عدم الاتصال."
                title to body
            }
            is TranslationResult.DecomposedCompoundMatch -> {
                val title = result.sourceText.trim()
                val body = buildString {
                    appendLine("**العربية:** ${result.synthesizedArabicText.trim()}")
                    appendLine("• مركب سريري (Compound Term)")
                    appendLine()
                    appendLine("**تفكيك العناصر (Components):**")
                    result.subTokens.forEach { token ->
                        val latin = token.latinRoot?.let { " (*$it*)" } ?: ""
                        appendLine("• ${token.tokenEn} -> ${token.translationAr}$latin")
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
     * Resolves the effective normalized height of an existing sticky note, accounting for
     * whether the card is collapsed/folded into a compact badge (~0.04f) or expanded (~0.16f).
     */
    fun getEffectiveHeightNorm(note: StickyCardAnnotation): Float =
        if (note.isFolded) FOLDED_CARD_HEIGHT_NORM
        else if (note.heightNorm > 0f) note.heightNorm
        else DEFAULT_CARD_HEIGHT_NORM

    /**
     * Resolves collision-free coordinates [xNorm, yNorm] in normalized page space [0.0, 1.0].
     *
     * Features:
     * 1. Filters [existingAnnotations] to those occupying the same margin column:
     *    `abs(existing.xNorm - targetXNorm) < (cardWidthNorm * 0.5f)`.
     * 2. Checks vertical overlap taking folded card dimensions into account:
     *    `candY < existing.yNorm + existing.heightNorm + spacing` AND
     *    `candY + cardHeightNorm > existing.yNorm - spacing`.
     * 3. Performs downward slotting past colliding notes: pushes proposed `yNorm` to
     *    `existing.yNorm + existing.heightNorm + verticalSpacingNorm` and re-evaluates iteratively.
     * 4. If downward slotting overflows page bottom (exceeds 1.0 - cardHeight - pageMarginBottom),
     *    attempts an upward search for gaps above [desiredYNorm].
     * 5. If both downward and upward slots are saturated on the margin, flips [targetXNorm] to
     *    the opposite margin column and repeats downward slotting.
     * 6. Strictly clamps final coordinates to safely remain in [0.01, 0.99] bounds.
     */
    fun resolveCollisionFreeCoordinates(
        targetXNorm: Float,
        desiredYNorm: Float,
        cardWidthNorm: Float = DEFAULT_CARD_WIDTH_NORM,
        cardHeightNorm: Float = DEFAULT_CARD_HEIGHT_NORM,
        existingAnnotations: List<StickyCardAnnotation>,
        verticalSpacingNorm: Float = DEFAULT_VERTICAL_SPACING_NORM,
        pageMarginBottomNorm: Float = DEFAULT_PAGE_MARGIN_BOTTOM_NORM
    ): Pair<Float, Float> {
        val pageMarginTopNorm = DEFAULT_PAGE_MARGIN_TOP_NORM
        val maxBottomY = (1.0f - cardHeightNorm - pageMarginBottomNorm).coerceAtLeast(pageMarginTopNorm)

        // Helper: Check if candidate vertical window [candY, candY + cardHeightNorm] collides with any note
        fun findCollision(candY: Float, notes: List<StickyCardAnnotation>): StickyCardAnnotation? {
            return notes.firstOrNull { existing ->
                val existingHeight = getEffectiveHeightNorm(existing)
                candY < (existing.yNorm + existingHeight + verticalSpacingNorm) &&
                    (candY + cardHeightNorm) > (existing.yNorm - verticalSpacingNorm)
            }
        }

        // Helper: Search for an open vertical slot in a specific margin column
        fun findSlotInColumn(colX: Float): Float? {
            val colNotes = existingAnnotations
                .filter { kotlin.math.abs(it.xNorm - colX) < (cardWidthNorm * 0.5f) }
                .sortedBy { it.yNorm }

            if (colNotes.isEmpty()) {
                return desiredYNorm.coerceIn(pageMarginTopNorm, maxBottomY)
            }

            // 1. Downward slotting
            var candY = desiredYNorm.coerceIn(pageMarginTopNorm, maxBottomY)
            var iterations = 0
            val maxIterations = colNotes.size + 3
            var downwardSuccess = false

            while (iterations < maxIterations) {
                val collision = findCollision(candY, colNotes)
                if (collision == null) {
                    if (candY in pageMarginTopNorm..maxBottomY) {
                        downwardSuccess = true
                    }
                    break
                } else {
                    candY = collision.yNorm + getEffectiveHeightNorm(collision) + verticalSpacingNorm
                    iterations++
                }
            }

            if (downwardSuccess && candY <= maxBottomY) {
                return candY
            }

            // 2. Upward search: when downward exceeds page bottom boundary, search for gaps above desiredYNorm
            val upwardCandidates = mutableListOf<Float>()

            for (note in colNotes) {
                val upY = note.yNorm - verticalSpacingNorm - cardHeightNorm
                if (upY in pageMarginTopNorm..maxBottomY) {
                    if (findCollision(upY, colNotes) == null) {
                        upwardCandidates.add(upY)
                    }
                }
            }

            if (findCollision(pageMarginTopNorm, colNotes) == null) {
                upwardCandidates.add(pageMarginTopNorm)
            }

            if (upwardCandidates.isNotEmpty()) {
                val preferred = upwardCandidates
                    .filter { it <= desiredYNorm }
                    .maxOrNull()
                    ?: upwardCandidates.minByOrNull { kotlin.math.abs(it - desiredYNorm) }

                if (preferred != null) {
                    return preferred
                }
            }

            return null
        }

        // Phase 1: Search within the target margin column
        val primaryY = findSlotInColumn(targetXNorm)
        if (primaryY != null) {
            val clampedX = targetXNorm.coerceIn(0.01f, (1.0f - cardWidthNorm).coerceAtLeast(0.01f))
            val clampedY = primaryY.coerceIn(0.01f, (1.0f - cardHeightNorm).coerceAtLeast(0.01f))
            return clampedX to clampedY
        }

        // Phase 2: Target margin column saturated -> flip to opposite margin
        val oppositeX = if (targetXNorm > 0.5f) {
            0.02f // Flip right to left margin
        } else {
            (1.0f - cardWidthNorm - 0.02f).coerceIn(0f, 1f) // Flip left to right margin
        }

        val secondaryY = findSlotInColumn(oppositeX)
        if (secondaryY != null) {
            val clampedX = oppositeX.coerceIn(0.01f, (1.0f - cardWidthNorm).coerceAtLeast(0.01f))
            val clampedY = secondaryY.coerceIn(0.01f, (1.0f - cardHeightNorm).coerceAtLeast(0.01f))
            return clampedX to clampedY
        }

        // Phase 3: Both margins saturated fallback -> clamp to safe page bounds
        val fallbackX = targetXNorm.coerceIn(0.01f, (1.0f - cardWidthNorm).coerceAtLeast(0.01f))
        val fallbackY = desiredYNorm.coerceIn(pageMarginTopNorm, maxBottomY)
        return fallbackX to fallbackY
    }

    /**
     * Calculates the nearest page margin coordinates $[xNorm, yNorm]$ in normalized page space $[0.0, 1.0]$.
     *
     * Snaps to either the right margin (standard reading flow for Arabic glosses) or left margin
     * depending on text center X, and executes deterministic collision avoidance and stacking.
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

        val desiredNormY = if (hasValidBounds) {
            selectionBoundsPageNorm.top.coerceIn(
                DEFAULT_PAGE_MARGIN_TOP_NORM,
                (1.0f - cardHeightNorm - DEFAULT_PAGE_MARGIN_BOTTOM_NORM).coerceAtLeast(DEFAULT_PAGE_MARGIN_TOP_NORM)
            )
        } else {
            0.20f
        }

        return resolveCollisionFreeCoordinates(
            targetXNorm = targetNormX,
            desiredYNorm = desiredNormY,
            cardWidthNorm = cardWidthNorm,
            cardHeightNorm = cardHeightNorm,
            existingAnnotations = existingNotes,
            verticalSpacingNorm = DEFAULT_VERTICAL_SPACING_NORM,
            pageMarginBottomNorm = DEFAULT_PAGE_MARGIN_BOTTOM_NORM
        )
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

        val colorLong = StickyCardPalette.TEAL
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
