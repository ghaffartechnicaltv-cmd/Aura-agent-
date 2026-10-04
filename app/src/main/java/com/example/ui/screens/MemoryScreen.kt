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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
fun MemoryScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val isEnabled by viewModel.memory.isMemoryEnabled.collectAsState()
    val longTermMemories by viewModel.memory.longTermMemories.collectAsState()
    val recentToolResults by viewModel.memory.recentToolResults.collectAsState()

    var newKey by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf("") }
    var memorySaveError by remember { mutableStateOf<String?>(null) }

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
                    text = "AURA MEMORY ENGINE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraCyanPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Context & Preferences",
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
            // Memory Toggle Card
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AuraSurfaceCard)
                        .border(1.dp, AuraSurfaceBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Long-Term Memory",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraTextPrimary
                        )
                        Text(
                            text = if (isEnabled) "Active: AURA retains user preferences." else "Disabled: No preferences stored.",
                            fontSize = 12.sp,
                            color = AuraTextSecondary
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { viewModel.memory.toggleMemory(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF001F28),
                            checkedTrackColor = AuraCyanPrimary,
                            uncheckedThumbColor = AuraTextMuted,
                            uncheckedTrackColor = AuraSurfaceCardElevated
                        ),
                        modifier = Modifier.testTag("memory_toggle_switch")
                    )
                }
            }

            // Security & Privacy Policy Banner
            item {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuraSuccessEmerald.copy(alpha = 0.08f))
                        .border(1.dp, AuraSuccessEmerald.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = AuraSuccessEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "ZERO SENSITIVE DATA GUARANTEE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraSuccessEmerald
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "AURA Action Guard strictly rejects storing passwords, OTP codes, authentication tokens, API secrets, or credit card details in memory.",
                            fontSize = 11.sp,
                            color = AuraTextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Add Memory Entry Form
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AuraSurfaceCard)
                        .border(1.dp, AuraSurfaceBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "ADD PREFERENCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraCyanPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newKey,
                        onValueChange = { newKey = it; memorySaveError = null },
                        placeholder = { Text("Preference key (e.g. preferred_alarm_time)", fontSize = 12.sp, color = AuraTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraCyanPrimary,
                            unfocusedBorderColor = AuraSurfaceBorder,
                            focusedTextColor = AuraTextPrimary,
                            unfocusedTextColor = AuraTextPrimary,
                            focusedContainerColor = AuraSurfaceCardElevated,
                            unfocusedContainerColor = AuraSurfaceCardElevated
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it; memorySaveError = null },
                        placeholder = { Text("Value (e.g. 07:00 AM weekdays)", fontSize = 12.sp, color = AuraTextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraCyanPrimary,
                            unfocusedBorderColor = AuraSurfaceBorder,
                            focusedTextColor = AuraTextPrimary,
                            unfocusedTextColor = AuraTextPrimary,
                            focusedContainerColor = AuraSurfaceCardElevated,
                            unfocusedContainerColor = AuraSurfaceCardElevated
                        ),
                        singleLine = true
                    )

                    if (memorySaveError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = memorySaveError!!,
                            fontSize = 11.sp,
                            color = AuraDangerCrimson
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (newKey.isNotBlank() && newValue.isNotBlank()) {
                                val saved = viewModel.memory.savePreference(newKey.trim(), newValue.trim())
                                if (saved) {
                                    newKey = ""
                                    newValue = ""
                                    memorySaveError = null
                                } else {
                                    memorySaveError = "Blocked by Action Guard: Contains sensitive credentials pattern."
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("save_memory_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuraCyanPrimary,
                            contentColor = Color(0xFF001F28)
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Save to Long-Term Memory", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Stored Long-Term Memories List
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "STORED USER PREFERENCES (${longTermMemories.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraTextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (longTermMemories.isNotEmpty()) {
                        Text(
                            text = "Clear All",
                            fontSize = 11.sp,
                            color = AuraDangerCrimson,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { viewModel.memory.clearAllLongTermMemories() }
                        )
                    }
                }
            }

            items(longTermMemories) { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AuraSurfaceCard)
                        .border(1.dp, AuraSurfaceBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.key,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraCyanPrimary
                        )
                        Text(
                            text = entry.value,
                            fontSize = 13.sp,
                            color = AuraTextPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.memory.deleteMemory(entry.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Memory",
                            tint = AuraDangerCrimson,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
