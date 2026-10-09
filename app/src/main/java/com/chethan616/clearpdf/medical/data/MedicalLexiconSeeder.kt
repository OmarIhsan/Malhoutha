package com.chethan616.clearpdf.medical.data

import com.chethan616.clearpdf.medical.model.MedicalDomain

/**
 * Built-in lexicon seeder providing comprehensive dental, anatomical, pathological,
 * and pharmacological terms with verified English, Arabic, and Latin clinical names.
 * Tailored for dental & medical textbooks, histology, and oral pathology curricula.
 */
object MedicalLexiconSeeder {

    data class SeedConcept(
        val cui: String? = null,
        val domain: MedicalDomain,
        val subspecialty: String? = null,
        val latinName: String? = null,
        val termsEn: List<String>,
        val termsAr: List<String>,
        val defEn: String? = null,
        val defAr: String? = null,
        val source: String
    ) {
        // Convenience constructor for single English & Arabic term
        constructor(
            cui: String? = null,
            domain: MedicalDomain,
            subspecialty: String? = null,
            latinName: String? = null,
            termEn: String,
            termAr: String,
            defEn: String? = null,
            defAr: String? = null,
            source: String
        ) : this(
            cui = cui,
            domain = domain,
            subspecialty = subspecialty,
            latinName = latinName,
            termsEn = listOf(termEn),
            termsAr = listOf(termAr),
            defEn = defEn,
            defAr = defAr,
            source = source
        )
    }

