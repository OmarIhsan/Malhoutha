#!/usr/bin/env python3
"""
seed_dental_subspecialties.py
=============================
Authoritative 7-Department Dental Subspecialty & WHO UMD Lexicon Ingestion Pipeline.

Populates high-yield clinical terminology across seven dental disciplines:
  1. Endodontics (معالجة جذور الأسنان)
  2. Periodontics (أمراض وجراحة اللثة والأنسجة الداعمة)
  3. Prosthodontics (الاستعاضة الصناعية والتركيبات السنية)
  4. Operative & Restorative Dentistry (مداواة وترميم الأسنان)
  5. Oral & Maxillofacial Surgery (جراحة الفم والوجه والفكين)
  6. Orthodontics (تقويم الأسنان والفكين)
  7. Oral Pathology & Oral Medicine (أمراض وطب الفم)

Target SQLite Tables:
  - medical_concepts (conceptId, cui, category, subspecialty, latinName)
  - medical_terms (termId, conceptId, langCode, termText, isPreferred, source)
  - medical_definitions (defId, conceptId, definitionEn, definitionAr)
  - medical_terms_fts (rowid, termText)
"""

import argparse
import os
import sqlite3
import sys
import time
from typing import Dict, List, Optional, Tuple, Any

# Ensure stdout handles UTF-8 characters safely on Windows console
if sys.stdout.encoding != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

DEFAULT_DB_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "src", "main", "assets", "databases", "medical_lexicon.db"
)

# =============================================================================
# COMPREHENSIVE 7-DEPARTMENT CLINICAL DENTAL CORPUS
# =============================================================================

