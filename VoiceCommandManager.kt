package com.example.voice

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import com.example.ai.WakeWordParser
import com.example.device.PhoneController

sealed interface VoiceIntentAction {
    data class OpenApp(
        val appName: String,
        val resolvedPackageName: String? = null
    ) : VoiceIntentAction

    data class OpenSettings(
        val settingsAction: String,
        val label: String
    ) : VoiceIntentAction

    data object OpenCamera : VoiceIntentAction

    data class SetAlarm(
        val hour24: Int,
        val minute: Int,
        val label: String = "NOVA Alarm"
    ) : VoiceIntentAction

    data class SetTimer(
        val durationSeconds: Int,
        val label: String = "NOVA Timer"
    ) : VoiceIntentAction

    data class OpenDialer(
        val phoneNumber: String? = null
    ) : VoiceIntentAction

    data class WebSearch(
        val query: String
    ) : VoiceIntentAction
}

data class VoiceCommandResult(
    val handled: Boolean,
    val success: Boolean = false,
    val action: VoiceIntentAction? = null,
    val replyText: String = "",
    val launchedIntent: Intent? = null,
    val lockScreenRestricted: Boolean = false
)

/**
 * Handles intent-based Android voice commands such as:
 * - "Open WhatsApp", "Open YouTube", "Open Chrome", or any installed launcher app
 * - "Open Settings", "Open Wi-Fi Settings", "Open Bluetooth Settings", "Open Display Settings"
 * - "Open Camera"
 * - "Set an alarm for 7:30 AM", "Set an alarm for 6 PM", "Set a 5 minute timer"
 * - "Search Google for ..."
 *
 * Builds standard Android [Intent]s and dispatches them safely with lock-screen checks
 * and localized English / Bengali / Hindi confirmations.
 */