    val DEFAULT_SEED_CONCEPTS: List<SeedConcept> = listOf(
        // --- DENTAL HISTOLOGY, EMBRYOLOGY & ANATOMY ---
        SeedConcept(
            cui = "C0011332",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Substantia adamantina",
            termsEn = listOf("Enamel", "Dental enamel", "Tooth enamel", "Enamels"),
            termsAr = listOf("ميناء الأسنان", "الميناء", "ميناء السن"),
            defEn = "The hard, highly mineralized outer protective layer of the anatomical crown of the tooth, formed by ameloblasts and composed primarily of hydroxyapatite crystals.",
            defAr = "النسيج الكلسي الأشد قساوة في جسم الإنسان الذي يغطي تاج السن التشريحي، تفرزه مصورات الميناء ويتكون أساساً من بلورات الهيدروكسي أباتيت.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0002446",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Ameloblastus",
            termsEn = listOf("Ameloblast", "Ameloblasts", "Ameloblastic cell"),
            termsAr = listOf("أرومة الميناء", "أرومات الميناء", "خلايا مصورة للميناء", "خلية مصورة للميناء"),
            defEn = "Specialized columnar epithelial cells of ectodermal origin that differentiate from the inner enamel epithelium to form and mineralize dental enamel.",
            defAr = "خلايا ظهارية عمودية متخصصة من أصل أدمي ظاهر تتمايز من الظهارة المينائية الداخلية لتفرز مصفوفة الميناء وتشرف على تمعدنها.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0011328",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Substantia eburnea",
            termsEn = listOf("Dentin", "Dentine", "Dental dentin"),
            termsAr = listOf("عاج الأسنان", "العاج", "عاج السن"),
            defEn = "The mineralized connective tissue forming the bulk of the tooth structure, located deep to the enamel and cementum and surrounding the pulp cavity.",
            defAr = "النسيج الضام المتمعدن الذي يشكل الكتلة الرئيسية لبنية السن، ويقع أسفل الميناء والملاط محيطاً بحجرة اللب وقنوات الجذور.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0028963",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Odontoblastus",
            termsEn = listOf("Odontoblast", "Odontoblasts", "Odontoblastic cell"),
            termsAr = listOf("أرومة العاج", "أرومات العاج", "خلايا مصورة للعاج", "خلية بانية للعاج"),
            defEn = "Specialized cells of neural crest ectomesenchymal origin lining the outer periphery of the dental pulp that produce and maintain dentin throughout life.",
            defAr = "خلايا أدمية متوسطة متخصصة تصطف على المحيط الخارجي للب السن وتفرز مصفوفة العاج وتستمر في صيانته وتكوينه طوال الحياة.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0011333",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ENDODONTICS",
            latinName = "Pulpa dentis",
            termsEn = listOf("Dental pulp", "Pulp", "Tooth pulp"),
            termsAr = listOf("لب السن", "اللب السني", "لب الأسنان"),
            defEn = "The richly vascularized, innervated loose connective tissue occupying the central pulp cavity of the tooth, responsible for dentin vitality, sensation, and defense.",
            defAr = "النسيج الضام الرخو الوعائي والعصبي الذي يشغل حجرة وقنوات السن، والمسؤول عن تغذية العاج وحيويته وحساسيته ودفاعه المناعي.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0007787",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "PERIODONTICS",
            latinName = "Cementum",
            termsEn = listOf("Cementum", "Dental cementum", "Tooth cementum"),
            termsAr = listOf("ملاط السن", "الملاط", "ملاط الأسنان"),
            defEn = "A specialized calcified avascular substance covering the anatomical root of the tooth, providing attachment for Sharpey's fibers of the periodontal ligament.",
            defAr = "طبقة متكلسة لاوعائية تغطي جذر السن التشريحي، وتثبت ألياف شاربي الخاصة برباط دواعم السن لربط الجذر بالعظم السنخي.",
            source = "PERIODONTOLOGY_CARRANZA"
        ),
        SeedConcept(
            cui = "C0224675",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Prisma adamatinum",
            termsEn = listOf("Enamel rod", "Enamel prism", "Enamel rods", "Enamel prisms"),
            termsAr = listOf("قضيب الميناء", "موشور الميناء", "قضبان الميناء", "مواشير الميناء"),
            defEn = "The primary structural unit of dental enamel, densely packed with millions of hydroxyapatite crystallites running from the dentinoenamel junction to the tooth surface.",
            defAr = "الوحدة البنائية الأساسية للميناء، تتكون من ملايين بلورات الهيدروكسي أباتيت المتراصة الممتدة من الملتقى المينائي العاجي نحو سطح السن الخارجي.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0224676",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Membrana adamantina",
            termsEn = listOf("Nasmyth membrane", "Nasmyth's membrane", "Dental cuticle"),
            termsAr = listOf("غشاء ناسميث", "غشاء ناسميث المينائي", "الغشاء المينائي"),
            defEn = "A delicate organic membrane covering the enamel of a newly erupted tooth, composed of the primary enamel cuticle and remnants of the reduced enamel epithelium.",
            defAr = "غشاء رقيق عضوي يغطي سطح تاج السن البازغ حديثاً، ويتألف من جليدة الميناء الأولية وبقايا ظهارة الميناء الضامرة ويزول بالاحتكاك والمضغ.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0224677",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Cuticula adamantina primaria",
            termsEn = listOf("Primary enamel cuticle", "Primary cuticle"),
            termsAr = listOf("جليدة الميناء الأولية", "الجليدة الأولية للميناء"),
            defEn = "The thin basal lamina-like acellular layer secreted by ameloblasts as their final functional act onto the surface of mature enamel prior to tooth eruption.",
            defAr = "طبقة رقيقة غير خلوية تشبه الصفيحة القاعدية، تفرزها أرومات الميناء كآخر عمل وظيفي لها على سطح الميناء الناضج قبل بزوغ السن.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0224678",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Tubuli dentinales",
            termsEn = listOf("Dentinal tubule", "Dentinal tubules", "Dentine tubule", "Dentine tubules"),
            termsAr = listOf("نبيبات عاجية", "النبيبات العاجية", "نبيب عاجي"),
            defEn = "Microscopic cylindrical canals extending radially through the entire thickness of dentin, housing odontoblastic processes and dentinal fluid.",
            defAr = "أقنية مجهرية أسطوانية تخترق كامل ثخانة العاج شعاعياً من اللب إلى الملتقى المينائي العاجي، وتحتوي على استطالات أرومات العاج وسائل العاج.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0224679",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Striae Retzii",
            termsEn = listOf("Striae of Retzius", "Retzius striae", "Incremental lines of Retzius"),
            termsAr = listOf("خطوط ريتزيوس", "خطوط ريتزيوس التزايدية", "أشرطة ريتزيوس"),
            defEn = "Incremental growth lines visible in microscopic sections of enamel reflecting rhythmic variations in the mineral deposition rate during amelogenesis.",
            defAr = "خطوط نمو تزايدية مجهرية تظهر في مقاطع الميناء تعكس التغيرات الدورية اليومية في وتيرة التمعدن أثناء تكون الميناء.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0224680",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Junctio dentinoenamelis",
            termsEn = listOf("Dentinoenamel junction", "DEJ", "Dentino-enamel junction", "Amelodentinal junction"),
            termsAr = listOf("الملتقى المينائي العاجي", "الحد المينائي العاجي"),
            defEn = "The scalloped, highly compliant anatomical interface between dental enamel and underlying coronal dentin that prevents catastrophic crack propagation.",
            defAr = "الحد الفاصل المتموج بين ميناء السن وعاج التاج، والذي يتمتع بمرونة وظيفية عالية تمنع انتشار التصدعات والكسور في بنية السن.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0224681",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "PERIODONTICS",
            latinName = "Junctio cementoenamelis",
            termsEn = listOf("Cementoenamel junction", "CEJ", "Cervical line"),
            termsAr = listOf("الملتقى الملاطي المينائي", "الخط العنقي للسن", "الملتقى المينائي الملاطي"),
            defEn = "The anatomical border where the enamel covering the crown of the tooth meets the cementum covering the root at the cervical region.",
            defAr = "الحد التشريحي العنقي حيث يلتقي الميناء المغطي للتاج بالملاط المغطي للجذر، ويشكل معلماً هاماً لتقييم انحسار اللثة وفقدان الارتباط.",
            source = "PERIODONTOLOGY_CARRANZA"
        ),
        SeedConcept(
            cui = "C0031086",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "PERIODONTICS",
            latinName = "Ligamentum periodontale",
            termsEn = listOf("Periodontal ligament", "PDL", "Periodontal membrane"),
            termsAr = listOf("رباط دواعم السن", "الرباط السنخي السني", "رباط حول السن"),
            defEn = "The fibrous connective tissue structure surrounding the tooth root, attaching cementum to the surrounding alveolar bone socket and absorbing masticatory shock.",
            defAr = "بنية نسيجية ليفية ضامة تحيط بجذر السن وتربط الملاط بالعظم السنخي، وتعمل كوسادة هيدروليكية لامتصاص قوى المضغ وإدراك الضغط.",
            source = "PERIODONTOLOGY_CARRANZA"
        ),
        SeedConcept(
            cui = "C0002279",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "PERIODONTICS",
            latinName = "Processus alveolaris",
            termsEn = listOf("Alveolar bone", "Alveolar process", "Alveolar ridge"),
            termsAr = listOf("العظم السنخي", "النتوء السنخي", "عظم السنخ"),
            defEn = "The specialized ridge of bone on the maxilla and mandible containing tooth sockets that support dentition.",
            defAr = "الامتداد العظمي المتخصص في الفكين العلوي والسفلي الذي يضم الأسناخ ويثبت جذور الأسنان.",
            source = "TERMINOLOGIA_ANATOMICA"
        ),
        SeedConcept(
            cui = "C0224682",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_ANATOMY",
            latinName = "Corona dentis",
            termsEn = listOf("Crown", "Anatomical crown", "Tooth crown"),
            termsAr = listOf("تاج السن / التاج التشريحي", "تاج السن", "التاج التشريحي", "التاج السني"),
            defEn = "The anatomical portion of a tooth that is covered by enamel, extending from the occlusal or incisal surface to the cementoenamel junction.",
            defAr = "الجزء التشريحي من السن المغطى بالميناء ويمتد من السطح الإطباقي أو القاطع حتى الملتقى الملاطي المينائي.",
            source = "ORAL_ANATOMY_WHEELER"
        ),
        SeedConcept(
            cui = "C0446700",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_ANATOMY",
            latinName = "Corona clinica",
            termsEn = listOf("Clinical crown"),
            termsAr = listOf("التاج السريري"),
            defEn = "The portion of the tooth that is visible and exposed in the oral cavity above the gingival margin.",
            defAr = "الجزء من السن المرئي والبارز في جوف الفم فوق الحافة اللثوية.",
            source = "ORAL_ANATOMY_WHEELER"
        ),
        SeedConcept(
            cui = "C0008940",
            domain = MedicalDomain.GENERAL_CLINICAL,
            subspecialty = "DIAGNOSTICS",
            latinName = "Clinicus",
            termsEn = listOf("Clinical"),
            termsAr = listOf("سريري / إكلينيكي", "سريري", "إكلينيكي"),
            defEn = "Relating to the observation, examination, and treatment of patients rather than theoretical study.",
            defAr = "متعلق بالفحص الطبي العملي وعلاج المرضى مباشرة.",
            source = "DORLAND_MEDICAL"
        ),
        SeedConcept(
            cui = "C0031809",
            domain = MedicalDomain.GENERAL_CLINICAL,
            subspecialty = "DIAGNOSTICS",
            latinName = null,
            termsEn = listOf("Clinical examination", "Clinical exam"),
            termsAr = listOf("فحص سريري / فحص إكلينيكي", "الفحص السريري", "فحص سريري"),
            defEn = "The physical examination of a patient to detect signs of disease, oral pathology, or systemic symptoms.",
            defAr = "الفحص الطبي المباشر للمريض للكشف عن علامات وأعراض المرض أو الآفات الفموية.",
            source = "DORLAND_MEDICAL"
        ),
        SeedConcept(
            cui = "C0681878",
            domain = MedicalDomain.GENERAL_CLINICAL,
            subspecialty = "DIAGNOSTICS",
            latinName = null,
            termsEn = listOf("Clinical significance", "Clinical relevance"),
            termsAr = listOf("أهمية سريرية / دلالة سريرية", "أهمية سريرية", "دلالة سريرية"),
            defEn = "The practical importance or diagnostic relevance of a finding, trial result, or observation in patient care.",
            defAr = "الأهمية العملية أو الدلالة التطبيقية لنتيجة أو تشخيص في رعاية المرضى.",
            source = "DORLAND_MEDICAL"
        ),
        SeedConcept(
            cui = "C1518013",
            domain = MedicalDomain.GENERAL_CLINICAL,
            subspecialty = "GENERAL_CLINICAL",
            latinName = "Margo",
            termsEn = listOf("Margin", "Margins"),
            termsAr = listOf("حافة / هامش", "حافة", "هامش"),
            defEn = "The border, edge, or boundary of an anatomical structure, wound, lesion, or dental restoration.",
            defAr = "الحد أو الحافة الخارجية المحيطة ببنية تشريحية أو جرح أو آفة أو ترميم سني.",
            source = "DORLAND_MEDICAL"
        ),
        SeedConcept(
            cui = "C0005521",
            domain = MedicalDomain.GENERAL_CLINICAL,
            subspecialty = "GENERAL_CLINICAL",
            latinName = "Biologicus",
            termsEn = listOf("biologic", "biological", "biology"),
            termsAr = listOf("حيوي", "بيولوجي", "علم الأحياء"),
            defEn = "Relating to biology or living organisms; biological products, processes, or structures.",
            defAr = "متعلق بعلم الأحياء أو الكائنات الحية؛ أو مستحضر / بنية ذات أصل حيوي.",
            source = "WHO_UMD_GENERAL"
        ),
        SeedConcept(
            cui = "C0225686",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "PERIODONTICS",
            latinName = "Margo gingivalis",
            termsEn = listOf("Gingival margin", "Margin of the gingiva", "Marginal gingiva"),
            termsAr = listOf("الحافة اللثوية", "حافة اللثة", "اللثة الحفافية"),
            defEn = "The coronal edge or free border of the gingiva surrounding the cervical portion of the tooth crown.",
            defAr = "الحافة الحرة العلوية للثة المحيطة بالمنطقة العنقية لتاج السن.",
            source = "PERIODONTOLOGY_CARRANZA"
        ),
        SeedConcept(
            cui = "C0205064",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_ANATOMY",
            latinName = "Cervicalis",
            termsEn = listOf("Cervical"),
            termsAr = listOf("عنقي", "عنقي سني"),
            defEn = "Relating to the cervix or neck of a tooth adjacent to the cementoenamel junction.",
            defAr = "متعلق بعنق السن أو المنطقة المجاورة للملتقى المينائي الملاطي (CEJ).",
            source = "ORAL_ANATOMY_WHEELER"
        ),
        SeedConcept(
            cui = "C0224687",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_ANATOMY",
            latinName = "Cuspis dentis",
            termsEn = listOf("Cusp", "Tooth cusp", "Dental cusp", "Cusps"),
            termsAr = listOf("شرفة السن / حدبة السن", "شرفة السن", "حدبة السن", "شرفة"),
            defEn = "An elevation or pointed mound on the occlusal surface of a premolar or molar tooth or the incisal portion of a canine.",
            defAr = "نتوء هرمي أو بارزة على السطح الإطباقي للأضراس والضواحك أو حافة الناب القاطعة.",
            source = "ORAL_ANATOMY_WHEELER"
        ),
        SeedConcept(
            cui = "C0224683",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_ANATOMY",
            latinName = "Radix dentis",
            termsEn = listOf("Root", "Tooth root", "Dental root", "Roots"),
            termsAr = listOf("جذر السن", "الجذر السني", "جذر / أصل", "جذور الأسنان"),
            defEn = "The anatomical portion of the tooth covered by cementum, embedded in the alveolar bone socket, and anchored by the periodontal ligament (with secondary general meaning: root/origin).",
            defAr = "الجزء التشريحي من السن المغطى بالملاط والمنغرس داخل السنخ العظمي والمثبت برباط دواعم السن (وله معنى عام ثانوي: جذر أو أصل الشيء).",
            source = "ORAL_ANATOMY_WHEELER"
        ),
        SeedConcept(
            cui = "C0011342",
            domain = MedicalDomain.PROCEDURE,
            subspecialty = "OPERATIVE_DENTISTRY",
            latinName = "Restauratio dentis",
            termsEn = listOf("Restoration", "Dental restoration", "Tooth restoration", "Restorations"),
            termsAr = listOf("حشوة / ترميم سني", "ترميم سني", "حشوة سنية"),
            defEn = "A material or prosthesis placed to restore the morphological integrity, function, and aesthetics of a defective or damaged tooth.",
            defAr = "مادة سنية أو تعويض يُوضع لإعادة الشكل التشريحي والوظيفة والناحية الجمالية للسن المتضرر.",
            source = "OPERATIVE_DENTISTRY_STURDEVANT"
        ),
        SeedConcept(
            cui = "C0181781",
            domain = MedicalDomain.PROCEDURE,
            subspecialty = "OPERATIVE_DENTISTRY",
            latinName = "Praeparatio cavitatis",
            termsEn = listOf("Preparation", "Cavity preparation", "Tooth preparation"),
            termsAr = listOf("تحضير السن / تحضير الحفرة", "تحضير السن", "تحضير الحفرة السنية"),
            defEn = "The mechanical alteration of a defective tooth to receive a restorative material or crown according to biomechanical principles.",
            defAr = "التعديل الميكانيكي الجراحي لبنية السن المصاب وفق أسس حيوية ميكانيكية لتجهيزه لاستقبال مادة الترميم أو التاج.",
            source = "OPERATIVE_DENTISTRY_STURDEVANT"
        ),
        SeedConcept(
            cui = "C0224684",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ENDODONTICS",
            latinName = "Apex radicis dentis",
            termsEn = listOf("Apex", "Root apex", "Apical tip"),
            termsAr = listOf("ذروة الجذر", "الذروة السنية", "ذروة السن"),
            defEn = "The terminal tip or anatomical end of the tooth root containing the apical foramen through which pulpal neurovascular bundles pass.",
            defAr = "النهاية الطرفية لجذر السن التي تضم الثقبة الذروية وتمر عبرها الحزم العصبية والأوعية الدموية المغذية للب.",
            source = "ENDODONTICS_COHEN"
        ),
        SeedConcept(
            cui = "C0224685",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ENDODONTICS",
            latinName = "Foramen apicale",
            termsEn = listOf("Apical foramen", "Apical foramina", "Root foramen"),
            termsAr = listOf("الثقبة الذروية", "الثقبة القمية", "ثقبة الذروة"),
            defEn = "The microscopic natural opening at the root apex communicating the dental pulp cavity with the periapical periodontal ligament tissues.",
            defAr = "الفتحة الطبيعية المجهرية عند ذروة جذر السن التي تصل اللب السني بالأنسجة المحيطة بالذروة والرباط السنخي.",
            source = "ENDODONTICS_COHEN"
        ),
        SeedConcept(
            cui = "C0224686",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ENDODONTICS",
            latinName = "Canalis radicis dentis",
            termsEn = listOf("Root canal", "Pulp canal", "Root canals"),
            termsAr = listOf("قناة الجذر", "القناة الجذرية", "قنوات الجذور"),
            defEn = "The tubular anatomical space within the tooth root traversing from the pulp chamber to the apical foramen.",
            defAr = "الممر الأنبوبي التشريحي داخل جذر السن الممتد من حجرة اللب حتى الثقبة الذروية.",
            source = "ENDODONTICS_COHEN"
        ),
        SeedConcept(
            cui = "C0224687",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ENDODONTICS",
            latinName = "Cavitas pulparis",
            termsEn = listOf("Pulp chamber", "Coronal pulp chamber", "Pulp cavity"),
            termsAr = listOf("حجرة اللب", "الحجرة اللبية", "التجويف اللبي"),
            defEn = "The central enlarged anatomical cavity within the coronal portion of the tooth housing the coronal dental pulp.",
            defAr = "التجويف المركزي المتسع داخل تاج السن الذي يحتوي على اللب التاجي.",
            source = "ENDODONTICS_COHEN"
        ),
        SeedConcept(
            cui = "C0224688",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Processus Tomes",
            termsEn = listOf("Tomes process", "Tomes' process", "Tomes processes"),
            termsAr = listOf("استطالة تومز", "بروز تومز", "استطالات تومز"),
            defEn = "The specialized conical projection at the apical secretory end of an ameloblast responsible for orienting hydroxyapatite crystals into distinct enamel rods.",
            defAr = "الاستطالة المخروطية المتخصصة في النهاية الإفرازية لأرومة الميناء المسؤولة عن توجيه البلورات وتشكيل قضبان الميناء.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0011330",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Amelogenesis",
            termsEn = listOf("Amelogenesis", "Enamel formation", "Amelogenic"),
            termsAr = listOf("تكون الميناء", "تشكل الميناء"),
            defEn = "The complex, tightly regulated biological process of enamel matrix deposition and mineralization by ameloblasts during odontogenesis.",
            defAr = "العملية البيولوجية المعقدة لترسيب ونضج مصفوفة الميناء وتمعدنها بواسطة أرومات الميناء أثناء تشكل الأسنان.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0011331",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Dentinogenesis",
            termsEn = listOf("Dentinogenesis", "Dentin formation"),
            termsAr = listOf("تكون العاج", "تشكل العاج"),
            defEn = "The process of predentin synthesis, apposition, and subsequent biomineralization by odontoblasts during tooth development and in response to injury.",
            defAr = "عملية تصنيع وإفراز طليعة العاج وتمعدنها لاحقاً بواسطة أرومات العاج أثناء نمو السن أو كاستجابة دفاعية للرضح والنخر.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0017565",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "PERIODONTICS",
            latinName = "Gingiva",
            termsEn = listOf("Gingiva", "Gingivae", "Gum", "Gums"),
            termsAr = listOf("اللثة", "النسيج اللثوي"),
            defEn = "The mucosal tissue covering the alveolar processes of the jaws and encircling the necks of the teeth to form a protective seal.",
            defAr = "الغشاء المخاطي المتخصص الذي يغطي العظم السنخي ويحيط بأعناق الأسنان مشكلاً طوقاً واقياً للأنسجة الداعمة العميقة.",
            source = "PERIODONTOLOGY_CARRANZA"
        ),
        SeedConcept(
            cui = "C0039502",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_ANATOMY",
            latinName = "Dens",
            termsEn = listOf("Tooth", "Teeth", "Dentition"),
            termsAr = listOf("السن", "الأسنان"),
            defEn = "Hard, calcified biological structures embedded in the jaws, specialized for mastication, speech phonetics, and facial aesthetics.",
            defAr = "بنى كلسية صلبة مغروسة في الفكين، مخصصة للمضغ والنطق ودعم المظهر الجمالي للوجه.",
            source = "TERMINOLOGIA_ANATOMICA"
        ),
        SeedConcept(
            cui = "C0224689",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Lamina dentalis",
            termsEn = listOf("Dental lamina", "Primary dental lamina"),
            termsAr = listOf("الصفيحة السنية", "الصفيحة السنية الأولية"),
            defEn = "A band of ectodermal epithelial thickening originating from the stomodeum giving rise to tooth buds during early craniofacial embryogenesis.",
            defAr = "شريط ظهاري جنيني يثخن من الظهارة الفموية وينغمد داخل اللحمة المتوسطة ليعطي براعم الأسنان اللبنية والدائمة.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0224690",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Organum adamatinum",
            termsEn = listOf("Enamel organ", "Dental organ"),
            termsAr = listOf("عضو الميناء", "العضو المينائي"),
            defEn = "The epithelial structure of a tooth germ that forms enamel, consisting of outer and inner enamel epithelia, stellate reticulum, and stratum intermedium.",
            defAr = "البنية الظهارية في برعم السن المسؤولة عن تكوين الميناء، وتتألف من الظهارة المينائية الخارجية والداخلية والشبكة النجمية والطبقة المتوسطة.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0224691",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Papilla dentalis",
            termsEn = listOf("Dental papilla", "Dentinal papilla"),
            termsAr = listOf("الحليمة السنية", "حليمة السن"),
            defEn = "The mesenchymal condensation enclosed by the enamel organ that differentiates into odontoblasts to form dentin and dental pulp.",
            defAr = "التكثف الوسنخيمي داخل عضو الميناء الذي تتمايز خلاياه السطحية إلى أرومات العاج لتشكل العاج ولب السن.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0224692",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Folliculus dentalis",
            termsEn = listOf("Dental follicle", "Dental sac"),
            termsAr = listOf("الجريب السني", "الكيس السني"),
            defEn = "The ectomesenchymal envelope surrounding the enamel organ and dental papilla, giving rise to cementum, periodontal ligament, and alveolar bone.",
            defAr = "الغلاف الوسنخيمي المحيط بعضو الميناء والحليمة السنية، والذي ينشأ منه الملاط ورباط دواعم السن والعظم السنخي المحيط.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0224693",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Striae Hunter-Schreger",
            termsEn = listOf("Hunter-Schreger bands", "Hunter Schreger bands", "HSB"),
            termsAr = listOf("حزم هنتر-شريغر", "أشرطة هنتر-شريغر"),
            defEn = "Alternating optical light (parazones) and dark (diazones) bands seen under reflected light in enamel sections, resulting from regular changes in enamel rod orientation.",
            defAr = "حزم بصرية متناوبة من الأشرطة الفاتحة والداكنة تظهر تحت الضوء المنعكس في مقاطع الميناء نتيجة تغيرات دورية في اتجاه قضبان الميناء لتقوية مقاومة الإجهاد.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0224694",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "ORAL_HISTOLOGY",
            latinName = "Perikymata",
            termsEn = listOf("Perikymata", "Perikyma"),
            termsAr = listOf("التموجات السطحية للميناء", "ميازيب الميناء السطحية"),
            defEn = "Wave-like transverse surface grooves on the crown of teeth representing the external clinical manifestations of the striae of Retzius.",
            defAr = "تلافيف وتموجات سطحية مجهرية على سطح تاج السن تمثل الانتهاء السطحي الخارجي لخطوط ريتزيوس التزايدية.",
            source = "ORAL_HISTOLOGY_ALHUWAIZI"
        ),
        SeedConcept(
            cui = "C0224695",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "PREVENTIVE_DENTISTRY",
            latinName = "Pellicula dentis",
            termsEn = listOf("Pellicle", "Acquired pellicle", "Salivary pellicle"),
            termsAr = listOf("جليدة الأسنان المكتسبة", "الجليدة المكتسبة", "الغشاء اللعابي المكتسب"),
            defEn = "An acellular glycoprotein film derived from saliva that rapidly adheres to clean enamel surfaces, providing lubricative protection and serving as the substrate for bacterial colonization.",
            defAr = "غشاء رقيق لاخلوي من بروتينات اللعاب السكرية يترسب بسرعة على سطح الميناء النظيف ليوفر الحماية من التآكل الحمضي ويشكل قاعدة لتثبت الجراثيم.",
            source = "PREVENTIVE_DENTISTRY"
        ),
        SeedConcept(
            cui = "C0024795",
            domain = MedicalDomain.GENERAL_CLINICAL,
            subspecialty = "ORAL_PHYSIOLOGY",
            latinName = "Masticatio",
            termsEn = listOf("Mastication", "Chewing"),
            termsAr = listOf("المضغ", "عملية المضغ"),
            defEn = "The physiological process of crushing and grinding food between the maxillary and mandibular teeth, mediated by the muscles of mastication and TMJ.",
            defAr = "العملية الفيزيولوجية لسحق وطحن الطعام بين أسنان الفكين بمساعدة عضلات المضغ والمفصل الفكي الصدغي واللعاب.",
            source = "ORAL_PHYSIOLOGY"
        ),
        SeedConcept(
            cui = "C0011329",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "DEVELOPMENTAL_PATHOLOGY",
            latinName = "Hypoplasia adamanti",
            termsEn = listOf("Enamel hypoplasia", "Hypoplasia"),
            termsAr = listOf("نقص تنسج الميناء", "نقص تشكل الميناء"),
            defEn = "A quantitative defect of enamel resulting from disruption of ameloblasts during matrix deposition, manifesting as pits, grooves, or complete absence of enamel.",
            defAr = "عيب نمائي كمي في الميناء ينجم عن اضطراب إفراز أرومات الميناء للمصفوفة العضوية، ويظهر سريرياً كنقر أو أخاديد أو نقص في ثخانة الميناء.",
            source = "ORAL_PATHOLOGY_NEVILLE"
        ),
        SeedConcept(
            cui = "C0016202",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "PREVENTIVE_DENTISTRY",
            latinName = "Fluorosis dentium",
            termsEn = listOf("Dental fluorosis", "Fluorosis", "Enamel fluorosis", "Mottled enamel"),
            termsAr = listOf("التسمم الفلوري للأسنان", "الفلور السني", "الميناء المبقع"),
            defEn = "A developmental disturbance of enamel mineralization caused by chronic excessive systemic fluoride intake during tooth development, producing opaque white spots or brown pitting.",
            defAr = "اضطراب نمائي في تمعدن الميناء ناجم عن تناول جرعات زائدة ومزمنة من الفلورايد أثناء تكلس الأسنان، يظهر كبقع بيضاء طباشيرية أو تصبغات بنية ونقر.",
            source = "PREVENTIVE_DENTISTRY"
        ),

        // --- CLINICAL PATHOLOGY, SURGERY & PHARMACOLOGY ---
        SeedConcept(
            cui = "C0002447",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "ORAL_PATHOLOGY",
            latinName = "Ameloblastoma",
            termsEn = listOf("Ameloblastoma", "Ameloblastomas"),
            termsAr = listOf("ورم أرومي مينائي", "الورم الأرومي المينائي"),
            defEn = "A benign but locally aggressive odontogenic epithelial neoplasm most commonly found in the posterior mandible.",
            defAr = "ورم سني المنشأ ظهاري حميد سريرياً لكنه ارتشاحي وعدواني موضعياً، يظهر غالباً في المنطقة الخلفية للفك السفلي.",
            source = "WHO_ODONTOGENIC_TUMORS"
        ),
        SeedConcept(
            cui = "C0028965",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "ORAL_PATHOLOGY",
            latinName = "Keratocystis odontogenica",
            termsEn = listOf("Odontogenic keratocyst", "OKC", "Keratocyst"),
            termsAr = listOf("كيس قرني سني المنشأ", "الكيس القرني السني المنشأ"),
            defEn = "A developmental intraosseous odontogenic cyst lined with parakeratinized stratified squamous epithelium known for high recurrence rates.",
            defAr = "كيس نسيجي سني المنشأ داخل العظم مبطن بنسيج طلائي حرشفي متقرن يتميز بنسبة نكس مرتفعة.",
            source = "WHO_ODONTOGENIC_TUMORS"
        ),
        SeedConcept(
            cui = "C0024477",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "HEAD_AND_NECK_ANATOMY",
            latinName = "Nervus alveolaris inferior",
            termsEn = listOf("Inferior alveolar nerve", "IAN", "Inferior dental nerve"),
            termsAr = listOf("العصب السنخي السفلي", "عصب سنخي سفلي"),
            defEn = "A major branch of the mandibular nerve (V3) traversing the mandibular canal to provide sensation to mandibular teeth and lower lip.",
            defAr = "فرع رئيسي من العصب الفكي السفلي يمر عبر القناة الفكية السفلية لتغذية أسنان الفك السفلي والشفة السفلى حسياً.",
            source = "TERMINOLOGIA_ANATOMICA"
        ),
        SeedConcept(
            cui = "C0023605",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "ORAL_MEDICINE",
            latinName = "Lichen planus oralis",
            termsEn = listOf("Lichen planus", "Oral lichen planus", "OLP"),
            termsAr = listOf("الحزاز المسطح", "حزاز مسطح فموي"),
            defEn = "A chronic T-cell mediated inflammatory disease of the oral mucosa manifesting with reticular Wickham's striae or erosive lesions.",
            defAr = "مرض التهابي مزمن متوسط بالخلايا التائية يصيب الغشاء المخاطي الفموي ويظهر بشكل خطوط ويكهام الشبكية أو آفات تآكلية.",
            source = "MESH"
        ),
        SeedConcept(
            cui = "C0002645",
            domain = MedicalDomain.PHARMACOLOGY,
            subspecialty = "ANTIMICROBIALS",
            latinName = "Amoxicillinum",
            termsEn = listOf("Amoxicillin", "Amoxil"),
            termsAr = listOf("أموكسيسيلين", "أموكسيسللين"),
            defEn = "A moderate-spectrum bactericidal beta-lactam antibiotic used as first-line empirical therapy in odontogenic and respiratory infections.",
            defAr = "مضاد حيوي قاتل للجراثيم من زمرة بيتا-لاكتام واسع/متوسط الطيف، يُستخدم كخط دفاع أول في الإنتانات السنية والتنفسية.",
            source = "WHO_EML"
        ),
        SeedConcept(
            cui = "C0034084",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "ENDODONTICS",
            latinName = "Pulpitis",
            termsEn = listOf("Pulpitis", "Reversible pulpitis", "Irreversible pulpitis"),
            termsAr = listOf("التهاب لب السن", "التهاب اللب", "التهاب اللب السني"),
            defEn = "Inflammation of dental pulp tissue resulting primarily from bacterial microleakage in advanced dental caries.",
            defAr = "التهاب في النسيج اللبي الوعائي العصبي داخل السن ناجم أساساً عن ارتشاح الجراثيم في نخر الأسنان المتقدم.",
            source = "MESH"
        ),
        SeedConcept(
            cui = "C0224673",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "MAXILLOFACIAL_ANATOMY",
            latinName = "Foramen mentale",
            termsEn = listOf("Mental foramen", "Mental foramina"),
            termsAr = listOf("الثقبة الذقنية", "ثقبة ذقنية"),
            defEn = "A bilateral anatomical aperture on the buccal aspect of the mandible transmitting the mental nerve and mental vessels.",
            defAr = "ثقبة تشريحية مزدوجة على السطح الخارجي لعظم الفك السفلي يمر عبرها العصب والأوعية الذقنية.",
            source = "TERMINOLOGIA_ANATOMICA"
        ),
        SeedConcept(
            cui = "C0017574",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "PERIODONTICS",
            latinName = "Gingivitis",
            termsEn = listOf("Gingivitis", "Plaque-induced gingivitis"),
            termsAr = listOf("التهاب اللثة", "التهاب لثوي"),
            defEn = "Reversible plaque-induced inflammation of the marginal gingival tissues characterized by erythema, edema, and bleeding on probing.",
            defAr = "التهاب لثوي عكوس ناجم عن تراكم اللويحة الجرثومية يتميز باحمرار اللثة وتورمها ونزفها عند الفحص بالمسبر.",
            source = "AAP_EFP_CLASSIFICATION"
        ),
        SeedConcept(
            cui = "C0031114",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "PERIODONTICS",
            latinName = "Periodontitis",
            termsEn = listOf("Periodontitis", "Chronic periodontitis", "Aggressive periodontitis"),
            termsAr = listOf("التهاب دواعم السن", "التهاب النسج الداعمة"),
            defEn = "A chronic inflammatory disease caused by microbial dysbiosis leading to irreversible loss of periodontal ligament attachment and alveolar bone resorption.",
            defAr = "مرض التهابي مزمن ناجم عن اختلال التوازن الميكروبي يؤدي إلى فقدان غير عكوس في ارتباط رباط دواعم السن وامتصاص العظم السنخي.",
            source = "AAP_EFP_CLASSIFICATION"
        ),
        SeedConcept(
            cui = "C0011334",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "OPERATIVE_DENTISTRY",
            latinName = "Caries dentium",
            termsEn = listOf("Dental caries", "Caries", "Tooth decay", "Cavity", "Cavities"),
            termsAr = listOf("نخر الأسنان", "تسوس الأسنان", "نخر السن"),
            defEn = "Demineralization of inorganic tooth substance caused by organic acids produced through bacterial fermentation of dietary carbohydrates.",
            defAr = "انحلال وتمعدن البنية غير العضوية للسن بفعل الأحماض العضوية الناتجة عن تخمر الكربوهيدرات الغذائية بواسطة الجراثيم.",
            source = "WHO_UMD"
        ),
        SeedConcept(
            cui = "C0024958",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "MAXILLOFACIAL_ANATOMY",
            latinName = "Sinus maxillaris",
            termsEn = listOf("Maxillary sinus", "Highmore antrum", "Antrum of Highmore"),
            termsAr = listOf("الجيب الفكي", "جيب فكي"),
            defEn = "The largest paranasal pneumatic cavity situated within the body of the maxilla, communicating with the middle nasal meatus.",
            defAr = "أكبر الجيوب الهوائية جانب الأنفية، يقع داخل جسم الفك العلوي ويتصل بالصماخ الأنفي الأوسط.",
            source = "TERMINOLOGIA_ANATOMICA"
        ),
        SeedConcept(
            cui = "C0149758",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "ENDODONTICS",
            latinName = "Abscessus periapicalis",
            termsEn = listOf("Periapical abscess", "Apical abscess", "Dentoalveolar abscess"),
            termsAr = listOf("خراج ذروي", "خراج حول الذروة"),
            defEn = "An acute or chronic suppurative collection of purulent exudate around the apex of a non-vital tooth.",
            defAr = "تجمع قيحي التهابي حاد أو مزمن محيط بذروة جذر سن متموت اللب.",
            source = "MESH"
        ),
        SeedConcept(
            cui = "C0000970",
            domain = MedicalDomain.PHARMACOLOGY,
            subspecialty = "ANALGESICS",
            latinName = "Paracetamolum",
            termsEn = listOf("Paracetamol", "Acetaminophen"),
            termsAr = listOf("باراسيتامول", "أسيتامينوفين"),
            defEn = "A centrally acting analgesic and antipyretic agent recommended for managing mild-to-moderate odontogenic pain.",
            defAr = "مسكن ألم وخافض حرارة ذو تأثير مركزي، يُعد خطاً أولياً لتسكين الآلام السنية الخفيفة إلى المتوسطة.",
            source = "WHO_EML"
        ),
        SeedConcept(
            cui = "C0039485",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "MAXILLOFACIAL_ANATOMY",
            latinName = "Articulatio temporomandibularis",
            termsEn = listOf("Temporomandibular joint", "TMJ"),
            termsAr = listOf("المفصل الفكي الصدغي", "مفصل فكي صدغي"),
            defEn = "A specialized bilateral synovial bicondylar ginglymoarthrodial joint connecting the mandibular condyle to the temporal squama.",
            defAr = "مفصل زليلي مزدوج متخصص يربط لقمة الفك السفلي بصدف العظم الصدغي ويتيح حركات المضغ والكلام.",
            source = "TERMINOLOGIA_ANATOMICA"
        )
    )