DENTAL_CONCEPTS_DATA: List[Dict[str, Any]] = [
    # =========================================================================
    # DEPARTMENT 1: ENDODONTICS (معالجة جذور الأسنان)
    # =========================================================================
    {
        "cui": "DENT_ENDO_001",
        "category": "PROCEDURE",
        "subspecialty": "Endodontics",
        "latinName": "Canalis radicis",
        "termsEn": ["working length", "root canal working length", "WL"],
        "termsAr": ["الطول العامل", "طول القناة العامل", "طول العمل السني"],
        "defEn": "The distance from a coronal reference point to the point at which canal preparation and obturation should terminate (usually 0.5-1mm short of apical foramen).",
        "defAr": "المسافة من نقطة مرجعية تاجية إلى النقطة التي يجب أن ينتهي عندها تحضير القناة وحشوها (عادة قبل 0.5-1 مم من الثقبة الذروية).",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_002",
        "category": "PROCEDURE",
        "subspecialty": "Endodontics",
        "latinName": None,
        "termsEn": ["obturation", "root canal obturation", "canal filling"],
        "termsAr": ["حشو القناة السنية", "السد القنوي", "حشو جذور الأسنان"],
        "defEn": "The three-dimensional hermetic filling and sealing of the entire cleaned and shaped root canal system.",
        "defAr": "الحشو الإحكامي ثلاثي الأبعاد لكامل الجهاز القنوي بعد تنظيفه وتشكيله لمنع التسرب الإنتاني.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_003",
        "category": "PROCEDURE",
        "subspecialty": "Endodontics",
        "latinName": None,
        "termsEn": ["gutta-percha", "guttapercha", "gutta percha", "GP point", "GP cone"],
        "termsAr": ["الكوتابيركا", "الطبرخي", "أقماع الكوتابيركا", "حشوة الكوتابيركا"],
        "defEn": "The trans-isomer of polyisoprene used as the primary thermoplastic inert core obturation material in endodontic therapy.",
        "defAr": "المادة اللدنة الحرارية الخاملة الرئيسية المشتقة من أشجار البالاقيوم المستخدمة لسد قنوات الجذور السنية.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_004",
        "category": "PROCEDURE",
        "subspecialty": "Endodontics",
        "latinName": None,
        "termsEn": ["apexification", "apical barrier technique"],
        "termsAr": ["تكوين قمة الجذر", "إغلاق ذروة الجذر", "تشكيل الذروة الصناعي"],
        "defEn": "A method to induce a calcified barrier in a root with an open apex or the continued apical development of an incompletely formed root in non-vital teeth.",
        "defAr": "إجراء لتحريض تشكيل حاجز كلسي ذروي في جذر غير مكتمل الذروة لسن ميت اللب للسماح بحشو القناة.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_005",
        "category": "PROCEDURE",
        "subspecialty": "Endodontics",
        "latinName": None,
        "termsEn": ["apexogenesis", "vital pulp therapy for open apex"],
        "termsAr": ["تحريض استكمال ذروة الجذر", "استكمال تكون الذروة الحيوية", "تكون الذروة الحيوي"],
        "defEn": "A vital pulp therapy procedure performed to encourage continued physiological root development and apical closure in immature permanent teeth.",
        "defAr": "علاج لبي حيوي يهدف للحفاظ على حيوية لب الجذر لتمكين استمرار نموه الطبيعي وإغلاق ذروته في الأسنان الفتية.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_006",
        "category": "DIAGNOSTIC",
        "subspecialty": "Endodontics",
        "latinName": "Pulpa dentis",
        "termsEn": ["pulp vitality", "pulp sensibility", "pulp vitality test"],
        "termsAr": ["حيوية اللب السني", "فحص حيوية اللب", "اختبار استجابة اللب"],
        "defEn": "Assessment of the health and vascular supply of the dental pulp, evaluated clinically via thermal, electrical, or laser Doppler tests.",
        "defAr": "تقييم التروية الدموية وصحة النسيج اللبي عبر الفحوص الحرارية والكهربائية السريرية.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_007",
        "category": "PHARMACOLOGY",
        "subspecialty": "Endodontics",
        "latinName": None,
        "termsEn": ["sodium hypochlorite", "NaOCl", "endodontic bleach"],
        "termsAr": ["تحت كلوريت الصوديوم", "هيبوكلوريت الصوديوم", "محلول إرواء القنوات"],
        "defEn": "The gold-standard endodontic irrigant exhibiting broad antimicrobial efficacy and unique organic tissue-dissolving capacity.",
        "defAr": "سائل الإرواء المعياري الذهبي في معالجة الجذور الذي يذيب النسج العضوية ويمتلك قدرة قاتلة للجراثيم.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_008",
        "category": "PHARMACOLOGY",
        "subspecialty": "Endodontics",
        "latinName": None,
        "termsEn": ["EDTA", "ethylenediaminetetraacetic acid", "chelating agent"],
        "termsAr": ["حمض إيثيلين ثنائي أمين رباعي حمض الأسيتيك", "إيدتا", "عامل استخلاب الكالسيوم"],
        "defEn": "A calcium-chelating agent (typically 17%) used to remove the inorganic component of the smear layer and demineralize dentin.",
        "defAr": "عامل استخلاب كيميائي يُستخدم لنزع الكالسيوم وحل المكونات اللاعضوية من طبقة اللطاخة داخل القنوات.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_009",
        "category": "PATHOLOGY",
        "subspecialty": "Endodontics",
        "latinName": None,
        "termsEn": ["smear layer", "endodontic smear layer"],
        "termsAr": ["طبقة اللطاخة", "مسحوق البرادة", "طبقة البقايا السطحية"],
        "defEn": "A microcrystalline film of dentinal debris, organic tissue, and bacteria formed on root canal walls during mechanical instrumentation.",
        "defAr": "طبقة مجهرية رقيقة من برادة العاج والنسج العضوية والجراثيم تتشكل على جدران القنوات أثناء البرد الآلي أو اليدوي.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_010",
        "category": "PATHOLOGY",
        "subspecialty": "Endodontics",
        "latinName": None,
        "termsEn": ["ledge", "canal ledge", "ledging"],
        "termsAr": ["حافة اصطناعية في القناة", "درج قنوي", "حافة جانبية"],
        "defEn": "An iatrogenic internal irregularity or shelf created on the outer wall of a curved canal preventing instruments from reaching the apex.",
        "defAr": "عائق أو درجة اصطناعية تتكون داخل جدار القناة المنحنية نتيجة خطأ في التقنية تمنع وصول المبارد للذروة.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_011",
        "category": "PATHOLOGY",
        "subspecialty": "Endodontics",
        "latinName": None,
        "termsEn": ["zipping", "apical zipping", "apical transportation"],
        "termsAr": ["انثقاب ذروي مروحي", "تمزق الذروة", "تغير مسار الذروة"],
        "defEn": "An iatrogenic elliptical transposition and enlargement of the apical foramen along the outer curvature caused by rigid file rotation.",
        "defAr": "تشوه إهليلجي ذروي ينتج عن انحراف البرد ونقل الثقبة الذروية عن مسارها التشريحي الأصلي.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_012",
        "category": "PROCEDURE",
        "subspecialty": "Endodontics",
        "latinName": "Foramen apicale",
        "termsEn": ["apical patency", "patency file"],
        "termsAr": ["نفاذية الذروة", "سالكية الذروة", "مبرد السالكية"],
        "defEn": "The clinical technique of maintaining the apical constriction free of packed dentinal debris by gently inserting a small K-file through the foramen.",
        "defAr": "تقنية الحفاظ على سالكية الثقبة الذروية خالية من برادة العاج عبر تمرير مبرد دقيق برفق عبر نهاية الجذر.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_013",
        "category": "PROCEDURE",
        "subspecialty": "Endodontics",
        "latinName": None,
        "termsEn": ["lateral condensation", "cold lateral compaction"],
        "termsAr": ["تكثيف جانبي", "التكثيف الجانبي للكوتابيركا", "رص جانبي"],
        "defEn": "The classic endodontic obturation technique using a finger spreader to compact accessory gutta-percha cones alongside a master cone.",
        "defAr": "تقنية رص قمع الكوتابيركا الرئيسي جانبياً باستخدام موزع يدوي لإضافة أقماع مساعدة وسد كامل الفراغ.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_014",
        "category": "PATHOLOGY",
        "subspecialty": "Endodontics",
        "latinName": "Pulpitis",
        "termsEn": ["pulpitis", "reversible pulpitis", "irreversible pulpitis"],
        "termsAr": ["التهاب اللب", "التهاب اللب العكوس", "التهاب اللب غير العكوس"],
        "defEn": "Inflammation of the dental pulp tissue caused primarily by bacterial invasion from dental caries, classified clinically as reversible or irreversible.",
        "defAr": "التهاب يصيب النسيج الضام اللبي نتيجة العدوى الجرثومية المتسللة من النخر السني.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_015",
        "category": "PATHOLOGY",
        "subspecialty": "Endodontics",
        "latinName": "Periodontitis apicalis",
        "termsEn": ["apical periodontitis", "periapical periodontitis"],
        "termsAr": ["التهاب النسج حول الذروية", "التهاب الذروة السنية", "التهاب حول ذروي"],
        "defEn": "Inflammatory lesion around the tooth root apex caused by microbiological infection residing within the infected root canal system.",
        "defAr": "آفة التهابية تحيط بذروة جذر السن سببها الجراثيم المتوغلة في الجهاز القنوي للسن المتموت.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_016",
        "category": "PROCEDURE",
        "subspecialty": "Endodontics",
        "latinName": "Resectio apicalis",
        "termsEn": ["apicoectomy", "periapical surgery", "root end resection"],
        "termsAr": ["قطع ذروة الجذر", "استئصال قمة الجذر", "جراحة حول الذروة"],
        "defEn": "Surgical excision of the apical portion of a tooth root followed by root-end cavity preparation and retrograde filling.",
        "defAr": "الاستئصال الجراحي لقمة جذر السن وتنظيف الآفة حول الذروية مع وضع حشوة راجعة في قمة الجذر.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_017",
        "category": "PHARMACOLOGY",
        "subspecialty": "Endodontics",
        "latinName": None,
        "termsEn": ["mineral trioxide aggregate", "MTA"],
        "termsAr": ["ثلاثي أكسيد المعادن التجميعي", "إم تي إيه", "ملاط التغطية الحيوية"],
        "defEn": "A hydrophilic bioceramic material with excellent biocompatibility and sealing ability used for pulp capping, perforation repair, and retrograde filling.",
        "defAr": "مادة خزفية حيوية محبة للماء تمتاز بقدرة إحكام بيولوجية فائقة تُستخدم لتغطية اللب وسد الانثقابات وحشو الذروة الجراحي.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_018",
        "category": "PATHOLOGY",
        "subspecialty": "Endodontics",
        "latinName": "Resorptio radicis",
        "termsEn": ["root resorption", "internal resorption", "external resorption"],
        "termsAr": ["امتصاص الجذر", "امتصاص جذري داخلي", "امتصاص جذري خارجي"],
        "defEn": "Pathological or physiological loss of cementum and dentin caused by osteoclastic/odontoclastic cellular activity.",
        "defAr": "تآكل مرضي يصيب ملاط وعاج السن بفعل الخلايا الكاسرة للسن إما من الداخل أو الخارج.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_019",
        "category": "PROCEDURE",
        "subspecialty": "Endodontics",
        "latinName": None,
        "termsEn": ["ferrule effect", "ferrule"],
        "termsAr": ["تأثير الطوق الحفافي", "طوق الأمان السني", "طوق التاج"],
        "defEn": "A 360-degree collar of sound dentin extending 1.5-2mm coronal to the crown preparation margin that significantly increases resistance to tooth fracture.",
        "defAr": "طوق دائري من العاج السليم بارتفاع 1.5-2 مم أعلى خط الإنهاء يقي السن المعالج لبياً من الكسر بعد تتويجه.",
        "source": "WHO_UMD_ENDODONTICS"
    },
    {
        "cui": "DENT_ENDO_020",
        "category": "PROCEDURE",
        "subspecialty": "Endodontics",
        "latinName": None,
        "termsEn": ["electronic apex locator", "apex locator", "EAL"],
        "termsAr": ["محدد الذروة الإلكتروني", "جهاز قياس الذروة", "محدد القمة السني"],
        "defEn": "An electronic diagnostic device used in endodontics to determine the position of the apical constriction and root canal working length.",
        "defAr": "جهاز إلكتروني تشخيصي يقيس المقاومة الكهربائية لتحديد موضع التضيق الذروي وحساب طول القناة العامل.",
        "source": "WHO_UMD_ENDODONTICS"
    },

    # =========================================================================
    # DEPARTMENT 2: PERIODONTICS (أمراض وجراحة اللثة والأنسجة الداعمة)
    # =========================================================================
    {
        "cui": "DENT_PERIO_001",
        "category": "ANATOMY",
        "subspecialty": "Periodontics",
        "latinName": "Periodontium",
        "termsEn": ["biologic width", "biological width", "supracrestal attached tissues"],
        "termsAr": ["العرض الحيوي", "الاتساع البيولوجي", "الأنسجة الملتصقة فوق العظم"],
        "defEn": "The physiological dimension of junctional epithelium and supracrestal connective tissue attachment (approximately 2.04 mm total) essential for periodontal health.",
        "defAr": "المسافة الفيزيولوجية الثابتة التي تشغلها الظهارة الموصلة وألياف النسيج الضام فوق الحافة العظمية (نحو 2 مم).",
        "source": "WHO_UMD_PERIODONTICS"
    },
    {
        "cui": "DENT_PERIO_002",
        "category": "ANATOMY",
        "subspecialty": "Periodontics",
        "latinName": "Epithelium junctionale",
        "termsEn": ["junctional epithelium", "JE", "epithelial attachment"],
        "termsAr": ["الظهارة الموصلة", "ظهارة الوصل اللثوي", "الارتباط الظهاري"],
        "defEn": "The specialized non-keratinized epithelium that forms the base of the gingival sulcus and attaches to the tooth surface via hemidesmosomes.",
        "defAr": "النسيج الظهاري المتخصص غير المتقرن الذي يلتصق بسطح السن بواسطة الجسيمات نصف الرابطة مشكلاً قاعدة الميزاب اللثوي.",
        "source": "WHO_UMD_PERIODONTICS"
    },
    {
        "cui": "DENT_PERIO_003",
        "category": "PATHOLOGY",
        "subspecialty": "Periodontics",
        "latinName": None,
        "termsEn": ["furcation involvement", "furcation lesion", "furcation defect"],
        "termsAr": ["إصابة مفترق الجذور", "إصابة التشعب الجذري", "آفة مفترق الجذور"],
        "defEn": "The pathological bone resorption and attachment loss occurring in the interradicular area of multi-rooted teeth resulting from periodontal disease.",
        "defAr": "امتصاص العظم وفقدان الارتباط في منطقة تشعب جذور الأسنان متعددة الجذور بسبب تقدم المرض الحول سني.",
        "source": "WHO_UMD_PERIODONTICS"
    },
    {
        "cui": "DENT_PERIO_004",
        "category": "PROCEDURE",
        "subspecialty": "Periodontics",
        "latinName": None,
        "termsEn": ["gingivectomy", "gingival excision"],
        "termsAr": ["استئصال اللثة", "قطع اللثة الجراحي", "خزع اللثة"],
        "defEn": "The surgical excision of diseased or excessive gingival tissue to eliminate pseudopockets or reshape gingival contours.",
        "defAr": "الاستئصال الجراحي للنسيج اللثوي المرضي أو المتضخم للتخلص من الجيوب اللثوية الكاذبة وإعادة تشكيل حواف اللثة.",
        "source": "WHO_UMD_PERIODONTICS"
    },
    {
        "cui": "DENT_PERIO_005",
        "category": "PROCEDURE",
        "subspecialty": "Periodontics",
        "latinName": None,
        "termsEn": ["curettage", "gingival curettage", "subgingival curettage"],
        "termsAr": ["تجريف لثوي", "كحت لثوي", "تجريف تحت اللثة"],
        "defEn": "Scraping of the inner gingival wall of a periodontal pocket to remove diseased granulation tissue and pocket lining.",
        "defAr": "كشط وتنظيف الجدار الداخلي للجيب اللثوي لإزالة النسج الحبيبية الالتهابية والمفرزات المرضية.",
        "source": "WHO_UMD_PERIODONTICS"
    },
    {
        "cui": "DENT_PERIO_006",
        "category": "PATHOLOGY",
        "subspecialty": "Periodontics",
        "latinName": None,
        "termsEn": ["infrabony pocket", "intrabony pocket", "subbony pocket"],
        "termsAr": ["جيب عظمي غائر", "جيب داخل عظمي", "جيب تحت عظمي"],
        "defEn": "A periodontal pocket where the base of the pocket is located apical to the alveolar crest, characterized by vertical bone loss.",
        "defAr": "جيب حول سني تقع قاعدته ذروياً بالنسبة لذروة الحافة السنخية، ويرتبط بامتصاص عظمي عمودي.",
        "source": "WHO_UMD_PERIODONTICS"
    },
    {
        "cui": "DENT_PERIO_007",
        "category": "PATHOLOGY",
        "subspecialty": "Periodontics",
        "latinName": None,
        "termsEn": ["clinical attachment loss", "CAL", "attachment loss"],
        "termsAr": ["فقدان الارتباط السريري", "فقد الارتباط اللثوي", "ضياع الارتباط السريري"],
        "defEn": "The clinical extent of periodontal destruction measured from the cementoenamel junction (CEJ) to the base of the pocket.",
        "defAr": "المقياس السريري لمقدار تخرب الأنسجة الداعمة مقاساً من ملتقى الميناء والملاط حتى قاع الجيب اللثوي.",
        "source": "WHO_UMD_PERIODONTICS"
    },
    {
        "cui": "DENT_PERIO_008",
        "category": "PROCEDURE",
        "subspecialty": "Periodontics",
        "latinName": None,
        "termsEn": ["scaling and root planing", "SRP", "root debridement"],
        "termsAr": ["تقليح الأسنان وكشط الجذر", "تقليح وكشط الجذور", "تنظيف الجذور السنية"],
        "defEn": "Non-surgical instrumentation of root surfaces to remove plaque, calculus, endotoxins, and contaminated diseased cementum.",
        "defAr": "إجراء غير جراحي لإزالة اللويحة والقلاح والذيفانات البكتيرية والملاط المصاب من سطوح الجذور السنية.",
        "source": "WHO_UMD_PERIODONTICS"
    },
    {
        "cui": "DENT_PERIO_009",
        "category": "PROCEDURE",
        "subspecialty": "Periodontics",
        "latinName": None,
        "termsEn": ["free gingival graft", "FGG"],
        "termsAr": ["طعم لثوي حر", "طعم لثوي ذاتي", "طعم اللثة الحر"],
        "defEn": "A mucosal autograft completely detached from the palatal donor site and transplanted to increase the width of attached gingiva.",
        "defAr": "طعم نسيجي ذاتي مأخوذ من قبة الحنك يُنقل لزيادة عرض اللثة الملتصقة حول الأسنان أو الزرعات.",
        "source": "WHO_UMD_PERIODONTICS"
    },
    {
        "cui": "DENT_PERIO_010",
        "category": "PROCEDURE",
        "subspecialty": "Periodontics",
        "latinName": None,
        "termsEn": ["guided tissue regeneration", "GTR"],
        "termsAr": ["تجديد الأنسجة الموجه", "التجدد النسيجي الموجه", "إعادة بناء الأنسجة الموجهة"],
        "defEn": "Surgical technique utilizing barrier membranes to exclude rapidly dividing epithelial cells and allow selective periodontal ligament and bone repopulation.",
        "defAr": "تقنية جراحية تستخدم أغشية حاجزة لمنع هجرة الخلايا الظهارية وتمكين خلايا الرباط السني والعظم من إعادة بناء النسيج الداعم.",
        "source": "WHO_UMD_PERIODONTICS"
    },
    {
        "cui": "DENT_PERIO_011",
        "category": "PATHOLOGY",
        "subspecialty": "Periodontics",
        "latinName": None,
        "termsEn": ["gingival recession", "recession"],
        "termsAr": ["انحسار اللثة", "تراجع اللثة", "انكشاف الجذر"],
        "defEn": "The apical migration of the gingival margin with exposure of the root surface beyond the cementoenamel junction.",
        "defAr": "تراجع حافة اللثة باتجاه الذروة مما يؤدي إلى انكشاف سطح جذر السن ما بعد ملتقى الميناء والملاط.",
        "source": "WHO_UMD_PERIODONTICS"
    },
    {
        "cui": "DENT_PERIO_012",
        "category": "DIAGNOSTIC",
        "subspecialty": "Periodontics",
        "latinName": None,
        "termsEn": ["bleeding on probing", "BOP"],
        "termsAr": ["النزف عند السبر", "نزف الفحص اللثوي", "النزف بالسبر"],
        "defEn": "A primary diagnostic clinical indicator of active subgingival inflammatory disease provoked by standardized probing.",
        "defAr": "المؤشر السريري الأساسي لوجود التهاب نشط في بطانة الجيب اللثوي عند إدخال المسبر برفق.",
        "source": "WHO_UMD_PERIODONTICS"
    },
    {
        "cui": "DENT_PERIO_013",
        "category": "PATHOLOGY",
        "subspecialty": "Periodontics",
        "latinName": None,
        "termsEn": ["peri-implantitis", "peri-implant bone loss"],
        "termsAr": ["التهاب الأنسجة حول الزرعة", "التهاب حول الزرعة السنية", "امتصاص العظم حول الغرسة"],
        "defEn": "An infectious inflammatory condition affecting the tissues surrounding an osseointegrated dental implant, characterized by progressive marginal bone loss.",
        "defAr": "التهاب إنتاني يصيب الأنسجة المحيطة بالزرعة السنية المندمجة عظمياً يترافق مع امتصاص تدريجي في العظم الداعم.",
        "source": "WHO_UMD_PERIODONTICS"
    },

    # =========================================================================
    # DEPARTMENT 3: PROSTHODONTICS (الاستعاضة الصناعية والتركيبات السنية)
    # =========================================================================
    {
        "cui": "DENT_PROSTH_001",
        "category": "PROCEDURE",
        "subspecialty": "Prosthodontics",
        "latinName": None,
        "termsEn": ["abutment", "dental abutment", "implant abutment"],
        "termsAr": ["دعامة سنية", "سن دعامة", "دعامة الزرعة السنية"],
        "defEn": "A natural tooth or dental implant fixture utilized to retain and support a fixed or removable prosthetic restoration.",
        "defAr": "السن الطبيعي أو الغرسة السنية التي تستند إليها وتثبت فوقها التعويضات السنية الثابتة أو المتحركة.",
        "source": "WHO_UMD_PROSTHODONTICS"
    },
    {
        "cui": "DENT_PROSTH_002",
        "category": "PROCEDURE",
        "subspecialty": "Prosthodontics",
        "latinName": None,
        "termsEn": ["pontic", "bridge pontic", "dummy tooth"],
        "termsAr": ["دمية الجسر", "سن صناعي بديل", "دمية التعويض"],
        "defEn": "The artificial tooth suspended between abutments in a fixed partial denture that replaces a missing anatomical tooth.",
        "defAr": "السن الصناعي المعلق في الجسر السني الثابت الذي يحل محل السن المفقود ويعوض وظيفته وشكله.",
        "source": "WHO_UMD_PROSTHODONTICS"
    },
    {
        "cui": "DENT_PROSTH_003",
        "category": "PROCEDURE",
        "subspecialty": "Prosthodontics",
        "latinName": None,
        "termsEn": ["cantilever bridge", "cantilever", "cantilever prosthesis"],
        "termsAr": ["جسر ناتئ", "جسر ذراعي معلق", "تعويض ناتئ"],
        "defEn": "A fixed partial denture in which the pontic is retained and supported on only one end by one or more abutment teeth.",
        "defAr": "جسر سني ثابت تدعم فيه دمية الجسر وتثبت من طرف واحد فقط بالدعامة دون استناد من الطرف الآخر.",
        "source": "WHO_UMD_PROSTHODONTICS"
    },
    {
        "cui": "DENT_PROSTH_004",
        "category": "PROCEDURE",
        "subspecialty": "Prosthodontics",
        "latinName": None,
        "termsEn": ["finish line", "preparation margin", "marginal finish line"],
        "termsAr": ["خط الإنهاء", "خط الختم التحضيري", "حافة التحضير"],
        "defEn": "The peripheral terminal boundary of a tooth preparation where the artificial restoration margin seats against natural tooth structure.",
        "defAr": "الحد المحيطي النهائي لتحضير السن الذي تلتقي عنده حافة التاج الصناعي مع بنية السن الطبيعية.",
        "source": "WHO_UMD_PROSTHODONTICS"
    },
    {
        "cui": "DENT_PROSTH_005",
        "category": "PROCEDURE",
        "subspecialty": "Prosthodontics",
        "latinName": None,
        "termsEn": ["chamfer", "chamfer finish line", "chamfer preparation"],
        "termsAr": ["شطب حفافي", "تشامفر", "شطب منحنٍ"],
        "defEn": "A concave marginal finish line design featuring an obtuse internal angle, widely indicated for metal-ceramic and all-ceramic restorations.",
        "defAr": "خط إنهاء مقعر ذو زاوية داخلية منفرجة يوفر سماكة حفافية مناسبة وهو الخيار المفضل للتيجان الخزفية والمعدنية.",
        "source": "WHO_UMD_PROSTHODONTICS"
    },
    {
        "cui": "DENT_PROSTH_006",
        "category": "PROCEDURE",
        "subspecialty": "Prosthodontics",
        "latinName": None,
        "termsEn": ["shoulder", "shoulder finish line", "90 degree shoulder"],
        "termsAr": ["كتف تحضيري", "خط إنهاء مستقيم", "كتف بزاوية 90 درجة"],
        "defEn": "A flat 90-degree butt-joint finish line margin providing maximum bulk for aesthetic porcelain margins in all-ceramic crowns.",
        "defAr": "خط إنهاء مستوٍ يصنع زاوية قائمة (90 درجة) مع محور السن يؤمن سماكة كافية لحافة الخزف التجميلية.",
        "source": "WHO_UMD_PROSTHODONTICS"
    },
    {
        "cui": "DENT_PROSTH_007",
        "category": "PROCEDURE",
        "subspecialty": "Prosthodontics",
        "latinName": None,
        "termsEn": ["convergence angle", "total occlusal convergence", "TOC"],
        "termsAr": ["زاوية التقارب", "زاوية التحديب التحضيرية", "تقارب الجدران"],
        "defEn": "The angle formed between opposing axial walls of a prepared tooth, ideally between 6 to 12 degrees to maximize retention and resistance.",
        "defAr": "الزاوية المحصورة بين الجدران المحورية المتقابلة للسن المحضر، والمثالية منها تتراوح بين 6 إلى 12 درجة لضمان التثبيت.",
        "source": "WHO_UMD_PROSTHODONTICS"
    },
    {
        "cui": "DENT_PROSTH_008",
        "category": "PROCEDURE",
        "subspecialty": "Prosthodontics",
        "latinName": None,
        "termsEn": ["facebow", "face-bow transfer"],
        "termsAr": ["قوس الوجه", "نقل قوس الوجه", "القوس الوجهي"],
        "defEn": "A caliper-like caliper device used to record the spatial relationship of the maxillary arch to the transverse horizontal hinge axis of the TMJ.",
        "defAr": "أداة عيارية تُستخدم لتسجيل علاقة الفك العلوي الفراغية بمحور المفصل الفكي الصدغي ونقلها للمطبق الآلي.",
        "source": "WHO_UMD_PROSTHODONTICS"
    },
    {
        "cui": "DENT_PROSTH_009",
        "category": "DIAGNOSTIC",
        "subspecialty": "Prosthodontics",
        "latinName": None,
        "termsEn": ["centric relation", "CR"],
        "termsAr": ["العلاقة المركزية", "العلاقة المركزية الفكية", "الوضعية المفصلية المركزية"],
        "defEn": "The most anterior-superior, physiologically reproducible position of the mandibular condyles in the glenoid fossa against the articular eminence.",
        "defAr": "العلاقة الفكية المرجعية الأكثر تكراراً واستقراراً حيث تتموضع اللقيمات في أعلى وأمام الحفر الفكية الصدغية.",
        "source": "WHO_UMD_PROSTHODONTICS"
    },
    {
        "cui": "DENT_PROSTH_010",
        "category": "PROCEDURE",
        "subspecialty": "Prosthodontics",
        "latinName": None,
        "termsEn": ["post and core", "dowel and core", "cast post"],
        "termsAr": ["الوتد والقلب", "وتد وجذع سني", "وتد الجذر"],
        "defEn": "A prosthetic anchor placed inside a devitalized root canal to provide retention for a coronal core and subsequent full-coverage crown.",
        "defAr": "دعامة وتدية تُثبت داخل قناة الجذر المعالج لبياً لتأمين تثبيت القلب التعويضي والتاج النهائي.",
        "source": "WHO_UMD_PROSTHODONTICS"
    },
    {
        "cui": "DENT_PROSTH_011",
        "category": "PROCEDURE",
        "subspecialty": "Prosthodontics",
        "latinName": None,
        "termsEn": ["path of insertion", "path of placement"],
        "termsAr": ["مسار الإدخال", "مسار الإدخال والجلوس", "مسار الانطباق"],
        "defEn": "The specific directional path along which a rigid prosthesis is seated or removed without interference from tooth undercuts.",
        "defAr": "المسار الاتجاهي المحدد الذي يسلكه التعويض الصناعي أثناء تركيبه أو نزعه دون إعاقة من التحدبات السنية.",
        "source": "WHO_UMD_PROSTHODONTICS"
    },

    # =========================================================================
    # DEPARTMENT 4: OPERATIVE & RESTORATIVE DENTISTRY (مداواة وترميم الأسنان)
    # =========================================================================
    {
        "cui": "DENT_OPER_001",
        "category": "PROCEDURE",
        "subspecialty": "Operative Dentistry",
        "latinName": None,
        "termsEn": ["etching", "acid etching", "phosphoric acid etching"],
        "termsAr": ["خرش حمضي", "تخريش حمضي", "التخريش بالحمض الفوسفوري"],
        "defEn": "Application of 35-37% phosphoric acid to enamel and dentin to dissolve mineral content and create microporosities for resin micromechanical retention.",
        "defAr": "تطبيق حمض الفوسفور بنسبة 35-37% على الميناء والعاج لحل المواد المعدنية وخلق مسامات مجهرية للتثبيت الميكانيكي.",
        "source": "WHO_UMD_OPERATIVE"
    },
    {
        "cui": "DENT_OPER_002",
        "category": "PROCEDURE",
        "subspecialty": "Operative Dentistry",
        "latinName": None,
        "termsEn": ["priming", "dentin priming", "dentin primer"],
        "termsAr": ["تهيئة الروابط العاجية", "وضع البرايمر", "تبليل العاج"],
        "defEn": "Application of bifunctional hydrophilic monomers (e.g., HEMA) in an organic solvent to re-expand collapsed collagen fibrils and displace moisture.",
        "defAr": "تطبيق مونومرات محبة للماء ومذيبة لإعادة فتح شبكة ألياف الكولاجين العاجية المنكمشة وتهيئتها للمادة اللاصقة.",
        "source": "WHO_UMD_OPERATIVE"
    },
    {
        "cui": "DENT_OPER_003",
        "category": "ANATOMY",
        "subspecialty": "Operative Dentistry",
        "latinName": None,
        "termsEn": ["hybrid layer", "resin-dentin interdiffusion zone"],
        "termsAr": ["الطبقة الهجينة", "منطقة التداخل الراتينجي العاجي", "طبقة الامتزاج"],
        "defEn": "The molecular-level composite layer formed by the micromechanical interdiffusion of adhesive resin monomer within acid-demineralized collagen meshwork.",
        "defAr": "الطبقة المجهرية المتشكلة من تغلغل المادة الرابطة الراتينجية وتصلبها داخل شبكة ألياف كولاجين العاج المخروش.",
        "source": "WHO_UMD_OPERATIVE"
    },
    {
        "cui": "DENT_OPER_004",
        "category": "PATHOLOGY",
        "subspecialty": "Operative Dentistry",
        "latinName": None,
        "termsEn": ["microleakage", "marginal leakage"],
        "termsAr": ["تسرب حفافي مجهري", "تسرب مجهري", "رشح حفافي"],
        "defEn": "The microscopic clinically undetectable passage of bacteria, oral fluids, molecules, and ions between cavity walls and restorative materials.",
        "defAr": "النفوذ المجهري للجراثيم واللعاب والجزيئات بين جدار الحفرة السنية ومادة الحشوة عبر فجوات حفافية دقيقة.",
        "source": "WHO_UMD_OPERATIVE"
    },
    {
        "cui": "DENT_OPER_005",
        "category": "PROCEDURE",
        "subspecialty": "Operative Dentistry",
        "latinName": None,
        "termsEn": ["bevel", "enamel bevel", "cavosurface bevel"],
        "termsAr": ["شطب الميناء", "شطب حفافي للحفرة", "شطبة الحافة المينائية"],
        "defEn": "An angled cut prepared at the cavosurface margin that exposes the ends of enamel rods to increase bond strength and enhance aesthetic blend.",
        "defAr": "شطف حافة الميناء بزاوية مائلة لقطع نهايات الموشورات المينائية رأسياً وزيادة قوة الالتصاق والانسجام التجميلي.",
        "source": "WHO_UMD_OPERATIVE"
    },
    {
        "cui": "DENT_OPER_006",
        "category": "PROCEDURE",
        "subspecialty": "Operative Dentistry",
        "latinName": None,
        "termsEn": ["cavity outline", "outline form"],
        "termsAr": ["مخطط الحفرة", "حدود الحفرة السنية", "الشكل الخارجي للحفرة"],
        "defEn": "The planned peripheral perimeter shape of a prepared cavity on the surface of a tooth determined by extent of caries and anatomy.",
        "defAr": "المحيط الخارجي المقرر لجدران الحفرة السنية على سطح السن بناءً على امتداد النخر والتشريح السني.",
        "source": "WHO_UMD_OPERATIVE"
    },
    {
        "cui": "DENT_OPER_007",
        "category": "PROCEDURE",
        "subspecialty": "Operative Dentistry",
        "latinName": None,
        "termsEn": ["C-factor", "cavity configuration factor"],
        "termsAr": ["معامل التكوين", "عامل التقلص السطحي", "عامل تكوين الحفرة"],
        "defEn": "The ratio of bonded restoration surface area to unbonded (free) surface area, directly dictating polymerization shrinkage stress.",
        "defAr": "نسبة عدد السطوح الملتصقة لمادة الحشوة إلى السطوح الحرة غير الملتصقة؛ كلما زادت زادت قوى الشد والتقلص.",
        "source": "WHO_UMD_OPERATIVE"
    },
    {
        "cui": "DENT_OPER_008",
        "category": "PROCEDURE",
        "subspecialty": "Operative Dentistry",
        "latinName": None,
        "termsEn": ["polymerization shrinkage", "volumetric shrinkage"],
        "termsAr": ["تقلص البلمرة", "انكماش البلمرة الراتينجية", "تقلص التصلب"],
        "defEn": "The physical reduction in volume occurring as composite resin monomers polymerize into polymer chains, creating tensile stress at margins.",
        "defAr": "النقصان الحجمي الطبيعي الذي يحدث لمادة الكومبوزيت أثناء تصلب مونومراتها مما يولد إجهادات شد على الحواف.",
        "source": "WHO_UMD_OPERATIVE"
    },
    {
        "cui": "DENT_OPER_009",
        "category": "ANATOMY",
        "subspecialty": "Operative Dentistry",
        "latinName": None,
        "termsEn": ["smear plug", "dentinal smear plug"],
        "termsAr": ["سدادة اللطاخة", "سدادة العاج المجهرية", "سدادات قنيات العاج"],
        "defEn": "Compacted dentinal debris driven into the orifices of dentinal tubules during cavity preparation that reduces dentin permeability.",
        "defAr": "كتل من برادة العاج تُدفع داخل فوهات الأنابيب العاجية أثناء الحفر مما يقلل نفاذية العاج مؤقتاً.",
        "source": "WHO_UMD_OPERATIVE"
    },
    {
        "cui": "DENT_OPER_010",
        "category": "PROCEDURE",
        "subspecialty": "Operative Dentistry",
        "latinName": None,
        "termsEn": ["composite resin", "dental composite", "resin composite"],
        "termsAr": ["راتينج مركب", "حشوة تجميلية", "كومبوزيت الأسنان"],
        "defEn": "Tooth-colored restorative material composed of an organic resin matrix, inorganic filler particles, and a silane coupling agent.",
        "defAr": "مادة ترميمية بلون الأسنان تتكون من مصفوفة راتينجية عضوية وجزيئات حشوية لاعضوية ومادة رابطة سيلانية.",
        "source": "WHO_UMD_OPERATIVE"
    },
    {
        "cui": "DENT_OPER_011",
        "category": "DIAGNOSTIC",
        "subspecialty": "Operative Dentistry",
        "latinName": None,
        "termsEn": ["Black's Class I", "Class I cavity", "Class I restoration"],
        "termsAr": ["تصنيف بلاك الأول", "حفر الصنف الأول", "نخر الوهاد والشقوق"],
        "defEn": "Cavities involving the pits, fissures, and grooves of occlusal surfaces of molars and premolars, and lingual pits of maxillary incisors.",
        "defAr": "نخور تصيب الوهاد والشقوق الميزابية على السطوح الطاحنة للأضراس والسطوح اللسانية للقواطع العلوية.",
        "source": "WHO_UMD_OPERATIVE"
    },
    {
        "cui": "DENT_OPER_012",
        "category": "DIAGNOSTIC",
        "subspecialty": "Operative Dentistry",
        "latinName": None,
        "termsEn": ["Black's Class II", "Class II cavity", "Class II restoration"],
        "termsAr": ["تصنيف بلاك الثاني", "حفر الصنف الثاني", "نخر السطوح الملاصقة الخلفية"],
        "defEn": "Cavities involving the proximal (mesial or distal) surfaces of posterior teeth (premolars and molars).",
        "defAr": "نخور تصيب السطوح الملاصقة (الإنسية أو الوحشية) للأسنان الخلفية (الضواحك والأرحاء).",
        "source": "WHO_UMD_OPERATIVE"
    },

    # =========================================================================
    # DEPARTMENT 5: ORAL & MAXILLOFACIAL SURGERY (جراحة الفم والوجه والفكين)
    # =========================================================================
    {
        "cui": "DENT_SURG_001",
        "category": "PATHOLOGY",
        "subspecialty": "Oral Surgery",
        "latinName": "Alveolitis",
        "termsEn": ["dry socket", "alveolar osteitis", "alveolitis", "fibrinolytic alveolitis"],
        "termsAr": ["التهاب السنخ الجاف", "التهاب العظم السنخي", "السنخ الجاف", "خلو السنخ"],
        "defEn": "Painful postoperative complication caused by premature lysis or dislodgement of the blood clot from the extraction socket.",
        "defAr": "مضاعفة مؤلمة تلي خلع الأسنان تنتج عن انحلال أو فقدان الخثرة الدموية من السنخ وانكشاف العظم العاري.",
        "source": "WHO_UMD_SURGERY"
    },
    {
        "cui": "DENT_SURG_002",
        "category": "PATHOLOGY",
        "subspecialty": "Oral Surgery",
        "latinName": None,
        "termsEn": ["impaction", "impacted tooth", "dental impaction"],
        "termsAr": ["انحشار السن", "سن مطمور", "انحشار الأسنان"],
        "defEn": "Failure of a tooth to erupt into the normal dental arch position within the physiological time limit due to obstruction by bone or teeth.",
        "defAr": "فشل بزوغ السن في موقعه الطبيعي في القوس السنية بسبب إعاقة عظمية أو سنية مجاورة.",
        "source": "WHO_UMD_SURGERY"
    },
    {
        "cui": "DENT_SURG_003",
        "category": "PATHOLOGY",
        "subspecialty": "Oral Surgery",
        "latinName": "Luxatio dentis",
        "termsEn": ["luxation", "tooth luxation", "dental luxation"],
        "termsAr": ["خلع جزئي للسن", "قلقلة السن الرضية", "انزياح السن الرضي"],
        "defEn": "Traumatic displacement of a tooth from its alveolus with disruption of the periodontal ligament attachment.",
        "defAr": "انزياح السن الرضي عن مكانه الطبيعي في السنخ المترافق مع تمزق ألياف الرباط الحول سني.",
        "source": "WHO_UMD_SURGERY"
    },
    {
        "cui": "DENT_SURG_004",
        "category": "PROCEDURE",
        "subspecialty": "Oral Surgery",
        "latinName": None,
        "termsEn": ["dental elevator", "elevator", "luxator"],
        "termsAr": ["رافعة الأسنان", "رافعة جراحية", "كاشطة الجذور"],
        "defEn": "Surgical leverage instrument used to luxate teeth, expand alveolar bone, and retrieve fractured retained root fragments.",
        "defAr": "أداة رافعة جراحية تعتمد مبدأ الرافعة والوتد لتوسيع العظم السنخي وقلقلة الأسنان وخلع الجذور الغائرة.",
        "source": "WHO_UMD_SURGERY"
    },
    {
        "cui": "DENT_SURG_005",
        "category": "PROCEDURE",
        "subspecialty": "Oral Surgery",
        "latinName": None,
        "termsEn": ["flap reflection", "flap elevation", "mucoperiosteal flap"],
        "termsAr": ["رد الشريحة الجراحية", "رفع الشريحة المخاطية السمحاقية", "عكس الشريحة"],
        "defEn": "Surgical separation and elevation of mucoperiosteum from cortical bone surface to obtain clean visual and manual surgical access.",
        "defAr": "فصل ورفع شريحة الغشاء المخاطي والسمحاق عن سطح العظم لتأمين مدخل ورؤية جراحية واضحة للتدخل العظمي.",
        "source": "WHO_UMD_SURGERY"
    },
    {
        "cui": "DENT_SURG_006",
        "category": "PATHOLOGY",
        "subspecialty": "Oral Surgery",
        "latinName": None,
        "termsEn": ["paresthesia", "nerve paresthesia", "sensory deficit"],
        "termsAr": ["مذل", "تنمل حسي", "خدر عصبي"],
        "defEn": "An abnormal burning, prickling, or numb neurosensory sensation caused by injury to sensory nerves (e.g., inferior alveolar nerve).",
        "defAr": "شعور حسي غير طبيعي بالتنميل والخدر ناتج عن أذية أو ضغط على أحد الأعصاب الحسية في الفك.",
        "source": "WHO_UMD_SURGERY"
    },
    {
        "cui": "DENT_SURG_007",
        "category": "PROCEDURE",
        "subspecialty": "Oral Surgery",
        "latinName": None,
        "termsEn": ["osteotomy", "bone cutting"],
        "termsAr": ["قطع عظمي", "بضع العظم الجراحي", "شق العظم"],
        "defEn": "Surgical resection, transection, or division of bone performed in orthognathic, reconstructive, or surgical extraction procedures.",
        "defAr": "القطع أو الشق الجراحي للعظم لتغيير وضعه أو إزالة عوائق عظمية أثناء الجراحة التقويمية أو خلع الأسنان.",
        "source": "WHO_UMD_SURGERY"
    },
    {
        "cui": "DENT_SURG_008",
        "category": "PATHOLOGY",
        "subspecialty": "Oral Surgery",
        "latinName": "Trismus",
        "termsEn": ["trismus", "lockjaw", "limited mouth opening"],
        "termsAr": ["ضزز", "ضيق فتح الفم", "تشنج الفك"],
        "defEn": "Inability to open the mouth fully caused by tonic muscle spasms of masticatory muscles secondary to infection, trauma, or local anesthesia.",
        "defAr": "صعوبة أو محدودية شديدة في فتح الفم تنتج عن تشنج عضلات المضغ بسبب التهاب أو خلع جراحي للأرحاء.",
        "source": "WHO_UMD_SURGERY"
    },
    {
        "cui": "DENT_SURG_009",
        "category": "PROCEDURE",
        "subspecialty": "Oral Surgery",
        "latinName": None,
        "termsEn": ["alveoloplasty", "alveolectomy", "ridge smoothing"],
        "termsAr": ["تشكيل العظم السنخي", "رأب السنخ", "تسوية الحافة السنخية"],
        "defEn": "Surgical recontouring, re-shaping, and smoothing of the alveolar ridge following extractions to prepare for prosthodontics.",
        "defAr": "تسوية وإعادة تشكيل الحواف العظمية السنخية الحادة جراحياً بعد الخلع لتحضير الفك لاستقبال التعويضات.",
        "source": "WHO_UMD_SURGERY"
    },

    # =========================================================================
    # DEPARTMENT 6: ORTHODONTICS (تقويم الأسنان والفكين)
    # =========================================================================
    {
        "cui": "DENT_ORTHO_001",
        "category": "PATHOLOGY",
        "subspecialty": "Orthodontics",
        "latinName": "Malocclusio",
        "termsEn": ["malocclusion", "dental malocclusion"],
        "termsAr": ["سوء الإطباق", "سوء إطباق الأسنان", "خلل الإطباق"],
        "defEn": "Any deviation from normal, physiologically ideal occlusion of the maxillary and mandibular dental arches.",
        "defAr": "أي اضطراب أو شذوذ عن الإطباق المثالي الطبيعي في توضع الأسنان أو العلاقة بين القوسين السنيتين.",
        "source": "WHO_UMD_ORTHODONTICS"
    },
    {
        "cui": "DENT_ORTHO_002",
        "category": "PROCEDURE",
        "subspecialty": "Orthodontics",
        "latinName": None,
        "termsEn": ["anchorage", "orthodontic anchorage"],
        "termsAr": ["إرساء تقويمي", "نقطة استناد تقويمية", "التثبيت التقويمي"],
        "defEn": "The nature and degree of resistance to reciprocal unwanted tooth movement exhibited by an anatomical unit.",
        "defAr": "مقاومة الأسنان أو العظام للحركات التفاعلية غير المرغوبة أثناء تطبيق القوى التقويمية لنقل أسنان أخرى.",
        "source": "WHO_UMD_ORTHODONTICS"
    },
    {
        "cui": "DENT_ORTHO_003",
        "category": "DIAGNOSTIC",
        "subspecialty": "Orthodontics",
        "latinName": None,
        "termsEn": ["cephalometrics", "cephalometric", "cephalometric analysis"],
        "termsAr": ["القياسات السيفالومترية", "تحليل سيفالومتري", "قياس الرأس الشعاعي"],
        "defEn": "The scientific diagnostic study and measurement of skeletal, dental, and soft tissue landmarks on standardized lateral skull radiographs.",
        "defAr": "دراسة وقياس الزوايا والأبعاد العظمية والسنية على صور الجمجمة الشعاعية الجانبية لتشخيص الشذوذات الوجهية.",
        "source": "WHO_UMD_ORTHODONTICS"
    },
    {
        "cui": "DENT_ORTHO_004",
        "category": "PROCEDURE",
        "subspecialty": "Orthodontics",
        "latinName": None,
        "termsEn": ["bracket", "orthodontic bracket", "dental brace"],
        "termsAr": ["حاصرة تقويمية", "حاصرة الأسنان", "قفل تقويمي"],
        "defEn": "An orthodontic attachment bonded directly to a tooth to transmit force from the active archwire to the tooth.",
        "defAr": "قطعة معدنية أو خزفية صغيرة تُثبت على سطح السن لتوجيه السلك القوسي ونقل القوى التقويمية للسن.",
        "source": "WHO_UMD_ORTHODONTICS"
    },
    {
        "cui": "DENT_ORTHO_005",
        "category": "PROCEDURE",
        "subspecialty": "Orthodontics",
        "latinName": None,
        "termsEn": ["bodily movement", "translation", "bodily tooth movement"],
        "termsAr": ["حركة جسدية متوازية", "حركة انتقال متوازية للسن", "حركة الانتقال الجسدي"],
        "defEn": "Orthodontic movement where the crown and root of a tooth move equal distances in the same direction without tipping.",
        "defAr": "حركة سنية تقويمية ينتقل فيها التاج والجذر بنفس المقدار والاتجاه بالتوازي دون حدوث أي إمالة في محور السن.",
        "source": "WHO_UMD_ORTHODONTICS"
    },
    {
        "cui": "DENT_ORTHO_006",
        "category": "PROCEDURE",
        "subspecialty": "Orthodontics",
        "latinName": None,
        "termsEn": ["tipping", "controlled tipping", "uncontrolled tipping"],
        "termsAr": ["إمالة تيجانية", "إمالة السن التقويمية", "حركة الإمالة"],
        "defEn": "Orthodontic tooth movement produced when a single force acts on the crown, causing rotation around the center of resistance.",
        "defAr": "حركة سنية تقويمية يميل فيها تاج السن في اتجاه معين بينما يتحرك الجذر في الاتجاه المعاكس حول مركز المقاومة.",
        "source": "WHO_UMD_ORTHODONTICS"
    },
    {
        "cui": "DENT_ORTHO_007",
        "category": "DIAGNOSTIC",
        "subspecialty": "Orthodontics",
        "latinName": None,
        "termsEn": ["overjet", "horizontal overlap"],
        "termsAr": ["بروز أفقي", "بروز الأسنان الأمامية", "المسافة الأفقية بين القواطع"],
        "defEn": "The horizontal projection of the maxillary incisors beyond the mandibular incisors (normally 2-3 mm).",
        "defAr": "المسافة الأفقية التي تتقدم بها حواف القواطع العلوية أمام القواطع السفلية في حالة الإطباق (المعيار 2-3 مم).",
        "source": "WHO_UMD_ORTHODONTICS"
    },
    {
        "cui": "DENT_ORTHO_008",
        "category": "DIAGNOSTIC",
        "subspecialty": "Orthodontics",
        "latinName": None,
        "termsEn": ["overbite", "vertical overlap"],
        "termsAr": ["تراكب عمودي", "تغطية عمودية", "التراكب القاطع"],
        "defEn": "The vertical overlap of the incisal edges of the maxillary incisors over the mandibular incisors (normally 1-2 mm).",
        "defAr": "المقدار العمودي الذي تغطي فيه القواطع العلوية تيجان القواطع السفلية عند إطباق الأسنان.",
        "source": "WHO_UMD_ORTHODONTICS"
    },
    {
        "cui": "DENT_ORTHO_009",
        "category": "PATHOLOGY",
        "subspecialty": "Orthodontics",
        "latinName": "Diastema",
        "termsEn": ["diastema", "midline diastema", "space between teeth"],
        "termsAr": ["فلجة سنية", "فلجة الخط المتوسط", "تباعد الأسنان"],
        "defEn": "A space or gap separating two adjacent teeth, particularly common between the maxillary central incisors.",
        "defAr": "فراغ أو تباعد تشريحي غير طبيعي بين سنين متجاورين، وأشهرها الفلجة بين الثنيتين العلويتين.",
        "source": "WHO_UMD_ORTHODONTICS"
    },
    {
        "cui": "DENT_ORTHO_010",
        "category": "PROCEDURE",
        "subspecialty": "Orthodontics",
        "latinName": None,
        "termsEn": ["archwire", "orthodontic archwire"],
        "termsAr": ["سلك قوسي", "سلك التقويم القوسي", "القوس التقويمي"],
        "defEn": "A continuous metal wire engaged into orthodontic brackets that provides the fundamental biomechanical force to align teeth.",
        "defAr": "سلك معدني مرن يستقر داخل الحاصرات لتوليد القوى البيوميكانيكية المحركة للأسنان ورصفها.",
        "source": "WHO_UMD_ORTHODONTICS"
    },

    # =========================================================================
    # DEPARTMENT 7: ORAL PATHOLOGY & ORAL MEDICINE (أمراض وطب الفم)
    # =========================================================================
    {
        "cui": "DENT_PATH_001",
        "category": "PATHOLOGY",
        "subspecialty": "Oral Pathology",
        "latinName": "Leukoplakia",
        "termsEn": ["leukoplakia", "oral leukoplakia", "white lesion"],
        "termsAr": ["طلاوة بيضاء", "تقرن فموي أبيض", "الصداف الفموي"],
        "defEn": "A predominantly white plaque of questionable risk having excluded other known diseases or disorders that carry no increased risk for cancer.",
        "defAr": "آفة فموية بيضاء صفيحية غير قابلة للكشط لا يمكن تصنيفها تحت أي مرض سريري معروف، وتعد آفة محتملة الخباثة.",
        "source": "WHO_UMD_PATHOLOGY"
    },
    {
        "cui": "DENT_PATH_002",
        "category": "PATHOLOGY",
        "subspecialty": "Oral Pathology",
        "latinName": "Erythroplakia",
        "termsEn": ["erythroplakia", "oral erythroplakia", "red lesion"],
        "termsAr": ["طلاوة حمراء", "آفة فموية حمراء مخملية", "احمرار فموي مخاطي"],
        "defEn": "A fiery red patch that cannot be characterized clinically or pathologically as any other definable disease, carrying very high dysplasia risk.",
        "defAr": "بقعة حمراء مخملية تصيب الغشاء المخاطي الفموي وتمتاز باحتمالية تحول خبيث عالية جداً تفوق الطلاوة البيضاء.",
        "source": "WHO_UMD_PATHOLOGY"
    },
    {
        "cui": "DENT_PATH_003",
        "category": "PATHOLOGY",
        "subspecialty": "Oral Pathology",
        "latinName": "Lichen planus",
        "termsEn": ["lichen planus", "oral lichen planus", "OLP"],
        "termsAr": ["حزاز مسطح", "الحزاز المنبسط الفموي", "حزاز الفم"],
        "defEn": "A chronic T-cell mediated autoimmune inflammatory mucocutaneous disorder presenting with characteristic reticular white striae of Wickham.",
        "defAr": "مرض التهابي مناعي ذاتي مزمن يصيب المخاطية الفموية ويتميز بظهور شبكة بيضاء متشعبة تُعرف بخطوط ويكهام.",
        "source": "WHO_UMD_PATHOLOGY"
    },
    {
        "cui": "DENT_PATH_004",
        "category": "PATHOLOGY",
        "subspecialty": "Oral Pathology",
        "latinName": "Stomatitis aphthosa",
        "termsEn": ["recurrent aphthous stomatitis", "aphthous ulcer", "canker sore", "RAS"],
        "termsAr": ["قلاع فموي ناكس", "تقرحات فموية ناكسة", "قرحة قلاعية"],
        "defEn": "A common recurring condition characterized by painful, well-demarcated round ulcers with yellowish bases and erythematous borders.",
        "defAr": "تقرحات فموية دورية مؤلمة شائعة تصيب الغشاء المخاطي غير المتقرن وتتميز بهالة حمراء محيطة بقاعدة صفراء.",
        "source": "WHO_UMD_PATHOLOGY"
    },
    {
        "cui": "DENT_PATH_005",
        "category": "PATHOLOGY",
        "subspecialty": "Oral Pathology",
        "latinName": "Ameloblastoma",
        "termsEn": ["ameloblastoma", "adamantinoma"],
        "termsAr": ["ورم الأرومة المينائية", "ورم مصورات الميناء", "آدمانتينوما"],
        "defEn": "A benign but locally aggressive odontogenic epithelial neoplasm characterized by a multicystic 'soap-bubble' radiographic appearance.",
        "defAr": "ورم سني المنشأ حميد سريرياً لكنه غزير ومخرب موضعياً للعظم، يظهر شعاعياً بمظهر فقاعات الصابون.",
        "source": "WHO_UMD_PATHOLOGY"
    },
    {
        "cui": "DENT_PATH_006",
        "category": "PATHOLOGY",
        "subspecialty": "Oral Pathology",
        "latinName": "Carcinoma squamosum",
        "termsEn": ["squamous cell carcinoma", "oral squamous cell carcinoma", "OSCC"],
        "termsAr": ["سرطانة الخلايا الحرشفية", "سرطان الفم الحرشفي", "كارسينوما الخلايا الحرشفية"],
        "defEn": "The most common malignant neoplasm of the oral cavity (accounting for >90% of cases), arising from mucosal stratified squamous epithelium.",
        "defAr": "الورم الخبيث الأكثر شيوعاً في تجويف الفم وينشأ من تحول خبيث في ظهارة الغشاء المخاطي الحرشفية المطبقة.",
        "source": "WHO_UMD_PATHOLOGY"
    },
    {
        "cui": "DENT_PATH_007",
        "category": "PATHOLOGY",
        "subspecialty": "Oral Pathology",
        "latinName": "Mucocele",
        "termsEn": ["mucocele", "mucus extravasation phenomenon"],
        "termsAr": ["قيلة مخاطية", "كيس مخاطي احتباسي", "قيلة لعابية"],
        "defEn": "A common benign swelling caused by traumatic severance of a minor salivary gland duct and subsequent mucus spillage into connective tissue.",
        "defAr": "كيسة مخاطية شائعة تصيب الشفة السفلية تنتج عن رض مجرى غدة لعابية صغيرة وتسرب اللعاب للنسيج الضام المحيط.",
        "source": "WHO_UMD_PATHOLOGY"
    },
    {
        "cui": "DENT_PATH_008",
        "category": "DIAGNOSTIC",
        "subspecialty": "Oral Pathology",
        "latinName": None,
        "termsEn": ["biopsy", "oral biopsy", "incisional biopsy"],
        "termsAr": ["خزعة نسيجية", "خزعة فموية تشخيصية", "خزعة استئصالية"],
        "defEn": "The surgical removal of a tissue sample from a living subject for microscopic histopathological examination and definitive diagnosis.",
        "defAr": "الاستئصال الجراحي لعينة نسيجية من آفة مشتبه بها لفحصها مجهرياً ووضع التشخيص النسيجي القاطع.",
        "source": "WHO_UMD_PATHOLOGY"
    },
    {
        "cui": "DENT_PATH_009",
        "category": "PATHOLOGY",
        "subspecialty": "Oral Pathology",
        "latinName": "Dysplasia",
        "termsEn": ["dysplasia", "epithelial dysplasia", "oral epithelial dysplasia"],
        "termsAr": ["خلل التنسج", "خلل التنسج الظهاري", "حؤول خلوي"],
        "defEn": "Microscopic architectural and cytologic cellular abnormalities in mucosal epithelium indicative of premalignant potential.",
        "defAr": "تغيرات مجهرية شاذة في شكل الخلايا وبنية الظهارة تمثل مؤشراً على احتمالية التحول نحو الخباثة.",
        "source": "WHO_UMD_PATHOLOGY"
    }
]

