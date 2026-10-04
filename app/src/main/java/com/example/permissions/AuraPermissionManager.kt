package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuraPermissionManager(private val context: Context) {

    // Internal tracked virtual permissions and user grants
    private val _virtualPermissions = MutableStateFlow(
        mapOf(
            "device.app_control" to true,
            "browser.control" to true,
            "computer.use" to true,
            "storage" to true,
            "location" to false
        )
    )
    val virtualPermissions: StateFlow<Map<String, Boolean>> = _virtualPermissions.asStateFlow()

    fun hasPermission(permissionKey: String): Boolean {
        return when (permissionKey) {
            "device.microphone", Manifest.permission.RECORD_AUDIO -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            }
            "device.contacts", Manifest.permission.READ_CONTACTS -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            }
            "device.call", Manifest.permission.CALL_PHONE -> {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            }
            "device.notifications", Manifest.permission.POST_NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                } else {
                    true
                }
            }
            "device.alarm" -> true
            "device.app_control" -> _virtualPermissions.value["device.app_control"] ?: true
            "browser.control" -> _virtualPermissions.value["browser.control"] ?: true
            "computer.use" -> _virtualPermissions.value["computer.use"] ?: true
            "none", "" -> true
            else -> _virtualPermissions.value[permissionKey] ?: true
        }
    }

    fun setVirtualPermission(key: String, granted: Boolean) {
        val current = _virtualPermissions.value.toMutableMap()
        current[key] = granted
        _virtualPermissions.value = current
    }

    fun getRequiredAndroidPermission(toolPermission: String): String? {
        return when (toolPermission) {
            "device.microphone" -> Manifest.permission.RECORD_AUDIO
            "device.contacts" -> Manifest.permission.READ_CONTACTS
            "device.call" -> Manifest.permission.CALL_PHONE
            "device.notifications" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.POST_NOTIFICATIONS
                } else null
            }
            else -> null
        }
    }
}
