package com.chethan616.clearpdf.medical.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Built-in autonomous seeder for general academic, connective, and scientific vocabulary (AWL 1-10).
 * Populates high-frequency lecture note transitions, procedural headings, and academic prose terms
 * into the isolated `general_terms` table without touching clinical lexicon tables.
 *
 * Architecture & Performance:
 * - Chunked into modular sublist collections to strictly prevent JVM 64KB bytecode limits (`Method code too large!`).
 * - Idempotent, sub-5ms lookup guarantee via @Insert(onConflict = OnConflictStrategy.IGNORE).
 * - Thread-safe cached initialization preventing redundant writes on subsequent lookups.
 */
object GeneralVocabularySeeder {

    @Volatile
    private var isSeededInMemory = false

    /**
     * Seeds default academic and transition vocabulary synchronously.
     * Early-exits if the database is already populated (from pre-compiled SQLite asset or prior seed).
     */
    fun seedDefaultVocabulary(dao: GeneralVocabularyDao, forceRefresh: Boolean = false) {
        val hasLandmark = dao.findExactGeneralTerm("biologic") != null && dao.findExactGeneralTerm("repair") != null
        if (!forceRefresh && isSeededInMemory && hasLandmark) return
        if (!forceRefresh && dao.countTerms() > 0 && hasLandmark) {
            isSeededInMemory = true
            return
        }

        for (chunk in ALL_SUBLIST_CHUNKS) {
            dao.insertTerms(chunk)
        }
        isSeededInMemory = true
    }

    /**
     * Non-blocking coroutine seeder executing on [Dispatchers.IO].
     */
    suspend fun seedDefaultVocabularyAsync(dao: GeneralVocabularyDao, forceRefresh: Boolean = false) = withContext(Dispatchers.IO) {
        seedDefaultVocabulary(dao, forceRefresh)
    }

    // =========================================================================
    // CHUNK 1: Connectors, Logical Transitions & Scholarly Adverbs
    // =========================================================================
    val DISCOURSE_CONNECTORS: List<GeneralTermEntity> = listOf(
        GeneralTermEntity(termEn = "furthermore", termAr = "علاوة على ذلك", partOfSpeech = "adv", shortDefinition = "In addition; moreover; besides"),
        GeneralTermEntity(termEn = "moreover", termAr = "فضلاً عن ذلك", partOfSpeech = "adv", shortDefinition = "As a further matter; besides; in addition"),
        GeneralTermEntity(termEn = "consequently", termAr = "وبالتالي / نتيجة لذلك", partOfSpeech = "adv", shortDefinition = "As a result or effect; therefore"),
        GeneralTermEntity(termEn = "subsequently", termAr = "في وقت لاحق / لاحقاً", partOfSpeech = "adv", shortDefinition = "After a particular event has happened; afterward"),
        GeneralTermEntity(termEn = "predominantly", termAr = "في الغالب / بشكل سائد", partOfSpeech = "adv", shortDefinition = "Mainly; for the most part; primarily"),
        GeneralTermEntity(termEn = "markedly", termAr = "بشكل ملحوظ / جلي", partOfSpeech = "adv", shortDefinition = "To an extent which is clearly noticeable; significantly"),
        GeneralTermEntity(termEn = "whereas", termAr = "في حين أن / بينما", partOfSpeech = "conj", shortDefinition = "In contrast or comparison with the fact that"),
        GeneralTermEntity(termEn = "accordingly", termAr = "وفقاً لذلك / بناءً عليه", partOfSpeech = "adv", shortDefinition = "In a way that is appropriate to the particular circumstances"),
        GeneralTermEntity(termEn = "conversely", termAr = "على العكس من ذلك", partOfSpeech = "adv", shortDefinition = "Introducing a statement or idea which reverses one just made"),
        GeneralTermEntity(termEn = "nevertheless", termAr = "مع ذلك / بالرغم من ذلك", partOfSpeech = "adv", shortDefinition = "In spite of that; notwithstanding; all the same"),
        GeneralTermEntity(termEn = "nonetheless", termAr = "ومع ذلك / على الرغم من ذلك", partOfSpeech = "adv", shortDefinition = "In spite of what has just been mentioned"),
        GeneralTermEntity(termEn = "notably", termAr = "لا سيما / بشكل خاص", partOfSpeech = "adv", shortDefinition = "Especially; in particular; worthy of attention"),
        GeneralTermEntity(termEn = "specifically", termAr = "تحديداً / على وجه التحديد", partOfSpeech = "adv", shortDefinition = "In a specific manner; clearly and explicitly"),
        GeneralTermEntity(termEn = "simultaneously", termAr = "في آن واحد / بالتزامن", partOfSpeech = "adv", shortDefinition = "At the same time; concurrently"),
        GeneralTermEntity(termEn = "initially", termAr = "في البداية / مبدئياً", partOfSpeech = "adv", shortDefinition = "At the beginning; originally"),
        GeneralTermEntity(termEn = "primarily", termAr = "أساساً / في المقام الأول", partOfSpeech = "adv", shortDefinition = "For the most part; mainly; chiefly"),
        GeneralTermEntity(termEn = "significantly", termAr = "بشكل كبير / ملحوظ", partOfSpeech = "adv", shortDefinition = "In a sufficiently great or important way"),
        GeneralTermEntity(termEn = "frequently", termAr = "تكراراً / في كثير من الأحيان", partOfSpeech = "adv", shortDefinition = "Regularly or habitually; often"),
        GeneralTermEntity(termEn = "rarely", termAr = "نادراً", partOfSpeech = "adv", shortDefinition = "Not often; seldom"),
        GeneralTermEntity(termEn = "concomitantly", termAr = "بالتزامن / بالترافق", partOfSpeech = "adv", shortDefinition = "At the same time as something else; simultaneously"),
        GeneralTermEntity(termEn = "alternatively", termAr = "أو بدلاً من ذلك / كخيار بديل", partOfSpeech = "adv", shortDefinition = "As another option or possibility"),
        GeneralTermEntity(termEn = "substantially", termAr = "بشكل جوهري / إلى حد كبير", partOfSpeech = "adv", shortDefinition = "To a great or significant extent"),
        GeneralTermEntity(termEn = "presumably", termAr = "من المفترض / على الأرجح", partOfSpeech = "adv", shortDefinition = "Used to convey that what is asserted is very likely"),
        GeneralTermEntity(termEn = "virtually", termAr = "فعلياً / تقريباً", partOfSpeech = "adv", shortDefinition = "Nearly; almost entirely"),
        GeneralTermEntity(termEn = "inevitably", termAr = "حتماً / لا محالة", partOfSpeech = "adv", shortDefinition = "As is certain to happen; unavoidably"),
        GeneralTermEntity(termEn = "invariably", termAr = "دوماً / بشكل ثابت", partOfSpeech = "adv", shortDefinition = "In every case or on every occasion; always"),
        GeneralTermEntity(termEn = "essentially", termAr = "أساساً / جوهرياً", partOfSpeech = "adv", shortDefinition = "Fundamentally; in essence"),
        GeneralTermEntity(termEn = "ultimately", termAr = "في نهاية المطاف / ختاماً", partOfSpeech = "adv", shortDefinition = "In the end; eventually"),
        GeneralTermEntity(termEn = "previously", termAr = "سابقاً / من قبل", partOfSpeech = "adv", shortDefinition = "At a previous or earlier time"),
        GeneralTermEntity(termEn = "incidentally", termAr = "عرضاً / بالمناسبة", partOfSpeech = "adv", shortDefinition = "By the way; incidentally"),
        GeneralTermEntity(termEn = "comparatively", termAr = "نسبياً / بالمقارنة", partOfSpeech = "adv", shortDefinition = "As compared to something else; relatively"),
        GeneralTermEntity(termEn = "consistently", termAr = "بشكل متسق / دوماً", partOfSpeech = "adv", shortDefinition = "In every case or at every time; continuously"),
        GeneralTermEntity(termEn = "extensively", termAr = "على نطاق واسع / بتوسع", partOfSpeech = "adv", shortDefinition = "To a large degree or in many areas"),
        GeneralTermEntity(termEn = "principally", termAr = "بشكل رئيسي", partOfSpeech = "adv", shortDefinition = "For the most part; chiefly"),
        GeneralTermEntity(termEn = "respectively", termAr = "على التوالي / بالترتيب", partOfSpeech = "adv", shortDefinition = "Separately or individually in the order mentioned"),
        GeneralTermEntity(termEn = "thereby", termAr = "وبذلك / وبناءً عليه", partOfSpeech = "adv", shortDefinition = "By that means; as a result of that"),
        GeneralTermEntity(termEn = "therefore", termAr = "لذلك / بناءً عليه", partOfSpeech = "adv", shortDefinition = "For that reason; consequently"),
        GeneralTermEntity(termEn = "thus", termAr = "وهكذا / بالتالي", partOfSpeech = "adv", shortDefinition = "As a result or consequence of this; in this manner"),
        GeneralTermEntity(termEn = "hence", termAr = "ومن ثم / ولهذا", partOfSpeech = "adv", shortDefinition = "As a consequence; for this reason"),
        GeneralTermEntity(termEn = "after", termAr = "بعد / عقب / في أعقاب", partOfSpeech = "prep", shortDefinition = "Following in time or at a later period")
    )

