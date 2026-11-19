package com.example.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.ai.LanguageDetector
import com.example.ai.OfflineCommandEngine
import com.example.ai.WakeWordParser
import com.example.data.local.NovaDatabase
import com.example.data.local.NovaLocalRepository
import com.example.data.preferences.UserPreferencesRepository
import com.example.device.DeviceStatsProvider
import com.example.device.PhoneController
import com.example.voice.HeyNovaKeywordSpotter
import com.example.voice.KwsModelState
import com.example.voice.NovaSpeechManager
import com.example.voice.NovaTtsManager
import com.example.voice.VoskModelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

enum class WakeEngineMode(val label: String) {
    INACTIVE("Inactive"),
    VOSK_KWS_ACTIVE("Vosk KWS (AudioRecord 16kHz)"),
    SPEECH_FALLBACK_ACTIVE("SpeechRecognizer Fallback"),
    FOLLOW_UP_LISTENING("Listening for Command"),
    PAUSED_BATTERY_SAVER("Paused (Battery Saver)")
}

/**
 * Foreground Service (`foregroundServiceType="microphone"`) that keeps the microphone active
 * in the background and runs the lightweight Vosk grammar-constrained Keyword Spotter
 * ([HeyNovaKeywordSpotter]) over continuous 16 kHz PCM [android.media.AudioRecord] frames
 * to detect "Hey NOVA" / "NOVA" while the app is backgrounded.
 *
 * When the offline Vosk acoustic model is still downloading or not yet installed, the service
 * seamlessly falls back to [NovaSpeechManager] and hot-switches to Vosk KWS once ready.
 */
