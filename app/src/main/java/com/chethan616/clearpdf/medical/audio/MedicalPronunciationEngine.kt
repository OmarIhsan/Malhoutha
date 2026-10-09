package com.chethan616.clearpdf.medical.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Offline-first, lifecycle-aware Text-To-Speech pronunciation engine for medical terminology,
 * Latin anatomical binomials, and clinical phonetics.
 */
class MedicalPronunciationEngine(context: Context) : TextToSpeech.OnInitListener {

    enum class PlaybackState { IDLE, INITIALIZING, PLAYING, ERROR }

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)

    private val _playbackState = MutableStateFlow(PlaybackState.INITIALIZING)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentlyPlayingText = MutableStateFlow<String?>(null)
    val currentlyPlayingText: StateFlow<String?> = _currentlyPlayingText.asStateFlow()

    private var isInitialized = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            isInitialized = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            _playbackState.value = if (isInitialized) PlaybackState.IDLE else PlaybackState.ERROR
            setupProgressListener()
        } else {
            _playbackState.value = PlaybackState.ERROR
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _playbackState.value = PlaybackState.PLAYING
            }

            override fun onDone(utteranceId: String?) {
                _playbackState.value = PlaybackState.IDLE
                _currentlyPlayingText.value = null
            }

            @Deprecated("Deprecated in Java", ReplaceWith("onError(utteranceId)"))
            override fun onError(utteranceId: String?) {
                _playbackState.value = PlaybackState.ERROR
                _currentlyPlayingText.value = null
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _playbackState.value = PlaybackState.ERROR
                _currentlyPlayingText.value = null
            }
        })
    }

    /**
     * Speaks the target term. Supports switching between English (en-US)
     * and Latin pronunciation rules when Latin anatomical roots are passed.
     */
    fun speakTerm(
        text: String,
        isLatin: Boolean = false,
        pitch: Float = 1.0f,
        speechRate: Float = 0.9f
    ) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        if (!isInitialized || tts == null) return

        tts?.apply {
            stop()
            setPitch(pitch)
            setSpeechRate(speechRate) // Slightly slowed down (0.9x) for clear medical phonetics

            // For Latin roots, prefer Italian/Latin phonetic locale if available, else standard US/UK
            if (isLatin) {
                val latinLocale = Locale("la")
                val available = isLanguageAvailable(latinLocale)
                if (available >= TextToSpeech.LANG_AVAILABLE) {
                    language = latinLocale
                } else {
                    language = Locale.US
                }
            } else {
                language = Locale.US
            }

            _currentlyPlayingText.value = trimmed
            _playbackState.value = PlaybackState.PLAYING
            speak(trimmed, TextToSpeech.QUEUE_FLUSH, null, "utterance_${System.currentTimeMillis()}")
        }
    }

    fun isPlayingText(text: String?): Boolean {
        return _playbackState.value == PlaybackState.PLAYING &&
            _currentlyPlayingText.value?.equals(text?.trim(), ignoreCase = true) == true
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _playbackState.value = PlaybackState.IDLE
        _currentlyPlayingText.value = null
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
        _currentlyPlayingText.value = null
        _playbackState.value = PlaybackState.IDLE
        if (INSTANCE == this) {
            INSTANCE = null
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MedicalPronunciationEngine? = null

        fun getInstance(context: Context): MedicalPronunciationEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MedicalPronunciationEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
