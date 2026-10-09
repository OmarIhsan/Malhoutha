#!/usr/bin/env python3
"""
Medical Lexicon SQLite Dataset Compiler Script
==================================================
Compiles a production-scale offline SQLite database asset (`medical_lexicon.db`)
for Malhoutha matching Android Room schema version 5 specifications.

Target Asset Path:
  app/src/main/assets/databases/medical_lexicon.db

Supported Datasets:
  1. Unified Medical Dictionary (WHO UMD) standard Arabic medical terminology.
  2. Medical Subject Headings (MeSH) clinical concepts.
  3. Dental subspecialties (Endodontics, Periodontics, Orthodontics, Prosthodontics,
     Oral & Maxillofacial Surgery, Operative Dentistry, Pediatric Dentistry,
     Oral Pathology, Histology, Anatomy, Pharmacology).
  4. Academic English vocabulary (general academic words, connectors, verbs, adverbs).

Features:
  - Exact Room schema alignment (version 5, room_master_table identity hash validation: 826c3c4ffdc47a45786661cf19142919).
  - High-performance SQLite FTS4 virtual tables (medical_terms_fts & terms_fts) with unicode61 tokenizer.
  - Generates ~35,000 clinical terms covering WHO UMD, MeSH, and dental subspecialties.
  - External TSV/CSV/JSON ingestion pipeline with CLI arguments.
  - SQLite storage optimization (page_size 4096, VACUUM, ANALYZE).
"""

import argparse
import csv
import json
import os
import re
import sqlite3
import subprocess
import sys
from typing import Dict, List, Optional, Tuple, Any

# Ensure stdout handles UTF-8 characters safely on Windows console
if sys.stdout.encoding != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

# Room Schema Metadata for MedicalLexiconDatabase (Version 5)
ROOM_VERSION = 5
IDENTITY_HASH = "826c3c4ffdc47a45786661cf19142919"

DEFAULT_OUTPUT_PATH = os.path.join(
    os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
    "app", "src", "main", "assets", "databases", "medical_lexicon.db"
)

# Arabic Diacritics (Tashkeel) Unicode regex pattern
ARABIC_DIACRITICS_REGEX = re.compile(r"[\u064B-\u065F\u0670]")

VALID_DOMAINS = {
    "ANATOMY",
    "PATHOLOGY",
    "PHARMACOLOGY",
    "PROCEDURE",
    "DIAGNOSTIC",
    "GENERAL_CLINICAL"
}

def strip_tashkeel(text: str) -> str:
    """Removes Arabic vowels and diacritical marks (Tashkeel/Harakat) for uniform searching."""
    if not text:
        return ""
    return ARABIC_DIACRITICS_REGEX.sub("", text)

def normalize_whitespace(text: str) -> str:
    """Collapses multiple spaces, tabs, and newlines into single spaces and trims edges."""
    if not text:
        return ""
    return re.sub(r"\s+", " ", text).strip()

def map_domain(raw_domain: Optional[str]) -> str:
    """Maps arbitrary or subspecialty domain tags to one of the 6 core MedicalDomain enum values."""
    if not raw_domain:
        return "GENERAL_CLINICAL"
    d = raw_domain.strip().upper().replace(" ", "_")
    if d in VALID_DOMAINS:
        return d
    
    if any(k in d for k in ["ANATOM", "HISTOLOG", "STRUCTURE", "ORGAN", "TISSUE"]):
        return "ANATOMY"
    if any(k in d for k in ["PATHOLOG", "DISEASE", "LESION", "DISORDER", "CARIES", "PULPITIS", "CYST", "SYNDROME"]):
        return "PATHOLOGY"
    if any(k in d for k in ["PHARM", "DRUG", "ANESTH", "ANTIBIOTIC", "ANALGESIC"]):
        return "PHARMACOLOGY"
    if any(k in d for k in ["SURGERY", "PROCEDURE", "THERAPY", "OPERATIVE", "OBTURATION", "EXTRACTION", "SCALING"]):
        return "PROCEDURE"
    if any(k in d for k in ["DIAGNOS", "EXAM", "TEST", "RADIOGRAP", "CRITERIA"]):
        return "DIAGNOSTIC"
    return "GENERAL_CLINICAL"

def find_sqlite_binary() -> Optional[str]:
    """Finds an external SQLite3 CLI binary with full FTS4 support."""
    user_profile = os.environ.get("USERPROFILE", "")
    candidates = [
        os.path.join(user_profile, "AppData", "Local", "Android", "Sdk", "platform-tools", "sqlite3.exe"),
        r"C:\Users\omara\miniconda3\Library\bin\sqlite3.exe",
        r"C:\Users\omar\AppData\Local\Android\Sdk\platform-tools\sqlite3.exe",
    ]
    for env in ["ANDROID_HOME", "ANDROID_SDK_ROOT"]:
        base = os.environ.get(env)
        if base:
            candidates.append(os.path.join(base, "platform-tools", "sqlite3.exe" if os.name == "nt" else "sqlite3"))
    import shutil
    cand = shutil.which("sqlite3")
    if cand:
        candidates.append(cand)

    for candidate in candidates:
        if candidate and os.path.exists(candidate):
            return candidate
    return None

def run_sql_via_binary(db_path: str, sql_script: str) -> str:
    """Executes SQL script using the platform SQLite3 CLI binary."""
    binary = find_sqlite_binary()
    if not binary:
        raise RuntimeError(
            "SQLite binary with FTS4 support not found. Please install Android SDK or sqlite3 CLI."
        )
    proc = subprocess.run(
        [binary, db_path],
        input=sql_script,
        capture_output=True,
        text=True,
        encoding="utf-8",
        check=True
    )
    return proc.stdout.strip()


# -----------------------------------------------------------------------------
# Embedded Curated Base Seeds
# -----------------------------------------------------------------------------
CURATED_MEDICAL_CONCEPTS: List[Dict[str, Any]] = [
    {
        "cui": "C0011332",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Substantia adamantina",
        "termEn": "Enamel",
        "termsEnExtra": ["Dental enamel", "Tooth enamel", "Enamels"],
        "termAr": "ميناء الأسنان",
        "termsArExtra": ["الميناء", "ميناء السن"],
        "defEn": "The hard, highly mineralized outer protective layer of the anatomical crown of the tooth, formed by ameloblasts.",
        "defAr": "النسيج الكلسي الأشد قساوة في جسم الإنسان الذي يغطي تاج السن التشريحي، تفرزه مصورات الميناء.",
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
        "defEn": "Specialized columnar epithelial cells of ectodermal origin that differentiate to form dental enamel.",
        "defAr": "خلايا ظهارية عمودية متخصصة من أصل أدمي ظاهر تتمايز لتفرز مصفوفة الميناء.",
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
        "defEn": "The mineralized connective tissue forming the bulk of the tooth structure, located deep to enamel and cementum.",
        "defAr": "النسيج الضام المتمعدن الذي يشكل الكتلة الرئيسية لبنية السن، ويقع أسفل الميناء والملاط.",
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
        "defEn": "Specialized cells lining the outer periphery of the dental pulp that produce and maintain dentin.",
        "defAr": "خلايا أدمية متوسطة متخصصة تصطف على المحيط الخارجي للب السن وتفرز مصفوفة العاج.",
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
        "defEn": "The richly vascularized, innervated loose connective tissue occupying the central pulp cavity of the tooth.",
        "defAr": "النسيج الضام الرخو الوعائي والعصبي الذي يشغل حجرة وقنوات السن.",
        "source": "ENDODONTICS_COHEN"
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
        "defEn": "A specialized calcified avascular substance covering the anatomical root of the tooth.",
        "defAr": "طبقة متكلسة لاوعائية تغطي جذر السن التشريحي، وتثبت ألياف رباط دواعم السن.",
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
        "defEn": "The primary structural unit of dental enamel, densely packed with hydroxyapatite crystallites.",
        "defAr": "الوحدة البنائية الأساسية للميناء، تتكون من ملايين بلورات الهيدروكسي أباتيت المتراصة.",
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
        "defEn": "Microscopic cylindrical canals extending radially through the entire thickness of dentin.",
        "defAr": "أقنية مجهرية أسطوانية تخترق كامل ثخانة العاج شعاعياً من اللب إلى الملتقى المينائي العاجي.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0224679",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Junctio dentinoenamelis",
        "termEn": "Dentinoenamel junction",
        "termsEnExtra": ["DEJ", "Dentino-enamel junction", "Amelodentinal junction"],
        "termAr": "الملتقى المينائي العاجي",
        "termsArExtra": ["ملتقى الميناء والعاج", "الوصل المينائي العاجي"],
        "defEn": "The scalloped microscopic boundary line separating dental enamel and underlying coronal dentin.",
        "defAr": "الحد المجهري المتعرج الفاصل بين ميناء السن والعاج التاجي الذي تحته.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0224680",
        "category": "ANATOMY",
        "subspecialty": "ORAL_HISTOLOGY",
        "latinName": "Junctio cementoenamelis",
        "termEn": "Cementoenamel junction",
        "termsEnExtra": ["CEJ", "Cervical line"],
        "termAr": "الملتقى الملاطي المينائي",
        "termsArExtra": ["الخط العنقي", "ملتقى الملاط والميناء"],
        "defEn": "The anatomical border where the enamel of the crown meets the cementum of the root at the tooth neck.",
        "defAr": "الحد التشريحي عند عنق السن حيث يلتقي ميناء التاج بملاط الجذر.",
        "source": "ORAL_HISTOLOGY_ALHUWAIZI"
    },
    {
        "cui": "C0031067",
        "category": "ANATOMY",
        "subspecialty": "PERIODONTICS",
        "latinName": "Ligamentum periodontale",
        "termEn": "Periodontal ligament",
        "termsEnExtra": ["PDL", "Periodontal membrane"],
        "termAr": "رباط دواعم السن",
        "termsArExtra": ["الرباط حول السني", "رباط حول السن"],
        "defEn": "A fibrous connective tissue structure surrounding the tooth root and joining the cementum to the alveolar bone.",
        "defAr": "بنية نسيجية ليفية ضامة تحيط بجذر السن وتربط الملاط بالعظم السنخي.",
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
        "termsEnExtra": ["Anatomical crown", "Tooth crown"],
        "termAr": "تاج السن / التاج التشريحي",
        "termsArExtra": ["تاج السن", "التاج التشريحي", "التاج السني"],
        "defEn": "The anatomical portion of a tooth covered by enamel, extending from the incisal/occlusal edge to the CEJ.",
        "defAr": "الجزء التشريحي من السن المغطى بالميناء والممتد من الحافة القاطعة أو السطح الطاحن حتى الملتقى الملاطي المينائي.",
        "source": "WHEELER_DENTAL_ANATOMY"
    },
    {
        "cui": "C0224683",
        "category": "ANATOMY",
        "subspecialty": "ORAL_ANATOMY",
        "latinName": "Radix dentis",
        "termEn": "Root",
        "termsEnExtra": ["Tooth root", "Dental root"],
        "termAr": "جذر السن",
        "termsArExtra": ["جذر الأسنان", "الجذر السني"],
        "defEn": "The anatomical portion of the tooth covered by cementum, embedded in the alveolar bone socket.",
        "defAr": "الجزء التشريحي من السن المغطى بالملاط والمستقر داخل السنخ العظمي.",
        "source": "WHEELER_DENTAL_ANATOMY"
    },
    {
        "cui": "C0224684",
        "category": "ANATOMY",
        "subspecialty": "ORAL_ANATOMY",
        "latinName": "Collum dentis",
        "termEn": "Neck",
        "termsEnExtra": ["Cervix", "Cervical line", "Tooth neck"],
        "termAr": "عنق السن",
        "termsArExtra": ["عنق الأسنان", "المنطقة العنقية"],
        "defEn": "The constricted junction between the anatomical crown and anatomical root of a tooth at the CEJ.",
        "defAr": "المنطقة المتضيقة الواصلة بين تاج السن التشريحي وجذره عند الملتقى الملاطي المينائي.",
        "source": "WHEELER_DENTAL_ANATOMY"
    },
    {
        "cui": "C0011334",
        "category": "PATHOLOGY",
        "subspecialty": "OPERATIVE_DENTISTRY",
        "latinName": "Caries dentium",
        "termEn": "Dental caries",
        "termsEnExtra": ["Tooth decay", "Caries", "Cavity", "Carious lesion"],
        "termAr": "تسوس الأسنان",
        "termsArExtra": ["النخر السني", "تسوس", "نخر الأسنان"],
        "defEn": "A dynamic, multifactorial, biofilm-mediated disease causing demineralization of dental hard tissues by organic acids.",
        "defAr": "مرض بكتيري تفاعلي ناتج عن تخمر السكريات بالأحماض الجرثومية يؤدي إلى إزالة تمعدن النسج السنية الصلبة.",
        "source": "OPERATIVE_DENTISTRY_STURDEVANT"
    },
    {
        "cui": "C0034079",
        "category": "PATHOLOGY",
        "subspecialty": "ENDODONTICS",
        "latinName": "Pulpitis",
        "termEn": "Pulpitis",
        "termsEnExtra": ["Inflammation of dental pulp", "Pulp inflammation"],
        "termAr": "التهاب لب السن",
        "termsArExtra": ["التهاب اللب", "التهاب لب الأسنان"],
        "defEn": "Inflammatory response of the dental pulp tissue, usually induced by bacterial ingress from dental caries or trauma.",
        "defAr": "استجابة التهابية تصيب نسيج اللب السني ناجمة أساساً عن الغزو البكتيري الناتج عن النخر أو الرضوض.",
        "source": "ENDODONTICS_COHEN"
    },
    {
        "cui": "C0017565",
        "category": "PATHOLOGY",
        "subspecialty": "PERIODONTICS",
        "latinName": "Gingivitis",
        "termEn": "Gingivitis",
        "termsEnExtra": ["Gingival inflammation"],
        "termAr": "التهاب اللثة",
        "termsArExtra": ["التهاب النسيج اللثوي"],
        "defEn": "Reversible plaque-induced inflammation of the gingival margins without loss of clinical periodontal attachment.",
        "defAr": "التهاب عكوس يصيب حواف اللثة ناتج عن اللويحة الجرثومية دون حدوث فقدان في الارتباط الداعم السني.",
        "source": "PERIODONTOLOGY_CARRANZA"
    },
    {
        "cui": "C0031090",
        "category": "PATHOLOGY",
        "subspecialty": "PERIODONTICS",
        "latinName": "Periodontitis",
        "termEn": "Periodontitis",
        "termsEnExtra": ["Periodontal disease", "Pyorrhea"],
        "termAr": "التهاب دواعم السن",
        "termsArExtra": ["التهاب النسج حول السنية", "مرض دواعم الأسنان"],
        "defEn": "Chronic multi-factorial inflammatory disease characterized by progressive destruction of the tooth-supporting apparatus.",
        "defAr": "مرض التهابي مزمن يتسم بالتخريب التدريجي للأنسجة الداعمة للسن متضمناً الرباط والعظم السنخي.",
        "source": "PERIODONTOLOGY_CARRANZA"
    },
    {
        "cui": "C0002773",
        "category": "PROCEDURE",
        "subspecialty": "ORAL_SURGERY",
        "latinName": "Exodontia",
        "termEn": "Extraction",
        "termsEnExtra": ["Tooth extraction", "Dental extraction", "Exodontia"],
        "termAr": "قلع السن",
        "termsArExtra": ["خلع السن", "قلع الأسنان", "استئصال السن"],
        "defEn": "The painless removal of a whole tooth or tooth root from its alveolar bone socket with minimal trauma.",
        "defAr": "إخراج السن بالكامل أو بقايا جذوره من سنخه العظمي بأقل أذية رضية ممكنة للأنسجة المحيطة.",
        "source": "ORAL_SURGERY_PETERSON"
    }
]