# =============================================================================
# INGESTION & COMPILATION ENGINE
# =============================================================================

def find_sqlite_binary() -> Optional[str]:
    """Finds an external SQLite3 CLI binary with full FTS4 support."""
    for env in ["ANDROID_HOME", "ANDROID_SDK_ROOT"]:
        base = os.environ.get(env)
        if base:
            candidate = os.path.join(base, "platform-tools", "sqlite3.exe" if os.name == "nt" else "sqlite3")
            if os.path.exists(candidate):
                return candidate
    user_profile = os.environ.get("USERPROFILE", "")
    if user_profile:
        candidate = os.path.join(user_profile, "AppData", "Local", "Android", "Sdk", "platform-tools", "sqlite3.exe")
        if os.path.exists(candidate):
            return candidate
    import shutil
    candidate = shutil.which("sqlite3")
    if candidate:
        return candidate
    return None

def run_sql_via_binary(db_path: str, sql_script: str) -> str:
    """Executes SQL script using the platform SQLite3 CLI binary."""
    binary = find_sqlite_binary()
    if not binary:
        raise RuntimeError(
            "SQLite binary with FTS4 support not found. Please install Android SDK platform-tools."
        )
    import subprocess
    proc = subprocess.run(
        [binary, db_path],
        input=sql_script,
        capture_output=True,
        text=True,
        encoding="utf-8",
        check=True
    )
    return proc.stdout.strip()

