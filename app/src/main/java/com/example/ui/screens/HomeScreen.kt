package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MessageSender
import com.example.model.TaskStatus
import com.example.ui.MainViewModel
import com.example.ui.components.AuraOrb
import com.example.ui.components.ToolResultBadge
import com.example.ui.theme.AuraCyanPrimary
import com.example.ui.theme.AuraDangerCrimson
import com.example.ui.theme.AuraDarkBackground
import com.example.ui.theme.AuraSuccessEmerald
import com.example.ui.theme.AuraSurfaceBorder
import com.example.ui.theme.AuraSurfaceCard
import com.example.ui.theme.AuraSurfaceCardElevated
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.AuraVioletSecondary
import com.example.ui.theme.AuraWarningAmber
import com.example.voice.VoiceState

@Composable
fun HomeScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val messages by viewModel.memory.conversationHistory.collectAsState()
    val isDemoMode by viewModel.auraCore.isDemoMode.collectAsState()
    val isProcessing by viewModel.auraCore.isProcessing.collectAsState()
    val activeTask by viewModel.taskManager.activeTask.collectAsState()
    val voiceState by viewModel.voiceManager.voiceState.collectAsState()
    val audioRms by viewModel.voiceManager.audioRms.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val voiceError by viewModel.voiceErrorMessage.collectAsState()

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDarkBackground)
    ) {
        // --- Top Header ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isProcessing) AuraCyanPrimary else AuraSuccessEmerald)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AURA AI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AuraTextPrimary,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Multimodal Personal Assistant Agent",
                    fontSize = 11.sp,
                    color = AuraTextMuted
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Demo vs Real Mode Toggle Chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDemoMode) AuraWarningAmber.copy(alpha = 0.15f) else AuraSuccessEmerald.copy(alpha = 0.15f))
                    .border(
                        1.dp,
                        if (isDemoMode) AuraWarningAmber.copy(alpha = 0.5f) else AuraSuccessEmerald.copy(alpha = 0.5f),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { viewModel.toggleDemoMode(!isDemoMode) }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("mode_toggle_chip")
            ) {
                Text(
                    text = if (isDemoMode) "DEMO MODE" else "REAL MODE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDemoMode) AuraWarningAmber else AuraSuccessEmerald
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Voice Speech Toggle Button
            IconButton(
                onClick = {
                    viewModel.voiceManager.isTtsEnabled = !viewModel.voiceManager.isTtsEnabled
                    if (!viewModel.voiceManager.isTtsEnabled) {
                        viewModel.voiceManager.stopSpeaking()
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (viewModel.voiceManager.isTtsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Toggle Speech",
                    tint = if (viewModel.voiceManager.isTtsEnabled) AuraCyanPrimary else AuraTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // --- Voice Error Notification if any ---
        if (voiceError != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AuraDangerCrimson.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = voiceError ?: "",
                    fontSize = 12.sp,
                    color = AuraDangerCrimson,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = AuraDangerCrimson,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { viewModel.clearVoiceError() }
                )
            }
        }

        // --- Active Task Live Bar ---
        if (activeTask != null && (activeTask?.status == TaskStatus.EXECUTING || activeTask?.status == TaskStatus.PLANNING || activeTask?.status == TaskStatus.WAITING_CONFIRMATION)) {
            val task = activeTask!!
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AuraSurfaceCard)
                    .border(1.dp, AuraCyanPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("active_task_bar")
            ) {
                CircularProgressIndicator(
                    color = AuraCyanPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TASK IN PROGRESS • ${task.status.name}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraCyanPrimary
                    )
                    Text(
                        text = task.userRequest,
                        fontSize = 12.sp,
                        color = AuraTextPrimary,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AuraDangerCrimson.copy(alpha = 0.2f))
                        .clickable { viewModel.cancelActiveTask() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "CANCEL",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraDangerCrimson
                    )
                }
            }
        }

        // --- Main Chat & Interactive Feed ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Orb in Header of Stream
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    AuraOrb(
                        voiceState = voiceState,
                        audioRms = audioRms,
                        isProcessing = isProcessing,
                        onOrbClick = { viewModel.toggleVoiceListening() }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Action Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        val quickActions = listOf(
                            "Google kholo",
                            "YouTube kholo aur kids ABC learning search karo",
                            "Ali ko call karo",
                            "Kal subah 7 baje alarm laga do",
                            "Pakistan ka weather search karo",
                            "Device status batao",
                            "Facebook post tayar karo"
                        )
                        items(quickActions) { action ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AuraSurfaceCardElevated)
                                    .border(1.dp, AuraSurfaceBorder, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.sendPrompt(action) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("quick_action_${action.take(10)}")
                            ) {
                                Text(
                                    text = action,
                                    fontSize = 12.sp,
                                    color = AuraCyanPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Chat Messages
            items(messages) { msg ->
                val isUser = msg.sender == MessageSender.USER
                Column(
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Sender Label
                    Text(
                        text = if (isUser) "YOU" else "AURA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUser) AuraVioletSecondary else AuraCyanPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    // Message Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (isUser) 0.85f else 1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isUser) AuraSurfaceCardElevated else AuraSurfaceCard)
                            .border(
                                1.dp,
                                if (isUser) AuraVioletSecondary.copy(alpha = 0.4f) else AuraSurfaceBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = msg.text,
                                fontSize = 14.sp,
                                color = AuraTextPrimary,
                                lineHeight = 20.sp
                            )

                            // Render Tool Execution Badges if any
                            if (msg.toolResults.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (toolRes in msg.toolResults) {
                                        ToolResultBadge(result = toolRes)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Bottom Command & Voice Bar ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(AuraSurfaceCard)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Voice Mic Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (voiceState == VoiceState.LISTENING) AuraDangerCrimson
                        else AuraCyanPrimary.copy(alpha = 0.2f)
                    )
                    .border(
                        1.5.dp,
                        if (voiceState == VoiceState.LISTENING) AuraDangerCrimson else AuraCyanPrimary,
                        CircleShape
                    )
                    .clickable { viewModel.toggleVoiceListening() }
                    .testTag("home_voice_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Voice Input",
                    tint = if (voiceState == VoiceState.LISTENING) Color.White else AuraCyanPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Command Text Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.updateInputText(it) },
                placeholder = {
                    Text(
                        text = "Ask AURA (English, Roman Urdu, Urdu)...",
                        fontSize = 12.sp,
                        color = AuraTextMuted
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuraCyanPrimary,
                    unfocusedBorderColor = AuraSurfaceBorder,
                    focusedTextColor = AuraTextPrimary,
                    unfocusedTextColor = AuraTextPrimary,
                    focusedContainerColor = AuraDarkBackground,
                    unfocusedContainerColor = AuraDarkBackground
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.sendCurrentPrompt() })
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(AuraCyanPrimary)
                    .clickable { viewModel.sendCurrentPrompt() }
                    .testTag("send_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send Command",
                    tint = Color(0xFF001F28),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
