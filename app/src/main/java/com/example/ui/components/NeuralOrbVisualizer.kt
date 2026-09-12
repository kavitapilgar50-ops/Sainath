package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantState
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VioletNeon
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NeuralOrbVisualizer(
    state: AssistantState,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    onClick: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbPulse")

    // Breathing pulse for idle & speaking
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = when (state) {
            AssistantState.IDLE -> 0.95f
            AssistantState.LISTENING -> 0.92f
            AssistantState.THINKING -> 0.88f
            AssistantState.SPEAKING -> 0.85f
        },
        targetValue = when (state) {
            AssistantState.IDLE -> 1.05f
            AssistantState.LISTENING -> 1.15f
            AssistantState.THINKING -> 1.12f
            AssistantState.SPEAKING -> 1.25f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.IDLE -> 2400
                    AssistantState.LISTENING -> 1000
                    AssistantState.THINKING -> 700
                    AssistantState.SPEAKING -> 600
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Rotation for thinking orbital rings
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    AssistantState.THINKING -> 2000
                    AssistantState.SPEAKING -> 4000
                    else -> 10000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationAngle"
    )

    // Secondary wave ripple for speaking
    val waveRipple by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "waveRipple"
    )

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .testTag("neural_orb_visualizer")
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val center = Offset(size.toPx() / 2f, size.toPx() / 2f)
            val baseRadius = (size.toPx() / 2f) * 0.65f

            // 1. Outer dynamic glow/soundwave rings
            when (state) {
                AssistantState.SPEAKING -> {
                    // Radiating audio acoustic pulses
                    for (i in 1..3) {
                        val ringRadius = baseRadius * (1f + (waveRipple + i * 0.3f) % 1.2f)
                        val ringAlpha = ((1.2f - ((waveRipple + i * 0.3f) % 1.2f)) / 1.2f).coerceIn(0f, 0.6f)
                        drawCircle(
                            color = NeonCyan.copy(alpha = ringAlpha * 0.4f),
                            radius = ringRadius,
                            center = center,
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                }
                AssistantState.LISTENING -> {
                    // Pulsing listening rings
                    drawCircle(
                        color = VioletNeon.copy(alpha = 0.35f),
                        radius = baseRadius * pulseScale * 1.2f,
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.2f),
                        radius = baseRadius * pulseScale * 1.35f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
                AssistantState.THINKING -> {
                    // Orbital rings with satellite nodes
                    val rad = Math.toRadians(rotationAngle.toDouble())
                    val orbitalRadius = baseRadius * 1.25f
                    val satellite1 = Offset(
                        (center.x + cos(rad) * orbitalRadius).toFloat(),
                        (center.y + sin(rad) * orbitalRadius).toFloat()
                    )
                    val satellite2 = Offset(
                        (center.x - cos(rad) * orbitalRadius).toFloat(),
                        (center.y - sin(rad) * orbitalRadius).toFloat()
                    )

                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(NeonCyan, VioletNeon, ElectricIndigo, NeonCyan)
                        ),
                        radius = orbitalRadius,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )

                    drawCircle(color = NeonCyan, radius = 4.dp.toPx(), center = satellite1)
                    drawCircle(color = VioletNeon, radius = 4.dp.toPx(), center = satellite2)
                }
                AssistantState.IDLE -> {
                    // Soft halo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(NeonCyan.copy(alpha = 0.25f), Color.Transparent),
                            center = center,
                            radius = baseRadius * 1.3f
                        ),
                        radius = baseRadius * 1.3f,
                        center = center
                    )
                }
            }

            // 2. Core glowing orb body
            val coreRadius = baseRadius * pulseScale

            val coreBrush = when (state) {
                AssistantState.IDLE -> Brush.radialGradient(
                    colors = listOf(CyanGlow, NeonCyan, ElectricIndigo, Color(0xFF0F172A)),
                    center = center.minus(Offset(coreRadius * 0.2f, coreRadius * 0.2f)),
                    radius = coreRadius * 1.2f
                )
                AssistantState.LISTENING -> Brush.radialGradient(
                    colors = listOf(Color.White, NeonCyan, VioletNeon, ElectricIndigo),
                    center = center,
                    radius = coreRadius * 1.2f
                )
                AssistantState.THINKING -> Brush.radialGradient(
                    colors = listOf(CyanGlow, VioletNeon, ElectricIndigo, Color(0xFF1E1B4B)),
                    center = center,
                    radius = coreRadius * 1.2f
                )
                AssistantState.SPEAKING -> Brush.radialGradient(
                    colors = listOf(Color.White, NeonCyan, VioletNeon, ElectricIndigo),
                    center = center,
                    radius = coreRadius * 1.3f
                )
            }

            // Draw core shadow / outer glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        when (state) {
                            AssistantState.SPEAKING -> NeonCyan.copy(alpha = 0.6f)
                            AssistantState.THINKING -> VioletNeon.copy(alpha = 0.5f)
                            AssistantState.LISTENING -> NeonCyan.copy(alpha = 0.5f)
                            AssistantState.IDLE -> NeonCyan.copy(alpha = 0.25f)
                        },
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius * 1.6f
                ),
                radius = coreRadius * 1.6f,
                center = center
            )

            // Draw core sphere
            drawCircle(
                brush = coreBrush,
                radius = coreRadius,
                center = center
            )

            // Inner gloss highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.65f), Color.Transparent),
                    center = center.minus(Offset(coreRadius * 0.35f, coreRadius * 0.35f)),
                    radius = coreRadius * 0.6f
                ),
                radius = coreRadius * 0.5f,
                center = center.minus(Offset(coreRadius * 0.2f, coreRadius * 0.2f))
            )
        }
    }
}