class VoiceCommandManager(
    private val context: Context,
    private val phoneController: PhoneController = PhoneController(context)
) {

    private val knownAppPackages: Map<String, String> = mapOf(
        "whatsapp" to "com.whatsapp",
        "হোয়াটসঅ্যাপ" to "com.whatsapp",
        "व्हाट्सएप" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "ইউটিউব" to "com.google.android.youtube",
        "यूट्यूब" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "ক্রোম" to "com.android.chrome",
        "क्रोम" to "com.android.chrome",
        "gmail" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",
        "calculator" to "com.google.android.calculator",
        "ক্যালকুলেটর" to "com.google.android.calculator",
        "कैलकुलेटर" to "com.google.android.calculator",
        "clock" to "com.google.android.deskclock",
        "play store" to "com.android.vending",
        "spotify" to "com.spotify.music",
        "instagram" to "com.instagram.android",
        "telegram" to "org.telegram.messenger"
    )

    /**
     * Parses a raw voice or text utterance (optionally prefixed with "NOVA" / "Hey NOVA")
     * into a typed [VoiceIntentAction], or returns `null` if it is not an intent command.
     */
    fun parseCommand(rawInput: String): VoiceIntentAction? {
        val wake = WakeWordParser.parse(rawInput)
        val cleaned = (if (wake.wakeDetected && !wake.isBareWakeWord) wake.strippedCommand else rawInput).trim()
        if (cleaned.isEmpty()) return null
        val lower = cleaned.lowercase()

        // 1. Settings Intents (specific pages before general settings)
        parseSettingsCommand(lower)?.let { return it }

        // 2. Alarm Intent ("Set an alarm for 7:30 AM", "Set alarm 6 PM", "অ্যালার্ম দাও", "अलार्म लगाओ")
        if (isAlarmCommand(lower)) {
            return parseAlarmAction(lower)
        }

        // 3. Timer Intent ("Set a timer for 5 minutes", "Start 30 second timer")
        if (isTimerCommand(lower)) {
            return parseTimerAction(lower)
        }

        // 4. Camera Intent
        if (lower == "camera" || lower == "open camera" || lower == "launch camera" ||
            lower == "take a photo" || lower.contains("ক্যামেরা খোলো") || lower.contains("कैमरा खोलो")
        ) {
            return VoiceIntentAction.OpenCamera
        }

        // 5. Dialer Intent (non-sensitive: opens dialer UI without placing call)
        if (lower == "open dialer" || lower == "open phone" || lower == "open phone app") {
            return VoiceIntentAction.OpenDialer()
        }

        // 6. Web Search Intent ("search google for ...", "google search ...")
        val searchRegex = Regex("""(?i)^(?:google\s+search|search\s+(?:on\s+)?google(?:\s+for)?)\s+(.+)$""")
        searchRegex.find(cleaned)?.let { match ->
            val query = match.groupValues[1].trim()
            if (query.isNotBlank()) return VoiceIntentAction.WebSearch(query)
        }

        // 7. Open Installed Apps ("Open WhatsApp", "Launch YouTube", "YouTube খোলো", "व्हाट्सएप खोलो")
        extractAppTarget(cleaned, lower)?.let { target ->
            val pkg = resolvePackageName(target)
            return VoiceIntentAction.OpenApp(appName = target, resolvedPackageName = pkg)
        }

        return null
    }

    /**
     * Builds the native Android [Intent] corresponding to a parsed [VoiceIntentAction].
     */
    fun buildIntent(action: VoiceIntentAction): Intent? {
        return when (action) {
            is VoiceIntentAction.OpenSettings -> {
                Intent(action.settingsAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            is VoiceIntentAction.OpenCamera -> {
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            is VoiceIntentAction.SetAlarm -> {
                Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, action.hour24.coerceIn(0, 23))
                    putExtra(AlarmClock.EXTRA_MINUTES, action.minute.coerceIn(0, 59))
                    putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            is VoiceIntentAction.SetTimer -> {
                Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, action.durationSeconds.coerceAtLeast(1))
                    putExtra(AlarmClock.EXTRA_MESSAGE, action.label)
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            is VoiceIntentAction.OpenDialer -> {
                val uri = if (!action.phoneNumber.isNullOrBlank()) {
                    Uri.parse("tel:${action.phoneNumber.trim()}")
                } else {
                    Uri.parse("tel:")
                }
                Intent(Intent.ACTION_DIAL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            is VoiceIntentAction.WebSearch -> {
                Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, action.query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            is VoiceIntentAction.OpenApp -> {
                val pm = context.packageManager
                val pkg = action.resolvedPackageName ?: resolvePackageName(action.appName)
                if (pkg != null) {
                    pm.getLaunchIntentForPackage(pkg)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } else {
                    null
                }
            }
        }
    }

    /**
     * Parses and executes an intent-based voice command safely, respecting Android lock-screen
     * policies and returning a user-ready confirmation in the detected language.
     */
    fun handleCommand(
        rawInput: String,
        languageCode: String = "en",
        isDeviceLocked: Boolean = false
    ): VoiceCommandResult {
        val action = parseCommand(rawInput) ?: return VoiceCommandResult(handled = false)

        // Lock-screen policy: allow alarms & timers, restrict launching arbitrary apps/settings
        if (isDeviceLocked && action !is VoiceIntentAction.SetAlarm && action !is VoiceIntentAction.SetTimer) {
            val lockedMsg = when (languageCode) {
                "bn" -> "ডিভাইস লক করা আছে। অ্যাপ বা সেটিংস খোলার আগে অনুগ্রহ করে ফোন আনলক করো।"
                "hi" -> "फ़ोन लॉक है। ऐप या सेटिंग्स खोलने से पहले कृपया फ़ोन अनलॉक करें।"
                else -> "Device is locked. Please unlock your phone before opening apps or settings."
            }
            return VoiceCommandResult(
                handled = true,
                success = false,
                action = action,
                replyText = lockedMsg,
                lockScreenRestricted = true
            )
        }

        val intent = buildIntent(action)
        if (intent == null) {
            val notFoundMsg = when (action) {
                is VoiceIntentAction.OpenApp -> when (languageCode) {
                    "bn" -> "'${action.appName}' অ্যাপটি এই ডিভাইসে ইনস্টল করা নেই।"
                    "hi" -> "'${action.appName}' ऐप इस डिवाइस पर इंस्टॉल नहीं है।"
                    else -> "App '${action.appName}' is not installed on this device."
                }
                else -> "Unable to prepare Android action for this command."
            }
            return VoiceCommandResult(
                handled = true,
                success = false,
                action = action,
                replyText = notFoundMsg
            )
        }

        return try {
            context.startActivity(intent)
            VoiceCommandResult(
                handled = true,
                success = true,
                action = action,
                replyText = formatSuccessReply(action, languageCode),
                launchedIntent = intent
            )
        } catch (e: ActivityNotFoundException) {
            // Fallback for WebSearch if no ACTION_WEB_SEARCH handler exists
            if (action is VoiceIntentAction.WebSearch) {
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/search?q=${Uri.encode(action.query)}")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val fallbackOk = runCatching { context.startActivity(browserIntent) }.isSuccess
                if (fallbackOk) {
                    return VoiceCommandResult(
                        handled = true,
                        success = true,
                        action = action,
                        replyText = formatSuccessReply(action, languageCode),
                        launchedIntent = browserIntent
                    )
                }
            }
            VoiceCommandResult(
                handled = true,
                success = false,
                action = action,
                replyText = "No compatible app found on this device to handle that action."
            )
        } catch (e: Exception) {
            VoiceCommandResult(
                handled = true,
                success = false,
                action = action,
                replyText = "Could not launch action: ${e.localizedMessage ?: "unknown error"}"
            )
        }
    }

    private fun parseSettingsCommand(lower: String): VoiceIntentAction.OpenSettings? {
        return when {
            lower.contains("wifi settings") || lower.contains("wi-fi settings") || lower.contains("ওয়াইফাই সেটিংস") ->
                VoiceIntentAction.OpenSettings(Settings.ACTION_WIFI_SETTINGS, "Wi-Fi Settings")
            lower.contains("bluetooth settings") || lower.contains("ব্লুটুথ সেটিংস") ->
                VoiceIntentAction.OpenSettings(Settings.ACTION_BLUETOOTH_SETTINGS, "Bluetooth Settings")
            lower.contains("display settings") || lower.contains("brightness settings") ->
                VoiceIntentAction.OpenSettings(Settings.ACTION_DISPLAY_SETTINGS, "Display Settings")
            lower.contains("sound settings") || lower.contains("volume settings") ->
                VoiceIntentAction.OpenSettings(Settings.ACTION_SOUND_SETTINGS, "Sound Settings")
            lower.contains("location settings") || lower.contains("gps settings") ->
                VoiceIntentAction.OpenSettings(Settings.ACTION_LOCATION_SOURCE_SETTINGS, "Location Settings")
            lower.contains("battery settings") ->
                VoiceIntentAction.OpenSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS, "Battery Settings")
            lower.contains("open settings") || lower.contains("android settings") ||
                lower == "settings" || lower.contains("সেটিংস খোলো") || lower.contains("सेटिंग्स खोलो") ->
                VoiceIntentAction.OpenSettings(Settings.ACTION_SETTINGS, "Settings")
            else -> null
        }
    }

    private fun isAlarmCommand(lower: String): Boolean =
        lower.contains("alarm") || lower.contains("অ্যালার্ম") || lower.contains("আল্যার্ম") || lower.contains("अलार्म")

    private fun isTimerCommand(lower: String): Boolean =
        lower.contains("timer") || lower.contains("টাইমার") || lower.contains("टाइमर")

    private fun parseAlarmAction(lower: String): VoiceIntentAction.SetAlarm {
        val timeRegex = Regex("""(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?)?""")
        val match = timeRegex.find(lower)
        var hour = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 7
        val minute = match?.groupValues?.getOrNull(2)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val meridiem = match?.groupValues?.getOrNull(3)?.replace(".", "").orEmpty()

        when (meridiem) {
            "pm" -> if (hour in 1..11) hour += 12
            "am" -> if (hour == 12) hour = 0
        }
        return VoiceIntentAction.SetAlarm(
            hour24 = hour.coerceIn(0, 23),
            minute = minute,
            label = "NOVA Alarm"
        )
    }

    private fun parseTimerAction(lower: String): VoiceIntentAction.SetTimer {
        val number = Regex("""\d+""").find(lower)?.value?.toIntOrNull()?.coerceAtLeast(1) ?: 5
        val seconds = when {
            lower.contains("hour") || lower.contains("ঘণ্টা") || lower.contains("ঘন্টা") || lower.contains("घंटा") ->
                number * 3600
            lower.contains("sec") || lower.contains("সেকেন্ড") || lower.contains("सेकंड") ->
                number
            else ->
                number * 60
        }
        return VoiceIntentAction.SetTimer(durationSeconds = seconds, label = "NOVA Timer")
    }

    private fun extractAppTarget(original: String, lower: String): String? {
        val openPattern = Regex("""(?i)^(?:open|launch|start)\s+(.+)$""").find(original.trim())
        if (openPattern != null) {
            return openPattern.groupValues[1].trim().trimEnd('.', '।', '!', '?')
        }
        val bnPattern = Regex("""^(.+?)\s*(?:খোলো|খুলো|চালু করো)[।!?\s]*$""").find(original.trim())
        if (bnPattern != null) {
            return bnPattern.groupValues[1].trim()
        }
        val hiPattern = Regex("""^(.+?)\s*(?:खोलो|खोलें|चलाओ)[।!?\s]*$""").find(original.trim())
        if (hiPattern != null) {
            return hiPattern.groupValues[1].trim()
        }
        if (lower in knownAppPackages.keys) {
            return original.trim()
        }
        return null
    }

    private fun resolvePackageName(appQuery: String): String? {
        val clean = appQuery.trim().lowercase()
        knownAppPackages.entries.firstOrNull { clean == it.key || clean.contains(it.key) }?.let {
            return it.value
        }
        val installed = phoneController.getInstalledLauncherApps()
        val match = installed.firstOrNull { it.label.lowercase() == clean }
            ?: installed.firstOrNull { it.label.lowercase().contains(clean) }
            ?: installed.firstOrNull { clean.contains(it.label.lowercase()) }
        return match?.packageName
    }

    private fun formatSuccessReply(action: VoiceIntentAction, languageCode: String): String {
        return when (action) {
            is VoiceIntentAction.OpenApp -> when (languageCode) {
                "bn" -> "${action.appName} খোলা হচ্ছে।"
                "hi" -> "${action.appName} खोला जा रहा है।"
                else -> "Opening ${action.appName}."
            }
            is VoiceIntentAction.OpenSettings -> when (languageCode) {
                "bn" -> "${action.label} খোলা হচ্ছে।"
                "hi" -> "${action.label} खोली जा रही है।"
                else -> "Opening ${action.label}."
            }
            is VoiceIntentAction.OpenCamera -> when (languageCode) {
                "bn" -> "ক্যামেরা খোলা হচ্ছে।"
                "hi" -> "कैमरा खोला जा रहा है।"
                else -> "Opening Camera."
            }
            is VoiceIntentAction.SetAlarm -> {
                val formatted = "%02d:%02d".format(action.hour24, action.minute)
                when (languageCode) {
                    "bn" -> "$formatted-এর জন্য অ্যালার্ম সেট করা হচ্ছে।"
                    "hi" -> "$formatted के लिए अलार्म सेट किया जा रहा है।"
                    else -> "Setting an alarm for $formatted."
                }
            }
            is VoiceIntentAction.SetTimer -> {
                val mins = action.durationSeconds / 60
                val secs = action.durationSeconds % 60
                val durationLabel = when {
                    mins > 0 && secs > 0 -> "${mins}m ${secs}s"
                    mins > 0 -> "$mins minutes"
                    else -> "$secs seconds"
                }
                when (languageCode) {
                    "bn" -> "$durationLabel-এর টাইমার চালু করা হচ্ছে।"
                    "hi" -> "$durationLabel का टाइमर शुरू किया जा रहा है।"
                    else -> "Starting a timer for $durationLabel."
                }
            }
            is VoiceIntentAction.OpenDialer -> "Opening Phone Dialer."
            is VoiceIntentAction.WebSearch -> "Searching Google for \"${action.query}\"."
        }
    }
}