    // =========================================================================
    // MODULAR DEPARTMENT SEED CHUNKS (PREVENT 64KB METHOD CODE LIMIT)
    // =========================================================================

    val ENDODONTIC_CLINICAL_CONCEPTS: List<SeedConcept> = listOf(
        SeedConcept(
            cui = "DENT_ENDO_001",
            domain = MedicalDomain.PROCEDURE,
            subspecialty = "Endodontics",
            latinName = "Canalis radicis",
            termsEn = listOf("working length", "root canal working length"),
            termsAr = listOf("الطول العامل", "طول القناة العامل"),
            defEn = "The distance from a coronal reference point to the point at which canal preparation terminates.",
            defAr = "المسافة من نقطة مرجعية تاجية إلى النقطة التي يجب أن ينتهي عندها تحضير القناة وحشوها.",
            source = "WHO_UMD_ENDODONTICS"
        ),
        SeedConcept(
            cui = "DENT_ENDO_002",
            domain = MedicalDomain.PROCEDURE,
            subspecialty = "Endodontics",
            latinName = null,
            termsEn = listOf("obturation", "root canal obturation"),
            termsAr = listOf("حشو القناة السنية", "السد القنوي"),
            defEn = "The three-dimensional hermetic filling and sealing of the cleaned and shaped root canal system.",
            defAr = "الحشو الإحكامي ثلاثي الأبعاد لكامل الجهاز القنوي بعد تنظيفه وتشكيله لمنع التسرب الإنتاني.",
            source = "WHO_UMD_ENDODONTICS"
        ),
        SeedConcept(
            cui = "DENT_ENDO_003",
            domain = MedicalDomain.PROCEDURE,
            subspecialty = "Endodontics",
            latinName = null,
            termsEn = listOf("gutta-percha", "guttapercha", "GP point"),
            termsAr = listOf("الكوتابيركا", "الطبرخي"),
            defEn = "The thermoplastic inert core obturation material used in endodontic therapy.",
            defAr = "المادة اللدنة الحرارية الخاملة المستخدمة لسد قنوات الجذور السنية.",
            source = "WHO_UMD_ENDODONTICS"
        ),
        SeedConcept(
            cui = "DENT_ENDO_009",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "Endodontics",
            latinName = null,
            termsEn = listOf("smear layer", "endodontic smear layer"),
            termsAr = listOf("طبقة اللطاخة", "مسحوق البرادة"),
            defEn = "A microcrystalline film of dentinal debris formed on canal walls during mechanical instrumentation.",
            defAr = "طبقة مجهرية رقيقة من برادة العاج تتشكل على جدران القنوات أثناء البرد الآلي.",
            source = "WHO_UMD_ENDODONTICS"
        )
    )

