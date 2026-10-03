package com.chethan616.clearpdf.ui.paper

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PaperConfigDto(
    val type: String,
    val baseColorArgb: Int,
    val lineRuleColorArgb: Int,
    val marginRuleColorArgb: Int,
    val gridSpacingDp: Float
)

fun PaperConfig.toDto(): PaperConfigDto = PaperConfigDto(
    type = type.name,
    baseColorArgb = baseColor.toArgb(),
    lineRuleColorArgb = lineRuleColor.toArgb(),
    marginRuleColorArgb = marginRuleColor.toArgb(),
    gridSpacingDp = gridSpacingDp
)

fun PaperConfigDto.toConfig(): PaperConfig {
    val templateType = try {
        PaperTemplateType.valueOf(type)
    } catch (_: Exception) {
        PaperTemplateType.RULED_COLLEGE
    }
    return PaperConfig(
        type = templateType,
        baseColor = Color(baseColorArgb),
        lineRuleColor = Color(lineRuleColorArgb),
        marginRuleColor = Color(marginRuleColorArgb),
        gridSpacingDp = gridSpacingDp
    )
}

/**
 * Manages paper configurations for note documents in Malhoutha.
 */
object NotePaperManager {
    private const val PREFS_NAME = "malhoutha_note_paper"
    private const val KEY_LAST_CONFIG = "last_paper_config"
    private const val PREFIX_DOC = "doc_paper_"
    private val json = Json { ignoreUnknownKeys = true }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun savePaperConfig(context: Context, uri: Uri, config: PaperConfig) {
        savePaperConfig(context, uri.toString(), config)
    }

    fun savePaperConfig(context: Context, uriString: String, config: PaperConfig) {
        val dto = config.toDto()
        val serialized = json.encodeToString(dto)
        prefs(context).edit()
            .putString(PREFIX_DOC + uriString, serialized)
            .putString(KEY_LAST_CONFIG, serialized)
            .apply()
    }

    fun getPaperConfig(context: Context, uri: Uri): PaperConfig? {
        return getPaperConfig(context, uri.toString())
    }

    fun getPaperConfig(context: Context, uriString: String): PaperConfig? {
        val raw = prefs(context).getString(PREFIX_DOC + uriString, null) ?: return null
        return try {
            json.decodeFromString<PaperConfigDto>(raw).toConfig()
        } catch (_: Exception) {
            null
        }
    }

    fun getLastConfig(context: Context): PaperConfig {
        val raw = prefs(context).getString(KEY_LAST_CONFIG, null)
        if (raw != null) {
            try {
                return json.decodeFromString<PaperConfigDto>(raw).toConfig()
            } catch (_: Exception) {}
        }
        return PaperColorPreset.WHITE.applyTo(PaperTemplateType.RULED_COLLEGE)
    }
}