    // =========================================================================
    // CHUNK 2: Procedural & Lecture Headings / Academic Structures
    // =========================================================================
    val PROCEDURAL_HEADINGS: List<GeneralTermEntity> = listOf(
        GeneralTermEntity(termEn = "consider", termAr = "يعتبر / يراعي / يأخذ بعين الاعتبار", partOfSpeech = "verb", shortDefinition = "Think carefully about; take into clinical account"),
        GeneralTermEntity(termEn = "considered", termAr = "معتبر / مدروس بعناية", partOfSpeech = "adj", shortDefinition = "Thought about carefully"),
        GeneralTermEntity(termEn = "considering", termAr = "بالنظر إلى / مع مراعاة", partOfSpeech = "prep", shortDefinition = "Taking into consideration"),
        GeneralTermEntity(termEn = "consideration", termAr = "اعتبار / مراعاة / تدبّر", partOfSpeech = "noun", shortDefinition = "Careful thought, deliberation, or clinical factor"),
        GeneralTermEntity(termEn = "considerations", termAr = "اعتبارات / مراعاة", partOfSpeech = "noun", shortDefinition = "Factors considered in clinical decision-making"),
        GeneralTermEntity(termEn = "observation", termAr = "ملاحظة / رصد سريري", partOfSpeech = "noun", shortDefinition = "The action of observing carefully"),
        GeneralTermEntity(termEn = "observations", termAr = "ملاحظات / معطيات رصدية", partOfSpeech = "noun", shortDefinition = "Noted facts or clinical findings"),
        GeneralTermEntity(termEn = "characteristic", termAr = "خاصية / سمة مميزة", partOfSpeech = "noun", shortDefinition = "A typical or noticeable feature or quality"),
        GeneralTermEntity(termEn = "characteristics", termAr = "خصائص / سمات", partOfSpeech = "noun", shortDefinition = "Distinctive typical qualities or clinical traits"),
        GeneralTermEntity(termEn = "parameter", termAr = "معيار / متغير / معامل", partOfSpeech = "noun", shortDefinition = "A measurable factor defining a system or condition"),
        GeneralTermEntity(termEn = "parameters", termAr = "معايير / معاملات / محددات", partOfSpeech = "noun", shortDefinition = "Measurable factors or guidelines determining a condition"),
        GeneralTermEntity(termEn = "criterion", termAr = "معيار / مقياس", partOfSpeech = "noun", shortDefinition = "A principle or standard by which something is judged"),
        GeneralTermEntity(termEn = "criteria", termAr = "معايير / مقاييس", partOfSpeech = "noun", shortDefinition = "Diagnostic standards used for assessment"),
        GeneralTermEntity(termEn = "dimension", termAr = "بُعد / قياس", partOfSpeech = "noun", shortDefinition = "A measurable extent (e.g. vertical dimension)"),
        GeneralTermEntity(termEn = "dimensions", termAr = "أبعاد / قياسات", partOfSpeech = "noun", shortDefinition = "Physical or conceptual spatial extents"),
        GeneralTermEntity(termEn = "mechanism", termAr = "آلية / ميكانيكية", partOfSpeech = "noun", shortDefinition = "A system of parts working together in a process"),
        GeneralTermEntity(termEn = "mechanisms", termAr = "آليات / مسارات حيوية", partOfSpeech = "noun", shortDefinition = "Biological, chemical, or physical pathways"),
        GeneralTermEntity(termEn = "technique", termAr = "تقنية / أسلوب إجرائي", partOfSpeech = "noun", shortDefinition = "A practical method applied to a particular task"),
        GeneralTermEntity(termEn = "techniques", termAr = "تقنيات / أساليب", partOfSpeech = "noun", shortDefinition = "Specific clinical or laboratory methods"),
        GeneralTermEntity(termEn = "protocol", termAr = "بروتوكول / خطة علاجية قياسية", partOfSpeech = "noun", shortDefinition = "The official procedure governing treatment"),
        GeneralTermEntity(termEn = "protocols", termAr = "بروتوكولات / قواعد إجرائية", partOfSpeech = "noun", shortDefinition = "Systematic therapeutic guidelines"),
        GeneralTermEntity(termEn = "evaluation", termAr = "تقييم / تقدير", partOfSpeech = "noun", shortDefinition = "The making of a judgment about clinical metrics"),
        GeneralTermEntity(termEn = "evaluations", termAr = "تقييمات", partOfSpeech = "noun", shortDefinition = "Diagnostic assessments or appraisals"),
        GeneralTermEntity(termEn = "outcome", termAr = "حصيلة / نتيجة علاجية", partOfSpeech = "noun", shortDefinition = "The way a clinical case turns out; therapeutic result"),
        GeneralTermEntity(termEn = "outcomes", termAr = "نتائج / مخرجات", partOfSpeech = "noun", shortDefinition = "Clinical results or long-term therapeutic effects"),
        GeneralTermEntity(termEn = "consequence", termAr = "عاقبة / نتيجة", partOfSpeech = "noun", shortDefinition = "A result or effect of an action or condition"),
        GeneralTermEntity(termEn = "consequences", termAr = "عواقب / تداعيات", partOfSpeech = "noun", shortDefinition = "Effects or after-effects of disease or treatment"),
        GeneralTermEntity(termEn = "hypothesis", termAr = "فرضية / فرضية علمية", partOfSpeech = "noun", shortDefinition = "A proposed explanation made as a starting point for investigation"),
        GeneralTermEntity(termEn = "hypotheses", termAr = "فرضيات / فرضيات علمية", partOfSpeech = "noun", shortDefinition = "Scientific hypotheses proposed for testing"),
        GeneralTermEntity(termEn = "validity", termAr = "صلاحية / موثوقية علمية", partOfSpeech = "noun", shortDefinition = "The quality of being logically or factually sound"),
        GeneralTermEntity(termEn = "implementation", termAr = "تنفيذ / تطبيق عملي", partOfSpeech = "noun", shortDefinition = "The process of putting a plan into effect"),
        GeneralTermEntity(termEn = "implication", termAr = "مقتضى / مغزى / أثر غير مباشر", partOfSpeech = "noun", shortDefinition = "The conclusion drawn from something although not stated"),
        GeneralTermEntity(termEn = "implications", termAr = "تداعيات / آثار ضمنية", partOfSpeech = "noun", shortDefinition = "Likely consequences or therapeutic deductions"),
        GeneralTermEntity(termEn = "overview", termAr = "نظرة عامة / ملخص شامل", partOfSpeech = "noun", shortDefinition = "A general review or summary of a subject"),
        GeneralTermEntity(termEn = "objective", termAr = "هدف / غاية / موضوعي", partOfSpeech = "noun", shortDefinition = "A thing aimed at or sought; a goal"),
        GeneralTermEntity(termEn = "objectives", termAr = "أهداف / غايات", partOfSpeech = "noun", shortDefinition = "Specific clinical or educational aims"),
        GeneralTermEntity(termEn = "framework", termAr = "إطار عمل / هيكل مفاهيمي", partOfSpeech = "noun", shortDefinition = "A basic structure underlying a system or concept"),
        GeneralTermEntity(termEn = "frameworks", termAr = "أطر عمل / هياكل", partOfSpeech = "noun", shortDefinition = "Underlying conceptual systems"),
        GeneralTermEntity(termEn = "methodology", termAr = "منهجية / طريقة علمية", partOfSpeech = "noun", shortDefinition = "A system of methods used in a particular area"),
        GeneralTermEntity(termEn = "methodologies", termAr = "منهجيات / طرائق علمية", partOfSpeech = "noun", shortDefinition = "Systems of methods or academic workflows"),
        GeneralTermEntity(termEn = "summary", termAr = "ملخص / موجز", partOfSpeech = "noun", shortDefinition = "A brief statement of the main points"),
        GeneralTermEntity(termEn = "conclusion", termAr = "خاتمة / استنتاج", partOfSpeech = "noun", shortDefinition = "A reasoned decision arrived at by evidence")
    )