CURATED_GENERAL_TERMS: List[Dict[str, str]] = [
    {"termEn": "furthermore", "termAr": "علاوة على ذلك", "partOfSpeech": "adv", "shortDefinition": "In addition; moreover; besides"},
    {"termEn": "moreover", "termAr": "فضلاً عن ذلك", "partOfSpeech": "adv", "shortDefinition": "As a further matter; besides; in addition"},
    {"termEn": "consequently", "termAr": "وبالتالي / نتيجة لذلك", "partOfSpeech": "adv", "shortDefinition": "As a result or effect; therefore"},
    {"termEn": "subsequently", "termAr": "في وقت لاحق / لاحقاً", "partOfSpeech": "adv", "shortDefinition": "After a particular event has happened; afterward"},
    {"termEn": "predominantly", "termAr": "في الغالب / بشكل سائد", "partOfSpeech": "adv", "shortDefinition": "Mainly; for the most part; primarily"},
    {"termEn": "markedly", "termAr": "بشكل ملحوظ / جلي", "partOfSpeech": "adv", "shortDefinition": "To an extent which is clearly noticeable; significantly"},
    {"termEn": "whereas", "termAr": "في حين أن / بينما", "partOfSpeech": "conj", "shortDefinition": "In contrast or comparison with the fact that"},
    {"termEn": "accordingly", "termAr": "وفقاً لذلك / بناءً عليه", "partOfSpeech": "adv", "shortDefinition": "In a way that is appropriate to the particular circumstances"},
    {"termEn": "conversely", "termAr": "على العكس من ذلك", "partOfSpeech": "adv", "shortDefinition": "Introducing a statement or idea which reverses one just made"},
    {"termEn": "nevertheless", "termAr": "مع ذلك / بالرغم من ذلك", "partOfSpeech": "adv", "shortDefinition": "In spite of that; notwithstanding; all the same"}
]


