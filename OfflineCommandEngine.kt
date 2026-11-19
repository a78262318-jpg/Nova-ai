package com.example.ai

import android.content.Context
import com.example.data.local.NovaLocalRepository
import com.example.device.DeviceStatsProvider
import com.example.device.PhoneController
import com.example.service.NovaNotificationListenerService
import com.example.voice.VoiceCommandManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

sealed interface SensitiveActionRequest {
    data class MakeCall(val displayName: String, val phoneNumber: String) : SensitiveActionRequest
    data class SendSms(val displayName: String, val phoneNumber: String, val message: String) : SensitiveActionRequest
    data class PrepareWhatsApp(val displayName: String, val phoneNumber: String?, val message: String) : SensitiveActionRequest
}

data class CommandExecutionOutcome(
    val handled: Boolean,
    val replyText: String = "",
    val languageCode: String = "en",
    val source: String = "offline",
    val sensitiveAction: SensitiveActionRequest? = null,
    val lockScreenRestricted: Boolean = false,
    val requiresWriteSettingsForBrightness: Int? = null
)

class OfflineCommandEngine(
    private val context: Context,
    private val statsProvider: DeviceStatsProvider,
    private val phoneController: PhoneController,
    private val localRepo: NovaLocalRepository,
    private val voiceCommandManager: VoiceCommandManager = VoiceCommandManager(context, phoneController)
) {

    suspend fun tryHandleCommand(
        rawInput: String,
        languageCode: String,
        isDeviceLocked: Boolean,
        forceOffline: Boolean
    ): CommandExecutionOutcome {
        val text = rawInput.trim()
        val lower = text.lowercase()

        // 1. Lock Screen Safe Mode enforcement
        if (isDeviceLocked) {
            if (isBatteryQuery(lower)) return batteryReply(languageCode)
            if (isTimeOrDateQuery(lower)) return timeDateReply(languageCode)
            if (isTimerQuery(lower) || isAlarmQuery(lower)) {
                val res = voiceCommandManager.handleCommand(text, languageCode, isDeviceLocked = true)
                if (res.handled) {
                    return CommandExecutionOutcome(true, res.replyText, languageCode, "device", lockScreenRestricted = res.lockScreenRestricted)
                }
            }
            if (isFindPhoneQuery(lower)) {
                val res = phoneController.triggerFindMyPhone()
                return CommandExecutionOutcome(true, res.message, languageCode, "device")
            }
            val calc = tryEvaluateMath(text)
            if (calc != null) {
                return CommandExecutionOutcome(true, formatMathReply(calc, languageCode), languageCode, "offline")
            }
            return CommandExecutionOutcome(
                handled = true,
                replyText = when (languageCode) {
                    "bn" -> "ডিভাইস লক করা আছে। নিরাপত্তার জন্য লক স্ক্রিনে শুধুমাত্র সময়, ব্যাটারি, টাইমার, অ্যালার্ম বা ক্যালকুলেটর ব্যবহার করা যাবে।"
                    "hi" -> "फ़ोन लॉक है। सुरक्षा के लिए लॉक स्क्रीन पर केवल समय, बैटरी, टाइमर, अलार्म या कैलकुलेटर की अनुमति है।"
                    else -> "Device is locked. For security, only time, battery, timers, alarms, and basic math are available on the lock screen."
                },
                languageCode = languageCode,
                source = "lockscreen",
                lockScreenRestricted = true
            )
        }

        // 2. Notification Reader (only with explicit Android Notification Access permission)
        if (lower.contains("read notification") || lower.contains("check notification") ||
            lower.contains("my notifications") || lower.contains("নোটিফিকেশন") || lower.contains("नोटिफिकेशन")
        ) {
            if (!NovaNotificationListenerService.isPermissionGranted(context)) {
                val msg = when (languageCode) {
                    "bn" -> "নোটিফিকেশন পড়ার জন্য Notification Access অনুমতি প্রয়োজন। অনুগ্রহ করে Permission Setup বা Privacy Dashboard থেকে এটি চালু করো।"
                    "hi" -> "नोटिफिकेशन पढ़ने के लिए Notification Access अनुमति आवश्यक है। कृपया Permission Setup या Privacy Dashboard से इसे चालू करें।"
                    else -> "Notification Access permission is not granted yet. Enable it in Permission Setup or Privacy Dashboard so NOVA can read your notifications."
                }
                return CommandExecutionOutcome(true, msg, languageCode, "device")
            }
            val items = NovaNotificationListenerService.recentNotifications.value
            val msg = if (items.isEmpty()) {
                when (languageCode) {
                    "bn" -> "এখন কোনো নতুন নোটিফিকেশন নেই।"
                    "hi" -> "अभी कोई नया नोटिफिकेशन नहीं है।"
                    else -> "You have no recent notifications right now."
                }
            } else {
                val summary = items.take(3).joinToString(". ") { n ->
                    val app = n.appPackage.substringAfterLast('.')
                    "$app: ${n.title} — ${n.text}"
                }
                "Here are your latest notifications: $summary"
            }
            return CommandExecutionOutcome(true, msg, languageCode, "device")
        }

        // 3. Battery & Charging Query
        if (isBatteryQuery(lower)) return batteryReply(languageCode)

        // 4. Time & Date Query
        if (isTimeOrDateQuery(lower)) return timeDateReply(languageCode)

        // 5. RAM & Storage Info
        if (lower.contains("ram") || lower.contains("memory") || lower.contains("storage") ||
            lower.contains("মেমরি") || lower.contains("স্টোরেজ") || lower.contains("रैम") || lower.contains("स्टोरेज")
        ) {
            val mem = statsProvider.getMemoryTelemetry()
            val storage = statsProvider.getStorageTelemetry()
            val msg = when (languageCode) {
                "bn" -> "র‍্যাম ব্যবহার হচ্ছে ${mem.usedRamMb} MB (মোট ${mem.totalRamMb} MB)। স্টোরেজ খালি আছে %.1f GB (মোট %.1f GB)।".format(storage.availableGb, storage.totalGb)
                "hi" -> "RAM उपयोग: ${mem.usedRamMb} MB / ${mem.totalRamMb} MB। उपलब्ध स्टोरेज: %.1f GB / %.1f GB।".format(storage.availableGb, storage.totalGb)
                else -> "RAM: ${mem.usedRamMb} MB used of ${mem.totalRamMb} MB (${mem.availableRamMb} MB free). Storage: %.1f GB free of %.1f GB.".format(storage.availableGb, storage.totalGb)
            }
            return CommandExecutionOutcome(true, msg, languageCode, "device")
        }

        // 6. Device Model & Android Version
        if (lower.contains("device info") || lower.contains("device model") || lower.contains("android version") ||
            lower.contains("ফোন মডেল") || lower.contains("डिवाइस मॉडल")
        ) {
            val snap = statsProvider.getSnapshot()
            val msg = when (languageCode) {
                "bn" -> "ডিভাইস: ${snap.manufacturer} ${snap.deviceModel}, অ্যান্ড্রয়েড ভার্সন ${snap.androidVersion} (SDK ${snap.sdkLevel})।"
                "hi" -> "डिवाइस: ${snap.manufacturer} ${snap.deviceModel}, Android ${snap.androidVersion} (SDK ${snap.sdkLevel})।"
                else -> "Device: ${snap.manufacturer} ${snap.deviceModel}, Android ${snap.androidVersion} (API ${snap.sdkLevel})."
            }
            return CommandExecutionOutcome(true, msg, languageCode, "device")
        }

        // 7. Network Status
        if (lower.contains("network status") || lower.contains("internet status") || lower.contains("wifi status") ||
            lower.contains("নেটওয়ার্ক") || lower.contains("नेटवर्क")
        ) {
            val net = statsProvider.getNetworkTelemetry()
            val msg = when (languageCode) {
                "bn" -> if (net.isConnected) "নেটওয়ার্ক সংযুক্ত আছে: ${net.transportType}।" else "ডিভাইসটি এখন অফলাইন।"
                "hi" -> if (net.isConnected) "नेटवर्क जुड़ा है: ${net.transportType}।" else "आपका फ़ोन अभी ऑफ़लाइन है।"
                else -> if (net.isConnected) "Connected via ${net.transportType} (validated: ${net.isValidated})." else "Device is currently offline."
            }
            return CommandExecutionOutcome(true, msg, languageCode, "device")
        }

        // 8. Flashlight Controls
        if (lower.contains("flashlight") || lower.contains("torch") || lower.contains("টর্চ") ||
            lower.contains("ফ্ল্যাশলাইট") || lower.contains("फ्लैशलाइट") || lower.contains("टॉर्च")
        ) {
            val turnOff = lower.contains("off") || lower.contains("বন্ধ") || lower.contains("নেভাও") || lower.contains("बंद")
            val res = phoneController.setFlashlight(!turnOff)
            val reply = when (languageCode) {
                "bn" -> if (res.success) (if (!turnOff) "ফ্ল্যাশলাইট চালু করা হয়েছে।" else "ফ্ল্যাশলাইট বন্ধ করা হয়েছে।") else res.message
                "hi" -> if (res.success) (if (!turnOff) "फ़्लैशलाइट चालू कर दी गई है।" else "फ़्लैशलाइट बंद कर दी गई है।") else res.message
                else -> res.message
            }
            return CommandExecutionOutcome(true, reply, languageCode, "device")
        }

        // 9. Volume Controls
        if (lower.contains("volume") || lower.contains("ভলিউম") || lower.contains("आवाज") || lower.contains("वॉल्यूम")) {
            val res = when {
                lower.contains("up") || lower.contains("increase") || lower.contains("বাড়াও") || lower.contains("बढ़ाओ") ->
                    phoneController.adjustMediaVolume(15)
                lower.contains("down") || lower.contains("decrease") || lower.contains("কমাও") || lower.contains("कम") ->
                    phoneController.adjustMediaVolume(-15)
                lower.contains("mute") || lower.contains("zero") ->
                    phoneController.setMediaVolumePercent(0)
                else -> {
                    val num = Regex("""\d{1,3}""").find(lower)?.value?.toIntOrNull()
                    if (num != null) phoneController.setMediaVolumePercent(num)
                    else return CommandExecutionOutcome(
                        true,
                        "Current media volume is ${phoneController.getMediaVolumePercent()}%.",
                        languageCode,
                        "device"
                    )
                }
            }
            return CommandExecutionOutcome(true, res.message, languageCode, "device")
        }

        // 10. Brightness Controls
        if (lower.contains("brightness") || lower.contains("ব্রাইটনেস") || lower.contains("चमक") || lower.contains("ब्राइटनेस")) {
            val num = Regex("""\d{1,3}""").find(lower)?.value?.toIntOrNull()
            val current = phoneController.getSystemBrightnessPercent()
            val target = when {
                num != null -> num.coerceIn(5, 100)
                lower.contains("max") || lower.contains("full") -> 100
                lower.contains("min") || lower.contains("low") || lower.contains("dim") -> 25
                lower.contains("up") || lower.contains("increase") || lower.contains("বাড়াও") || lower.contains("बढ़ाओ") ->
                    (current + 20).coerceIn(5, 100)
                lower.contains("down") || lower.contains("decrease") || lower.contains("কমাও") || lower.contains("कम") ->
                    (current - 20).coerceIn(5, 100)
                else -> 65
            }
            if (!phoneController.canWriteSystemSettings()) {
                val msg = when (languageCode) {
                    "bn" -> "স্ক্রিনের ব্রাইটনেস $target% করতে Modify System Settings অনুমতি প্রয়োজন। অনুগ্রহ করে সেটিংসে এটি চালু করো।"
                    "hi" -> "स्क्रीन ब्राइटनेस $target% करने के लिए Modify System Settings अनुमति आवश्यक है। कृपया सेटिंग्स में इसे चालू करें।"
                    else -> "To set screen brightness to $target%, please allow Modify System Settings for NOVA AI."
                }
                return CommandExecutionOutcome(
                    handled = true,
                    replyText = msg,
                    languageCode = languageCode,
                    source = "device",
                    requiresWriteSettingsForBrightness = target
                )
            }
            val res = phoneController.setSystemBrightnessPercent(target)
            return CommandExecutionOutcome(true, res.message, languageCode, "device")
        }

        // 11. Find My Phone
        if (isFindPhoneQuery(lower)) {
            val res = phoneController.triggerFindMyPhone()
            return CommandExecutionOutcome(true, res.message, languageCode, "device")
        }

        // 12. Save / Read Offline Notes
        if (lower.startsWith("save note ") || lower.startsWith("note: ") || lower.startsWith("নোট রাখো ") || lower.startsWith("नोट सेव करो ")) {
            val noteBody = text
                .removePrefix("save note ").removePrefix("Save note ")
                .removePrefix("note: ").removePrefix("Note: ")
                .removePrefix("নোট রাখো ").removePrefix("नोट सेव करो ")
                .trim()
            if (noteBody.isNotBlank()) {
                localRepo.saveNote(title = "Voice Note", body = noteBody)
                val msg = when (languageCode) {
                    "bn" -> "তোমার নোটটি সেভ করা হয়েছে: \"$noteBody\"।"
                    "hi" -> "आपका नोट सेव हो गया है: \"$noteBody\"।"
                    else -> "Saved offline note: \"$noteBody\"."
                }
                return CommandExecutionOutcome(true, msg, languageCode, "offline")
            }
        }

        // 15. Phone Call / SMS / WhatsApp preparation (ALWAYS require confirmation!)
        val sensitive = parseSensitiveAction(text, lower)
        if (sensitive != null) {
            val promptMsg = when (sensitive) {
                is SensitiveActionRequest.MakeCall -> when (languageCode) {
                    "bn" -> "${sensitive.displayName} (${sensitive.phoneNumber})-কে কল করার আগে অনুগ্রহ করে নিশ্চিত করো।"
                    "hi" -> "${sensitive.displayName} (${sensitive.phoneNumber}) को कॉल करने के लिए कृपया पुष्टि करें।"
                    else -> "Please confirm before calling ${sensitive.displayName} (${sensitive.phoneNumber})."
                }
                is SensitiveActionRequest.SendSms -> when (languageCode) {
                    "bn" -> "${sensitive.displayName}-কে এসএমএস পাঠানোর আগে অনুগ্রহ করে নিশ্চিত করো।"
                    "hi" -> "${sensitive.displayName} को SMS भेजने से पहले कृपया पुष्टि करें।"
                    else -> "Please confirm before sending SMS to ${sensitive.displayName}."
                }
                is SensitiveActionRequest.PrepareWhatsApp -> when (languageCode) {
                    "bn" -> "${sensitive.displayName}-এর জন্য হোয়াটসঅ্যাপ মেসেজ প্রস্তুত। পাঠাতে নিশ্চিত করো।"
                    "hi" -> "${sensitive.displayName} के लिए WhatsApp संदेश तैयार है। कृपया पुष्टि करें।"
                    else -> "WhatsApp message prepared for ${sensitive.displayName}. Tap confirm to open WhatsApp."
                }
            }
            return CommandExecutionOutcome(
                handled = true,
                replyText = promptMsg,
                languageCode = languageCode,
                source = "device",
                sensitiveAction = sensitive
            )
        }

        // 14. Android Intent Commands via VoiceCommandManager (Open WhatsApp, Open Settings, Set Alarm/Timer, Camera, Apps)
        val intentResult = voiceCommandManager.handleCommand(
            rawInput = text,
            languageCode = languageCode,
            isDeviceLocked = isDeviceLocked
        )
        if (intentResult.handled) {
            return CommandExecutionOutcome(
                handled = true,
                replyText = intentResult.replyText,
                languageCode = languageCode,
                source = "device",
                lockScreenRestricted = intentResult.lockScreenRestricted
            )
        }

        // 17. Offline Math Calculator
        val mathResult = tryEvaluateMath(text)
        if (mathResult != null) {
            return CommandExecutionOutcome(true, formatMathReply(mathResult, languageCode), languageCode, "offline")
        }

        // 18. Strict Offline Mode OR no internet fallback
        if (forceOffline || !statsProvider.getNetworkTelemetry().isConnected) {
            val notes = localRepo.getNotesSnapshot()
            if (lower.contains("my notes") || lower.contains("saved notes") || lower.contains("নোট দেখাও") || lower.contains("नोट दिखाओ")) {
                val summary = if (notes.isEmpty()) "No saved notes yet." else notes.take(3).joinToString("; ") { "${it.title}: ${it.body}" }
                return CommandExecutionOutcome(true, summary, languageCode, "offline")
            }
            val msg = when (languageCode) {
                "bn" -> "আমি এখন অফলাইন মোডে আছি। ইন্টারনেট ছাড়া তুমি অ্যাপ খোলা, ব্যাটারি/র‍্যাম দেখা, ফ্ল্যাশলাইট, ভলিউম, অ্যালার্ম, টাইমার, নোট বা ক্যালকুলেটর চালাতে পারো।"
                "hi" -> "मैं अभी ऑफ़लाइन मोड में हूँ। बिना इंटरनेट के आप ऐप खोल सकते हैं, बैटरी/RAM देख सकते हैं, फ़्लैशलाइट, वॉल्यूम, अलार्म, टाइमर, नोट्स या कैलकुलेटर उपयोग कर सकते हैं।"
                else -> "NOVA is currently in Offline Mode. You can still launch apps, check battery/device stats, toggle flashlight/volume, set timers/alarms, save notes, or run math calculations. Connect to the internet for Gemini AI."
            }
            return CommandExecutionOutcome(true, msg, languageCode, "offline")
        }

        return CommandExecutionOutcome(handled = false, languageCode = languageCode)
    }

    private fun isBatteryQuery(lower: String): Boolean =
        lower.contains("battery") || lower.contains("charging") || lower.contains("ব্যাটারি") ||
            lower.contains("চার্জ") || lower.contains("बैटरी") || lower.contains("चार्ज")

    private fun isTimeOrDateQuery(lower: String): Boolean =
        lower.contains("what time") || lower.contains("current time") || lower.contains("what is the time") ||
            lower.contains("date today") || lower.contains("কয়টা বাজে") || lower.contains("সময় কত") ||
            lower.contains("সময় কত") || lower.contains("टाइम") || lower.contains("समय") || lower.contains("कितने बजे")

    private fun isTimerQuery(lower: String): Boolean =
        lower.contains("timer") || lower.contains("টাইমার") || lower.contains("टाइमर")

    private fun isAlarmQuery(lower: String): Boolean =
        lower.contains("alarm") || lower.contains("অ্যালার্ম") || lower.contains("আল্যার্ম") || lower.contains("अलार्म")

    private fun isFindPhoneQuery(lower: String): Boolean =
        lower.contains("find my phone") || lower.contains("ring my phone") ||
            lower.contains("ফোন খোঁজো") || lower.contains("फ़ोन ढूंढो")

    private fun batteryReply(languageCode: String): CommandExecutionOutcome {
        val b = statsProvider.getBatteryTelemetry()
        val msg = when (languageCode) {
            "bn" -> if (b.isCharging) {
                "তোমার ফোনের ব্যাটারি এখন ${b.percentage}% এবং ${b.chargeSource}-এ চার্জ হচ্ছে।"
            } else {
                "তোমার ফোনের ব্যাটারি এখন ${b.percentage}% (চার্জিং বন্ধ)।"
            }
            "hi" -> if (b.isCharging) {
                "आपके फ़ोन की बैटरी अभी ${b.percentage}% है और ${b.chargeSource} से चार्ज हो रही है।"
            } else {
                "आपके फ़ोन की बैटरी अभी ${b.percentage}% है।"
            }
            else -> if (b.isCharging) {
                "Your battery is at ${b.percentage}% and charging via ${b.chargeSource}."
            } else {
                "Your battery is at ${b.percentage}% (discharging)."
            }
        }
        return CommandExecutionOutcome(true, msg, languageCode, "device")
    }

    private fun timeDateReply(languageCode: String): CommandExecutionOutcome {
        val now = Date()
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(now)
        val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(now)
        val msg = when (languageCode) {
            "bn" -> "এখন সময় $timeStr, আজ $dateStr।"
            "hi" -> "अभी समय है $timeStr, आज $dateStr है।"
            else -> "It is currently $timeStr on $dateStr."
        }
        return CommandExecutionOutcome(true, msg, languageCode, "device")
    }

    private fun parseSensitiveAction(original: String, lower: String): SensitiveActionRequest? {
        if (lower.startsWith("call ") || lower.startsWith("make a phone call to ") || lower.contains("কল করো")) {
            val target = original
                .replace(Regex("(?i)^(make a phone )?call(\\s+to)?\\s+"), "")
                .replace("কল করো", "")
                .trim()
            if (target.isNotBlank()) {
                val contact = phoneController.readContacts(target, 1).firstOrNull()
                return SensitiveActionRequest.MakeCall(
                    displayName = contact?.name ?: target,
                    phoneNumber = contact?.phoneNumber ?: target.filter { it.isDigit() || it == '+' }.ifBlank { target }
                )
            }
        }
        if (lower.startsWith("sms ") || lower.startsWith("send sms ") || lower.startsWith("text ")) {
            val body = original.replace(Regex("(?i)^(send\\s+)?(sms|text)\\s+"), "").trim()
            val parts = body.split(":", limit = 2)
            val target = parts.firstOrNull()?.trim().orEmpty()
            val msg = parts.getOrNull(1)?.trim().orEmpty().ifBlank { "Hello from NOVA AI" }
            if (target.isNotBlank()) {
                val contact = phoneController.readContacts(target, 1).firstOrNull()
                return SensitiveActionRequest.SendSms(
                    displayName = contact?.name ?: target,
                    phoneNumber = contact?.phoneNumber ?: target,
                    message = msg
                )
            }
        }
        if (lower.contains("whatsapp message") || lower.startsWith("whatsapp ")) {
            val body = original.replace(Regex("(?i)^.*whatsapp(\\s+message)?(\\s+to)?\\s+"), "").trim()
            if (body.isNotBlank() && !lower.contains("open whatsapp")) {
                val parts = body.split(":", limit = 2)
                val target = parts.firstOrNull()?.trim().orEmpty()
                val msg = parts.getOrNull(1)?.trim().orEmpty().ifBlank { "Hello!" }
                val contact = phoneController.readContacts(target, 1).firstOrNull()
                return SensitiveActionRequest.PrepareWhatsApp(
                    displayName = contact?.name ?: target,
                    phoneNumber = contact?.phoneNumber,
                    message = msg
                )
            }
        }
        return null
    }

    private fun formatMathReply(value: Double, languageCode: String): String {
        val formatted = if (value % 1.0 == 0.0) value.toLong().toString() else "%.4f".format(value).trimEnd('0').trimEnd('.')
        return when (languageCode) {
            "bn" -> "উত্তর হলো $formatted।"
            "hi" -> "उत्तर है $formatted।"
            else -> "Result: $formatted"
        }
    }

    fun tryEvaluateMath(input: String): Double? {
        val cleaned = input.lowercase()
            .replace("calculate", "")
            .replace("what is", "")
            .replace("কত হয়", "")
            .replace("হিসাব করো", "")
            .replace("kitna hota hai", "")
            .replace("x", "*")
            .replace("×", "*")
            .replace("÷", "/")
            .trim()
        if (!cleaned.any { it.isDigit() } || !cleaned.any { it in listOf('+', '-', '*', '/', '^', '%') }) {
            return null
        }
        if (!cleaned.all { it.isDigit() || it in listOf('+', '-', '*', '/', '^', '%', '.', '(', ')', ' ') }) {
            return null
        }
        return runCatching { MathEvaluator(cleaned).parse() }.getOrNull()
    }

    private class MathEvaluator(private val expr: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < expr.length) expr[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < expr.length) throw IllegalArgumentException("Unexpected char")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                x = when {
                    eat('+'.code) -> x + parseTerm()
                    eat('-'.code) -> x - parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                x = when {
                    eat('*'.code) -> x * parseFactor()
                    eat('/'.code) -> {
                        val d = parseFactor()
                        if (d == 0.0) throw ArithmeticException("Divide by zero")
                        x / d
                    }
                    eat('%'.code) -> x % parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()
            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                if (!eat(')'.code)) throw IllegalArgumentException("Missing )")
            } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
                while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                x = expr.substring(startPos, pos).toDouble()
            } else {
                throw IllegalArgumentException("Unexpected")
            }
            if (eat('^'.code)) x = x.pow(parseFactor())
            return x
        }
    }
}