    val PERIODONTIC_CLINICAL_CONCEPTS: List<SeedConcept> = listOf(
        SeedConcept(
            cui = "DENT_PERIO_001",
            domain = MedicalDomain.ANATOMY,
            subspecialty = "Periodontics",
            latinName = "Periodontium",
            termsEn = listOf("biologic width", "biological width"),
            termsAr = listOf("العرض الحيوي", "الاتساع البيولوجي"),
            defEn = "The physiological dimension of junctional epithelium and supracrestal connective tissue attachment.",
            defAr = "المسافة الفيزيولوجية الثابتة التي تشغلها الظهارة الموصلة وألياف النسيج الضام فوق الحافة العظمية.",
            source = "WHO_UMD_PERIODONTICS"
        ),
        SeedConcept(
            cui = "DENT_PERIO_003",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "Periodontics",
            latinName = null,
            termsEn = listOf("furcation involvement", "furcation defect"),
            termsAr = listOf("إصابة مفترق الجذور", "إصابة التشعب الجذري"),
            defEn = "Pathological bone resorption occurring in the interradicular area of multi-rooted teeth.",
            defAr = "امتصاص العظم وفقدان الارتباط في منطقة تشعب جذور الأسنان متعددة الجذور.",
            source = "WHO_UMD_PERIODONTICS"
        ),
        SeedConcept(
            cui = "DENT_PERIO_007",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "Periodontics",
            latinName = null,
            termsEn = listOf("clinical attachment loss", "CAL"),
            termsAr = listOf("فقدان الارتباط السريري", "فقد الارتباط اللثوي"),
            defEn = "The clinical extent of periodontal destruction measured from the CEJ to the base of the pocket.",
            defAr = "المقياس السريري لمقدار تخرب الأنسجة الداعمة مقاساً من ملتقى الميناء والملاط حتى قاع الجيب اللثوي.",
            source = "WHO_UMD_PERIODONTICS"
        )
    )