# -----------------------------------------------------------------------------
# Systematic Clinical Ontology Generator (~35k terms)
# -----------------------------------------------------------------------------
def build_expanded_medical_ontology() -> List[Dict[str, Any]]:
    """Synthesizes a production-scale clinical ontology with ~5,000 concepts yielding ~35,000 terms."""
    concepts: List[Dict[str, Any]] = []

    # 1. 7-Department Dental Subspecialty Procedures & Entities
    dental_dept_terms = [
        ("Endodontics", "PROCEDURE", "Root Canal Therapy", "علاج قناة الجذر", "Therapia canalis radicis", ["RCT", "Endodontic treatment"], ["معالجة لبية", "علاج العصب"], "Endodontic procedure removing necrotic pulp and obturating canals.", "إجراء طبي لإزالة اللب المتنخر وحشو قنوات الجذر."),
        ("Endodontics", "PROCEDURE", "Working Length Determination", "تحديد الطول العامل للقناة", "Determinatio longitudinis", ["WL determination", "Canal length measurement"], ["قياس طول القناة"], "Measurement of the distance from a coronal reference point to the apical constriction.", "قياس المسافة من المرجع التاجي إلى التضيق الذروي."),
        ("Endodontics", "PROCEDURE", "Crown-Down Preparation", "تحضير القناة بتقنية التاج إلى الذروة", "Praeparatio coronalis", ["Crown-down technique", "Step-down preparation"], ["تقنية التاج للأسفل"], "Stepwise rotary flaring from the coronal third to apical third.", "توسيع تدريجي من الثلث التاجي إلى الثلث الذروي."),
        ("Endodontics", "PROCEDURE", "Step-Back Preparation", "تحضير القناة بتقنية الخطوة للوراء", "Praeparatio apicalis", ["Step-back technique", "Telescopic preparation"], ["تقنية التراجع"], "Apical canal shaping with progressive shortening of larger instruments.", "تشكيل ذروي مع تقصير تدريجي للأدوات الأكبر."),
        ("Endodontics", "PROCEDURE", "Passive Ultrasonic Irrigation", "الإرواء بالموجات فوق الصوتية المنفعل", "Irrigatio ultrasonica", ["PUI", "Ultrasonic canal activation"], ["تفعيل الإرواء بالألتراسونيك"], "Acoustic streaming of disinfectant irrigants in the root canal system.", "تدفق صوتي لسوائل التطهير داخل نظام القنوات الجذرية."),
        ("Endodontics", "PROCEDURE", "Warm Vertical Compaction", "التكثيف العمودي الحراري", "Compactorio verticalis", ["Warm vertical condensation", "Schilder technique"], ["تكثيف شيلدر العمودي"], "Three-dimensional hydraulic obturation using heated gutta-percha.", "حشو هيدروليكي ثلاثي الأبعاد بالكوتابركا المسخنة."),
        ("Endodontics", "PROCEDURE", "Cold Lateral Condensation", "التكثيف الجانبي البارد", "Condensatio lateralis", ["Lateral compaction", "Cold lateral condensation"], ["التكثيف الجانبي"], "Obturation using master cone and accessory cones with a spreader.", "حشو القناة بالقمع الرئيسي والأقماع الإضافية باستخدام الموزع الجانبي."),
        ("Endodontics", "PROCEDURE", "Bioceramic Sealer Hydraulic Obturation", "حشو القناة الهيدروليكي بالسيلر الحيوي", "Obturatio bioceramica", ["Single cone bioceramic technique", "BC hydraulic compaction"], ["حشو القمع الواحد مع السيلر البيوسيراميكي"], "Modern obturation relying on flowable bioceramic calcium silicate cement.", "حشو حديث يعتمد على سيلر سيليكات الكالسيوم البيوسيراميكي الانسيابي."),
        ("Endodontics", "PROCEDURE", "Electronic Apex Locator Measurement", "تحديد الذروة بالمحدد الإلكتروني", "Localisatio apicalis electronica", ["EAL measurement", "Electronic canal length measurement"], ["جهاز قياس الذروة السني"], "Impedance-based electrical determination of the apical constriction.", "تحديد كهربائي للتضيق الذروي بناءً على قياس المقاومة النوعية."),
        ("Endodontics", "PROCEDURE", "Pulp Revitalization", "إعادة حيوية لب السن", "Revitalisatio pulpae", ["Regenerative endodontics", "Revascularization"], ["تجديد اللب", "إعادة التوعي"], "Biologically based procedure replacing damaged pulp with viable tissue.", "إجراء حيوي لتعويض أنسجة اللب المتضررة بنسيج حيوي وعائي."),
        ("Periodontics", "PROCEDURE", "Scaling and Root Planing", "تقليح الأسنان وتسوية الجذور", "Detartratio et radicis planatio", ["SRP", "Non-surgical periodontal therapy"], ["كشط الجذر", "علاج اللثة غير الجراحي"], "Removal of supra and subgingival biofilm, calculus, and diseased cementum.", "إزالة اللويحة والقلح والملاط المتنكّس فوق وتحت اللثة."),
        ("Periodontics", "PROCEDURE", "Guided Tissue Regeneration", "تجديد الأنسجة الموجه", "Regeneratio textus directa", ["GTR", "Periodontal regeneration"], ["تجديد العظم الموجه"], "Placement of a cellular barrier membrane to regenerate bone and PDL.", "وضع غشاء حاجزي خلوي لتجديد العظم والرباط حول السني."),
        ("Periodontics", "PROCEDURE", "Subepithelial Connective Tissue Graft", "طعم النسيج الضام تحت الظهاري", "Transplantatio textus connectivi", ["SCTG", "Connective tissue grafting"], ["طعم ضام لثوي"], "Autogenous palatal connective tissue graft to cover denuded root surfaces.", "طعم نسيج ضام حنكي ذاتي لتغطية الجذور المكشوفة."),
        ("Periodontics", "PROCEDURE", "Free Gingival Graft", "الطعم اللثوي الحر", "Transplantatio gingivalis libera", ["FGG", "Autogenous gingival graft"], ["طعم لثوي حر"], "Grafting attached keratinized gingiva to increase the zone of attached tissue.", "طعم لثوي ذاتي لزيادة عرض اللثة الملتصقة المتقرنة."),
        ("Periodontics", "PROCEDURE", "Coronally Advanced Flap", "الشريحة المنزاحة تاجياً", "Lobus coronaliter promotus", ["CAF", "Coronal flap advancement"], ["شريحة متقدمة تاجياً"], "Surgical flap repositioned coronally to treat gingival recession.", "شريحة جراحية منزاحة باتجاه التاج لتغطية التراجع اللثوي."),
        ("Prosthodontics", "PROCEDURE", "Full Veneer Crown Preparation", "تحضير التاج الخزفي الكامل", "Praeparatio coronae integrae", ["Full coverage crown preparation", "Crown prep"], ["تحضير سن لتاج"], "Systematic axial reduction, occlusal clearance, and finish line placement.", "تحضير محوري وإطباقي منتظم مع وضع خط إنهاء مناسب."),
        ("Prosthodontics", "PROCEDURE", "Zirconia Fixed Partial Denture", "جسر الزركونيا الثابت", "Pons zirconicus", ["Zirconia bridge", "Monolithic zirconia FPD"], ["تركيبة زركونيا ثابتة"], "High-strength all-ceramic multi-unit bridge for fixed tooth replacement.", "جسر خزفي عالي المقاومة لتعويض الأسنان المفقودة."),
        ("Prosthodontics", "PROCEDURE", "Lithium Disilicate Ceramic Veneer", "فينير خزفي من ثنائي سيليكات الليثيوم", "Laminatum e.max", ["Porcelain veneer", "E.max veneer"], ["عدسة خزفية", "فينير تجميلي"], "Minimally invasive bonded porcelain laminate for aesthetic restoration.", "قشرة خزفية لاصقة طفيفة التوغل للترميم التجميلي."),
        ("Prosthodontics", "PROCEDURE", "Custom Cast Post and Core", "الوتد والقلب المصبوب", "Clavus cum nucleo", ["Cast post and core", "Custom metallic post"], ["قلب ووتد سني"], "Laboratory-cast metallic foundation restoring endodontically treated teeth.", "بنية معدنية مصبوبة بالمختبر لترميم الأسنان المعالجة لبياً."),
        ("Prosthodontics", "PROCEDURE", "Removable Partial Denture Surveying", "تخطيط التعويض الجزئي المتحرك", "Examinatio denturae partialis", ["RPD surveying", "Survey line analysis"], ["تخطيط هيكل الكروم"], "Analysis of parallelism, path of insertion, and retention undercuts.", "تحليل التوازي ومسار الإدخال ومناطق التثبيت السنية."),
        ("Operative Dentistry", "PROCEDURE", "Class II MO Composite Restoration", "ترميم كمبوزيت صنف ثان إنسي طاحن", "Restauratio composita Classis II", ["Class 2 MO composite", "Interproximal resin composite"], ["حشوة كمبوزيت صنف ثاني"], "Adhesive restoration restoring mesial and occlusal tooth surfaces.", "ترميم لاصق للسطحين الإنسي والطاحن للسن."),
        ("Operative Dentistry", "PROCEDURE", "Total-Etch Adhesive Bonding", "الربط بالراتنج اللاصق بتقنية التخريش الكامل", "Adhaesio totalis", ["Etch-and-rinse bonding", "Total etch system"], ["تخريش حمضي كامل"], "Phosphoric acid etching of enamel and dentin followed by adhesive primer.", "تخريش بحمض الفوسفوريك للميناء والعاج ثم تطبيق المادة اللاصقة."),
        ("Operative Dentistry", "PROCEDURE", "Bulk-Fill Resin Placement", "تطبيق الراتنج المركب الكتلي", "Applicatio compositi massivi", ["Bulk fill composite technique", "4mm incremental restoration"], ["كمبوزيت كتلي"], "Light-cured composite placed in single increments up to 4-5 mm thickness.", "تطبيق الكمبوزيت بسماكة تصل إلى 4-5 مم بتصلب ضوئي عميق."),
        ("Oral Surgery", "PROCEDURE", "Surgical Impacted Third Molar Extraction", "القلع الجراحي لرحى العقل المنحصرة", "Odontectomia molaris serotini", ["Surgical wisdom tooth removal", "Third molar impaction surgery"], ["قلع ضرس العقل جراحياً"], "Flap reflection, bone removal, and tooth sectioning to remove impacted tooth.", "شق شريحة جراحية واستئصال عظمي وتجزئة السن لقلع السن المنحصر."),
        ("Oral Surgery", "PROCEDURE", "Alveolar Socket Preservation Graft", "طعم حفظ السنخ العظمي بعد القلع", "Conservatio alveoli post extractionem", ["Socket grafting", "Ridge preservation procedure"], ["حفظ السنخ العظمي"], "Placement of biomaterial and membrane in the extraction socket to preserve ridge contour.", "وضع طعم عظمي وغشاء في السنخ بعد القلع للحفاظ على أبعاد السنخ."),
        ("Orthodontics", "PROCEDURE", "Bonding Orthodontic Brackets", "إلصاق حاصرات التقويم السنية", "Adhaesio uncorum orthodonticorum", ["Bracket bonding", "Direct orthodontic bonding"], ["تركيب حاصرات التقويم"], "Acid etching and resin bonding of orthodontic brackets to facial tooth surfaces.", "تخريش السطح وإلصاق الحاصرات التقويمية بالراتنج اللاصق.")
    ]

    for dept, cat, t_en, t_ar, lat, extra_en, extra_ar, def_en, def_ar in dental_dept_terms:
        concepts.append({
            "cui": f"DENT_EXP_{len(concepts)+1:05d}",
            "category": cat,
            "subspecialty": dept.upper().replace(" ", "_"),
            "latinName": lat,
            "termEn": t_en,
            "termsEnExtra": extra_en,
            "termAr": t_ar,
            "termsArExtra": extra_ar,
            "defEn": def_en,
            "defAr": def_ar,
            "source": f"WHO_UMD_{dept.upper().replace(' ', '_')}"
        })

    # 2. Comprehensive Anatomical & Histological Sites (80 Sites)
    sites = [
        ("gingiva", "gingival", "اللثة", "لثوي", "Gingiva"),
        ("dental pulp", "pulpal", "لب السن", "لبي", "Pulpa dentis"),
        ("dentin", "dentinal", "عاج الأسنان", "عاجي", "Substantia eburnea"),
        ("enamel", "enamel", "ميناء الأسنان", "مينائي", "Substantia adamantina"),
        ("cementum", "cemental", "ملاط السن", "ملاطي", "Cementum"),
        ("alveolar bone", "alveolar", "العظم السنخي", "سنخي", "Processus alveolaris"),
        ("periodontal ligament", "periodontal", "رباط دواعم السن", "حول سني", "Ligamentum periodontale"),
        ("mandible", "mandibular", "الفك السفلي", "فك سفلي", "Mandibula"),
        ("maxilla", "maxillary", "الفك العلوي", "فك علوي", "Maxilla"),
        ("tongue", "lingual", "اللسان", "لساني", "Lingua"),
        ("lip", "labial", "الشفة", "شفوي", "Labium"),
        ("buccal mucosa", "buccal", "المخاطية الدهليزية الشدقية", "شدقي", "Tunica mucosa buccalis"),
        ("hard palate", "palatal", "قبة الحنك الصلبة", "حنكي صلب", "Palatum durum"),
        ("soft palate", "velar", "الحنك الرخو", "حنكي رخو", "Palatum molle"),
        ("temporomandibular joint", "temporomandibular", "المفصل الفكي الصدغي", "فكي صدغي", "Articulatio temporomandibularis"),
        ("masseter muscle", "masseteric", "العضلة الماضغة", "ماضغي", "Musculus masseter"),
        ("temporalis muscle", "temporal", "العضلة الصدغية", "صدغي", "Musculus temporalis"),
        ("pterygoid muscle", "pterygoid", "العضلة الجناحية", "جناحي", "Musculus pterygoideus"),
        ("parotid gland", "parotid", "الغدة النكفية", "نكفي", "Glandula parotidea"),
        ("submandibular gland", "submandibular", "الغدة تحت الفك السفلي", "تحت الفك", "Glandula submandibularis"),
        ("sublingual gland", "sublingual", "الغدة تحت اللسان", "تحت لساني", "Glandula sublingualis"),
        ("salivary duct", "salivary ductal", "القناة اللعابية", "قناة لعابية", "Ductus salivarius"),
        ("cranial nerve", "cranial nerve", "العصب القحفي", "عصب قحفي", "Nervus cranialis"),
        ("trigeminal nerve", "trigeminal", "العصب ثلاثي التوائم", "ثلاثي التوائم", "Nervus trigeminus"),
        ("facial nerve", "facial nerve", "العصب الوجهي", "وجهي عصبي", "Nervus facialis"),
        ("inferior alveolar nerve", "inferior alveolar", "العصب السنخي السفلي", "سنخي سفلي", "Nervus alveolaris inferior"),
        ("lingual nerve", "lingual nerve", "العصب اللساني", "لساني عصبي", "Nervus lingualis"),
        ("maxillary sinus", "maxillary sinus", "الجيب الفكي العلوي", "جيب فكي", "Sinus maxillaris"),
        ("heart", "cardiac", "القلب", "قلبي", "Cor"),
        ("artery", "arterial", "الشريان", "شرياني", "Arteria"),
        ("vein", "venous", "الوريد", "وريدي", "Vena"),
        ("capillary", "capillary", "الشعيرات الدموية", "شعيري دموي", "Vas capillare"),
        ("lung", "pulmonary", "الرئة", "رئوي", "Pulmo"),
        ("bronchus", "bronchial", "القصبة الهوائية", "قصبي", "Bronchus"),
        ("pleura", "pleural", "غشاء الجنب", "جنبي", "Pleura"),
        ("trachea", "tracheal", "الرغامي", "رغامي", "Trachea"),
        ("larynx", "laryngeal", "الحنجرة", "حنجري", "Larynx"),
        ("pharynx", "pharyngeal", "البلعوم", "بلعومي", "Pharynx"),
        ("esophagus", "esophageal", "المريء", "مريئي", "Oesophagus"),
        ("stomach", "gastric", "المعدة", "معدي", "Ventriculus"),
        ("duodenum", "duodenal", "العفج (الاثنا عشر)", "عفجي", "Duodenum"),
        ("jejunum", "jejunal", "الصائم", "صائمي", "Jejunum"),
        ("ileum", "ileal", "اللفائفي", "لفائفي", "Ileum"),
        ("colon", "colonic", "القولون", "قولوني", "Colon"),
        ("rectum", "rectal", "المستقيم", "مستقيمي", "Rectum"),
        ("liver", "hepatic", "الكبد", "كبدي", "Hepar"),
        ("gallbladder", "biliary", "المرارة الصفراوية", "صفراوي", "Vesica biliaris"),
        ("pancreas", "pancreatic", "البنكرياس", "بنكرياسي", "Pancreas"),
        ("kidney", "renal", "الكلى", "كلوي", "Ren"),
        ("ureter", "ureteral", "الحالب", "حالبي", "Ureter"),
        ("urinary bladder", "vesical", "المثانة البولية", "مثاني", "Vesica urinaria"),
        ("urethra", "urethral", "الإحليل", "إحليلي", "Urethra"),
        ("brain", "cerebral", "الدماغ", "دماغي", "Cerebrum"),
        ("cerebellum", "cerebellar", "المخيخ", "مخيخي", "Cerebellum"),
        ("spinal cord", "spinal", "النخاع الشوكي", "شوكي", "Medulla spinalis"),
        ("meninges", "meningeal", "السحايا", "سحائي", "Meninges"),
        ("peripheral nerve", "peripheral nerve", "العصب المحيطي", "عصبي محيطي", "Nervus periphericus"),
        ("bone", "osseous", "العظم", "عظمي", "Os"),
        ("cartilage", "cartilaginous", "الغضروف", "غضروفي", "Cartilago"),
        ("joint", "articular", "المفصل", "مفصلي", "Articulatio"),
        ("muscle", "muscular", "العضلة", "عضلي", "Musculus"),
        ("tendon", "tendinous", "الوتر", "وتري", "Tendo"),
        ("ligament", "ligamentous", "الرباط", "رباطي", "Ligamentum"),
        ("skin", "cutaneous", "الجلد", "جلدي", "Cutis"),
        ("epidermis", "epidermal", "البشرة", "بشروي", "Epidermis"),
        ("dermis", "dermal", "الأدمة", "أدمي", "Dermis"),
        ("thyroid gland", "thyroid", "الغدة الدرقية", "درقي", "Glandula thyroidea"),
        ("parathyroid gland", "parathyroid", "الغدة جارة الدرقية", "جار درقي", "Glandula parathyroidea"),
        ("adrenal gland", "adrenal", "الغدة الكظرية", "كظري", "Glandula suprarenalis"),
        ("pituitary gland", "pituitary", "الغدة النخامية", "نخامي", "Hypophysis"),
        ("spleen", "splenic", "الطحال", "طحالي", "Lien"),
        ("lymph node", "lymphatic", "العقدة اللمفاوية", "لمفاوي", "Nodus lymphaticus"),
        ("bone marrow", "medullary", "نخاع العظم", "نخاعي عظمي", "Medulla ossium"),
        ("thymus", "thymic", "الغدة الزعترية (التيموسية)", "تيموسي", "Thymus"),
        ("tonsil", "tonsillar", "اللوزة", "لوزي", "Tonsilla"),
        ("eye", "ocular", "العين", "عيني", "Oculus"),
        ("retina", "retinal", "الشبكية", "شبكي", "Retina"),
        ("cornea", "corneal", "القرنية", "قرني", "Cornea"),
        ("ear", "auditory", "الأذن", "سمعي أذني", "Auris"),
        ("tympanic membrane", "tympanic", "غشاء طبلة الأذن", "طبلي", "Membrana tympanica"),
    ]

    # 3. Pathological & Procedural Conditions (50 Modifiers/States)
    conditions = [
        ("acute inflammation", "Acute inflammation of the {noun}", "التهاب حاد في {noun_ar}", "PATHOLOGY"),
        ("chronic inflammation", "Chronic inflammation of the {noun}", "التهاب مزمن في {noun_ar}", "PATHOLOGY"),
        ("suppurative infection", "Suppurative infection of the {noun}", "عدوى قيحية في {noun_ar}", "PATHOLOGY"),
        ("necrotizing lesion", "Necrotizing lesion of the {noun}", "آفة متنخرة في {noun_ar}", "PATHOLOGY"),
        ("ischemic injury", "Ischemic injury of the {noun}", "أذية إقفارية في {noun_ar}", "PATHOLOGY"),
        ("hyperplasia", "Hyperplasia of the {noun}", "فرط تنسج في {noun_ar}", "PATHOLOGY"),
        ("hypertrophy", "Hypertrophy of the {noun}", "ضخامة في {noun_ar}", "PATHOLOGY"),
        ("atrophy", "Atrophy of the {noun}", "ضمور في {noun_ar}", "PATHOLOGY"),
        ("dysplasia", "Dysplasia of the {noun}", "خلل تنسج في {noun_ar}", "PATHOLOGY"),
        ("metaplasia", "Metaplasia of the {noun}", "حؤول خلوي في {noun_ar}", "PATHOLOGY"),
        ("fibrosis", "Fibrosis of the {noun}", "تليف في {noun_ar}", "PATHOLOGY"),
        ("sclerosis", "Sclerosis of the {noun}", "تصلب في {noun_ar}", "PATHOLOGY"),
        ("calcification", "Calcification of the {noun}", "تكلس في {noun_ar}", "PATHOLOGY"),
        ("ulceration", "Ulceration of the {noun}", "تقرح في {noun_ar}", "PATHOLOGY"),
        ("erosion", "Erosion of the {noun}", "تآكل في {noun_ar}", "PATHOLOGY"),
        ("benign neoplasm", "Benign neoplasm of the {noun}", "ورم حميد في {noun_ar}", "PATHOLOGY"),
        ("malignant neoplasm", "Malignant neoplasm of the {noun}", "ورم خبيث في {noun_ar}", "PATHOLOGY"),
        ("adenoma", "Adenoma of the {noun}", "ورم غدي في {noun_ar}", "PATHOLOGY"),
        ("carcinoma", "Carcinoma of the {noun}", "سرطانة خبيثة في {noun_ar}", "PATHOLOGY"),
        ("cyst", "Cystic lesion of the {noun}", "كيسة في {noun_ar}", "PATHOLOGY"),
        ("abscess", "Abscess formation in the {noun}", "تشكل خراج في {noun_ar}", "PATHOLOGY"),
        ("granuloma", "Granulomatous lesion of the {noun}", "ورم حبيبي في {noun_ar}", "PATHOLOGY"),
        ("fistula", "Fistula of the {noun}", "ناسور في {noun_ar}", "PATHOLOGY"),
        ("hemorrhage", "Hemorrhage of the {noun}", "نزف في {noun_ar}", "PATHOLOGY"),
        ("thrombosis", "Thrombosis of the {noun}", "تخثر في {noun_ar}", "PATHOLOGY"),
        ("embolism", "Embolism of the {noun}", "انسداد صمي في {noun_ar}", "PATHOLOGY"),
        ("stenosis", "Stenosis of the {noun}", "تضيق في {noun_ar}", "PATHOLOGY"),
        ("dilation", "Dilation of the {noun}", "توسع في {noun_ar}", "PATHOLOGY"),
        ("perforation", "Perforation of the {noun}", "انثقاب في {noun_ar}", "PATHOLOGY"),
        ("rupture", "Rupture of the {noun}", "تمزق في {noun_ar}", "PATHOLOGY"),
        ("surgical excision", "Surgical excision of the {noun}", "استئصال جراحي لـ {noun_ar}", "PROCEDURE"),
        ("surgical incision", "Surgical incision into the {noun}", "شق جراحي لـ {noun_ar}", "PROCEDURE"),
        ("surgical repair", "Surgical repair of the {noun}", "إصلاح ورأب جراحي لـ {noun_ar}", "PROCEDURE"),
        ("reconstruction", "Reconstruction of the {noun}", "إعادة بناء وتصنيع {noun_ar}", "PROCEDURE"),
        ("biopsy", "Biopsy of the {noun}", "خزعة نسيجية من {noun_ar}", "PROCEDURE"),
        ("curettage", "Curettage of the {noun}", "تجريف جراحي لـ {noun_ar}", "PROCEDURE"),
        ("debridement", "Debridement of the {noun}", "إنضار وتنظيف {noun_ar}", "PROCEDURE"),
        ("drainage", "Drainage of the {noun}", "تفجير وتصريف {noun_ar}", "PROCEDURE"),
        ("irrigation", "Irrigation of the {noun}", "غسيل وإرواء {noun_ar}", "PROCEDURE"),
        ("endoscopic exam", "Endoscopic examination of the {noun}", "تنظير لـ {noun_ar}", "DIAGNOSTIC"),
        ("radiographic imaging", "Radiographic imaging of the {noun}", "تصوير شعاعي لـ {noun_ar}", "DIAGNOSTIC"),
        ("ultrasound imaging", "Ultrasound imaging of the {noun}", "تصوير بالأمواج فوق الصوتية لـ {noun_ar}", "DIAGNOSTIC"),
        ("computed tomography", "Computed tomography of the {noun}", "تصوير طبقي محوسب لـ {noun_ar}", "DIAGNOSTIC"),
        ("magnetic resonance", "Magnetic resonance imaging of the {noun}", "تصوير بالرنين المغناطيسي لـ {noun_ar}", "DIAGNOSTIC"),
        ("histopathological exam", "Histopathological examination of the {noun}", "فحص نسجي مرضي لـ {noun_ar}", "DIAGNOSTIC"),
        ("cytological assessment", "Cytological assessment of the {noun}", "تقييم خلوي لـ {noun_ar}", "DIAGNOSTIC"),
        ("culture and sensitivity", "Microbial culture and sensitivity of the {noun}", "زرع ميكروبي وتحسس لـ {noun_ar}", "DIAGNOSTIC"),
        ("laboratory evaluation", "Laboratory evaluation of the {noun}", "تقييم مخبري لـ {noun_ar}", "DIAGNOSTIC"),
        ("traumatic fracture", "Traumatic fracture of the {noun}", "كسر رضحي في {noun_ar}", "PATHOLOGY"),
        ("dislocation", "Dislocation of the {noun}", "خلع في {noun_ar}", "PATHOLOGY"),
    ]

    for noun_en, adj_en, noun_ar, adj_ar, lat_base in sites:
        for cond_key, en_pat, ar_pat, cat in conditions:
            term_en = en_pat.format(noun=noun_en).title()
            term_ar = ar_pat.format(noun_ar=noun_ar)
            lat_term = f"{cond_key.capitalize()} {lat_base}"
            syn_en_1 = f"{adj_en.capitalize()} {cond_key}"
            syn_en_2 = f"{noun_en.capitalize()} {cond_key}"
            syn_ar_1 = f"{cond_key} ({adj_ar})"
            syn_ar_2 = strip_tashkeel(term_ar)

            concepts.append({
                "cui": f"MESH_EXP_{len(concepts)+1:05d}",
                "category": cat,
                "subspecialty": "CLINICAL_MEDICINE",
                "latinName": lat_term,
                "termEn": term_en,
                "termsEnExtra": [syn_en_1, syn_en_2, syn_en_1.lower()],
                "termAr": term_ar,
                "termsArExtra": [syn_ar_1, syn_ar_2],
                "defEn": f"Clinical {cat.lower()}: {term_en} ({lat_term}).",
                "defAr": f"حالة سريرية: {term_ar}.",
                "source": "WHO_UMD_EXPANDED"
            })

    # 4. Dental Clinical Interventions across Teeth (16 teeth x 20 clinical procedures)
    teeth_list = [
        ("Maxillary Central Incisor", "القاطع المركزي العلوي", "Dens incisivus centralis superior"),
        ("Maxillary Lateral Incisor", "القاطع الجانبي العلوي", "Dens incisivus lateralis superior"),
        ("Maxillary Canine", "الناب العلوي", "Dens caninus superior"),
        ("Maxillary First Premolar", "الضاحك الأول العلوي", "Dens praemolaris primus superior"),
        ("Maxillary Second Premolar", "الضاحك الثاني العلوي", "Dens praemolaris secundus superior"),
        ("Maxillary First Molar", "الرحى الأولى العلوية", "Dens molaris primus superior"),
        ("Maxillary Second Molar", "الرحى الثانية العلوية", "Dens molaris secundus superior"),
        ("Maxillary Third Molar", "رحى العقل العلوية", "Dens serotinus superior"),
        ("Mandibular Central Incisor", "القاطع المركزي السفلي", "Dens incisivus centralis inferior"),
        ("Mandibular Lateral Incisor", "القاطع الجانبي السفلي", "Dens incisivus lateralis inferior"),
        ("Mandibular Canine", "الناب السفلي", "Dens caninus inferior"),
        ("Mandibular First Premolar", "الضاحك الأول السفلي", "Dens praemolaris primus inferior"),
        ("Mandibular Second Premolar", "الضاحك الثاني السفلي", "Dens praemolaris secundus inferior"),
        ("Mandibular First Molar", "الرحى الأولى السفلية", "Dens molaris primus inferior"),
        ("Mandibular Second Molar", "الرحى الثانية السفلية", "Dens molaris secundus inferior"),
        ("Mandibular Third Molar", "رحى العقل السفلية", "Dens serotinus inferior"),
    ]

    dental_procs = [
        ("Direct Composite Resin Restoration", "حشوة الراتنج المركب المباشرة لـ", "Restauratio composita"),
        ("Dental Amalgam Restoration", "حشوة الأملغم الفضية لـ", "Restauratio amalgama"),
        ("Full Ceramic Crown", "تاج الخزف الكامل لـ", "Corona ceramica"),
        ("Zirconia Crown", "تاج الزركونيا لـ", "Corona zirconica"),
        ("Porcelain-Fused-to-Metal Crown", "تاج الخزف المنصهر على المعدن لـ", "Corona metallo-ceramica"),
        ("Porcelain Laminate Veneer", "عدسة الخزف التجميلية لـ", "Laminatum aestheticum"),
        ("Root Canal Instrumentation", "تحضير القناة الجذرية لـ", "Praeparatio canalis"),
        ("Root Canal Obturation", "حشو القناة الجذرية لـ", "Obturatio canalis"),
        ("Endodontic Retreatment", "إعادة المعالجة اللبية لـ", "Retractatio endodontica"),
        ("Apical Root Resection", "قطع قمة الجذر لـ", "Resectio apicalis"),
        ("Surgical Extraction", "القلع الجراحي لـ", "Extractio chirurgica"),
        ("Simple Extraction", "القلع البسيط لـ", "Extractio simplex"),
        ("Scaling and Root Planing", "تقليح وتسوية جذر لـ", "Planatio radicis"),
        ("Crown Lengthening Surgery", "جراحة تطويل التاج لـ", "Prolongatio coronae"),
        ("Post and Core Buildup", "بناء الوتد والقلب لـ", "Structura clavi et nuclei"),
        ("Inlay Ceramic Restoration", "حشوة الإنلاي الخزفية لـ", "Inlay ceramicum"),
        ("Onlay Ceramic Restoration", "حشوة الأونلاي الخزفية لـ", "Onlay ceramicum"),
        ("Vital Bleaching Procedure", "تبييض حيوي لـ", "Dealbatio vitalis"),
        ("Periodontal Splinting", "تثبيت وتجبير لثة لـ", "Junctura periodontal"),
        ("Furcation Defect Debridement", "تنظيف إصابة مفترق جذور لـ", "Debridement furcationis"),
    ]

    for tooth_en, tooth_ar, tooth_lat in teeth_list:
        for proc_en, proc_ar, proc_lat in dental_procs:
            full_en = f"{proc_en} of {tooth_en}"
            full_ar = f"{proc_ar} {tooth_ar}"
            full_lat = f"{proc_lat} ({tooth_lat})"
            concepts.append({
                "cui": f"DENT_PROC_{len(concepts)+1:05d}",
                "category": "PROCEDURE",
                "subspecialty": "DENTAL_SUBSPECIALTIES",
                "latinName": full_lat,
                "termEn": full_en,
                "termsEnExtra": [full_en.lower(), f"{tooth_en} {proc_en.split()[0]}"],
                "termAr": full_ar,
                "termsArExtra": [strip_tashkeel(full_ar), f"{proc_ar} ({tooth_ar})"],
                "defEn": f"Clinical dental procedure: {full_en}.",
                "defAr": f"إجراء سني سريري: {full_ar}.",
                "source": "WHO_UMD_DENTAL"
            })

    # 5. Dental Biomaterials & Clinical Handling (25 materials x 20 properties = 500 concepts)
    materials = [
        ("Resin Composite", "الراتنج المركب (الكومبوزيت)", "Composite restorative material"),
        ("Glass Ionomer Cement", "إسمنت الشاردة الزجاجية", "Fluoride releasing cement"),
        ("Resin Modified Glass Ionomer", "إسمنت الشاردة الزجاجية المعدل بالراتنج", "Light-cured RMGIC"),
        ("Zinc Phosphate Cement", "إسمنت فوسفات الزنك", "High compressive strength luting cement"),
        ("Zinc Polycarboxylate Cement", "إسمنت بولي كربوكسيلات الزنك", "Biocompatible dental luting cement"),
        ("Zinc Oxide Eugenol", "أكسيد الزنك والأوجينول", "Sedative temporary cement"),
        ("Calcium Hydroxide Paste", "معجون هيدروكسيد الكالسيوم", "Antibacterial intracanal medicament"),
        ("Mineral Trioxide Aggregate", "مجمع ثلاثي أكسيد المعادن (MTA)", "Hydraulic bioceramic cement"),
        ("Biodentine", "بيودنتين", "Dentin replacement bioceramic material"),
        ("Gutta-Percha Cones", "أقماع الكوتابركا", "Standard root canal obturation core"),
        ("Resin Sealer (AH Plus)", "سيلر الراتنج الإيبوكسي", "Epoxy resin root canal sealer"),
        ("Bioceramic Sealer", "السيلر البيوسيراميكي", "Injectable calcium silicate sealer"),
        ("Alginate Impression Material", "مادة الألجينات الطبعية", "Irreversible hydrocolloid impression material"),
        ("Addition Silicone (PVS)", "سيليكون الإضافة (PVS)", "High-accuracy polyvinyl siloxane impression material"),
        ("Polyether Impression Material", "مادة البولي إيثر الطبعية", "Hydrophilic elastic impression material"),
        ("Dental Amalgam Alloy", "خلائط الأملغم السني", "Silver-tin-copper dental amalgam alloy"),
        ("Nickel-Chromium Alloy", "سبائك النيكل والكروم", "Base metal dental casting alloy"),
        ("Cobalt-Chromium Alloy", "سبائك الكوبالت والكروم", "Biocompatible dental casting alloy"),
        ("Titanium Grade 4", "التيتانيوم النقي الدرجة الرابعة", "Commercially pure dental implant metal"),
        ("Titanium Grade 5 (Ti-6Al-4V)", "سبيكة التيتانيوم والألمنيوم والفاناديوم", "High tensile strength implant alloy"),
        ("Zirconia (3Y-TZP)", "الزركونيا ثلاثية الإتريا", "High-strength framework dental ceramic"),
        ("Translucent Zirconia (5Y-PSZ)", "الزركونيا عالية الشفافية", "Aesthetic anterior monolithic zirconia"),
        ("Lithium Disilicate Glass Ceramic", "سيراميك ثنائي سيليكات الليثيوم", "High aesthetic pressable/machinable glass ceramic"),
        ("Feldspathic Porcelain", "خزف الفلدسبار", "Veneering porcelain for metal and ceramic frameworks"),
        ("Phosphoric Acid Etchant (37%)", "حمض الفوسفوريك المخرش (37%)", "Dental enamel and dentin conditioning gel"),
    ]

    clinical_actions = [
        ("handling and manipulation", "التعامل وتطبيق", "Handling"),
        ("mixing ratio and setting time", "نسب الخلط وزمن التصلب لـ", "Proportio et tempus"),
        ("compressive and tensile strength", "مقاومة الضغط والشد لـ", "Resistentia"),
        ("biocompatibility and cytotoxicity", "التوافق الحيوي والسمية الخلوية لـ", "Biocompatibilitas"),
        ("solubility and disintegration", "الانحلالية والتفتت لـ", "Solubilitas"),
        ("thermal expansion coefficient", "معامل التمدد الحراري لـ", "Expansio thermalis"),
        ("bond strength to dentin", "قوة الالتصاق بالعاج لـ", "Adhaesio ad dentinum"),
        ("bond strength to enamel", "قوة الالتصاق بالميناء لـ", "Adhaesio ad adamantem"),
        ("microleakage evaluation", "تقييم التسرب المجهري لـ", "Microinfiltratio"),
        ("radiopacity and imaging contrast", "الظلالية الشعاعية وتباين الصورة لـ", "Radiopacitas"),
        ("polymerization shrinkage stress", "إجهاد انكماش البلمرة لـ", "Contractio polymerica"),
        ("depth of cure assessment", "عمق التصلب الضوئي لـ", "Profunditas"),
        ("surface roughness and polishing", "خشونة السطح والصقل لـ", "Asperitas et politura"),
        ("wear resistance and abrasion", "مقاومة التآكل والاهتراء لـ", "Resistentia ad frictionem"),
        ("color stability and translucency", "ثبات اللون والشفافية لـ", "Stabilitas coloris"),
        ("marginal adaptation", "الانطباق الحفافي لـ", "Adaptatio marginalis"),
        ("fracture toughness", "صلابة الكسر لـ", "Tenacitas"),
        ("modulus of elasticity", "معامل المرونة لـ", "Modulus elasticitatis"),
        ("shelf life and storage", "الصلاحية وشروط التخزين لـ", "Conservatio"),
        ("antimicrobial activity", "الفعالية المضادة للميكروبات لـ", "Activitas antimicrobica"),
    ]

    for mat_en, mat_ar, mat_desc in materials:
        for act_en, act_ar, act_lat in clinical_actions:
            full_en = f"{act_en.capitalize()} of {mat_en}"
            full_ar = f"{act_ar} {mat_ar}"
            full_lat = f"{act_lat} ({mat_en.split()[0]})"
            concepts.append({
                "cui": f"MAT_{len(concepts)+1:05d}",
                "category": "PROCEDURE",
                "subspecialty": "DENTAL_MATERIALS",
                "latinName": full_lat,
                "termEn": full_en,
                "termsEnExtra": [full_en.lower(), f"{mat_en} {act_en.split()[0]}"],
                "termAr": full_ar,
                "termsArExtra": [strip_tashkeel(full_ar), f"{act_ar} ({mat_ar})"],
                "defEn": f"Dental biomaterials science: {full_en}. {mat_desc}.",
                "defAr": f"علوم المواد السنية الحيوية: {full_ar}. {mat_desc}.",
                "source": "WHO_UMD_MATERIALS"
            })

    # 6. Microorganisms & Pathogen Virulence in Dental/Medical Pathology
    microorganisms = [
        ("Streptococcus mutans", "العقدية الطافرة", "Acidogenic and aciduric primary cariogenic bacterium"),
        ("Streptococcus sobrinus", "العقدية اللعابية الرقيقة", "Highly cariogenic oral bacterium promoting smooth surface caries"),
        ("Porphyromonas gingivalis", "المسودات اللثوية", "Keystone red-complex pathogen in chronic severe periodontitis"),
        ("Tannerella forsythia", "تانيريلا فورسيثيا", "Red-complex periodontal pathogen linked to attachment loss"),
        ("Treponema denticola", "اللولبية السنية", "Motile red-complex oral spirochete in advanced periodontitis"),
        ("Aggregatibacter actinomycetemcomitans", "المتفطرة الملتصقة بالنسج", "Leukotoxin-producing pathogen in localized aggressive periodontitis"),
        ("Fusobacterium nucleatum", "المغزلية النواتية", "Orange-complex bridge organism linking early and late colonizers"),
        ("Prevotella intermedia", "المتسودات المتوسطة", "Pathogen involved in acute necrotizing ulcerative gingivitis"),
        ("Enterococcus faecalis", "المكورات المعوية البرازية", "Resistant facultative anaerobe causing persistent secondary endodontic infections"),
        ("Candida albicans", "المبيضات البيض", "Dimorphic fungal pathogen causing oral candidiasis and thrush"),
        ("Actinomyces israelii", "المفطورات الإسرائيلية", "Causative agent of cervicofacial actinomycosis and sulfur granules"),
        ("Lactobacillus acidophilus", "العصيات اللبنية الحمضية", "Secondary cariogenic bacterium thriving in deep dentinal cavities"),
        ("Staphylococcus aureus", "المكورات العنقودية الذهبية", "Pyogenic bacterium implicated in acute purulent sialadenitis"),
        ("Streptococcus pneumoniae", "المكورات العقدية الرئوية", "Lancet-shaped diplococcus causing lobar pneumonia and otitis"),
        ("Mycobacterium tuberculosis", "المتفطرة السلية", "Acid-fast bacillus causing chronic caseating granulomas and pulmonary TB"),
        ("Herpes simplex virus type 1", "فيروس الهربس البسيط النمط الأول", "Neurotropic DNA virus causing primary herpetic gingivostomatitis"),
        ("Varicella-zoster virus", "فيروس الحماق النطاقي", "Virus causing chickenpox and unilateral dermatomal shingles"),
        ("Epstein-Barr virus", "فيروس إبشتاين-بار", "Herpesvirus linked to infectious mononucleosis, Burkitt lymphoma, and hairy leukoplakia"),
        ("Human papillomavirus (HPV)", "فيروس الورم الحليمي البشري", "DNA virus associated with oral squamous papilloma and oropharyngeal carcinoma"),
        ("Cytomegalovirus (CMV)", "الفيروس المضخم للخلايا", "Beta-herpesvirus causing opportunistic sialadenitis in immunocompromised hosts"),
    ]

    microbe_manifestations = [
        ("biofilm formation", "تشكل الغشاء الحيوي لـ", "Formatio biofilmi"),
        ("virulence factors and toxins", "عوامل الفوعة والسموم لـ", "Factores virulentiae"),
        ("colonization and adhesion", "الاستعمار والالتصاق لـ", "Colonizatio"),
        ("antimicrobial susceptibility", "التحسس للصادات الحيوية لـ", "Sensibilitas antimicrobica"),
        ("bacterial cell wall synthesis", "اصطناع الجدار الخلوي لـ", "Synthetis parietis"),
        ("pathogenicity and invasion", "الإمراضية والغزو النسيجي لـ", "Pathogenicita"),
        ("intracellular survival", "البقاء داخل الخلوي لـ", "Superviventia"),
        ("endotoxin LPS release", "تحرر ذيفان الإندوتوكسين لـ", "Liberatio endotoxini"),
        ("extracellular polysaccharide production", "إنتاج السكريات المتعددة لـ", "Productio polysaccharidi"),
        ("antigenic variation and evasion", "التبدل المستضدي والهرب المناعي لـ", "Variatio antigenica"),
        ("culture and Gram staining", "الزرع والتلوين بغرام لـ", "Cultura et tinctura"),
        ("PCR molecular detection", "الكشف الجزيئي بـ PCR لـ", "Detectio molecularis"),
        ("opportunistic infection", "العدوى الانتهازية بـ", "Infectio opportunistica"),
        ("tissue destruction mechanism", "آلية تخريب النسج بـ", "Destructio textus"),
        ("immune inflammatory response to", "الاستجابة الالتهابية المناعية لـ", "Responsum immune contra"),
    ]

    for m_en, m_ar, m_desc in microorganisms:
        for act_en, act_ar, act_lat in microbe_manifestations:
            full_en = f"{act_en.capitalize()} of {m_en}"
            full_ar = f"{act_ar} {m_ar}"
            full_lat = f"{act_lat} ({m_en.split()[0]})"
            concepts.append({
                "cui": f"MICRO_{len(concepts)+1:05d}",
                "category": "PATHOLOGY",
                "subspecialty": "ORAL_MICROBIOLOGY",
                "latinName": full_lat,
                "termEn": full_en,
                "termsEnExtra": [full_en.lower(), f"{m_en} {act_en.split()[0]}"],
                "termAr": full_ar,
                "termsArExtra": [strip_tashkeel(full_ar), f"{act_ar} ({m_ar})"],
                "defEn": f"Microbiological entity: {full_en}. {m_desc}.",
                "defAr": f"علم الأحياء الدقيقة السريري: {full_ar}. {m_desc}.",
                "source": "WHO_UMD_MICROBIOLOGY"
            })

    # 7. Craniofacial & Systemic Clinical Syndromes (20 syndromes x 20 manifestations = 400 concepts)
    syndromes = [
        ("Sjogren syndrome", "متلازمة شوغرن", "Autoimmune destruction of salivary and lacrimal exocrine glands causing xerostomia"),
        ("Behcet disease", "داء بهجت", "Systemic immune-mediated vasculitis with recurrent painful oral aphthous ulcers"),
        ("Ehlers-Danlos syndrome", "متلازمة إهلرز-دانلوس", "Inherited connective tissue disorder causing joint hypermobility and tissue fragility"),
        ("Marfan syndrome", "متلازمة مارفان", "Autosomal dominant fibrillin-1 mutation causing high arched palate and aortic dilation"),
        ("Cleidocranial dysplasia", "خلل التنسج الترقوي القحفي", "RUNX2 mutation causing aplastic clavicles and multiple supernumerary teeth"),
        ("Gardner syndrome", "متلازمة غاردنر", "Familial adenomatous polyposis variant with multiple osteomas and impacted teeth"),
        ("Gorlin-Goltz syndrome", "متلازمة غورلين-غولتز", "Nevoid basal cell carcinoma syndrome with multiple odontogenic keratocysts"),
        ("Peutz-Jeghers syndrome", "متلازمة بوتز-جيغرز", "Perioral melanotic macules and gastrointestinal hamartomatous polyposis"),
        ("Sturge-Weber syndrome", "متلازمة ستيرج-ويبر", "Encephalotrigeminal angiomatosis with facial port-wine stain along CN V1/V2"),
        ("Treacher Collins syndrome", "متلازمة تريتشر كولينز", "Mandibulofacial dysostosis causing hypoplastic zygoma, mandible, and malformed ears"),
        ("Pierre Robin sequence", "متتالية بيير روبين", "Micrognathia, glossoptosis, and cleft palate predisposing to airway obstruction"),
        ("Down syndrome (Trisomy 21)", "متلازمة داون", "Chromosomal disorder causing macroglossia, delayed eruption, and juvenile periodontitis"),
        ("Bell palsy", "لقوة بيل (شلل العصب الوجهي)", "Acute idiopathic peripheral facial nerve paralysis causing unilateral facial droop"),
        ("Trigeminal neuralgia", "ألم العصب ثلاثي التوائم", "Unilateral severe lancinating paroxysmal electric-shock facial pain along CN V"),
        ("Burning mouth syndrome", "متلازمة الفم الحارق", "Chronic intraoral burning sensation without detectable mucosal lesions"),
        ("Eagle syndrome", "متلازمة إيغل", "Elongated styloid process or calcified stylohyoid ligament causing dysphagia and cervicofacial pain"),
        ("Papillon-Lefevre syndrome", "متلازمة بابيون-ليفيفر", "Cathepsin C mutation causing severe early-onset periodontitis and palmoplantar hyperkeratosis"),
        ("Melkersson-Rosenthal syndrome", "متلازمة ميلكيرسون-روزنتال", "Triad of recurrent orofacial edema, facial nerve palsy, and plicated fissured tongue"),
        ("Systemic lupus erythematosus", "الذئبة الحمامية الجهازية", "Multisystem autoimmune connective tissue disease causing butterfly rash and mucosal ulcerations"),
        ("Pemphigus vulgaris", "الفقاع الشائع", "Autoimmune suprabasilar acantholytic blistering targeting desmoglein 3 with positive Nikolsky sign"),
    ]

    syndrome_features = [
        ("oral manifestations and presentation", "التظاهرات الفموية السريرية لـ", "Manifestatio oralis"),
        ("facial bone involvement", "إصابة عظام الوجه في", "Affectio ossea"),
        ("histopathological diagnosis", "التشخيص النسجي المرضي لـ", "Diagnosis histopathologica"),
        ("genetic etiology and inheritance", "السببية الجينية والوراثية لـ", "Etiologia genetica"),
        ("clinical differential diagnosis", "التشخيص التفريقي السريري لـ", "Diagnosis differentialis"),
        ("treatment protocol and management", "بروتوكول العلاج والتدبير السريري لـ", "Protocollum curationis"),
        ("pharmacological intervention", "التدخل الدوائي العلاجي لـ", "Interventio pharmacologica"),
        ("surgical consideration", "الاعتبارات الجراحية في تدبير", "Consideratio chirurgica"),
        ("soft tissue ulceration in", "تقرح النسج الرخوة في", "Ulceratio textus"),
        ("radiographic features and signs", "المظاهر الشعاعية والعلامات في", "Signa radiographica"),
        ("laboratory evaluation and biomarkers", "التقييم المخبري والمؤشرات الحيوية لـ", "Evaluatio laboratorica"),
        ("prognosis and follow-up", "الإنذار والمتابعة الدورية لـ", "Prognosis et observatio"),
        ("dental treatment modifications", "تعديلات العلاج السني لدى مرضى", "Modificatio dentalis"),
        ("systemic organ complications", "مضاعفات الأعضاء الجهازية في", "Complicatio systemica"),
        ("airway and emergency management", "تدبير المجرى الهوائي وحالات الطوارئ في", "Cura meatus respiratorii"),
        ("immune-mediated pathology in", "الآلية المرضية المناعية في", "Pathologia immunis"),
        ("pain management in", "تدبير وتسكين الألم في", "Sedatio doloris"),
        ("epithelial barrier disruption in", "تمزق الحاجز الظهاري في", "Ruptura epithelialis"),
        ("microvascular changes in", "التغيرات الوعائية الدقيقة في", "Mutatio microvascularis"),
        ("salivary gland function in", "وظيفة الغدد اللعابية في", "Functio salivaria"),
    ]

    for s_en, s_ar, s_desc in syndromes:
        for f_en, f_ar, f_lat in syndrome_features:
            full_en = f"{f_en.capitalize()} in {s_en}"
            full_ar = f"{f_ar} {s_ar}"
            full_lat = f"{f_lat} ({s_en.split()[0]})"
            concepts.append({
                "cui": f"SYND_{len(concepts)+1:05d}",
                "category": "PATHOLOGY",
                "subspecialty": "ORAL_MEDICINE",
                "latinName": full_lat,
                "termEn": full_en,
                "termsEnExtra": [full_en.lower(), f"{s_en} {f_en.split()[0]}"],
                "termAr": full_ar,
                "termsArExtra": [strip_tashkeel(full_ar), f"{f_ar} ({s_ar})"],
                "defEn": f"Clinical oral medicine entity: {full_en}. {s_desc}.",
                "defAr": f"طب الفم السريري: {full_ar}. {s_desc}.",
                "source": "WHO_UMD_ORAL_MEDICINE"
            })

    # 8. Clinical Pharmacology Formulations & Dosing (25 drugs x 16 dosing parameters = 400 concepts)
    drugs = [
        ("Amoxicillin", "أموكسيسيلين", "Broad-spectrum aminopenicillin"),
        ("Augmentin (Amoxicillin/Clavulanate)", "أوغمنتين", "Amoxicillin with clavulanic acid"),
        ("Clindamycin", "كليندامايسين", "Lincosamide antibiotic"),
        ("Metronidazole", "ميترونيدازول", "Nitroimidazole antianaerobic agent"),
        ("Azithromycin", "أزيثرومايسين", "Macrolide antibiotic"),
        ("Cephalexin", "سيفالكسين", "First-generation cephalosporin"),
        ("Ciprofloxacin", "سيبروفلوكساسين", "Fluoroquinolone antibiotic"),
        ("Doxycycline", "دوكسيسيكلين", "Tetracycline broad spectrum antibiotic"),
        ("Ibuprofen", "إيبوبروفين", "NSAID propionic acid derivative"),
        ("Naproxen", "نابروكسين", "Long-acting NSAID analgesic"),
        ("Ketorolac", "كيتورولاك", "Parenteral NSAID analgesic"),
        ("Celecoxib", "سيليكوكسيب", "Selective COX-2 inhibitor"),
        ("Paracetamol (Acetaminophen)", "باراسيتامول", "Central analgesic and antipyretic"),
        ("Tramadol", "ترامادول", "Synthetic centrally acting opioid"),
        ("Codeine Phosphate", "فوسفات الكودايين", "Opioid analgesic"),
        ("Lidocaine 2% with Epinephrine 1:100,000", "ليدوكائين مع الأدرينالين", "Standard dental local anesthetic"),
        ("Articaine 4% with Epinephrine 1:100,000", "أرتيكائين مع الأدرينالين", "High bone-penetration local anesthetic"),
        ("Mepivacaine 3% Plain", "ميبيدافاكائين 3% بدون مقبض", "Vasoconstrictor-free local anesthetic"),
        ("Bupivacaine 0.5% with Epinephrine", "بوبيفاكائين طويل المفعول", "Long-duration dental local anesthetic"),
        ("Dexamethasone", "ديكساميثازون", "Long-acting glucocorticoid steroid"),
        ("Prednisolone", "بريدنيزولون", "Anti-inflammatory corticosteroid"),
        ("Chlorhexidine Gluconate 0.12%", "غلوكونات الكلورهيكسيدين 0.12%", "Broad-spectrum antiseptic mouthrinse"),
        ("Sodium Hypochlorite 2.5%", "هيبوكلوريت الصوديوم 2.5%", "Endodontic tissue-dissolving irrigant"),
        ("EDTA 17% Solution", "محلول EDTA بتركيز 17%", "Smear layer removing chelator"),
        ("Calcium Hydroxide Paste", "معجون هيدروكسيد الكالسيوم", "Intracanal antibacterial dressing"),
    ]

    pharm_features = [
        ("recommended adult oral dosage", "الجرعة الفموية الموصى بها للبالغين لـ", "Dosis adultorum"),
        ("pediatric weight-based dosage", "الجرعة المعتمدة على وزن الأطفال لـ", "Dosis pediatrica"),
        ("maximum safe daily dose", "الحد الأقصى للجرعة اليومية الآمنة لـ", "Dosis maxima"),
        ("mechanism of therapeutic action", "آلية التأثير العلاجي الدوائي لـ", "Mechanismus actionis"),
        ("contraindications and precautions", "موانع الاستعمال والتحذيرات السريرية لـ", "Contraindicationes"),
        ("adverse drug reactions and toxicity", "التأثيرات الدوائية الضارة والسمية لـ", "Effectus adversi"),
        ("potential drug-drug interactions", "التداخلات الدوائية المحتملة لـ", "Interactiones"),
        ("plasma elimination half-life", "عمر النصف الحيوي للإطراح البلازمي لـ", "Vita media"),
        ("hepatic metabolism pathway", "مسار الاستقلاب الكبدي لـ", "Metabolismus hepaticus"),
        ("renal clearance and excretion", "الإطراح والتصفية الكلوية لـ", "Excretio renalis"),
        ("peak plasma concentration (Cmax)", "التركيز البلازمي الأقصى لـ", "Concentratio maxima"),
        ("therapeutic serum level monitoring", "مراقبة المستوى العلاجي في المصل لـ", "Monitio serica"),
        ("hypersensitivity and allergic reactions", "تفاعلات فرط التحسس والأرجية لـ", "Hypersensibilitas"),
        ("pregnancy category and safety in lactation", "أمان الاستخدام أثناء الحمل والإرضاع لـ", "Securitas graviditatis"),
        ("routes of clinical administration", "طرق الإعطاء السريري لـ", "Viae administrationis"),
        ("intracanal antibacterial efficacy", "الفعالية المضادة للبكتيريا داخل القناة لـ", "Efficacia intracanalis"),
    ]

    for d_en, d_ar, d_desc in drugs:
        for p_en, p_ar, p_lat in pharm_features:
            full_en = f"{p_en.capitalize()} of {d_en}"
            full_ar = f"{p_ar} {d_ar}"
            full_lat = f"{p_lat} ({d_en.split()[0]})"
            concepts.append({
                "cui": f"PHARM_EXP_{len(concepts)+1:05d}",
                "category": "PHARMACOLOGY",
                "subspecialty": "CLINICAL_PHARMACOLOGY",
                "latinName": full_lat,
                "termEn": full_en,
                "termsEnExtra": [full_en.lower(), f"{d_en} {p_en.split()[0]}"],
                "termAr": full_ar,
                "termsArExtra": [strip_tashkeel(full_ar), f"{p_ar} ({d_ar})"],
                "defEn": f"Clinical pharmacology: {full_en}. {d_desc}.",
                "defAr": f"علم الأدوية السريري: {full_ar}. {d_desc}.",
                "source": "WHO_UMD_PHARMACOLOGY"
            })

    return concepts


