package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.service.NovaNotificationListenerService
import com.example.ui.components.NovaCosmicBackground
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.NovaPrimaryButton
import com.example.ui.components.NovaThinkingSpinner
import com.example.ui.components.NovaTopBar
import com.example.ui.components.openWriteSystemSettings
import com.example.ui.components.rememberLifecycleResumeTick
import com.example.ui.components.rememberNovaMicPermissionController
import com.example.ui.theme.NovaBorderGlow
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaPurple
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary
import com.example.ui.theme.NovaVoidBlack
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    hasCompletedOnboarding: Boolean,
    onFinished: (goToDashboard: Boolean) -> Unit
) {
    LaunchedEffect(hasCompletedOnboarding) {
        delay(1200L)
        onFinished(hasCompletedOnboarding)
    }

    NovaCosmicBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.nova_app_icon),
                    contentDescription = stringResource(R.string.cd_nova_logo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(124.dp)
                        .clip(CircleShape)
                        .border(2.dp, NovaCyan.copy(alpha = 0.7f), CircleShape)
                )
                Text(
                    text = "NOVA AI",
                    style = MaterialTheme.typography.displayLarge,
                    color = NovaTextPrimary
                )
                Text(
                    text = "FULL ANDROID VOICE ASSISTANT",
                    style = MaterialTheme.typography.labelMedium,
                    color = NovaCyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                NovaThinkingSpinner(size = 42.dp)
                Text(
                    text = "Initializing Neural Core & Voice Engine...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NovaTextSecondary
                )
            }
        }
    }
}

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSkipToDashboard: () -> Unit
) {
    NovaCosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Image(
                painter = painterResource(id = R.drawable.img_welcome_hero),
                contentDescription = "NOVA AI Welcome Visual",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.5.dp, NovaCyan.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
            )

            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Meet NOVA AI",
                style = MaterialTheme.typography.displayMedium,
                color = NovaTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Say \"Hey NOVA\" or \"NOVA\" — she replies \"Yes, I'm listening. How can I help you?\" and assists in Bengali, Hindi, and English.",
                style = MaterialTheme.typography.bodyLarge,
                color = NovaTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))
            WelcomeFeatureRow(
                icon = Icons.Filled.GraphicEq,
                title = "Real Wake Word Detection",
                subtitle = "Foreground microphone listener for \"Hey NOVA\" and \"NOVA\" with visible status."
            )
            Spacer(modifier = Modifier.height(10.dp))
            WelcomeFeatureRow(
                icon = Icons.Filled.Psychology,
                title = "Gemini AI + Offline Engine",
                subtitle = "Context-aware cloud reasoning when online; phone controls & notes offline."
            )
            Spacer(modifier = Modifier.height(10.dp))
            WelcomeFeatureRow(
                icon = Icons.Filled.Language,
                title = "Bengali • Hindi • English",
                subtitle = "Automatic language matching across speech recognition and voice output."
            )
            Spacer(modifier = Modifier.height(10.dp))
            WelcomeFeatureRow(
                icon = Icons.Filled.Lock,
                title = "Privacy & Lock-Screen Safe",
                subtitle = "Explicit confirmations for calls/SMS and strict Android lock-screen rules."
            )

            Spacer(modifier = Modifier.height(24.dp))
            NovaPrimaryButton(
                text = "Configure Permissions",
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_continue_button")
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onSkipToDashboard,
                modifier = Modifier.testTag("welcome_skip_button")
            ) {
                Text("Skip to NOVA Dashboard", color = NovaTextSecondary)
            }
        }
    }
}

@Composable
private fun WelcomeFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = NovaCyan.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, NovaCyan.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = NovaCyan,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = NovaTextSecondary)
            }
        }
    }
}