    val PROSTHODONTIC_CLINICAL_CONCEPTS: List<SeedConcept> = listOf(
        SeedConcept(
            cui = "DENT_PROSTH_001",
            domain = MedicalDomain.PROCEDURE,
            subspecialty = "Prosthodontics",
            latinName = null,
            termsEn = listOf("abutment", "dental abutment"),
            termsAr = listOf("دعامة سنية", "سن دعامة"),
            defEn = "A natural tooth or dental implant fixture utilized to retain and support a prosthetic restoration.",
            defAr = "السن الطبيعي أو الغرسة السنية التي تستند إليها وتثبت فوقها التعويضات السنية.",
            source = "WHO_UMD_PROSTHODONTICS"
        ),
        SeedConcept(
            cui = "DENT_PROSTH_002",
            domain = MedicalDomain.PROCEDURE,
            subspecialty = "ProsthodONTICS",
            latinName = null,
            termsEn = listOf("pontic", "bridge pontic"),
            termsAr = listOf("دمية الجسر", "سن صناعي بديل"),
            defEn = "The artificial tooth suspended between abutments in a fixed partial denture.",
            defAr = "السن الصناعي المعلق في الجسر السني الثابت الذي يحل محل السن المفقود.",
            source = "WHO_UMD_PROSTHODONTICS"
        ),
        SeedConcept(
            cui = "DENT_PROSTH_003",
            domain = MedicalDomain.PROCEDURE,
            subspecialty = "Prosthodontics",
            latinName = null,
            termsEn = listOf("cantilever bridge", "cantilever"),
            termsAr = listOf("جسر ناتئ", "جسر ذراعي معلق"),
            defEn = "A fixed partial denture in which the pontic is retained on only one end.",
            defAr = "جسر سني ثابت تدعم فيه دمية الجسر وتثبت من طرف واحد فقط بالدعامة.",
            source = "WHO_UMD_PROSTHODONTICS"
        ),
        SeedConcept(
            cui = "DENT_PROSTH_005",
            domain = MedicalDomain.PROCEDURE,
            subspecialty = "Prosthodontics",
            latinName = null,
            termsEn = listOf("chamfer", "chamfer finish line"),
            termsAr = listOf("شطب حفافي", "تشامفر"),
            defEn = "A concave marginal finish line design widely indicated for fixed restorations.",
            defAr = "خط إنهاء مقعر ذو زاوية منفرجة يؤمن سماكة كافية للتيجان الخزفية والمعدنية.",
            source = "WHO_UMD_PROSTHODONTICS"
        )
    )