    // =========================================================================
    // CHUNK 3: Essential Biomedical & Academic Verbs & Derived Forms
    // =========================================================================
    val BIOMEDICAL_PROSE_TERMS: List<GeneralTermEntity> = listOf(
        GeneralTermEntity(termEn = "biologic", termAr = "حيوي / بيولوجي", partOfSpeech = "adj", shortDefinition = "Relating to biology or living organisms; a biological product"),
        GeneralTermEntity(termEn = "biological", termAr = "حيوي / بيولوجي", partOfSpeech = "adj", shortDefinition = "Relating to biology or living organisms"),
        GeneralTermEntity(termEn = "biology", termAr = "علم الأحياء / بيولوجيا", partOfSpeech = "noun", shortDefinition = "The scientific study of life and living organisms"),
        GeneralTermEntity(termEn = "biologically", termAr = "حيوياً / بيولوجياً", partOfSpeech = "adv", shortDefinition = "In a biological manner; with respect to biology"),
        GeneralTermEntity(termEn = "exhibit", termAr = "يُظهر / يُبدي", partOfSpeech = "verb", shortDefinition = "Manifest or show clearly; display outward signs"),
        GeneralTermEntity(termEn = "exhibits", termAr = "يُظهر / يُبدي", partOfSpeech = "verb", shortDefinition = "Manifests or displays outward clinical signs"),
        GeneralTermEntity(termEn = "exhibited", termAr = "أظهر / أبدى", partOfSpeech = "verb", shortDefinition = "Manifested or displayed outwardly"),
        GeneralTermEntity(termEn = "exhibiting", termAr = "مُظهراً / مُبدياً", partOfSpeech = "verb", shortDefinition = "Displaying outward clinical symptoms or signs"),
        GeneralTermEntity(termEn = "manifest", termAr = "يتجلى / يظهر", partOfSpeech = "verb", shortDefinition = "Display or show by one's acts or appearance; obvious"),
        GeneralTermEntity(termEn = "manifests", termAr = "يتجلى / يظهر", partOfSpeech = "verb", shortDefinition = "Displays or appears distinctly in clinical pathology"),
        GeneralTermEntity(termEn = "manifested", termAr = "تجلى / ظهر", partOfSpeech = "verb", shortDefinition = "Became evident or displayed outwardly"),
        GeneralTermEntity(termEn = "manifestation", termAr = "مظهر / تجلٍ / علامة سريرية", partOfSpeech = "noun", shortDefinition = "An event or symptom that clearly shows a disease"),
        GeneralTermEntity(termEn = "manifestations", termAr = "مظاهر / تجليات سريرية", partOfSpeech = "noun", shortDefinition = "Outward physical or clinical signs of pathology"),
        GeneralTermEntity(termEn = "differentiate", termAr = "يُميز / يتفرع / يتمايز", partOfSpeech = "verb", shortDefinition = "Recognize differences; cellular specialization"),
        GeneralTermEntity(termEn = "differentiates", termAr = "يُميز / يتمايز", partOfSpeech = "verb", shortDefinition = "Distinguishes between entities; specializes"),
        GeneralTermEntity(termEn = "differentiated", termAr = "مُتمايز / مُميز", partOfSpeech = "adj", shortDefinition = "Specialized or distinguished from others"),
        GeneralTermEntity(termEn = "differentiation", termAr = "تمايز / تفريق", partOfSpeech = "noun", shortDefinition = "The biological process of cellular specialization"),
        GeneralTermEntity(termEn = "exacerbate", termAr = "يفاقم / يزيد من شدة", partOfSpeech = "verb", shortDefinition = "Make a disease or problem worse or more acute"),
        GeneralTermEntity(termEn = "exacerbates", termAr = "يفاقم / يزيد من شدة", partOfSpeech = "verb", shortDefinition = "Aggravates a clinical pathology"),
        GeneralTermEntity(termEn = "exacerbated", termAr = "تفاقم / زادت شدته", partOfSpeech = "adj", shortDefinition = "Made worse or aggravated"),
        GeneralTermEntity(termEn = "exacerbation", termAr = "تفاقم / نوبة اشتداد", partOfSpeech = "noun", shortDefinition = "An acute worsening of symptoms or disease"),
        GeneralTermEntity(termEn = "demonstrate", termAr = "يُثبت / يُوضح", partOfSpeech = "verb", shortDefinition = "Clearly show the truth of something by evidence"),
        GeneralTermEntity(termEn = "demonstrates", termAr = "يُثبت / يُوضح", partOfSpeech = "verb", shortDefinition = "Shows conclusively by evidence"),
        GeneralTermEntity(termEn = "demonstrated", termAr = "أثبت / أوضح", partOfSpeech = "adj", shortDefinition = "Shown conclusively through evidence"),
        GeneralTermEntity(termEn = "predispose", termAr = "يُهيئ لـ / يجعل عرضة لـ", partOfSpeech = "verb", shortDefinition = "Make someone susceptible to a disease"),
        GeneralTermEntity(termEn = "predisposes", termAr = "يُهيئ لـ", partOfSpeech = "verb", shortDefinition = "Makes susceptible to a pathological condition"),
        GeneralTermEntity(termEn = "predisposed", termAr = "مُهيأ لـ / لديه قابلية", partOfSpeech = "adj", shortDefinition = "Susceptible or inclined to a clinical state"),
        GeneralTermEntity(termEn = "predisposition", termAr = "استعداد / قابلية مسبقة", partOfSpeech = "noun", shortDefinition = "A biological susceptibility to a condition"),
        GeneralTermEntity(termEn = "synthesize", termAr = "يُخلّق / يصنع حيوياً", partOfSpeech = "verb", shortDefinition = "Produce a substance by biological synthesis"),
        GeneralTermEntity(termEn = "synthesized", termAr = "مُخلّق / مُصنع حيوياً", partOfSpeech = "adj", shortDefinition = "Produced via biochemical synthesis"),
        GeneralTermEntity(termEn = "synthesis", termAr = "تخليق / اصطناع حيوي", partOfSpeech = "noun", shortDefinition = "The production of chemical compounds by reaction"),
        GeneralTermEntity(termEn = "secrete", termAr = "يفرز", partOfSpeech = "verb", shortDefinition = "Produce and discharge a substance from a gland"),
        GeneralTermEntity(termEn = "secretes", termAr = "يفرز", partOfSpeech = "verb", shortDefinition = "Discharges from a cell or gland"),
        GeneralTermEntity(termEn = "secreted", termAr = "مُفرَز", partOfSpeech = "adj", shortDefinition = "Discharged by cells or glands"),
        GeneralTermEntity(termEn = "secretion", termAr = "إفراز", partOfSpeech = "noun", shortDefinition = "A substance produced and discharged by cells"),
        GeneralTermEntity(termEn = "secretions", termAr = "إفرازات", partOfSpeech = "noun", shortDefinition = "Substances discharged by tissues or glands"),
        GeneralTermEntity(termEn = "regenerate", termAr = "يتجدد / يُعيد تكوين", partOfSpeech = "verb", shortDefinition = "Regrow or reform lost biological tissue"),
        GeneralTermEntity(termEn = "regenerated", termAr = "مُتجدد", partOfSpeech = "adj", shortDefinition = "Restored or reformed tissue"),
        GeneralTermEntity(termEn = "regeneration", termAr = "تجدد / إعادة تشكل النسيج", partOfSpeech = "noun", shortDefinition = "The natural renewal and growth of tissues"),
        GeneralTermEntity(termEn = "deteriorate", termAr = "يتدهور / يتراجع", partOfSpeech = "verb", shortDefinition = "Become progressively worse in health"),
        GeneralTermEntity(termEn = "deterioration", termAr = "تدهور / تردي", partOfSpeech = "noun", shortDefinition = "The process of becoming progressively worse"),
        GeneralTermEntity(termEn = "proliferate", termAr = "يتكاثر / يتكاثر خلوياً", partOfSpeech = "verb", shortDefinition = "Increase rapidly in cell numbers"),
        GeneralTermEntity(termEn = "proliferation", termAr = "تكاثر خلوي / انتشار", partOfSpeech = "noun", shortDefinition = "Rapid reproduction of a cell or tissue"),
        GeneralTermEntity(termEn = "facilitate", termAr = "يُسهّل / يُيسّر", partOfSpeech = "verb", shortDefinition = "Make an action or process easy or easier"),
        GeneralTermEntity(termEn = "facilitated", termAr = "مُيسّر / مُسهّل", partOfSpeech = "adj", shortDefinition = "Assisted or made less resistant"),
        GeneralTermEntity(termEn = "constrain", termAr = "يُقيّد / يَحُد من", partOfSpeech = "verb", shortDefinition = "Severely restrict the scope or activity of"),
        GeneralTermEntity(termEn = "constrained", termAr = "مُقيّد / محدود", partOfSpeech = "adj", shortDefinition = "Restricted or limited"),
        GeneralTermEntity(termEn = "constraint", termAr = "قيد / محدّد", partOfSpeech = "noun", shortDefinition = "A limitation or restriction"),
        GeneralTermEntity(termEn = "constraints", termAr = "قيود / محددات", partOfSpeech = "noun", shortDefinition = "Restricting limits on procedures"),
        GeneralTermEntity(termEn = "integrate", termAr = "يدمج / يندمج حيوياً", partOfSpeech = "verb", shortDefinition = "Combine with another so they become a whole"),
        GeneralTermEntity(termEn = "integrated", termAr = "مُدمج / متكامل", partOfSpeech = "adj", shortDefinition = "Incorporated into a complete whole"),
        GeneralTermEntity(termEn = "integration", termAr = "اندماج / تكامل (حيوي)", partOfSpeech = "noun", shortDefinition = "The biological or structural union of parts"),
        GeneralTermEntity(termEn = "underlying", termAr = "أساسي / باطني / كامن", partOfSpeech = "adj", shortDefinition = "Situated beneath; fundamental or root cause"),
        GeneralTermEntity(termEn = "adjacent", termAr = "مجاور / ملاصق", partOfSpeech = "adj", shortDefinition = "Next to or adjoining something else"),
        GeneralTermEntity(termEn = "repair", termAr = "يرمّم / يُصلح / ترميم", partOfSpeech = "verb", shortDefinition = "Restore or mend damaged tissue or structure"),
        GeneralTermEntity(termEn = "repairs", termAr = "يرمّم / يُصلح / ترميم", partOfSpeech = "verb", shortDefinition = "Restores or mends damaged tissue"),
        GeneralTermEntity(termEn = "repaired", termAr = "يرمّم / يُصلح / ترميم", partOfSpeech = "adj", shortDefinition = "Restored or mended"),
        GeneralTermEntity(termEn = "repairing", termAr = "يرمّم / يُصلح / ترميم", partOfSpeech = "verb", shortDefinition = "Restoring or mending damaged tissue"),
        GeneralTermEntity(termEn = "provide", termAr = "يُوفّر / يُقدّم / يُزوّد", partOfSpeech = "verb", shortDefinition = "Make available for use; supply"),
        GeneralTermEntity(termEn = "provides", termAr = "يُوفّر / يُقدّم / يُزوّد", partOfSpeech = "verb", shortDefinition = "Makes available for use; supplies"),
        GeneralTermEntity(termEn = "provided", termAr = "يُوفّر / يُقدّم / يُزوّد", partOfSpeech = "adj", shortDefinition = "Supplied or made available"),
        GeneralTermEntity(termEn = "providing", termAr = "يُوفّر / يُقدّم / يُزوّد", partOfSpeech = "verb", shortDefinition = "Supplying or making available"),
        GeneralTermEntity(termEn = "prevalent", termAr = "شائع / منتشر", partOfSpeech = "adj", shortDefinition = "Widespread in a particular area or at a particular time"),
        GeneralTermEntity(termEn = "bilateral", termAr = "ثنائي الجانب", partOfSpeech = "adj", shortDefinition = "Affecting or undertaken on both sides"),
        GeneralTermEntity(termEn = "bilaterally", termAr = "على كلا الجانبين / بالجانبين", partOfSpeech = "adv", shortDefinition = "On both sides of the body"),
        GeneralTermEntity(termEn = "compromise", termAr = "يُعرض للخطر / يضر", partOfSpeech = "verb", shortDefinition = "Weaken or harm clinical stability")
    )