@Composable
fun PermissionSetupScreen(
    onOpenSettings: (action: String, withPackageUri: Boolean) -> Unit,
    onContinue: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var manualRefreshTick by remember { mutableIntStateOf(0) }
    val resumeTick = rememberLifecycleResumeTick()
    val refreshTick = manualRefreshTick + resumeTick

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        manualRefreshTick++
    }

    val micController = rememberNovaMicPermissionController(
        onSpeechPermissionGranted = { manualRefreshTick++ },
        onWakePermissionGranted = { manualRefreshTick++ }
    )

    val hasMic = remember(refreshTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val hasNotif = remember(refreshTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }
    val hasContacts = remember(refreshTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }
    val hasPhone = remember(refreshTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    }
    val hasSms = remember(refreshTick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
    }
    val hasWriteSettings = remember(refreshTick) { Settings.System.canWrite(context) }
    val hasNotifListener = remember(refreshTick) { NovaNotificationListenerService.isPermissionGranted(context) }

    NovaCosmicBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            NovaTopBar(
                title = "Permission Setup",
                subtitle = "Granted individually with full transparency",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PermissionCard(
                    icon = Icons.Filled.Mic,
                    title = "Microphone (Required for Voice & \"NOVA\" Wake Word)",
                    description = "Used when you tap Speak or enable the visible Background Wake-Word Service.",
                    granted = hasMic,
                    testTag = "perm_mic_btn",
                    onGrant = { micController.requestForWakeService() }
                )

                PermissionCard(
                    icon = Icons.Filled.Notifications,
                    title = "Notifications",
                    description = "Shows the required microphone notification while listening and reminder alerts.",
                    granted = hasNotif,
                    testTag = "perm_notif_btn",
                    onGrant = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            singlePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )

                PermissionCard(
                    icon = Icons.Filled.Contacts,
                    title = "Contacts (Optional)",
                    description = "Lets NOVA find contacts by name before placing calls or drafting messages.",
                    granted = hasContacts,
                    testTag = "perm_contacts_btn",
                    onGrant = { singlePermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }
                )

                PermissionCard(
                    icon = Icons.Filled.Call,
                    title = "Phone Call (Optional)",
                    description = "Initiates calls only after your explicit on-screen confirmation.",
                    granted = hasPhone,
                    testTag = "perm_call_btn",
                    onGrant = { singlePermissionLauncher.launch(Manifest.permission.CALL_PHONE) }
                )

                PermissionCard(
                    icon = Icons.Filled.Sms,
                    title = "Send SMS (Optional)",
                    description = "Sends SMS after confirmation. Without it, NOVA prepares a draft in Messages.",
                    granted = hasSms,
                    testTag = "perm_sms_btn",
                    onGrant = { singlePermissionLauncher.launch(Manifest.permission.SEND_SMS) }
                )

                PermissionCard(
                    icon = Icons.Filled.SettingsBrightness,
                    title = "Modify System Settings (Screen Brightness)",
                    description = "Permits adjusting global screen brightness via \"NOVA\" voice commands or Device Manager.",
                    granted = hasWriteSettings,
                    testTag = "perm_brightness_btn",
                    onGrant = {
                        context.openWriteSystemSettings()
                    }
                )

                PermissionCard(
                    icon = Icons.Filled.NotificationsActive,
                    title = "Notification Access (Optional)",
                    description = "Strictly user-authorized read of non-sensitive notifications (OTPs/passwords excluded).",
                    granted = hasNotifListener,
                    testTag = "perm_notif_listener_btn",
                    onGrant = {
                        onOpenSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS, false)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            Surface(
                color = NovaVoidBlack.copy(alpha = 0.85f),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = { manualRefreshTick++ },
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("Refresh Status", color = NovaCyan)
                    }
                    NovaPrimaryButton(
                        text = "Enter NOVA AI",
                        onClick = onContinue,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("permissions_continue_btn")
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    testTag: String,
    onGrant: () -> Unit
) {
    NovaGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (granted) NovaEmerald.copy(alpha = 0.6f) else NovaBorderGlow
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (granted) NovaEmerald else NovaCyan,
                modifier = Modifier.size(26.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, color = NovaTextSecondary)
            }
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Granted",
                        tint = NovaEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Granted", style = MaterialTheme.typography.labelMedium, color = NovaEmerald)
                }
            } else {
                Button(
                    onClick = onGrant,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag(testTag),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NovaPurple,
                        contentColor = NovaTextPrimary
                    )
                ) {
                    Text("Allow", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