    val OPERATIVE_CLINICAL_CONCEPTS: List<SeedConcept> = listOf(
        SeedConcept(
            cui = "DENT_OPER_001",
            domain = MedicalDomain.PROCEDURE,
            subspecialty = "Operative Dentistry",
            latinName = null,
            termsEn = listOf("etching", "acid etching"),
            termsAr = listOf("خرش حمضي", "تخريش حمضي"),
            defEn = "Application of phosphoric acid to create microporosities for resin micromechanical retention.",
            defAr = "تطبيق حمض الفوسفور لخلق مسامات مجهرية تؤمن التثبيت الميكانيكي للحشوة.",
            source = "WHO_UMD_OPERATIVE"
        ),
        SeedConcept(
            cui = "DENT_OPER_007",
            domain = MedicalDomain.PROCEDURE,
            subspecialty = "Operative Dentistry",
            latinName = null,
            termsEn = listOf("C-factor", "cavity configuration factor"),
            termsAr = listOf("معامل التكوين", "عامل التقلص السطحي"),
            defEn = "The ratio of bonded restoration surface area to unbonded surface area.",
            defAr = "نسبة عدد السطوح الملتصقة لمادة الحشوة إلى السطوح الحرة غير الملتصقة.",
            source = "WHO_UMD_OPERATIVE"
        ),
        SeedConcept(
            cui = "DENT_OPER_010",
            domain = MedicalDomain.PROCEDURE,
            subspecialty = "Operative Dentistry",
            latinName = null,
            termsEn = listOf("composite resin", "dental composite"),
            termsAr = listOf("راتينج مركب", "حشوة تجميلية"),
            defEn = "Tooth-colored restorative material composed of resin matrix and inorganic fillers.",
            defAr = "مادة ترميمية بلون الأسنان تتكون من مصفوفة راتينجية وجزيئات حشوية لاعضوية.",
            source = "WHO_UMD_OPERATIVE"
        )
    )

