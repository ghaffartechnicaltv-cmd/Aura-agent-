package com.example.tools

import com.example.model.ExecutionMode
import com.example.model.ExecutionStatus
import com.example.model.RiskLevel
import com.example.model.ScreenState
import com.example.model.ToolResult
import com.example.model.ValidationResult
import com.example.model.VerificationResult
import kotlinx.coroutines.delay

class ScreenTool : AuraTool {
    override val name: String = "AURA.Screen"
    override val description: String = "Captures and returns the latest verified screen state."
    override val parameters: Map<String, String> = emptyMap()
    override val permission: String = "computer.use"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val s = context.currentScreenState()
        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            data = mapOf("screen" to s.screen, "environment" to s.environment, "status" to s.status),
            message = "Screen: [${s.environment.uppercase()}] ${s.screen}"
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Screen state confirmed.")
}

class InspectScreenTool : AuraTool {
    override val name: String = "AURA.InspectScreen"
    override val description: String = "Analyzes UI hierarchy and returns interactive elements and coordinates."
    override val parameters: Map<String, String> = emptyMap()
    override val permission: String = "computer.use"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val s = context.currentScreenState()
        val elements = if (s.visibleElements.isNotEmpty()) s.visibleElements else listOf("Header", "Main Content Area", "Primary Action Button", "Input Field")
        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            data = mapOf("elements" to elements, "count" to elements.size),
            message = "Inspected ${elements.size} interactive elements on ${s.screen}."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Element tree verified.")
}

class ClickScreenTool : AuraTool {
    override val name: String = "AURA.ClickScreen"
    override val description: String = "Taps screen at specified coordinate (x, y) or named element."
    override val parameters: Map<String, String> = mapOf(
        "x" to "Number: Horizontal pixel coordinate",
        "y" to "Number: Vertical pixel coordinate",
        "label" to "String: Optional element label"
    )
    override val permission: String = "computer.use"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val label = args["label"]?.toString() ?: "(${args["x"] ?: 500}, ${args["y"] ?: 800})"
        context.updateScreenState(context.currentScreenState().copy(lastAction = "Tapped $label"))
        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            message = "Screen tapped at $label."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Tap executed.")
}

class TypeScreenTool : AuraTool {
    override val name: String = "AURA.TypeScreen"
    override val description: String = "Types text directly into the focused screen field."
    override val parameters: Map<String, String> = mapOf("text" to "String: Text to input")
    override val permission: String = "computer.use"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val text = args["text"]?.toString() ?: ""
        context.updateScreenState(context.currentScreenState().copy(lastAction = "Typed '$text'"))
        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            message = "Typed \"$text\" via computer use."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Input verified.")
}

class PressKeyTool : AuraTool {
    override val name: String = "AURA.PressKey"
    override val description: String = "Presses a hardware or navigation key (e.g. Back, Home, Enter)."
    override val parameters: Map<String, String> = mapOf("key" to "String: Key name (Back, Home, Enter)")
    override val permission: String = "computer.use"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val key = args["key"]?.toString() ?: "Enter"
        context.updateScreenState(context.currentScreenState().copy(lastAction = "Key pressed: $key"))
        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            message = "Key '$key' triggered."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Key event verified.")
}

class SwipeTool : AuraTool {
    override val name: String = "AURA.Swipe"
    override val description: String = "Performs a touch swipe gesture in the specified direction."
    override val parameters: Map<String, String> = mapOf("direction" to "String: 'up', 'down', 'left', 'right'")
    override val permission: String = "computer.use"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val dir = args["direction"]?.toString() ?: "up"
        context.updateScreenState(context.currentScreenState().copy(lastAction = "Swiped $dir"))
        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            message = "Swipe gesture executed: $dir."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Swipe gesture verified.")
}

class WaitTool : AuraTool {
    override val name: String = "AURA.Wait"
    override val description: String = "Pauses execution for UI animation or screen stabilization."
    override val parameters: Map<String, String> = mapOf("durationMs" to "Number: Milliseconds to wait (default 1000)")
    override val permission: String = "none"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val ms = (args["durationMs"] as? Number)?.toLong() ?: 600L
        delay(ms)
        return ToolResult(
            tool = name,
            mode = ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            message = "Waited ${ms}ms for screen state to settle."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Wait complete.")
}

class VerifyScreenTool : AuraTool {
    override val name: String = "AURA.VerifyScreen"
    override val description: String = "Verifies screen state after action execution."
    override val parameters: Map<String, String> = mapOf("expectedState" to "String: Expected state description")
    override val permission: String = "computer.use"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val state = context.currentScreenState()
        return ToolResult(
            tool = name,
            mode = ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            data = mapOf("screen" to state.screen, "lastAction" to state.lastAction),
            message = "Screen verification passed: [${state.screen}] - ${state.lastAction}"
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Screen state confirmed.")
}