class NovaVoiceService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var keywordSpotter: HeyNovaKeywordSpotter
    private lateinit var speechManager: NovaSpeechManager
    private lateinit var ttsManager: NovaTtsManager
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var offlineEngine: OfflineCommandEngine
    private lateinit var statsProvider: DeviceStatsProvider

    private var wakeLock: PowerManager.WakeLock? = null
    private var serviceActive = false
    private var awaitFollowUpCommand = false
    private var consecutiveErrorCount = 0
    private var restartJob: Job? = null
    private var energyMirrorJob: Job? = null
    private var modelObserverJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        keywordSpotter = HeyNovaKeywordSpotter(this)
        speechManager = NovaSpeechManager(this)
        ttsManager = NovaTtsManager(this)
        prefsRepo = UserPreferencesRepository(this)
        statsProvider = DeviceStatsProvider(this)
        val localRepo = NovaLocalRepository(NovaDatabase.getInstance(this))
        offlineEngine = OfflineCommandEngine(this, statsProvider, PhoneController(this), localRepo)
        VoskModelManager.refreshState(this)

        energyMirrorJob = serviceScope.launch {
            keywordSpotter.micEnergy.collect { energy ->
                _micEnergy.value = energy
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                serviceScope.launch { prefsRepo.setWakeWordEnabled(false) }
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RELOAD_ENGINE -> {
                if (serviceActive) {
                    stopActiveRecognizers()
                    startDetectionEngine()
                }
                return START_STICKY
            }
        }

        val hasMic = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMic) {
            _statusMessage.value = "Permission denied: Microphone permission required."
            _engineMode.value = WakeEngineMode.INACTIVE
            _isRunning.value = false
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification("Initializing microphone for \"NOVA\" / \"Hey NOVA\"...")
        try {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
        } catch (e: Exception) {
            _statusMessage.value = "Blocked by Android background mic restriction: ${e.localizedMessage}"
            _engineMode.value = WakeEngineMode.INACTIVE
            _isRunning.value = false
            stopSelf()
            return START_NOT_STICKY
        }

        _isRunning.value = true
        acquirePartialWakeLock()

        if (!serviceActive) {
            serviceActive = true
            observeModelReadinessForHotSwitch()
            startDetectionEngine()
        }
        return START_STICKY
    }

    private fun observeModelReadinessForHotSwitch() {
        modelObserverJob?.cancel()
        modelObserverJob = serviceScope.launch {
            VoskModelManager.state.collect { modelState ->
                if (serviceActive &&
                    modelState is KwsModelState.Ready &&
                    _engineMode.value == WakeEngineMode.SPEECH_FALLBACK_ACTIVE &&
                    !awaitFollowUpCommand &&
                    !ttsManager.isSpeaking.value
                ) {
                    speechManager.stopListening()
                    startVoskKeywordSpotting(modelState.modelDir)
                }
            }
        }
    }

    private fun startDetectionEngine() {
        if (!serviceActive) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _statusMessage.value = "Microphone permission revoked. Service stopped."
            stopSelf()
            return
        }

        if (ttsManager.isSpeaking.value) {
            scheduleRestart(700L)
            return
        }

        serviceScope.launch {
            val prefs = prefsRepo.preferencesFlow.first()
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (prefs.batterySaverWakePause && pm?.isPowerSaveMode == true) {
                stopActiveRecognizers()
                _engineMode.value = WakeEngineMode.PAUSED_BATTERY_SAVER
                _statusMessage.value = "Paused during Android Battery Saver"
                updateForegroundNotification("Paused (Battery Saver active)")
                scheduleRestart(5000L)
                return@launch
            }

            if (awaitFollowUpCommand) {
                startFollowUpSpeechCapture(prefs.preferredLanguage.code)
                return@launch
            }

            val readyModelDir = VoskModelManager.getReadyModelDirOrNull(this@NovaVoiceService)
            if (readyModelDir != null) {
                startVoskKeywordSpotting(readyModelDir)
            } else {
                // Kick off offline Vosk model download automatically when online so we upgrade seamlessly
                if (statsProvider.getNetworkTelemetry().isConnected) {
                    VoskModelManager.startDownloadIfNeeded(this@NovaVoiceService)
                }
                startSpeechFallbackCycle(prefs.preferredLanguage.code)
            }
        }
    }

    private fun startVoskKeywordSpotting(modelDir: File) {
        if (!serviceActive) return
        speechManager.stopListening()

        val started = keywordSpotter.start(
            modelDir = modelDir,
            onKeywordDetected = { detectedPhrase ->
                serviceScope.launch {
                    consecutiveErrorCount = 0
                    _lastHeard.value = detectedPhrase
                    // Pause AudioRecord capture immediately so TTS & follow-up SpeechRecognizer get clean mic access
                    keywordSpotter.pauseCapture()
                    handleWakeWordTrigger(isBareWakeWord = true, command = "")
                }
            },
            onError = { errMsg ->
                serviceScope.launch {
                    if (!serviceActive) return@launch
                    if (errMsg.contains("permission", ignoreCase = true)) {
                        _statusMessage.value = errMsg
                        stopSelf()
                        return@launch
                    }
                    // Gracefully degrade to SpeechRecognizer fallback if AudioRecord hardware is busy
                    _statusMessage.value = "KWS fallback: $errMsg"
                    consecutiveErrorCount = (consecutiveErrorCount + 1).coerceAtMost(4)
                    val prefs = prefsRepo.preferencesFlow.first()
                    startSpeechFallbackCycle(prefs.preferredLanguage.code)
                }
            }
        )

        if (started) {
            _engineMode.value = WakeEngineMode.VOSK_KWS_ACTIVE
            _statusMessage.value = "Vosk KWS Active • Say \"NOVA\" or \"Hey NOVA\""
            updateForegroundNotification("Vosk KWS active • Say \"NOVA\" or \"Hey NOVA\"")
        }
    }

    private fun startSpeechFallbackCycle(preferredLangCode: String) {
        if (!serviceActive) return
        keywordSpotter.pauseCapture()
        _engineMode.value = WakeEngineMode.SPEECH_FALLBACK_ACTIVE

        val modelNote = when (val st = VoskModelManager.state.value) {
            is KwsModelState.Downloading -> " (downloading KWS ${st.progressPercent}%)"
            is KwsModelState.Extracting -> " (preparing KWS model)"
            else -> ""
        }
        _statusMessage.value = "Listening for \"NOVA\" / \"Hey NOVA\"$modelNote"
        updateForegroundNotification("Listening for \"NOVA\" / \"Hey NOVA\"$modelNote")

        val langTag = LanguageDetector.toBcp47Tag(
            if (preferredLangCode == "auto") "en" else preferredLangCode
        )

        speechManager.startListening(
            languageTag = langTag,
            preferOffline = true,
            onResult = { transcript ->
                consecutiveErrorCount = 0
                _lastHeard.value = transcript

                val parsed = WakeWordParser.parse(transcript)
                if (parsed.wakeDetected) {
                    handleWakeWordTrigger(parsed.isBareWakeWord, parsed.strippedCommand)
                } else {
                    scheduleRestart(550L)
                }
            },
            onError = { errMsg ->
                consecutiveErrorCount = (consecutiveErrorCount + 1).coerceAtMost(4)
                if (errMsg.contains("permission", ignoreCase = true)) {
                    _statusMessage.value = errMsg
                    stopSelf()
                    return@startListening
                }
                val backoffMs = (800L * consecutiveErrorCount).coerceAtMost(3500L)
                scheduleRestart(backoffMs)
            }
        )
    }

    private fun startFollowUpSpeechCapture(preferredLangCode: String) {
        if (!serviceActive) return
        keywordSpotter.pauseCapture()
        _engineMode.value = WakeEngineMode.FOLLOW_UP_LISTENING
        _statusMessage.value = "Listening for your command..."
        updateForegroundNotification("Activated • Listening for your command...")

        val langTag = LanguageDetector.toBcp47Tag(
            if (preferredLangCode == "auto") "en" else preferredLangCode
        )

        speechManager.startListening(
            languageTag = langTag,
            preferOffline = !statsProvider.getNetworkTelemetry().isConnected,
            onResult = { transcript ->
                awaitFollowUpCommand = false
                consecutiveErrorCount = 0
                _lastHeard.value = transcript
                executeCommandInService(transcript)
            },
            onError = { _ ->
                awaitFollowUpCommand = false
                scheduleRestart(600L)
            }
        )
    }

    private fun handleWakeWordTrigger(isBareWakeWord: Boolean, command: String) {
        serviceScope.launch {
            val prefs = prefsRepo.preferencesFlow.first()
            val snap = statsProvider.getSnapshot()

            if (snap.isDeviceLocked && !prefs.lockScreenSupportEnabled) {
                _statusMessage.value = "Ignored while screen locked (Lock-Screen Support is OFF)"
                scheduleRestart(1000L)
                return@launch
            }

            if (isBareWakeWord) {
                val wakeReply = getString(R.string.nova_wake_response)
                _statusMessage.value = "Activated: $wakeReply"
                updateForegroundNotification("Activated • \"$wakeReply\"")
                ttsManager.speak(
                    text = wakeReply,
                    languageCode = "en",
                    speed = prefs.ttsSpeed,
                    pitch = prefs.ttsPitch,
                    preferredVoiceName = prefs.selectedVoiceName
                ) {
                    if (!snap.isDeviceLocked) {
                        val openIntent = Intent(this@NovaVoiceService, MainActivity::class.java).apply {
                            putExtra(EXTRA_OPEN_VOICE, true)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        runCatching { startActivity(openIntent) }
                    }
                    awaitFollowUpCommand = true
                    scheduleRestart(300L)
                }
            } else {
                executeCommandInService(command)
            }
        }
    }

    private fun executeCommandInService(command: String) {
        serviceScope.launch {
            val prefs = prefsRepo.preferencesFlow.first()
            val snap = statsProvider.getSnapshot()
            val lang = LanguageDetector.detectLanguage(command, prefs.preferredLanguage)
            val outcome = offlineEngine.tryHandleCommand(
                rawInput = command,
                languageCode = lang,
                isDeviceLocked = snap.isDeviceLocked,
                forceOffline = !snap.network.isConnected
            )
            val response = if (outcome.handled) {
                outcome.replyText
            } else {
                "Opening NOVA for your AI question."
            }
            _statusMessage.value = "Responded: $response"
            ttsManager.speak(
                text = response,
                languageCode = lang,
                speed = prefs.ttsSpeed,
                pitch = prefs.ttsPitch,
                preferredVoiceName = prefs.selectedVoiceName
            ) {
                if (!outcome.handled && !snap.isDeviceLocked) {
                    val openIntent = Intent(this@NovaVoiceService, MainActivity::class.java).apply {
                        putExtra(EXTRA_OPEN_VOICE, true)
                        putExtra(EXTRA_PENDING_COMMAND, command)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    runCatching { startActivity(openIntent) }
                }
                scheduleRestart(800L)
            }
        }
    }

    private fun stopActiveRecognizers() {
        restartJob?.cancel()
        keywordSpotter.pauseCapture()
        speechManager.stopListening()
    }

    private fun updateForegroundNotification(contentText: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching {
            nm.notify(NOTIFICATION_ID, buildForegroundNotification(contentText))
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.nova_voice_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.nova_voice_channel_desc)
            }
            nm.createNotificationChannel(channel)
        }

        val openPending = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply { putExtra(EXTRA_OPEN_VOICE, true) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPending = PendingIntent.getService(
            this,
            1,
            Intent(this, NovaVoiceService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("NOVA Microphone Active")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openPending)
            .addAction(0, "Stop Listening", stopPending)
            .build()
    }

    @SuppressLint("WakelockTimeout")
    private fun acquirePartialWakeLock() {
        runCatching {
            if (wakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NovaAI::KwsMicLock")
                    ?.apply { setReferenceCounted(false) }
            }
            if (wakeLock?.isHeld == false) {
                // Bounded timeout (2 hours max per acquisition) to prevent runaway drain
                wakeLock?.acquire(2 * 60 * 60 * 1000L)
            }
        }
    }

    private fun releasePartialWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        }
        wakeLock = null
    }

    private fun scheduleRestart(delayMs: Long) {
        if (!serviceActive) return
        restartJob?.cancel()
        restartJob = serviceScope.launch {
            delay(delayMs)
            startDetectionEngine()
        }
    }

    override fun onDestroy() {
        serviceActive = false
        restartJob?.cancel()
        energyMirrorJob?.cancel()
        modelObserverJob?.cancel()
        keywordSpotter.release()
        speechManager.destroy()
        ttsManager.shutdown()
        releasePartialWakeLock()
        serviceScope.cancel()
        _isRunning.value = false
        _engineMode.value = WakeEngineMode.INACTIVE
        _micEnergy.value = 0f
        if (!_statusMessage.value.startsWith("Permission denied") &&
            !_statusMessage.value.startsWith("Blocked by Android")
        ) {
            _statusMessage.value = "Inactive"
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "nova_voice_wake_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP = "com.example.ACTION_STOP_VOICE_SERVICE"
        const val ACTION_RELOAD_ENGINE = "com.example.ACTION_RELOAD_KWS_ENGINE"
        const val EXTRA_OPEN_VOICE = "extra_open_voice"
        const val EXTRA_PENDING_COMMAND = "extra_pending_command"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _engineMode = MutableStateFlow(WakeEngineMode.INACTIVE)
        val engineMode: StateFlow<WakeEngineMode> = _engineMode.asStateFlow()

        private val _micEnergy = MutableStateFlow(0f)
        val micEnergy: StateFlow<Float> = _micEnergy.asStateFlow()

        private val _lastHeard = MutableStateFlow("")
        val lastHeard: StateFlow<String> = _lastHeard.asStateFlow()

        private val _statusMessage = MutableStateFlow("Inactive")
        val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

        fun start(context: Context): Boolean {
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasMic) {
                _statusMessage.value = "Permission denied: Grant Microphone permission first."
                _engineMode.value = WakeEngineMode.INACTIVE
                return false
            }
            return try {
                val intent = Intent(context, NovaVoiceService::class.java)
                ContextCompat.startForegroundService(context, intent)
                true
            } catch (e: Exception) {
                _statusMessage.value = "Cannot start background mic service: ${e.localizedMessage}"
                _engineMode.value = WakeEngineMode.INACTIVE
                false
            }
        }

        fun reloadEngineIfRunning(context: Context) {
            if (!_isRunning.value) return
            runCatching {
                val intent = Intent(context, NovaVoiceService::class.java).apply {
                    action = ACTION_RELOAD_ENGINE
                }
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, NovaVoiceService::class.java))
        }
    }
}