    val ORAL_SURGERY_CLINICAL_CONCEPTS: List<SeedConcept> = listOf(
        SeedConcept(
            cui = "DENT_SURG_001",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "Oral Surgery",
            latinName = "Alveolitis",
            termsEn = listOf("dry socket", "alveolar osteitis", "alveolitis"),
            termsAr = listOf("التهاب السنخ الجاف", "التهاب العظم السنخي", "السنخ الجاف"),
            defEn = "Painful postoperative complication caused by premature loss of the blood clot from the socket.",
            defAr = "مضاعفة مؤلمة تلي خلع الأسنان تنتج عن انحلال أو فقدان الخثرة الدموية من السنخ.",
            source = "WHO_UMD_SURGERY"
        ),
        SeedConcept(
            cui = "DENT_SURG_002",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "Oral Surgery",
            latinName = null,
            termsEn = listOf("impaction", "impacted tooth"),
            termsAr = listOf("انحشار السن", "سن مطمور"),
            defEn = "Failure of a tooth to erupt into the normal arch position due to obstruction.",
            defAr = "فشل بزوغ السن في موقعه الطبيعي في القوس السنية بسبب إعاقة عظمية أو سنية.",
            source = "WHO_UMD_SURGERY"
        )
    )

    val ORTHODONTIC_CLINICAL_CONCEPTS: List<SeedConcept> = listOf(
        SeedConcept(
            cui = "DENT_ORTHO_001",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "Orthodontics",
            latinName = "Malocclusio",
            termsEn = listOf("malocclusion", "dental malocclusion"),
            termsAr = listOf("سوء الإطباق", "سوء إطباق الأسنان"),
            defEn = "Any deviation from normal, physiologically ideal occlusion.",
            defAr = "أي اضطراب أو شذوذ عن الإطباق المثالي الطبيعي في توضع الأسنان أو القوسين.",
            source = "WHO_UMD_ORTHODONTICS"
        ),
        SeedConcept(
            cui = "DENT_ORTHO_003",
            domain = MedicalDomain.DIAGNOSTIC,
            subspecialty = "Orthodontics",
            latinName = null,
            termsEn = listOf("cephalometrics", "cephalometric"),
            termsAr = listOf("القياسات السيفالومترية", "تحليل سيفالومتري"),
            defEn = "The diagnostic study and measurement of landmarks on lateral skull radiographs.",
            defAr = "دراسة وقياس الأبعاد العظمية والسنية على صور الجمجمة الشعاعية الجانبية.",
            source = "WHO_UMD_ORTHODONTICS"
        )
    )

