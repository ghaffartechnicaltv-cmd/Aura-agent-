package com.example.tools

import android.content.Intent
import com.example.adapters.AndroidAdapter
import com.example.model.ExecutionMode
import com.example.model.ExecutionStatus
import com.example.model.RiskLevel
import com.example.model.ToolResult
import com.example.model.ValidationResult
import com.example.model.VerificationResult

// --- AURA.OpenSocialApp ---
class OpenSocialAppTool : AuraTool {
    override val name: String = "AURA.OpenSocialApp"
    override val description: String = "Opens a social media application like YouTube, Facebook, or Instagram."
    override val parameters: Map<String, String> = mapOf("platform" to "String: 'YouTube', 'Facebook', 'Instagram'")
    override val permission: String = "device.app_control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val platform = args["platform"]?.toString() ?: "YouTube"
        if (context.isDemoMode) {
            return ToolResult(
                tool = name,
                mode = ExecutionMode.DEMO,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                message = "DEMO ACTION: $platform opened."
            )
        }
        val (success, msg) = AndroidAdapter.openApp(context.context, platform)
        return ToolResult(
            tool = name,
            mode = ExecutionMode.REAL,
            status = if (success) ExecutionStatus.SUCCESS else ExecutionStatus.FAILED,
            verified = success,
            message = msg
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(result.verified, "Social platform opened.")
}

// --- AURA.PreparePost ---
class PreparePostTool : AuraTool {
    override val name: String = "AURA.PreparePost"
    override val description: String = "Prepares and drafts a post for social media publication."
    override val parameters: Map<String, String> = mapOf(
        "platform" to "String: Target platform (Facebook, Twitter/X, Instagram)",
        "content" to "String: Post text content"
    )
    override val permission: String = "none"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult {
        val content = args["content"]?.toString()?.trim()
        if (content.isNullOrEmpty()) return ValidationResult(false, "Argument 'content' is required.")
        return ValidationResult(true)
    }

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val platform = args["platform"]?.toString() ?: "Social Media"
        val content = args["content"]!!.toString().trim()
        return ToolResult(
            tool = name,
            mode = ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            data = mapOf("platform" to platform, "content" to content),
            message = "Post draft ready for $platform: \"$content\""
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Post draft created.")
}

// --- AURA.PublishPost ---
class PublishPostTool : AuraTool {
    override val name: String = "AURA.PublishPost"
    override val description: String = "Publishes a post to social media. Requires explicit user confirmation."
    override val parameters: Map<String, String> = mapOf(
        "platform" to "String: Target platform",
        "content" to "String: Post text to publish"
    )
    override val permission: String = "device.app_control"
    override val riskLevel: RiskLevel = RiskLevel.CONFIRM
    override val requiresConfirmation: Boolean = true

    override suspend fun validate(args: Map<String, Any?>): ValidationResult {
        val content = args["content"]?.toString()?.trim()
        if (content.isNullOrEmpty()) return ValidationResult(false, "Argument 'content' is required.")
        return ValidationResult(true)
    }

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val platform = args["platform"]?.toString() ?: "Social Media"
        val content = args["content"]!!.toString().trim()

        if (context.isDemoMode) {
            return ToolResult(
                tool = name,
                mode = ExecutionMode.DEMO,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                data = mapOf("platform" to platform, "content" to content),
                message = "DEMO ACTION: Post to $platform was simulated. No actual publication took place."
            )
        }

        // Real share intent to social networks
        return try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.context.startActivity(Intent.createChooser(shareIntent, "Publish via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            ToolResult(
                tool = name,
                mode = ExecutionMode.REAL,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                data = mapOf("platform" to platform, "content" to content),
                message = "Post dispatched to Android social sharing framework for $platform."
            )
        } catch (e: Exception) {
            ToolResult(
                tool = name,
                mode = ExecutionMode.REAL,
                status = ExecutionStatus.FAILED,
                verified = false,
                message = "Publish failed: ${e.message}"
            )
        }
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(result.verified, "Publication intent dispatched.")
}
