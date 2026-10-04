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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.ui.MainViewModel
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
import com.example.ui.theme.AuraWarningAmber

@Composable
fun SettingsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val isDemoMode by viewModel.auraCore.isDemoMode.collectAsState()
    val virtualPerms by viewModel.permissionManager.virtualPermissions.collectAsState()
    val isKeyConfigured = viewModel.geminiService.isApiKeyConfigured()

    var selectedLanguage by remember { mutableStateOf("Auto (Urdu / Roman Urdu / English / Hindi)") }
    val languages = listOf(
        "Auto (Urdu / Roman Urdu / English / Hindi)",
        "Roman Urdu (e.g. Google kholo, Alarm laga do)",
        "Urdu (اردو)",
        "English (US)",
        "Hindi (हिंदी)"
    )

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
                    text = "AURA SETTINGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraCyanPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "System & Security",
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Demo Mode Toggle
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AuraSurfaceCard)
                        .border(
                            1.dp,
                            if (isDemoMode) AuraWarningAmber else AuraSurfaceBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Demo Mode",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDemoMode) AuraWarningAmber else AuraTextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isDemoMode) AuraWarningAmber.copy(alpha = 0.15f) else AuraSuccessEmerald.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isDemoMode) "SIMULATED" else "REAL APIS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDemoMode) AuraWarningAmber else AuraSuccessEmerald
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isDemoMode)
                                    "When ON, tool actions are simulated as DEMO ACTIONS without touching real device alarms, calls, or settings."
                                else
                                    "When OFF, AURA calls real Android platform APIs and verified external services.",
                                fontSize = 12.sp,
                                color = AuraTextSecondary
                            )
                        }
                        Switch(
                            checked = isDemoMode,
                            onCheckedChange = { viewModel.toggleDemoMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF001F28),
                                checkedTrackColor = AuraWarningAmber,
                                uncheckedThumbColor = AuraCyanPrimary,
                                uncheckedTrackColor = AuraSurfaceCardElevated
                            ),
                            modifier = Modifier.testTag("demo_mode_switch")
                        )
                    }
                }
            }

            // 2. Gemini Reasoning Model & Key Status
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AuraSurfaceCard)
                        .border(1.dp, AuraSurfaceBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = AuraCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI REASONING MODEL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraCyanPrimary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = viewModel.geminiService.activeModel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AuraTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AuraSurfaceCardElevated)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isKeyConfigured) Icons.Default.CheckCircle else Icons.Default.Key,
                            contentDescription = null,
                            tint = if (isKeyConfigured) AuraSuccessEmerald else AuraWarningAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isKeyConfigured) "Gemini API Key Connected" else "Default Local Multilingual Agent Engine",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AuraTextPrimary
                            )
                            Text(
                                text = if (isKeyConfigured) "Cloud reasoning with Gemini 3.5 Flash" else "Full agentic reasoning, multilingual parsing & tool execution active.",
                                fontSize = 11.sp,
                                color = AuraTextMuted
                            )
                        }
                    }
                }
            }

            // 3. Language Selection
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AuraSurfaceCard)
                        .border(1.dp, AuraSurfaceBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = AuraCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LANGUAGE & VOCAL NLU",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraCyanPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (lang in languages) {
                            val isSel = lang == selectedLanguage
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) AuraCyanPrimary.copy(alpha = 0.15f) else AuraSurfaceCardElevated)
                                    .border(
                                        1.dp,
                                        if (isSel) AuraCyanPrimary.copy(alpha = 0.6f) else AuraSurfaceBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedLanguage = lang }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = lang,
                                    fontSize = 13.sp,
                                    color = if (isSel) AuraCyanPrimary else AuraTextPrimary,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (isSel) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AuraCyanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Permission Controls
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AuraSurfaceCard)
                        .border(1.dp, AuraSurfaceBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AuraCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AURA PERMISSION MANAGER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraCyanPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val permissionItems = listOf(
                        Triple("device.app_control", "Device App Control", "Launch and control installed apps"),
                        Triple("browser.control", "Browser Automation", "Navigate, search, and inspect pages"),
                        Triple("computer.use", "Computer Use Interface", "Tap coordinates, type text, swipe"),
                        Triple("device.call", "Telephony & Calls", "Initiate calls and prepare SMS"),
                        Triple("device.contacts", "Contacts Access", "Resolve names and numbers"),
                        Triple("device.alarm", "Android Alarm System", "Set and cancel alarms")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for ((key, title, desc) in permissionItems) {
                            val granted = virtualPerms[key] ?: true
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AuraSurfaceCardElevated)
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AuraTextPrimary
                                    )
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        color = AuraTextMuted
                                    )
                                }
                                Switch(
                                    checked = granted,
                                    onCheckedChange = { viewModel.permissionManager.setVirtualPermission(key, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF001F28),
                                        checkedTrackColor = AuraCyanPrimary,
                                        uncheckedThumbColor = AuraTextMuted,
                                        uncheckedTrackColor = AuraSurfaceCard
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 5. Action Guard Security Architecture Details
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AuraSurfaceCard)
                        .border(1.dp, AuraSurfaceBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "AURA ACTION GUARD SPECIFICATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraCyanPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• SAFE: Read-only, Web Search, Open App, Public Info\n" +
                                "• CONFIRM: Calls, SMS, Post Publication, Alarm Cancellation\n" +
                                "• BLOCK: 2FA/CAPTCHA bypass, Credential exfiltration, Prompt Injection\n" +
                                "• UNAVAILABLE: Tool or hardware interface absent on device\n" +
                                "• NO FAKE EXECUTION: Verified against actual system APIs",
                        fontSize = 12.sp,
                        color = AuraTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}
