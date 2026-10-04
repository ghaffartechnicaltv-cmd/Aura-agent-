package com.example.security

import com.example.model.PendingConfirmation
import com.example.model.RiskLevel
import com.example.model.ToolError
import com.example.model.ToolResult
import com.example.permissions.AuraPermissionManager
import com.example.tools.AuraTool
import com.example.tools.ToolExecutionContext

sealed class ActionGuardDecision {
    object SafeToExecute : ActionGuardDecision()
    data class RequiresConfirmation(val pendingConfirmation: PendingConfirmation) : ActionGuardDecision()
    data class Blocked(val reason: String) : ActionGuardDecision()
    data class Unavailable(val reason: String) : ActionGuardDecision()
}

class AuraActionGuard(private val permissionManager: AuraPermissionManager) {

    private val BLOCKED_KEYWORDS = listOf(
        "bypass 2fa", "bypass two-factor", "bypass captcha", "crack password",
        "steal credentials", "exfiltrate private key", "disable security"
    )

    fun evaluate(
        tool: AuraTool,
        args: Map<String, Any?>,
        stepIndex: Int,
        userExplicitlyConfirmed: Boolean = false
    ): ActionGuardDecision {
        // 1. Check for inherently blocked security violations
        val argsString = args.values.joinToString(" ") { it?.toString() ?: "" }.lowercase()
        if (BLOCKED_KEYWORDS.any { argsString.contains(it) } || PromptInjectionGuard.isMaliciousGoal(argsString)) {
            return ActionGuardDecision.Blocked(
                "Action blocked by AURA Action Guard: Violates platform security and safety policy."
            )
        }

        // 2. Check if tool is explicitly BLOCK level
        if (tool.riskLevel == RiskLevel.BLOCK) {
            return ActionGuardDecision.Blocked("Tool '${tool.name}' is blocked by security policy.")
        }

        // 3. Check permission
        if (!permissionManager.hasPermission(tool.permission)) {
            return ActionGuardDecision.Unavailable(
                "Required permission '${tool.permission}' is unavailable or denied by user."
            )
        }

        // 4. Check if confirmation is required
        if (tool.riskLevel == RiskLevel.CONFIRM || tool.requiresConfirmation) {
            if (!userExplicitlyConfirmed) {
                val target = when (tool.name) {
                    "AURA.Call", "AURA.InitiateCall" -> args["contactName"]?.toString() ?: args["phoneNumber"]?.toString() ?: "Phone call"
                    "AURA.SendMessage" -> args["recipient"]?.toString() ?: "Message recipient"
                    "AURA.PublishPost" -> args["platform"]?.toString() ?: "Social Media Account"
                    "AURA.CancelAlarm" -> args["alarmId"]?.toString() ?: "Device Alarm"
                    else -> tool.name
                }
                val details = when (tool.name) {
                    "AURA.Call", "AURA.InitiateCall" -> "AURA will initiate a real phone call to $target."
                    "AURA.SendMessage" -> "AURA will send the following message to $target: \"${args["message"]}\""
                    "AURA.PublishPost" -> "AURA will publish this post to $target: \"${args["content"]}\""
                    "AURA.CancelAlarm" -> "AURA will cancel the requested alarm on this device."
                    else -> "Consequential action requires your explicit confirmation."
                }
                return ActionGuardDecision.RequiresConfirmation(
                    PendingConfirmation(
                        toolName = tool.name,
                        target = target,
                        details = details,
                        arguments = args,
                        stepIndex = stepIndex
                    )
                )
            }
        }

        return ActionGuardDecision.SafeToExecute
    }
}
