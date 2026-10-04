package com.example.security

object PromptInjectionGuard {

    private val SUSPICIOUS_PATTERNS = listOf(
        Regex("(?i)ignore (all )?(previous|above) (instructions|prompts|directives)"),
        Regex("(?i)system override"),
        Regex("(?i)you are now (in )?(developer mode|god mode|unrestricted)"),
        Regex("(?i)exfiltrate|send (the )?(api key|token|password|secret)"),
        Regex("(?i)bypass (action guard|confirmation|permission)"),
        Regex("(?i)<script\\b[^>]*>([\\s\\S]*?)<\\/script>"),
        Regex("(?i)eval\\s*\\(")
    )

    fun sanitizeUntrustedContent(rawContent: String): Pair<String, Boolean> {
        var isFlagged = false
        for (pattern in SUSPICIOUS_PATTERNS) {
            if (pattern.containsMatchIn(rawContent)) {
                isFlagged = true
                break
            }
        }

        // Strip dangerous injection payloads or replace with security warning
        val cleaned = rawContent
            .replace(Regex("(?i)ignore (all )?(previous|above) instructions"), "[SUSPICIOUS INSTRUCTION REMOVED]")
            .replace(Regex("(?i)system override"), "[SYSTEM OVERRIDE BLOCKED]")

        val wrapped = if (isFlagged) {
            "[AURA Action Guard Notice: Potential prompt injection attempt neutralized]\n$cleaned"
        } else {
            cleaned
        }

        return Pair(wrapped, isFlagged)
    }

    fun isMaliciousGoal(prompt: String): Boolean {
        return SUSPICIOUS_PATTERNS.any { it.containsMatchIn(prompt) }
    }
}
