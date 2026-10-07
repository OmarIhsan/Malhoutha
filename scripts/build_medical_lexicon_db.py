#!/usr/bin/env python3
"""
Seed Asset Pipeline: Generates the pre-packaged SQLite FTS4 medical lexicon database
for offline academic and clinical translation in Malhoutha.
Target: app/src/main/assets/databases/medical_lexicon.db
"""

import os
import sqlite3

DB_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "src", "main", "assets", "databases", "medical_lexicon.db"
)

IDENTITY_HASH = "658ddc719aeb1df7bbde4639d82798b4"

SEED_CONCEPTS = [
    # --- DENTAL HISTOLOGY, EMBRYOLOGY & ANATOMY ---
    {
        "cui": "C0011332",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Substantia adamantina",
        "termEn": "Enamel",
        "termsEnExtra": ["Dental enamel", "Tooth enamel", "Enamels"],
        "termAr": "ميناء الأسنان",
        "termsArExtra": ["الميناء", "ميناء السن"],
        "defEn": "The hard, highly mineralized outer protective layer of the anatomical crown of the tooth, formed by ameloblasts and composed primarily of hydroxyapatite crystals.",
        "defAr": "النسيج الكلسي الأشد قساوة في جسم الإنسان الذي يغطي تاج السن التشريحي، تفرزه مصورات الميناء ويتكون أساساً من بلورات الهيدروكسي أباتيت.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0002446",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Ameloblastus",
        "termEn": "Ameloblast",
        "termsEnExtra": ["Ameloblasts", "Ameloblastic cell"],
        "termAr": "أرومة الميناء",
        "termsArExtra": ["أرومات الميناء", "خلايا مصورة للميناء", "خلية مصورة للميناء"],
        "defEn": "Specialized columnar epithelial cells of ectodermal origin that differentiate from the inner enamel epithelium to form and mineralize dental enamel.",
        "defAr": "خلايا ظهارية عمودية متخصصة من أصل أدمي ظاهر تتمايز من الظهارة المينائية الداخلية لتفرز مصفوفة الميناء وتشرف على تمعدنها.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0011328",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Substantia eburnea",
        "termEn": "Dentin",
        "termsEnExtra": ["Dentine", "Dental dentin"],
        "termAr": "عاج الأسنان",
        "termsArExtra": ["العاج", "عاج السن"],
        "defEn": "The mineralized connective tissue forming the bulk of the tooth structure, located deep to the enamel and cementum and surrounding the pulp cavity.",
        "defAr": "النسيج الضام المتمعدن الذي يشكل الكتلة الرئيسية لبنية السن، ويقع أسفل الميناء والملاط محيطاً بحجرة اللب وقنوات الجذور.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0028963",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Odontoblastus",
        "termEn": "Odontoblast",
        "termsEnExtra": ["Odontoblasts", "Odontoblastic cell"],
        "termAr": "أرومة العاج",
        "termsArExtra": ["أرومات العاج", "خلايا مصورة للعاج", "خلية بانية للعاج"],
        "defEn": "Specialized cells of neural crest ectomesenchymal origin lining the outer periphery of the dental pulp that produce and maintain dentin throughout life.",
        "defAr": "خلايا أدمية متوسطة متخصصة تصطف على المحيط الخارجي للب السن وتفرز مصفوفة العاج وتستمر في صيانته وتكوينه طوال الحياة.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0011333",
        "category": "ANATOMY",
        "subspecialty": "ENDODONTICS",
        "latinName": "Pulpa dentis",
        "termEn": "Dental pulp",
        "termsEnExtra": ["Pulp", "Tooth pulp"],
        "termAr": "لب السن",
        "termsArExtra": ["اللب السني", "لب الأسنان"],
        "defEn": "The richly vascularized, innervated loose connective tissue occupying the central pulp cavity of the tooth, responsible for dentin vitality, sensation, and defense.",
        "defAr": "النسيج الضام الرخو الوعائي والعصبي الذي يشغل حجرة وقنوات السن، والمسؤول عن تغذية العاج وحيويته وحساسيته ودفاعه المناعي.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0007787",
        "category": "ANATOMY",
        "subspecialty": "PERIODONTICS",
        "latinName": "Cementum",
        "termEn": "Cementum",
        "termsEnExtra": ["Dental cementum", "Tooth cementum"],
        "termAr": "ملاط السن",
        "termsArExtra": ["الملاط", "ملاط الأسنان"],
        "defEn": "A specialized calcified avascular substance covering the anatomical root of the tooth, providing attachment for Sharpey's fibers of the periodontal ligament.",
        "defAr": "طبقة متكلسة لاوعائية تغطي جذر السن التشريحي، وتثبت ألياف شاربي الخاصة برباط دواعم السن لربط الجذر بالعظم السنخي.",
        "source": "PERIODONTOLOGY_CARRANZA"
    },
    {
        "cui": "C0224675",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Prisma adamatinum",
        "termEn": "Enamel rod",
        "termsEnExtra": ["Enamel prism", "Enamel rods", "Enamel prisms"],
        "termAr": "قضيب الميناء",
        "termsArExtra": ["موشور الميناء", "قضبان الميناء", "مواشير الميناء"],
        "defEn": "The primary structural unit of dental enamel, densely packed with millions of hydroxyapatite crystallites running from the dentinoenamel junction to the tooth surface.",
        "defAr": "الوحدة البنائية الأساسية للميناء، تتكون من ملايين بلورات الهيدروكسي أباتيت المتراصة الممتدة من الملتقى المينائي العاجي نحو سطح السن الخارجي.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0224676",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Membrana adamantina",
        "termEn": "Nasmyth membrane",
        "termsEnExtra": ["Nasmyth's membrane", "Dental cuticle"],
        "termAr": "غشاء ناسميث",
        "termsArExtra": ["غشاء ناسميث المينائي", "الغشاء المينائي"],
        "defEn": "A delicate organic membrane covering the enamel of a newly erupted tooth, composed of the primary enamel cuticle and remnants of the reduced enamel epithelium.",
        "defAr": "غشاء رقيق عضوي يغطي سطح تاج السن البازغ حديثاً، ويتألف من جليدة الميناء الأولية وبقايا ظهارة الميناء الضامرة ويزول بالاحتكاك والمضغ.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0224677",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Cuticula adamantina primaria",
        "termEn": "Primary enamel cuticle",
        "termsEnExtra": ["Primary cuticle"],
        "termAr": "جليدة الميناء الأولية",
        "termsArExtra": ["الجليدة الأولية للميناء"],
        "defEn": "The thin basal lamina-like acellular layer secreted by ameloblasts as their final functional act onto the surface of mature enamel prior to tooth eruption.",
        "defAr": "طبقة رقيقة غير خلوية تشبه الصفيحة القاعدية، تفرزها أرومات الميناء كآخر عمل وظيفي لها على سطح الميناء الناضج قبل بزوغ السن.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0224678",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Tubuli dentinales",
        "termEn": "Dentinal tubule",
        "termsEnExtra": ["Dentinal tubules", "Dentine tubule", "Dentine tubules"],
        "termAr": "نبيبات عاجية",
        "termsArExtra": ["النبيبات العاجية", "نبيب عاجي"],
        "defEn": "Microscopic cylindrical canals extending radially through the entire thickness of dentin, housing odontoblastic processes and dentinal fluid.",
        "defAr": "أقنية مجهرية أسطوانية تخترق كامل ثخانة العاج شعاعياً من اللب إلى الملتقى المينائي العاجي، وتحتوي على استطالات أرومات العاج وسائل العاج.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0224679",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Striae Retzii",
        "termEn": "Striae of Retzius",
        "termsEnExtra": ["Retzius striae", "Incremental lines of Retzius"],
        "termAr": "خطوط ريتزيوس",
        "termsArExtra": ["خطوط ريتزيوس التزايدية", "أشرطة ريتزيوس"],
        "defEn": "Incremental growth lines visible in microscopic sections of enamel reflecting rhythmic variations in the mineral deposition rate during amelogenesis.",
        "defAr": "خطوط نمو تزايدية مجهرية تظهر في مقاطع الميناء تعكس التغيرات الدورية اليومية في وتيرة التمعدن أثناء تكون الميناء.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0224680",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Junctio dentinoenamelis",
        "termEn": "Dentinoenamel junction",
        "termsEnExtra": ["DEJ", "Dentino-enamel junction", "Amelodentinal junction"],
        "termAr": "الملتقى المينائي العاجي",
        "termsArExtra": ["الحد المينائي العاجي"],
        "defEn": "The scalloped, highly compliant anatomical interface between dental enamel and underlying coronal dentin that prevents catastrophic crack propagation.",
        "defAr": "الحد الفاصل المتموج بين ميناء السن وعاج التاج، والذي يتمتع بمرونة وظيفية عالية تمنع انتشار التصدعات والكسور في بنية السن.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0224681",
        "category": "ANATOMY",
        "subspecialty": "PERIODONTICS",
        "latinName": "Junctio cementoenamelis",
        "termEn": "Cementoenamel junction",
        "termsEnExtra": ["CEJ", "Cervical line"],
        "termAr": "الملتقى الملاطي المينائي",
        "termsArExtra": ["الخط العنقي للسن", "الملتقى المينائي الملاطي"],
        "defEn": "The anatomical border where the enamel covering the crown of the tooth meets the cementum covering the root at the cervical region.",
        "defAr": "الحد التشريحي العنقي حيث يلتقي الميناء المغطي للتاج بالملاط المغطي للجذر، ويشكل معلماً هاماً لتقييم انحسار اللثة وفقدان الارتباط.",
        "source": "PERIODONTOLOGY_CARRANZA"
    },
    {
        "cui": "C0031086",
        "category": "ANATOMY",
        "subspecialty": "PERIODONTICS",
        "latinName": "Ligamentum periodontale",
        "termEn": "Periodontal ligament",
        "termsEnExtra": ["PDL", "Periodontal membrane"],
        "termAr": "رباط دواعم السن",
        "termsArExtra": ["الرباط السنخي السني", "رباط حول السن"],
        "defEn": "The fibrous connective tissue structure surrounding the tooth root, attaching cementum to the surrounding alveolar bone socket and absorbing masticatory shock.",
        "defAr": "بنية نسيجية ليفية ضامة تحيط بجذر السن وتربط الملاط بالعظم السنخي، وتعمل كوسادة هيدروليكية لامتصاص قوى المضغ وإدراك الضغط.",
        "source": "PERIODONTOLOGY_CARRANZA"
    },
    {
        "cui": "C0002279",
        "category": "ANATOMY",
        "subspecialty": "PERIODONTICS",
        "latinName": "Processus alveolaris",
        "termEn": "Alveolar bone",
        "termsEnExtra": ["Alveolar process", "Alveolar ridge"],
        "termAr": "العظم السنخي",
        "termsArExtra": ["النتوء السنخي", "عظم السنخ"],
        "defEn": "The specialized ridge of bone on the maxilla and mandible containing tooth sockets that support dentition.",
        "defAr": "الامتداد العظمي المتخصص في الفكين العلوي والسفلي الذي يضم الأسناخ ويثبت جذور الأسنان.",
        "source": "TERMINOLOGIA_ANATOMICA"
    },
    {
        "cui": "C0224682",
        "category": "ANATOMY",
        "subspecialty": "ORAL_ANATOMY",
        "latinName": "Corona dentis",
        "termEn": "Crown",
        "termsEnExtra": ["Tooth crown", "Clinical crown", "Anatomical crown"],
        "termAr": "تاج السن",
        "termsArExtra": ["التاج السني", "التاج التشريحي", "التاج السريري"],
        "defEn": "The portion of a tooth that is normally covered by enamel (anatomical crown) or visible in the oral cavity above the gingival margin (clinical crown).",
        "defAr": "الجزء من السن المغطى بالميناء (التاج التشريحي) أو الجزء المرئي في جوف الفم فوق الحافة اللثوية (التاج السريري).",
        "source": "ORAL_ANATOMY_WHEELER"
    },
    {
        "cui": "C0224683",
        "category": "ANATOMY",
        "subspecialty": "ORAL_ANATOMY",
        "latinName": "Radix dentis",
        "termEn": "Root",
        "termsEnExtra": ["Tooth root", "Dental root", "Roots"],
        "termAr": "جذر السن",
        "termsArExtra": ["الجذر السني", "جذور الأسنان"],
        "defEn": "The anatomical portion of the tooth covered by cementum, embedded in the alveolar bone socket, and anchored by the periodontal ligament.",
        "defAr": "الجزء التشريحي من السن المغطى بالملاط، والمنغرس داخل السنخ العظمي والمثبت بواسطة رباط دواعم السن.",
        "source": "ORAL_ANATOMY_WHEELER"
    },
    {
        "cui": "C0224684",
        "category": "ANATOMY",
        "subspecialty": "ENDODONTICS",
        "latinName": "Apex radicis dentis",
        "termEn": "Apex",
        "termsEnExtra": ["Root apex", "Apical tip"],
        "termAr": "ذروة الجذر",
        "termsArExtra": ["الذروة السنية", "ذروة السن"],
        "defEn": "The terminal tip or anatomical end of the tooth root containing the apical foramen through which pulpal neurovascular bundles pass.",
        "defAr": "النهاية الطرفية لجذر السن التي تضم الثقبة الذروية وتمر عبرها الحزم العصبية والأوعية الدموية المغذية للب.",
        "source": "ENDODONTICS_COHEN"
    },
    {
        "cui": "C0224685",
        "category": "ANATOMY",
        "subspecialty": "ENDODONTICS",
        "latinName": "Foramen apicale",
        "termEn": "Apical foramen",
        "termsEnExtra": ["Apical foramina", "Root foramen"],
        "termAr": "الثقبة الذروية",
        "termsArExtra": ["الثقبة القمية", "ثقبة الذروة"],
        "defEn": "The microscopic natural opening at the root apex communicating the dental pulp cavity with the periapical periodontal ligament tissues.",
        "defAr": "الفتحة الطبيعية المجهرية عند ذروة جذر السن التي تصل اللب السني بالأنسجة المحيطة بالذروة والرباط السنخي.",
        "source": "ENDODONTICS_COHEN"
    },
    {
        "cui": "C0224686",
        "category": "ANATOMY",
        "subspecialty": "ENDODONTICS",
        "latinName": "Canalis radicis dentis",
        "termEn": "Root canal",
        "termsEnExtra": ["Pulp canal", "Root canals"],
        "termAr": "قناة الجذر",
        "termsArExtra": ["القناة الجذرية", "قنوات الجذور"],
        "defEn": "The tubular anatomical space within the tooth root traversing from the pulp chamber to the apical foramen.",
        "defAr": "الممر الأنبوبي التشريحي داخل جذر السن الممتد من حجرة اللب حتى الثقبة الذروية.",
        "source": "ENDODONTICS_COHEN"
    },
    {
        "cui": "C0224687",
        "category": "ANATOMY",
        "subspecialty": "ENDODONTICS",
        "latinName": "Cavitas pulparis",
        "termEn": "Pulp chamber",
        "termsEnExtra": ["Coronal pulp chamber", "Pulp cavity"],
        "termAr": "حجرة اللب",
        "termsArExtra": ["الحجرة اللبية", "التجويف اللبي"],
        "defEn": "The central enlarged anatomical cavity within the coronal portion of the tooth housing the coronal dental pulp.",
        "defAr": "التجويف المركزي المتسع داخل تاج السن الذي يحتوي على اللب التاجي.",
        "source": "ENDODONTICS_COHEN"
    },
    {
        "cui": "C0224688",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Processus Tomes",
        "termEn": "Tomes process",
        "termsEnExtra": ["Tomes' process", "Tomes processes"],
        "termAr": "استطالة تومز",
        "termsArExtra": ["بروز تومز", "استطالات تومز"],
        "defEn": "The specialized conical projection at the apical secretory end of an ameloblast responsible for orienting hydroxyapatite crystals into distinct enamel rods.",
        "defAr": "الاستطالة المخروطية المتخصصة في النهاية الإفرازية لأرومة الميناء المسؤولة عن توجيه البلورات وتشكيل قضبان الميناء.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0011330",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Amelogenesis",
        "termEn": "Amelogenesis",
        "termsEnExtra": ["Enamel formation", "Amelogenic"],
        "termAr": "تكون الميناء",
        "termsArExtra": ["تشكل الميناء"],
        "defEn": "The complex, tightly regulated biological process of enamel matrix deposition and mineralization by ameloblasts during odontogenesis.",
        "defAr": "العملية البيولوجية المعقدة لترسيب ونضج مصفوفة الميناء وتمعدنها بواسطة أرومات الميناء أثناء تشكل الأسنان.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0011331",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Dentinogenesis",
        "termEn": "Dentinogenesis",
        "termsEnExtra": ["Dentin formation"],
        "termAr": "تكون العاج",
        "termsArExtra": ["تشكل العاج"],
        "defEn": "The process of predentin synthesis, apposition, and subsequent biomineralization by odontoblasts during tooth development and in response to injury.",
        "defAr": "عملية تصنيع وإفراز طليعة العاج وتمعدنها لاحقاً بواسطة أرومات العاج أثناء نمو السن أو كاستجابة دفاعية للرضح والنخر.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0017565",
        "category": "ANATOMY",
        "subspecialty": "PERIODONTICS",
        "latinName": "Gingiva",
        "termEn": "Gingiva",
        "termsEnExtra": ["Gingivae", "Gum", "Gums"],
        "termAr": "اللثة",
        "termsArExtra": ["النسيج اللثوي"],
        "defEn": "The mucosal tissue covering the alveolar processes of the jaws and encircling the necks of the teeth to form a protective seal.",
        "defAr": "الغشاء المخاطي المتخصص الذي يغطي العظم السنخي ويحيط بأعناق الأسنان مشكلاً طوقاً واقياً للأنسجة الداعمة العميقة.",
        "source": "PERIODONTOLOGY_CARRANZA"
    },
    {
        "cui": "C0039502",
        "category": "ANATOMY",
        "subspecialty": "ORAL_ANATOMY",
        "latinName": "Dens",
        "termEn": "Tooth",
        "termsEnExtra": ["Teeth", "Dentition"],
        "termAr": "السن",
        "termsArExtra": ["الأسنان"],
        "defEn": "Hard, calcified biological structures embedded in the jaws, specialized for mastication, speech phonetics, and facial aesthetics.",
        "defAr": "بنى كلسية صلبة مغروسة في الفكين، مخصصة للمضغ والنطق ودعم المظهر الجمالي للوجه.",
        "source": "TERMINOLOGIA_ANATOMICA"
    },
    {
        "cui": "C0224689",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Lamina dentalis",
        "termEn": "Dental lamina",
        "termsEnExtra": ["Primary dental lamina"],
        "termAr": "الصفيحة السنية",
        "termsArExtra": ["الصفيحة السنية الأولية"],
        "defEn": "A band of ectodermal epithelial thickening originating from the stomodeum giving rise to tooth buds during early craniofacial embryogenesis.",
        "defAr": "شريط ظهاري جنيني يثخن من الظهارة الفموية وينغمد داخل اللحمة المتوسطة ليعطي براعم الأسنان اللبنية والدائمة.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0224690",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Organum adamatinum",
        "termEn": "Enamel organ",
        "termsEnExtra": ["Dental organ"],
        "termAr": "عضو الميناء",
        "termsArExtra": ["العضو المينائي"],
        "defEn": "The epithelial structure of a tooth germ that forms enamel, consisting of outer and inner enamel epithelia, stellate reticulum, and stratum intermedium.",
        "defAr": "البنية الظهارية في برعم السن المسؤولة عن تكوين الميناء، وتتألف من الظهارة المينائية الخارجية والداخلية والشبكة النجمية والطبقة المتوسطة.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0224691",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Papilla dentalis",
        "termEn": "Dental papilla",
        "termsEnExtra": ["Dentinal papilla"],
        "termAr": "الحليمة السنية",
        "termsArExtra": ["حليمة السن"],
        "defEn": "The mesenchymal condensation enclosed by the enamel organ that differentiates into odontoblasts to form dentin and dental pulp.",
        "defAr": "التكثف الوسنخيمي داخل عضو الميناء الذي تتمايز خلاياه السطحية إلى أرومات العاج لتشكل العاج ولب السن.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0224692",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Folliculus dentalis",
        "termEn": "Dental follicle",
        "termsEnExtra": ["Dental sac"],
        "termAr": "الجريب السني",
        "termsArExtra": ["الكيس السني"],
        "defEn": "The ectomesenchymal envelope surrounding the enamel organ and dental papilla, giving rise to cementum, periodontal ligament, and alveolar bone.",
        "defAr": "الغلاف الوسنخيمي المحيط بعضو الميناء والحليمة السنية، والذي ينشأ منه الملاط ورباط دواعم السن والعظم السنخي المحيط.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0224693",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Striae Hunter-Schreger",
        "termEn": "Hunter-Schreger bands",
        "termsEnExtra": ["Hunter Schreger bands", "HSB"],
        "termAr": "حزم هنتر-شريغر",
        "termsArExtra": ["أشرطة هنتر-شريغر"],
        "defEn": "Alternating optical light (parazones) and dark (diazones) bands seen under reflected light in enamel sections, resulting from regular changes in enamel rod orientation.",
        "defAr": "حزم بصرية متناوبة من الأشرطة الفاتحة والداكنة تظهر تحت الضوء المنعكس في مقاطع الميناء نتيجة تغيرات دورية في اتجاه قضبان الميناء لتقوية مقاومة الإجهاد.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0224694",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Perikymata",
        "termEn": "Perikymata",
        "termsEnExtra": ["Perikyma"],
        "termAr": "التموجات السطحية للميناء",
        "termsArExtra": ["ميازيب الميناء السطحية"],
        "defEn": "Wave-like transverse surface grooves on the crown of teeth representing the external clinical manifestations of the striae of Retzius.",
        "defAr": "تلافيف وتموجات سطحية مجهرية على سطح تاج السن تمثل الانتهاء السطحي الخارجي لخطوط ريتزيوس التزايدية.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0224695",
        "category": "ANATOMY",
        "subspecialty": "PREVENTIVE_DENTISTRY",
        "latinName": "Pellicula dentis",
        "termEn": "Pellicle",
        "termsEnExtra": ["Acquired pellicle", "Salivary pellicle"],
        "termAr": "جليدة الأسنان المكتسبة",
        "termsArExtra": ["الجليدة المكتسبة", "الغشاء اللعابي المكتسب"],
        "defEn": "An acellular glycoprotein film derived from saliva that rapidly adheres to clean enamel surfaces, providing lubricative protection and serving as the substrate for bacterial colonization.",
        "defAr": "غشاء رقيق لاخلوي من بروتينات اللعاب السكرية يترسب بسرعة على سطح الميناء النظيف ليوفر الحماية من التآكل الحمضي ويشكل قاعدة لتثبت الجراثيم.",
        "source": "PREVENTIVE_DENTISTRY"
    },
    {
        "cui": "C0024795",
        "category": "PHYSIOLOGY",
        "subspecialty": "ORAL_PHYSIOLOGY",
        "latinName": "Masticatio",
        "termEn": "Mastication",
        "termsEnExtra": ["Chewing"],
        "termAr": "المضغ",
        "termsArExtra": ["عملية المضغ"],
        "defEn": "The physiological process of crushing and grinding food between the maxillary and mandibular teeth, mediated by the muscles of mastication and TMJ.",
        "defAr": "العملية الفيزيولوجية لسحق وطحن الطعام بين أسنان الفكين بمساعدة عضلات المضغ والمفصل الفكي الصدغي واللعاب.",
        "source": "ORAL_PHYSIOLOGY"
    },
    {
        "cui": "C0011329",
        "category": "PATHOLOGY",
        "subspecialty": "DEVELOPMENTAL_PATHOLOGY",
        "latinName": "Hypoplasia adamanti",
        "termEn": "Enamel hypoplasia",
        "termsEnExtra": ["Hypoplasia"],
        "termAr": "نقص تنسج الميناء",
        "termsArExtra": ["نقص تشكل الميناء"],
        "defEn": "A quantitative defect of enamel resulting from disruption of ameloblasts during matrix deposition, manifesting as pits, grooves, or complete absence of enamel.",
        "defAr": "عيب نمائي كمي في الميناء ينجم عن اضطراب إفراز أرومات الميناء للمصفوفة العضوية، ويظهر سريرياً كنقر أو أخاديد أو نقص في ثخانة الميناء.",
        "source": "ORAL_PATHOLOGY_NEVILLE"
    },
    {
        "cui": "C0016202",
        "category": "PATHOLOGY",
        "subspecialty": "PREVENTIVE_DENTISTRY",
        "latinName": "Fluorosis dentium",
        "termEn": "Dental fluorosis",
        "termsEnExtra": ["Fluorosis", "Enamel fluorosis", "Mottled enamel"],
        "termAr": "التسمم الفلوري للأسنان",
        "termsArExtra": ["الفلور السني", "الميناء المبقع"],
        "defEn": "A developmental disturbance of enamel mineralization caused by chronic excessive systemic fluoride intake during tooth development, producing opaque white spots or brown pitting.",
        "defAr": "اضطراب نمائي في تمعدن الميناء ناجم عن تناول جرعات زائدة ومزمنة من الفلورايد أثناء تكلس الأسنان، يظهر كبقع بيضاء طباشيرية أو تصبغات بنية ونقر.",
        "source": "PREVENTIVE_DENTISTRY"
    },

    # --- CLINICAL PATHOLOGY, SURGERY & PHARMACOLOGY ---
    {
        "cui": "C0002447",
        "category": "PATHOLOGY",
        "subspecialty": "ORAL_PATHOLOGY",
        "latinName": "Ameloblastoma",
        "termEn": "Ameloblastoma",
        "termsEnExtra": ["Ameloblastomas"],
        "termAr": "ورم أرومي مينائي",
        "termsArExtra": ["الورم الأرومي المينائي"],
        "defEn": "A benign but locally aggressive odontogenic epithelial neoplasm most commonly found in the posterior mandible.",
        "defAr": "ورم سني المنشأ ظهاري حميد سريرياً لكنه ارتشاحي وعدواني موضعياً، يظهر غالباً في المنطقة الخلفية للفك السفلي.",
        "source": "WHO_ODONTOGENIC_TUMORS"
    },
    {
        "cui": "C0028965",
        "category": "PATHOLOGY",
        "subspecialty": "ORAL_PATHOLOGY",
        "latinName": "Keratocystis odontogenica",
        "termEn": "Odontogenic keratocyst",
        "termsEnExtra": ["OKC", "Keratocyst"],
        "termAr": "كيس قرني سني المنشأ",
        "termsArExtra": ["الكيس القرني السني المنشأ"],
        "defEn": "A developmental intraosseous odontogenic cyst lined with parakeratinized stratified squamous epithelium known for high recurrence rates.",
        "defAr": "كيس نسيجي سني المنشأ داخل العظم مبطن بنسيج طلائي حرشفي متقرن يتميز بنسبة نكس مرتفعة.",
        "source": "WHO_ODONTOGENIC_TUMORS"
    },
    {
        "cui": "C0024477",
        "category": "ANATOMY",
        "subspecialty": "HEAD_AND_NECK_ANATOMY",
        "latinName": "Nervus alveolaris inferior",
        "termEn": "Inferior alveolar nerve",
        "termsEnExtra": ["IAN", "Inferior dental nerve"],
        "termAr": "العصب السنخي السفلي",
        "termsArExtra": ["عصب سنخي سفلي"],
        "defEn": "A major branch of the mandibular nerve (V3) traversing the mandibular canal to provide sensation to mandibular teeth and lower lip.",
        "defAr": "فرع رئيسي من العصب الفكي السفلي يمر عبر القناة الفكية السفلية لتغذية أسنان الفك السفلي والشفة السفلى حسياً.",
        "source": "TERMINOLOGIA_ANATOMICA"
    },
    {
        "cui": "C0023605",
        "category": "PATHOLOGY",
        "subspecialty": "ORAL_MEDICINE",
        "latinName": "Lichen planus oralis",
        "termEn": "Lichen planus",
        "termsEnExtra": ["Oral lichen planus", "OLP"],
        "termAr": "الحزاز المسطح",
        "termsArExtra": ["حزاز مسطح فموي"],
        "defEn": "A chronic T-cell mediated inflammatory disease of the oral mucosa manifesting with reticular Wickham's striae or erosive lesions.",
        "defAr": "مرض التهابي مزمن متوسط بالخلايا التائية يصيب الغشاء المخاطي الفموي ويظهر بشكل خطوط ويكهام الشبكية أو آفات تآكلية.",
        "source": "MESH"
    },
    {
        "cui": "C0002645",
        "category": "PHARMACOLOGY",
        "subspecialty": "ANTIMICROBIALS",
        "latinName": "Amoxicillinum",
        "termEn": "Amoxicillin",
        "termsEnExtra": ["Amoxil"],
        "termAr": "أموكسيسيلين",
        "termsArExtra": ["أموكسيسللين"],
        "defEn": "A moderate-spectrum bactericidal beta-lactam antibiotic used as first-line empirical therapy in odontogenic and respiratory infections.",
        "defAr": "مضاد حيوي قاتل للجراثيم من زمرة بيتا-لاكتام واسع/متوسط الطيف، يُستخدم كخط دفاع أول في الإنتانات السنية والتنفسية.",
        "source": "WHO_EML"
    },
    {
        "cui": "C0034084",
        "category": "PATHOLOGY",
        "subspecialty": "ENDODONTICS",
        "latinName": "Pulpitis",
        "termEn": "Pulpitis",
        "termsEnExtra": ["Reversible pulpitis", "Irreversible pulpitis"],
        "termAr": "التهاب لب السن",
        "termsArExtra": ["التهاب اللب", "التهاب اللب السني"],
        "defEn": "Inflammation of dental pulp tissue resulting primarily from bacterial microleakage in advanced dental caries.",
        "defAr": "التهاب في النسيج اللبي الوعائي العصبي داخل السن ناجم أساساً عن ارتشاح الجراثيم في نخر الأسنان المتقدم.",
        "source": "MESH"
    },
    {
        "cui": "C0224673",
        "category": "ANATOMY",
        "subspecialty": "MAXILLOFACIAL_ANATOMY",
        "latinName": "Foramen mentale",
        "termEn": "Mental foramen",
        "termsEnExtra": ["Mental foramina"],
        "termAr": "الثقبة الذقنية",
        "termsArExtra": ["ثقبة ذقنية"],
        "defEn": "A bilateral anatomical aperture on the buccal aspect of the mandible transmitting the mental nerve and mental vessels.",
        "defAr": "ثقبة تشريحية مزدوجة على السطح الخارجي لعظم الفك السفلي يمر عبرها العصب والأوعية الذقنية.",
        "source": "TERMINOLOGIA_ANATOMICA"
    },
    {
        "cui": "C0017574",
        "category": "PATHOLOGY",
        "subspecialty": "PERIODONTICS",
        "latinName": "Gingivitis",
        "termEn": "Gingivitis",
        "termsEnExtra": ["Plaque-induced gingivitis"],
        "termAr": "التهاب اللثة",
        "termsArExtra": ["التهاب لثوي"],
        "defEn": "Reversible plaque-induced inflammation of the marginal gingival tissues characterized by erythema, edema, and bleeding on probing.",
        "defAr": "التهاب لثوي عكوس ناجم عن تراكم اللويحة الجرثومية يتميز باحمرار اللثة وتورمها ونزفها عند الفحص بالمسبر.",
        "source": "AAP_EFP_CLASSIFICATION"
    },
    {
        "cui": "C0031114",
        "category": "PATHOLOGY",
        "subspecialty": "PERIODONTICS",
        "latinName": "Periodontitis",
        "termEn": "Periodontitis",
        "termsEnExtra": ["Chronic periodontitis", "Aggressive periodontitis"],
        "termAr": "التهاب دواعم السن",
        "termsArExtra": ["التهاب النسج الداعمة"],
        "defEn": "A chronic inflammatory disease caused by microbial dysbiosis leading to irreversible loss of periodontal ligament attachment and alveolar bone resorption.",
        "defAr": "مرض التهابي مزمن ناجم عن اختلال التوازن الميكروبي يؤدي إلى فقدان غير عكوس في ارتباط رباط دواعم السن وامتصاص العظم السنخي.",
        "source": "AAP_EFP_CLASSIFICATION"
    },
    {
        "cui": "C0011334",
        "category": "PATHOLOGY",
        "subspecialty": "OPERATIVE_DENTISTRY",
        "latinName": "Caries dentium",
        "termEn": "Dental caries",
        "termsEnExtra": ["Caries", "Tooth decay", "Cavity", "Cavities"],
        "termAr": "نخر الأسنان",
        "termsArExtra": ["تسوس الأسنان", "نخر السن"],
        "defEn": "Demineralization of inorganic tooth substance caused by organic acids produced through bacterial fermentation of dietary carbohydrates.",
        "defAr": "انحلال وتمعدن البنية غير العضوية للسن بفعل الأحماض العضوية الناتجة عن تخمر الكربوهيدرات الغذائية بواسطة الجراثيم.",
        "source": "WHO_UMD"
    },
    {
        "cui": "C0024958",
        "category": "ANATOMY",
        "subspecialty": "MAXILLOFACIAL_ANATOMY",
        "latinName": "Sinus maxillaris",
        "termEn": "Maxillary sinus",
        "termsEnExtra": ["Highmore antrum", "Antrum of Highmore"],
        "termAr": "الجيب الفكي",
        "termsArExtra": ["جيب فكي"],
        "defEn": "The largest paranasal pneumatic cavity situated within the body of the maxilla, communicating with the middle nasal meatus.",
        "defAr": "أكبر الجيوب الهوائية جانب الأنفية، يقع داخل جسم الفك العلوي ويتصل بالصماخ الأنفي الأوسط.",
        "source": "TERMINOLOGIA_ANATOMICA"
    },
    {
        "cui": "C0149758",
        "category": "PATHOLOGY",
        "subspecialty": "ENDODONTICS",
        "latinName": "Abscessus periapicalis",
        "termEn": "Periapical abscess",
        "termsEnExtra": ["Apical abscess", "Dentoalveolar abscess"],
        "termAr": "خراج ذروي",
        "termsArExtra": ["خراج حول الذروة"],
        "defEn": "An acute or chronic suppurative collection of purulent exudate around the apex of a non-vital tooth.",
        "defAr": "تجمع قيحي التهابي حاد أو مزمن محيط بذروة جذر سن متموت اللب.",
        "source": "MESH"
    },
    {
        "cui": "C0000970",
        "category": "PHARMACOLOGY",
        "subspecialty": "ANALGESICS",
        "latinName": "Paracetamolum",
        "termEn": "Paracetamol",
        "termsEnExtra": ["Acetaminophen"],
        "termAr": "باراسيتامول",
        "termsArExtra": ["أسيتامينوفين"],
        "defEn": "A centrally acting analgesic and antipyretic agent recommended for managing mild-to-moderate odontogenic pain.",
        "defAr": "مسكن ألم وخافض حرارة ذو تأثير مركزي، يُعد خطاً أولياً لتسكين الآلام السنية الخفيفة إلى المتوسطة.",
        "source": "WHO_EML"
    },
    {
        "cui": "C0039485",
        "category": "ANATOMY",
        "subspecialty": "MAXILLOFACIAL_ANATOMY",
        "latinName": "Articulatio temporomandibularis",
        "termEn": "Temporomandibular joint",
        "termsEnExtra": ["TMJ"],
        "termAr": "المفصل الفكي الصدغي",
        "termsArExtra": ["مفصل فكي صدغي"],
        "defEn": "A specialized bilateral synovial bicondylar ginglymoarthrodial joint connecting the mandibular condyle to the temporal squama.",
        "defAr": "مفصل زليلي مزدوج متخصص يربط لقمة الفك السفلي بصدف العظم الصدغي ويتيح حركات المضغ والكلام.",
        "source": "TERMINOLOGIA_ANATOMICA"
    }
]

