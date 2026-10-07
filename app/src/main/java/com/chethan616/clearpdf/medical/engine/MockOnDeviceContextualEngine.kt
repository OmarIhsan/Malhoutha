package com.chethan616.clearpdf.medical.engine

import com.chethan616.clearpdf.medical.data.MedicalLexiconDao
import com.chethan616.clearpdf.medical.domain.ExtractedMedicalEntity
import com.chethan616.clearpdf.medical.domain.TranslationResult
import com.chethan616.clearpdf.medical.model.MedicalDomain
import java.util.Locale

/**
 * Deterministic offline fallback engine simulating the MediaPipe/LiteRT on-device SLM.
 * Parses sentence structures, identifies key clinical concepts, generates contextual
 * Arabic translations, extracts entities, and creates relevant clinical notes.
 */
class MockOnDeviceContextualEngine(
    private val lexiconDao: MedicalLexiconDao? = null
) : OnDeviceContextualEngine {

    override suspend fun isModelAvailable(): Boolean = true

    override suspend fun translateContextual(
        sourceText: String,
        surroundingContext: String?
    ): TranslationResult.ContextualSentence {
        val extractedEntities = extractEntities(sourceText)
        val dominantDomain = determineDominantDomain(extractedEntities)
        val translatedArabic = synthesizeArabicTranslation(sourceText, extractedEntities)
        val clinicalNotes = generateClinicalNotes(extractedEntities, dominantDomain)

        return TranslationResult.ContextualSentence(
            sourceText = sourceText,
            targetArabicText = translatedArabic,
            domain = dominantDomain,
            clinicalNotes = clinicalNotes,
            highlightedEntities = extractedEntities
        )
    }

    private suspend fun extractEntities(text: String): List<ExtractedMedicalEntity> {
        val entities = mutableListOf<ExtractedMedicalEntity>()
        val lowerText = text.lowercase(Locale.ROOT)

        // 1. Primary clinical concepts check from DAO or dictionary
        for (pattern in KNOWN_CONCEPTS) {
            if (lowerText.contains(pattern.englishTerm.lowercase(Locale.ROOT))) {
                var arabicName = pattern.arabicTerm
                var domain = pattern.domain

                // If DAO is available, fetch latest verified terminology
                if (lexiconDao != null) {
                    val match = lexiconDao.findExactMatch(pattern.englishTerm)
                    if (match != null) {
                        arabicName = match.arabicTerm
                        domain = match.domain
                    }
                }

                entities.add(
                    ExtractedMedicalEntity(
                        englishTerm = pattern.englishTerm,
                        arabicEquivalent = arabicName,
                        domain = domain
                    )
                )
            }
        }

        return entities.distinctBy { it.englishTerm.lowercase(Locale.ROOT) }
    }

    private fun determineDominantDomain(entities: List<ExtractedMedicalEntity>): MedicalDomain {
        if (entities.isEmpty()) return MedicalDomain.GENERAL_CLINICAL
        // Return domain with highest frequency among detected entities
        return entities.groupBy { it.domain }
            .maxByOrNull { it.value.size }
            ?.key ?: MedicalDomain.GENERAL_CLINICAL
    }

    private fun synthesizeArabicTranslation(
        sourceText: String,
        entities: List<ExtractedMedicalEntity>
    ): String {
        // High-fidelity academic medical translation templates
        val lower = sourceText.lowercase(Locale.ROOT).trim()

        if (lower.contains("ameloblastoma") && lower.contains("mandible")) {
            return "يُعد الورم الأرومي المينائي ورماً سنياً ظهارياً حميداً سريرياً لكنه عدواني وارتشاحي موضعياً، ويظهر غالباً في الجزء الخلفي من الفك السفلي."
        }
        if (lower.contains("odontogenic keratocyst") && (lower.contains("recurrence") || lower.contains("recurrent"))) {
            return "يتميز الكيس القرني سني المنشأ بمعدل نكس مرتفع وسلوك حيوي ارتشاحي يستلزم تدبيراً جراحياً دقيقاً ومتابعة دورية."
        }
        if (lower.contains("inferior alveolar nerve") && (lower.contains("molar") || lower.contains("extraction") || lower.contains("canal"))) {
            return "يمر العصب السنخي السفلي عبر القناة الفكية السفلية، ويستوجب تقييماً شعاعياً دقيقاً أثناء قلع الأرحاء السفلية لتجنب الخدر والاعتلال العصبي."
        }
        if (lower.contains("lichen planus") && (lower.contains("mucosa") || lower.contains("striae"))) {
            return "الحزاز المسطح الفموي هو مرض التهابي مناعي مزمن يصيب الغشاء المخاطي ويظهر غالباً بشكل خطوط ويكهام البيضاء الشبكية."
        }
        if (lower.contains("alveolar bone") && (lower.contains("resorption") || lower.contains("periodontitis"))) {
            return "يؤدي التهاب دواعم السن المتقدم إلى ارتشاف تدريجي في العظم السنخي وفقدان الارتباط النسيجي الداعم للأسنان."
        }
        if (lower.contains("amoxicillin") && (lower.contains("antibiotic") || lower.contains("infection"))) {
            return "يُعد الأموكسيسيلين مضاداً حيوياً واسع الطيف من زمرة بيتا-لاكتام، ويُستخدم كخط علاجي أول في الإنتانات سنية المنشأ الحادة."
        }
        if (lower.contains("pulpitis") && lower.contains("caries")) {
            return "ينجم التهاب لب السن بشكل أساسي عن ارتشاح الجراثيم المترافق مع النخر السني المتقدم، ويتطلب تدبيراً لبياً محافظاً أو استئصالاً للب."
        }

        // Sentence-level term contextual replacement fallback
        var translated = sourceText
        for (entity in entities) {
            val regex = Regex("""(?i)\b${Regex.escape(entity.englishTerm)}\b""")
            translated = regex.replace(translated, "${entity.arabicEquivalent} (${entity.englishTerm})")
        }

        return "الترجمة السياقية: $translated"
    }

    private fun generateClinicalNotes(
        entities: List<ExtractedMedicalEntity>,
        domain: MedicalDomain
    ): String? {
        if (entities.isEmpty()) return null

        val notes = mutableListOf<String>()
        for (entity in entities) {
            when (entity.englishTerm.lowercase(Locale.ROOT)) {
                "inferior alveolar nerve" -> {
                    notes.add("ملاحظة سريرية تشريحية: يجب تحديد مسار العصب السنخي السفلي بدقة عبر التصوير المقطعي (CBCT) لتفادي الخدر الدائم.")
                }
                "enamel" -> {
                    notes.add("ملاحظة نسج الفم: الميناء هو النسيج الأشد قساوة وتمعدناً في الجسم، ولا يتجدد ذاتياً بعد تخربه لتموت أرومات الميناء بعد اكتمال التاج.")
                }
                "ameloblast", "ameloblasts" -> {
                    notes.add("ملاحظة نمائية: تفرز أرومات الميناء مصفوفة الميناء العضوية ثم تضمر مكونة غشاء ناسميث وجليدة الميناء الأولية.")
                }
                "dentin", "dentine" -> {
                    notes.add("ملاحظة حيوية: يستمر تكوين العاج الثانوي والترميمي طوال حياة السن بفضل حيوية أرومات العاج المبطنة للب السن.")
                }
                "odontoblast", "odontoblasts" -> {
                    notes.add("ملاحظة نسجية: تتوضع أرومات العاج على المحيط الخارجي للب السن وترسل استطالاتها داخل النبيبات العاجية.")
                }
                "pulp", "dental pulp" -> {
                    notes.add("ملاحظة معالجة لبية: يحتوي لب السن على حزم وعائية عصبية مسؤولة عن إحساس وحيوية السن والدفاع ضد النخر.")
                }
                "ameloblastoma" -> {
                    notes.add("ملاحظة تشخيصية: يتطلب الورم استئصالاً بهامش أمان سليم نظراً لطبيعته الارتشاحية ونسبة النكس العالية عند التجريف البسيط.")
                }
                "odontogenic keratocyst" -> {
                    notes.add("ملاحظة نسيجية: يرتبط متلازمة غورلين-غولتز (Gorlin-Goltz syndrome) في حال وجود أكياس قرنية متعددة.")
                }
                "amoxicillin" -> {
                    notes.add("ملاحظة دوائية: تأكد من سوابق التحسس للبنسلينات قبل وصف الدواء.")
                }
                "alveolar bone" -> {
                    notes.add("ملاحظة نسج داعمة: تقييم كثافة وارتفاع العظم السنخي حاسم قبل التخطيط لغرس الأسنان (Dental Implants).")
                }
                "mental foramen" -> {
                    notes.add("ملاحظة جراحية: تتوضع الثقبة الذقنية عادة بين ذروتي الضاحكين السفليين ويجب تجنب أذيتها أثناء التبسيط الجراحي.")
                }
            }
        }

        return if (notes.isNotEmpty()) {
            notes.joinToString("\n")
        } else {
            "تصنيف النطاق الطبي: ${domain.name}"
        }
    }

    companion object {
        private data class ConceptDefinition(
            val englishTerm: String,
            val arabicTerm: String,
            val domain: MedicalDomain
        )

        private val KNOWN_CONCEPTS: List<ConceptDefinition> by lazy {
            com.chethan616.clearpdf.medical.data.MedicalLexiconSeeder.DEFAULT_SEED_CONCEPTS.flatMap { item ->
                item.termsEn.map { enTerm ->
                    ConceptDefinition(
                        englishTerm = enTerm,
                        arabicTerm = item.termsAr.firstOrNull() ?: "",
                        domain = item.domain
                    )
                }
            }
        }
    }
}
