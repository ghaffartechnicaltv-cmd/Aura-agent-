package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExecutionMode
import com.example.model.ExecutionStatus
import com.example.model.ToolResult
import com.example.ui.theme.AuraCyanPrimary
import com.example.ui.theme.AuraDangerCrimson
import com.example.ui.theme.AuraSuccessEmerald
import com.example.ui.theme.AuraWarningAmber

@Composable
fun ToolResultBadge(result: ToolResult, modifier: Modifier = Modifier) {
    val isDemo = result.mode == ExecutionMode.DEMO
    val isSuccess = result.status == ExecutionStatus.SUCCESS
    val isUnavail = result.status == ExecutionStatus.UNAVAILABLE

    val badgeColor = when {
        isDemo -> AuraWarningAmber
        isUnavail -> Color(0xFF94A3B8)
        isSuccess -> AuraSuccessEmerald
        else -> AuraDangerCrimson
    }

    val badgeText = when {
        isDemo -> "DEMO ACTION"
        isUnavail -> "UNAVAILABLE"
        isSuccess -> "REAL EXECUTION"
        else -> "FAILED"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(badgeColor.copy(alpha = 0.12f))
            .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Icon(
            imageVector = when {
                isDemo -> Icons.Default.Info
                isSuccess -> Icons.Default.CheckCircle
                else -> Icons.Default.Error
            },
            contentDescription = null,
            tint = badgeColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = badgeText,
            color = badgeColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        if (result.verified) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "• VERIFIED",
                color = AuraCyanPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
