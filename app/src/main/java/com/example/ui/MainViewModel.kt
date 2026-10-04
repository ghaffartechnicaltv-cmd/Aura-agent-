package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.agent.AuraCore
import com.example.agent.AuraTaskManager
import com.example.gemini.GeminiService
import com.example.memory.AuraMemory
import com.example.model.ChatMessage
import com.example.model.MessageSender
import com.example.model.PendingConfirmation
import com.example.permissions.AuraPermissionManager
import com.example.security.AuraActionGuard
import com.example.tools.AuraToolRouter
import com.example.voice.VoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppTab {
    HOME,
    TASKS,
    TOOLS,
    MEMORY,
    SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val permissionManager = AuraPermissionManager(application)
    val toolRouter = AuraToolRouter()
    val actionGuard = AuraActionGuard(permissionManager)
    val taskManager = AuraTaskManager()
    val memory = AuraMemory(application)
    val geminiService = GeminiService()
    val voiceManager = VoiceManager(application)

    val auraCore = AuraCore(
        context = application,
        toolRouter = toolRouter,
        actionGuard = actionGuard,
        taskManager = taskManager,
        memory = memory,
        geminiService = geminiService,
        permissionManager = permissionManager
    )

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _voiceErrorMessage = MutableStateFlow<String?>(null)
    val voiceErrorMessage: StateFlow<String?> = _voiceErrorMessage.asStateFlow()

    init {
        // Welcome message explaining AURA's agentic architecture
        if (memory.conversationHistory.value.isEmpty()) {
            memory.addChatMessage(
                ChatMessage(
                    sender = MessageSender.AURA,
                    text = "Hello! I am AURA, your multimodal AI personal assistant agent.\n\n" +
                            "I can understand English, Urdu, Roman Urdu, and Hindi. I plan multi-step actions, call verified tools (Device, Browser, Computer Use, Web Research), check permissions, and guard consequential actions with AURA Action Guard."
                )
            )
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendCurrentPrompt() {
        val query = _inputText.value.trim()
        if (query.isEmpty()) return
        _inputText.value = ""
        auraCore.submitRequest(query, isVoice = false) { reply ->
            voiceManager.speak(reply)
        }
    }

    fun sendPrompt(prompt: String) {
        if (prompt.isBlank()) return
        auraCore.submitRequest(prompt, isVoice = false) { reply ->
            voiceManager.speak(reply)
        }
    }

    fun toggleVoiceListening() {
        val currentState = voiceManager.voiceState.value
        if (currentState == com.example.voice.VoiceState.LISTENING) {
            voiceManager.stopListening()
        } else {
            voiceManager.startListening(
                onResult = { text ->
                    auraCore.submitRequest(text, isVoice = true) { reply ->
                        voiceManager.speak(reply)
                    }
                },
                onError = { err ->
                    _voiceErrorMessage.value = err
                }
            )
        }
    }

    fun clearVoiceError() {
        _voiceErrorMessage.value = null
    }

    fun confirmAction() {
        auraCore.confirmPendingAction { reply ->
            voiceManager.speak(reply)
        }
    }

    fun cancelAction() {
        auraCore.cancelPendingAction { reply ->
            voiceManager.speak(reply)
        }
    }

    fun cancelActiveTask() {
        taskManager.cancelActiveTask("User aborted task.")
    }

    fun toggleDemoMode(enabled: Boolean) {
        auraCore.setDemoMode(enabled)
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.shutdown()
    }
}