    // =========================================================================
    // CHUNK 4: AWL Sublist 1 Core Essentials
    // =========================================================================
    val AWL_SUBLIST_1: List<GeneralTermEntity> = listOf(
        GeneralTermEntity(termEn = "analyze", termAr = "يحلل / يدرس تحليلياً", partOfSpeech = "verb", shortDefinition = "Examine methodically the structure of"),
        GeneralTermEntity(termEn = "analysis", termAr = "تحليل / دراسة تحليلية", partOfSpeech = "noun", shortDefinition = "Detailed examination of elements"),
        GeneralTermEntity(termEn = "analyses", termAr = "تحليلات", partOfSpeech = "noun", shortDefinition = "Detailed examinations of elements"),
        GeneralTermEntity(termEn = "approach", termAr = "مقاربة / نهج / يقترب", partOfSpeech = "noun", shortDefinition = "A way of dealing with a situation; surgical access"),
        GeneralTermEntity(termEn = "area", termAr = "منطقة / مجال", partOfSpeech = "noun", shortDefinition = "A region or part of the body"),
        GeneralTermEntity(termEn = "assess", termAr = "يقيّم / يقدّر", partOfSpeech = "verb", shortDefinition = "Evaluate the nature, ability, or quality of"),
        GeneralTermEntity(termEn = "assessment", termAr = "تقييم / تقدير سريري", partOfSpeech = "noun", shortDefinition = "The evaluation of condition or disease"),
        GeneralTermEntity(termEn = "assume", termAr = "يفترض", partOfSpeech = "verb", shortDefinition = "Suppose to be the case, without proof"),
        GeneralTermEntity(termEn = "assumption", termAr = "افتراض", partOfSpeech = "noun", shortDefinition = "A thing accepted as true without proof"),
        GeneralTermEntity(termEn = "authority", termAr = "سلطة / مرجعية علمية", partOfSpeech = "noun", shortDefinition = "The recognized expert clinical body"),
        GeneralTermEntity(termEn = "available", termAr = "متاح / متوفر", partOfSpeech = "adj", shortDefinition = "Able to be used or obtained"),
        GeneralTermEntity(termEn = "benefit", termAr = "فائدة / منفعة", partOfSpeech = "noun", shortDefinition = "An advantage gained from an intervention"),
        GeneralTermEntity(termEn = "concept", termAr = "مفهوم / فكرة عامة", partOfSpeech = "noun", shortDefinition = "An abstract idea; a general notion"),
        GeneralTermEntity(termEn = "consist", termAr = "يتألف من / يتكون من", partOfSpeech = "verb", shortDefinition = "Be composed or made up of"),
        GeneralTermEntity(termEn = "context", termAr = "سياق / قرينة", partOfSpeech = "noun", shortDefinition = "Circumstances forming the setting for a statement"),
        GeneralTermEntity(termEn = "create", termAr = "يخلق / ينشئ", partOfSpeech = "verb", shortDefinition = "Bring something into existence"),
        GeneralTermEntity(termEn = "data", termAr = "بيانات / معطيات", partOfSpeech = "noun", shortDefinition = "Facts and statistics collected together"),
        GeneralTermEntity(termEn = "define", termAr = "يحدد / يُعرّف", partOfSpeech = "verb", shortDefinition = "State or describe exactly the meaning of"),
        GeneralTermEntity(termEn = "derive", termAr = "يشتق / يستمد", partOfSpeech = "verb", shortDefinition = "Obtain something from a specified source"),
        GeneralTermEntity(termEn = "distribute", termAr = "يوزع / ينشر", partOfSpeech = "verb", shortDefinition = "Spread throughout an anatomical area"),
        GeneralTermEntity(termEn = "establish", termAr = "يؤسس / يثبت علمياً", partOfSpeech = "verb", shortDefinition = "Prove firmly beyond doubt"),
        GeneralTermEntity(termEn = "estimate", termAr = "يقدّر / تخمين كمي", partOfSpeech = "verb", shortDefinition = "Roughly calculate the value or quantity of"),
        GeneralTermEntity(termEn = "evident", termAr = "واضح / جلي", partOfSpeech = "adj", shortDefinition = "Plain or obvious; clearly understood"),
        GeneralTermEntity(termEn = "factor", termAr = "عامل / عنصر مؤثر", partOfSpeech = "noun", shortDefinition = "A circumstance contributing to a result"),
        GeneralTermEntity(termEn = "identify", termAr = "يحدد / يتعرف على", partOfSpeech = "verb", shortDefinition = "Establish or indicate who or what something is"),
        GeneralTermEntity(termEn = "indicate", termAr = "يشير إلى / يدل على", partOfSpeech = "verb", shortDefinition = "Point out; show; express the necessity of"),
        GeneralTermEntity(termEn = "individual", termAr = "فرد / فردي", partOfSpeech = "noun", shortDefinition = "Single human being or entity"),
        GeneralTermEntity(termEn = "interpret", termAr = "يفسر / يؤول", partOfSpeech = "verb", shortDefinition = "Explain the meaning of tests or images"),
        GeneralTermEntity(termEn = "involve", termAr = "يتضمن / يشمل / يُشرك", partOfSpeech = "verb", shortDefinition = "Have or include as a necessary part"),
        GeneralTermEntity(termEn = "major", termAr = "رئيسي / كبير", partOfSpeech = "adj", shortDefinition = "Important, serious, or significant"),
        GeneralTermEntity(termEn = "method", termAr = "طريقة / أسلوب", partOfSpeech = "noun", shortDefinition = "A particular procedure for accomplishing something"),
        GeneralTermEntity(termEn = "occur", termAr = "يحدث / يطرأ", partOfSpeech = "verb", shortDefinition = "Happen; take place; exist or be found"),
        GeneralTermEntity(termEn = "percent", termAr = "بالمائة / نسبة مئوية", partOfSpeech = "noun", shortDefinition = "One part in every hundred"),
        GeneralTermEntity(termEn = "principle", termAr = "مبدأ / قاعدة أساسية", partOfSpeech = "noun", shortDefinition = "A fundamental truth serving as the foundation"),
        GeneralTermEntity(termEn = "proceed", termAr = "يشرع / يواصل / ينطلق", partOfSpeech = "verb", shortDefinition = "Carry on after a pause or interruption"),
        GeneralTermEntity(termEn = "process", termAr = "عملية / مسار حيوي / بروز تشريحي", partOfSpeech = "noun", shortDefinition = "A series of actions or anatomical bone projection"),
        GeneralTermEntity(termEn = "require", termAr = "يتطلب / يستلزم", partOfSpeech = "verb", shortDefinition = "Need for a particular purpose; demand"),
        GeneralTermEntity(termEn = "research", termAr = "بحث / دراسة علمية", partOfSpeech = "noun", shortDefinition = "Systematic investigation to establish facts"),
        GeneralTermEntity(termEn = "respond", termAr = "يستجيب / يتفاعل", partOfSpeech = "verb", shortDefinition = "React quickly or favourably to something"),
        GeneralTermEntity(termEn = "role", termAr = "دور / وظيفة", partOfSpeech = "noun", shortDefinition = "The function assumed by a factor or person"),
        GeneralTermEntity(termEn = "section", termAr = "مقطع / قسم / شريحة نسيجية", partOfSpeech = "noun", shortDefinition = "Histological cut or anatomical division"),
        GeneralTermEntity(termEn = "significant", termAr = "ذو دلالة / هام / ملحوظ", partOfSpeech = "adj", shortDefinition = "Sufficiently great or important to be worthy of attention"),
        GeneralTermEntity(termEn = "similar", termAr = "مماثل / شبيه", partOfSpeech = "adj", shortDefinition = "Having a resemblance in form or character"),
        GeneralTermEntity(termEn = "source", termAr = "مصدر / منبع", partOfSpeech = "noun", shortDefinition = "Origin or reference material"),
        GeneralTermEntity(termEn = "specific", termAr = "محدد / نوعي / خاص", partOfSpeech = "adj", shortDefinition = "Clearly defined or identified"),
        GeneralTermEntity(termEn = "structure", termAr = "بنية / هيكل / تركيب", partOfSpeech = "noun", shortDefinition = "Arrangement of and relations between parts"),
        GeneralTermEntity(termEn = "vary", termAr = "يتغير / يتباين", partOfSpeech = "verb", shortDefinition = "Differ in size, amount, degree, or nature")
    )

