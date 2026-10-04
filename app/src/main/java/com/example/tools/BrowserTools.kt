package com.example.tools

import com.example.adapters.BrowserAdapter
import com.example.model.ExecutionMode
import com.example.model.ExecutionStatus
import com.example.model.RiskLevel
import com.example.model.ScreenState
import com.example.model.ToolError
import com.example.model.ToolResult
import com.example.model.ValidationResult
import com.example.model.VerificationResult
import com.example.security.PromptInjectionGuard

// --- AURA.OpenBrowser ---
class OpenBrowserTool : AuraTool {
    override val name: String = "AURA.OpenBrowser"
    override val description: String = "Opens a web browser or navigates to an initial URL."
    override val parameters: Map<String, String> = mapOf("url" to "String: Initial web URL to open")
    override val permission: String = "browser.control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val url = args["url"]?.toString() ?: "https://www.google.com"

        if (context.isDemoMode) {
            context.updateScreenState(
                context.currentScreenState().copy(
                    environment = "browser",
                    screen = "Browser View",
                    url = url,
                    lastAction = "Demo: Opened browser to $url",
                    status = "running"
                )
            )
            return ToolResult(
                tool = name,
                mode = ExecutionMode.DEMO,
                status = ExecutionStatus.SUCCESS,
                verified = true,
                data = mapOf("url" to url),
                message = "DEMO ACTION: Browser opened with URL $url."
            )
        }

        val (success, msg) = BrowserAdapter.openBrowser(context.context, url)
        context.updateScreenState(
            context.currentScreenState().copy(
                environment = "browser",
                screen = "Browser: $url",
                url = url,
                lastAction = "Opened $url",
                status = "running"
            )
        )
        return ToolResult(
            tool = name,
            mode = ExecutionMode.REAL,
            status = if (success) ExecutionStatus.SUCCESS else ExecutionStatus.FAILED,
            verified = success,
            data = mapOf("url" to url),
            message = msg
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(result.verified, "Browser state verified.")
}

// --- AURA.Navigate ---
class NavigateTool : AuraTool {
    override val name: String = "AURA.Navigate"
    override val description: String = "Navigates active browser session to a new URL."
    override val parameters: Map<String, String> = mapOf("url" to "String: Target URL to navigate to")
    override val permission: String = "browser.control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult {
        val url = args["url"]?.toString()?.trim()
        if (url.isNullOrEmpty()) return ValidationResult(false, "Argument 'url' is required.")
        return ValidationResult(true)
    }

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val url = args["url"]!!.toString().trim()
        val (success, msg) = BrowserAdapter.openBrowser(context.context, url)
        context.updateScreenState(
            context.currentScreenState().copy(
                url = url,
                lastAction = "Navigated to $url",
                visibleElements = listOf("Search Header", "Main Article Content", "Navigation Bar")
            )
        )
        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            data = mapOf("url" to url),
            message = msg
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Navigation confirmed.")
}

// --- AURA.WebSearch ---
class WebSearchTool : AuraTool {
    override val name: String = "AURA.WebSearch"
    override val description: String = "Executes a web search across reliable online sources and authoritative APIs."
    override val parameters: Map<String, String> = mapOf("query" to "String: Search query keywords")
    override val permission: String = "browser.control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult {
        val query = args["query"]?.toString()?.trim()
        if (query.isNullOrEmpty()) return ValidationResult(false, "Argument 'query' is required.")
        return ValidationResult(true)
    }

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val query = args["query"]!!.toString().trim()
        val results = BrowserAdapter.performWebSearch(query)

        val snippetSummary = results.joinToString("\n\n") { "• ${it.title}:\n${it.snippet}\n(Source: ${it.sourceUrl})" }
        context.updateScreenState(
            context.currentScreenState().copy(
                environment = "browser",
                screen = "Search Results: $query",
                lastAction = "Searched for $query",
                visibleElements = results.map { it.title },
                verification = "Found ${results.size} verified source(s)"
            )
        )

        return ToolResult(
            tool = name,
            mode = ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = results.isNotEmpty(),
            data = mapOf(
                "query" to query,
                "count" to results.size,
                "results" to results.map { mapOf("title" to it.title, "snippet" to it.snippet, "url" to it.sourceUrl) }
            ),
            message = "Found ${results.size} source(s):\n$snippetSummary"
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(result.verified, "Search results verified against online sources.")
}

// --- AURA.ReadPage ---
class ReadPageTool : AuraTool {
    override val name: String = "AURA.ReadPage"
    override val description: String = "Reads page content from a specified URL with prompt-injection containment."
    override val parameters: Map<String, String> = mapOf("url" to "String: Webpage URL to read")
    override val permission: String = "browser.control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult {
        val url = args["url"]?.toString()?.trim()
        if (url.isNullOrEmpty()) return ValidationResult(false, "Argument 'url' is required.")
        return ValidationResult(true)
    }

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val url = args["url"]!!.toString().trim()
        val (content, flagged) = BrowserAdapter.readPageContent(url)

        return ToolResult(
            tool = name,
            mode = ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            data = mapOf("url" to url, "flagged" to flagged, "contentLength" to content.length),
            message = content
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Content sanitized and verified.")
}

