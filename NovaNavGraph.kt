package com.example.ui.navigation

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.WriteSettingsPermissionDialog
import com.example.ui.components.openWriteSystemSettings
import com.example.ui.components.rememberLifecycleResumeTick
import com.example.ui.screens.AboutNovaScreen
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.BatteryDashboardScreen
import com.example.ui.screens.ConversationHistoryScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeviceManagerScreen
import com.example.ui.screens.FemaleVoiceSettingsScreen
import com.example.ui.screens.LanguageSettingsScreen
import com.example.ui.screens.OnlineOfflineSettingsScreen
import com.example.ui.screens.PermissionSetupScreen
import com.example.ui.screens.PersonalMemoryScreen
import com.example.ui.screens.PrivacyDashboardScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.viewmodel.NovaViewModel

@Composable
fun NovaNavGraph(
    viewModel: NovaViewModel,
    openVoiceDirectly: Boolean = false,
    pendingVoiceCommand: String? = null
) {
    val navController = rememberNavController()

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val orbState by viewModel.orbState.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.rmsLevel.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnlineEffective.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val wakeRunning by viewModel.wakeServiceRunning.collectAsStateWithLifecycle()
    val wakeStatus by viewModel.wakeServiceStatus.collectAsStateWithLifecycle()
    val wakeEngineMode by viewModel.wakeEngineMode.collectAsStateWithLifecycle()
    val kwsModelState by viewModel.kwsModelState.collectAsStateWithLifecycle()
    val lastReply by viewModel.lastAssistantReply.collectAsStateWithLifecycle()
    val lastLang by viewModel.lastDetectedLang.collectAsStateWithLifecycle()
    val flashlightOn by viewModel.flashlightOn.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val pendingSensitive by viewModel.pendingSensitiveAction.collectAsStateWithLifecycle()
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val mediaVolume by viewModel.mediaVolumePercent.collectAsStateWithLifecycle()
    val brightness by viewModel.brightnessPercent.collectAsStateWithLifecycle()
    val pendingBrightnessPerm by viewModel.pendingBrightnessPermission.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val contacts by viewModel.contacts.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val voices by viewModel.availableVoices.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val resumeTick = rememberLifecycleResumeTick()
    LaunchedEffect(resumeTick) {
        if (resumeTick > 0) {
            viewModel.onSystemSettingsResumed()
        }
    }

    if (pendingBrightnessPerm != null) {
        WriteSettingsPermissionDialog(
            targetBrightnessPercent = pendingBrightnessPerm,
            onDismiss = { viewModel.dismissBrightnessPermissionPrompt() },
            onOpenWriteSettings = {
                context.openWriteSystemSettings()
            }
        )
    }

    LaunchedEffect(openVoiceDirectly, pendingVoiceCommand) {
        if (openVoiceDirectly) {
            navController.navigate(NovaRoute.VoiceAssistant) {
                launchSingleTop = true
            }
        }
        if (!pendingVoiceCommand.isNullOrBlank()) {
            viewModel.submitCommand(pendingVoiceCommand, fromVoice = true, speakReply = true)
        }
    }

    NavHost(
        navController = navController,
        startDestination = NovaRoute.Splash
    ) {
        composable<NovaRoute.Splash> {
            SplashScreen(
                hasCompletedOnboarding = prefs.hasCompletedOnboarding,
                onFinished = { goToDashboard ->
                    val target = if (openVoiceDirectly) {
                        NovaRoute.VoiceAssistant
                    } else if (goToDashboard) {
                        NovaRoute.Dashboard
                    } else {
                        NovaRoute.Welcome
                    }
                    navController.navigate(target) {
                        popUpTo<NovaRoute.Splash> { inclusive = true }
                    }
                }
            )
        }

        composable<NovaRoute.Welcome> {
            WelcomeScreen(
                onGetStarted = { navController.navigate(NovaRoute.PermissionSetup) },
                onSkipToDashboard = {
                    viewModel.completeOnboarding()
                    navController.navigate(NovaRoute.Dashboard) {
                        popUpTo<NovaRoute.Welcome> { inclusive = true }
                    }
                }
            )
        }

        composable<NovaRoute.PermissionSetup> {
            PermissionSetupScreen(
                onOpenSettings = { action, withPkg -> viewModel.openSettings(action, withPkg) },
                onContinue = {
                    viewModel.completeOnboarding()
                    navController.navigate(NovaRoute.Dashboard) {
                        popUpTo<NovaRoute.Welcome> { inclusive = true }
                    }
                },
                onBack = if (navController.previousBackStackEntry != null) {
                    { navController.popBackStack() }
                } else null
            )
        }

        composable<NovaRoute.Dashboard> {
            DashboardScreen(
                orbState = orbState,
                rmsLevel = rmsLevel,
                isOnline = isOnline,
                telemetry = telemetry,
                preferences = prefs,
                wakeServiceRunning = wakeRunning,
                wakeStatusMessage = wakeStatus,
                lastReply = lastReply,
                flashlightOn = flashlightOn,
                statusBanner = statusBanner,
                pendingSensitiveAction = pendingSensitive,
                onNavigate = { route -> navController.navigate(route) },
                onTestWakeWord = { viewModel.testWakeWordNova() },
                onStartVoice = {
                    navController.navigate(NovaRoute.VoiceAssistant)
                    viewModel.startVoiceListening(speakResult = true)
                },
                onSubmitTextCommand = { cmd -> viewModel.submitCommand(cmd, fromVoice = false, speakReply = false) },
                onToggleFlashlight = { viewModel.toggleFlashlight() },
                onToggleWakeService = { enable -> viewModel.setWakeWordServiceEnabled(enable) },
                onToggleThemeMode = { viewModel.toggleThemeMode() },
                onConfirmSensitive = { viewModel.confirmSensitiveAction() },
                onCancelSensitive = { viewModel.dismissSensitiveAction() },
                onDismissBanner = { viewModel.clearBanner() }
            )
        }

        composable<NovaRoute.VoiceAssistant> {
            VoiceAssistantScreen(
                orbState = orbState,
                rmsLevel = rmsLevel,
                isListening = isListening,
                isSpeaking = isSpeaking,
                isOnline = isOnline,
                transportType = telemetry.network.transportType,
                wakeServiceRunning = wakeRunning,
                wakeStatusMessage = wakeStatus,
                wakeEngineLabel = wakeEngineMode.label,
                partialTranscript = partialTranscript,
                lastReply = lastReply,
                lastDetectedLang = lastLang,
                preferences = prefs,
                pendingSensitiveAction = pendingSensitive,
                onBack = { navController.popBackStack() },
                onSelectLanguage = { viewModel.setPreferredLanguage(it) },
                onToggleContinuous = { viewModel.setContinuousConversation(it) },
                onToggleWakeService = { viewModel.setWakeWordServiceEnabled(it) },
                onSpeedChange = { viewModel.setTtsSpeed(it) },
                onStartListening = { viewModel.startVoiceListening(speakResult = true) },
                onStopListening = { viewModel.stopVoiceListening() },
                onStopSpeaking = { viewModel.stopSpeaking() },
                onTriggerWakeNova = { viewModel.testWakeWordNova() },
                onRunSampleCommand = { viewModel.submitCommand(it, fromVoice = true, speakReply = true) },
                onConfirmSensitive = { viewModel.confirmSensitiveAction() },
                onCancelSensitive = { viewModel.dismissSensitiveAction() }
            )
        }

        composable<NovaRoute.AiChat> {
            AiChatScreen(
                messages = messages,
                aiState = aiState,
                isOnline = isOnline,
                pendingSensitiveAction = pendingSensitive,
                onBack = { navController.popBackStack() },
                onSendMessage = { viewModel.submitCommand(it, fromVoice = false, speakReply = false) },
                onStartVoice = { navController.navigate(NovaRoute.VoiceAssistant) },
                onClearHistory = { viewModel.clearChatHistory() },
                onConfirmSensitive = { viewModel.confirmSensitiveAction() },
                onCancelSensitive = { viewModel.dismissSensitiveAction() },
                onSpeakMessage = { text -> viewModel.speakMessageDirect(text) },
                onCancelRequest = { viewModel.cancelAiRequest() }
            )
        }

        composable<NovaRoute.DeviceManager> {
            DeviceManagerScreen(
                telemetry = telemetry,
                mediaVolumePercent = mediaVolume,
                brightnessPercent = brightness,
                flashlightOn = flashlightOn,
                installedApps = installedApps,
                contacts = contacts,
                pendingSensitiveAction = pendingSensitive,
                onBack = { navController.popBackStack() },
                onRefresh = { viewModel.refreshTelemetry() },
                onVolumeChange = { viewModel.setMediaVolume(it) },
                onBrightnessChange = { viewModel.setBrightness(it) },
                onToggleFlashlight = { viewModel.toggleFlashlight() },
                onOpenSettings = { action, withPkg -> viewModel.openSettings(action, withPkg) },
                onLaunchApp = { viewModel.launchApp(it) },
                onLoadContacts = { viewModel.loadContacts() },
                onTriggerFindPhone = { viewModel.triggerFindMyPhone() },
                onStopFindPhone = { viewModel.stopFindMyPhone() },
                onExecuteCommand = { viewModel.submitCommand(it, fromVoice = false, speakReply = false) },
                onConfirmSensitive = { viewModel.confirmSensitiveAction() },
                onCancelSensitive = { viewModel.dismissSensitiveAction() }
            )
        }

        composable<NovaRoute.BatteryDashboard> {
            BatteryDashboardScreen(
                telemetry = telemetry,
                onBack = { navController.popBackStack() },
                onRefresh = { viewModel.refreshTelemetry() }
            )
        }

        composable<NovaRoute.PersonalMemory> {
            PersonalMemoryScreen(
                memories = memories,
                notes = notes,
                reminders = reminders,
                preferences = prefs,
                pendingSensitiveAction = pendingSensitive,
                onBack = { navController.popBackStack() },
                onAddMemory = { cat, content -> viewModel.addMemory(cat, content) },
                onToggleMemoryApproval = { viewModel.toggleMemoryApproval(it) },
                onDeleteMemory = { viewModel.deleteMemory(it) },
                onSaveNote = { title, body -> viewModel.saveNote(title, body) },
                onDeleteNote = { viewModel.deleteNote(it) },
                onCreateReminder = { title, mins -> viewModel.createReminder(title, mins) },
                onDeleteReminder = { viewModel.deleteReminder(it) },
                onSetQuickAlarm = { h, m, label -> viewModel.setQuickAlarm(h, m, label) },
                onSetQuickTimer = { mins, label -> viewModel.setQuickTimer(mins, label) },
                onEvaluateMath = { expr -> viewModel.evaluateCalculator(expr) },
                onSaveEmergencyContact = { name, phone -> viewModel.setEmergencyContact(name, phone) },
                onCallEmergencyContact = { viewModel.requestCallEmergencyContact() },
                onConfirmSensitive = { viewModel.confirmSensitiveAction() },
                onCancelSensitive = { viewModel.dismissSensitiveAction() }
            )
        }

        composable<NovaRoute.ConversationHistory> {
            ConversationHistoryScreen(
                messages = messages,
                onBack = { navController.popBackStack() },
                onDeleteMessage = { viewModel.deleteHistoryMessage(it) },
                onClearAll = { viewModel.clearChatHistory() }
            )
        }

        composable<NovaRoute.LanguageSettings> {
            LanguageSettingsScreen(
                preferences = prefs,
                onBack = { navController.popBackStack() },
                onSelectLanguage = { viewModel.setPreferredLanguage(it) }
            )
        }

        composable<NovaRoute.FemaleVoiceSettings> {
            FemaleVoiceSettingsScreen(
                preferences = prefs,
                availableVoices = voices,
                onBack = { navController.popBackStack() },
                onSpeedChange = { viewModel.setTtsSpeed(it) },
                onPitchChange = { viewModel.setTtsPitch(it) },
                onSelectVoice = { viewModel.setSelectedVoice(it) },
                onSelectPersonality = { viewModel.setPersonality(it) },
                onTestVoice = { viewModel.testFemaleVoice(it) }
            )
        }

        composable<NovaRoute.OnlineOfflineSettings> {
            OnlineOfflineSettingsScreen(
                preferences = prefs,
                isOnlineEffective = isOnline,
                wakeServiceRunning = wakeRunning,
                wakeStatusMessage = wakeStatus,
                wakeEngineMode = wakeEngineMode,
                kwsModelState = kwsModelState,
                onBack = { navController.popBackStack() },
                onSelectThemeMode = { viewModel.setThemeMode(it) },
                onSelectNetworkMode = { viewModel.setNetworkMode(it) },
                onToggleWakeService = { viewModel.setWakeWordServiceEnabled(it) },
                onToggleBatterySaverPause = { viewModel.setBatterySaverWakePause(it) },
                onToggleLockScreenSupport = { viewModel.setLockScreenSupport(it) },
                onDownloadKwsModel = { viewModel.downloadKwsModel() },
                onCancelKwsDownload = { viewModel.cancelKwsModelDownload() },
                onDeleteKwsModel = { viewModel.deleteKwsModel() }
            )
        }

        composable<NovaRoute.PrivacyDashboard> {
            PrivacyDashboardScreen(
                preferences = prefs,
                wakeServiceRunning = wakeRunning,
                onBack = { navController.popBackStack() },
                onToggleConfirmSensitive = { viewModel.setConfirmSensitiveActions(it) },
                onClearHistory = { viewModel.clearChatHistory() },
                onWipeAllData = { viewModel.wipeAllPersonalData() },
                onOpenNotificationListenerSettings = {
                    viewModel.openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, false)
                }
            )
        }

        composable<NovaRoute.AboutNova> {
            AboutNovaScreen(
                onBack = { navController.popBackStack() },
                onOpenPermissions = { navController.navigate(NovaRoute.PermissionSetup) }
            )
        }
    }
}
