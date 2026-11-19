package com.example.ai

import android.util.Log
import com.example.data.local.ChatMessageEntity
import com.example.data.local.MemoryEntity
import com.example.data.preferences.NovaPersonality
import com.example.data.remote.OnlineInfoRepository
import com.example.device.DeviceTelemetrySnapshot
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiBrainRepository"

sealed interface GeminiStreamEvent {
    data class Partial(val accumulatedText: String) : GeminiStreamEvent
    data class Completed(val finalText: String, val usedWebContext: Boolean) : GeminiStreamEvent
    data class Failed(val message: String) : GeminiStreamEvent
}

class GeminiBrainRepository(
    private val onlineInfoRepo: OnlineInfoRepository
) {
    private val model by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    fun streamAssistantResponse(
        userPrompt: String,
        detectedLanguage: String,
        personality: NovaPersonality,
        approvedMemories: List<MemoryEntity>,
        recentHistory: List<ChatMessageEntity>,
        telemetry: DeviceTelemetrySnapshot
    ): Flow<GeminiStreamEvent> = flow {
        var enrichedContext = ""
        var usedWeb = false

        val lower = userPrompt.lowercase()
        if (lower.contains("weather") || lower.contains("temperature") ||
            lower.contains("আবহাওয়া") || lower.contains("मौसम")
        ) {
            val city = extractCityName(userPrompt)
            onlineInfoRepo.fetchWeather(city).onSuccess { w ->
                usedWeb = true
                enrichedContext += "\n[Live Weather Data for ${w.city}, ${w.country}: ${w.temperatureC}°C, ${w.condition}, Humidity ${w.humidityPercent ?: "--"}%, Wind ${w.windKmh ?: "--"} km/h]\n"
            }
        } else if (lower.startsWith("search ") || lower.contains("who is ") ||
            lower.contains("what is ") || lower.contains("খুঁজে দাও") || lower.contains("सर्च करो")
        ) {
            val topic = userPrompt
                .replace(Regex("(?i)^(search|who is|what is|tell me about)\\s+"), "")
                .trim('?', '.', ' ')
            if (topic.isNotBlank() && topic.length <= 60) {
                onlineInfoRepo.fetchWebSummary(topic).onSuccess { summary ->
                    usedWeb = true
                    enrichedContext += "\n[Live Online Reference Summary: $summary]\n"
                }
            }
        }

        val memoryBlock = if (approvedMemories.isEmpty()) {
            "None saved yet."
        } else {
            approvedMemories.joinToString("\n") { "- [${it.category}] ${it.content}" }
        }

        val targetLangName = when (detectedLanguage) {
            "bn" -> "Bengali (বাংলা)"
            "hi" -> "Hindi (हिन्दी)"
            else -> "English"
        }

        val systemPrompt = buildString {
            appendLine("You are NOVA, a futuristic, natural female personal Android AI voice assistant.")
            appendLine("Personality style: ${personality.title} — ${personality.instruction}")
            appendLine("CRITICAL LANGUAGE RULE: Reply naturally in $targetLangName matching the user's language.")
            appendLine("Live Device Telemetry: Battery ${telemetry.battery.percentage}% (${if (telemetry.battery.isCharging) "Charging" else "Discharging"}), RAM free ${telemetry.memory.availableRamMb}MB, Network ${telemetry.network.transportType}, Device ${telemetry.manufacturer} ${telemetry.deviceModel}.")
            appendLine("User-Approved Personal Memory:\n$memoryBlock")
            if (enrichedContext.isNotBlank()) {
                appendLine("Real-time Online Data:\n$enrichedContext")
            }
            appendLine("Keep responses natural and clear for spoken Text-to-Speech while remaining accurate and helpful.")
        }

        val chatHistory = recentHistory.takeLast(8).map { msg ->
            content(role = if (msg.role == "user") "user" else "model") {
                text(msg.content)
            }
        }

        val maxAttempts = 2
        var lastError: String? = null

        for (attempt in 1..maxAttempts) {
            val builder = StringBuilder()
            try {
                val chat = model.startChat(
                    history = listOf(
                        content("user") { text("System Instructions:\n$systemPrompt") },
                        content("model") { text("Understood. I am NOVA and I will follow these instructions.") }
                    ) + chatHistory
                )

                // 45s per-attempt timeout prevents 60s UI hangs while supporting intelligent single retry
                withTimeout(45_000L) {
                    chat.sendMessageStream(userPrompt).collect { chunk ->
                        val piece = chunk.text.orEmpty()
                        if (piece.isNotEmpty()) {
                            builder.append(piece)
                            emit(GeminiStreamEvent.Partial(builder.toString()))
                        }
                    }
                }

                val finalResponse = builder.toString().trim()
                if (finalResponse.isNotBlank()) {
                    emit(GeminiStreamEvent.Completed(finalResponse, usedWeb))
                    return@flow
                } else {
                    lastError = "Gemini returned an empty response."
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini request timed out on attempt $attempt", e)
                lastError = "Network timed out while reaching Gemini AI. Please check your connection and try again."
                // Only retry if no partial chunks were already streamed
                if (builder.isNotEmpty() || attempt == maxAttempts) {
                    emit(GeminiStreamEvent.Failed(lastError))
                    return@flow
                }
                delay(1200L)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini AI error on attempt $attempt", e)
                lastError = e.localizedMessage ?: "Gemini AI connection failed. Check network or Firebase App Check."
                val isTransient = lastError.contains("timeout", ignoreCase = true) ||
                    lastError.contains("unavailable", ignoreCase = true) ||
                    lastError.contains("network", ignoreCase = true)
                if (!isTransient || builder.isNotEmpty() || attempt == maxAttempts) {
                    emit(GeminiStreamEvent.Failed(lastError))
                    return@flow
                }
                delay(1200L)
            }
        }

        emit(GeminiStreamEvent.Failed(lastError ?: "Unable to reach Gemini AI."))
    }

    private fun extractCityName(prompt: String): String {
        val inMatch = Regex("""(?i)\bin\s+([a-zA-Z\s]{2,25})""").find(prompt)
        if (inMatch != null) return inMatch.groupValues[1].trim()
        return "Dhaka"
    }
}
