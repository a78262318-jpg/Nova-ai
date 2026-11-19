package com.example.ui.navigation

import kotlinx.serialization.Serializable

sealed interface NovaRoute {
    @Serializable data object Splash : NovaRoute
    @Serializable data object Welcome : NovaRoute
    @Serializable data object PermissionSetup : NovaRoute
    @Serializable data object Dashboard : NovaRoute
    @Serializable data object VoiceAssistant : NovaRoute
    @Serializable data object AiChat : NovaRoute
    @Serializable data object DeviceManager : NovaRoute
    @Serializable data object BatteryDashboard : NovaRoute
    @Serializable data object PersonalMemory : NovaRoute
    @Serializable data object ConversationHistory : NovaRoute
    @Serializable data object LanguageSettings : NovaRoute
    @Serializable data object FemaleVoiceSettings : NovaRoute
    @Serializable data object OnlineOfflineSettings : NovaRoute
    @Serializable data object PrivacyDashboard : NovaRoute
    @Serializable data object AboutNova : NovaRoute
}
