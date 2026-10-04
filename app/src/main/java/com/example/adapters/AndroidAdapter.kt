package com.example.adapters

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ContactMatch(
    val name: String,
    val phoneNumber: String
)

data class DeviceStatusInfo(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val networkType: String,
    val isOnline: Boolean,
    val osVersion: String,
    val deviceModel: String
)

object AndroidAdapter {

    fun openApp(context: Context, target: String): Pair<Boolean, String> {
        val pm = context.packageManager
        val query = target.lowercase().trim()

        val knownPackages = mapOf(
            "youtube" to "com.google.android.youtube",
            "google" to "com.google.android.googlequicksearchbox",
            "chrome" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "whatsapp" to "com.whatsapp",
            "settings" to "com.android.settings",
            "camera" to "com.android.camera",
            "clock" to "com.google.android.deskclock",
            "calculator" to "com.google.android.calculator"
        )

        val targetPkg = knownPackages[query] ?: query

        // Try getting launch intent
        val launchIntent = pm.getLaunchIntentForPackage(targetPkg)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return Pair(true, "Application '$target' launched successfully.")
        }

        // Try matching installed apps by display label
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in installedApps) {
                val label = pm.getApplicationLabel(app).toString().lowercase()
                if (label.contains(query) || app.packageName.lowercase().contains(query)) {
                    val intent = pm.getLaunchIntentForPackage(app.packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        return Pair(true, "Application '${pm.getApplicationLabel(app)}' launched.")
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore reflection / query limits
        }

        // Check fallback for browser / search
        if (query.contains("google")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return Pair(true, "Google opened in browser.")
        } else if (query.contains("youtube")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return Pair(true, "YouTube opened in browser.")
        }

        return Pair(false, "App '$target' is not installed or cannot be opened on this device.")
    }

    fun findContacts(context: Context, nameQuery: String): List<ContactMatch> {
        val matches = mutableListOf<ContactMatch>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$nameQuery%")

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )
            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = if (nameIdx >= 0) it.getString(nameIdx) else "Unknown"
                    val number = if (numIdx >= 0) it.getString(numIdx) else ""
                    matches.add(ContactMatch(name, number))
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
            return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }

        return matches
    }

    fun makeCall(context: Context, phoneNumber: String, hasCallPermission: Boolean): Pair<Boolean, String> {
        return try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            val intent = if (hasCallPermission) {
                Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
            Pair(true, "Call initiated to $cleanNumber")
        } catch (e: Exception) {
            Pair(false, "Failed to initiate call: ${e.message}")
        }
    }

    fun setAlarm(
        context: Context,
        hour: Int,
        minute: Int,
        message: String,
        skipUi: Boolean = false
    ): Pair<Boolean, String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, skipUi)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeFormatted = String.format(Locale.US, "%02d:%02d", hour, minute)
            Pair(true, "Alarm set for $timeFormatted ($message)")
        } catch (e: Exception) {
            Pair(false, "Could not set alarm via Android AlarmClock: ${e.message}")
        }
    }

    fun openSettings(context: Context, settingType: String): Pair<Boolean, String> {
        return try {
            val action = when (settingType.lowercase()) {
                "wifi" -> Settings.ACTION_WIFI_SETTINGS
                "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
                "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
                "display" -> Settings.ACTION_DISPLAY_SETTINGS
                "apps" -> Settings.ACTION_APPLICATION_SETTINGS
                "date", "time" -> Settings.ACTION_DATE_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Pair(true, "Settings opened ($settingType).")
        } catch (e: Exception) {
            Pair(false, "Unable to open settings: ${e.message}")
        }
    }

    fun getDeviceTime(): Map<String, Any> {
        val now = Date()
        val tz = TimeZone.getDefault()
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        return mapOf(
            "time" to timeFormat.format(now),
            "date" to dateFormat.format(now),
            "timezone" to tz.id,
            "raw_timestamp" to now.time
        )
    }

    fun getDeviceStatus(context: Context): DeviceStatusInfo {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val isCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val status = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } else false

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        var netType = "None"
        var isOnline = false

        if (cm != null) {
            val activeNet = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNet)
            if (caps != null) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    netType = "Wi-Fi"
                    isOnline = true
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    netType = "Cellular Mobile Data"
                    isOnline = true
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    netType = "Ethernet"
                    isOnline = true
                }
            }
        }

        return DeviceStatusInfo(
            batteryLevel = batteryPct,
            isCharging = isCharging,
            networkType = netType,
            isOnline = isOnline,
            osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        )
    }
}
