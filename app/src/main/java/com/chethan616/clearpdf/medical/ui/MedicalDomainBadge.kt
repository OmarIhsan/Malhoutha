package com.chethan616.clearpdf.medical.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chethan616.clearpdf.medical.domain.TranslationResult
import com.chethan616.clearpdf.medical.model.MedicalDomain
import com.chethan616.clearpdf.ui.theme.LocalIsDarkMode

/**
 * Unified canonical Malhoutha Brand Domain Badge (Step 3 Selection Redesign).
 * Strips all internal taxonomy/tier markers ([TIER 1], [TIER 2], [GENERAL], [ACADEMIC])
 * and replaces them with a singular, frosted Clinical Teal capsule bound strictly to
 * MaterialTheme.colorScheme.primary.
 *
 * If the translation source is generic academic vocabulary, the badge is suppressed entirely.
 */
@Composable
fun ClinicalDomainBadge(
    domainText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        modifier = modifier.wrapContentSize()
    ) {
        Text(
            text = domainText.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Resolves functional clinical subspecialty or domain tags from a [TranslationResult],
 * suppressing generic academic and internal taxonomy indicators.
 */
@Composable
fun MedicalDomainBadge(
    result: TranslationResult?,
    modifier: Modifier = Modifier,
    isDark: Boolean = LocalIsDarkMode.current
) {
    if (result == null) return

    val domainLabel: String? = when (result) {
        is TranslationResult.LexicalMatch -> {
            val isGeneral = result.sourceLexicon == "GENERAL_ACADEMIC_VOCAB"
            if (isGeneral) {
                // Suppress generic academic/tier 2 badges entirely
                null
            } else {
                val sub = result.subspecialty?.lowercase()
                when {
                    sub != null && (sub.contains("endo") || sub.contains("لب")) -> "ENDO"
                    sub != null && (sub.contains("perio") || sub.contains("لثة")) -> "PERIO"
                    sub != null && (sub.contains("prostho") || sub.contains("تعويض")) -> "PROSTHO"
                    sub != null && (sub.contains("surg") || sub.contains("جراح")) -> "SURGERY"
                    sub != null && (sub.contains("ortho") || sub.contains("تقويم")) -> "ORTHO"
                    sub != null && (sub.contains("pedia") || sub.contains("pedo") || sub.contains("أطفال")) -> "PEDO"
                    sub != null && (sub.contains("histo") || sub.contains("نسيج")) -> "HISTO"
                    !result.subspecialty.isNullOrBlank() -> result.subspecialty.uppercase()
                    else -> when (result.domain) {
                        MedicalDomain.ANATOMY -> "ANATOMY"
                        MedicalDomain.PATHOLOGY -> "PATHOLOGY"
                        MedicalDomain.PHARMACOLOGY -> "PHARMACOLOGY"
                        MedicalDomain.PROCEDURE -> "PROCEDURE"
                        MedicalDomain.DIAGNOSTIC -> "DIAGNOSTIC"
                        MedicalDomain.GENERAL_CLINICAL -> "CLINICAL"
                    }
                }
            }
        }
        else -> null
    }

    if (!domainLabel.isNullOrBlank()) {
        ClinicalDomainBadge(
            domainText = domainLabel,
            modifier = modifier
        )
    }
}

/**
 * Backward-compatible overload for [ClinicalDomainBadge].
 */
@Composable
fun ClinicalDomainBadge(
    result: TranslationResult?,
    isDark: Boolean = LocalIsDarkMode.current,
    modifier: Modifier = Modifier
) {
    MedicalDomainBadge(result = result, modifier = modifier, isDark = isDark)
}

/**
 * Backward-compatible overload for [MedicalDomainBadge].
 */
@Composable
fun MedicalDomainBadge(
    domainText: String,
    modifier: Modifier = Modifier
) {
    ClinicalDomainBadge(domainText = domainText, modifier = modifier)
}
