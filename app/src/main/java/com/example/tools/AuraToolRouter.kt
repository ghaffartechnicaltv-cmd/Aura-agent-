package com.example.tools

import com.example.model.ExecutionMode
import com.example.model.ExecutionStatus
import com.example.model.ToolError
import com.example.model.ToolResult
import com.example.model.ValidationResult

class AuraToolRouter {

    private val registry = mutableMapOf<String, AuraTool>()

    init {
        // Device tools
        register(OpenAppTool())
        register(FindContactTool())
        register(CallTool())
        register(SetAlarmTool())
        register(CancelAlarmTool())
        register(OpenSettingsTool())
        register(DeviceTimeTool())
        register(DeviceStatusTool())

        // Browser tools
        register(OpenBrowserTool())
        register(NavigateTool())
        register(WebSearchTool())
        register(ReadPageTool())
        register(ClickTool())
        register(TypeTool())
        register(ScrollTool())
        register(GoBackTool())
        register(NewTabTool())
        register(CloseTabTool())
        register(ScreenshotTool())
        register(VerifyPageTool())

        // Computer Use tools
        register(ScreenTool())
        register(InspectScreenTool())
        register(ClickScreenTool())
        register(TypeScreenTool())
        register(PressKeyTool())
        register(SwipeTool())
        register(WaitTool())
        register(VerifyScreenTool())

        // Web Research tools
        register(SearchTool())
        register(CompareTool())
        register(SummarizeTool())

        // Communication tools
        register(PrepareMessageTool())
        register(SendMessageTool())
        register(InitiateCallTool())

        // Social Media tools
        register(OpenSocialAppTool())
        register(PreparePostTool())
        register(PublishPostTool())
    }

    fun register(tool: AuraTool) {
        registry[tool.name] = tool
    }

    fun getTool(name: String): AuraTool? {
        return registry[name]
    }

    fun getAllTools(): List<AuraTool> {
        return registry.values.toList()
    }

    suspend fun validateToolCall(toolName: String, args: Map<String, Any?>): ValidationResult {
        val tool = registry[toolName] ?: return ValidationResult(
            false,
            "Tool '$toolName' is not registered in AURA Tools. Unsupported tool."
        )
        return tool.validate(args)
    }

    suspend fun executeTool(
        toolName: String,
        args: Map<String, Any?>,
        context: ToolExecutionContext
    ): ToolResult {
        val tool = registry[toolName] ?: return ToolResult(
            tool = toolName,
            mode = ExecutionMode.REAL,
            status = ExecutionStatus.UNAVAILABLE,
            verified = false,
            message = "Tool '$toolName' is not registered in AURA Tools.",
            error = ToolError("TOOL_NOT_FOUND", "Unsupported tool requested")
        )

        val validation = tool.validate(args)
        if (!validation.isValid) {
            return ToolResult(
                tool = toolName,
                mode = ExecutionMode.REAL,
                status = ExecutionStatus.FAILED,
                verified = false,
                message = "Validation error: ${validation.errorMessage}",
                error = ToolError("VALIDATION_ERROR", validation.errorMessage ?: "Invalid parameters")
            )
        }

        return try {
            val result = tool.execute(args, context)
            val verification = tool.verify(result, context)
            result.copy(verified = verification.isVerified)
        } catch (e: Throwable) {
            tool.handleError(e)
        }
    }
}