def build_database():
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)

    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    cur.execute("PRAGMA foreign_keys = ON;")

    # Schema defined by Room
    cur.execute("""
        CREATE TABLE IF NOT EXISTS `medical_concepts` (
            `conceptId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `cui` TEXT,
            `category` TEXT NOT NULL,
            `subspecialty` TEXT,
            `latinName` TEXT
        );
    """)

    cur.execute("""
        CREATE TABLE IF NOT EXISTS `medical_terms` (
            `termId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `conceptId` INTEGER NOT NULL,
            `langCode` TEXT NOT NULL,
            `termText` TEXT NOT NULL,
            `isPreferred` INTEGER NOT NULL,
            `source` TEXT NOT NULL,
            FOREIGN KEY(`conceptId`) REFERENCES `medical_concepts`(`conceptId`) ON UPDATE NO ACTION ON DELETE CASCADE
        );
    """)
    cur.execute("CREATE INDEX IF NOT EXISTS `index_medical_terms_conceptId` ON `medical_terms` (`conceptId`);")
    cur.execute("CREATE INDEX IF NOT EXISTS `index_medical_terms_termText` ON `medical_terms` (`termText`);")
    cur.execute("CREATE INDEX IF NOT EXISTS `index_medical_terms_langCode_termText` ON `medical_terms` (`langCode`, `termText`);")

    cur.execute("""
        CREATE TABLE IF NOT EXISTS `medical_definitions` (
            `defId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `conceptId` INTEGER NOT NULL,
            `definitionEn` TEXT,
            `definitionAr` TEXT,
            FOREIGN KEY(`conceptId`) REFERENCES `medical_concepts`(`conceptId`) ON UPDATE NO ACTION ON DELETE CASCADE
        );
    """)
    cur.execute("CREATE INDEX IF NOT EXISTS `index_medical_definitions_conceptId` ON `medical_definitions` (`conceptId`);")

    # FTS4 Virtual Table & Triggers matching Room's exact specification
    cur.execute("""
        CREATE VIRTUAL TABLE IF NOT EXISTS `medical_terms_fts` USING FTS4(
            `termText` TEXT NOT NULL,
            tokenize=unicode61,
            content=`medical_terms`
        );
    """)

    cur.execute("""
        CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medical_terms_fts_BEFORE_UPDATE
        BEFORE UPDATE ON `medical_terms` BEGIN
            DELETE FROM `medical_terms_fts` WHERE `docid`=OLD.`rowid`;
        END;
    """)
    cur.execute("""
        CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medical_terms_fts_BEFORE_DELETE
        BEFORE DELETE ON `medical_terms` BEGIN
            DELETE FROM `medical_terms_fts` WHERE `docid`=OLD.`rowid`;
        END;
    """)
    cur.execute("""
        CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medical_terms_fts_AFTER_UPDATE
        AFTER UPDATE ON `medical_terms` BEGIN
            INSERT INTO `medical_terms_fts`(`docid`, `termText`) VALUES (NEW.`rowid`, NEW.`termText`);
        END;
    """)
    cur.execute("""
        CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medical_terms_fts_AFTER_INSERT
        AFTER INSERT ON `medical_terms` BEGIN
            INSERT INTO `medical_terms_fts`(`docid`, `termText`) VALUES (NEW.`rowid`, NEW.`termText`);
        END;
    """)

    # Room master table for schema identity validation
    cur.execute("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT);")
    cur.execute(f"INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, '{IDENTITY_HASH}');")

    # Seed data
    for item in SEED_CONCEPTS:
        cur.execute(
            "INSERT INTO medical_concepts (cui, category, subspecialty, latinName) VALUES (?, ?, ?, ?)",
            (item.get("cui"), item["category"], item.get("subspecialty"), item.get("latinName"))
        )
        concept_id = cur.lastrowid

        # Insert Primary English term
        cur.execute(
            "INSERT INTO medical_terms (conceptId, langCode, termText, isPreferred, source) VALUES (?, 'en', ?, 1, ?)",
            (concept_id, item["termEn"], item["source"])
        )

        # Insert Extra English terms / synonyms
        for extra in item.get("termsEnExtra", []):
            if extra.lower() != item["termEn"].lower():
                cur.execute(
                    "INSERT INTO medical_terms (conceptId, langCode, termText, isPreferred, source) VALUES (?, 'en', ?, 0, ?)",
                    (concept_id, extra, item["source"])
                )

        # Insert Primary Arabic term
        cur.execute(
            "INSERT INTO medical_terms (conceptId, langCode, termText, isPreferred, source) VALUES (?, 'ar', ?, 1, ?)",
            (concept_id, item["termAr"], item["source"])
        )

        # Insert Extra Arabic terms
        for extra_ar in item.get("termsArExtra", []):
            if extra_ar != item["termAr"]:
                cur.execute(
                    "INSERT INTO medical_terms (conceptId, langCode, termText, isPreferred, source) VALUES (?, 'ar', ?, 0, ?)",
                    (concept_id, extra_ar, item["source"])
                )

        # Insert Latin term if distinct
        latin = item.get("latinName")
        if latin and latin.lower() != item["termEn"].lower():
            cur.execute(
                "INSERT INTO medical_terms (conceptId, langCode, termText, isPreferred, source) VALUES (?, 'la', ?, 0, ?)",
                (concept_id, latin, item["source"])
            )

        # Insert Definition
        def_en = item.get("defEn")
        def_ar = item.get("defAr")
        if def_en or def_ar:
            cur.execute(
                "INSERT INTO medical_definitions (conceptId, definitionEn, definitionAr) VALUES (?, ?, ?)",
                (concept_id, def_en, def_ar)
            )

    conn.commit()

    # Rebuild FTS index to guarantee all content is indexed
    cur.execute("INSERT INTO medical_terms_fts(medical_terms_fts) VALUES('rebuild');")
    conn.commit()

    cur.execute("SELECT COUNT(*) FROM medical_concepts;")
    concepts_count = cur.fetchone()[0]

    cur.execute("SELECT COUNT(*) FROM medical_terms;")
    terms_count = cur.fetchone()[0]

    cur.execute("SELECT COUNT(*) FROM medical_definitions;")
    defs_count = cur.fetchone()[0]

    # Verify FTS query on Enamel
    cur.execute("SELECT docid, termText FROM medical_terms_fts WHERE medical_terms_fts MATCH 'Enamel*';")
    fts_results = cur.fetchall()

    conn.close()
    print(f"Successfully generated {DB_PATH}")
    print(f"Concepts: {concepts_count}, Terms: {terms_count}, Definitions: {defs_count}")
    print(f"FTS Enamel test match count: {len(fts_results)}")
    for r in fts_results:
        print("  Match:", r)

if __name__ == "__main__":
    build_database()
