package com.example.agent

import android.content.Context
import com.example.gemini.GeminiService
import com.example.memory.AuraMemory
import com.example.model.ChatMessage
import com.example.model.ExecutionMode
import com.example.model.ExecutionStatus
import com.example.model.MessageSender
import com.example.model.PendingConfirmation
import com.example.model.ScreenState
import com.example.model.TaskStatus
import com.example.model.TaskStep
import com.example.model.TaskStepStatus
import com.example.model.ToolError
import com.example.model.ToolResult
import com.example.permissions.AuraPermissionManager
import com.example.security.ActionGuardDecision
import com.example.security.AuraActionGuard
import com.example.tools.AuraToolRouter
import com.example.tools.ToolExecutionContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuraCore(
    private val context: Context,
    val toolRouter: AuraToolRouter,
    val actionGuard: AuraActionGuard,
    val taskManager: AuraTaskManager,
    val memory: AuraMemory,
    val geminiService: GeminiService,
    val permissionManager: AuraPermissionManager
) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var activeExecutionJob: Job? = null

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    fun setDemoMode(enabled: Boolean) {
        _isDemoMode.value = enabled
    }

    fun submitRequest(userPrompt: String, isVoice: Boolean = false, onSpeechReply: ((String) -> Unit)? = null) {
        if (userPrompt.isBlank()) return

        // 1. Record User Message
        val userMsg = ChatMessage(
            sender = MessageSender.USER,
            text = userPrompt,
            isVoice = isVoice
        )
        memory.addChatMessage(userMsg)

        // 2. Start Core Pipeline
        activeExecutionJob?.cancel()
        activeExecutionJob = scope.launch {
            _isProcessing.value = true
            executePipeline(userPrompt, onSpeechReply)
            _isProcessing.value = false
        }
    }

    private suspend fun executePipeline(userPrompt: String, onSpeechReply: ((String) -> Unit)?) {
        // --- 1. Intent Understanding & Task Planning ---
        val historyTurns = memory.conversationHistory.value
            .takeLast(6)
            .map { Pair(if (it.sender == MessageSender.USER) "user" else "model", it.text) }

        val (reasoning, planSteps) = geminiService.planAndReason(userPrompt, historyTurns)

        if (planSteps.isNullOrEmpty()) {
            // Conversational direct reply
            val replyMsg = ChatMessage(
                sender = MessageSender.AURA,
                text = reasoning.ifEmpty { "I understand, but no specific action was required." }
            )
            memory.addChatMessage(replyMsg)
            onSpeechReply?.invoke(replyMsg.text)
            return
        }

        // --- 2. Create Active Task ---
        val task = taskManager.createTask(
            userRequest = userPrompt,
            language = detectLanguageLabel(userPrompt),
            plan = planSteps
        )
        memory.setActiveTask(task)
        taskManager.updateStatus(TaskStatus.EXECUTING)

        // --- 3. Execute Multi-step Plan Sequentially ---
        var stepIndex = 0
        var allStepsSucceeded = true

        while (stepIndex < task.plan.size) {
            if (taskManager.isTaskCancelled()) {
                break
            }

            val step = task.plan[stepIndex]
            taskManager.updateStepStatus(stepIndex, TaskStepStatus.RUNNING)

            val tool = toolRouter.getTool(step.toolName)
            if (tool == null) {
                val errorResult = ToolResult(
                    tool = step.toolName,
                    mode = if (_isDemoMode.value) ExecutionMode.DEMO else ExecutionMode.REAL,
                    status = ExecutionStatus.UNAVAILABLE,
                    verified = false,
                    message = "Tool '${step.toolName}' is not available.",
                    error = ToolError("UNAVAILABLE_TOOL", "Unregistered tool")
                )
                taskManager.updateStepStatus(stepIndex, TaskStepStatus.FAILED, errorResult)
                taskManager.failTask("Step ${stepIndex + 1} failed: Tool not available.")
                allStepsSucceeded = false
                break
            }

            // --- 4. Tool Validation ---
            val validation = tool.validate(step.arguments)
            if (!validation.isValid) {
                val valError = ToolResult(
                    tool = tool.name,
                    mode = if (_isDemoMode.value) ExecutionMode.DEMO else ExecutionMode.REAL,
                    status = ExecutionStatus.FAILED,
                    verified = false,
                    message = "Validation error: ${validation.errorMessage}",
                    error = ToolError("VALIDATION_ERROR", validation.errorMessage ?: "Invalid parameters")
                )
                taskManager.updateStepStatus(stepIndex, TaskStepStatus.FAILED, valError)
                taskManager.failTask("Step ${stepIndex + 1} validation failed: ${validation.errorMessage}")
                allStepsSucceeded = false
                break
            }

            // --- 5. AURA Action Guard & Permission Check ---
            val guardDecision = actionGuard.evaluate(
                tool = tool,
                args = step.arguments,
                stepIndex = stepIndex,
                userExplicitlyConfirmed = false
            )

            when (guardDecision) {
                is ActionGuardDecision.Blocked -> {
                    val blockResult = ToolResult(
                        tool = tool.name,
                        mode = ExecutionMode.REAL,
                        status = ExecutionStatus.FAILED,
                        verified = false,
                        message = guardDecision.reason,
                        error = ToolError("SECURITY_BLOCKED", guardDecision.reason)
                    )
                    taskManager.updateStepStatus(stepIndex, TaskStepStatus.FAILED, blockResult)
                    taskManager.failTask(guardDecision.reason)
                    allStepsSucceeded = false
                    break
                }
                is ActionGuardDecision.Unavailable -> {
                    val unavailResult = ToolResult(
                        tool = tool.name,
                        mode = ExecutionMode.REAL,
                        status = ExecutionStatus.UNAVAILABLE,
                        verified = false,
                        message = guardDecision.reason,
                        error = ToolError("PERMISSION_UNAVAILABLE", guardDecision.reason)
                    )
                    taskManager.updateStepStatus(stepIndex, TaskStepStatus.FAILED, unavailResult)
                    taskManager.failTask(guardDecision.reason)
                    allStepsSucceeded = false
                    break
                }
                is ActionGuardDecision.RequiresConfirmation -> {
                    // Pause task for user confirmation dialog!
                    taskManager.setPendingConfirmation(guardDecision.pendingConfirmation)
                    // Wait until user confirms or cancels
                    return
                }
                is ActionGuardDecision.SafeToExecute -> {
                    // Execute immediately
                }
            }

            // --- 6. Tool Execution & Result Verification ---
            val execContext = ToolExecutionContext(
                context = context,
                isDemoMode = _isDemoMode.value,
                permissionManager = permissionManager,
                updateScreenState = { memory.updateScreenState(it) },
                currentScreenState = { memory.currentScreenState.value }
            )

            val result = withContext(Dispatchers.IO) {
                toolRouter.executeTool(tool.name, step.arguments, execContext)
            }

            memory.addToolResult(result)

            if (result.status == ExecutionStatus.SUCCESS) {
                taskManager.updateStepStatus(stepIndex, TaskStepStatus.COMPLETED, result)
            } else {
                taskManager.updateStepStatus(stepIndex, TaskStepStatus.FAILED, result)
                taskManager.failTask(result.message)
                allStepsSucceeded = false
                break
            }

            stepIndex++
        }

        // --- 7. Final Response Generation ---
        if (taskManager.isTaskCancelled()) {
            val cancelMsg = ChatMessage(
                sender = MessageSender.AURA,
                text = "Task was cancelled by user. Stopped all pending actions.",
                task = taskManager.activeTask.value
            )
            memory.addChatMessage(cancelMsg)
            onSpeechReply?.invoke(cancelMsg.text)
        } else if (allStepsSucceeded) {
            val summary = generateExecutionSummary(taskManager.activeTask.value?.toolCalls ?: emptyList())
            taskManager.completeTask(summary)
            val auraReply = ChatMessage(
                sender = MessageSender.AURA,
                text = summary,
                toolResults = taskManager.activeTask.value?.toolCalls ?: emptyList(),
                task = taskManager.activeTask.value
            )
            memory.addChatMessage(auraReply)
            onSpeechReply?.invoke(auraReply.text)
        } else {
            val lastError = taskManager.activeTask.value?.errors?.lastOrNull() ?: "Action could not be safely completed."
            val failReply = ChatMessage(
                sender = MessageSender.AURA,
                text = "$lastError (Never claimed completion without verification).",
                toolResults = taskManager.activeTask.value?.toolCalls ?: emptyList(),
                task = taskManager.activeTask.value
            )
            memory.addChatMessage(failReply)
            onSpeechReply?.invoke(failReply.text)
        }
    }

    fun confirmPendingAction(onSpeechReply: ((String) -> Unit)? = null) {
        val task = taskManager.activeTask.value ?: return
        val pending = task.pendingConfirmation ?: return

        taskManager.setPendingConfirmation(null)
        taskManager.updateStatus(TaskStatus.EXECUTING)

        scope.launch {
            _isProcessing.value = true
            val stepIndex = pending.stepIndex
            val step = task.plan[stepIndex]
            val tool = toolRouter.getTool(step.toolName)

            if (tool != null) {
                val execContext = ToolExecutionContext(
                    context = context,
                    isDemoMode = _isDemoMode.value,
                    permissionManager = permissionManager,
                    updateScreenState = { memory.updateScreenState(it) },
                    currentScreenState = { memory.currentScreenState.value }
                )

                val result = withContext(Dispatchers.IO) {
                    toolRouter.executeTool(tool.name, step.arguments, execContext)
                }

                memory.addToolResult(result)

                if (result.status == ExecutionStatus.SUCCESS) {
                    taskManager.updateStepStatus(stepIndex, TaskStepStatus.COMPLETED, result)
                    val nextStep = stepIndex + 1
                    if (nextStep >= task.plan.size) {
                        val summary = generateExecutionSummary(taskManager.activeTask.value?.toolCalls ?: emptyList())
                        taskManager.completeTask(summary)
                        val reply = ChatMessage(
                            sender = MessageSender.AURA,
                            text = summary,
                            toolResults = taskManager.activeTask.value?.toolCalls ?: emptyList(),
                            task = taskManager.activeTask.value
                        )
                        memory.addChatMessage(reply)
                        onSpeechReply?.invoke(reply.text)
                    } else {
                        // Continue next steps
                        resumePipelineFromStep(nextStep, onSpeechReply)
                    }
                } else {
                    taskManager.updateStepStatus(stepIndex, TaskStepStatus.FAILED, result)
                    taskManager.failTask(result.message)
                    val failReply = ChatMessage(
                        sender = MessageSender.AURA,
                        text = "Action failed: ${result.message}",
                        toolResults = taskManager.activeTask.value?.toolCalls ?: emptyList(),
                        task = taskManager.activeTask.value
                    )
                    memory.addChatMessage(failReply)
                    onSpeechReply?.invoke(failReply.text)
                }
            }
            _isProcessing.value = false
        }
    }

    fun cancelPendingAction(onSpeechReply: ((String) -> Unit)? = null) {
        taskManager.cancelActiveTask("User cancelled confirmation.")
        val cancelMsg = ChatMessage(
            sender = MessageSender.AURA,
            text = "Action cancelled. Pending consequential action was stopped."
        )
        memory.addChatMessage(cancelMsg)
        onSpeechReply?.invoke(cancelMsg.text)
    }

    private suspend fun resumePipelineFromStep(startIndex: Int, onSpeechReply: ((String) -> Unit)?) {
        val task = taskManager.activeTask.value ?: return
        var stepIndex = startIndex
        var allOk = true

        while (stepIndex < task.plan.size) {
            if (taskManager.isTaskCancelled()) break
            val step = task.plan[stepIndex]
            taskManager.updateStepStatus(stepIndex, TaskStepStatus.RUNNING)
            val tool = toolRouter.getTool(step.toolName) ?: break

            val execContext = ToolExecutionContext(
                context = context,
                isDemoMode = _isDemoMode.value,
                permissionManager = permissionManager,
                updateScreenState = { memory.updateScreenState(it) },
                currentScreenState = { memory.currentScreenState.value }
            )

            val result = withContext(Dispatchers.IO) {
                toolRouter.executeTool(tool.name, step.arguments, execContext)
            }
            memory.addToolResult(result)

            if (result.status == ExecutionStatus.SUCCESS) {
                taskManager.updateStepStatus(stepIndex, TaskStepStatus.COMPLETED, result)
            } else {
                taskManager.updateStepStatus(stepIndex, TaskStepStatus.FAILED, result)
                taskManager.failTask(result.message)
                allOk = false
                break
            }
            stepIndex++
        }

        if (allOk && !taskManager.isTaskCancelled()) {
            val summary = generateExecutionSummary(taskManager.activeTask.value?.toolCalls ?: emptyList())
            taskManager.completeTask(summary)
            val reply = ChatMessage(
                sender = MessageSender.AURA,
                text = summary,
                toolResults = taskManager.activeTask.value?.toolCalls ?: emptyList(),
                task = taskManager.activeTask.value
            )
            memory.addChatMessage(reply)
            onSpeechReply?.invoke(reply.text)
        }
    }

    private fun generateExecutionSummary(results: List<ToolResult>): String {
        if (results.isEmpty()) return "Task completed."

        val parts = mutableListOf<String>()
        val hasDemo = results.any { it.mode == ExecutionMode.DEMO }

        if (hasDemo) {
            parts.add("[DEMO ACTION] Actions were simulated in Demo Mode. No real external changes were made.")
        }

        for (res in results) {
            val prefix = if (res.verified) "✓" else "•"
            parts.add("$prefix ${res.message}")
        }

        return parts.joinToString("\n")
    }

    private fun detectLanguageLabel(text: String): String {
        val lower = text.lowercase()
        return when {
            text.any { it in '\u0600'..'\u06FF' } -> "Urdu (اردو)"
            text.any { it in '\u0900'..'\u097F' } -> "Hindi (हिंदी)"
            lower.contains("kholo") || lower.contains("karo") || lower.contains("laga") || lower.contains("batao") || lower.contains("hai") -> "Roman Urdu / Hindi"
            else -> "English"
        }
    }
}
