package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ai.SensitiveActionRequest
import com.example.device.ContactInfo
import com.example.device.DeviceTelemetrySnapshot
import com.example.device.InstalledAppInfo
import com.example.ui.components.NovaCosmicBackground
import com.example.ui.components.NovaGlassCard
import com.example.ui.components.NovaTopBar
import com.example.ui.components.SensitiveActionConfirmCard
import com.example.ui.components.openWriteSystemSettings
import com.example.ui.components.rememberLifecycleResumeTick
import com.example.ui.theme.NovaAmber
import com.example.ui.theme.NovaBorderGlow
import com.example.ui.theme.NovaCoralError
import com.example.ui.theme.NovaCyan
import com.example.ui.theme.NovaElectricBlue
import com.example.ui.theme.NovaEmerald
import com.example.ui.theme.NovaOnAccent
import com.example.ui.theme.NovaOnVibrant
import com.example.ui.theme.NovaPurple
import com.example.ui.theme.NovaSurfaceGlass
import com.example.ui.theme.NovaTextPrimary
import com.example.ui.theme.NovaTextSecondary

@Composable
fun DeviceManagerScreen(
    telemetry: DeviceTelemetrySnapshot,
    mediaVolumePercent: Int,
    brightnessPercent: Int,
    flashlightOn: Boolean,
    installedApps: List<InstalledAppInfo>,
    contacts: List<ContactInfo>,
    pendingSensitiveAction: SensitiveActionRequest?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onToggleFlashlight: () -> Unit,
    onOpenSettings: (String, Boolean) -> Unit,
    onLaunchApp: (String) -> Unit,
    onLoadContacts: () -> Unit,
    onTriggerFindPhone: () -> Unit,
    onStopFindPhone: () -> Unit,
    onExecuteCommand: (String) -> Unit,
    onConfirmSensitive: () -> Unit,
    onCancelSensitive: () -> Unit
) {
    val context = LocalContext.current
    val resumeTick = rememberLifecycleResumeTick()
    val canWriteSettings = remember(resumeTick) { Settings.System.canWrite(context) }
    var phoneTarget by remember { mutableStateOf("") }
    var messageBody by remember { mutableStateOf("") }
    var appFilter by remember { mutableStateOf("") }
    var showApps by remember { mutableStateOf(false) }

    val contactsPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onLoadContacts()
    }

    NovaCosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            NovaTopBar(
                title = "Device Manager",
                subtitle = "${telemetry.manufacturer} ${telemetry.deviceModel} • Android ${telemetry.androidVersion}",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pendingSensitiveAction != null) {
                    SensitiveActionConfirmCard(
                        action = pendingSensitiveAction,
                        onConfirm = onConfirmSensitive,
                        onCancel = onCancelSensitive
                    )
                }

                // Hardware Telemetry Grid (real APIs only)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TelemetryGaugeCard(
                        icon = Icons.Filled.Memory,
                        title = "RAM Usage",
                        primaryValue = "${telemetry.memory.usedRamMb} / ${telemetry.memory.totalRamMb} MB",
                        subValue = "${telemetry.memory.availableRamMb} MB available",
                        fraction = telemetry.memory.usedFraction,
                        accent = NovaCyan,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryGaugeCard(
                        icon = Icons.Filled.Storage,
                        title = "Internal Storage",
                        primaryValue = "%.1f / %.1f GB".format(telemetry.storage.usedGb, telemetry.storage.totalGb),
                        subValue = "%.1f GB free".format(telemetry.storage.availableGb),
                        fraction = telemetry.storage.usedFraction,
                        accent = NovaPurple,
                        modifier = Modifier.weight(1f)
                    )
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DeveloperBoard, contentDescription = null, tint = NovaElectricBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CPU & System", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                        }
                        OutlinedButton(onClick = onRefresh, modifier = Modifier.height(40.dp)) {
                            Text("Refresh", color = NovaCyan)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "CPU: ${telemetry.cpu.coreCount} Cores • ABI: ${telemetry.cpu.architecture} • Hardware: ${telemetry.cpu.hardware}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Wifi, contentDescription = null, tint = NovaEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (telemetry.network.isConnected) {
                                "Network: ${telemetry.network.transportType} (Metered: ${telemetry.network.isMetered})"
                            } else {
                                "Network: Offline"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = NovaTextPrimary
                        )
                    }
                }

                // Native Phone Controls (Volume, Brightness, Flashlight, Settings, Find My Phone)
                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Native Device Controls", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = NovaCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Media Volume: $mediaVolumePercent%", color = NovaTextPrimary, style = MaterialTheme.typography.labelMedium)
                    }
                    Slider(
                        value = mediaVolumePercent.toFloat(),
                        onValueChange = { onVolumeChange(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(thumbColor = NovaCyan, activeTrackColor = NovaCyan)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.BrightnessHigh, contentDescription = null, tint = NovaAmber)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Brightness: $brightnessPercent%", color = NovaTextPrimary, style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = if (canWriteSettings) "System brightness control active" else "Modify System Settings required",
                                    color = if (canWriteSettings) NovaEmerald else NovaAmber,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        if (!canWriteSettings) {
                            OutlinedButton(
                                onClick = { context.openWriteSystemSettings() },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("device_permit_brightness_btn")
                            ) {
                                Text("Allow", color = NovaAmber)
                            }
                        }
                    }
                    Slider(
                        value = brightnessPercent.toFloat(),
                        onValueChange = { onBrightnessChange(it.toInt()) },
                        valueRange = 5f..100f,
                        colors = SliderDefaults.colors(thumbColor = NovaAmber, activeTrackColor = NovaAmber)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onToggleFlashlight,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("device_flashlight_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (flashlightOn) NovaAmber else NovaElectricBlue,
                                contentColor = if (flashlightOn) NovaOnAccent else NovaOnVibrant
                            )
                        ) {
                            Icon(
                                imageVector = if (flashlightOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (flashlightOn) "Torch ON" else "Torch OFF", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onOpenSettings(Settings.ACTION_SETTINGS, false) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = null, tint = NovaCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Settings", color = NovaTextPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onTriggerFindPhone,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("find_phone_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NovaPurple,
                                contentColor = NovaOnVibrant
                            )
                        ) {
                            Icon(Icons.Filled.RingVolume, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Find My Phone")
                        }
                        OutlinedButton(
                            onClick = onStopFindPhone,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Stop Alert", color = NovaTextSecondary)
                        }
                    }
                }

                // Call, SMS & WhatsApp Hub (Always confirmed!)
                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Calls, SMS & WhatsApp", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                        OutlinedButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_CONTACTS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) onLoadContacts() else contactsPermLauncher.launch(Manifest.permission.READ_CONTACTS)
                            },
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(Icons.Filled.Contacts, contentDescription = null, tint = NovaCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Contacts", color = NovaCyan)
                        }
                    }

                    if (contacts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        contacts.take(4).forEach { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { phoneTarget = c.phoneNumber }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(c.name, color = NovaTextPrimary, style = MaterialTheme.typography.bodyMedium)
                                Text(c.phoneNumber, color = NovaCyan, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneTarget,
                        onValueChange = { phoneTarget = it },
                        label = { Text("Contact Name or Phone Number") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaCyan,
                            unfocusedBorderColor = NovaBorderGlow,
                            focusedTextColor = NovaTextPrimary,
                            unfocusedTextColor = NovaTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = messageBody,
                        onValueChange = { messageBody = it },
                        label = { Text("Message (for SMS or WhatsApp)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NovaCyan,
                            unfocusedBorderColor = NovaBorderGlow,
                            focusedTextColor = NovaTextPrimary,
                            unfocusedTextColor = NovaTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { if (phoneTarget.isNotBlank()) onExecuteCommand("call $phoneTarget") },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NovaEmerald, contentColor = NovaOnAccent)
                        ) {
                            Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Call", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                if (phoneTarget.isNotBlank()) {
                                    onExecuteCommand("sms $phoneTarget: ${messageBody.ifBlank { "Hello from NOVA" }}")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NovaElectricBlue, contentColor = NovaOnVibrant)
                        ) {
                            Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SMS")
                        }
                        Button(
                            onClick = {
                                onExecuteCommand("whatsapp ${phoneTarget.ifBlank { "contact" }}: ${messageBody.ifBlank { "Hello!" }}")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NovaPurple, contentColor = NovaOnVibrant)
                        ) {
                            Text("WhatsApp")
                        }
                    }
                }

                // Installed App Launcher
                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showApps = !showApps },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Apps, contentDescription = null, tint = NovaCyan)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("App Launcher (${installedApps.size} apps)", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                        }
                        Text(if (showApps) "Hide" else "Expand", color = NovaCyan, style = MaterialTheme.typography.labelLarge)
                    }
                    if (showApps) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = appFilter,
                            onValueChange = { appFilter = it },
                            placeholder = { Text("Filter apps...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        installedApps
                            .filter { appFilter.isBlank() || it.label.contains(appFilter, ignoreCase = true) }
                            .take(18)
                            .forEach { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onLaunchApp(app.label) }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = NovaTextSecondary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(app.label, color = NovaTextPrimary, modifier = Modifier.weight(1f))
                                    Text("Open", color = NovaCyan, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TelemetryGaugeCard(
    icon: ImageVector,
    title: String,
    primaryValue: String,
    subValue: String,
    fraction: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    NovaGlassCard(modifier = modifier, borderColor = accent.copy(alpha = 0.5f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { fraction },
            color = accent,
            trackColor = NovaSurfaceGlass,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(primaryValue, style = MaterialTheme.typography.labelMedium, color = NovaTextPrimary)
        Text(subValue, style = MaterialTheme.typography.labelSmall, color = NovaTextSecondary)
    }
}

@Composable
fun BatteryDashboardScreen(
    telemetry: DeviceTelemetrySnapshot,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val battery = telemetry.battery
    val levelColor = when {
        battery.percentage >= 50 -> NovaEmerald
        battery.percentage >= 20 -> NovaAmber
        else -> NovaCoralError
    }

    NovaCosmicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            NovaTopBar(
                title = "Battery Dashboard",
                subtitle = "Live Android BatteryManager Telemetry",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val trackColor = NovaBorderGlow.copy(alpha = 0.4f)
                        val arcCyan = NovaCyan
                        Box(
                            modifier = Modifier.size(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val stroke = 14.dp.toPx()
                                drawArc(
                                    color = trackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(listOf(levelColor, arcCyan, levelColor)),
                                    startAngle = -90f,
                                    sweepAngle = (battery.percentage / 100f) * 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (battery.isCharging) Icons.Filled.BatteryChargingFull else Icons.Filled.BatteryStd,
                                    contentDescription = "Battery level",
                                    tint = levelColor,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = "${battery.percentage}%",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = NovaTextPrimary
                                )
                                Text(
                                    text = if (battery.isCharging) "Charging (${battery.chargeSource})" else "Discharging",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = levelColor
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NovaGlassCard(modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Thermostat, contentDescription = "Temperature", tint = NovaAmber)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Temperature", style = MaterialTheme.typography.labelMedium, color = NovaTextSecondary)
                        Text(
                            text = battery.temperatureCelsius?.let { "%.1f °C".format(it) } ?: "Unavailable",
                            style = MaterialTheme.typography.titleLarge,
                            color = NovaTextPrimary
                        )
                    }
                    NovaGlassCard(modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.BatteryChargingFull, contentDescription = "Voltage", tint = NovaCyan)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Voltage", style = MaterialTheme.typography.labelMedium, color = NovaTextSecondary)
                        Text(
                            text = battery.voltageMv?.let { "$it mV" } ?: "Unavailable",
                            style = MaterialTheme.typography.titleLarge,
                            color = NovaTextPrimary
                        )
                    }
                }

                NovaGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Battery Health & Chemistry", style = MaterialTheme.typography.titleMedium, color = NovaTextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Health Status: ${battery.healthLabel}", style = MaterialTheme.typography.bodyMedium, color = NovaEmerald)
                    Text("Technology: ${battery.technology}", style = MaterialTheme.typography.bodyMedium, color = NovaTextSecondary)
                    Text("Power Source: ${battery.chargeSource}", style = MaterialTheme.typography.bodyMedium, color = NovaTextSecondary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Refresh Metrics", color = NovaCyan)
                    }
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NovaElectricBlue)
                    ) {
                        Text("Battery Saver")
                    }
                }
            }
        }
    }
}
