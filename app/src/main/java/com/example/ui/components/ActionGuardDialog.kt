package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.PendingConfirmation
import com.example.ui.theme.AuraCyanPrimary
import com.example.ui.theme.AuraDangerCrimson
import com.example.ui.theme.AuraSurfaceBorder
import com.example.ui.theme.AuraSurfaceCard
import com.example.ui.theme.AuraSurfaceCardElevated
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.AuraWarningAmber

@Composable
fun ActionGuardDialog(
    confirmation: PendingConfirmation,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AuraSurfaceCard)
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(AuraWarningAmber, AuraCyanPrimary)),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(24.dp)
                .testTag("action_guard_dialog")
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AuraWarningAmber.copy(alpha = 0.15f))
                            .border(1.dp, AuraWarningAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "AURA Action Guard",
                            tint = AuraWarningAmber,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "AURA ACTION GUARD",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraWarningAmber,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Confirm Action",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Details Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AuraSurfaceCardElevated)
                        .border(1.dp, AuraSurfaceBorder, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "ACTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraTextMuted
                    )
                    Text(
                        text = confirmation.toolName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AuraCyanPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "TARGET",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraTextMuted
                    )
                    Text(
                        text = confirmation.target,
                        fontSize = 13.sp,
                        color = AuraTextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "DETAILS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraTextMuted
                    )
                    Text(
                        text = confirmation.details,
                        fontSize = 13.sp,
                        color = AuraTextSecondary,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Safety Warning Note
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = AuraWarningAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "This action has real-world consequences and will not execute without your permission.",
                        fontSize = 11.sp,
                        color = AuraTextMuted,
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Actions: Cancel & Confirm
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_guard_cancel_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AuraDangerCrimson
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = Brush.horizontalGradient(listOf(AuraDangerCrimson, AuraDangerCrimson))
                        )
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_guard_confirm_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AuraCyanPrimary,
                            contentColor = Color(0xFF001F28)
                        )
                    ) {
                        Text(
                            text = "Confirm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
