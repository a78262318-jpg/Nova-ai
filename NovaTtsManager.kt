package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

data class VoiceOption(
    val name: String,
    val localeTag: String,
    val displayLabel: String,
    val isNetworkRequired: Boolean
)

class NovaTtsManager(context: Context) {

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceOption>>(emptyList())
    val availableVoices: StateFlow<List<VoiceOption>> = _availableVoices.asStateFlow()

    private var onCompleteCallback: (() -> Unit)? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            _isReady.value = true
            refreshVoices()
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                onCompleteCallback?.invoke()
                onCompleteCallback = null
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                onCompleteCallback = null
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                onCompleteCallback = null
            }
        })
    }

    fun refreshVoices() {
        val voices = runCatching { tts.voices?.toList().orEmpty() }.getOrDefault(emptyList())
        val filtered = voices.filter { v ->
            val lang = v.locale.language.lowercase()
            lang in listOf("bn", "hi", "en")
        }.sortedWith(
            compareByDescending<Voice> { isLikelyFemale(it) }
                .thenBy { it.locale.language }
                .thenBy { it.name }
        ).map { v ->
            val femaleTag = if (isLikelyFemale(v)) "Female" else "Standard"
            val offlineTag = if (v.isNetworkConnectionRequired) "Online" else "Offline"
            VoiceOption(
                name = v.name,
                localeTag = v.locale.toLanguageTag(),
                displayLabel = "${v.locale.displayLanguage} ($femaleTag • $offlineTag) — ${v.name.takeLast(14)}",
                isNetworkRequired = v.isNetworkConnectionRequired
            )
        }
        _availableVoices.value = filtered
    }

    fun speak(
        text: String,
        languageCode: String = "en",
        speed: Float = 1.0f,
        pitch: Float = 1.08f,
        preferredVoiceName: String = "",
        onDone: (() -> Unit)? = null
    ) {
        if (!_isReady.value || text.isBlank()) {
            onDone?.invoke()
            return
        }
        onCompleteCallback = onDone

        val targetLocale = when (languageCode) {
            "bn" -> Locale("bn", "BD")
            "hi" -> Locale("hi", "IN")
            else -> Locale.US
        }
        tts.language = targetLocale
        tts.setSpeechRate(speed.coerceIn(0.6f, 1.6f))
        tts.setPitch(pitch.coerceIn(0.75f, 1.35f))

        selectBestFemaleVoice(targetLocale, preferredVoiceName)?.let { chosen ->
            runCatching { tts.voice = chosen }
        }

        val cleanSpeech = text
            .replace("**", "")
            .replace("`", "")
            .replace("#", "")
            .trim()
        val utteranceId = UUID.randomUUID().toString()
        tts.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        onCompleteCallback = null
        runCatching { tts.stop() }
        _isSpeaking.value = false
    }

    fun shutdown() {
        stop()
        runCatching { tts.shutdown() }
    }

    private fun selectBestFemaleVoice(locale: Locale, preferredVoiceName: String): Voice? {
        val voices = runCatching { tts.voices?.toList().orEmpty() }.getOrDefault(emptyList())
        if (preferredVoiceName.isNotBlank()) {
            val exact = voices.firstOrNull {
                it.name == preferredVoiceName && it.locale.language == locale.language
            }
            if (exact != null) return exact
        }
        val langVoices = voices.filter { it.locale.language.equals(locale.language, ignoreCase = true) }
        return langVoices.firstOrNull { isLikelyFemale(it) && !it.isNetworkConnectionRequired }
            ?: langVoices.firstOrNull { isLikelyFemale(it) }
            ?: langVoices.firstOrNull()
    }

    private fun isLikelyFemale(voice: Voice): Boolean {
        val n = voice.name.lowercase()
        val f = voice.features.joinToString(" ").lowercase()
        return n.contains("female") || n.contains("-f-") || n.endsWith("f") ||
            n.contains("sfg") || n.contains("bn-bd-x-ban") || n.contains("hi-in-x-hia") ||
            n.contains("en-us-x-sfg") || n.contains("en-us-x-tpf") ||
            f.contains("female")
    }
}