    // =========================================================================
    // CHUNK 5: AWL Sublist 2 Core Essentials
    // =========================================================================
    val AWL_SUBLIST_2: List<GeneralTermEntity> = listOf(
        GeneralTermEntity(termEn = "achieve", termAr = "يحقق / ينجز", partOfSpeech = "verb", shortDefinition = "Successfully bring about or reach by effort"),
        GeneralTermEntity(termEn = "acquire", termAr = "يكتسب", partOfSpeech = "verb", shortDefinition = "Develop a trait or pathology; obtain"),
        GeneralTermEntity(termEn = "acquired", termAr = "مكتسب (غير خلقي)", partOfSpeech = "adj", shortDefinition = "Developed after birth, not genetic or congenital"),
        GeneralTermEntity(termEn = "administer", termAr = "يعطي (دواء) / يدير", partOfSpeech = "verb", shortDefinition = "Dispense or apply a remedy or drug"),
        GeneralTermEntity(termEn = "affect", termAr = "يؤثر على / يصيب", partOfSpeech = "verb", shortDefinition = "Have an effect on; strike with disease"),
        GeneralTermEntity(termEn = "appropriate", termAr = "مناسب / ملائم", partOfSpeech = "adj", shortDefinition = "Suitable or proper in the circumstances"),
        GeneralTermEntity(termEn = "aspect", termAr = "جانب / مظهر / وجهة تشريحية", partOfSpeech = "noun", shortDefinition = "A particular feature; anatomical facing"),
        GeneralTermEntity(termEn = "assist", termAr = "يساعد / يعاون", partOfSpeech = "verb", shortDefinition = "Help someone in clinical practice"),
        GeneralTermEntity(termEn = "category", termAr = "فئة / تصنيف", partOfSpeech = "noun", shortDefinition = "A division having shared characteristics"),
        GeneralTermEntity(termEn = "complex", termAr = "معقد / مركب", partOfSpeech = "adj", shortDefinition = "Consisting of many different connected parts"),
        GeneralTermEntity(termEn = "conclude", termAr = "يستنتج / يختتم", partOfSpeech = "verb", shortDefinition = "Arrive at a judgment by reasoning"),
        GeneralTermEntity(termEn = "conduct", termAr = "يُجري / يوجّه / يوصل", partOfSpeech = "verb", shortDefinition = "Organize and carry out; transmit impulses"),
        GeneralTermEntity(termEn = "consequent", termAr = "لاحق / ناجم عن", partOfSpeech = "adj", shortDefinition = "Following as a result or effect"),
        GeneralTermEntity(termEn = "construct", termAr = "يبني / يشيد", partOfSpeech = "verb", shortDefinition = "Build or fabricate a prosthesis or theory"),
        GeneralTermEntity(termEn = "consume", termAr = "يستهلك / يتناول", partOfSpeech = "verb", shortDefinition = "Use up a resource; ingest food or drink"),
        GeneralTermEntity(termEn = "design", termAr = "تصميم / يخطط", partOfSpeech = "noun", shortDefinition = "A plan showing the look and function of an object"),
        GeneralTermEntity(termEn = "distinct", termAr = "متميز / واضح / منفصل", partOfSpeech = "adj", shortDefinition = "Recognizably different in nature"),
        GeneralTermEntity(termEn = "element", termAr = "عنصر / جزء أساسي", partOfSpeech = "noun", shortDefinition = "An essential part of something abstract or physical"),
        GeneralTermEntity(termEn = "equate", termAr = "يساوي بين / يماثل", partOfSpeech = "verb", shortDefinition = "Consider one thing to be the same as another"),
        GeneralTermEntity(termEn = "evaluate", termAr = "يقيّم / يقدّر", partOfSpeech = "verb", shortDefinition = "Form an idea of the amount, number, or value of"),
        GeneralTermEntity(termEn = "feature", termAr = "سمة / ملمح / خاصية", partOfSpeech = "noun", shortDefinition = "A distinctive attribute or aspect"),
        GeneralTermEntity(termEn = "final", termAr = "نهائي / أخير", partOfSpeech = "adj", shortDefinition = "Coming at the end of a series; concluding"),
        GeneralTermEntity(termEn = "focus", termAr = "بؤرة / يركز على", partOfSpeech = "noun", shortDefinition = "The center of interest; point of infection"),
        GeneralTermEntity(termEn = "impact", termAr = "أثر / انحصار السن / يصطدم", partOfSpeech = "noun", shortDefinition = "Action of coming into contact; dental impaction"),
        GeneralTermEntity(termEn = "impacted", termAr = "منحصر (مثل الضرس المطمور)", partOfSpeech = "adj", shortDefinition = "Wedged firmly in the jaw bone preventing eruption"),
        GeneralTermEntity(termEn = "injure", termAr = "يصيب / يجرح", partOfSpeech = "verb", shortDefinition = "Do physical harm or damage to tissue"),
        GeneralTermEntity(termEn = "institute", termAr = "معهد / يؤسس", partOfSpeech = "noun", shortDefinition = "An organization having a particular purpose"),
        GeneralTermEntity(termEn = "maintain", termAr = "يحافظ على / يصون", partOfSpeech = "verb", shortDefinition = "Cause or enable a condition to continue"),
        GeneralTermEntity(termEn = "normal", termAr = "طبيعي / سوي", partOfSpeech = "adj", shortDefinition = "Conforming to a standard; expected physiology"),
        GeneralTermEntity(termEn = "obtain", termAr = "يحصل على / يستمد", partOfSpeech = "verb", shortDefinition = "Get, acquire, or secure something"),
        GeneralTermEntity(termEn = "participate", termAr = "يشارك / يساهم", partOfSpeech = "verb", shortDefinition = "Take part in an action or clinical trial"),
        GeneralTermEntity(termEn = "perceive", termAr = "يُدرك / يلاحظ حسياً", partOfSpeech = "verb", shortDefinition = "Become conscious of something; notice"),
        GeneralTermEntity(termEn = "positive", termAr = "إيجابي / مؤكد", partOfSpeech = "adj", shortDefinition = "Confirming the presence of a bacterium or sign"),
        GeneralTermEntity(termEn = "potential", termAr = "محتمل / قدرة كامنة", partOfSpeech = "adj", shortDefinition = "Showing the capacity to develop in the future"),
        GeneralTermEntity(termEn = "previous", termAr = "سابق", partOfSpeech = "adj", shortDefinition = "Existing or occurring before in time"),
        GeneralTermEntity(termEn = "primary", termAr = "أولي / أساسي (مثل الأسنان اللبنية)", partOfSpeech = "adj", shortDefinition = "Earliest in time or order; deciduous dentition"),
        GeneralTermEntity(termEn = "range", termAr = "نطاق / مدى / يمتد", partOfSpeech = "noun", shortDefinition = "Area of variation between limits on a scale"),
        GeneralTermEntity(termEn = "region", termAr = "منطقة / ناحية تشريحية", partOfSpeech = "noun", shortDefinition = "An area, especially part of the body"),
        GeneralTermEntity(termEn = "regulate", termAr = "ينظم / يضبط", partOfSpeech = "verb", shortDefinition = "Control rate or concentration in physiology"),
        GeneralTermEntity(termEn = "relevant", termAr = "ذو صلة / وثيق الصلة", partOfSpeech = "adj", shortDefinition = "Closely connected or appropriate to the case"),
        GeneralTermEntity(termEn = "resource", termAr = "مورد / إمكانية", partOfSpeech = "noun", shortDefinition = "Available physical or clinical assets"),
        GeneralTermEntity(termEn = "restrict", termAr = "يقيّد / يحصر", partOfSpeech = "verb", shortDefinition = "Put a limit on; keep under control"),
        GeneralTermEntity(termEn = "secure", termAr = "يؤمّن / يثبت بإحكام", partOfSpeech = "verb", shortDefinition = "Fix or attach firmly; protected against hazard"),
        GeneralTermEntity(termEn = "select", termAr = "يختار / ينتقي", partOfSpeech = "verb", shortDefinition = "Carefully choose as being the most suitable"),
        GeneralTermEntity(termEn = "site", termAr = "موقع / موضع جراحي", partOfSpeech = "noun", shortDefinition = "Anatomical or operational position"),
        GeneralTermEntity(termEn = "strategy", termAr = "استراتيجية / خطة تدبير", partOfSpeech = "noun", shortDefinition = "A plan designed to achieve a therapeutic aim"),
        GeneralTermEntity(termEn = "survey", termAr = "استقصاء / مسح / استطلاع", partOfSpeech = "noun", shortDefinition = "Investigation of the experience of a group"),
        GeneralTermEntity(termEn = "transfer", termAr = "ينقل / تحويل", partOfSpeech = "verb", shortDefinition = "Move from one place or tissue to another")
    )

