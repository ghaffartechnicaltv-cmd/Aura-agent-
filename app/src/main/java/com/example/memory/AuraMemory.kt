package com.example.memory

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AuraTask
import com.example.model.ChatMessage
import com.example.model.ScreenState
import com.example.model.ToolResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class MemoryEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)

class AuraMemory(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("aura_memory_prefs", Context.MODE_PRIVATE)

    // Short-Term Memory
    private val _conversationHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val conversationHistory: StateFlow<List<ChatMessage>> = _conversationHistory.asStateFlow()

    private val _activeTask = MutableStateFlow<AuraTask?>(null)
    val activeTask: StateFlow<AuraTask?> = _activeTask.asStateFlow()

    private val _recentToolResults = MutableStateFlow<List<ToolResult>>(emptyList())
    val recentToolResults: StateFlow<List<ToolResult>> = _recentToolResults.asStateFlow()

    private val _currentScreenState = MutableStateFlow(ScreenState())
    val currentScreenState: StateFlow<ScreenState> = _currentScreenState.asStateFlow()

    // Long-Term Memory
    private val _isMemoryEnabled = MutableStateFlow(prefs.getBoolean("memory_enabled", true))
    val isMemoryEnabled: StateFlow<Boolean> = _isMemoryEnabled.asStateFlow()

    private val _longTermMemories = MutableStateFlow<List<MemoryEntry>>(loadMemories())
    val longTermMemories: StateFlow<List<MemoryEntry>> = _longTermMemories.asStateFlow()

    private val FORBIDDEN_SECRET_PATTERNS = listOf(
        Regex("(?i)password"),
        Regex("(?i)otp"),
        Regex("(?i)pin"),
        Regex("(?i)secret_key"),
        Regex("(?i)private_key"),
        Regex("(?i)api_key"),
        Regex("(?i)auth_token"),
        Regex("(?i)access_token"),
        Regex("(?i)bearer ")
    )

    fun addChatMessage(message: ChatMessage) {
        _conversationHistory.value = _conversationHistory.value + message
    }

    fun clearChat() {
        _conversationHistory.value = emptyList()
    }

    fun setActiveTask(task: AuraTask?) {
        _activeTask.value = task
    }

    fun addToolResult(result: ToolResult) {
        val current = _recentToolResults.value.toMutableList()
        current.add(0, result)
        if (current.size > 20) current.removeAt(current.size - 1)
        _recentToolResults.value = current
    }

    fun updateScreenState(state: ScreenState) {
        _currentScreenState.value = state
    }

    // Long-Term Memory Operations
    fun savePreference(key: String, value: String): Boolean {
        if (!_isMemoryEnabled.value) return false

        // Security check: Block secret storage
        for (pattern in FORBIDDEN_SECRET_PATTERNS) {
            if (pattern.containsMatchIn(key) || pattern.containsMatchIn(value)) {
                return false // Blocked: Never store sensitive credentials
            }
        }

        val current = _longTermMemories.value.filter { it.key != key }.toMutableList()
        current.add(MemoryEntry(key = key, value = value))
        _longTermMemories.value = current
        persistMemories(current)
        return true
    }

    fun deleteMemory(id: String) {
        val current = _longTermMemories.value.filter { it.id != id }
        _longTermMemories.value = current
        persistMemories(current)
    }

    fun clearAllLongTermMemories() {
        _longTermMemories.value = emptyList()
        persistMemories(emptyList())
    }

    fun toggleMemory(enabled: Boolean) {
        _isMemoryEnabled.value = enabled
        prefs.edit().putBoolean("memory_enabled", enabled).apply()
    }

    private fun loadMemories(): List<MemoryEntry> {
        val jsonStr = prefs.getString("memories_json", null) ?: return listOf(
            MemoryEntry(key = "preferred_language", value = "English / Roman Urdu"),
            MemoryEntry(key = "assistant_persona", value = "Professional, precise, safety-first personal assistant")
        )
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<MemoryEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MemoryEntry(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        key = obj.getString("key"),
                        value = obj.getString("value"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistMemories(list: List<MemoryEntry>) {
        val array = JSONArray()
        for (entry in list) {
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("key", entry.key)
            obj.put("value", entry.value)
            obj.put("timestamp", entry.timestamp)
            array.put(obj)
        }
        prefs.edit().putString("memories_json", array.toString()).apply()
    }
}