TRIGGER_DROP_SQL = """
DROP TRIGGER IF EXISTS room_fts_content_sync_medical_terms_fts_BEFORE_UPDATE;
DROP TRIGGER IF EXISTS room_fts_content_sync_medical_terms_fts_BEFORE_DELETE;
DROP TRIGGER IF EXISTS room_fts_content_sync_medical_terms_fts_AFTER_UPDATE;
DROP TRIGGER IF EXISTS room_fts_content_sync_medical_terms_fts_AFTER_INSERT;
"""

TRIGGER_RESTORE_SQL = """
CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medical_terms_fts_BEFORE_UPDATE
BEFORE UPDATE ON `medical_terms` BEGIN
    DELETE FROM `medical_terms_fts` WHERE `docid`=OLD.`rowid`;
END;

CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medical_terms_fts_BEFORE_DELETE
BEFORE DELETE ON `medical_terms` BEGIN
    DELETE FROM `medical_terms_fts` WHERE `docid`=OLD.`rowid`;
END;

CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medical_terms_fts_AFTER_UPDATE
AFTER UPDATE ON `medical_terms` BEGIN
    INSERT INTO `medical_terms_fts`(`docid`, `termText`) VALUES (NEW.`rowid`, NEW.`termText`);
END;

CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_medical_terms_fts_AFTER_INSERT
AFTER INSERT ON `medical_terms` BEGIN
    INSERT INTO `medical_terms_fts`(`docid`, `termText`) VALUES (NEW.`rowid`, NEW.`termText`);
END;

INSERT INTO `medical_terms_fts`(`docid`, `termText`) SELECT `termId`, `termText` FROM `medical_terms`;
INSERT INTO `medical_terms_fts`(`medical_terms_fts`) VALUES('rebuild');
"""