    // =========================================================================
    // CHUNK 6: AWL Sublist 3 & 4 Essentials
    // =========================================================================
    val AWL_SUBLIST_3: List<GeneralTermEntity> = listOf(
        GeneralTermEntity(termEn = "alternative", termAr = "بديل / خيار بديل", partOfSpeech = "noun", shortDefinition = "One of two or more available possibilities"),
        GeneralTermEntity(termEn = "circumstance", termAr = "ظرف / حالة ملابسة", partOfSpeech = "noun", shortDefinition = "A condition connected with a clinical event"),
        GeneralTermEntity(termEn = "compensate", termAr = "يعوّض / يوازن النقص", partOfSpeech = "verb", shortDefinition = "Physiological adjustment to counter a defect"),
        GeneralTermEntity(termEn = "component", termAr = "مكون / جزء تركيبي", partOfSpeech = "noun", shortDefinition = "A part or element of a larger whole"),
        GeneralTermEntity(termEn = "consent", termAr = "موافقة / إقرار طبي مستنير", partOfSpeech = "noun", shortDefinition = "Permission for something to happen (informed consent)"),
        GeneralTermEntity(termEn = "considerable", termAr = "كبير / ذو شأن", partOfSpeech = "adj", shortDefinition = "Notably large in size, amount, or extent"),
        GeneralTermEntity(termEn = "constant", termAr = "ثابت / مستمر", partOfSpeech = "adj", shortDefinition = "Occurring continuously over time; unchanging"),
        GeneralTermEntity(termEn = "contribute", termAr = "يساهم في / يشارك في التسبب", partOfSpeech = "verb", shortDefinition = "Help to cause or bring about an effect"),
        GeneralTermEntity(termEn = "coordinate", termAr = "ينسّق / إحداثي", partOfSpeech = "verb", shortDefinition = "Bring elements into a harmonious relationship"),
        GeneralTermEntity(termEn = "core", termAr = "لب / قلب البنية (قلب الوتد)", partOfSpeech = "noun", shortDefinition = "Central part; dental core build-up"),
        GeneralTermEntity(termEn = "correspond", termAr = "يتطابق / يتوافق مع", partOfSpeech = "verb", shortDefinition = "Match or agree almost exactly"),
        GeneralTermEntity(termEn = "deduce", termAr = "يستنتج / يستدل", partOfSpeech = "verb", shortDefinition = "Arrive at a conclusion by reasoning"),
        GeneralTermEntity(termEn = "document", termAr = "يوثق / وثيقة", partOfSpeech = "verb", shortDefinition = "Record in written or photographic form"),
        GeneralTermEntity(termEn = "dominate", termAr = "يهيمن / يسود", partOfSpeech = "verb", shortDefinition = "Have a commanding influence on; genetic dominance"),
        GeneralTermEntity(termEn = "emphasis", termAr = "تأكيد / تركيز", partOfSpeech = "noun", shortDefinition = "Special importance given to something"),
        GeneralTermEntity(termEn = "ensure", termAr = "يضمن / يحرص على", partOfSpeech = "verb", shortDefinition = "Make certain that something will occur"),
        GeneralTermEntity(termEn = "exclude", termAr = "يستبعد / يستثني تشخيصياً", partOfSpeech = "verb", shortDefinition = "Rule out in differential diagnosis"),
        GeneralTermEntity(termEn = "illustrate", termAr = "يُوضّح / يبيّن بالرسم أو الدليل", partOfSpeech = "verb", shortDefinition = "Explain by using examples or images"),
        GeneralTermEntity(termEn = "imply", termAr = "يلمح إلى / يفيد ضمناً", partOfSpeech = "verb", shortDefinition = "Strongly suggest something not stated directly"),
        GeneralTermEntity(termEn = "initial", termAr = "أولي / مبدئي", partOfSpeech = "adj", shortDefinition = "Existing or occurring at the beginning"),
        GeneralTermEntity(termEn = "instance", termAr = "مثال / حالة / نموذج", partOfSpeech = "noun", shortDefinition = "A clinical example or single occurrence"),
        GeneralTermEntity(termEn = "interact", termAr = "يتفاعل مع / يتداخل", partOfSpeech = "verb", shortDefinition = "Act so as to have an effect on another (e.g. drug interaction)"),
        GeneralTermEntity(termEn = "justify", termAr = "يبرر / يسوّغ", partOfSpeech = "verb", shortDefinition = "Show or prove to be clinically reasonable"),
        GeneralTermEntity(termEn = "layer", termAr = "طبقة نسيجية / رداء", partOfSpeech = "noun", shortDefinition = "A sheet or thickness of tissue or material"),
        GeneralTermEntity(termEn = "link", termAr = "يربط / رابط / صلة", partOfSpeech = "verb", shortDefinition = "Identify a causal or anatomical relationship"),
        GeneralTermEntity(termEn = "locate", termAr = "يحدد موقع / يتموضع", partOfSpeech = "verb", shortDefinition = "Discover the exact anatomical place of"),
        GeneralTermEntity(termEn = "maximize", termAr = "يعظّم / يزيد للحد الأقصى", partOfSpeech = "verb", shortDefinition = "Make as large or great as possible"),
        GeneralTermEntity(termEn = "minimize", termAr = "يقلل للحد الأدنى", partOfSpeech = "verb", shortDefinition = "Reduce to the smallest possible degree"),
        GeneralTermEntity(termEn = "minor", termAr = "صغير / ثانوي / طفيف", partOfSpeech = "adj", shortDefinition = "Lesser in importance or seriousness"),
        GeneralTermEntity(termEn = "physical", termAr = "فيزيائي / جسدي / بدني", partOfSpeech = "adj", shortDefinition = "Relating to the body or physics"),
        GeneralTermEntity(termEn = "proportion", termAr = "نسبة / تناسب", partOfSpeech = "noun", shortDefinition = "A part considered in relation to a whole"),
        GeneralTermEntity(termEn = "publish", termAr = "ينشر (بحثاً علمياً)", partOfSpeech = "verb", shortDefinition = "Issue research in a medical journal"),
        GeneralTermEntity(termEn = "react", termAr = "يتفاعل / يستجيب كيميائياً أو حيوياً", partOfSpeech = "verb", shortDefinition = "Undergo chemical or biological response"),
        GeneralTermEntity(termEn = "rely", termAr = "يعتمد على", partOfSpeech = "verb", shortDefinition = "Depend on with trust or reproducibility"),
        GeneralTermEntity(termEn = "remove", termAr = "يزيل / يستأصل", partOfSpeech = "verb", shortDefinition = "Take away; surgically excise tissue"),
        GeneralTermEntity(termEn = "sequence", termAr = "تسلسل / تتابع زمني", partOfSpeech = "noun", shortDefinition = "Order in which related events follow"),
        GeneralTermEntity(termEn = "specify", termAr = "يحدد بدقة / ينص على", partOfSpeech = "verb", shortDefinition = "Identify clearly and definitely"),
        GeneralTermEntity(termEn = "sufficient", termAr = "كافٍ", partOfSpeech = "adj", shortDefinition = "Adequate for clinical requirements"),
        GeneralTermEntity(termEn = "technical", termAr = "تقني / فني", partOfSpeech = "adj", shortDefinition = "Relating to practical dental skill"),
        GeneralTermEntity(termEn = "technology", termAr = "تكنولوجيا / تقنية متقدمة", partOfSpeech = "noun", shortDefinition = "Scientific knowledge for practical use"),
        GeneralTermEntity(termEn = "valid", termAr = "صحيح / صالح / معتبر علمياً", partOfSpeech = "adj", shortDefinition = "Having a sound basis in logic or fact"),
        GeneralTermEntity(termEn = "volume", termAr = "حجم / سعة", partOfSpeech = "noun", shortDefinition = "The amount of space an object occupies")
    )

    // =========================================================================
    // CHUNK 7: AWL Sublist 4 & 5 Essentials
    // =========================================================================
    val AWL_SUBLIST_4: List<GeneralTermEntity> = listOf(
        GeneralTermEntity(termEn = "access", termAr = "مدخل / منفذ جراحي", partOfSpeech = "noun", shortDefinition = "Means of entering (e.g. access cavity)"),
        GeneralTermEntity(termEn = "adequate", termAr = "كافٍ / ملائم", partOfSpeech = "adj", shortDefinition = "Satisfactory in quality or quantity"),
        GeneralTermEntity(termEn = "apparent", termAr = "ظاهري / واضح", partOfSpeech = "adj", shortDefinition = "Clearly visible or seeming real"),
        GeneralTermEntity(termEn = "approximate", termAr = "تقريبي / يقارب", partOfSpeech = "adj", shortDefinition = "Close to actual, but not exact"),
        GeneralTermEntity(termEn = "attribute", termAr = "يعزو إلى / سمة مميزة", partOfSpeech = "verb", shortDefinition = "Regard as caused by; a characteristic"),
        GeneralTermEntity(termEn = "communicate", termAr = "يتواصل / ينقل (عدوى)", partOfSpeech = "verb", shortDefinition = "Share info; transmit infection or pathology"),
        GeneralTermEntity(termEn = "concentrate", termAr = "يركّز / يتركز", partOfSpeech = "verb", shortDefinition = "Increase the strength of a solution; focus"),
        GeneralTermEntity(termEn = "contrast", termAr = "تباين / مادة تباين شعاعية", partOfSpeech = "noun", shortDefinition = "Striking difference; radiopaque agent"),
        GeneralTermEntity(termEn = "cycle", termAr = "دورة / حلقة متكررة", partOfSpeech = "noun", shortDefinition = "Repeated biological sequence"),
        GeneralTermEntity(termEn = "debate", termAr = "نقاش / جدل علمي", partOfSpeech = "noun", shortDefinition = "Formal discussion in medical literature"),
        GeneralTermEntity(termEn = "despite", termAr = "على الرغم من", partOfSpeech = "prep", shortDefinition = "Without being affected by; in spite of"),
        GeneralTermEntity(termEn = "emerge", termAr = "ينبثق / يظهر", partOfSpeech = "verb", shortDefinition = "Become visible; emergence profile"),
        GeneralTermEntity(termEn = "error", termAr = "خطأ / انحراف عن الصواب", partOfSpeech = "noun", shortDefinition = "A mistake or divergence from standard"),
        GeneralTermEntity(termEn = "goal", termAr = "هدف / غاية علاجية", partOfSpeech = "noun", shortDefinition = "A desired clinical aim or result"),
        GeneralTermEntity(termEn = "grant", termAr = "منحة / يمنح", partOfSpeech = "verb", shortDefinition = "Agree to give or funding"),
        GeneralTermEntity(termEn = "implement", termAr = "يطبق / ينفذ / أداة", partOfSpeech = "verb", shortDefinition = "Put a clinical plan into effect"),
        GeneralTermEntity(termEn = "impose", termAr = "يفرض / يسلّط", partOfSpeech = "verb", shortDefinition = "Apply mechanical stress or condition"),
        GeneralTermEntity(termEn = "internal", termAr = "داخلي / باطني", partOfSpeech = "adj", shortDefinition = "Located inside the body or root canal"),
        GeneralTermEntity(termEn = "external", termAr = "خارجي / سطحي", partOfSpeech = "adj", shortDefinition = "Belonging to outer surface or structure"),
        GeneralTermEntity(termEn = "investigate", termAr = "يستقصي / يفحص بدقة", partOfSpeech = "verb", shortDefinition = "Inquire systematically into facts or disease"),
        GeneralTermEntity(termEn = "obvious", termAr = "واضح / جلي", partOfSpeech = "adj", shortDefinition = "Easily perceived or understood; self-evident"),
        GeneralTermEntity(termEn = "occupy", termAr = "يشغل (حيزاً تشريحياً)", partOfSpeech = "verb", shortDefinition = "Fill an anatomical or biological space"),
        GeneralTermEntity(termEn = "option", termAr = "خيار / بديل", partOfSpeech = "noun", shortDefinition = "A thing that may be chosen"),
        GeneralTermEntity(termEn = "output", termAr = "نتاج / مخرجات (مثل النتاج القلبي)", partOfSpeech = "noun", shortDefinition = "The amount produced by an organ or system"),
        GeneralTermEntity(termEn = "overall", termAr = "شامل / إجمالي", partOfSpeech = "adj", shortDefinition = "Taking everything into account; prognosis"),
        GeneralTermEntity(termEn = "parallel", termAr = "موازٍ / توازي", partOfSpeech = "adj", shortDefinition = "Side by side continuously"),
        GeneralTermEntity(termEn = "phase", termAr = "طور / مرحلة", partOfSpeech = "noun", shortDefinition = "A distinct stage in clinical treatment"),
        GeneralTermEntity(termEn = "predict", termAr = "يتنبأ بـ / يتوقع", partOfSpeech = "verb", shortDefinition = "Estimate what will happen in the future"),
        GeneralTermEntity(termEn = "principal", termAr = "رئيسي / أساسي", partOfSpeech = "adj", shortDefinition = "First in order of importance; main"),
        GeneralTermEntity(termEn = "prior", termAr = "سابق / مسبق", partOfSpeech = "adj", shortDefinition = "Existing before in time or order"),
        GeneralTermEntity(termEn = "professional", termAr = "مهني / محترف", partOfSpeech = "adj", shortDefinition = "Relating to a medical profession"),
        GeneralTermEntity(termEn = "promote", termAr = "يعزز / يحفّز", partOfSpeech = "verb", shortDefinition = "Actively encourage tissue healing"),
        GeneralTermEntity(termEn = "regimen", termAr = "نظام علاجي / خطة دوائية", partOfSpeech = "noun", shortDefinition = "Prescribed course of medical treatment"),
        GeneralTermEntity(termEn = "resolve", termAr = "يزول / يشفى / يحل", partOfSpeech = "verb", shortDefinition = "Subside without scarring (inflammation)"),
        GeneralTermEntity(termEn = "retain", termAr = "يستبقي / يحتفظ بـ", partOfSpeech = "verb", shortDefinition = "Preserve a natural tooth or prosthesis"),
        GeneralTermEntity(termEn = "series", termAr = "سلسلة / مجموعة متتالية", partOfSpeech = "noun", shortDefinition = "A number of similar cases or events"),
        GeneralTermEntity(termEn = "statistic", termAr = "إحصائية / رقم إحصائي", partOfSpeech = "noun", shortDefinition = "A numerical fact from a study"),
        GeneralTermEntity(termEn = "status", termAr = "حالة / وضع راهن", partOfSpeech = "noun", shortDefinition = "State of affairs (e.g. periodontal status)"),
        GeneralTermEntity(termEn = "stress", termAr = "إجهاد / ضغط ميكانيكي أو نفسي", partOfSpeech = "noun", shortDefinition = "Force per unit area within materials"),
        GeneralTermEntity(termEn = "subsequent", termAr = "لاحق / تالٍ", partOfSpeech = "adj", shortDefinition = "Coming after something in time; following"),
        GeneralTermEntity(termEn = "undertake", termAr = "يتعهد بـ / يُجري", partOfSpeech = "verb", shortDefinition = "Commit oneself to and begin an intervention")
    )

