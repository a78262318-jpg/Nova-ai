package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ai.SensitiveActionRequest
import com.example.data.local.ChatMessageEntity
import com.example.ui.components.NovaCosmicBackground
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.NovaThinkingSpinner
import com.example.ui.components.NovaTopBar
import com.example.ui.components.SensitiveActionConfirmCard
import com.example.ui.components.rememberNovaMicPermissionController
import com.example.ui.theme.NovaBorderGlow
import com.example.ui.theme.NovaCoralError
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaElectricBlue
import com.example.ui.theme.NovaOfflineSlate
import com.example.ui.theme.NovaPurple
import com.example.ui.theme.NovaSurfaceElevated
import com.example.ui.theme.NovaSurfaceGlass
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary
import com.example.ui.theme.NovaOnAccent
import com.example.ui.viewmodel.AiUiState

@Composable
fun AiChatScreen(
    messages: List<ChatMessageEntity>,
    aiState: AiUiState,
    isOnline: Boolean,
    pendingSensitiveAction: SensitiveActionRequest?,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onStartVoice: () -> Unit,
    onClearHistory: () -> Unit,
    onConfirmSensitive: () -> Unit,
    onCancelSensitive: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onCancelRequest: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val micController = rememberNovaMicPermissionController(
        onSpeechPermissionGranted = onStartVoice
    )

    LaunchedEffect(messages.size, aiState) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    NovaCosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
        ) {
            NovaTopBar(
                title = "NOVA AI Brain",
                subtitle = if (isOnline) "Gemini 3.8 Flash • Streaming Online" else "Offline Command Engine Active",
                onBack = onBack,
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = onClearHistory,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("chat_clear_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = "Clear Chat History",
                                tint = NovaTextSecondary
                            )
                        }
                    }
                }
            )

            if (!isOnline) {
                Surface(
                    color = NovaOfflineSlate.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, NovaOfflineSlate.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = NovaOfflineSlate, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Offline: Local phone controls, calculator, notes & battery stats work. Connect to internet for Gemini AI.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NovaTextSecondary
                        )
                    }
                }
            }

            if (messages.isEmpty() && aiState !is AiUiState.Streaming && aiState !is AiUiState.Loading) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_chat),
                        contentDescription = "Start chatting with NOVA",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, NovaCyan.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Ask NOVA Anything",
                        style = MaterialTheme.typography.headlineMedium,
                        color = NovaTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Chat in Bengali, Hindi, or English for reasoning, translations, weather, summaries, or Android controls.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Weather in Dhaka",
                            "Translate 'Good morning' to Bengali & Hindi",
                            "Explain black holes simply",
                            "Nova, YouTube খোলো।"
                        ).forEach { chip ->
                            SuggestionChip(
                                onClick = { onSendMessage(chip) },
                                label = { Text(chip, color = NovaTextPrimary) }
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(msg = msg, onSpeak = { onSpeakMessage(msg.content) })
                    }

                    if (aiState is AiUiState.Loading) {
                        item(key = "loading_indicator") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                NovaThinkingSpinner(size = 28.dp)
                                Text("NOVA is thinking...", style = MaterialTheme.typography.bodyMedium, color = NovaCyan)
                            }
                        }
                    }

                    if (aiState is AiUiState.Streaming) {
                        item(key = "streaming_bubble") {
                            ChatBubble(
                                msg = ChatMessageEntity(
                                    role = "assistant",
                                    content = aiState.text,
                                    source = "gemini"
                                ),
                                onSpeak = {}
                            )
                        }
                    }

                    if (aiState is AiUiState.Error) {
                        item(key = "error_card") {
                            NovaGlassCard(borderColor = NovaCoralError) {
                                Text(
                                    text = aiState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NovaCoralError
                                )
                            }
                        }
                    }
                }
            }

            if (pendingSensitiveAction != null) {
                SensitiveActionConfirmCard(
                    action = pendingSensitiveAction,
                    onConfirm = onConfirmSensitive,
                    onCancel = onCancelSensitive,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Bottom Composer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledIconButton(
                    onClick = { micController.requestForSpeech() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = NovaSurfaceElevated,
                        contentColor = NovaCyan
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_mic_btn")
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.cd_mic_button))
                }

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Message NOVA...", color = NovaTextSecondary) },
                    maxLines = 3,
                    shape = RoundedCornerShape(18.dp),
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
                        .testTag("chat_input_field")
                )

                val isGenerating = aiState is AiUiState.Loading || aiState is AiUiState.Streaming
                FilledIconButton(
                    onClick = {
                        if (isGenerating) {
                            onCancelRequest()
                        } else if (input.isNotBlank()) {
                            onSendMessage(input)
                            input = ""
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isGenerating) NovaCoralError else NovaCyan,
                        contentColor = NovaOnAccent
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(if (isGenerating) "chat_stop_btn" else "chat_send_btn")
                ) {
                    Icon(
                        imageVector = if (isGenerating) Icons.Filled.Stop else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isGenerating) "Stop AI response" else stringResource(R.string.cd_send_command)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    msg: ChatMessageEntity,
    onSpeak: () -> Unit
) {
    val isUser = msg.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) NovaElectricBlue.copy(alpha = 0.28f) else NovaSurfaceGlass.copy(alpha = 0.88f),
            border = BorderStroke(
                1.dp,
                if (isUser) NovaElectricBlue.copy(alpha = 0.7f) else NovaPurple.copy(alpha = 0.55f)
            ),
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = msg.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NovaTextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${msg.languageCode.uppercase()} • ${msg.source}",
                        style = MaterialTheme.typography.labelSmall,
                        color = NovaCyan
                    )
                    if (!isUser) {
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VolumeUp,
                                contentDescription = "Read aloud",
                                tint = NovaTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
