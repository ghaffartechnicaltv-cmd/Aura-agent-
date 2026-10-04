package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScreenState
import com.example.ui.theme.AuraCyanPrimary
import com.example.ui.theme.AuraSurfaceBorder
import com.example.ui.theme.AuraSurfaceCard
import com.example.ui.theme.AuraSurfaceCardElevated
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenStateCard(screenState: ScreenState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AuraSurfaceCard)
            .border(1.dp, AuraSurfaceBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = when (screenState.environment.lowercase()) {
                    "browser" -> Icons.Default.Language
                    "desktop" -> Icons.Default.Computer
                    else -> Icons.Default.Smartphone
                },
                contentDescription = null,
                tint = AuraCyanPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "SCREEN STATE • [${screenState.environment.uppercase()}]",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AuraCyanPrimary,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = screenState.status.uppercase(),
                fontSize = 10.sp,
                color = AuraTextMuted,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = screenState.screen,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = AuraTextPrimary
        )

        if (screenState.url.isNotEmpty()) {
            Text(
                text = screenState.url,
                fontSize = 11.sp,
                color = AuraTextMuted
            )
        }

        if (screenState.lastAction.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Last Action: ${screenState.lastAction}",
                fontSize = 12.sp,
                color = AuraTextSecondary
            )
        }

        if (screenState.visibleElements.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Visible Elements:",
                fontSize = 10.sp,
                color = AuraTextMuted,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (elem in screenState.visibleElements.take(6)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AuraSurfaceCardElevated)
                            .border(0.5.dp, AuraSurfaceBorder, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = elem,
                            fontSize = 10.sp,
                            color = AuraTextSecondary
                        )
                    }
                }
            }
        }
    }
}
