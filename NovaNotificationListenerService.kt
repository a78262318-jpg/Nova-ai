package com.example.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CapturedNotification(
    val appPackage: String,
    val title: String,
    val text: String,
    val postTime: Long
)

class NovaNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val extras = notification.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()

        // Privacy rule: never store OTPs, passwords, or empty system alerts
        val combined = "$title $text".lowercase()
        if (combined.contains("otp") || combined.contains("password") || combined.contains("verification code")) {
            return
        }
        if (title.isBlank() && text.isBlank()) return

        val current = _recentNotifications.value.toMutableList()
        current.add(0, CapturedNotification(sbn.packageName, title, text, sbn.postTime))
        _recentNotifications.value = current.take(15)
    }

    companion object {
        private val _recentNotifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
        val recentNotifications: StateFlow<List<CapturedNotification>> = _recentNotifications.asStateFlow()

        fun clearCaptured() {
            _recentNotifications.value = emptyList()
        }

        fun isPermissionGranted(context: Context): Boolean {
            val enabledListeners = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ).orEmpty()
            val component = ComponentName(context, NovaNotificationListenerService::class.java)
            return enabledListeners.contains(component.flattenToString())
        }
    }
}
