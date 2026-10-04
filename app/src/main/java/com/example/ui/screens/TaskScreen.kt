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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AuraTask
import com.example.model.TaskStatus
import com.example.model.TaskStepStatus
import com.example.ui.MainViewModel
import com.example.ui.components.ScreenStateCard
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val activeTask by viewModel.taskManager.activeTask.collectAsState()
    val allTasks by viewModel.taskManager.tasks.collectAsState()
    val screenState by viewModel.memory.currentScreenState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "TASK MANAGER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraCyanPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Multi-Step Execution Pipeline",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Active Task Live Card
            if (activeTask != null) {
                item {
                    ActiveTaskDetailCard(
                        task = activeTask!!,
                        onCancelTask = { viewModel.cancelActiveTask() }
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AuraSurfaceCard)
                            .border(1.dp, AuraSurfaceBorder, RoundedCornerShape(14.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                tint = AuraTextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No active task running.",
                                fontSize = 14.sp,
                                color = AuraTextMuted
                            )
                            Text(
                                text = "Submit a request on the Home tab to start a task.",
                                fontSize = 12.sp,
                                color = AuraTextMuted
                            )
                        }
                    }
                }
            }

            // 2. Real-time Screen State (Computer Use & Browser State Inspector)
            item {
                ScreenStateCard(screenState = screenState)
            }

            // 3. Task History Header
            item {
                Text(
                    text = "TASK EXECUTION HISTORY (${allTasks.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTextMuted,
                    letterSpacing = 0.8.sp
                )
            }

            // 4. Task History Items
            items(allTasks) { task ->
                TaskHistoryCard(task = task)
            }
        }
    }
}

@Composable
fun ActiveTaskDetailCard(task: AuraTask, onCancelTask: () -> Unit) {
    val statusColor = when (task.status) {
        TaskStatus.PLANNING -> AuraVioletSecondary
        TaskStatus.EXECUTING -> AuraCyanPrimary
        TaskStatus.WAITING_CONFIRMATION -> AuraWarningAmber
        TaskStatus.WAITING_USER -> AuraWarningAmber
        TaskStatus.COMPLETED -> AuraSuccessEmerald
        TaskStatus.FAILED -> AuraDangerCrimson
        TaskStatus.CANCELLED -> Color(0xFF94A3B8)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AuraSurfaceCard)
            .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("active_task_detail_card")
    ) {
        // Status Badge + Task ID
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = task.status.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = task.taskId,
                fontSize = 11.sp,
                color = AuraTextMuted,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))

            if (task.status == TaskStatus.EXECUTING || task.status == TaskStatus.PLANNING || task.status == TaskStatus.WAITING_CONFIRMATION) {
                OutlinedButton(
                    onClick = onCancelTask,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraDangerCrimson),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(text = "Cancel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Goal
        Text(
            text = task.userRequest,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AuraTextPrimary
        )

        Text(
            text = "Language detected: ${task.languageDetected}",
            fontSize = 11.sp,
            color = AuraTextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Step Pipeline
        Text(
            text = "EXECUTION PLAN (${task.plan.size} STEPS):",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = AuraCyanPrimary,
            letterSpacing = 0.8.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            task.plan.forEachIndexed { index, step ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AuraSurfaceCardElevated)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Step Status Icon
                    when (step.status) {
                        TaskStepStatus.COMPLETED -> Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AuraSuccessEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        TaskStepStatus.RUNNING -> CircularProgressIndicator(
                            color = AuraCyanPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        TaskStepStatus.FAILED -> Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = null,
                            tint = AuraDangerCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                        else -> Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, AuraTextMuted, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 9.sp,
                                color = AuraTextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = step.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AuraTextPrimary
                        )
                        Text(
                            text = step.toolName,
                            fontSize = 10.sp,
                            color = AuraCyanPrimary
                        )
                        if (step.result != null) {
                            Text(
                                text = step.result?.message ?: "",
                                fontSize = 11.sp,
                                color = AuraTextSecondary,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }

        // Verification Summary if completed
        if (task.verificationSummary.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AuraSuccessEmerald.copy(alpha = 0.1f))
                    .border(1.dp, AuraSuccessEmerald.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "Verification: ${task.verificationSummary}",
                    fontSize = 11.sp,
                    color = AuraSuccessEmerald,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun TaskHistoryCard(task: AuraTask) {
    val dateStr = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(task.createdAt))
    val isSuccess = task.status == TaskStatus.COMPLETED

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AuraSurfaceCard)
            .border(1.dp, AuraSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSuccess) AuraSuccessEmerald.copy(alpha = 0.15f) else AuraDangerCrimson.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = task.status.name,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess) AuraSuccessEmerald else AuraDangerCrimson
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = dateStr,
                fontSize = 11.sp,
                color = AuraTextMuted
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${task.toolCalls.size} tool calls",
                fontSize = 10.sp,
                color = AuraCyanPrimary
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = task.userRequest,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AuraTextPrimary
        )

        if (task.toolCalls.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            for (toolRes in task.toolCalls.take(2)) {
                Text(
                    text = "• ${toolRes.tool}: ${toolRes.message.take(80)}...",
                    fontSize = 11.sp,
                    color = AuraTextMuted
                )
            }
        }
    }
}