    val ORAL_PATHOLOGY_CLINICAL_CONCEPTS: List<SeedConcept> = listOf(
        SeedConcept(
            cui = "DENT_PATH_001",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "Oral Pathology",
            latinName = "Leukoplakia",
            termsEn = listOf("leukoplakia", "oral leukoplakia"),
            termsAr = listOf("طلاوة بيضاء", "تقرن فموي أبيض"),
            defEn = "A predominantly white plaque of questionable risk carrying potential for malignancy.",
            defAr = "آفة فموية بيضاء صفيحية غير قابلة للكشط تعد آفة محتملة الخباثة.",
            source = "WHO_UMD_PATHOLOGY"
        ),
        SeedConcept(
            cui = "DENT_PATH_003",
            domain = MedicalDomain.PATHOLOGY,
            subspecialty = "Oral Pathology",
            latinName = "Lichen planus",
            termsEn = listOf("lichen planus", "oral lichen planus"),
            termsAr = listOf("حزاز مسطح", "الحزاز المنبسط الفموي"),
            defEn = "A chronic autoimmune inflammatory mucocutaneous disorder with Wickham striae.",
            defAr = "مرض التهابي مناعي ذاتي مزمن يصيب المخاطية الفموية ويتميز بخطوط ويكهام البيضاء.",
            source = "WHO_UMD_PATHOLOGY"
        )
    )

    val ALL_SEED_CHUNKS: List<List<SeedConcept>> by lazy {
        listOf(
            DEFAULT_SEED_CONCEPTS,
            ENDODONTIC_CLINICAL_CONCEPTS,
            PERIODONTIC_CLINICAL_CONCEPTS,
            PROSTHODONTIC_CLINICAL_CONCEPTS,
            OPERATIVE_CLINICAL_CONCEPTS,
            ORAL_SURGERY_CLINICAL_CONCEPTS,
            ORTHODONTIC_CLINICAL_CONCEPTS,
            ORAL_PATHOLOGY_CLINICAL_CONCEPTS
        )
    }

    /**
     * Inserts default medical concepts, terms, and definitions into the database.
     * Guarantees all essential dental & medical concepts exist even if database was partially initialized.
     */
    suspend fun seedDefaultLexicon(dao: MedicalLexiconDao, forceRefresh: Boolean = false) {
        val clinicalMatch = dao.findExactMatch("clinical")
        val biologicMatch = dao.findExactMatch("biologic")
        val needsSeed = forceRefresh ||
            dao.getTermCount() == 0 ||
            dao.findExactMatch("Enamel") == null ||
            clinicalMatch == null ||
            clinicalMatch.domain != MedicalDomain.GENERAL_CLINICAL ||
            biologicMatch == null

        if (!needsSeed && dao.getTermCount() >= 50) {
            return
        }

        // If refreshing corrupted/outdated seeds or missing landmark terms, clear old concepts to ensure clean state
        if (forceRefresh || (clinicalMatch != null && clinicalMatch.domain != MedicalDomain.GENERAL_CLINICAL) || biologicMatch == null) {
            dao.clearConcepts()
        }

        for (chunk in ALL_SEED_CHUNKS) {
            for (item in chunk) {
                val concept = MedicalConceptEntity(
                    cui = item.cui,
                    category = item.domain.name,
                    subspecialty = item.subspecialty,
                    latinName = item.latinName
                )
                val conceptId = dao.insertConcept(concept)

                // Insert all English term variations
                for ((index, enTerm) in item.termsEn.withIndex()) {
                    val entity = MedicalTermEntity(
                        conceptId = conceptId,
                        langCode = "en",
                        termText = enTerm,
                        isPreferred = (index == 0),
                        source = item.source
                    )
                    dao.insertTerm(entity)
                }

                // Insert all Arabic term variations
                for ((index, arTerm) in item.termsAr.withIndex()) {
                    val entity = MedicalTermEntity(
                        conceptId = conceptId,
                        langCode = "ar",
                        termText = arTerm,
                        isPreferred = (index == 0),
                        source = item.source
                    )
                    dao.insertTerm(entity)
                }

                // Insert Latin name if distinct from primary English term
                if (item.latinName != null && !item.termsEn.any { it.equals(item.latinName, ignoreCase = true) }) {
                    val laTerm = MedicalTermEntity(
                        conceptId = conceptId,
                        langCode = "la",
                        termText = item.latinName,
                        isPreferred = false,
                        source = item.source
                    )
                    dao.insertTerm(laTerm)
                }

                // Insert clinical definitions
                if (item.defEn != null || item.defAr != null) {
                    val def = MedicalDefinitionEntity(
                        conceptId = conceptId,
                        definitionEn = item.defEn,
                        definitionAr = item.defAr
                    )
                    dao.insertDefinition(def)
                }
            }
        }
    }
}