    // =========================================================================
    // CHUNK 8: AWL Sublist 5-7 Essentials
    // =========================================================================
    val AWL_SUBLIST_5: List<GeneralTermEntity> = listOf(
        GeneralTermEntity(termEn = "academic", termAr = "أكاديمي / جامعي", partOfSpeech = "adj", shortDefinition = "Relating to education and scholarship"),
        GeneralTermEntity(termEn = "adjust", termAr = "يعدّل / يضبط", partOfSpeech = "verb", shortDefinition = "Alter slightly for proper fit"),
        GeneralTermEntity(termEn = "alter", termAr = "يغير / يبدل", partOfSpeech = "verb", shortDefinition = "Change in character or composition"),
        GeneralTermEntity(termEn = "aware", termAr = "واعٍ / مدرك لـ", partOfSpeech = "adj", shortDefinition = "Having knowledge of a situation or fact"),
        GeneralTermEntity(termEn = "capacity", termAr = "سعة / قدرة استيعابية", partOfSpeech = "noun", shortDefinition = "Maximum amount something can hold; ability"),
        GeneralTermEntity(termEn = "challenge", termAr = "تحدٍ / يطعن في", partOfSpeech = "noun", shortDefinition = "A difficult clinical task testing skill"),
        GeneralTermEntity(termEn = "compound", termAr = "مركب كيميائي / مضاعف", partOfSpeech = "noun", shortDefinition = "Substance of two or more elements; fracture"),
        GeneralTermEntity(termEn = "conflict", termAr = "تعارض / تضارب", partOfSpeech = "noun", shortDefinition = "Incompatibility between opinions or principles"),
        GeneralTermEntity(termEn = "consult", termAr = "يستشير / يراجع طبيباً", partOfSpeech = "verb", shortDefinition = "Seek advice from an expert doctor"),
        GeneralTermEntity(termEn = "decline", termAr = "يتراجع / ينخفض / يرفض", partOfSpeech = "verb", shortDefinition = "Diminish in strength; deteriorate"),
        GeneralTermEntity(termEn = "discrete", termAr = "منفصل / متمايز", partOfSpeech = "adj", shortDefinition = "Individually separate and distinct"),
        GeneralTermEntity(termEn = "enable", termAr = "يمكّن / يتيح", partOfSpeech = "verb", shortDefinition = "Provide the means to do something"),
        GeneralTermEntity(termEn = "entity", termAr = "كيان / وحدة مستقلة", partOfSpeech = "noun", shortDefinition = "A thing with distinct existence (disease entity)"),
        GeneralTermEntity(termEn = "equivalent", termAr = "مكافئ / معادل", partOfSpeech = "adj", shortDefinition = "Equal in value, function, or effect"),
        GeneralTermEntity(termEn = "evolve", termAr = "يتطور / ينشأ تدريجياً", partOfSpeech = "verb", shortDefinition = "Develop gradually from simpler forms"),
        GeneralTermEntity(termEn = "expand", termAr = "يتمدد / يوسع", partOfSpeech = "verb", shortDefinition = "Make or become larger; palatal expansion"),
        GeneralTermEntity(termEn = "expose", termAr = "يكشف / يعرّض لـ", partOfSpeech = "verb", shortDefinition = "Make visible; pulp exposure"),
        GeneralTermEntity(termEn = "fundamental", termAr = "أساسي / جوهري", partOfSpeech = "adj", shortDefinition = "Forming a necessary base or core"),
        GeneralTermEntity(termEn = "generate", termAr = "يولّد / يُنتج", partOfSpeech = "verb", shortDefinition = "Produce heat, cells, or impulses"),
        GeneralTermEntity(termEn = "margin", termAr = "حافة / هامش جراحي أو ترميمي", partOfSpeech = "noun", shortDefinition = "Edge of restoration or surgical border"),
        GeneralTermEntity(termEn = "marginal", termAr = "حافي / هامشي (مثل اللثة الحافية)", partOfSpeech = "adj", shortDefinition = "Situated at the edge; marginal gingiva"),
        GeneralTermEntity(termEn = "mental", termAr = "عقلي / ذقني (تشريحياً)", partOfSpeech = "adj", shortDefinition = "Relating to mind or chin (mental foramen)"),
        GeneralTermEntity(termEn = "modify", termAr = "يعدّل / يغيّر جزئياً", partOfSpeech = "verb", shortDefinition = "Make minor changes to something"),
        GeneralTermEntity(termEn = "monitor", termAr = "يراقب / يرصد / جهاز مراقبة", partOfSpeech = "verb", shortDefinition = "Observe progress over time"),
        GeneralTermEntity(termEn = "network", termAr = "شبكة (أوعية أو أعصاب)", partOfSpeech = "noun", shortDefinition = "Plexus of vessels or nerves"),
        GeneralTermEntity(termEn = "notion", termAr = "فكرة / تصور ذهني", partOfSpeech = "noun", shortDefinition = "A conception or belief about something"),
        GeneralTermEntity(termEn = "orient", termAr = "يوجّه / يحدد الاتجاه", partOfSpeech = "verb", shortDefinition = "Position relative to anatomy"),
        GeneralTermEntity(termEn = "perspective", termAr = "منظور / وجهة نظر", partOfSpeech = "noun", shortDefinition = "A point of view regarding a subject"),
        GeneralTermEntity(termEn = "precise", termAr = "دقيق / مضبوط", partOfSpeech = "adj", shortDefinition = "Marked by exactness and accuracy"),
        GeneralTermEntity(termEn = "prime", termAr = "رئيسي / أولي / يهيئ", partOfSpeech = "adj", shortDefinition = "Of first importance; apply adhesive primer"),
        GeneralTermEntity(termEn = "pursue", termAr = "يتابع / يسعى وراء", partOfSpeech = "verb", shortDefinition = "Continue along an investigation"),
        GeneralTermEntity(termEn = "ratio", termAr = "نسبة بين مقدارين", partOfSpeech = "noun", shortDefinition = "Quantitative relation between amounts"),
        GeneralTermEntity(termEn = "reject", termAr = "يرفض / يلفظ (مثل رفض الطعم)", partOfSpeech = "verb", shortDefinition = "Immune attack on transplanted tissue"),
        GeneralTermEntity(termEn = "stable", termAr = "مستقر / ثابت", partOfSpeech = "adj", shortDefinition = "Firmly established; hemodynamically stable"),
        GeneralTermEntity(termEn = "substitute", termAr = "بديل / يستبدل", partOfSpeech = "noun", shortDefinition = "Serving in place of another; bone graft"),
        GeneralTermEntity(termEn = "sustain", termAr = "يتحمل / يحافظ على / يستمر", partOfSpeech = "verb", shortDefinition = "Support physically; undergo injury"),
        GeneralTermEntity(termEn = "target", termAr = "هدف / مستهدف / يستهدف", partOfSpeech = "noun", shortDefinition = "Objective or cellular binding target"),
        GeneralTermEntity(termEn = "trend", termAr = "منحى / اتجاه عام", partOfSpeech = "noun", shortDefinition = "General direction in which data develops"),
        GeneralTermEntity(termEn = "version", termAr = "نسخة / إصدار", partOfSpeech = "noun", shortDefinition = "A specific form differing from others")
    )

