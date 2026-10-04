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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RiskLevel
import com.example.model.ToolResult
import com.example.tools.AuraTool
import com.example.ui.MainViewModel
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

@Composable
fun ToolsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val allTools = remember { viewModel.toolRouter.getAllTools() }
    var selectedCategory by remember { mutableStateOf("All") }
    var lastTestResult by remember { mutableStateOf<ToolResult?>(null) }

    val categories = listOf("All", "Device", "Browser", "Computer Use", "Web Research", "Communication", "Social Media")

    val filteredTools = remember(selectedCategory) {
        when (selectedCategory) {
            "Device" -> allTools.filter { it.name.startsWith("AURA.OpenApp") || it.name.startsWith("AURA.FindContact") || it.name.startsWith("AURA.Call") || it.name.startsWith("AURA.SetAlarm") || it.name.startsWith("AURA.CancelAlarm") || it.name.startsWith("AURA.OpenSettings") || it.name.startsWith("AURA.Device") }
            "Browser" -> allTools.filter { it.name.contains("Browser") || it.name.contains("Navigate") || it.name.contains("WebSearch") || it.name.contains("ReadPage") || it.name.contains("Click") || it.name.contains("Type") || it.name.contains("Scroll") || it.name.contains("GoBack") || it.name.contains("Tab") || it.name.contains("Screenshot") || it.name.contains("VerifyPage") }
            "Computer Use" -> allTools.filter { it.name.contains("Screen") || it.name.contains("Key") || it.name.contains("Swipe") || it.name.contains("Wait") }
            "Web Research" -> allTools.filter { it.name.contains("Search") || it.name.contains("Compare") || it.name.contains("Summarize") }
            "Communication" -> allTools.filter { it.name.contains("Message") || it.name.contains("InitiateCall") }
            "Social Media" -> allTools.filter { it.name.contains("Social") || it.name.contains("Post") }
            else -> allTools
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDarkBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = "AURA TOOL SYSTEM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraCyanPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Tool Registry (${allTools.size} Tools)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCategory
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) AuraCyanPrimary else AuraSurfaceCardElevated)
                        .border(1.dp, if (isSelected) AuraCyanPrimary else AuraSurfaceBorder, RoundedCornerShape(12.dp))
                        .clickable { selectedCategory = cat }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF001F28) else AuraTextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Last Test Result Banner if any
        if (lastTestResult != null) {
            val res = lastTestResult!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AuraSurfaceCard)
                    .border(1.dp, AuraCyanPrimary, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "SANDBOX RESULT: ${res.tool}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraCyanPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    ToolResultBadge(result = res)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = res.message,
                    fontSize = 12.sp,
                    color = AuraTextPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tool List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredTools) { tool ->
                ToolDirectoryCard(
                    tool = tool,
                    onTestTool = {
                        val testArgs = when (tool.name) {
                            "AURA.OpenApp" -> mapOf("appName" to "YouTube")
                            "AURA.WebSearch" -> mapOf("query" to "Pakistan weather")
                            "AURA.SetAlarm" -> mapOf("time" to "07:00 AM", "label" to "Morning Alarm")
                            "AURA.FindContact" -> mapOf("name" to "Ali")
                            "AURA.Call" -> mapOf("phoneNumber" to "+1 (555) 019-2834")
                            "AURA.DeviceTime" -> emptyMap()
                            "AURA.DeviceStatus" -> emptyMap()
                            "AURA.PreparePost" -> mapOf("platform" to "Twitter", "content" to "Testing AURA Agent!")
                            "AURA.Screen" -> emptyMap()
                            else -> mapOf("param" to "test")
                        }
                        viewModel.sendPrompt("Test ${tool.name}")
                    }
                )
            }
        }
    }
}

@Composable
fun ToolDirectoryCard(tool: AuraTool, onTestTool: () -> Unit) {
    val riskColor = when (tool.riskLevel) {
        RiskLevel.SAFE -> AuraSuccessEmerald
        RiskLevel.CONFIRM -> AuraWarningAmber
        RiskLevel.BLOCK -> AuraDangerCrimson
        RiskLevel.UNAVAILABLE -> Color(0xFF94A3B8)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AuraSurfaceCard)
            .border(1.dp, AuraSurfaceBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = tool.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AuraCyanPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(riskColor.copy(alpha = 0.15f))
                    .border(1.dp, riskColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tool.riskLevel.name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = riskColor
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = tool.description,
            fontSize = 12.sp,
            color = AuraTextSecondary,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Permission: ${tool.permission}",
                fontSize = 10.sp,
                color = AuraTextMuted
            )
            if (tool.requiresConfirmation) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• Requires Confirmation",
                    fontSize = 10.sp,
                    color = AuraWarningAmber
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(
                onClick = onTestTool,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = AuraCyanPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Trigger", fontSize = 10.sp, color = AuraCyanPrimary)
            }
        }
    }
}
