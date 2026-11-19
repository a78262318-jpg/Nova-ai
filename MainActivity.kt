package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.configureAppCheck
import com.example.data.preferences.NovaThemeMode
import com.example.service.NovaVoiceService
import com.example.ui.navigation.NovaNavGraph
import com.example.ui.theme.NovaAiTheme
import com.example.ui.viewmodel.NovaViewModel

class MainActivity : ComponentActivity() {

    private var openVoiceFromIntent by mutableStateOf(false)
    private var pendingVoiceCommand by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI or ViewModels
        configureAppCheck(this, intent)

        openVoiceFromIntent = intent?.getBooleanExtra(NovaVoiceService.EXTRA_OPEN_VOICE, false) == true
        pendingVoiceCommand = intent?.getStringExtra(NovaVoiceService.EXTRA_PENDING_COMMAND)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            val novaViewModel: NovaViewModel = viewModel()
            val prefs by novaViewModel.preferences.collectAsStateWithLifecycle()
            val isDark = prefs.themeMode == NovaThemeMode.DARK

            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            NovaAiTheme(themeMode = prefs.themeMode) {
                NovaNavGraph(
                    viewModel = novaViewModel,
                    openVoiceDirectly = openVoiceFromIntent,
                    pendingVoiceCommand = pendingVoiceCommand
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(NovaVoiceService.EXTRA_OPEN_VOICE, false)) {
            openVoiceFromIntent = true
        }
        intent.getStringExtra(NovaVoiceService.EXTRA_PENDING_COMMAND)?.let {
            pendingVoiceCommand = it
        }
    }
}
