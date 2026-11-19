package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ai.GeminiBrainRepository
import com.example.ai.GeminiStreamEvent
import com.example.ai.LanguageDetector
import com.example.ai.OfflineCommandEngine
import com.example.ai.SensitiveActionRequest
import com.example.ai.WakeWordParser
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.data.local.NovaDatabase
import com.example.data.local.NovaLocalRepository
import com.example.data.local.ReminderEntity
import com.example.data.preferences.NetworkMode
import com.example.data.preferences.NovaPersonality
import com.example.data.preferences.NovaPreferences
import com.example.data.preferences.NovaThemeMode
import com.example.data.preferences.PreferredLanguage
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.OnlineInfoRepository
import com.example.device.ActionResult
import com.example.device.ContactInfo
import com.example.device.DeviceStatsProvider
import com.example.device.DeviceTelemetrySnapshot
import com.example.device.InstalledAppInfo
import com.example.device.PhoneController
import com.example.service.NovaVoiceService
import com.example.service.WakeEngineMode
import com.example.voice.KwsModelState
import com.example.voice.NovaSpeechManager
import com.example.voice.NovaTtsManager
import com.example.voice.VoiceOption
import com.example.voice.VoskModelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class OrbVisualState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR,
    OFFLINE
}

sealed interface AiUiState {
    data object Idle : AiUiState
    data object Loading : AiUiState
    data class Streaming(val text: String) : AiUiState
    data class Success(val text: String) : AiUiState
    data class Error(val message: String) : AiUiState
}

class NovaViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val localRepo = NovaLocalRepository(NovaDatabase.getInstance(appContext))
    private val prefsRepo = UserPreferencesRepository(appContext)
    private val statsProvider = DeviceStatsProvider(appContext)
    val phoneController = PhoneController(appContext)
    private val onlineInfoRepo = OnlineInfoRepository()
    private val geminiBrain = GeminiBrainRepository(onlineInfoRepo)
    private val offlineEngine = OfflineCommandEngine(appContext, statsProvider, phoneController, localRepo)
    private val ttsManager = NovaTtsManager(appContext)
    private val speechManager = NovaSpeechManager(appContext)

    val preferences: StateFlow<NovaPreferences> = prefsRepo.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, NovaPreferences())

    val chatMessages: StateFlow<List<ChatMessageEntity>> = localRepo.messages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = localRepo.memories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = localRepo.notes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ReminderEntity>> = localRepo.reminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking
    val isListening: StateFlow<Boolean> = speechManager.isListening
    val rmsLevel: StateFlow<Float> = speechManager.rmsLevel
    val partialTranscript: StateFlow<String> = speechManager.partialTranscript
    val availableVoices: StateFlow<List<VoiceOption>> = ttsManager.availableVoices

    val wakeServiceRunning: StateFlow<Boolean> = NovaVoiceService.isRunning
    val wakeServiceStatus: StateFlow<String> = NovaVoiceService.statusMessage
    val wakeEngineMode: StateFlow<WakeEngineMode> = NovaVoiceService.engineMode
    val kwsModelState: StateFlow<KwsModelState> = VoskModelManager.state

    private val _telemetry = MutableStateFlow(statsProvider.getSnapshot())
    val telemetry: StateFlow<DeviceTelemetrySnapshot> = _telemetry.asStateFlow()

    private val _aiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val aiState: StateFlow<AiUiState> = _aiState.asStateFlow()

    private val _orbState = MutableStateFlow(OrbVisualState.IDLE)
    val orbState: StateFlow<OrbVisualState> = _orbState.asStateFlow()

    private val _lastAssistantReply = MutableStateFlow("")
    val lastAssistantReply: StateFlow<String> = _lastAssistantReply.asStateFlow()

    private val _lastDetectedLang = MutableStateFlow("en")
    val lastDetectedLang: StateFlow<String> = _lastDetectedLang.asStateFlow()

    private val _pendingSensitiveAction = MutableStateFlow<SensitiveActionRequest?>(null)
    val pendingSensitiveAction: StateFlow<SensitiveActionRequest?> = _pendingSensitiveAction.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _mediaVolumePercent = MutableStateFlow(phoneController.getMediaVolumePercent())
    val mediaVolumePercent: StateFlow<Int> = _mediaVolumePercent.asStateFlow()

    private val _brightnessPercent = MutableStateFlow(phoneController.getSystemBrightnessPercent())
    val brightnessPercent: StateFlow<Int> = _brightnessPercent.asStateFlow()

    private val _pendingBrightnessPermission = MutableStateFlow<Int?>(null)
    val pendingBrightnessPermission: StateFlow<Int?> = _pendingBrightnessPermission.asStateFlow()

    private val _flashlightOn = MutableStateFlow(false)
    val flashlightOn: StateFlow<Boolean> = _flashlightOn.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactInfo>>(emptyList())
    val contacts: StateFlow<List<ContactInfo>> = _contacts.asStateFlow()

    val isOnlineEffective: StateFlow<Boolean> = combine(
        _telemetry,
        preferences
    ) { snap, prefs ->
        when (prefs.networkMode) {
            NetworkMode.OFFLINE_ONLY -> false
            NetworkMode.ONLINE_ONLY -> snap.network.isConnected
            NetworkMode.AUTO -> snap.network.isConnected
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null
    private var activeCommandJob: Job? = null

    init {
        VoskModelManager.refreshState(appContext)
        observeOrbSynchronization()
        registerNetworkMonitor()
        startTelemetryTicker()
        loadInstalledApps()
    }

    private fun observeOrbSynchronization() {
        viewModelScope.launch {
            combine(
                isListening,
                isSpeaking,
                _aiState,
                isOnlineEffective
            ) { listening, speaking, ai, online ->
                when {
                    listening -> OrbVisualState.LISTENING
                    speaking -> OrbVisualState.SPEAKING
                    ai is AiUiState.Loading || ai is AiUiState.Streaming -> OrbVisualState.PROCESSING
                    ai is AiUiState.Error -> OrbVisualState.ERROR
                    !online -> OrbVisualState.OFFLINE
                    else -> OrbVisualState.IDLE
                }
            }.collect { state ->
                _orbState.value = state
            }
        }
    }

    private fun registerNetworkMonitor() {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refreshTelemetry()
            override fun onLost(network: Network) = refreshTelemetry()
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) = refreshTelemetry()
        }
        connectivityCallback = callback
        runCatching {
            cm.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
        }
    }

    private fun startTelemetryTicker() {
        viewModelScope.launch {
            while (true) {
                refreshTelemetry()
                delay(4000L)
            }
        }
    }

    fun refreshTelemetry() {
        _telemetry.value = statsProvider.getSnapshot()
        _mediaVolumePercent.value = phoneController.getMediaVolumePercent()
        _brightnessPercent.value = phoneController.getSystemBrightnessPercent()
    }

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = phoneController.getInstalledLauncherApps()
        }
    }

    fun loadContacts(query: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            _contacts.value = phoneController.readContacts(query = query, limit = 60)
        }
    }

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun cancelAiRequest() {
        activeCommandJob?.cancel()
        activeCommandJob = null
        _aiState.value = AiUiState.Idle
        _statusBanner.value = "Request cancelled."
    }

    fun testWakeWordNova() {
        val wakeReply = appContext.getString(R.string.nova_wake_response)
        _lastDetectedLang.value = "en"
        _lastAssistantReply.value = wakeReply
        _aiState.value = AiUiState.Success(wakeReply)
        viewModelScope.launch {
            localRepo.addMessage(
                ChatMessageEntity(
                    role = "user",
                    content = "Hey NOVA",
                    languageCode = "en",
                    source = "wake",
                    isVoiceInput = true
                )
            )
            localRepo.addMessage(
                ChatMessageEntity(
                    role = "assistant",
                    content = wakeReply,
                    languageCode = "en",
                    source = "wake"
                )
            )
            val prefs = preferences.value
            ttsManager.speak(
                text = wakeReply,
                languageCode = "en",
                speed = prefs.ttsSpeed,
                pitch = prefs.ttsPitch,
                preferredVoiceName = prefs.selectedVoiceName
            ) {
                if (prefs.continuousConversation) {
                    startVoiceListening(speakResult = true)
                }
            }
        }
    }

    fun startVoiceListening(speakResult: Boolean = true) {
        if (ttsManager.isSpeaking.value) {
            ttsManager.stop()
        }
        val prefs = preferences.value
        val forceOffline = prefs.networkMode == NetworkMode.OFFLINE_ONLY || !isOnlineEffective.value
        val langTag = when (prefs.preferredLanguage) {
            PreferredLanguage.BENGALI -> "bn-BD"
            PreferredLanguage.HINDI -> "hi-IN"
            PreferredLanguage.ENGLISH -> "en-US"
            PreferredLanguage.AUTO -> LanguageDetector.toBcp47Tag(_lastDetectedLang.value)
        }

        speechManager.startListening(
            languageTag = langTag,
            preferOffline = forceOffline,
            onResult = { transcript ->
                submitCommand(
                    rawInput = transcript,
                    fromVoice = true,
                    speakReply = speakResult
                )
            },
            onError = { err ->
                _aiState.value = AiUiState.Error(err)
                _statusBanner.value = err
            }
        )
    }

    fun stopVoiceListening() {
        speechManager.stopListening()
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun submitCommand(
        rawInput: String,
        fromVoice: Boolean = false,
        speakReply: Boolean = fromVoice
    ) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return

        if (ttsManager.isSpeaking.value) {
            ttsManager.stop()
        }
        activeCommandJob?.cancel()

        activeCommandJob = viewModelScope.launch {
            val prefs = preferences.value
            val snap = statsProvider.getSnapshot()
            _telemetry.value = snap

            val wake = WakeWordParser.parse(trimmed)
            if (wake.wakeDetected && wake.isBareWakeWord) {
                testWakeWordNova()
                return@launch
            }

            val effectiveText = if (wake.wakeDetected) wake.strippedCommand else trimmed
            val lang = LanguageDetector.detectLanguage(effectiveText, prefs.preferredLanguage)
            _lastDetectedLang.value = lang

            localRepo.addMessage(
                ChatMessageEntity(
                    role = "user",
                    content = trimmed,
                    languageCode = lang,
                    source = if (fromVoice) "voice" else "chat",
                    isVoiceInput = fromVoice
                )
            )

            _aiState.value = AiUiState.Loading

            val forceOffline = prefs.networkMode == NetworkMode.OFFLINE_ONLY
            val offlineOutcome = offlineEngine.tryHandleCommand(
                rawInput = effectiveText,
                languageCode = lang,
                isDeviceLocked = snap.isDeviceLocked,
                forceOffline = forceOffline
            )

            if (offlineOutcome.handled) {
                if (offlineOutcome.requiresWriteSettingsForBrightness != null) {
                    _pendingBrightnessPermission.value = offlineOutcome.requiresWriteSettingsForBrightness
                }
                if (offlineOutcome.sensitiveAction != null) {
                    if (prefs.confirmSensitiveActions) {
                        _pendingSensitiveAction.value = offlineOutcome.sensitiveAction
                    } else {
                        executeSensitiveAction(offlineOutcome.sensitiveAction)
                    }
                }
                val reply = offlineOutcome.replyText
                _lastAssistantReply.value = reply
                _aiState.value = AiUiState.Success(reply)
                localRepo.addMessage(
                    ChatMessageEntity(
                        role = "assistant",
                        content = reply,
                        languageCode = lang,
                        source = offlineOutcome.source
                    )
                )
                if (speakReply) {
                    speakText(reply, lang, prefs, continueAfter = fromVoice && prefs.continuousConversation && !snap.isDeviceLocked)
                }
                return@launch
            }

            val approvedMemories = withContext(Dispatchers.IO) { localRepo.getApprovedMemories() }
            val recentHistory = withContext(Dispatchers.IO) { localRepo.getRecentMessages(10).reversed() }

            geminiBrain.streamAssistantResponse(
                userPrompt = effectiveText,
                detectedLanguage = lang,
                personality = prefs.personality,
                approvedMemories = approvedMemories,
                recentHistory = recentHistory,
                telemetry = snap
            ).collect { event ->
                when (event) {
                    is GeminiStreamEvent.Partial -> {
                        _aiState.value = AiUiState.Streaming(event.accumulatedText)
                        _lastAssistantReply.value = event.accumulatedText
                    }
                    is GeminiStreamEvent.Completed -> {
                        _aiState.value = AiUiState.Success(event.finalText)
                        _lastAssistantReply.value = event.finalText
                        localRepo.addMessage(
                            ChatMessageEntity(
                                role = "assistant",
                                content = event.finalText,
                                languageCode = lang,
                                source = if (event.usedWebContext) "gemini+web" else "gemini"
                            )
                        )
                        if (speakReply) {
                            speakText(
                                event.finalText,
                                lang,
                                prefs,
                                continueAfter = fromVoice && prefs.continuousConversation && !snap.isDeviceLocked
                            )
                        }
                    }
                    is GeminiStreamEvent.Failed -> {
                        _aiState.value = AiUiState.Error(event.message)
                        _statusBanner.value = event.message
                    }
                }
            }
        }
    }

    private fun speakText(
        text: String,
        languageCode: String,
        prefs: NovaPreferences,
        continueAfter: Boolean = false
    ) {
        ttsManager.speak(
            text = text,
            languageCode = languageCode,
            speed = prefs.ttsSpeed,
            pitch = prefs.ttsPitch,
            preferredVoiceName = prefs.selectedVoiceName
        ) {
            if (continueAfter) {
                startVoiceListening(speakResult = true)
            }
        }
    }

    fun speakMessageDirect(text: String) {
        val prefs = preferences.value
        val lang = LanguageDetector.detectLanguage(text, prefs.preferredLanguage)
        ttsManager.speak(
            text = text,
            languageCode = lang,
            speed = prefs.ttsSpeed,
            pitch = prefs.ttsPitch,
            preferredVoiceName = prefs.selectedVoiceName
        )
    }

    fun confirmSensitiveAction() {
        val action = _pendingSensitiveAction.value ?: return
        _pendingSensitiveAction.value = null
        executeSensitiveAction(action)
    }

    fun dismissSensitiveAction() {
        _pendingSensitiveAction.value = null
        _statusBanner.value = "Action cancelled by user."
    }

    private fun executeSensitiveAction(action: SensitiveActionRequest) {
        val result: ActionResult = when (action) {
            is SensitiveActionRequest.MakeCall -> phoneController.placePhoneCall(action.phoneNumber)
            is SensitiveActionRequest.SendSms -> phoneController.sendSms(action.phoneNumber, action.message)
            is SensitiveActionRequest.PrepareWhatsApp -> phoneController.prepareWhatsAppMessage(action.phoneNumber, action.message)
        }
        _statusBanner.value = result.message
    }

    fun toggleFlashlight() {
        val next = !_flashlightOn.value
        val res = phoneController.setFlashlight(next)
        if (res.success) _flashlightOn.value = next
        _statusBanner.value = res.message
    }

    fun setMediaVolume(percent: Int) {
        val res = phoneController.setMediaVolumePercent(percent)
        _mediaVolumePercent.value = phoneController.getMediaVolumePercent()
        if (!res.success) _statusBanner.value = res.message
    }

    fun setBrightness(percent: Int) {
        if (!phoneController.canWriteSystemSettings()) {
            _pendingBrightnessPermission.value = percent.coerceIn(5, 100)
            _statusBanner.value = "Allow Modify System Settings to adjust screen brightness."
            return
        }
        val res = phoneController.setSystemBrightnessPercent(percent)
        _brightnessPercent.value = phoneController.getSystemBrightnessPercent()
        _statusBanner.value = res.message
    }

    fun dismissBrightnessPermissionPrompt() {
        _pendingBrightnessPermission.value = null
    }

    fun onSystemSettingsResumed() {
        val pending = _pendingBrightnessPermission.value
        if (pending != null && phoneController.canWriteSystemSettings()) {
            _pendingBrightnessPermission.value = null
            val res = phoneController.setSystemBrightnessPercent(pending)
            _brightnessPercent.value = phoneController.getSystemBrightnessPercent()
            _statusBanner.value = res.message
        } else {
            _brightnessPercent.value = phoneController.getSystemBrightnessPercent()
        }
    }

    fun launchApp(query: String) {
        val res = phoneController.launchAppByName(query)
        _statusBanner.value = res.message
    }

    fun openSettings(action: String, withPackageUri: Boolean = false) {
        val res = phoneController.openSystemSettings(action, withPackageUri)
        if (!res.success) _statusBanner.value = res.message
    }

    fun triggerFindMyPhone() {
        _statusBanner.value = phoneController.triggerFindMyPhone().message
    }

    fun stopFindMyPhone() {
        phoneController.stopFindMyPhone()
        _statusBanner.value = "Find My Phone alert stopped."
    }

    fun setQuickAlarm(hour: Int, minute: Int, label: String) {
        _statusBanner.value = phoneController.setSystemAlarm(hour, minute, label).message
    }

    fun setQuickTimer(minutes: Int, label: String) {
        _statusBanner.value = phoneController.setSystemTimer(minutes * 60, label).message
    }

    fun createReminder(title: String, delayMinutes: Int) {
        viewModelScope.launch {
            val triggerAt = System.currentTimeMillis() + (delayMinutes.coerceAtLeast(1) * 60_000L)
            val id = localRepo.saveReminder(title, triggerAt)
            _statusBanner.value = phoneController.scheduleLocalReminder(id, title, triggerAt).message
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch { localRepo.deleteReminder(id) }
    }

    fun addMemory(category: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch { localRepo.addMemory(category, content.trim(), approved = true) }
    }

    fun toggleMemoryApproval(memory: MemoryEntity) {
        viewModelScope.launch { localRepo.toggleMemoryApproval(memory) }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch { localRepo.deleteMemory(id) }
    }

    fun saveNote(title: String, body: String) {
        if (title.isBlank() && body.isBlank()) return
        viewModelScope.launch { localRepo.saveNote(title.ifBlank { "Note" }, body.trim()) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { localRepo.deleteNote(id) }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            localRepo.clearHistory()
            _statusBanner.value = "Conversation history cleared."
        }
    }

    fun deleteHistoryMessage(id: Long) {
        viewModelScope.launch { localRepo.deleteMessage(id) }
    }

    fun wipeAllPersonalData() {
        viewModelScope.launch {
            localRepo.wipeAllPersonalData()
            prefsRepo.resetAllPreferences()
            _statusBanner.value = "All personal memories, notes, and chat history erased."
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { prefsRepo.setOnboardingDone(true) }
    }

    fun setPreferredLanguage(lang: PreferredLanguage) {
        viewModelScope.launch { prefsRepo.setPreferredLanguage(lang) }
    }

    fun setTtsSpeed(speed: Float) {
        viewModelScope.launch { prefsRepo.setTtsSpeed(speed) }
    }

    fun setTtsPitch(pitch: Float) {
        viewModelScope.launch { prefsRepo.setTtsPitch(pitch) }
    }

    fun setSelectedVoice(voiceName: String) {
        viewModelScope.launch { prefsRepo.setSelectedVoiceName(voiceName) }
    }

    fun testFemaleVoice(langCode: String) {
        val prefs = preferences.value
        val sample = when (langCode) {
            "bn" -> "হ্যাঁ, বলো। আমি নোভা, তোমার পার্সোনাল এআই অ্যাসিস্ট্যান্ট।"
            "hi" -> "हाँ, बोलिए। मैं नोवा हूँ, आपकी पर्सनल एआई असिस्टेंट।"
            else -> appContext.getString(R.string.nova_wake_response)
        }
        ttsManager.speak(
            text = sample,
            languageCode = langCode,
            speed = prefs.ttsSpeed,
            pitch = prefs.ttsPitch,
            preferredVoiceName = prefs.selectedVoiceName
        )
    }

    fun setPersonality(personality: NovaPersonality) {
        viewModelScope.launch { prefsRepo.setPersonality(personality) }
    }

    fun setNetworkMode(mode: NetworkMode) {
        viewModelScope.launch { prefsRepo.setNetworkMode(mode) }
    }

    fun setThemeMode(mode: NovaThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun toggleThemeMode() {
        val next = if (preferences.value.themeMode == NovaThemeMode.DARK) {
            NovaThemeMode.LIGHT
        } else {
            NovaThemeMode.DARK
        }
        viewModelScope.launch { prefsRepo.setThemeMode(next) }
    }

    fun setContinuousConversation(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setContinuousConversation(enabled) }
    }

    fun setBatterySaverWakePause(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setBatterySaverWakePause(enabled) }
    }

    fun setLockScreenSupport(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setLockScreenSupportEnabled(enabled) }
    }

    fun setConfirmSensitiveActions(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setConfirmSensitiveActions(enabled) }
    }

    fun setEmergencyContact(name: String, phone: String) {
        viewModelScope.launch {
            prefsRepo.setEmergencyContact(name, phone)
            _statusBanner.value = "Emergency contact saved."
        }
    }

    fun requestCallEmergencyContact() {
        val prefs = preferences.value
        if (prefs.emergencyContactPhone.isBlank()) {
            _statusBanner.value = "Set an emergency contact first in Personal Memory."
            return
        }
        _pendingSensitiveAction.value = SensitiveActionRequest.MakeCall(
            displayName = prefs.emergencyContactName.ifBlank { "Emergency Contact" },
            phoneNumber = prefs.emergencyContactPhone
        )
    }

    fun setWakeWordServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val started = NovaVoiceService.start(appContext)
                prefsRepo.setWakeWordEnabled(started)
                if (!started) {
                    _statusBanner.value = NovaVoiceService.statusMessage.value
                }
            } else {
                prefsRepo.setWakeWordEnabled(false)
                NovaVoiceService.stop(appContext)
            }
        }
    }

    fun downloadKwsModel() {
        VoskModelManager.startDownloadIfNeeded(appContext) {
            NovaVoiceService.reloadEngineIfRunning(appContext)
            _statusBanner.value = "Offline Vosk KWS model ready for 'Hey NOVA' detection."
        }
    }

    fun cancelKwsModelDownload() {
        VoskModelManager.cancelDownload()
    }

    fun deleteKwsModel() {
        VoskModelManager.deleteModel(appContext)
        NovaVoiceService.reloadEngineIfRunning(appContext)
        _statusBanner.value = "Offline KWS model removed."
    }

    fun evaluateCalculator(expression: String): String {
        val result = offlineEngine.tryEvaluateMath(expression)
        return if (result != null) {
            if (result % 1.0 == 0.0) result.toLong().toString()
            else "%.4f".format(result).trimEnd('0').trimEnd('.')
        } else {
            "Invalid expression"
        }
    }

    override fun onCleared() {
        connectivityCallback?.let { cb ->
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            runCatching { cm?.unregisterNetworkCallback(cb) }
        }
        ttsManager.shutdown()
        speechManager.destroy()
        super.onCleared()
    }
}
