package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ai.LanguageDetector
import com.example.ai.SensitiveActionRequest
import com.example.data.preferences.NovaPreferences
import com.example.data.preferences.PreferredLanguage
import com.example.ui.components.NovaCosmicBackground
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.NovaOnlineStatusBadge
import com.example.ui.components.NovaOrbView
import com.example.ui.components.NovaSpeakingWaveform
import com.example.ui.components.NovaTopBar
import com.example.ui.components.SensitiveActionConfirmCard
import com.example.ui.components.rememberNovaMicPermissionController
import com.example.ui.theme.NovaCoralError
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaPurple
import com.example.ui.theme.NovaSurfaceElevated
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary
import com.example.ui.theme.NovaOnAccent
import com.example.ui.viewmodel.OrbVisualState

@Composable
fun VoiceAssistantScreen(
    orbState: OrbVisualState,
    rmsLevel: Float,
    isListening: Boolean,
    isSpeaking: Boolean,
    isOnline: Boolean,
    transportType: String,
    wakeServiceRunning: Boolean,
    wakeStatusMessage: String,
    wakeEngineLabel: String,
    partialTranscript: String,
    lastReply: String,
    lastDetectedLang: String,
    preferences: NovaPreferences,
    pendingSensitiveAction: SensitiveActionRequest?,
    onBack: () -> Unit,
    onSelectLanguage: (PreferredLanguage) -> Unit,
    onToggleContinuous: (Boolean) -> Unit,
    onToggleWakeService: (Boolean) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
    onTriggerWakeNova: () -> Unit,
    onRunSampleCommand: (String) -> Unit,
    onConfirmSensitive: () -> Unit,
    onCancelSensitive: () -> Unit
) {
    val micController = rememberNovaMicPermissionController(
        onSpeechPermissionGranted = onStartListening,
        onWakePermissionGranted = { onToggleWakeService(true) }
    )

    fun triggerMic() {
        micController.requestForSpeech()
    }

    NovaCosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            NovaTopBar(
                title = "Voice Assistant",
                subtitle = "Wake word: \"Hey NOVA\" or \"NOVA\" • ${LanguageDetector.languageDisplayName(lastDetectedLang)}",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NovaOnlineStatusBadge(
                    isOnline = isOnline,
                    transportType = transportType,
                    wakeActive = wakeServiceRunning
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PreferredLanguage.entries.forEach { lang ->
                        val selected = preferences.preferredLanguage == lang
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectLanguage(lang) },
                            label = { Text(lang.nativeLabel) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NovaCyan.copy(alpha = 0.22f),
                                selectedLabelColor = NovaCyan
                            )
                        )
                    }
                }

                NovaOrbView(
                    state = orbState,
                    rmsLevel = rmsLevel,
                    size = 210.dp,
                    onClick = { if (isListening) onStopListening() else triggerMic() }
                )

                NovaSpeakingWaveform(active = isSpeaking || isListening)

                if (isSpeaking) {
                    Button(
                        onClick = onStopSpeaking,
                        colors = ButtonDefaults.buttonColors(containerColor = NovaCoralError),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("voice_interrupt_btn")
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop Speaking")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Interrupt / Stop NOVA", fontWeight = FontWeight.Bold)
                    }
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isListening) "LISTENING..." else "TRANSCRIPT & RESPONSE",
                        style = MaterialTheme.typography.labelMedium,
                        color = NovaCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (partialTranscript.isNotBlank()) {
                        Text(
                            text = "You: $partialTranscript",
                            style = MaterialTheme.typography.bodyLarge,
                            color = NovaTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(
                        text = lastReply.ifBlank { "Say \"Hey NOVA\" or \"NOVA\" — NOVA responds: \"Yes, I'm listening. How can I help you?\"" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaTextSecondary
                    )
                }

                if (pendingSensitiveAction != null) {
                    SensitiveActionConfirmCard(
                        action = pendingSensitiveAction,
                        onConfirm = onConfirmSensitive,
                        onCancel = onCancelSensitive
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Hey NOVA",
                        "Nova, YouTube খোলো।",
                        "Nova, मेरा बैटरी कितना है?",
                        "Hey Nova, open WhatsApp."
                    ).forEach { sample ->
                        SuggestionChip(
                            onClick = { onRunSampleCommand(sample) },
                            label = { Text(sample, color = NovaTextPrimary) }
                        )
                    }
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Background Wake-Word Service", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                            Text(
                                text = if (wakeServiceRunning) {
                                    "$wakeEngineLabel • $wakeStatusMessage"
                                } else {
                                    "Keeps mic active in background for \"Hey NOVA\""
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (wakeServiceRunning) NovaCyan else NovaTextSecondary
                            )
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
                            colors = SwitchDefaults.colors(checkedThumbColor = NovaCyan)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Conversation", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                            Text("Automatically listen again after NOVA replies", style = MaterialTheme.typography.bodyMedium, color = NovaTextSecondary)
                        }
                        Switch(
                            checked = preferences.continuousConversation,
                            onCheckedChange = onToggleContinuous,
                            colors = SwitchDefaults.colors(checkedThumbColor = NovaCyan)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Voice Speed: %.2fx".format(preferences.ttsSpeed),
                        style = MaterialTheme.typography.labelMedium,
                        color = NovaCyan
                    )
                    Slider(
                        value = preferences.ttsSpeed,
                        onValueChange = onSpeedChange,
                        valueRange = 0.6f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = NovaCyan, activeTrackColor = NovaCyan)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onTriggerWakeNova,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NovaSurfaceElevated,
                        contentColor = NovaTextPrimary
                    ),
                    border = BorderStroke(1.dp, NovaPurple),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("voice_say_nova_btn")
                ) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = NovaPurple)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("\"Hey NOVA\"", textAlign = TextAlign.Center)
                }

                Surface(
                    shape = CircleShape,
                    color = if (isListening) NovaCoralError else NovaCyan,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("voice_main_mic_btn"),
                    onClick = { if (isListening) onStopListening() else triggerMic() }
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = if (isListening) "Stop Listening" else "Start Listening",
                        tint = NovaOnAccent,
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}
