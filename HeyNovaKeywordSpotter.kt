package com.example.voice

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/**
 * Pure parser for Vosk grammar-constrained JSON outputs:
 * - Final result: `{"text": "hey nova"}`
 * - Partial result: `{"partial": "hey nova"}`
 */
object KwsJsonParser {

    const val HEY_NOVA_GRAMMAR = """["nova", "hey nova", "hi nova", "ok nova", "[unk]"]"""

    private val wakePhrases = listOf("hey nova", "hi nova", "ok nova", "nova")

    fun extractDetectedKeyword(jsonText: String): String? {
        if (jsonText.isBlank()) return null
        return try {
            val obj = JSONObject(jsonText)
            val candidate = obj.optString("text", "")
                .ifBlank { obj.optString("partial", "") }
                .trim()
                .lowercase()
            if (candidate.isEmpty() || candidate == "[unk]") return null
            val tokens = candidate.split(Regex("\\s+")).filter { it.isNotBlank() && it != "[unk]" }
            if (tokens.isEmpty()) return null
            val cleaned = tokens.joinToString(" ")
            wakePhrases.firstOrNull { phrase ->
                cleaned == phrase || cleaned.endsWith(" $phrase") || cleaned.contains(phrase)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Computes a normalized [0f..1f] microphone audio energy level from 16-bit PCM samples.
     */
    fun computeNormalizedRms(pcmSamples: ShortArray, sampleCount: Int): Float {
        if (sampleCount <= 0) return 0f
        val count = sampleCount.coerceAtMost(pcmSamples.size)
        var sumSq = 0.0
        for (i in 0 until count) {
            val v = pcmSamples[i].toDouble() / 32768.0
            sumSq += v * v
        }
        val rms = sqrt(sumSq / count)
        // Amplify typical conversational voice RMS (~0.02..0.25) to [0..1]
        return (rms * 5.0).toFloat().coerceIn(0f, 1f)
    }
}

/**
 * Lightweight on-device Keyword Spotting (KWS) engine built on [AudioRecord] + Vosk Grammar Mode.
 * Keeps the 16 kHz mono PCM microphone stream active inside a microphone Foreground Service
 * and continuously evaluates audio chunks strictly against the restricted "Hey NOVA" grammar.
 */
class HeyNovaKeywordSpotter(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isSpotting = MutableStateFlow(false)
    val isSpotting: StateFlow<Boolean> = _isSpotting.asStateFlow()

    private val _micEnergy = MutableStateFlow(0f)
    val micEnergy: StateFlow<Float> = _micEnergy.asStateFlow()

    private val running = AtomicBoolean(false)
    private var spotterJob: Job? = null
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var loadedModelPath: String? = null
    private var lastTriggerTimeMs: Long = 0L

    fun hasRecordAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Starts continuous 16 kHz PCM capture and grammar-constrained Vosk keyword spotting.
     * Must only be invoked while `RECORD_AUDIO` is granted and ideally from a foreground service
     * with `foregroundServiceType="microphone"`.
     */
    @SuppressLint("MissingPermission")
    fun start(
        modelDir: File,
        onKeywordDetected: (keyword: String) -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        if (!hasRecordAudioPermission()) {
            onError("Permission denied: RECORD_AUDIO is required for keyword spotting.")
            return false
        }
        if (!VoskModelManager.isValidModelDirectory(modelDir)) {
            onError("Offline Vosk KWS model is not ready.")
            return false
        }
        if (running.getAndSet(true)) {
            return true
        }

        spotterJob = scope.launch {
            try {
                LibVosk.setLogLevel(LogLevel.WARNINGS)
                if (model == null || loadedModelPath != modelDir.absolutePath) {
                    runCatching { model?.close() }
                    model = Model(modelDir.absolutePath)
                    loadedModelPath = modelDir.absolutePath
                }

                runCatching { recognizer?.close() }
                recognizer = Recognizer(model, SAMPLE_RATE_HZ.toFloat(), KwsJsonParser.HEY_NOVA_GRAMMAR)

                val minBufferBytes = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE_HZ,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBufferBytes <= 0) {
                    throw IllegalStateException("Device audio hardware rejected 16 kHz mono PCM input.")
                }

                // 200ms frame buffer at 16kHz 16-bit mono = 6400 bytes
                val bufferBytes = maxOf(minBufferBytes * 2, 6400)
                val record = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE_HZ,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferBytes
                )

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    record.release()
                    throw IllegalStateException("AudioRecord failed to initialize.")
                }

                audioRecord = record
                record.startRecording()
                _isSpotting.value = true

                val shortBuffer = ShortArray(FRAME_SAMPLES)
                while (isActive && running.get()) {
                    val readSamples = record.read(shortBuffer, 0, shortBuffer.size)
                    if (readSamples > 0) {
                        _micEnergy.value = KwsJsonParser.computeNormalizedRms(shortBuffer, readSamples)
                        val rec = recognizer ?: break

                        val isFinal = rec.acceptWaveForm(shortBuffer, readSamples)
                        val json = if (isFinal) rec.result else rec.partialResult
                        val detected = KwsJsonParser.extractDetectedKeyword(json)
                        if (detected != null) {
                            val now = System.currentTimeMillis()
                            if (now - lastTriggerTimeMs >= TRIGGER_COOLDOWN_MS) {
                                lastTriggerTimeMs = now
                                rec.reset()
                                onKeywordDetected(detected)
                            }
                        }
                    } else if (readSamples == AudioRecord.ERROR_INVALID_OPERATION ||
                        readSamples == AudioRecord.ERROR_BAD_VALUE ||
                        readSamples == AudioRecord.ERROR_DEAD_OBJECT
                    ) {
                        throw IllegalStateException("AudioRecord read error ($readSamples)")
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Microphone permission error in KWS loop", e)
                onError("Microphone permission revoked: ${e.localizedMessage}")
            } catch (t: Throwable) {
                if (running.get()) {
                    Log.e(TAG, "Vosk KWS runtime error", t)
                    onError("Keyword spotter stopped: ${t.localizedMessage ?: t::class.simpleName}")
                }
            } finally {
                releaseAudioOnly()
                _isSpotting.value = false
                _micEnergy.value = 0f
                running.set(false)
            }
        }
        return true
    }

    /**
     * Stops active microphone recording so TTS playback or full-sentence SpeechRecognizer
     * can acquire the microphone without contention, while keeping the Vosk [Model] cached in RAM.
     */
    fun pauseCapture() {
        running.set(false)
        spotterJob?.cancel()
        spotterJob = null
        releaseAudioOnly()
        _isSpotting.value = false
        _micEnergy.value = 0f
    }

    private fun releaseAudioOnly() {
        runCatching {
            audioRecord?.let { rec ->
                if (rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    rec.stop()
                }
                rec.release()
            }
        }
        audioRecord = null
        runCatching { recognizer?.close() }
        recognizer = null
    }

    fun release() {
        pauseCapture()
        runCatching { model?.close() }
        model = null
        loadedModelPath = null
        scope.cancel()
    }

    companion object {
        private const val TAG = "HeyNovaKeywordSpotter"
        const val SAMPLE_RATE_HZ = 16000
        private const val FRAME_SAMPLES = 3200 // 200ms frames at 16kHz
        private const val TRIGGER_COOLDOWN_MS = 2500L
    }
}
