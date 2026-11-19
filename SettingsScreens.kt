package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.preferences.NetworkMode
import com.example.data.preferences.NovaPersonality
import com.example.data.preferences.NovaPreferences
import com.example.data.preferences.NovaThemeMode
import com.example.data.preferences.PreferredLanguage
import com.example.service.NovaNotificationListenerService
import com.example.service.WakeEngineMode
import com.example.ui.components.NovaCosmicBackground
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.NovaPrimaryButton
import com.example.ui.components.NovaTopBar
import com.example.ui.components.rememberNovaMicPermissionController
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaBorderGlow
import com.example.ui.theme.NovaCoralError
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaOnVibrant
import com.example.ui.theme.NovaPurple
import com.example.ui.theme.NovaSurfaceElevated
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary
import com.example.voice.KwsModelState
import com.example.voice.VoiceOption

@Composable
fun LanguageSettingsScreen(
    preferences: NovaPreferences,
    onBack: () -> Unit,
    onSelectLanguage: (PreferredLanguage) -> Unit
) {
    val context = LocalContext.current
    NovaCosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            NovaTopBar(
                title = "Language Settings",
                subtitle = "Bengali • Hindi • English",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Preferred Recognition & Reply Mode", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    PreferredLanguage.entries.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLanguage(lang) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.preferredLanguage == lang,
                                onClick = { onSelectLanguage(lang) },
                                colors = RadioButtonDefaults.colors(selectedColor = NovaCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(lang.displayName, style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                                Text(lang.nativeLabel, style = MaterialTheme.typography.bodyMedium, color = NovaCyan)
                            }
                        }
                    }
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Offline Speech Recognition Packs", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "For offline voice recognition in Bengali, Hindi, and English, download offline language packs in Android Voice Input settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent("com.google.android.googlequicksearchbox.VOICE_SEARCH_SETTING")
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }.onFailure {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Open Android Voice Pack Settings", color = NovaCyan)
                    }
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Multilingual Wake Examples", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• \"Hey NOVA\" / \"NOVA\" -> \"Yes, I'm listening. How can I help you?\"", color = NovaCyan, style = MaterialTheme.typography.bodyMedium)
                    Text("• \"Nova, YouTube খোলো।\"", color = NovaTextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text("• \"Nova, मेरा बैटरी कितना है?\"", color = NovaTextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Text("• \"Hey Nova, open WhatsApp.\"", color = NovaTextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun FemaleVoiceSettingsScreen(
    preferences: NovaPreferences,
    availableVoices: List<VoiceOption>,
    onBack: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onSelectVoice: (String) -> Unit,
    onSelectPersonality: (NovaPersonality) -> Unit,
    onTestVoice: (langCode: String) -> Unit
) {
    NovaCosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            NovaTopBar(
                title = "Voice & Persona",
                subtitle = "Natural multilingual TTS & assistant tone",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Speech Speed: %.2fx".format(preferences.ttsSpeed), color = NovaCyan, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = preferences.ttsSpeed,
                        onValueChange = onSpeedChange,
                        valueRange = 0.6f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = NovaCyan, activeTrackColor = NovaCyan)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Voice Pitch: %.2fx".format(preferences.ttsPitch), color = NovaPurple, style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = preferences.ttsPitch,
                        onValueChange = onPitchChange,
                        valueRange = 0.75f..1.35f,
                        colors = SliderDefaults.colors(thumbColor = NovaPurple, activeTrackColor = NovaPurple)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onTestVoice("bn") }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = NovaCyan, modifier = Modifier.size(16.dp))
                            Text("বাংলা", color = NovaTextPrimary)
                        }
                        OutlinedButton(onClick = { onTestVoice("hi") }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = NovaCyan, modifier = Modifier.size(16.dp))
                            Text("हिन्दी", color = NovaTextPrimary)
                        }
                        OutlinedButton(onClick = { onTestVoice("en") }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = NovaCyan, modifier = Modifier.size(16.dp))
                            Text("English", color = NovaTextPrimary)
                        }
                    }
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Assistant Personality", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    NovaPersonality.entries.forEach { persona ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPersonality(persona) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.personality == persona,
                                onClick = { onSelectPersonality(persona) },
                                colors = RadioButtonDefaults.colors(selectedColor = NovaCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(persona.title, color = NovaTextPrimary, style = MaterialTheme.typography.titleMedium)
                                Text(persona.instruction, color = NovaTextSecondary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Device TTS Voices (${availableVoices.size})", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (availableVoices.isEmpty()) {
                        Text(
                            "System default voices for Bengali, Hindi, and English will be selected automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NovaTextSecondary
                        )
                    } else {
                        availableVoices.take(12).forEach { voice ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectVoice(voice.name) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = preferences.selectedVoiceName == voice.name,
                                    onClick = { onSelectVoice(voice.name) },
                                    colors = RadioButtonDefaults.colors(selectedColor = NovaPurple)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(voice.displayLabel, color = NovaTextPrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineOfflineSettingsScreen(
    preferences: NovaPreferences,
    isOnlineEffective: Boolean,
    wakeServiceRunning: Boolean,
    wakeStatusMessage: String,
    wakeEngineMode: WakeEngineMode,
    kwsModelState: KwsModelState,
    onBack: () -> Unit,
    onSelectThemeMode: (NovaThemeMode) -> Unit,
    onSelectNetworkMode: (NetworkMode) -> Unit,
    onToggleWakeService: (Boolean) -> Unit,
    onToggleBatterySaverPause: (Boolean) -> Unit,
    onToggleLockScreenSupport: (Boolean) -> Unit,
    onDownloadKwsModel: () -> Unit,
    onCancelKwsDownload: () -> Unit,
    onDeleteKwsModel: () -> Unit
) {
    val micController = rememberNovaMicPermissionController(
        onWakePermissionGranted = { onToggleWakeService(true) }
    )

    NovaCosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            NovaTopBar(
                title = "Appearance, Wake & Offline",
                subtitle = if (isOnlineEffective) "Online Brain Ready • ${preferences.themeMode.label}" else "Offline Local Engine • ${preferences.themeMode.label}",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NovaGlassCard(modifier = Modifier.fillMaxWidth(), borderColor = NovaCyan.copy(alpha = 0.6f)) {
                    val isDark = preferences.themeMode == NovaThemeMode.DARK
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = "Interface Theme",
                                tint = if (isDark) NovaCyan else NovaAmber
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("NOVA Visual Theme", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                                Text(
                                    text = if (isDark) "Dark Futuristic (Obsidian & Neon Cyan)" else "Light Futuristic (Aero Daylight & Laser Cyan)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NovaTextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = !isDark,
                            onCheckedChange = { lightEnabled ->
                                onSelectThemeMode(if (lightEnabled) NovaThemeMode.LIGHT else NovaThemeMode.DARK)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = NovaCyan),
                            modifier = Modifier.testTag("theme_mode_switch")
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        NovaThemeMode.entries.forEach { mode ->
                            val selected = preferences.themeMode == mode
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) NovaCyan.copy(alpha = 0.16f) else NovaSurfaceElevated,
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) NovaCyan else NovaBorderGlow
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectThemeMode(mode) }
                                    .testTag(if (mode == NovaThemeMode.DARK) "theme_mode_dark_btn" else "theme_mode_light_btn")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (mode == NovaThemeMode.DARK) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                        contentDescription = mode.label,
                                        tint = if (selected) NovaCyan else NovaTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.shortLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (selected) NovaCyan else NovaTextPrimary,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Connectivity Mode", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    NetworkMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectNetworkMode(mode) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.networkMode == mode,
                                onClick = { onSelectNetworkMode(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = NovaCyan)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode.label, color = NovaTextPrimary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth(), borderColor = NovaCyan.copy(alpha = 0.6f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Mic, contentDescription = null, tint = NovaCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Background \"Hey NOVA\" / \"NOVA\" Service", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                                Text(
                                    text = "Status: $wakeStatusMessage",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NovaTextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = wakeServiceRunning,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    micController.requestForWakeService()
                                } else {
                                    onToggleWakeService(false)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = NovaCyan),
                            modifier = Modifier.testTag("wake_service_switch")
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.BatterySaver, contentDescription = null, tint = NovaAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Pause in Battery Saver", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                                Text("Automatically pauses wake cycles when Android Battery Saver is on.", style = MaterialTheme.typography.bodyMedium, color = NovaTextSecondary)
                            }
                        }
                        Switch(
                            checked = preferences.batterySaverWakePause,
                            onCheckedChange = onToggleBatterySaverPause,
                            colors = SwitchDefaults.colors(checkedThumbColor = NovaAmber)
                        )
                    }
                }

                // On-Device Vosk Lightweight Keyword Spotting Engine Card
                NovaGlassCard(modifier = Modifier.fillMaxWidth(), borderColor = NovaPurple.copy(alpha = 0.6f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = NovaPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("On-Device Keyword Spotter (Vosk KWS)", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                            Text(
                                text = "Active Engine: ${wakeEngineMode.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = NovaCyan
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Runs continuous 16 kHz AudioRecord capture with a restricted Vosk grammar ([\"hey nova\", \"nova\", \"[unk]\"]) completely offline inside the microphone foreground service.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    when (kwsModelState) {
                        is KwsModelState.NotDownloaded -> {
                            OutlinedButton(
                                onClick = onDownloadKwsModel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("download_kws_model_btn")
                            ) {
                                Icon(Icons.Filled.CloudDownload, contentDescription = null, tint = NovaCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Download Offline KWS Model (~40 MB)", color = NovaCyan)
                            }
                        }
                        is KwsModelState.Downloading -> {
                            Text(
                                text = "Downloading Vosk small English model: ${kwsModelState.progressPercent}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = NovaCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { kwsModelState.progressPercent / 100f },
                                color = NovaCyan,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = onCancelKwsDownload,
                                modifier = Modifier.testTag("cancel_kws_download_btn")
                            ) {
                                Text("Cancel Download", color = NovaCoralError)
                            }
                        }
                        is KwsModelState.Extracting -> {
                            Text(
                                text = "Extracting offline acoustic model...",
                                style = MaterialTheme.typography.labelMedium,
                                color = NovaCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                color = NovaPurple,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        is KwsModelState.Ready -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Model Ready (vosk-model-small-en-us)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NovaEmerald
                                )
                                TextButton(
                                    onClick = onDeleteKwsModel,
                                    modifier = Modifier.testTag("delete_kws_model_btn")
                                ) {
                                    Text("Remove", color = NovaCoralError)
                                }
                            }
                        }
                        is KwsModelState.Error -> {
                            Text(
                                text = kwsModelState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NovaCoralError
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = onDownloadKwsModel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Text("Retry KWS Model Download", color = NovaCyan)
                            }
                        }
                    }
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth(), borderColor = NovaAmber.copy(alpha = 0.5f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = NovaAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Android Background Microphone Limitations", style = MaterialTheme.typography.titleMedium, color = NovaAmber)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "• Android 14+ requires starting a microphone foreground service while the app is open and displaying a persistent notification.\n" +
                            "• Some OEM battery managers suspend background SpeechRecognizer services unless battery optimization is exempted.\n" +
                            "• Always-on DSP hotword detection when the screen is off is restricted by Android to system-signed assistants.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaTextSecondary
                    )
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = NovaEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Lock-Screen Safe Commands", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                                Text(
                                    "Allows time, battery, timers, alarms & basic math while locked. Never bypasses PIN/biometrics or reads sensitive data.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NovaTextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = preferences.lockScreenSupportEnabled,
                            onCheckedChange = onToggleLockScreenSupport,
                            colors = SwitchDefaults.colors(checkedThumbColor = NovaEmerald)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyDashboardScreen(
    preferences: NovaPreferences,
    wakeServiceRunning: Boolean,
    onBack: () -> Unit,
    onToggleConfirmSensitive: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    onWipeAllData: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit
) {
    val context = LocalContext.current
    var showWipeDialog by remember { mutableStateOf(false) }
    val notifAccessGranted = NovaNotificationListenerService.isPermissionGranted(context)

    if (showWipeDialog) {
        AlertDialog(
            onDismissRequest = { showWipeDialog = false },
            title = { Text("Delete All Personal Data?") },
            text = { Text("This permanently deletes all conversation history, personal memories, saved notes, reminders, and emergency contact preferences from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onWipeAllData()
                        showWipeDialog = false
                    }
                ) {
                    Text("Delete Everything", color = NovaCoralError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    NovaCosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            NovaTopBar(
                title = "Privacy Dashboard",
                subtitle = "Zero-trust local controls & data management",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = NovaCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Microphone & Background Transparency", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (wakeServiceRunning) {
                            "Wake-Word Service is currently ACTIVE with a visible system notification. NOVA never records audio secretly."
                        } else {
                            "Background microphone is OFF. Audio is only captured when you actively tap Speak."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaTextSecondary
                    )
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Confirm Calls, SMS & WhatsApp", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                            Text("Always require explicit on-screen confirmation before placing calls or sending messages.", style = MaterialTheme.typography.bodyMedium, color = NovaTextSecondary)
                        }
                        Switch(
                            checked = preferences.confirmSensitiveActions,
                            onCheckedChange = onToggleConfirmSensitive,
                            colors = SwitchDefaults.colors(checkedThumbColor = NovaEmerald)
                        )
                    }
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = NovaPurple)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notification Access (Opt-In)", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (notifAccessGranted) "Enabled by user. OTPs and password notifications are automatically ignored." else "Disabled. Grant only if you want NOVA to read incoming notifications.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaTextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenNotificationListenerSettings,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Manage Notification Access", color = NovaCyan)
                    }
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth(), borderColor = NovaCoralError.copy(alpha = 0.6f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = NovaCoralError)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Data Deletion Controls", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onClearHistory,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Clear Conversation History", color = NovaAmber)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showWipeDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NovaCoralError,
                            contentColor = NovaOnVibrant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("wipe_personal_data_btn")
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete All Personal Data", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AboutNovaScreen(
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    NovaCosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            NovaTopBar(
                title = "About NOVA AI",
                subtitle = "Full Android Voice Assistant",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.nova_app_icon),
                        contentDescription = "NOVA AI Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .border(2.dp, NovaCyan, CircleShape)
                    )
                }

                Text("NOVA AI", style = MaterialTheme.typography.displayMedium, color = NovaTextPrimary)
                Text("Personal Multilingual Android AI Voice Assistant", style = MaterialTheme.typography.labelMedium, color = NovaCyan)

                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Core Capabilities", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Wake Word: \"Hey NOVA\" or \"NOVA\" (activates with \"Yes, I'm listening. How can I help you?\")", color = NovaTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("• Languages: Bengali, Hindi, and English with automatic detection & natural TTS", color = NovaTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("• AI Brain: Secure Firebase AI Logic (Gemini 3.8 Flash) with retry & timeout safety", color = NovaTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("• Local Engine: Room persistence, app launching, notifications, alarms, calculator & device controls", color = NovaTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("• Security: Individual permissions, visible mic indicator, confirmation gates, and lock-screen protection", color = NovaTextSecondary, style = MaterialTheme.typography.bodyMedium)
                }

                NovaPrimaryButton(
                    text = "Open Permission Dashboard",
                    onClick = onOpenPermissions,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
