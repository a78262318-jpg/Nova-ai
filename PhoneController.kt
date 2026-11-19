package com.example.device

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.example.service.ReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder

data class InstalledAppInfo(
    val label: String,
    val packageName: String
)

data class ContactInfo(
    val name: String,
    val phoneNumber: String
)

data class ActionResult(
    val success: Boolean,
    val message: String
)

class PhoneController(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private var activeRingtone: Ringtone? = null
    private var findPhoneJob: Job? = null

    fun getMediaVolumePercent(): Int {
        val am = audioManager ?: return 50
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        return ((current * 100f) / max).toInt()
    }

    fun setMediaVolumePercent(percent: Int): ActionResult {
        val am = audioManager ?: return ActionResult(false, "Audio service unavailable")
        val clamped = percent.coerceIn(0, 100)
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = ((clamped / 100f) * max).toInt().coerceIn(0, max)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return ActionResult(true, "Media volume set to $clamped%.")
    }

    fun adjustMediaVolume(deltaPercent: Int): ActionResult {
        val current = getMediaVolumePercent()
        return setMediaVolumePercent(current + deltaPercent)
    }

    fun canWriteSystemSettings(): Boolean = Settings.System.canWrite(context)

    fun getSystemBrightnessPercent(): Int {
        return try {
            val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            ((raw * 100f) / 255f).toInt().coerceIn(0, 100)
        } catch (_: Exception) {
            50
        }
    }

    fun setSystemBrightnessPercent(percent: Int): ActionResult {
        if (!canWriteSystemSettings()) {
            return ActionResult(
                false,
                "Modify System Settings permission is needed to change global brightness."
            )
        }
        return try {
            val clamped = percent.coerceIn(5, 100)
            val value = ((clamped / 100f) * 255f).toInt().coerceIn(10, 255)
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            ActionResult(true, "Screen brightness set to $clamped%.")
        } catch (e: Exception) {
            ActionResult(false, "Unable to change brightness: ${e.localizedMessage}")
        }
    }

    fun hasFlashlight(): Boolean {
        return findFlashCameraId() != null
    }

    private fun findFlashCameraId(): String? {
        val cm = cameraManager ?: return null
        return try {
            cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            null
        }
    }

    fun setFlashlight(enabled: Boolean): ActionResult {
        val cm = cameraManager ?: return ActionResult(false, "Camera service unavailable")
        val cameraId = findFlashCameraId()
            ?: return ActionResult(false, "No flashlight hardware detected on this device.")
        return try {
            cm.setTorchMode(cameraId, enabled)
            ActionResult(true, if (enabled) "Flashlight turned ON." else "Flashlight turned OFF.")
        } catch (e: Exception) {
            ActionResult(false, "Flashlight unavailable: ${e.localizedMessage}")
        }
    }

    fun getInstalledLauncherApps(): List<InstalledAppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return activities
            .map {
                InstalledAppInfo(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun launchAppByName(query: String): ActionResult {
        val cleanQuery = query.trim().lowercase()
        val aliasPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "ইউটিউব" to "com.google.android.youtube",
            "यूट्यूब" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "হোয়াটসঅ্যাপ" to "com.whatsapp",
            "व्हाट्सएप" to "com.whatsapp",
            "chrome" to "com.android.chrome",
            "gmail" to "com.google.android.gm",
            "maps" to "com.google.android.apps.maps",
            "camera" to "com.android.camera",
            "ক্যামেরা" to "com.android.camera",
            "कैमरा" to "com.android.camera",
            "calculator" to "com.google.android.calculator",
            "ক্যালকুলেটর" to "com.google.android.calculator",
            "कैलकुलेटर" to "com.google.android.calculator",
            "settings" to "com.android.settings",
            "সেটিংস" to "com.android.settings",
            "सेटिंग्स" to "com.android.settings",
            "clock" to "com.google.android.deskclock",
            "play store" to "com.android.vending",
            "spotify" to "com.spotify.music"
        )

        val pm = context.packageManager
        // Special direct settings alias
        if (cleanQuery in listOf("settings", "সেটিংস", "सेटिंग्स", "android settings")) {
            return openSystemSettings(Settings.ACTION_SETTINGS)
        }

        val aliasedPkg = aliasPackages.entries.firstOrNull { cleanQuery.contains(it.key) }?.value
        if (aliasedPkg != null) {
            val launch = pm.getLaunchIntentForPackage(aliasedPkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return ActionResult(true, "Opening ${cleanQuery.replaceFirstChar { it.uppercase() }}.")
            }
        }

        val apps = getInstalledLauncherApps()
        val match = apps.firstOrNull { it.label.lowercase() == cleanQuery }
            ?: apps.firstOrNull { it.label.lowercase().contains(cleanQuery) }
            ?: apps.firstOrNull { cleanQuery.contains(it.label.lowercase()) }

        if (match != null) {
            val launch = pm.getLaunchIntentForPackage(match.packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return ActionResult(true, "Opening ${match.label}.")
            }
        }
        return ActionResult(false, "App '$query' is not installed on this device.")
    }

    fun openSystemSettings(action: String = Settings.ACTION_SETTINGS, withPackageUri: Boolean = false): ActionResult {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (withPackageUri) {
                    data = Uri.parse("package:${context.packageName}")
                }
            }
            context.startActivity(intent)
            ActionResult(true, "Opened system settings.")
        } catch (e: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(fallback) }
            ActionResult(false, "Could not open specific settings page: ${e.localizedMessage}")
        }
    }

    fun setSystemAlarm(hour: Int, minute: Int, message: String = "NOVA Alarm"): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(true, "Setting alarm for %02d:%02d.".format(hour, minute))
        } catch (e: Exception) {
            ActionResult(false, "No Clock app available to set alarm: ${e.localizedMessage}")
        }
    }

    fun setSystemTimer(seconds: Int, message: String = "NOVA Timer"): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(1))
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val mins = seconds / 60
            val remSec = seconds % 60
            val label = if (mins > 0) "${mins}m ${remSec}s" else "${remSec}s"
            ActionResult(true, "Timer started for $label.")
        } catch (e: Exception) {
            ActionResult(false, "Clock timer intent unavailable: ${e.localizedMessage}")
        }
    }

    fun scheduleLocalReminder(id: Long, title: String, triggerAtMillis: Long): ActionResult {
        return try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: return ActionResult(false, "AlarmManager unavailable")
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("reminder_id", id)
                putExtra("reminder_title", title)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
            }
            ActionResult(true, "Reminder '$title' scheduled.")
        } catch (e: Exception) {
            ActionResult(false, "Could not schedule reminder: ${e.localizedMessage}")
        }
    }

    fun readContacts(query: String = "", limit: Int = 50): List<ContactInfo> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        val list = mutableListOf<ContactInfo>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = if (query.isNotBlank()) {
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        } else null
        val selectionArgs = if (query.isNotBlank()) arrayOf("%$query%") else null

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext() && list.size < limit) {
                val name = cursor.getString(nameIdx).orEmpty()
                val num = cursor.getString(numIdx).orEmpty()
                if (name.isNotBlank() && num.isNotBlank()) {
                    list.add(ContactInfo(name, num))
                }
            }
        }
        return list.distinctBy { "${it.name}|${it.phoneNumber}" }
    }

    @SuppressLint("MissingPermission")
    fun placePhoneCall(phoneNumber: String): ActionResult {
        val clean = phoneNumber.trim()
        if (clean.isBlank()) return ActionResult(false, "Phone number is empty.")
        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        return try {
            val intent = Intent(
                if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL,
                Uri.parse("tel:$clean")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult(
                true,
                if (hasCallPermission) "Calling $clean..." else "Opening dialer for $clean..."
            )
        } catch (e: Exception) {
            ActionResult(false, "Unable to initiate call: ${e.localizedMessage}")
        }
    }

    fun sendSms(phoneNumber: String, message: String): ActionResult {
        val clean = phoneNumber.trim()
        if (clean.isBlank() || message.isBlank()) {
            return ActionResult(false, "Phone number and message are required.")
        }
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return if (hasSmsPermission) {
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(clean, null, message, null, null)
                ActionResult(true, "SMS sent to $clean.")
            } catch (e: Exception) {
                ActionResult(false, "Failed to send SMS: ${e.localizedMessage}")
            }
        } else {
            try {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$clean")).apply {
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "Prepared SMS draft in Messaging app for $clean.")
            } catch (e: Exception) {
                ActionResult(false, "No SMS app available: ${e.localizedMessage}")
            }
        }
    }

    fun prepareWhatsAppMessage(phoneNumber: String?, message: String): ActionResult {
        return try {
            val encoded = URLEncoder.encode(message, "UTF-8")
            val cleanPhone = phoneNumber?.replace(Regex("[^+0-9]"), "").orEmpty()
            val intent = if (cleanPhone.isNotBlank()) {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${cleanPhone.removePrefix("+")}?text=$encoded"))
            } else {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    setPackage("com.whatsapp")
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult(true, "Prepared WhatsApp message.")
        } catch (e: Exception) {
            ActionResult(false, "WhatsApp is not installed or unavailable: ${e.localizedMessage}")
        }
    }

    fun triggerFindMyPhone(): ActionResult {
        stopFindMyPhone()
        return try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            activeRingtone = ringtone
            ringtone?.play()

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            findPhoneJob = CoroutineScope(Dispatchers.Default).launch {
                repeat(10) { idx ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(350, VibrationEffect.DEFAULT_AMPLITUDE))
                    }
                    setFlashlight(idx % 2 == 0)
                    delay(450)
                }
                setFlashlight(false)
                ringtone?.stop()
            }
            ActionResult(true, "Find My Phone alert triggered (ringing + flashlight strobe).")
        } catch (e: Exception) {
            ActionResult(false, "Alert failed: ${e.localizedMessage}")
        }
    }

    fun stopFindMyPhone() {
        findPhoneJob?.cancel()
        findPhoneJob = null
        runCatching { activeRingtone?.stop() }
        activeRingtone = null
        setFlashlight(false)
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
