# Medical Lexicon SQLite Asset Pipeline

This directory is designated for the pre-packaged offline medical lexicon SQLite database (`medical_lexicon.db`).

## Runtime Lifecycle & Initialization
1. **Pre-packaged Asset Mode**:
   - When `medical_lexicon.db` is present in this directory, `MedicalLexiconDatabase` automatically calls `createFromAsset("databases/medical_lexicon.db")` to load the database instantly with pre-indexed FTS tables.
2. **Dynamic In-Code Seeder Fallback**:
   - If `medical_lexicon.db` is not present, `MedicalLexiconDatabase` initializes an empty SQLite schema via Room and executes `MedicalLexiconSeeder.seedDefaultLexicon(...)` inside `RoomDatabase.Callback.onCreate()`.
   - This ensures immediate offline functionality for essential oral pathology, anatomy, and pharmacology terms (*Ameloblastoma*, *Alveolar bone*, *Lichen planus*, *Amoxicillin*, *Pulpitis*, *Mental foramen*, etc.).

## Seed Script
The database creation script is available at:
`scripts/build_medical_lexicon_db.py`
