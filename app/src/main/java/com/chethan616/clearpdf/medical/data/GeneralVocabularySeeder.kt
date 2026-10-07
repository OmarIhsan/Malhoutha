package com.chethan616.clearpdf.medical.data

/**
 * Built-in autonomous seeder for general academic, connective, and descriptive vocabulary.
 * Populates high-frequency lecture note transitions and academic prose terms into the isolated
 * general_terms table without touching clinical lexicon tables.
 */
object GeneralVocabularySeeder {

    fun seedDefaultVocabulary(dao: GeneralVocabularyDao) {
        // Relies on @Insert(onConflict = OnConflictStrategy.IGNORE)
        dao.insertTerms(DEFAULT_GENERAL_TERMS)
    }

    val DEFAULT_GENERAL_TERMS: List<GeneralTermEntity> = listOf(
        // --- Connectors & Adverbs ---
        GeneralTermEntity(
            termEn = "furthermore",
            termAr = "علاوة على ذلك",
            partOfSpeech = "adverb",
            shortDefinition = "In addition; moreover; besides"
        ),
        GeneralTermEntity(
            termEn = "moreover",
            termAr = "فضلاً عن ذلك",
            partOfSpeech = "adverb",
            shortDefinition = "As a further matter; besides; in addition"
        ),
        GeneralTermEntity(
            termEn = "consequently",
            termAr = "وبالتالي / نتيجة لذلك",
            partOfSpeech = "adverb",
            shortDefinition = "As a result or effect; therefore"
        ),
        GeneralTermEntity(
            termEn = "subsequently",
            termAr = "في وقت لاحق / لاحقاً",
            partOfSpeech = "adverb",
            shortDefinition = "After a particular event has happened; afterward"
        ),
        GeneralTermEntity(
            termEn = "predominantly",
            termAr = "في الغالب / بشكل سائد",
            partOfSpeech = "adverb",
            shortDefinition = "Mainly; for the most part; primarily"
        ),
        GeneralTermEntity(
            termEn = "markedly",
            termAr = "بشكل ملحوظ / جلي",
            partOfSpeech = "adverb",
            shortDefinition = "To an extent which is clearly noticeable; significantly"
        ),
        GeneralTermEntity(
            termEn = "whereas",
            termAr = "في حين أن / بينما",
            partOfSpeech = "conjunction",
            shortDefinition = "In contrast or comparison with the fact that"
        ),
        GeneralTermEntity(
            termEn = "accordingly",
            termAr = "وفقاً لذلك / بناءً عليه",
            partOfSpeech = "adverb",
            shortDefinition = "In a way that is appropriate to the particular circumstances"
        ),
        GeneralTermEntity(
            termEn = "conversely",
            termAr = "على العكس من ذلك",
            partOfSpeech = "adverb",
            shortDefinition = "Introducing a statement or idea which reverses one that has just been made"
        ),
        GeneralTermEntity(
            termEn = "nevertheless",
            termAr = "مع ذلك / بالرغم من ذلك",
            partOfSpeech = "adverb",
            shortDefinition = "In spite of that; notwithstanding; all the same"
        ),
        GeneralTermEntity(
            termEn = "nonetheless",
            termAr = "ومع ذلك / على الرغم من ذلك",
            partOfSpeech = "adverb",
            shortDefinition = "In spite of what has just been mentioned"
        ),
        GeneralTermEntity(
            termEn = "notably",
            termAr = "لا سيما / بشكل خاص",
            partOfSpeech = "adverb",
            shortDefinition = "Especially; in particular; worthy of attention"
        ),
        GeneralTermEntity(
            termEn = "specifically",
            termAr = "تحديداً / على وجه التحديد",
            partOfSpeech = "adverb",
            shortDefinition = "In a specific manner; clearly and explicitly"
        ),
        GeneralTermEntity(
            termEn = "simultaneously",
            termAr = "في آن واحد / بالتزامن",
            partOfSpeech = "adverb",
            shortDefinition = "At the same time; concurrently"
        ),
        GeneralTermEntity(
            termEn = "initially",
            termAr = "في البداية / مبدئياً",
            partOfSpeech = "adverb",
            shortDefinition = "At the beginning; originally"
        ),
        GeneralTermEntity(
            termEn = "primarily",
            termAr = "أساساً / في المقام الأول",
            partOfSpeech = "adverb",
            shortDefinition = "For the most part; mainly; chiefly"
        ),
        GeneralTermEntity(
            termEn = "significantly",
            termAr = "بشكل كبير / ملحوظ",
            partOfSpeech = "adverb",
            shortDefinition = "In a sufficiently great or important way"
        ),
        GeneralTermEntity(
            termEn = "frequently",
            termAr = "تكراراً / في كثير من الأحيان",
            partOfSpeech = "adverb",
            shortDefinition = "Regularly or habitually; often"
        ),
        GeneralTermEntity(
            termEn = "rarely",
            termAr = "نادراً",
            partOfSpeech = "adverb",
            shortDefinition = "Not often; seldom"
        ),
        GeneralTermEntity(
            termEn = "concomitantly",
            termAr = "بالتزامن / بالترافق",
            partOfSpeech = "adverb",
            shortDefinition = "At the same time as something else; simultaneously"
        ),

        // --- Descriptive & Comparative ---
        GeneralTermEntity(
            termEn = "adjacent",
            termAr = "مجاور / ملاصق",
            partOfSpeech = "adjective",
            shortDefinition = "Next to or adjoining something else"
        ),
        GeneralTermEntity(
            termEn = "underlying",
            termAr = "أساسي / باطني / كامن",
            partOfSpeech = "adjective",
            shortDefinition = "Situated beneath; fundamental or root cause"
        ),
        GeneralTermEntity(
            termEn = "prevalent",
            termAr = "شائع / منتشر",
            partOfSpeech = "adjective",
            shortDefinition = "Widespread in a particular area or at a particular time"
        ),
        GeneralTermEntity(
            termEn = "prevalence",
            termAr = "انتشار / شيوع",
            partOfSpeech = "noun",
            shortDefinition = "The fact or condition of being widespread or prevalent"
        ),
        GeneralTermEntity(
            termEn = "bilateral",
            termAr = "ثنائي الجانب",
            partOfSpeech = "adjective",
            shortDefinition = "Having or relating to two sides; affecting both sides"
        ),
        GeneralTermEntity(
            termEn = "bilaterally",
            termAr = "على كلا الجانبين",
            partOfSpeech = "adverb",
            shortDefinition = "In a manner affecting or involving both sides"
        ),
        GeneralTermEntity(
            termEn = "unilateral",
            termAr = "أحادي الجانب",
            partOfSpeech = "adjective",
            shortDefinition = "Performed by or affecting only one person, group, or side"
        ),
        GeneralTermEntity(
            termEn = "unilaterally",
            termAr = "من جانب واحد",
            partOfSpeech = "adverb",
            shortDefinition = "Affecting or occurring on only one side"
        ),
        GeneralTermEntity(
            termEn = "superficial",
            termAr = "سطحي",
            partOfSpeech = "adjective",
            shortDefinition = "Existing or occurring at or on the surface"
        ),
        GeneralTermEntity(
            termEn = "superficially",
            termAr = "سطحياً",
            partOfSpeech = "adverb",
            shortDefinition = "On the surface; as appears from the surface"
        ),
        GeneralTermEntity(
            termEn = "deep-seated",
            termAr = "عميق / متجذر",
            partOfSpeech = "adjective",
            shortDefinition = "Firmly established at a deep level; situated far below"
        ),
        GeneralTermEntity(
            termEn = "deep seated",
            termAr = "عميق / متجذر",
            partOfSpeech = "adjective",
            shortDefinition = "Firmly established at a deep level; situated far below"
        ),
        GeneralTermEntity(
            termEn = "susceptible",
            termAr = "عرضة لـ / قابل للتأثر",
            partOfSpeech = "adjective",
            shortDefinition = "Likely or liable to be influenced or harmed by a particular thing"
        ),
        GeneralTermEntity(
            termEn = "susceptibility",
            termAr = "قابلية / حساسية لـ",
            partOfSpeech = "noun",
            shortDefinition = "The state or fact of being likely or liable to be influenced"
        ),
        GeneralTermEntity(
            termEn = "compromise",
            termAr = "يُعرض للخطر / يضر",
            partOfSpeech = "verb",
            shortDefinition = "Weaken, endanger, or impair"
        ),
        GeneralTermEntity(
            termEn = "compromised",
            termAr = "معرض للخطر / متضرر",
            partOfSpeech = "adjective",
            shortDefinition = "Made vulnerable, impaired, or functioning suboptimally"
        ),
        GeneralTermEntity(
            termEn = "intermittent",
            termAr = "متقطع",
            partOfSpeech = "adjective",
            shortDefinition = "Occurring at irregular intervals; not continuous or steady"
        ),
        GeneralTermEntity(
            termEn = "intermittently",
            termAr = "بشكل متقطع",
            partOfSpeech = "adverb",
            shortDefinition = "At irregular intervals; occasionally"
        ),
        GeneralTermEntity(
            termEn = "persistent",
            termAr = "مستمر / دائم",
            partOfSpeech = "adjective",
            shortDefinition = "Continuing firmly or obstinately; lasting or prolonged"
        ),
        GeneralTermEntity(
            termEn = "persistently",
            termAr = "باستمرار / بشكل متواصل",
            partOfSpeech = "adverb",
            shortDefinition = "In a persistent or enduring manner"
        ),
        GeneralTermEntity(
            termEn = "persistence",
            termAr = "استمرار / ديمومة",
            partOfSpeech = "noun",
            shortDefinition = "The continuation of an effect after its cause is removed"
        ),
        GeneralTermEntity(
            termEn = "predominant",
            termAr = "سائد / غالب",
            partOfSpeech = "adjective",
            shortDefinition = "Present as the strongest or main element"
        ),
        GeneralTermEntity(
            termEn = "marked",
            termAr = "ملحوظ / بارز",
            partOfSpeech = "adjective",
            shortDefinition = "Clearly noticeable; distinct or conspicuous"
        ),
        GeneralTermEntity(
            termEn = "progressive",
            termAr = "تدريجي / متقدم",
            partOfSpeech = "adjective",
            shortDefinition = "Happening or developing gradually or in stages"
        ),
        GeneralTermEntity(
            termEn = "concomitant",
            termAr = "مصاحب / مترافق",
            partOfSpeech = "adjective",
            shortDefinition = "Naturally accompanying or associated"
        ),
        GeneralTermEntity(
            termEn = "adequate",
            termAr = "كافٍ / ملائم",
            partOfSpeech = "adjective",
            shortDefinition = "Satisfactory or acceptable in quality or quantity"
        ),
        GeneralTermEntity(
            termEn = "inadequate",
            termAr = "غير كافٍ / غير ملائم",
            partOfSpeech = "adjective",
            shortDefinition = "Lacking the quality or quantity required; insufficient"
        ),
        GeneralTermEntity(
            termEn = "sufficient",
            termAr = "كافٍ",
            partOfSpeech = "adjective",
            shortDefinition = "Enough; adequate for a given purpose"
        ),
        GeneralTermEntity(
            termEn = "insufficient",
            termAr = "غير كافٍ",
            partOfSpeech = "adjective",
            shortDefinition = "Not enough; inadequate"
        ),
        GeneralTermEntity(
            termEn = "characteristic",
            termAr = "سمة مميزة / مميز",
            partOfSpeech = "adjective/noun",
            shortDefinition = "Typical of a particular person, place, or thing"
        ),
        GeneralTermEntity(
            termEn = "essential",
            termAr = "أساسي / جوهري",
            partOfSpeech = "adjective",
            shortDefinition = "Extremely important or necessary; fundamental"
        ),
        GeneralTermEntity(
            termEn = "associated",
            termAr = "مرتبط / مقترن",
            partOfSpeech = "adjective",
            shortDefinition = "Connected or related in sequence or causation"
        ),
        GeneralTermEntity(
            termEn = "potential",
            termAr = "محتمل / إمكانية",
            partOfSpeech = "adjective/noun",
            shortDefinition = "Having or showing the capacity to develop into something in the future"
        ),
        GeneralTermEntity(
            termEn = "preceding",
            termAr = "سابق",
            partOfSpeech = "adjective",
            shortDefinition = "Existing, occurring, or having been before"
        ),
        GeneralTermEntity(
            termEn = "following",
            termAr = "تالٍ / عقب",
            partOfSpeech = "adjective",
            shortDefinition = "Coming after or as a consequence of"
        ),
        GeneralTermEntity(
            termEn = "extent",
            termAr = "مدى / نطاق",
            partOfSpeech = "noun",
            shortDefinition = "The degree, distance, or scope to which something extends"
        ),
        GeneralTermEntity(
            termEn = "severity",
            termAr = "شدة / وخامة",
            partOfSpeech = "noun",
            shortDefinition = "The condition of being severe, acute, or harsh"
        ),

        // --- Action & Mechanism ---
        GeneralTermEntity(
            termEn = "indicate",
            termAr = "يشير إلى / يدل على",
            partOfSpeech = "verb",
            shortDefinition = "Point out or show; suggest the necessity of"
        ),
        GeneralTermEntity(
            termEn = "indicates",
            termAr = "يشير إلى / يدل على",
            partOfSpeech = "verb",
            shortDefinition = "Points out or shows"
        ),
        GeneralTermEntity(
            termEn = "indicated",
            termAr = "مُشار إليه / موصى به",
            partOfSpeech = "adjective/verb",
            shortDefinition = "Recommended or specified for a particular purpose"
        ),
        GeneralTermEntity(
            termEn = "indicating",
            termAr = "مشيراً إلى / دالاً على",
            partOfSpeech = "verb",
            shortDefinition = "Pointing out, showing, or suggesting"
        ),
        GeneralTermEntity(
            termEn = "indication",
            termAr = "دلالة / داعي الاستعمال",
            partOfSpeech = "noun",
            shortDefinition = "A sign or piece of information that indicates something"
        ),
        GeneralTermEntity(
            termEn = "contraindicated",
            termAr = "مضاد استطباب / غير موصى به",
            partOfSpeech = "adjective",
            shortDefinition = "Inadvisable or forbidden because of risks"
        ),
        GeneralTermEntity(
            termEn = "contraindication",
            termAr = "موانع الاستعمال",
            partOfSpeech = "noun",
            shortDefinition = "A specific situation in which a drug or procedure should not be used"
        ),
        GeneralTermEntity(
            termEn = "exhibit",
            termAr = "يُظهر / يُبدي",
            partOfSpeech = "verb",
            shortDefinition = "Manifest or show clearly; display outward signs"
        ),
        GeneralTermEntity(
            termEn = "exhibits",
            termAr = "يُظهر / يُبدي",
            partOfSpeech = "verb",
            shortDefinition = "Manifests or shows clearly"
        ),
        GeneralTermEntity(
            termEn = "exhibited",
            termAr = "أظهر / أبدى",
            partOfSpeech = "verb",
            shortDefinition = "Manifested or displayed outwardly"
        ),
        GeneralTermEntity(
            termEn = "exhibiting",
            termAr = "مُظهراً / مُبدياً",
            partOfSpeech = "verb",
            shortDefinition = "Manifesting or displaying outwardly"
        ),
        GeneralTermEntity(
            termEn = "manifest",
            termAr = "يتجلى / يظهر",
            partOfSpeech = "verb/adj",
            shortDefinition = "Display or show by one's acts or appearance; obvious"
        ),
        GeneralTermEntity(
            termEn = "manifests",
            termAr = "يتجلى / يظهر",
            partOfSpeech = "verb",
            shortDefinition = "Displays or appears distinctly"
        ),
        GeneralTermEntity(
            termEn = "manifested",
            termAr = "تجلى / ظهر",
            partOfSpeech = "verb",
            shortDefinition = "Became evident or displayed outwardly"
        ),
        GeneralTermEntity(
            termEn = "manifestation",
            termAr = "مظهر / تجلٍ / علامة سريرية",
            partOfSpeech = "noun",
            shortDefinition = "An event, action, or object that clearly shows or embodies something"
        ),
        GeneralTermEntity(
            termEn = "correlate",
            termAr = "يرتبط بـ / يتوافق مع",
            partOfSpeech = "verb",
            shortDefinition = "Have a mutual relationship or connection"
        ),
        GeneralTermEntity(
            termEn = "correlates",
            termAr = "يرتبط بـ / يتوافق مع",
            partOfSpeech = "verb",
            shortDefinition = "Has a mutual relationship or connection"
        ),
        GeneralTermEntity(
            termEn = "correlated",
            termAr = "مرتبط بـ",
            partOfSpeech = "verb/adj",
            shortDefinition = "Having a mutual connection or dependence"
        ),
        GeneralTermEntity(
            termEn = "correlation",
            termAr = "ارتباط / علاقة ترابطية",
            partOfSpeech = "noun",
            shortDefinition = "A mutual relationship or connection between two or more things"
        ),
        GeneralTermEntity(
            termEn = "differentiate",
            termAr = "يُميز / يتفرع",
            partOfSpeech = "verb",
            shortDefinition = "Recognize or ascertain what makes someone or something different"
        ),
        GeneralTermEntity(
            termEn = "differentiates",
            termAr = "يُميز / يفرق بين",
            partOfSpeech = "verb",
            shortDefinition = "Recognizes or identifies differences between things"
        ),
        GeneralTermEntity(
            termEn = "differentiated",
            termAr = "مُتمايز / مُميز",
            partOfSpeech = "verb/adj",
            shortDefinition = "Specialized or distinguished from others"
        ),
        GeneralTermEntity(
            termEn = "differentiation",
            termAr = "تمايز / تفريق",
            partOfSpeech = "noun",
            shortDefinition = "The process of becoming different, specialized, or distinguishable"
        ),
        GeneralTermEntity(
            termEn = "exacerbate",
            termAr = "يفاقم / يزيد من شدة",
            partOfSpeech = "verb",
            shortDefinition = "Make a problem, bad situation, or negative feeling worse"
        ),
        GeneralTermEntity(
            termEn = "exacerbates",
            termAr = "يفاقم / يزيد من شدة",
            partOfSpeech = "verb",
            shortDefinition = "Makes a problem or clinical condition worse"
        ),
        GeneralTermEntity(
            termEn = "exacerbated",
            termAr = "تفاقم / زادت شدته",
            partOfSpeech = "verb/adj",
            shortDefinition = "Made worse or aggravated"
        ),
        GeneralTermEntity(
            termEn = "exacerbation",
            termAr = "تفاقم / نوبة اشتداد",
            partOfSpeech = "noun",
            shortDefinition = "An acute increase in the severity of a disease or its signs and symptoms"
        ),
        GeneralTermEntity(
            termEn = "demonstrate",
            termAr = "يُثبت / يُوضح",
            partOfSpeech = "verb",
            shortDefinition = "Clearly show the existence or truth of something by giving proof"
        ),
        GeneralTermEntity(
            termEn = "demonstrates",
            termAr = "يُثبت / يُوضح",
            partOfSpeech = "verb",
            shortDefinition = "Shows clearly by evidence or illustration"
        ),
        GeneralTermEntity(
            termEn = "demonstrated",
            termAr = "أثبت / أوضح",
            partOfSpeech = "verb/adj",
            shortDefinition = "Shown clearly and conclusively"
        ),
        GeneralTermEntity(
            termEn = "predispose",
            termAr = "يُهيئ لـ / يجعل عرضة لـ",
            partOfSpeech = "verb",
            shortDefinition = "Make someone liable or inclined to a specified attitude or condition"
        ),
        GeneralTermEntity(
            termEn = "predisposed",
            termAr = "مُهيأ لـ / لديه قابلية",
            partOfSpeech = "adjective",
            shortDefinition = "Made susceptible or inclined to a condition"
        ),
        GeneralTermEntity(
            termEn = "predisposition",
            termAr = "استعداد / قابلية مسبقة",
            partOfSpeech = "noun",
            shortDefinition = "A liability or tendency to suffer from a particular condition"
        ),

        // --- Academic Prepositions & Connectors ---
        GeneralTermEntity(
            termEn = "after",
            termAr = "بعد / عقب / في أعقاب",
            partOfSpeech = "preposition/conjunction",
            shortDefinition = "In the time following an event or period"
        ),
        GeneralTermEntity(
            termEn = "before",
            termAr = "قبل / قبيل",
            partOfSpeech = "preposition/conjunction",
            shortDefinition = "During the period of time preceding a particular event"
        ),
        GeneralTermEntity(
            termEn = "during",
            termAr = "أثناء / خلال",
            partOfSpeech = "preposition",
            shortDefinition = "Throughout the course or duration of"
        ),
        GeneralTermEntity(
            termEn = "between",
            termAr = "بين",
            partOfSpeech = "preposition",
            shortDefinition = "In the space, time, or interval separating two entities"
        ),

        // --- Essential Academic & Clinical Verbs & Forms ---
        GeneralTermEntity(
            termEn = "provide",
            termAr = "يُوفّر / يُقدّم / يُزوّد",
            partOfSpeech = "verb",
            shortDefinition = "Make available for use; supply"
        ),
        GeneralTermEntity(
            termEn = "provides",
            termAr = "يُوفّر / يُقدّم / يُزوّد",
            partOfSpeech = "verb",
            shortDefinition = "Makes available for use; supplies"
        ),
        GeneralTermEntity(
            termEn = "provided",
            termAr = "يُوفّر / يُقدّم / يُزوّد",
            partOfSpeech = "verb/adjective",
            shortDefinition = "Made available or supplied; given"
        ),
        GeneralTermEntity(
            termEn = "providing",
            termAr = "يُوفّر / يُقدّم / يُزوّد",
            partOfSpeech = "verb",
            shortDefinition = "Supplying or making available for use"
        ),
        GeneralTermEntity(
            termEn = "repair",
            termAr = "يرمّم / يُصلح / ترميم",
            partOfSpeech = "verb/noun",
            shortDefinition = "Restore damaged tissue or structure to a sound or healthy state"
        ),
        GeneralTermEntity(
            termEn = "repairing",
            termAr = "يرمّم / يُصلح / ترميم",
            partOfSpeech = "verb",
            shortDefinition = "Restoring damaged biological tissue or physical integrity"
        ),
        GeneralTermEntity(
            termEn = "repaired",
            termAr = "يرمّم / يُصلح / ترميم",
            partOfSpeech = "verb/adjective",
            shortDefinition = "Restored to a sound, healthy, or functional state"
        ),
        GeneralTermEntity(
            termEn = "heal",
            termAr = "يلتئم / شفاء",
            partOfSpeech = "verb",
            shortDefinition = "Become sound or healthy again; cause tissue to regenerate"
        ),
        GeneralTermEntity(
            termEn = "healing",
            termAr = "يلتئم / شفاء",
            partOfSpeech = "noun/verb",
            shortDefinition = "The physiological process of restoring health or repairing tissues"
        ),
        GeneralTermEntity(
            termEn = "treat",
            termAr = "يعالج / علاج",
            partOfSpeech = "verb",
            shortDefinition = "Apply medical care, intervention, or surgical management"
        ),
        GeneralTermEntity(
            termEn = "treatment",
            termAr = "يعالج / علاج",
            partOfSpeech = "noun",
            shortDefinition = "Medical or surgical care given to a patient for an illness or injury"
        ),
        GeneralTermEntity(
            termEn = "cause",
            termAr = "يُسبّب / سبب",
            partOfSpeech = "verb/noun",
            shortDefinition = "Make something happen; the etiology or reason for an effect"
        ),
        GeneralTermEntity(
            termEn = "caused",
            termAr = "يُسبّب / سبب",
            partOfSpeech = "verb",
            shortDefinition = "Brought about by a specific factor, pathogen, or event"
        ),
        GeneralTermEntity(
            termEn = "occur",
            termAr = "يحدث / يطرأ",
            partOfSpeech = "verb",
            shortDefinition = "Happen; take place; be found or present in a clinical setting"
        ),
        GeneralTermEntity(
            termEn = "occurring",
            termAr = "يحدث / يطرأ",
            partOfSpeech = "verb",
            shortDefinition = "Happening; taking place; manifesting"
        ),
        GeneralTermEntity(
            termEn = "result",
            termAr = "ينتج عن / نتيجة",
            partOfSpeech = "verb/noun",
            shortDefinition = "A consequence, outcome, or effect of an action or disease process"
        ),
        GeneralTermEntity(
            termEn = "resulting",
            termAr = "ينتج عن / نتيجة",
            partOfSpeech = "verb/adjective",
            shortDefinition = "Occurring or following as a consequence or result"
        ),
        GeneralTermEntity(
            termEn = "require",
            termAr = "يتطلّب / يستلزم",
            partOfSpeech = "verb",
            shortDefinition = "Need for a particular clinical purpose; demand"
        ),
        GeneralTermEntity(
            termEn = "required",
            termAr = "يتطلّب / يستلزم",
            partOfSpeech = "verb/adjective",
            shortDefinition = "Needed, compulsory, or indicated in clinical management"
        ),
        GeneralTermEntity(
            termEn = "allow",
            termAr = "يسمح / يُتيح",
            partOfSpeech = "verb",
            shortDefinition = "Permit; make it possible for a reaction or process to occur"
        ),
        GeneralTermEntity(
            termEn = "prevent",
            termAr = "يمنع / يقي",
            partOfSpeech = "verb",
            shortDefinition = "Keep something undesirable from happening; avert"
        ),
        GeneralTermEntity(
            termEn = "reduce",
            termAr = "يُقلّل / يخفّض",
            partOfSpeech = "verb",
            shortDefinition = "Make smaller or less in amount, degree, or severity"
        ),
        GeneralTermEntity(
            termEn = "increase",
            termAr = "يزيد / يرفع",
            partOfSpeech = "verb/noun",
            shortDefinition = "Become or make greater in size, amount, intensity, or degree"
        ),
        GeneralTermEntity(
            termEn = "observe",
            termAr = "يلاحظ / يرصد",
            partOfSpeech = "verb",
            shortDefinition = "Notice, perceive, or record clinical signs or biological phenomena"
        ),
        GeneralTermEntity(
            termEn = "contain",
            termAr = "يحتوي على",
            partOfSpeech = "verb",
            shortDefinition = "Have or hold within its structure or biological space"
        ),
        GeneralTermEntity(
            termEn = "include",
            termAr = "يتضمّن / يشمل",
            partOfSpeech = "verb",
            shortDefinition = "Comprise or contain as part of a whole"
        ),
        GeneralTermEntity(
            termEn = "associate",
            termAr = "يرتبط بـ / مقترن",
            partOfSpeech = "verb",
            shortDefinition = "Connect with something else in thought, etiology, or sequence"
        )
    )
}
