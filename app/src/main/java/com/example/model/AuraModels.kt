package com.example.model

enum class RiskLevel {
    SAFE,
    CONFIRM,
    BLOCK,
    UNAVAILABLE
}

enum class ExecutionMode {
    REAL,
    DEMO
}

enum class ExecutionStatus {
    SUCCESS,
    FAILED,
    UNAVAILABLE
}

data class ToolError(
    val code: String,
    val message: String
)

data class ToolResult(
    val tool: String,
    val mode: ExecutionMode,
    val status: ExecutionStatus,
    val verified: Boolean,
    val data: Map<String, Any?>? = null,
    val message: String = "",
    val error: ToolError? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class VerificationResult(
    val isVerified: Boolean,
    val details: String
)

data class ScreenState(
    val environment: String = "mobile", // browser | mobile | desktop
    val screen: String = "AURA Core Dashboard",
    val url: String = "",
    val visibleElements: List<String> = emptyList(),
    val task: String = "",
    val lastAction: String = "",
    val verification: String = "",
    val status: String = "idle" // planning | running | waiting_confirmation | waiting_user | completed | failed | cancelled
)

enum class TaskStatus {
    PLANNING,
    EXECUTING,
    WAITING_CONFIRMATION,
    WAITING_USER,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class TaskStepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    SKIPPED
}

data class TaskStep(
    val stepId: String,
    val title: String,
    val toolName: String,
    val arguments: Map<String, Any?> = emptyMap(),
    var status: TaskStepStatus = TaskStepStatus.PENDING,
    var result: ToolResult? = null
)

data class PendingConfirmation(
    val toolName: String,
    val target: String,
    val details: String,
    val arguments: Map<String, Any?>,
    val stepIndex: Int
)

data class AuraTask(
    val taskId: String,
    val userRequest: String,
    val languageDetected: String = "Auto (English / Roman Urdu / Urdu / Hindi)",
    val plan: List<TaskStep>,
    var currentStepIndex: Int = 0,
    var status: TaskStatus = TaskStatus.PLANNING,
    val toolCalls: MutableList<ToolResult> = mutableListOf(),
    val errors: MutableList<String> = mutableListOf(),
    var verificationSummary: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    var completedAt: Long? = null,
    var pendingConfirmation: PendingConfirmation? = null
)

enum class MessageSender {
    USER,
    AURA,
    SYSTEM
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val toolResults: List<ToolResult> = emptyList(),
    val task: AuraTask? = null,
    val isVoice: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
