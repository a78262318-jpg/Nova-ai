package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ai.SensitiveActionRequest
import com.example.data.preferences.NovaPreferences
import com.example.data.preferences.NovaThemeMode
import com.example.device.DeviceTelemetrySnapshot
import com.example.ui.components.NovaCosmicBackground
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.NovaOnlineStatusBadge
import com.example.ui.components.NovaOrbView
import com.example.ui.components.SensitiveActionConfirmCard
import com.example.ui.components.rememberNovaMicPermissionController
import com.example.ui.navigation.NovaRoute
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaBorderGlow
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaElectricBlue
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaOnAccent
import com.example.ui.theme.NovaOnVibrant
import com.example.ui.theme.NovaPurple
import com.example.ui.theme.NovaSurfaceElevated
import com.example.ui.theme.NovaSurfaceGlass
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary
import com.example.ui.viewmodel.OrbVisualState

@Composable
fun DashboardScreen(
    orbState: OrbVisualState,
    rmsLevel: Float,
    isOnline: Boolean,
    telemetry: DeviceTelemetrySnapshot,
    preferences: NovaPreferences,
    wakeServiceRunning: Boolean,
    wakeStatusMessage: String,
    lastReply: String,
    flashlightOn: Boolean,
    statusBanner: String?,
    pendingSensitiveAction: SensitiveActionRequest?,
    onNavigate: (NovaRoute) -> Unit,
    onTestWakeWord: () -> Unit,
    onStartVoice: () -> Unit,
    onSubmitTextCommand: (String) -> Unit,
    onToggleFlashlight: () -> Unit,
    onToggleWakeService: (Boolean) -> Unit,
    onToggleThemeMode: () -> Unit,
    onConfirmSensitive: () -> Unit,
    onCancelSensitive: () -> Unit,
    onDismissBanner: () -> Unit
) {
    var commandInput by remember { mutableStateOf("") }

    val micController = rememberNovaMicPermissionController(
        onSpeechPermissionGranted = onStartVoice,
        onWakePermissionGranted = { onToggleWakeService(true) }
    )

    fun requestOrStartVoice() {
        micController.requestForSpeech()
    }

    NovaCosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header: NOVA Logo + Title + History & Settings Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nova_app_icon),
                    contentDescription = stringResource(R.string.cd_nova_logo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, NovaCyan.copy(alpha = 0.7f), CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NOVA",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NovaTextPrimary
                    )
                    Text(
                        text = "Say \"Hey NOVA\" or \"NOVA\" • ${preferences.preferredLanguage.nativeLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = NovaCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val isDarkTheme = preferences.themeMode == NovaThemeMode.DARK
                IconButton(
                    onClick = onToggleThemeMode,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dashboard_theme_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = if (isDarkTheme) {
                            stringResource(R.string.cd_switch_to_light_theme)
                        } else {
                            stringResource(R.string.cd_switch_to_dark_theme)
                        },
                        tint = if (isDarkTheme) NovaAmber else NovaPurple
                    )
                }
                IconButton(
                    onClick = { onNavigate(NovaRoute.ConversationHistory) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dashboard_history_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = "Conversation History",
                        tint = NovaTextPrimary
                    )
                }
                IconButton(
                    onClick = { onNavigate(NovaRoute.PermissionSetup) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dashboard_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Permissions and Settings",
                        tint = NovaTextPrimary
                    )
                }
            }

            // Live Online/Offline + Visible Microphone Indicator
            NovaOnlineStatusBadge(
                isOnline = isOnline,
                transportType = telemetry.network.transportType,
                wakeActive = wakeServiceRunning
            )

            if (!statusBanner.isNullOrBlank()) {
                NovaGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NovaCyan.copy(alpha = 0.6f),
                    onClick = onDismissBanner
                ) {
                    Text(
                        text = statusBanner,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaCyan
                    )
                }
            }

            if (pendingSensitiveAction != null) {
                SensitiveActionConfirmCard(
                    action = pendingSensitiveAction,
                    onConfirm = onConfirmSensitive,
                    onCancel = onCancelSensitive
                )
            }

            // Hero 3D AI Orb + Mic + Wake Trigger
            NovaGlassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NovaOrbView(
                        state = orbState,
                        rmsLevel = rmsLevel,
                        size = 186.dp,
                        modifier = Modifier.testTag("dashboard_orb"),
                        onClick = { requestOrStartVoice() }
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = when (orbState) {
                            OrbVisualState.LISTENING -> "Listening in Bengali / Hindi / English..."
                            OrbVisualState.PROCESSING -> "NOVA Neural Core Processing..."
                            OrbVisualState.SPEAKING -> "NOVA Speaking..."
                            OrbVisualState.ERROR -> "Attention Required"
                            OrbVisualState.OFFLINE -> "Offline Core Ready"
                            OrbVisualState.IDLE -> "Ready • Tap Orb or Say \"Hey NOVA\""
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = NovaCyan
                    )

                    if (lastReply.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = lastReply,
                            style = MaterialTheme.typography.bodyMedium,
                            color = NovaTextPrimary,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { requestOrStartVoice() }
                                .testTag("dashboard_mic_btn"),
                            color = NovaCyan,
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.cd_mic_button), tint = NovaOnAccent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Speak", color = NovaOnAccent, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onTestWakeWord() }
                                .testTag("dashboard_wake_test_btn"),
                            color = NovaPurple.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, NovaPurple)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.GraphicEq, contentDescription = "Activate NOVA", tint = NovaTextPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("\"Hey NOVA\"", color = NovaTextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Text Command Input Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commandInput,
                    onValueChange = { commandInput = it },
                    placeholder = {
                        Text(
                            "Type a command or ask NOVA...",
                            color = NovaTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaCyan,
                        unfocusedBorderColor = NovaBorderGlow,
                        focusedContainerColor = NovaSurfaceGlass,
                        unfocusedContainerColor = NovaSurfaceGlass,
                        focusedTextColor = NovaTextPrimary,
                        unfocusedTextColor = NovaTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dashboard_command_input")
                )
                FilledIconButton(
                    onClick = {
                        if (commandInput.isNotBlank()) {
                            onSubmitTextCommand(commandInput)
                            commandInput = ""
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = NovaElectricBlue,
                        contentColor = NovaOnVibrant
                    ),
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("dashboard_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.cd_send_command)
                    )
                }
            }

            // Battery & Background Wake-Word Mic Service Control Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NovaGlassCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(NovaRoute.BatteryDashboard) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (telemetry.battery.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
                            contentDescription = "Battery",
                            tint = if (telemetry.battery.percentage > 25) NovaEmerald else NovaAmber
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Battery", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${telemetry.battery.percentage}% • ${telemetry.battery.chargeSource}",
                        style = MaterialTheme.typography.labelMedium,
                        color = NovaCyan
                    )
                    Text(
                        text = "RAM free: ${telemetry.memory.availableRamMb} MB",
                        style = MaterialTheme.typography.labelSmall,
                        color = NovaTextSecondary
                    )
                }

                NovaGlassCard(
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(NovaRoute.OnlineOfflineSettings) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Mic, contentDescription = "Wake Listener", tint = if (wakeServiceRunning) NovaCyan else NovaTextSecondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Wake Mic", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
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
                            modifier = Modifier.testTag("dashboard_wake_switch")
                        )
                    }
                    Text(
                        text = wakeStatusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (wakeServiceRunning) NovaCyan else NovaTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Quick Command Shortcuts
            Text(
                text = "Quick Voice & Device Shortcuts",
                style = MaterialTheme.typography.titleMedium,
                color = NovaTextPrimary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickShortcutChip(
                    icon = Icons.Filled.FlashlightOn,
                    label = if (flashlightOn) "Torch OFF" else "Torch ON",
                    onClick = onToggleFlashlight
                )
                QuickShortcutChip(
                    icon = Icons.Filled.PhoneAndroid,
                    label = "YouTube খোলো",
                    onClick = { onSubmitTextCommand("Nova, YouTube খোলো।") }
                )
                QuickShortcutChip(
                    icon = Icons.Filled.BatteryStd,
                    label = "मेरा बैटरी?",
                    onClick = { onSubmitTextCommand("Nova, मेरा बैटरी कितना है?") }
                )
                QuickShortcutChip(
                    icon = Icons.Filled.Timer,
                    label = "5m Timer",
                    onClick = { onSubmitTextCommand("Set a timer for 5 minutes") }
                )
                QuickShortcutChip(
                    icon = Icons.Filled.Calculate,
                    label = "256 * 18",
                    onClick = { onSubmitTextCommand("Calculate 256 * 18") }
                )
            }

            // All NOVA Modules & Screens Hub
            Text(
                text = "NOVA Systems & Screens",
                style = MaterialTheme.typography.titleMedium,
                color = NovaTextPrimary
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleTile(
                        icon = Icons.Filled.GraphicEq,
                        title = "Voice Assistant",
                        subtitle = "Wake word & continuous talk",
                        accent = NovaCyan,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_voice_assistant",
                        onClick = { onNavigate(NovaRoute.VoiceAssistant) }
                    )
                    ModuleTile(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = "Gemini AI Chat",
                        subtitle = "Streaming Q&A & search",
                        accent = NovaPurple,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_ai_chat",
                        onClick = { onNavigate(NovaRoute.AiChat) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleTile(
                        icon = Icons.Filled.Memory,
                        title = "Device Manager",
                        subtitle = "Controls, apps & calls",
                        accent = NovaElectricBlue,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_device_manager",
                        onClick = { onNavigate(NovaRoute.DeviceManager) }
                    )
                    ModuleTile(
                        icon = Icons.Filled.Psychology,
                        title = "Personal Memory",
                        subtitle = "Memory, notes & calc",
                        accent = NovaEmerald,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_personal_memory",
                        onClick = { onNavigate(NovaRoute.PersonalMemory) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleTile(
                        icon = Icons.Filled.BatteryChargingFull,
                        title = "Battery Hub",
                        subtitle = "Temp, voltage & health",
                        accent = NovaAmber,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_battery",
                        onClick = { onNavigate(NovaRoute.BatteryDashboard) }
                    )
                    ModuleTile(
                        icon = Icons.Filled.Language,
                        title = "Languages",
                        subtitle = "Bengali, Hindi, English",
                        accent = NovaCyan,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_language",
                        onClick = { onNavigate(NovaRoute.LanguageSettings) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleTile(
                        icon = Icons.Filled.RecordVoiceOver,
                        title = "Female Voice",
                        subtitle = "Speed, pitch & persona",
                        accent = NovaPurple,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_voice_settings",
                        onClick = { onNavigate(NovaRoute.FemaleVoiceSettings) }
                    )
                    ModuleTile(
                        icon = Icons.Filled.Wifi,
                        title = "Wake & Offline",
                        subtitle = "Background mic & lock mode",
                        accent = NovaEmerald,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_online_offline",
                        onClick = { onNavigate(NovaRoute.OnlineOfflineSettings) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ModuleTile(
                        icon = Icons.Filled.Security,
                        title = "Privacy & Data",
                        subtitle = "Controls & data wipe",
                        accent = NovaAmber,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_privacy",
                        onClick = { onNavigate(NovaRoute.PrivacyDashboard) }
                    )
                    ModuleTile(
                        icon = Icons.Filled.Info,
                        title = "About NOVA",
                        subtitle = "Architecture & safety",
                        accent = NovaElectricBlue,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_about",
                        onClick = { onNavigate(NovaRoute.AboutNova) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickShortcutChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    SuggestionChip(
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null, tint = NovaCyan, modifier = Modifier.size(16.dp)) },
        label = { Text(label, color = NovaTextPrimary) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = NovaSurfaceElevated),
        border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = NovaBorderGlow)
    )
}

@Composable
private fun ModuleTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    NovaGlassCard(
        modifier = modifier.testTag(testTag),
        borderColor = accent.copy(alpha = 0.45f),
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accent,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = NovaTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
