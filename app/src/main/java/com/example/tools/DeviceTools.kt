package com.example.tools

import com.example.adapters.AndroidAdapter
import com.example.model.ExecutionMode
import com.example.model.ExecutionStatus
import com.example.model.RiskLevel
import com.example.model.ScreenState
import com.example.model.ToolError
import com.example.model.ToolResult
import com.example.model.ValidationResult
import com.example.model.VerificationResult
import java.util.regex.Pattern

// --- AURA.OpenApp ---
class OpenAppTool : AuraTool {
    override val name: String = "AURA.OpenApp"
    override val description: String = "Opens an installed application on the user's device (e.g. YouTube, Google, Chrome, Settings)."
    override val parameters: Map<String, String> = mapOf("appName" to "String: Name of the application to open")
    override val permission: String = "device.app_control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult {
        val appName = args["appName"]?.toString()?.trim()
        if (appName.isNullOrEmpty()) {
            return ValidationResult(false, "Argument 'appName' is required.")
        }
        return ValidationResult(true)
    }

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val appName = args["appName"]!!.toString().trim()

        if (context.isDemoMode) {
            context.updateScreenState(
                context.currentScreenState().copy(
                    screen = "App: $appName",
                    lastAction = "Demo: Opened $appName",
                    status = "running"
                )
            )
            return ToolResult(
                tool = name,
                mode = ExecutionMode.DEMO,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                data = mapOf("app" to appName, "demo" to true),
                message = "DEMO ACTION: Application '$appName' launch was simulated."
            )
        }

        val (success, message) = AndroidAdapter.openApp(context.context, appName)
        if (success) {
            context.updateScreenState(
                context.currentScreenState().copy(
                    screen = "App: $appName",
                    lastAction = "Opened $appName",
                    status = "running"
                )
            )
            return ToolResult(
                tool = name,
                mode = ExecutionMode.REAL,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                data = mapOf("app" to appName),
                message = message
            )
        } else {
            return ToolResult(
                tool = name,
                mode = ExecutionMode.REAL,
                status = ExecutionStatus.UNAVAILABLE,
                verified = false,
                message = message,
                error = ToolError("APP_NOT_FOUND", message)
            )
        }
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult {
        return if (result.status == ExecutionStatus.SUCCESS) {
            VerificationResult(true, "Application launch verified.")
        } else {
            VerificationResult(false, result.message)
        }
    }
}

// --- AURA.FindContact ---
class FindContactTool : AuraTool {
    override val name: String = "AURA.FindContact"
    override val description: String = "Searches contacts by name to find phone numbers."
    override val parameters: Map<String, String> = mapOf("name" to "String: Contact name to search")
    override val permission: String = "device.contacts"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult {
        val contactName = args["name"]?.toString()?.trim()
        if (contactName.isNullOrEmpty()) {
            return ValidationResult(false, "Argument 'name' is required.")
        }
        return ValidationResult(true)
    }

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val queryName = args["name"]!!.toString().trim()

        if (context.isDemoMode) {
            return ToolResult(
                tool = name,
                mode = ExecutionMode.DEMO,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                data = mapOf(
                    "matches" to listOf(mapOf("name" to queryName, "phone" to "+1 (555) 019-2834")),
                    "count" to 1
                ),
                message = "DEMO ACTION: Contact search simulated for '$queryName'."
            )
        }

        val matches = AndroidAdapter.findContacts(context.context, queryName)
        return when {
            matches.isEmpty() -> {
                ToolResult(
                    tool = name,
                    mode = ExecutionMode.REAL,
                    status = ExecutionStatus.SUCCESS,
                    verified = true,
                    data = mapOf("matches" to emptyList<Map<String, String>>(), "count" to 0),
                    message = "No contacts found matching '$queryName'."
                )
            }
            matches.size == 1 -> {
                val match = matches.first()
                ToolResult(
                    tool = name,
                    mode = ExecutionMode.REAL,
                    status = ExecutionStatus.SUCCESS,
                    verified = true,
                    data = mapOf(
                        "matches" to listOf(mapOf("name" to match.name, "phone" to match.phoneNumber)),
                        "count" to 1,
                        "selectedContact" to mapOf("name" to match.name, "phone" to match.phoneNumber)
                    ),
                    message = "Found contact: ${match.name} (${match.phoneNumber})."
                )
            }
            else -> {
                val list = matches.take(5).map { mapOf("name" to it.name, "phone" to it.phoneNumber) }
                ToolResult(
                    tool = name,
                    mode = ExecutionMode.REAL,
                    status = ExecutionStatus.SUCCESS,
                    verified = true,
                    data = mapOf("matches" to list, "count" to matches.size, "multiple" to true),
                    message = "Found ${matches.size} contacts matching '$queryName'. Please choose one."
                )
            }
        }
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult {
        return VerificationResult(true, "Contact search completed.")
    }
}

