package com.example.agent

import com.example.model.AuraTask
import com.example.model.PendingConfirmation
import com.example.model.TaskStatus
import com.example.model.TaskStep
import com.example.model.TaskStepStatus
import com.example.model.ToolResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class AuraTaskManager {

    private val _tasks = MutableStateFlow<List<AuraTask>>(emptyList())
    val tasks: StateFlow<List<AuraTask>> = _tasks.asStateFlow()

    private val _activeTask = MutableStateFlow<AuraTask?>(null)
    val activeTask: StateFlow<AuraTask?> = _activeTask.asStateFlow()

    @Volatile
    private var isCancelled = false

    fun createTask(userRequest: String, language: String, plan: List<TaskStep>): AuraTask {
        val task = AuraTask(
            taskId = "AURA-TASK-" + UUID.randomUUID().toString().take(8).uppercase(),
            userRequest = userRequest,
            languageDetected = language,
            plan = plan,
            status = TaskStatus.PLANNING
        )
        isCancelled = false
        _activeTask.value = task
        _tasks.value = listOf(task) + _tasks.value
        return task
    }

    fun isTaskCancelled(): Boolean = isCancelled

    fun cancelActiveTask(reason: String = "User cancelled task.") {
        isCancelled = true
        val current = _activeTask.value ?: return
        val updated = current.copy(
            status = TaskStatus.CANCELLED,
            completedAt = System.currentTimeMillis()
        )
        updated.errors.add(reason)
        _activeTask.value = updated
        updateTaskInHistory(updated)
    }

    fun updateStatus(status: TaskStatus) {
        val current = _activeTask.value ?: return
        val updated = current.copy(status = status)
        _activeTask.value = updated
        updateTaskInHistory(updated)
    }

    fun updateStepStatus(stepIndex: Int, stepStatus: TaskStepStatus, result: ToolResult? = null) {
        val current = _activeTask.value ?: return
        if (stepIndex in current.plan.indices) {
            val step = current.plan[stepIndex]
            step.status = stepStatus
            if (result != null) {
                step.result = result
                current.toolCalls.add(result)
            }
            val updated = current.copy(currentStepIndex = stepIndex)
            _activeTask.value = updated
            updateTaskInHistory(updated)
        }
    }

    fun setPendingConfirmation(confirmation: PendingConfirmation?) {
        val current = _activeTask.value ?: return
        val updated = current.copy(
            pendingConfirmation = confirmation,
            status = if (confirmation != null) TaskStatus.WAITING_CONFIRMATION else current.status
        )
        _activeTask.value = updated
        updateTaskInHistory(updated)
    }

    fun completeTask(verificationSummary: String) {
        val current = _activeTask.value ?: return
        val updated = current.copy(
            status = TaskStatus.COMPLETED,
            completedAt = System.currentTimeMillis(),
            verificationSummary = verificationSummary,
            pendingConfirmation = null
        )
        _activeTask.value = updated
        updateTaskInHistory(updated)
    }

    fun failTask(errorMessage: String) {
        val current = _activeTask.value ?: return
        current.errors.add(errorMessage)
        val updated = current.copy(
            status = TaskStatus.FAILED,
            completedAt = System.currentTimeMillis(),
            pendingConfirmation = null
        )
        _activeTask.value = updated
        updateTaskInHistory(updated)
    }

    private fun updateTaskInHistory(task: AuraTask) {
        _tasks.value = _tasks.value.map { if (it.taskId == task.taskId) task else it }
    }
}