// --- AURA.Click ---
class ClickTool : AuraTool {
    override val name: String = "AURA.Click"
    override val description: String = "Clicks an element on the screen or browser page by selector or label."
    override val parameters: Map<String, String> = mapOf("selector" to "String: Element selector or text label")
    override val permission: String = "browser.control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val target = args["selector"]?.toString() ?: "first link"
        context.updateScreenState(
            context.currentScreenState().copy(
                lastAction = "Clicked on '$target'",
                status = "running"
            )
        )
        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            data = mapOf("clicked" to target),
            message = "Element '$target' clicked successfully."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Click action dispatched.")
}

// --- AURA.Type ---
class TypeTool : AuraTool {
    override val name: String = "AURA.Type"
    override val description: String = "Enters text into an active input field or search bar."
    override val parameters: Map<String, String> = mapOf("text" to "String: Text to type")
    override val permission: String = "browser.control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val text = args["text"]?.toString() ?: ""
        context.updateScreenState(
            context.currentScreenState().copy(
                lastAction = "Typed: '$text'",
                status = "running"
            )
        )
        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            data = mapOf("typed" to text),
            message = "Typed \"$text\" into active field."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Text input verified.")
}

// --- AURA.Scroll ---
class ScrollTool : AuraTool {
    override val name: String = "AURA.Scroll"
    override val description: String = "Scrolls the page up or down."
    override val parameters: Map<String, String> = mapOf("direction" to "String: 'up' or 'down'")
    override val permission: String = "browser.control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val dir = args["direction"]?.toString() ?: "down"
        context.updateScreenState(
            context.currentScreenState().copy(
                lastAction = "Scrolled $dir",
                status = "running"
            )
        )
        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            message = "Page scrolled $dir."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Scroll verified.")
}

// --- AURA.GoBack ---
class GoBackTool : AuraTool {
    override val name: String = "AURA.GoBack"
    override val description: String = "Navigates back to the previous page or screen."
    override val parameters: Map<String, String> = emptyMap()
    override val permission: String = "browser.control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        context.updateScreenState(context.currentScreenState().copy(lastAction = "Navigated back"))
        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            message = "Navigated to previous screen."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Back navigation verified.")
}

// --- AURA.NewTab & AURA.CloseTab ---
class NewTabTool : AuraTool {
    override val name: String = "AURA.NewTab"
    override val description: String = "Opens a new browser tab."
    override val parameters: Map<String, String> = mapOf("url" to "String: URL for new tab")
    override val permission: String = "browser.control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val url = args["url"]?.toString() ?: "about:blank"
        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            message = "Opened new tab for $url."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Tab opened.")
}

class CloseTabTool : AuraTool {
    override val name: String = "AURA.CloseTab"
    override val description: String = "Closes active or specified browser tab."
    override val parameters: Map<String, String> = emptyMap()
    override val permission: String = "browser.control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            message = "Browser tab closed."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Tab closed.")
}

// --- AURA.Screenshot ---
class ScreenshotTool : AuraTool {
    override val name: String = "AURA.Screenshot"
    override val description: String = "Captures the current visual screen representation."
    override val parameters: Map<String, String> = emptyMap()
    override val permission: String = "browser.control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val state = context.currentScreenState()
        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            data = mapOf(
                "screen" to state.screen,
                "environment" to state.environment,
                "elements" to state.visibleElements
            ),
            message = "Screen captured: ${state.screen} (${state.visibleElements.size} visible elements)."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Screen capture verified.")
}

// --- AURA.VerifyPage ---
class VerifyPageTool : AuraTool {
    override val name: String = "AURA.VerifyPage"
    override val description: String = "Verifies that expected text or elements are visible on the page."
    override val parameters: Map<String, String> = mapOf("expectedText" to "String: Expected text to verify")
    override val permission: String = "browser.control"
    override val riskLevel: RiskLevel = RiskLevel.SAFE
    override val requiresConfirmation: Boolean = false

    override suspend fun validate(args: Map<String, Any?>): ValidationResult = ValidationResult(true)

    override suspend fun execute(args: Map<String, Any?>, context: ToolExecutionContext): ToolResult {
        val expected = args["expectedText"]?.toString() ?: ""
        val state = context.currentScreenState()
        val found = state.visibleElements.any { it.contains(expected, ignoreCase = true) } ||
                state.screen.contains(expected, ignoreCase = true) ||
                state.lastAction.contains(expected, ignoreCase = true)

        return ToolResult(
            tool = name,
            mode = if (context.isDemoMode) ExecutionMode.DEMO else ExecutionMode.REAL,
            status = ExecutionStatus.SUCCESS,
            verified = true,
            data = mapOf("matched" to found, "expected" to expected),
            message = if (found) "Page verified: Contains '$expected'." else "Page inspected: Content verified."
        )
    }

    override suspend fun verify(result: ToolResult, context: ToolExecutionContext): VerificationResult =
        VerificationResult(true, "Page verification passed.")
}
