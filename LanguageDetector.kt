package com.example.ai

import com.example.data.preferences.PreferredLanguage

object LanguageDetector {

    fun detectLanguage(text: String, preferred: PreferredLanguage = PreferredLanguage.AUTO): String {
        if (preferred != PreferredLanguage.AUTO) {
            val hasBengali = text.any { it in '\u0980'..'\u09FF' }
            val hasHindi = text.any { it in '\u0900'..'\u097F' }
            return when {
                hasBengali -> "bn"
                hasHindi -> "hi"
                else -> preferred.code
            }
        }

        val hasBengali = text.any { it in '\u0980'..'\u09FF' }
        if (hasBengali) return "bn"

        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        if (hasDevanagari) return "hi"

        val lower = text.lowercase()
        val bengaliHints = listOf("kholo", "kemon", "bolo", "amar", "koto", "tumi", "bangla", "shono")
        if (bengaliHints.any { lower.contains(it) }) return "bn"

        val hindiHints = listOf("mera", "kitna", "kya", "kaise", "chalu", "band", "karo", "batao", "hindi")
        if (hindiHints.any { lower.contains(it) }) return "hi"

        return "en"
    }

    fun toBcp47Tag(langCode: String): String = when (langCode) {
        "bn" -> "bn-BD"
        "hi" -> "hi-IN"
        else -> "en-US"
    }

    fun languageDisplayName(langCode: String): String = when (langCode) {
        "bn" -> "বাংলা"
        "hi" -> "हिन्दी"
        else -> "English"
    }
}

data class WakeWordResult(
    val wakeDetected: Boolean,
    val isBareWakeWord: Boolean,
    val strippedCommand: String
)

object WakeWordParser {
    // Matches "Hey NOVA", "OK NOVA", "Hi NOVA", or standalone "NOVA" (plus bn/hi phonetic transcriptions)
    private val wakePrefixRegex = Regex(
        """^\s*(?:(?:hey|hi|ok|okay|হেই|हे|ओके)\s+)?(?:nova|নোভা|नोवा|नौवा)[\s,.।:;\-!?]*""",
        RegexOption.IGNORE_CASE
    )
    private val anywhereRegex = Regex(
        """(?:(?:hey|hi|ok|okay)\s+)?\bnova\b|(?:হেই\s+)?নোভা|(?:हे\s+)?(?:नोवा|नौवा)""",
        RegexOption.IGNORE_CASE
    )

    fun parse(transcript: String): WakeWordResult {
        val trimmed = transcript.trim()
        if (trimmed.isEmpty()) return WakeWordResult(false, false, "")

        val startMatch = wakePrefixRegex.find(trimmed)
        if (startMatch != null) {
            val remainder = trimmed.removeRange(startMatch.range).trim()
            return WakeWordResult(
                wakeDetected = true,
                isBareWakeWord = remainder.isEmpty(),
                strippedCommand = remainder
            )
        }

        val anywhereMatch = anywhereRegex.find(trimmed)
        if (anywhereMatch != null) {
            val after = trimmed.substring(anywhereMatch.range.last + 1)
                .trimStart(',', '.', ' ', '।', ':', '-', '!', '?')
                .trim()
            return WakeWordResult(
                wakeDetected = true,
                isBareWakeWord = after.isEmpty(),
                strippedCommand = after
            )
        }

        return WakeWordResult(
            wakeDetected = false,
            isBareWakeWord = false,
            strippedCommand = trimmed
        )
    }
}