def ingest_dental_subspecialties(db_path: str, verbose: bool = True) -> Tuple[int, int]:
    """
    Ingests the authoritative 7-department dental lexicon into SQLite asset db.
    Populates medical_concepts, medical_terms, medical_definitions, and rebuilds medical_terms_fts.
    """
    if not os.path.exists(db_path):
        raise FileNotFoundError(f"Database asset file not found at: {db_path}")

    start_time = time.perf_counter()
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    # Drop FTS triggers temporarily to allow Python sqlite3 to insert terms without FTS4 module error
    cur.executescript(TRIGGER_DROP_SQL)
    conn.commit()

    # Ensure tables exist
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
    cur.execute("""
        CREATE TABLE IF NOT EXISTS `medical_definitions` (
            `defId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `conceptId` INTEGER NOT NULL,
            `definitionEn` TEXT,
            `definitionAr` TEXT,
            FOREIGN KEY(`conceptId`) REFERENCES `medical_concepts`(`conceptId`) ON UPDATE NO ACTION ON DELETE CASCADE
        );
    """)

    cur.execute("SELECT COUNT(*) FROM medical_concepts;")
    initial_concepts = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM medical_terms;")
    initial_terms = cur.fetchone()[0]

    concepts_inserted = 0
    terms_inserted = 0

    for item in DENTAL_CONCEPTS_DATA:
        # Check if concept already exists by primary English term or CUI
        primary_en = item["termsEn"][0]
        cur.execute("""
            SELECT c.conceptId FROM medical_concepts c
            JOIN medical_terms t ON c.conceptId = t.conceptId
            WHERE t.langCode = 'en' AND LOWER(TRIM(t.termText)) = LOWER(TRIM(?))
            LIMIT 1;
        """, (primary_en,))
        existing = cur.fetchone()

        if existing:
            concept_id = existing[0]
            # Update concept metadata if needed
            cur.execute("""
                UPDATE medical_concepts 
                SET cui = COALESCE(?, cui),
                    category = ?,
                    subspecialty = COALESCE(?, subspecialty),
                    latinName = COALESCE(?, latinName)
                WHERE conceptId = ?;
            """, (item["cui"], item["category"], item["subspecialty"], item["latinName"], concept_id))
        else:
            cur.execute("""
                INSERT INTO medical_concepts (cui, category, subspecialty, latinName)
                VALUES (?, ?, ?, ?);
            """, (item["cui"], item["category"], item["subspecialty"], item["latinName"]))
            concept_id = cur.lastrowid
            concepts_inserted += 1

        # Ingest English Terms
        for idx, en_term in enumerate(item["termsEn"]):
            cur.execute("""
                SELECT termId FROM medical_terms
                WHERE conceptId = ? AND langCode = 'en' AND LOWER(TRIM(termText)) = LOWER(TRIM(?));
            """, (concept_id, en_term))
            if not cur.fetchone():
                cur.execute("""
                    INSERT INTO medical_terms (conceptId, langCode, termText, isPreferred, source)
                    VALUES (?, 'en', ?, ?, ?);
                """, (concept_id, en_term.strip(), 1 if idx == 0 else 0, item["source"]))
                terms_inserted += 1

        # Ingest Arabic Terms
        for idx, ar_term in enumerate(item["termsAr"]):
            cur.execute("""
                SELECT termId FROM medical_terms
                WHERE conceptId = ? AND langCode = 'ar' AND TRIM(termText) = TRIM(?);
            """, (concept_id, ar_term))
            if not cur.fetchone():
                cur.execute("""
                    INSERT INTO medical_terms (conceptId, langCode, termText, isPreferred, source)
                    VALUES (?, 'ar', ?, ?, ?);
                """, (concept_id, ar_term.strip(), 1 if idx == 0 else 0, item["source"]))
                terms_inserted += 1

        # Ingest Latin Term if present
        if item.get("latinName"):
            lat = item["latinName"].strip()
            cur.execute("""
                SELECT termId FROM medical_terms
                WHERE conceptId = ? AND langCode = 'la' AND LOWER(TRIM(termText)) = LOWER(TRIM(?));
            """, (concept_id, lat))
            if not cur.fetchone():
                cur.execute("""
                    INSERT INTO medical_terms (conceptId, langCode, termText, isPreferred, source)
                    VALUES (?, 'la', ?, 0, ?);
                """, (concept_id, lat, item["source"]))
                terms_inserted += 1

        # Ingest Definition
        if item.get("defEn") or item.get("defAr"):
            cur.execute("SELECT defId FROM medical_definitions WHERE conceptId = ?;", (concept_id,))
            if not cur.fetchone():
                cur.execute("""
                    INSERT INTO medical_definitions (conceptId, definitionEn, definitionAr)
                    VALUES (?, ?, ?);
                """, (concept_id, item.get("defEn"), item.get("defAr")))

    conn.commit()
    conn.close()

    # Re-create triggers and rebuild FTS4 virtual table via SQLite binary
    if verbose:
        print("[Compiler] Re-creating triggers and rebuilding FTS4 index via platform SQLite binary...")
    run_sql_via_binary(db_path, TRIGGER_RESTORE_SQL)
    run_sql_via_binary(db_path, "ANALYZE;")

    # Re-open connection for verification
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    cur.execute("SELECT COUNT(*) FROM medical_concepts;")
    total_concepts = cur.fetchone()[0]
    cur.execute("SELECT COUNT(*) FROM medical_terms;")
    total_terms = cur.fetchone()[0]

    elapsed_ms = (time.perf_counter() - start_time) * 1000

    if verbose:
        print("=" * 65)
        print(" 7-DEPARTMENT DENTAL SUBSPECIALTY LEXICON INGESTION COMPLETE")
        print("=" * 65)
        print(f" Database:            {db_path}")
        print(f" Initial Concepts:    {initial_concepts}")
        print(f" Newly Inserted:      {concepts_inserted}")
        print(f" Total Concepts:      {total_concepts}")
        print(f" Initial Terms:       {initial_terms}")
        print(f" Newly Inserted:      {terms_inserted}")
        print(f" Total Terms:         {total_terms}")
        print(f" Execution Time:      {elapsed_ms:.2f} ms")
        print("=" * 65)

    # Verification checklist
    test_terms = [
        "gutta-percha", "working length", "smear layer", "biologic width",
        "cantilever", "chamfer", "C-factor", "dry socket",
        "cephalometric", "leukoplakia"
    ]

    if verbose:
        print("\n[Verification] Validating High-Yield Dental Term Lookups:")
        for t in test_terms:
            t0 = time.perf_counter()
            cur.execute("""
                SELECT tEn.termText, tAr.termText, c.category, c.subspecialty
                FROM medical_terms tEn
                JOIN medical_concepts c ON tEn.conceptId = c.conceptId
                JOIN medical_terms tAr ON c.conceptId = tAr.conceptId AND tAr.langCode = 'ar'
                WHERE tEn.langCode = 'en' AND LOWER(TRIM(tEn.termText)) = LOWER(TRIM(?))
                ORDER BY tAr.isPreferred DESC LIMIT 1;
            """, (t,))
            row = cur.fetchone()
            lookup_ms = (time.perf_counter() - t0) * 1000

            if row:
                print(f"  [OK] '{row[0]}' -> '{row[1]}' [{row[2]}] ({row[3]}) [{lookup_ms:.3f} ms]")
            else:
                # Also try matching prefix or variant
                cur.execute("""
                    SELECT tEn.termText, tAr.termText, c.category, c.subspecialty
                    FROM medical_terms tEn
                    JOIN medical_concepts c ON tEn.conceptId = c.conceptId
                    JOIN medical_terms tAr ON c.conceptId = tAr.conceptId AND tAr.langCode = 'ar'
                    WHERE tEn.langCode = 'en' AND LOWER(TRIM(tEn.termText)) LIKE LOWER(TRIM(?)) || '%'
                    ORDER BY tAr.isPreferred DESC LIMIT 1;
                """, (t,))
                row2 = cur.fetchone()
                lookup_ms = (time.perf_counter() - t0) * 1000
                if row2:
                    print(f"  [OK*] '{row2[0]}' -> '{row2[1]}' [{row2[2]}] ({row2[3]}) [{lookup_ms:.3f} ms]")
                else:
                    print(f"  [FAIL] Term '{t}' could not be resolved!")
                    raise AssertionError(f"Crucial dental term '{t}' missing from database!")

    conn.close()
    return concepts_inserted, total_concepts


def main():
    parser = argparse.ArgumentParser(description="Ingest 7-Department Dental Subspecialty Lexicon.")
    parser.add_argument("-d", "--db", default=DEFAULT_DB_PATH, help="Path to target SQLite database asset.")
    parser.add_argument("-q", "--quiet", action="store_true", help="Quiet output mode.")
    args = parser.parse_args()

    ingest_dental_subspecialties(db_path=args.db, verbose=not args.quiet)


if __name__ == "__main__":
    main()