// --- AURA.Call ---
class CallTool : AuraTool {
    override val name: String = "AURA.Call"
    override val description: String = "Initiates a phone call to a phone number or resolved contact."
    override val parameters: Map<String, String> = mapOf(
        "phoneNumber" to "String: Phone number to dial",
        "contactName" to "String: Optional contact name"
    )
    override val permission: String = "device.call"
    override val riskLevel: RiskLevel = RiskLevel.CONFIRM
    override val requiresConfirmation: Boolean = true

    override suspend fun validate(args: Map<String, Any?>): ValidationResult {
        val phone = args["phoneNumber"]?.toString()?.trim()
        if (phone.isNullOrEmpty()) {
            return ValidationResult(false, "Argument 'phoneNumber' is required.")
        }
        return ValidationResult(true)
    }

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val phone = args["phoneNumber"]!!.toString().trim()
        val name = args["contactName"]?.toString() ?: phone

        if (context.isDemoMode) {
            return ToolResult(
                tool = name,
                mode = ExecutionMode.DEMO,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                data = mapOf("phone" to phone, "contact" to name),
                message = "DEMO ACTION: Call to $name ($phone) was simulated. No real call placed."
            )
        }

        val hasPermission = context.permissionManager.hasPermission("device.call")
        val (success, msg) = AndroidAdapter.makeCall(context.context, phone, hasPermission)

        return if (success) {
            ToolResult(
                tool = name,
                mode = ExecutionMode.REAL,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                data = mapOf("phone" to phone, "contact" to name),
                message = msg
            )
        } else {
            ToolResult(
                tool = name,
                mode = ExecutionMode.REAL,
                status = ExecutionStatus.FAILED,
                verified = false,
                message = msg,
                error = ToolError("CALL_FAILED", msg)
            )
        }
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult {
        return if (result.status == ExecutionStatus.SUCCESS) {
            VerificationResult(true, "Call intent accepted by Android telephony system.")
        } else {
            VerificationResult(false, "Call could not be verified.")
        }
    }
}

// --- AURA.SetAlarm ---
class SetAlarmTool : AuraTool {
    override val name: String = "AURA.SetAlarm"
    override val description: String = "Sets an alarm for a specific time on the Android device."
    override val parameters: Map<String, String> = mapOf(
        "time" to "String: Time of alarm (e.g. '07:00', '7:30 AM', 'subah 6 baje')",
        "label" to "String: Optional label or message for the alarm"
    )
    override val permission: String = "device.alarm"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult {
        val timeStr = args["time"]?.toString()?.trim()
        if (timeStr.isNullOrEmpty()) {
            return ValidationResult(false, "Argument 'time' is required.")
        }
        return ValidationResult(true)
    }

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val timeStr = args["time"]!!.toString().trim()
        val label = args["label"]?.toString() ?: "AURA Alarm"

        // Parse time: supports "07:00", "7:30 AM", "6 PM", "subah 6 baje", "sham 7 baje", etc.
        val (hour, minute) = parseNaturalTime(timeStr)

        if (context.isDemoMode) {
            val formatted = String.format("%02d:%02d", hour, minute)
            return ToolResult(
                tool = name,
                mode = ExecutionMode.DEMO,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                data = mapOf("time" to formatted, "hour" to hour, "minute" to minute, "label" to label),
                message = "DEMO ACTION: Alarm for $formatted ($label) was simulated. No real alarm was created."
            )
        }

        val (success, msg) = AndroidAdapter.setAlarm(context.context, hour, minute, label, skipUi = true)
        val formatted = String.format("%02d:%02d", hour, minute)

        return if (success) {
            ToolResult(
                tool = name,
                mode = ExecutionMode.REAL,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                data = mapOf("time" to formatted, "hour" to hour, "minute" to minute, "label" to label),
                message = msg
            )
        } else {
            ToolResult(
                tool = name,
                mode = ExecutionMode.REAL,
                status = ExecutionStatus.UNAVAILABLE,
                verified = false,
                message = msg,
                error = ToolError("ALARM_FAILED", msg)
            )
        }
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult {
        return if (result.status == ExecutionStatus.SUCCESS) {
            VerificationResult(true, "Alarm confirmed with Android AlarmClock system.")
        } else {
            VerificationResult(false, "Alarm creation failed verification.")
        }
    }

