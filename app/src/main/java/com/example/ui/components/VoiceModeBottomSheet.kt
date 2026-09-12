package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantState
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceModeBottomSheet(
    onDismiss: () -> Unit,
    state: AssistantState,
    liveTranscript: String,
    lastMyraResponse: String,
    onMicClick: () -> Unit,
    onStopSpeechClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DeepSpace,
        modifier = Modifier.testTag("gemini_live_voice_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (state) {
                                    AssistantState.LISTENING -> AccentCoral
                                    AssistantState.THINKING -> VioletNeon
                                    AssistantState.SPEAKING -> NeonCyan
                                    AssistantState.IDLE -> TextSecondary
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (state) {
                            AssistantState.LISTENING -> "सुन रही हूँ (Listening)..."
                            AssistantState.THINKING -> "सोच रही हूँ (Thinking)..."
                            AssistantState.SPEAKING -> "बोल रही हूँ (Speaking)..."
                            AssistantState.IDLE -> "MYRA Live तैयार है (Ready)"
                        },
                        color = when (state) {
                            AssistantState.LISTENING -> AccentCoral
                            AssistantState.THINKING -> VioletNeon
                            AssistantState.SPEAKING -> NeonCyan
                            AssistantState.IDLE -> TextSecondary
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            // Central Pulsing Orb
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                NeuralOrbVisualizer(
                    state = state,
                    size = 180.dp,
                    onClick = {
                        if (state == AssistantState.SPEAKING) {
                            onStopSpeechClick()
                        } else {
                            onMicClick()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "MYRA Live Audio",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Hinglish • Gemini Voice Protocol",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Live Transcript Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val displayText = when {
                        state == AssistantState.LISTENING && liveTranscript.isNotBlank() -> "आप: \"$liveTranscript\""
                        state == AssistantState.LISTENING -> "बोलिए, मैं सुन रही हूँ..."
                        state == AssistantState.THINKING -> "रिसर्च और उत्तर तैयार कर रही हूँ..."
                        state == AssistantState.SPEAKING && lastMyraResponse.isNotBlank() -> lastMyraResponse
                        else -> "माइक बटन दबाएँ और कुछ भी पूछें — मौसम, खबरें, टेक, या कोडिंग।"
                    }

                    Text(
                        text = displayText,
                        color = if (state == AssistantState.LISTENING && liveTranscript.isNotBlank()) NeonCyan else TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 4
                    )
                }
            }

            // Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Keyboard / Text Switch
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .border(1.dp, CardBorder, CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.Keyboard, contentDescription = "Text Mode", tint = TextSecondary)
                }

                // Primary Mic Button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(if (state == AssistantState.LISTENING) AccentCoral else NeonCyan)
                        .border(2.dp, if (state == AssistantState.LISTENING) AccentCoral else NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onMicClick,
                        modifier = Modifier.size(68.dp).testTag("voice_mode_mic_button")
                    ) {
                        Icon(
                            imageVector = if (state == AssistantState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = DeepSpace,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Stop Audio Playback Button
                IconButton(
                    onClick = onStopSpeechClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .border(1.dp, CardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = if (state == AssistantState.SPEAKING) NeonCyan else TextSecondary
                    )
                }
            }
        }
    }
}
