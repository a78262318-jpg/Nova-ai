package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary

/**
 * Increments every time the hosting lifecycle resumes (e.g. when returning from
 * Android System Settings after granting Modify System Settings or Microphone access).
 */
@Composable
fun rememberLifecycleResumeTick(): Int {
    val lifecycleOwner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                tick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return tick
}

fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

fun Context.hasRecordAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

fun Context.hasPostNotificationsPermission(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

fun Context.openAppDetailsSettings() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

fun Context.openWriteSystemSettings() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }.onFailure {
        runCatching {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Stable
class NovaMicPermissionController(
    private val requestSpeechAction: () -> Unit,
    private val requestWakeAction: () -> Unit
) {
    fun requestForSpeech() = requestSpeechAction()
    fun requestForWakeService() = requestWakeAction()
}

/**
 * Unified runtime permission controller for Microphone (`RECORD_AUDIO`) and Android 13+
 * Foreground Service Notification (`POST_NOTIFICATIONS`), with automatic rationale /
 * App Settings fallback dialog when permissions are denied.
 */
@Composable
fun rememberNovaMicPermissionController(
    onSpeechPermissionGranted: () -> Unit = {},
    onWakePermissionGranted: () -> Unit = {}
): NovaMicPermissionController {
    val context = LocalContext.current
    var showSettingsDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("Microphone Permission Required") }
    var dialogMessage by remember {
        mutableStateOf(
            "NOVA AI needs Microphone permission to hear your voice commands and detect the \"NOVA\" wake word."
        )
    }
    var pendingIsWake by remember { mutableStateOf(false) }

    val speechMicLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onSpeechPermissionGranted()
        } else {
            val activity = context.findActivity()
            val permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
            dialogTitle = "Microphone Access Needed"
            dialogMessage = if (permanentlyDenied) {
                "Microphone access was disabled for NOVA AI. Open App Settings -> Permissions -> Microphone and allow access to use voice commands."
            } else {
                "NOVA AI cannot listen to speech commands without Microphone permission. You can retry or enable it in App Settings."
            }
            pendingIsWake = false
            showSettingsDialog = true
        }
    }

    val wakeMultiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micGranted = results[Manifest.permission.RECORD_AUDIO] == true || context.hasRecordAudioPermission()
        if (micGranted) {
            onWakePermissionGranted()
        } else {
            dialogTitle = "Enable Microphone for \"NOVA\" Wake Word"
            dialogMessage = "Background \"NOVA\" wake-word detection requires Microphone permission (and Notifications on Android 13+ to show the active microphone indicator). Open App Settings to enable it."
            pendingIsWake = true
            showSettingsDialog = true
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text(dialogTitle, color = NovaTextPrimary) },
            text = { Text(dialogMessage, color = NovaTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSettingsDialog = false
                        context.openAppDetailsSettings()
                    }
                ) {
                    Text("Open App Settings", color = NovaCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSettingsDialog = false
                        if (pendingIsWake) {
                            val perms = buildList {
                                add(Manifest.permission.RECORD_AUDIO)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !context.hasPostNotificationsPermission()
                                ) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }.toTypedArray()
                            wakeMultiLauncher.launch(perms)
                        } else {
                            speechMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Text("Try Again", color = NovaAmber)
                }
            }
        )
    }

    return remember(context, onSpeechPermissionGranted, onWakePermissionGranted) {
        NovaMicPermissionController(
            requestSpeechAction = {
                if (context.hasRecordAudioPermission()) {
                    onSpeechPermissionGranted()
                } else {
                    speechMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            requestWakeAction = {
                val needMic = !context.hasRecordAudioPermission()
                val needNotif = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !context.hasPostNotificationsPermission()
                if (!needMic && !needNotif) {
                    onWakePermissionGranted()
                } else {
                    val perms = buildList {
                        if (needMic) add(Manifest.permission.RECORD_AUDIO)
                        if (needNotif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                    wakeMultiLauncher.launch(perms)
                }
            }
        )
    }
}

@Composable
fun WriteSettingsPermissionDialog(
    targetBrightnessPercent: Int?,
    onDismiss: () -> Unit,
    onOpenWriteSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Allow Modify System Settings", color = NovaTextPrimary) },
        text = {
            val detail = if (targetBrightnessPercent != null) {
                "To change your screen brightness to $targetBrightnessPercent%, Android requires granting NOVA AI the \"Modify system settings\" permission."
            } else {
                "Android requires the \"Modify system settings\" permission before NOVA AI can adjust your device's screen brightness."
            }
            Text(detail, color = NovaTextSecondary)
        },
        confirmButton = {
            TextButton(onClick = onOpenWriteSettings) {
                Text("Open Settings", color = NovaCyan, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now", color = NovaTextSecondary)
            }
        }
    )
}