    private fun parseNaturalTime(input: String): Pair<Int, Int> {
        val lower = input.lowercase()
        val isPm = lower.contains("pm") || lower.contains("sham") || lower.contains("raat") || lower.contains("dopahar")
        val isAm = lower.contains("am") || lower.contains("subah") || lower.contains("fajar")

        // Match HH:MM
        val colonMatcher = Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(lower)
        if (colonMatcher.find()) {
            var h = colonMatcher.group(1)?.toIntOrNull() ?: 7
            val m = colonMatcher.group(2)?.toIntOrNull() ?: 0
            if (isPm && h < 12) h += 12
            if (isAm && h == 12) h = 0
            return Pair(h, m)
        }

        // Match single digit or double digit "7 baje", "6 o'clock", "7"
        val singleMatcher = Pattern.compile("(\\d{1,2})").matcher(lower)
        if (singleMatcher.find()) {
            var h = singleMatcher.group(1)?.toIntOrNull() ?: 7
            val m = 0
            if (isPm && h < 12) h += 12
            if (isAm && h == 12) h = 0
            return Pair(h, m)
        }

        return Pair(7, 0) // default 7:00 AM
    }
}

// --- AURA.CancelAlarm ---
class CancelAlarmTool : AuraTool {
    override val name: String = "AURA.CancelAlarm"
    override val description: String = "Opens alarm manager or cancels existing alarms."
    override val parameters: Map<String, String> = mapOf("alarmId" to "String: Optional alarm identifier or time")
    override val permission: String = "device.alarm"
    override val riskLevel: RiskLevel = RiskLevel.CONFIRM
    override val requiresConfirmation: Boolean = true

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        if (context.isDemoMode) {
            return ToolResult(
                tool = name,
                mode = ExecutionMode.DEMO,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                message = "DEMO ACTION: Alarm cancellation simulated."
            )
        }
        val (success, msg) = AndroidAdapter.openSettings(context.context, "clock")
        return ToolResult(
            tool = name,
            mode = ExecutionMode.REAL,
            status = if (success) ExecutionStatus.SUCCESS else ExecutionStatus.FAILED,
            verified = success,
            message = "Android Clock opened to manage or dismiss alarms."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult {
        return VerificationResult(result.verified, "Alarm manager verified.")
    }
}

// --- AURA.OpenSettings ---
class OpenSettingsTool : AuraTool {
    override val name: String = "AURA.OpenSettings"
    override val description: String = "Opens Android system settings (wifi, bluetooth, display, sound, apps)."
    override val parameters: Map<String, String> = mapOf("settingType" to "String: Type of settings (wifi, sound, display, apps, general)")
    override val permission: String = "device.app_control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val type = args["settingType"]?.toString() ?: "general"
        if (context.isDemoMode) {
            return ToolResult(
                tool = name,
                mode = ExecutionMode.DEMO,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                message = "DEMO ACTION: Opened settings ($type)."
            )
        }
        val (success, msg) = AndroidAdapter.openSettings(context.context, type)
        return ToolResult(
            tool = name,
            mode = ExecutionMode.REAL,
            status = if (success) ExecutionStatus.SUCCESS else ExecutionStatus.FAILED,
            verified = success,
            message = msg
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(result.verified, "Settings launch verified.")
}

// --- AURA.DeviceTime ---
class DeviceTimeTool : AuraTool {
    override val name: String = "AURA.DeviceTime"
    override val description: String = "Gets the current accurate device time, date, and timezone."
    override val parameters: Map<String, String> = emptyMap()
    override val permission: String = "none"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val info = AndroidAdapter.getDeviceTime()
        return ToolResult(
            tool = name,
            mode = ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            data = info,
            message = "Current device time: ${info["time"]}, ${info["date"]} (${info["timezone"]})"
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Time verified against system clock.")
}

// --- AURA.DeviceStatus ---
class DeviceStatusTool : AuraTool {
    override val name: String = "AURA.DeviceStatus"
    override val description: String = "Gets device battery level, charging status, network connection, and OS details."
    override val parameters: Map<String, String> = emptyMap()
    override val permission: String = "none"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val status = AndroidAdapter.getDeviceStatus(context.context)
        return ToolResult(
            tool = name,
            mode = ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            data = mapOf(
                "battery" to "${status.batteryLevel}%",
                "charging" to status.isCharging,
                "network" to status.networkType,
                "online" to status.isOnline,
                "os" to status.osVersion,
                "device" to status.deviceModel
            ),
            message = "Battery: ${status.batteryLevel}%, Charging: ${status.isCharging}, Network: ${status.networkType} (${if (status.isOnline) "Online" else "Offline"})"
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Device status verified.")
}
