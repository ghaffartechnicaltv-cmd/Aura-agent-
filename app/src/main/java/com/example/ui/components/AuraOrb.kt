package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AuraCyanGlow
import com.example.ui.theme.AuraCyanPrimary
import com.example.ui.theme.AuraDangerCrimson
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraVioletGlow
import com.example.ui.theme.AuraVioletSecondary
import com.example.voice.VoiceState

@Composable
fun AuraOrb(
    voiceState: VoiceState,
    audioRms: Float,
    isProcessing: Boolean,
    onOrbClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraOrbTransitions")

    // Breathing pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbPulse"
    )

    // Rotation for processing
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    val rmsBoost = (audioRms.coerceIn(0f, 10f) / 10f) * 0.25f
    val currentScale = if (voiceState == VoiceState.LISTENING) pulseScale + rmsBoost else pulseScale

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOrbClick
                )
                .testTag("aura_orb"),
            contentAlignment = Alignment.Center
        ) {
            // Background Canvas: Radiant Pulsating Glow Rings
            Canvas(modifier = Modifier.size(160.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 2f) * 0.72f * currentScale

                // Outer ambient glow ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (voiceState == VoiceState.LISTENING) AuraDangerCrimson.copy(alpha = 0.35f)
                            else if (voiceState == VoiceState.SPEAKING) AuraCyanPrimary.copy(alpha = 0.4f)
                            else AuraVioletSecondary.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width / 2f
                    ),
                    radius = size.width / 2f
                )

                // Middle interactive ripple ring
                drawCircle(
                    color = when (voiceState) {
                        VoiceState.LISTENING -> AuraDangerCrimson.copy(alpha = 0.6f)
                        VoiceState.SPEAKING -> AuraCyanPrimary.copy(alpha = 0.7f)
                        else -> AuraCyanPrimary.copy(alpha = 0.3f)
                    },
                    radius = baseRadius * 1.15f,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Outer rotating dash arc for futuristic HUD feel
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(AuraCyanPrimary, AuraVioletSecondary, AuraCyanPrimary)
                    ),
                    startAngle = rotation,
                    sweepAngle = 240f,
                    useCenter = false,
                    topLeft = Offset(center.x - baseRadius * 1.25f, center.y - baseRadius * 1.25f),
                    size = androidx.compose.ui.geometry.Size(baseRadius * 2.5f, baseRadius * 2.5f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Core Orb Sphere
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = when (voiceState) {
                                VoiceState.LISTENING -> listOf(Color(0xFFFF5983), Color(0xFF900C3F), Color(0xFF1E0311))
                                VoiceState.SPEAKING -> listOf(Color(0xFF5CF2BD), Color(0xFF00B4D8), Color(0xFF021B35))
                                else -> listOf(Color(0xFF67E8F9), Color(0xFF7C3AED), Color(0xFF0A0F1D))
                            }
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.7f), Color.Transparent)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        voiceState == VoiceState.LISTENING -> Icons.Default.Mic
                        voiceState == VoiceState.SPEAKING -> Icons.AutoMirrored.Filled.VolumeUp
                        isProcessing -> Icons.Default.Stop
                        else -> Icons.Default.Mic
                    },
                    contentDescription = "AURA Assistant Status",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // State Label
        val stateText = when {
            isProcessing -> "AURA IS REASONING..."
            voiceState == VoiceState.LISTENING -> "LISTENING... (TAP TO STOP)"
            voiceState == VoiceState.PROCESSING -> "ANALYZING SPEECH..."
            voiceState == VoiceState.SPEAKING -> "SPEAKING... (TAP TO MUTE)"
            else -> "TAP OR TYPE TO ASK AURA"
        }

        val stateColor = when {
            isProcessing -> AuraCyanPrimary
            voiceState == VoiceState.LISTENING -> AuraDangerCrimson
            voiceState == VoiceState.SPEAKING -> AuraCyanPrimary
            else -> AuraTextMuted
        }

        Text(
            text = stateText,
            color = stateColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}
