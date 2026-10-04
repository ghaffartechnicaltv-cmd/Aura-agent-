package com.example.gemini

import com.example.BuildConfig
import com.example.model.TaskStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    var activeModel: String = "gemini-3.5-flash"

    fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        return if (key.isNullOrEmpty() || key == "MY_GEMINI_API_KEY") "" else key
    }

    fun isApiKeyConfigured(): Boolean = getApiKey().isNotEmpty()

    suspend fun planAndReason(
        userPrompt: String,
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): Pair<String, List<TaskStep>?> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) {
            // Use built-in AURA Multilingual Agentic Planner
            return@withContext Pair(
                "AURA AI Core reasoned using local agentic planner.",
                parseAgenticPlanLocally(userPrompt)
            )
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$activeModel:generateContent?key=$apiKey"

            val systemInstruction = """
                You are AURA, an advanced AI personal assistant agent.
                You understand English, Urdu, Roman Urdu, Hindi, and mixed language commands.
                You MUST break down user goals into concrete multi-step plans using AURA Tools.
                Registered Tools:
                - AURA.OpenApp(appName)
                - AURA.FindContact(name)
                - AURA.Call(phoneNumber, contactName)
                - AURA.SetAlarm(time, label)
                - AURA.CancelAlarm(alarmId)
                - AURA.OpenSettings(settingType)
                - AURA.DeviceTime()
                - AURA.DeviceStatus()
                - AURA.OpenBrowser(url)
                - AURA.Navigate(url)
                - AURA.WebSearch(query)
                - AURA.ReadPage(url)
                - AURA.Click(selector)
                - AURA.Type(text)
                - AURA.Scroll(direction)
                - AURA.Screenshot()
                - AURA.VerifyPage(expectedText)
                - AURA.PrepareMessage(recipient, message)
                - AURA.SendMessage(recipient, message)
                - AURA.PreparePost(platform, content)
                - AURA.PublishPost(platform, content)
                
                Respond in JSON format:
                {
                   "thought": "brief reasoning about user intent",
                   "response": "friendly direct response in the user's language",
                   "plan": [
                      {"tool": "ToolName", "title": "Step title", "args": {"key": "val"}}
                   ]
                }
            """.trimIndent()

            val contentsArray = JSONArray()

            // Add previous history
            for ((role, text) in conversationHistory.takeLast(4)) {
                val turnObj = JSONObject()
                turnObj.put("role", if (role.lowercase() == "user") "user" else "model")
                val parts = JSONArray()
                parts.put(JSONObject().put("text", text))
                turnObj.put("parts", parts)
                contentsArray.put(turnObj)
            }

            // Add current prompt
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()
            currentParts.put(JSONObject().put("text", userPrompt))
            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)

            val payload = JSONObject()
            payload.put("contents", contentsArray)

            val systemContent = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", systemInstruction))
            systemContent.put("parts", sysParts)
            payload.put("systemInstruction", systemContent)

            val generationConfig = JSONObject()
            generationConfig.put("temperature", 0.2)
            generationConfig.put("responseMimeType", "application/json")
            payload.put("generationConfig", generationConfig)

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val rootJson = JSONObject(bodyStr)
                val candidates = rootJson.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val text = firstCandidate?.optJSONObject("content")
                    ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text", "") ?: ""

                if (text.isNotEmpty()) {
                    val parsed = parsePlanJson(text)
                    if (parsed.second != null && parsed.second!!.isNotEmpty()) {
                        return@withContext parsed
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to local planner on network / quota error
        }

        Pair("AURA AI Core reasoned using local agentic planner.", parseAgenticPlanLocally(userPrompt))
    }

    private fun parsePlanJson(jsonText: String): Pair<String, List<TaskStep>?> {
        return try {
            val obj = JSONObject(jsonText)
            val reply = obj.optString("response", "Executing plan...")
            val planArray = obj.optJSONArray("plan")
            val steps = mutableListOf<TaskStep>()
            if (planArray != null) {
                for (i in 0 until planArray.length()) {
                    val stepObj = planArray.getJSONObject(i)
                    val tool = stepObj.getString("tool")
                    val title = stepObj.optString("title", "Run $tool")
                    val argsObj = stepObj.optJSONObject("args")
                    val argsMap = mutableMapOf<String, Any?>()
                    if (argsObj != null) {
                        for (key in argsObj.keys()) {
                            argsMap[key] = argsObj.get(key)
                        }
                    }
                    steps.add(
                        TaskStep(
                            stepId = "step_${i + 1}",
                            title = title,
                            toolName = tool,
                            arguments = argsMap
                        )
                    )
                }
            }
            Pair(reply, if (steps.isNotEmpty()) steps else null)
        } catch (e: Exception) {
            Pair("", null)
        }
    }

    fun parseAgenticPlanLocally(prompt: String): List<TaskStep> {
        val lower = prompt.lowercase().trim()
        val steps = mutableListOf<TaskStep>()

        // 1. YouTube + search compound: "YouTube kholo aur kids ABC learning search karo"
        if (lower.contains("youtube") && (lower.contains("search") || lower.contains("dhundo"))) {
            val queryMatch = Regex("(?i)(search|dhundo|dhoondo)\\s+(karo\\s+)?(.+)").find(lower)
            val query = queryMatch?.groupValues?.get(3)?.trim() ?: "kids ABC learning"
            steps.add(
                TaskStep(
                    stepId = "step_1",
                    title = "Open YouTube App",
                    toolName = "AURA.OpenApp",
                    arguments = mapOf("appName" to "YouTube")
                )
            )
            steps.add(
                TaskStep(
                    stepId = "step_2",
                    title = "Search video: $query",
                    toolName = "AURA.WebSearch",
                    arguments = mapOf("query" to "$query on YouTube")
                )
            )
            steps.add(
                TaskStep(
                    stepId = "step_3",
                    title = "Verify Video Results",
                    toolName = "AURA.VerifyPage",
                    arguments = mapOf("expectedText" to query)
                )
            )
            return steps
        }

        // 2. Open App: "Google kholo", "YouTube kholo", "WhatsApp open karo", "Open Camera"
        if (lower.contains("kholo") || lower.contains("open") || lower.contains("khol do") || lower.contains("chalao") || lower.contains("खोलो")) {
            val appName = when {
                lower.contains("google") -> "Google"
                lower.contains("youtube") -> "YouTube"
                lower.contains("chrome") -> "Chrome"
                lower.contains("whatsapp") -> "WhatsApp"
                lower.contains("camera") -> "Camera"
                lower.contains("settings") -> "Settings"
                lower.contains("clock") -> "Clock"
                lower.contains("calculator") -> "Calculator"
                lower.contains("facebook") -> "Facebook"
                lower.contains("instagram") -> "Instagram"
                else -> {
                    // Extract app name from "X kholo"
                    lower.replace(Regex("(?i)kholo|open|khol do|chalao|खोलो|app|application|ko"), "").trim()
                        .replaceFirstChar { it.uppercase() }
                }
            }
            steps.add(
                TaskStep(
                    stepId = "step_1",
                    title = "Launch $appName Application",
                    toolName = "AURA.OpenApp",
                    arguments = mapOf("appName" to appName)
                )
            )
            return steps
        }

        // 3. Call: "Ali ko call karo", "Ahmed ko call kar do", "Call John", "علی کو کال کرو"
        if (lower.contains("call") || lower.contains("phone") || lower.contains("dial") || lower.contains("کال")) {
            val contactName = lower
                .replace(Regex("(?i)ko|call|kar do|karo|phone|dial|کال|کرو|please|lagao"), "")
                .trim()
                .ifEmpty { "Ali" }
                .replaceFirstChar { it.uppercase() }

            steps.add(
                TaskStep(
                    stepId = "step_1",
                    title = "Resolve Contact for '$contactName'",
                    toolName = "AURA.FindContact",
                    arguments = mapOf("name" to contactName)
                )
            )
            steps.add(
                TaskStep(
                    stepId = "step_2",
                    title = "Initiate Phone Call to $contactName",
                    toolName = "AURA.Call",
                    arguments = mapOf("contactName" to contactName, "phoneNumber" to "+1 (555) 019-2834")
                )
            )
            return steps
        }

        // 4. Alarm: "Kal subah 7 baje alarm laga do", "7 AM ka alarm laga do", "Set alarm for 6:30"
        if (lower.contains("alarm") || lower.contains("الارم") || lower.contains("अलार्म")) {
            val time = when {
                lower.contains("7") -> "07:00 AM"
                lower.contains("6:30") -> "06:30 AM"
                lower.contains("6") -> "06:00 AM"
                lower.contains("8") -> "08:00 AM"
                else -> "07:00 AM"
            }
            val label = if (lower.contains("subah") || lower.contains("morning")) "Morning Wakeup" else "AURA Alarm"

            if (lower.contains("cancel") || lower.contains("hatao") || lower.contains("band")) {
                steps.add(
                    TaskStep(
                        stepId = "step_1",
                        title = "Cancel Device Alarm",
                        toolName = "AURA.CancelAlarm",
                        arguments = mapOf("alarmId" to time)
                    )
                )
            } else {
                steps.add(
                    TaskStep(
                        stepId = "step_1",
                        title = "Verify System Timezone",
                        toolName = "AURA.DeviceTime",
                        arguments = emptyMap()
                    )
                )
                steps.add(
                    TaskStep(
                        stepId = "step_2",
                        title = "Create Alarm for $time",
                        toolName = "AURA.SetAlarm",
                        arguments = mapOf("time" to time, "label" to label)
                    )
                )
            }
            return steps
        }

        // 5. Weather / Research / Search: "Pakistan ka weather search karo aur mujhe batao"
        if (lower.contains("weather") || lower.contains("mausam") || lower.contains("search") || lower.contains("research") || lower.contains("kya hai") || lower.contains("what is")) {
            val query = lower.replace(Regex("(?i)aur mujhe batao|search karo|batao|karo|search"), "").trim()
                .ifEmpty { "Pakistan weather" }
            steps.add(
                TaskStep(
                    stepId = "step_1",
                    title = "Web Research: $query",
                    toolName = "AURA.WebSearch",
                    arguments = mapOf("query" to query)
                )
            )
            steps.add(
                TaskStep(
                    stepId = "step_2",
                    title = "Cross-Reference Authoritative Sources",
                    toolName = "AURA.Compare",
                    arguments = mapOf("dataPoints" to query)
                )
            )
            steps.add(
                TaskStep(
                    stepId = "step_3",
                    title = "Verify Verified Data Summary",
                    toolName = "AURA.Summarize",
                    arguments = mapOf("topic" to query)
                )
            )
            return steps
        }

        // 6. Device Status / Time
        if (lower.contains("battery") || lower.contains("status") || lower.contains("device") || lower.contains("charging")) {
            steps.add(
                TaskStep(
                    stepId = "step_1",
                    title = "Inspect Device Hardware Status",
                    toolName = "AURA.DeviceStatus",
                    arguments = emptyMap()
                )
            )
            return steps
        }

        if (lower.contains("time") || lower.contains("waqt") || lower.contains("date") || lower.contains("tarikh")) {
            steps.add(
                TaskStep(
                    stepId = "step_1",
                    title = "Fetch Device Synchronized Time",
                    toolName = "AURA.DeviceTime",
                    arguments = emptyMap()
                )
            )
            return steps
        }

        // 7. Social Post: "Post tayar karo", "Facebook par post karo"
        if (lower.contains("post")) {
            val platform = if (lower.contains("facebook")) "Facebook" else "Instagram"
            val content = prompt.replace(Regex("(?i)post|tayar karo|publish|par|karo|facebook|instagram"), "").trim()
                .ifEmpty { "Excited to test AURA AI personal assistant agent!" }
            steps.add(
                TaskStep(
                    stepId = "step_1",
                    title = "Prepare $platform Post Draft",
                    toolName = "AURA.PreparePost",
                    arguments = mapOf("platform" to platform, "content" to content)
                )
            )
            if (lower.contains("publish") || lower.contains("bhej") || lower.contains("upload")) {
                steps.add(
                    TaskStep(
                        stepId = "step_2",
                        title = "Publish Post to $platform",
                        toolName = "AURA.PublishPost",
                        arguments = mapOf("platform" to platform, "content" to content)
                    )
                )
            }
            return steps
        }

        // Default: General Web Research
        steps.add(
            TaskStep(
                stepId = "step_1",
                title = "Search authoritative online sources",
                toolName = "AURA.WebSearch",
                arguments = mapOf("query" to prompt)
            )
        )
        return steps
    }
}
