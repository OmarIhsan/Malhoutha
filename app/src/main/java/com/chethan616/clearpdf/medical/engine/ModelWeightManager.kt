package com.chethan616.clearpdf.medical.engine

import android.content.Context
import java.io.File

/**
 * Manages storage, file discovery, and integrity verification for on-device
 * quantized Small Language Model (SLM) weights.
 */
object ModelWeightManager {

    /** Subdirectory under app files reserved for medical model weights. */
    const val MODEL_DIR_NAME = "medical_models"

    /** Recognized filenames for INT4 quantized LLM task/bin weights. */
    val CANDIDATE_FILENAMES = listOf(
        "medical_slm.bin",
        "medical_qwen2.5_1.5b_int4.bin",
        "gemma-2b-it-cpu-int4.bin",
        "gemma-2b-it-gpu-int4.bin",
        "model.bin",
        "model.task"
    )

    /** Minimum byte threshold to ensure model weights are non-empty (~10MB sanity check). */
    private const val MIN_VALID_MODEL_BYTES = 10L * 1024L * 1024L

    /**
     * Returns true if a valid on-device model weights file exists in the designated directories.
     */
    fun isModelAvailable(context: Context): Boolean {
        return getActiveModelFile(context) != null
    }

    /**
     * Resolves the primary directory where model weights can be placed or downloaded.
     */
    fun getPrimaryModelDirectory(context: Context): File {
        val dir = File(context.filesDir, MODEL_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Locates the active SLM weight file across internal and external storage locations.
     */
    fun getActiveModelFile(context: Context): File? {
        val searchDirs = listOfNotNull(
            File(context.filesDir, MODEL_DIR_NAME),
            context.getExternalFilesDir(MODEL_DIR_NAME),
            context.getExternalFilesDir(null)?.let { File(it, MODEL_DIR_NAME) }
        )

        for (dir in searchDirs) {
            if (!dir.exists() || !dir.isDirectory) continue

            // 1. Direct candidate matching
            for (filename in CANDIDATE_FILENAMES) {
                val candidate = File(dir, filename)
                if (isValidModelFile(candidate)) {
                    return candidate
                }
            }

            // 2. Fallback scan for any .bin or .task file exceeding threshold
            val anyModel = dir.listFiles { file ->
                file.isFile && (file.name.endsWith(".bin") || file.name.endsWith(".task"))
            }?.firstOrNull { isValidModelFile(it) }

            if (anyModel != null) {
                return anyModel
            }
        }

        return null
    }

    /**
     * Returns the size in bytes of the active model, or 0 if missing.
     */
    fun getActiveModelSizeBytes(context: Context): Long {
        return getActiveModelFile(context)?.length() ?: 0L
    }

    private fun isValidModelFile(file: File): Boolean {
        return file.exists() && file.isFile && file.length() >= MIN_VALID_MODEL_BYTES
    }
}
