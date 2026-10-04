package com.example.tools

import android.content.Intent
import android.net.Uri
import com.example.model.ExecutionMode
import com.example.model.ExecutionStatus
import com.example.model.RiskLevel
import com.example.model.ToolError
import com.example.model.ToolResult
import com.example.model.ValidationResult
import com.example.model.VerificationResult

// --- AURA.PrepareMessage ---
class PrepareMessageTool : AuraTool {
    override val name: String = "AURA.PrepareMessage"
    override val description: String = "Drafts a message text for a recipient for user review."
    override val parameters: Map<String, String> = mapOf(
        "recipient" to "String: Target contact or number",
        "message" to "String: Proposed message text"
    )
    override val permission: String = "none"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult {
        val msg = args["message"]?.toString()?.trim()
        if (msg.isNullOrEmpty()) return ValidationResult(false, "Argument 'message' is required.")
        return ValidationResult(true)
    }

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val recipient = args["recipient"]?.toString() ?: "Recipient"
        val message = args["message"]!!.toString().trim()
        return ToolResult(
            tool = name,
            mode = ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            data = mapOf("recipient" to recipient, "message" to message),
            message = "Draft prepared for $recipient: \"$message\""
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Draft created.")
}

// --- AURA.SendMessage ---
class SendMessageTool : AuraTool {
    override val name: String = "AURA.SendMessage"
    override val description: String = "Sends an SMS message to a recipient. Requires user confirmation."
    override val parameters: Map<String, String> = mapOf(
        "recipient" to "String: Phone number or contact name",
        "message" to "String: Text message content to send"
    )
    override val permission: String = "device.call"
    override val riskLevel: RiskLevel = RiskLevel.CONFIRM
    override val requiresConfirmation: Boolean = true

    override suspend fun validate(args: Map<String, Any?>): ValidationResult {
        val msg = args["message"]?.toString()?.trim()
        if (msg.isNullOrEmpty()) return ValidationResult(false, "Argument 'message' is required.")
        return ValidationResult(true)
    }

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val recipient = args["recipient"]?.toString() ?: ""
        val message = args["message"]!!.toString().trim()

        if (context.isDemoMode) {
            return ToolResult(
                tool = name,
                mode = ExecutionMode.DEMO,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                data = mapOf("recipient" to recipient, "message" to message),
                message = "DEMO ACTION: Message to $recipient was simulated. No actual SMS dispatched."
            )
        }

        return try {
            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$recipient")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.context.startActivity(smsIntent)
            ToolResult(
                tool = name,
                mode = ExecutionMode.REAL,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                data = mapOf("recipient" to recipient, "message" to message),
                message = "Message intent successfully dispatched to Android SMS provider for $recipient."
            )
        } catch (e: Exception) {
            ToolResult(
                tool = name,
                mode = ExecutionMode.REAL,
                status = ExecutionStatus.FAILED,
                verified = false,
                message = "Failed to launch SMS dispatcher: ${e.message}",
                error = ToolError("SMS_DISPATCH_FAILED", e.message ?: "Unknown error")
            )
        }
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(result.verified, "SMS dispatcher confirmed.")
}

// --- AURA.InitiateCall ---
class InitiateCallTool : AuraTool {
    override val name: String = "AURA.InitiateCall"
    override val description: String = "Initiates an outbound phone call after confirmation."
    override val parameters: Map<String, String> = mapOf(
        "phoneNumber" to "String: Target phone number",
        "contactName" to "String: Optional contact name"
    )
    override val permission: String = "device.call"
    override val riskLevel: RiskLevel = RiskLevel.CONFIRM
    override val requiresConfirmation: Boolean = true

    private val delegate = CallTool()

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = delegate.validate(args)
    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult = delegate.execute(args, context)
    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult = delegate.verify(result, context)
}