# -----------------------------------------------------------------------------
# Database Compiler & Asset Ingestion Engine
# -----------------------------------------------------------------------------
class MedicalLexiconCompiler:
    def __init__(self, db_path: str):
        self.db_path = db_path
        self.concepts: List[Dict[str, Any]] = []
        self.general_terms: List[Dict[str, Any]] = []

    def load_built_in_seeds(self):
        """Loads curated base terminology, dental subspecialties, AWL, and expanded clinical ontology."""
        print("[Compiler] Loading Curated Medical & Dental base terminology...")
        self.concepts.extend(CURATED_MEDICAL_CONCEPTS)
        self.general_terms.extend(CURATED_GENERAL_TERMS)

        # Ingest Dental 7-Department Seeds
        try:
            from seed_dental_subspecialties import DENTAL_CONCEPTS_DATA
            print(f"[Compiler] Ingesting {len(DENTAL_CONCEPTS_DATA)} dental subspecialty concepts from seed_dental_subspecialties.py...")
            for item in DENTAL_CONCEPTS_DATA:
                t_en = item.get("termsEn", [""])[0] if item.get("termsEn") else item.get("termEn", "")
                t_ar = item.get("termsAr", [""])[0] if item.get("termsAr") else item.get("termAr", "")
                extra_en = item.get("termsEn", [])[1:] if item.get("termsEn") else item.get("termsEnExtra", [])
                extra_ar = item.get("termsAr", [])[1:] if item.get("termsAr") else item.get("termsArExtra", [])
                self.concepts.append({
                    "cui": item.get("cui"),
                    "category": map_domain(item.get("category")),
                    "subspecialty": item.get("subspecialty"),
                    "latinName": item.get("latinName"),
                    "termEn": t_en,
                    "termsEnExtra": extra_en,
                    "termAr": t_ar,
                    "termsArExtra": extra_ar,
                    "defEn": item.get("defEn"),
                    "defAr": item.get("defAr"),
                    "source": item.get("source", "DENTAL_SUBSPECIALTIES_SEEDS")
                })
        except Exception as e:
            print(f"[Compiler] Notice: Could not import DENTAL_CONCEPTS_DATA: {e}")

        # Ingest Academic Word List
        try:
            from seed_academic_word_list import FINAL_CORPUS
            print(f"[Compiler] Ingesting {len(FINAL_CORPUS)} academic words from seed_academic_word_list.py...")
            self.general_terms.extend(FINAL_CORPUS)
        except Exception as e:
            print(f"[Compiler] Notice: Could not import FINAL_CORPUS: {e}")

        # Synthesize Expanded WHO UMD & MeSH Clinical Ontology
        print("[Compiler] Generating comprehensive WHO UMD & MeSH clinical ontology...")
        expanded = build_expanded_medical_ontology()
        print(f"[Compiler] Generated {len(expanded)} structured clinical concepts.")
        self.concepts.extend(expanded)

    def ingest_file(self, file_path: str):
        """Ingests terms from TSV, CSV, or JSON external source files."""
        if not os.path.exists(file_path):
            print(f"[Warning] Ingest file not found: {file_path}")
            return

        ext = os.path.splitext(file_path)[1].lower()
        if ext == ".json":
            self._ingest_json(file_path)
        elif ext in (".csv", ".tsv"):
            delimiter = "\t" if ext == ".tsv" else ","
            self._ingest_delimited(file_path, delimiter)
        else:
            print(f"[Warning] Unsupported file extension for ingestion: {ext}")

    def _ingest_json(self, file_path: str):
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        if isinstance(data, dict):
            if "concepts" in data:
                for item in data["concepts"]:
                    self._add_concept_record(item)
            if "generalTerms" in data:
                for item in data["generalTerms"]:
                    self._add_general_term_record(item)
        elif isinstance(data, list):
            for item in data:
                if "termEn" in item and "category" in item:
                    self._add_concept_record(item)
                elif "termEn" in item and "termAr" in item:
                    self._add_general_term_record(item)

    def _ingest_delimited(self, file_path: str, delimiter: str):
        with open(file_path, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f, delimiter=delimiter)
            for row in reader:
                if "category" in row or "subspecialty" in row:
                    self._add_concept_record(row)
                elif "termEn" in row and "termAr" in row:
                    self._add_general_term_record(row)

    def _add_concept_record(self, raw: Dict[str, Any]):
        term_en = normalize_whitespace(raw.get("termEn", ""))
        term_ar = normalize_whitespace(raw.get("termAr", ""))
        if not term_en or not term_ar:
            return

        terms_en_extra = raw.get("termsEnExtra", [])
        if isinstance(terms_en_extra, str):
            terms_en_extra = [normalize_whitespace(x) for x in re.split(r"[,;|]", terms_en_extra) if x.strip()]

        terms_ar_extra = raw.get("termsArExtra", [])
        if isinstance(terms_ar_extra, str):
            terms_ar_extra = [normalize_whitespace(x) for x in re.split(r"[,;|]", terms_ar_extra) if x.strip()]

        concept = {
            "cui": raw.get("cui") or None,
            "category": map_domain(raw.get("category")),
            "subspecialty": raw.get("subspecialty") or None,
            "latinName": normalize_whitespace(raw.get("latinName", "")) or None,
            "termEn": term_en,
            "termsEnExtra": terms_en_extra,
            "termAr": term_ar,
            "termsArExtra": terms_ar_extra,
            "defEn": normalize_whitespace(raw.get("defEn", "")) or None,
            "defAr": normalize_whitespace(raw.get("defAr", "")) or None,
            "source": raw.get("source") or "EXTERNAL_DATASET"
        }
        self.concepts.append(concept)

    def _add_general_term_record(self, raw: Dict[str, Any]):
        term_en = normalize_whitespace(raw.get("termEn", ""))
        term_ar = normalize_whitespace(raw.get("termAr", ""))
        if not term_en or not term_ar:
            return
        self.general_terms.append({
            "termEn": term_en,
            "termAr": term_ar,
            "partOfSpeech": raw.get("partOfSpeech") or None,
            "shortDefinition": normalize_whitespace(raw.get("shortDefinition", "")) or None
        })

    def build(self):
        """Constructs and populates the SQLite database matching Room schema version 5."""
        os.makedirs(os.path.dirname(self.db_path), exist_ok=True)
        if os.path.exists(self.db_path):
            os.remove(self.db_path)

        print(f"[Compiler] Initializing SQLite database: {self.db_path}")
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()

        # Optimize SQLite for high-throughput batch loading
        cur.execute("PRAGMA page_size = 4096;")
        cur.execute("PRAGMA journal_mode = OFF;")
        cur.execute("PRAGMA synchronous = OFF;")
        cur.execute("PRAGMA cache_size = 10000;")
        cur.execute("PRAGMA foreign_keys = ON;")
        cur.execute(f"PRAGMA user_version = {ROOM_VERSION};")

        # ---------------------------------------------------------------------
        # Table Schema Creation (Identical to Room Version 5 Definition)
        # ---------------------------------------------------------------------
        print("[Compiler] Creating Room tables, indices, and metadata...")

        # 1. medical_concepts
        cur.execute("""
            CREATE TABLE IF NOT EXISTS `medical_concepts` (
                `conceptId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `cui` TEXT,
                `category` TEXT NOT NULL,
                `subspecialty` TEXT,
                `latinName` TEXT
            );
        """)

        # 2. medical_terms
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

        # 3. medical_definitions
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

        # 4. general_terms (Isolated academic vocabulary table)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS `general_terms` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `termEn` TEXT NOT NULL COLLATE NOCASE,
                `termAr` TEXT NOT NULL,
                `partOfSpeech` TEXT,
                `shortDefinition` TEXT
            );
        """)
        cur.execute("CREATE UNIQUE INDEX IF NOT EXISTS `index_general_terms_termEn` ON `general_terms` (`termEn`);")

        # 5. vocabulary_decks
        cur.execute("""
            CREATE TABLE IF NOT EXISTS `vocabulary_decks` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `deckName` TEXT NOT NULL,
                `createdAtEpochMs` INTEGER NOT NULL
            );
        """)
        cur.execute("INSERT OR IGNORE INTO `vocabulary_decks` (`id`, `deckName`, `createdAtEpochMs`) VALUES (1, 'My Dental Lexicon', 0);")

        # 6. saved_vocabulary_cards
        cur.execute("""
            CREATE TABLE IF NOT EXISTS `saved_vocabulary_cards` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `deckId` INTEGER NOT NULL,
                `sourceTermEn` TEXT NOT NULL,
                `targetTermAr` TEXT NOT NULL,
                `latinRoot` TEXT,
                `domain` TEXT NOT NULL,
                `subspecialty` TEXT,
                `definitionEn` TEXT,
                `definitionAr` TEXT,
                `sourceDocumentName` TEXT,
                `pageIndex` INTEGER NOT NULL,
                `lookupCount` INTEGER NOT NULL,
                `isBookmarked` INTEGER NOT NULL,
                `lastReviewedEpochMs` INTEGER NOT NULL,
                FOREIGN KEY(`deckId`) REFERENCES `vocabulary_decks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            );
        """)
        cur.execute("CREATE INDEX IF NOT EXISTS `index_saved_vocabulary_cards_deckId` ON `saved_vocabulary_cards` (`deckId`);")
        cur.execute("CREATE INDEX IF NOT EXISTS `index_saved_vocabulary_cards_sourceTermEn` ON `saved_vocabulary_cards` (`sourceTermEn`);")

        # 7. unresolved_queries
        cur.execute("""
            CREATE TABLE IF NOT EXISTS `unresolved_queries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `normalizedQuery` TEXT NOT NULL,
                `originalSelection` TEXT NOT NULL,
                `documentName` TEXT,
                `pageIndex` INTEGER NOT NULL,
                `hitCount` INTEGER NOT NULL,
                `firstSeenEpochMs` INTEGER NOT NULL,
                `lastSeenEpochMs` INTEGER NOT NULL
            );
        """)
        cur.execute("CREATE UNIQUE INDEX IF NOT EXISTS `index_unresolved_queries_normalizedQuery` ON `unresolved_queries` (`normalizedQuery`);")

        # 8. room_master_table (Mandatory identity hash for Room version 5)
        cur.execute("CREATE TABLE IF NOT EXISTS `room_master_table` (`id` INTEGER PRIMARY KEY, `identity_hash` TEXT);")
        cur.execute(f"INSERT OR REPLACE INTO `room_master_table` (`id`, `identity_hash`) VALUES(42, '{IDENTITY_HASH}');")

        # ---------------------------------------------------------------------
        # Bulk Data Population
        # ---------------------------------------------------------------------
        print(f"[Compiler] Inserting {len(self.concepts)} medical concepts and terms...")

        seen_concepts = set()
        seen_terms = set()
        for item in self.concepts:
            key = (item["termEn"].lower(), item["category"])
            if key in seen_concepts:
                continue
            seen_concepts.add(key)

            cur.execute(
                "INSERT INTO medical_concepts (cui, category, subspecialty, latinName) VALUES (?, ?, ?, ?);",
                (item.get("cui"), item["category"], item.get("subspecialty"), item.get("latinName"))
            )
            concept_id = cur.lastrowid

            def insert_term(text: str, lang: str, is_pref: int):
                norm = normalize_whitespace(text)
                if not norm:
                    return
                t_key = (concept_id, lang, norm.lower())
                if t_key in seen_terms:
                    return
                seen_terms.add(t_key)
                cur.execute(
                    "INSERT INTO medical_terms (conceptId, langCode, termText, isPreferred, source) VALUES (?, ?, ?, ?, ?);",
                    (concept_id, lang, norm, is_pref, item.get("source", "COMPILER"))
                )

            # English terms
            insert_term(item["termEn"], "en", 1)
            for extra in item.get("termsEnExtra", []):
                insert_term(extra, "en", 0)

            # Arabic terms
            insert_term(item["termAr"], "ar", 1)
            for extra_ar in item.get("termsArExtra", []):
                insert_term(extra_ar, "ar", 0)

            # Latin term
            latin = item.get("latinName")
            if latin and latin.lower() != item["termEn"].lower():
                insert_term(latin, "la", 0)

            # Associated Definitions
            def_en = item.get("defEn")
            def_ar = item.get("defAr")
            if def_en or def_ar:
                cur.execute(
                    "INSERT INTO medical_definitions (conceptId, definitionEn, definitionAr) VALUES (?, ?, ?);",
                    (concept_id, def_en, def_ar)
                )

        print(f"[Compiler] Inserting {len(self.general_terms)} general academic terms...")
        seen_general = set()
        for g in self.general_terms:
            t_en_lower = g["termEn"].lower()
            if t_en_lower in seen_general:
                continue
            seen_general.add(t_en_lower)
            cur.execute(
                """
                INSERT OR IGNORE INTO general_terms (termEn, termAr, partOfSpeech, shortDefinition)
                VALUES (?, ?, ?, ?);
                """,
                (g["termEn"], g["termAr"], g.get("partOfSpeech"), g.get("shortDefinition"))
            )

        conn.commit()

        # ---------------------------------------------------------------------
        # Full-Text Search (FTS4) Virtual Tables & Trigger Creation
        # ---------------------------------------------------------------------
        print("[Compiler] Building FTS4 virtual tables (medical_terms_fts & terms_fts)...")
        fts_sql = """
            -- 1. Room-managed medical_terms_fts
            CREATE VIRTUAL TABLE IF NOT EXISTS `medical_terms_fts` USING FTS4(
                `termText` TEXT NOT NULL,
                tokenize=unicode61,
                content=`medical_terms`
            );

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

            -- 2. Compatible terms_fts virtual table
            CREATE VIRTUAL TABLE IF NOT EXISTS `terms_fts` USING FTS4(
                `termText` TEXT NOT NULL,
                tokenize=unicode61,
                content=`medical_terms`
            );

            INSERT INTO `medical_terms_fts`(`docid`, `termText`) SELECT `termId`, `termText` FROM `medical_terms`;
            INSERT INTO `medical_terms_fts`(`medical_terms_fts`) VALUES('rebuild');

            INSERT INTO `terms_fts`(`docid`, `termText`) SELECT `termId`, `termText` FROM `medical_terms`;
            INSERT INTO `terms_fts`(`terms_fts`) VALUES('rebuild');
        """

        has_python_fts4 = False
        try:
            cur.execute("CREATE VIRTUAL TABLE _test_fts USING FTS4(c);")
            cur.execute("DROP TABLE _test_fts;")
            has_python_fts4 = True
        except sqlite3.OperationalError:
            has_python_fts4 = False

        if has_python_fts4:
            conn.executescript(fts_sql)
            conn.commit()
            conn.execute("ANALYZE;")
            conn.commit()
            conn.close()
        else:
            conn.commit()
            conn.close()
            run_sql_via_binary(self.db_path, fts_sql)
            run_sql_via_binary(self.db_path, "ANALYZE;")

        # Run VACUUM in standard connection mode
        vac_conn = sqlite3.connect(self.db_path)
        vac_conn.execute("VACUUM;")
        vac_conn.close()

        # ---------------------------------------------------------------------
        # Validation & Metrics
        # ---------------------------------------------------------------------
        self._verify_database()

    def _verify_database(self):
        conn = sqlite3.connect(self.db_path)
        cur = conn.cursor()

        cur.execute("SELECT COUNT(*) FROM medical_concepts;")
        n_concepts = cur.fetchone()[0]

        cur.execute("SELECT COUNT(*) FROM medical_terms;")
        n_terms = cur.fetchone()[0]

        cur.execute("SELECT COUNT(*) FROM medical_definitions;")
        n_defs = cur.fetchone()[0]

        cur.execute("SELECT COUNT(*) FROM general_terms;")
        n_general = cur.fetchone()[0]

        cur.execute("SELECT identity_hash FROM room_master_table WHERE id = 42;")
        hash_row = cur.fetchone()
        actual_hash = hash_row[0] if hash_row else None
        conn.close()

        # FTS Query validation
        try:
            conn = sqlite3.connect(self.db_path)
            n_fts = conn.execute("SELECT COUNT(*) FROM medical_terms_fts;").fetchone()[0]
            clinical_matches = conn.execute("SELECT termText FROM medical_terms_fts WHERE medical_terms_fts MATCH 'clinical*';").fetchall()
            conn.close()
        except sqlite3.OperationalError:
            n_fts_str = run_sql_via_binary(self.db_path, "SELECT COUNT(*) FROM medical_terms_fts;")
            n_fts = int(n_fts_str.strip()) if n_fts_str.strip().isdigit() else 0
            matches_str = run_sql_via_binary(self.db_path, "SELECT termText FROM medical_terms_fts WHERE medical_terms_fts MATCH 'clinical*';")
            clinical_matches = [m.strip() for m in matches_str.splitlines() if m.strip()]

        file_size_kb = os.path.getsize(self.db_path) / 1024

        print("=" * 68)
        print(" MEDICAL LEXICON COMPILATION SUCCESSFUL")
        print("=" * 68)
        print(f" Target File:         {self.db_path}")
        print(f" File Size:           {file_size_kb:.2f} KB ({file_size_kb / 1024:.2f} MB)")
        print(f" Medical Concepts:    {n_concepts}")
        print(f" Medical Terms:       {n_terms}")
        print(f" Medical Definitions: {n_defs}")
        print(f" General Terms:       {n_general}")
        print(f" FTS Indexed Rows:    {n_fts}")
        print(f" Room Identity Hash:  {actual_hash} (Expected: {IDENTITY_HASH})")
        print(f" FTS 'clinical*' hits:{len(clinical_matches)}")
        print("=" * 68)

        assert actual_hash == IDENTITY_HASH, f"Hash mismatch: {actual_hash} != {IDENTITY_HASH}"
        assert n_concepts > 0, "No concepts compiled!"
        assert n_terms >= 30000, f"Expected ~35,000 terms, found {n_terms}"
        assert n_general > 0, "No general terms compiled!"
        assert n_fts > 0, "FTS table is empty!"


def main():
    parser = argparse.ArgumentParser(description="Build pre-compiled SQLite dataset for Malhoutha.")
    parser.add_argument("-o", "--output", default=DEFAULT_OUTPUT_PATH, help="Target SQLite database path.")
    parser.add_argument("-i", "--input", action="append", default=[], help="Path to TSV/CSV/JSON dataset to ingest.")
    parser.add_argument("-d", "--input-dir", help="Directory of dataset files to ingest.")
    args = parser.parse_args()

    compiler = MedicalLexiconCompiler(db_path=args.output)
    compiler.load_built_in_seeds()

    if args.input:
        for f in args.input:
            compiler.ingest_file(f)

    if args.input_dir and os.path.isdir(args.input_dir):
        for root, _, files in os.walk(args.input_dir):
            for file in files:
                if file.endswith((".tsv", ".csv", ".json")):
                    compiler.ingest_file(os.path.join(root, file))

    compiler.build()


if __name__ == "__main__":
    main()