    // =========================================================================
    // CHUNK 9: AWL Sublist 6-10 Specialized Vocabulary
    // =========================================================================
    val AWL_SUBLIST_6_TO_10: List<GeneralTermEntity> = listOf(
        GeneralTermEntity(termEn = "abstract", termAr = "ملخص البحث / مجرد", partOfSpeech = "noun", shortDefinition = "Summary of a research article"),
        GeneralTermEntity(termEn = "accurate", termAr = "دقيق / صحيح تماماً", partOfSpeech = "adj", shortDefinition = "Correct in all clinical details"),
        GeneralTermEntity(termEn = "acknowledge", termAr = "يعترف بـ / يقر بـ", partOfSpeech = "verb", shortDefinition = "Accept the truth or existence of"),
        GeneralTermEntity(termEn = "aggregate", termAr = "تجمع / ركام / يتجمع", partOfSpeech = "noun", shortDefinition = "Combined whole; platelet aggregate"),
        GeneralTermEntity(termEn = "attach", termAr = "يربط / يلتصق بـ", partOfSpeech = "verb", shortDefinition = "Fasten to; periodontal attachment"),
        GeneralTermEntity(termEn = "capable", termAr = "قادر على / ذو كفاءة", partOfSpeech = "adj", shortDefinition = "Having the ability to perform a task"),
        GeneralTermEntity(termEn = "cite", termAr = "يستشهد بـ / يذكر مرجعاً", partOfSpeech = "verb", shortDefinition = "Quote reference as evidence"),
        GeneralTermEntity(termEn = "cooperate", termAr = "يتعاون / يتآزر", partOfSpeech = "verb", shortDefinition = "Work jointly toward an end"),
        GeneralTermEntity(termEn = "display", termAr = "يعرض / يُظهر", partOfSpeech = "verb", shortDefinition = "Show prominent exhibition or symptom"),
        GeneralTermEntity(termEn = "diverse", termAr = "متنوع / متباين", partOfSpeech = "adj", shortDefinition = "Showing great variety"),
        GeneralTermEntity(termEn = "domain", termAr = "نطاق / مجال تخصصي", partOfSpeech = "noun", shortDefinition = "A sphere of activity or discipline"),
        GeneralTermEntity(termEn = "enhance", termAr = "يعزز / يحسّن", partOfSpeech = "verb", shortDefinition = "Improve the quality or efficacy of"),
        GeneralTermEntity(termEn = "exceed", termAr = "يتجاوز / يتعدى الحد", partOfSpeech = "verb", shortDefinition = "Be greater than a threshold"),
        GeneralTermEntity(termEn = "explicit", termAr = "صريح / واضح لا لبس فيه", partOfSpeech = "adj", shortDefinition = "Stated clearly leaving no doubt"),
        GeneralTermEntity(termEn = "flexible", termAr = "مرن / قابل للتكيف", partOfSpeech = "adj", shortDefinition = "Capable of bending without breaking"),
        GeneralTermEntity(termEn = "inhibit", termAr = "يثبط / يكبح", partOfSpeech = "verb", shortDefinition = "Slow down or stop an enzymatic process"),
        GeneralTermEntity(termEn = "initiate", termAr = "يبدأ / يطلق", partOfSpeech = "verb", shortDefinition = "Cause a biological process to begin"),
        GeneralTermEntity(termEn = "interval", termAr = "فاصل زمني / فترة بينية", partOfSpeech = "noun", shortDefinition = "Elapsed time between appointments"),
        GeneralTermEntity(termEn = "lecture", termAr = "محاضرة علمية", partOfSpeech = "noun", shortDefinition = "Educational talk to an audience"),
        GeneralTermEntity(termEn = "minimum", termAr = "الحد الأدنى", partOfSpeech = "noun", shortDefinition = "The least quantity possible"),
        GeneralTermEntity(termEn = "neutral", termAr = "متعادل (كيميائياً) / محايد", partOfSpeech = "adj", shortDefinition = "Having pH around 7"),
        GeneralTermEntity(termEn = "precede", termAr = "يسبق / يتقدم على", partOfSpeech = "verb", shortDefinition = "Come before in time or order"),
        GeneralTermEntity(termEn = "rational", termAr = "عقلاني / منطقي", partOfSpeech = "adj", shortDefinition = "Based on reason and logic"),
        GeneralTermEntity(termEn = "recover", termAr = "يتعافى / يسترد", partOfSpeech = "verb", shortDefinition = "Return to normal health after surgery"),
        GeneralTermEntity(termEn = "reveal", termAr = "يكشف عن / يُظهر", partOfSpeech = "verb", shortDefinition = "Disclose previously hidden pathology on X-ray"),
        GeneralTermEntity(termEn = "scope", termAr = "نطاق / مجال / منظار", partOfSpeech = "noun", shortDefinition = "The extent of subject matter"),
        GeneralTermEntity(termEn = "trace", termAr = "أثر ضئيل / يتتبع", partOfSpeech = "noun", shortDefinition = "A minute measurable quantity"),
        GeneralTermEntity(termEn = "transform", termAr = "يتحول / يغيّر طبيعة", partOfSpeech = "verb", shortDefinition = "Make a dramatic change in tissue form"),
        GeneralTermEntity(termEn = "transport", termAr = "ينقل / نقل خلوي", partOfSpeech = "verb", shortDefinition = "Cellular membrane transport of solutes"),
        GeneralTermEntity(termEn = "utilize", termAr = "يستخدم / يوظف", partOfSpeech = "verb", shortDefinition = "Make practical and effective use of"),
        GeneralTermEntity(termEn = "adapt", termAr = "يتكيف / يتلاءم", partOfSpeech = "verb", shortDefinition = "Biological adaptation to new loads"),
        GeneralTermEntity(termEn = "confirm", termAr = "يؤكد / يثبت تشخيصاً", partOfSpeech = "verb", shortDefinition = "Diagnostic verification"),
        GeneralTermEntity(termEn = "contrary", termAr = "على النقيض / معاكس", partOfSpeech = "adj", shortDefinition = "Opposite in nature or direction"),
        GeneralTermEntity(termEn = "convert", termAr = "يحوّل / ينقلب إلى", partOfSpeech = "verb", shortDefinition = "Change from one state to another"),
        GeneralTermEntity(termEn = "eliminate", termAr = "يقضي على / يستأصل تماماً", partOfSpeech = "verb", shortDefinition = "Eradicate infection completely"),
        GeneralTermEntity(termEn = "empirical", termAr = "تجريبي / مبني على الملاحظة", partOfSpeech = "adj", shortDefinition = "Verifiable by observation or experiment"),
        GeneralTermEntity(termEn = "extract", termAr = "يخلع (سناً) / يستخلص", partOfSpeech = "verb", shortDefinition = "Remove tooth from socket in bone"),
        GeneralTermEntity(termEn = "extraction", termAr = "خلع السن / استخلاص", partOfSpeech = "noun", shortDefinition = "Removal of a tooth from the jaw"),
        GeneralTermEntity(termEn = "insert", termAr = "يُدرج / يغرس / يُدخل", partOfSpeech = "verb", shortDefinition = "Place or fit into something; muscle insertion"),
        GeneralTermEntity(termEn = "intervene", termAr = "يتدخل سريرياً / جراحياً", partOfSpeech = "verb", shortDefinition = "Action taken to improve a condition"),
        GeneralTermEntity(termEn = "isolate", termAr = "يعزل (حقل العمل) / يعزل جرثومة", partOfSpeech = "verb", shortDefinition = "Rubber dam isolation of tooth"),
        GeneralTermEntity(termEn = "priority", termAr = "أولوية / أسبقية", partOfSpeech = "noun", shortDefinition = "Regarded as more urgent or important"),
        GeneralTermEntity(termEn = "release", termAr = "يحرر / يطلق مادة", partOfSpeech = "verb", shortDefinition = "Discharge fluoride or medication"),
        GeneralTermEntity(termEn = "reversible", termAr = "عكوس / قابل للشفاء", partOfSpeech = "adj", shortDefinition = "Able to heal back to normal (reversible pulpitis)"),
        GeneralTermEntity(termEn = "irreversible", termAr = "غير عكوس / دائم", partOfSpeech = "adj", shortDefinition = "Permanent pathology (irreversible pulpitis)"),
        GeneralTermEntity(termEn = "simulate", termAr = "يحاكي / يماثل", partOfSpeech = "verb", shortDefinition = "Imitate conditions in lab bench test"),
        GeneralTermEntity(termEn = "survive", termAr = "ينجو / يصمد (مثل بقاء السن)", partOfSpeech = "verb", shortDefinition = "Continue to exist and function (implant survival)"),
        GeneralTermEntity(termEn = "transmit", termAr = "ينقل / يمرر (إشارة أو عدوى)", partOfSpeech = "verb", shortDefinition = "Nerve impulse transmission"),
        GeneralTermEntity(termEn = "visible", termAr = "مرئي / ظاهر للعيان", partOfSpeech = "adj", shortDefinition = "Able to be seen with magnification"),
        GeneralTermEntity(termEn = "detect", termAr = "يكتشف / يرصد سريرياً", partOfSpeech = "verb", shortDefinition = "Discover caries or early lesions"),
        GeneralTermEntity(termEn = "restore", termAr = "يرمم / يعيد بناء السن", partOfSpeech = "verb", shortDefinition = "Rebuild tooth form and function"),
        GeneralTermEntity(termEn = "restoration", termAr = "ترميم / حشوة أو تاج سني", partOfSpeech = "noun", shortDefinition = "A dental filling, crown, or prosthesis"),
        GeneralTermEntity(termEn = "undergo", termAr = "يخضع لـ / يمر بتجربة علاجية", partOfSpeech = "verb", shortDefinition = "Subjected to dental surgery"),
        GeneralTermEntity(termEn = "persist", termAr = "يستمر / يثبت", partOfSpeech = "verb", shortDefinition = "Continue to exist despite therapy")
    )

    /**
     * Aggregation of all sublist chunks.
     * Prevents single method code limit by partitioning bytecode into separate array initializers.
     */
    val ALL_SUBLIST_CHUNKS: List<List<GeneralTermEntity>> by lazy {
        listOf(
            DISCOURSE_CONNECTORS,
            PROCEDURAL_HEADINGS,
            BIOMEDICAL_PROSE_TERMS,
            AWL_SUBLIST_1,
            AWL_SUBLIST_2,
            AWL_SUBLIST_3,
            AWL_SUBLIST_4,
            AWL_SUBLIST_5,
            AWL_SUBLIST_6_TO_10
        )
    }

    /**
     * Backward-compatible lazy flattened list containing all academic and discourse terms.
     */
    val DEFAULT_GENERAL_TERMS: List<GeneralTermEntity> by lazy {
        ALL_SUBLIST_CHUNKS.flatten()
    }
}
