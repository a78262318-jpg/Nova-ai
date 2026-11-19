package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.novaDataStore: DataStore<Preferences> by preferencesDataStore(name = "nova_preferences")

enum class PreferredLanguage(val code: String, val displayName: String, val nativeLabel: String) {
    AUTO("auto", "Auto Detect", "Auto (BN / HI / EN)"),
    BENGALI("bn", "Bengali", "বাংলা"),
    HINDI("hi", "Hindi", "हिन्दी"),
    ENGLISH("en", "English", "English")
}

enum class NetworkMode(val label: String) {
    AUTO("Auto (Smart Online + Offline)"),
    ONLINE_ONLY("Online Priority"),
    OFFLINE_ONLY("Strict Offline Mode")
}

enum class NovaThemeMode(val label: String, val shortLabel: String) {
    DARK("Dark Futuristic", "Dark"),
    LIGHT("Light Futuristic", "Light")
}

enum class NovaPersonality(val title: String, val instruction: String) {
    WARM("Warm & Helpful", "Be warm, reassuring, clear, and naturally conversational."),
    CONCISE("Concise & Fast", "Give crisp, direct, high-signal answers without filler."),
    TUTOR("Scholarly Tutor", "Explain concepts step-by-step with clear examples for learning."),
    FUTURISTIC("Futuristic AI", "Respond like an intelligent, sleek personal sci-fi AI core.")
}

data class NovaPreferences(
    val hasCompletedOnboarding: Boolean = false,
    val preferredLanguage: PreferredLanguage = PreferredLanguage.AUTO,
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 1.08f,
    val selectedVoiceName: String = "",
    val personality: NovaPersonality = NovaPersonality.WARM,
    val networkMode: NetworkMode = NetworkMode.AUTO,
    val themeMode: NovaThemeMode = NovaThemeMode.DARK,
    val wakeWordEnabled: Boolean = false,
    val batterySaverWakePause: Boolean = true,
    val lockScreenSupportEnabled: Boolean = true,
    val continuousConversation: Boolean = true,
    val confirmSensitiveActions: Boolean = true,
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = ""
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val SELECTED_VOICE = stringPreferencesKey("selected_voice")
        val PERSONALITY = stringPreferencesKey("personality")
        val NETWORK_MODE = stringPreferencesKey("network_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val BATTERY_SAVER_PAUSE = booleanPreferencesKey("battery_saver_wake_pause")
        val LOCK_SCREEN_ENABLED = booleanPreferencesKey("lock_screen_enabled")
        val CONTINUOUS_CONVERSATION = booleanPreferencesKey("continuous_conversation")
        val CONFIRM_SENSITIVE = booleanPreferencesKey("confirm_sensitive")
        val EMERGENCY_NAME = stringPreferencesKey("emergency_name")
        val EMERGENCY_PHONE = stringPreferencesKey("emergency_phone")
    }

    val preferencesFlow: Flow<NovaPreferences> = context.novaDataStore.data.map { prefs ->
        NovaPreferences(
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_DONE] ?: false,
            preferredLanguage = PreferredLanguage.entries.find {
                it.code == prefs[Keys.PREFERRED_LANGUAGE]
            } ?: PreferredLanguage.AUTO,
            ttsSpeed = prefs[Keys.TTS_SPEED] ?: 1.0f,
            ttsPitch = prefs[Keys.TTS_PITCH] ?: 1.08f,
            selectedVoiceName = prefs[Keys.SELECTED_VOICE] ?: "",
            personality = NovaPersonality.entries.find {
                it.name == prefs[Keys.PERSONALITY]
            } ?: NovaPersonality.WARM,
            networkMode = NetworkMode.entries.find {
                it.name == prefs[Keys.NETWORK_MODE]
            } ?: NetworkMode.AUTO,
            themeMode = NovaThemeMode.entries.find {
                it.name == prefs[Keys.THEME_MODE]
            } ?: NovaThemeMode.DARK,
            wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
            batterySaverWakePause = prefs[Keys.BATTERY_SAVER_PAUSE] ?: true,
            lockScreenSupportEnabled = prefs[Keys.LOCK_SCREEN_ENABLED] ?: true,
            continuousConversation = prefs[Keys.CONTINUOUS_CONVERSATION] ?: true,
            confirmSensitiveActions = prefs[Keys.CONFIRM_SENSITIVE] ?: true,
            emergencyContactName = prefs[Keys.EMERGENCY_NAME] ?: "",
            emergencyContactPhone = prefs[Keys.EMERGENCY_PHONE] ?: ""
        )
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.novaDataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setPreferredLanguage(language: PreferredLanguage) {
        context.novaDataStore.edit { it[Keys.PREFERRED_LANGUAGE] = language.code }
    }

    suspend fun setTtsSpeed(speed: Float) {
        context.novaDataStore.edit { it[Keys.TTS_SPEED] = speed.coerceIn(0.6f, 1.6f) }
    }

    suspend fun setTtsPitch(pitch: Float) {
        context.novaDataStore.edit { it[Keys.TTS_PITCH] = pitch.coerceIn(0.75f, 1.35f) }
    }

    suspend fun setSelectedVoiceName(voiceName: String) {
        context.novaDataStore.edit { it[Keys.SELECTED_VOICE] = voiceName }
    }

    suspend fun setPersonality(personality: NovaPersonality) {
        context.novaDataStore.edit { it[Keys.PERSONALITY] = personality.name }
    }

    suspend fun setNetworkMode(mode: NetworkMode) {
        context.novaDataStore.edit { it[Keys.NETWORK_MODE] = mode.name }
    }

    suspend fun setThemeMode(mode: NovaThemeMode) {
        context.novaDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setBatterySaverWakePause(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.BATTERY_SAVER_PAUSE] = enabled }
    }

    suspend fun setLockScreenSupportEnabled(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.LOCK_SCREEN_ENABLED] = enabled }
    }

    suspend fun setContinuousConversation(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.CONTINUOUS_CONVERSATION] = enabled }
    }

    suspend fun setConfirmSensitiveActions(confirm: Boolean) {
        context.novaDataStore.edit { it[Keys.CONFIRM_SENSITIVE] = confirm }
    }

    suspend fun setEmergencyContact(name: String, phone: String) {
        context.novaDataStore.edit {
            it[Keys.EMERGENCY_NAME] = name.trim()
            it[Keys.EMERGENCY_PHONE] = phone.trim()
        }
    }

    suspend fun resetAllPreferences() {
        context.novaDataStore.edit {
            val onboarding = it[Keys.ONBOARDING_DONE] ?: true
            it.clear()
            it[Keys.ONBOARDING_DONE] = onboarding
        }
    }
}
