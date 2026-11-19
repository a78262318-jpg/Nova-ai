package com.example.device

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.io.File

data class BatteryTelemetry(
    val percentage: Int,
    val isCharging: Boolean,
    val chargeSource: String,
    val temperatureCelsius: Float?,
    val voltageMv: Int?,
    val healthLabel: String,
    val technology: String
)

data class MemoryTelemetry(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val usedRamMb: Long
) {
    val usedFraction: Float
        get() = if (totalRamMb > 0) (usedRamMb.toFloat() / totalRamMb.toFloat()).coerceIn(0f, 1f) else 0f
}

data class StorageTelemetry(
    val totalGb: Float,
    val availableGb: Float,
    val usedGb: Float
) {
    val usedFraction: Float
        get() = if (totalGb > 0f) (usedGb / totalGb).coerceIn(0f, 1f) else 0f
}

data class CpuTelemetry(
    val coreCount: Int,
    val architecture: String,
    val hardware: String,
    val board: String
)

data class NetworkTelemetry(
    val isConnected: Boolean,
    val transportType: String,
    val isValidated: Boolean,
    val isMetered: Boolean
)

data class DeviceTelemetrySnapshot(
    val battery: BatteryTelemetry,
    val memory: MemoryTelemetry,
    val storage: StorageTelemetry,
    val cpu: CpuTelemetry,
    val network: NetworkTelemetry,
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val sdkLevel: Int,
    val isDeviceLocked: Boolean
)

class DeviceStatsProvider(private val context: Context) {

    fun getSnapshot(): DeviceTelemetrySnapshot {
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        return DeviceTelemetrySnapshot(
            battery = getBatteryTelemetry(),
            memory = getMemoryTelemetry(),
            storage = getStorageTelemetry(),
            cpu = getCpuTelemetry(),
            network = getNetworkTelemetry(),
            deviceModel = Build.MODEL ?: "Android Device",
            manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Unknown",
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkLevel = Build.VERSION.SDK_INT,
            isDeviceLocked = keyguard?.isKeyguardLocked == true
        )
    }

    fun getBatteryTelemetry(): BatteryTelemetry {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 0

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val source = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Adapter"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            BatteryManager.BATTERY_PLUGGED_DOCK -> "Dock"
            else -> if (isCharging) "Charging" else "Battery"
        }

        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val tempC = if (tempTenths != null && tempTenths != Int.MIN_VALUE && tempTenths > -1000) {
            tempTenths / 10f
        } else null

        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)?.takeIf { it > 0 }

        val healthInt = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val health = when (healthInt) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        val tech = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY).orEmpty().ifBlank { "Li-ion" }

        return BatteryTelemetry(
            percentage = pct,
            isCharging = isCharging,
            chargeSource = source,
            temperatureCelsius = tempC,
            voltageMv = voltage,
            healthLabel = health,
            technology = tech
        )
    }

    fun getMemoryTelemetry(): MemoryTelemetry {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val info = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(info)
        val totalMb = info.totalMem / (1024L * 1024L)
        val availMb = info.availMem / (1024L * 1024L)
        val usedMb = (totalMb - availMb).coerceAtLeast(0L)
        return MemoryTelemetry(
            totalRamMb = totalMb,
            availableRamMb = availMb,
            usedRamMb = usedMb
        )
    }

    fun getStorageTelemetry(): StorageTelemetry {
        val dataDir: File = Environment.getDataDirectory()
        val stat = StatFs(dataDir.path)
        val totalBytes = stat.totalBytes.toDouble()
        val availBytes = stat.availableBytes.toDouble()
        val gb = 1024.0 * 1024.0 * 1024.0
        val totalGb = (totalBytes / gb).toFloat()
        val availGb = (availBytes / gb).toFloat()
        val usedGb = (totalGb - availGb).coerceAtLeast(0f)
        return StorageTelemetry(totalGb = totalGb, availableGb = availGb, usedGb = usedGb)
    }

    fun getCpuTelemetry(): CpuTelemetry {
        val cores = Runtime.getRuntime().availableProcessors()
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"
        return CpuTelemetry(
            coreCount = cores,
            architecture = abi,
            hardware = Build.HARDWARE ?: "Unknown",
            board = Build.BOARD ?: "Unknown"
        )
    }

    fun getNetworkTelemetry(): NetworkTelemetry {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkTelemetry(false, "Offline", isValidated = false, isMetered = false)
        val activeNetwork = cm.activeNetwork
            ?: return NetworkTelemetry(false, "Offline", isValidated = false, isMetered = false)
        val caps = cm.getNetworkCapabilities(activeNetwork)
            ?: return NetworkTelemetry(false, "Offline", isValidated = false, isMetered = false)

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val transport = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> if (hasInternet) "Connected" else "Offline"
        }
        return NetworkTelemetry(
            isConnected = hasInternet,
            transportType = transport,
            isValidated = validated,
            isMetered = cm.isActiveNetworkMetered
        )
    }
}
