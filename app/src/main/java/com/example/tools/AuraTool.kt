package com.example.tools

import android.content.Context
import com.example.model.ExecutionMode
import com.example.model.ExecutionStatus
import com.example.model.RiskLevel
import com.example.model.ScreenState
import com.example.model.ToolError
import com.example.model.ToolResult
import com.example.model.ValidationResult
import com.example.model.VerificationResult
import com.example.permissions.AuraPermissionManager

data class ToolExecutionContext(
    val context: Context,
    val isDemoMode: Boolean,
    val permissionManager: AuraPermissionManager,
    val updateScreenState: (ScreenState) -> Unit,
    val currentScreenState: () -> ScreenState
)

interface AuraTool {
    val name: String
    val description: String
    val parameters: Map<String, String>
    val permission: String
    val riskLevel: RiskLevel
    val requiresConfirmation: Boolean

    suspend fun validate(args: Map<String, Any?>): ValidationResult
    suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult
    suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult
    fun handleError(e: Throwable): ToolResult {
        return ToolResult(
            tool = name,
            mode = ExecutionMode.REAL,
            status = ExecutionStatus.FAILED,
            verified = false,
            error = ToolError("EXECUTION_ERROR", e.localizedMessage ?: "Unknown execution failure")
        )
    }
}
