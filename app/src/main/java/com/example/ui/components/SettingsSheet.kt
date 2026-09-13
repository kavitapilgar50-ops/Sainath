package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGrounded
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    currentModel: String,
    onModelChange: (String) -> Unit,
    autoSpeak: Boolean,
    onAutoSpeakChange: (Boolean) -> Unit,
    speechRate: Float,
    onSpeechRateChange: (Float) -> Unit,
    customApiKey: String,
    onCustomApiKeyChange: (String) -> Unit,
    onOpenPermissions: () -> Unit = {},
    onClearChatHistory: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var keyInput by remember { mutableStateOf(customApiKey) }
    var currentRate by remember { mutableFloatStateOf(speechRate) }

    val hasBuildConfigKey = remember {
        runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()?.let {
            it.isNotBlank() && it != "MY_GEMINI_API_KEY"
        } == true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        modifier = Modifier.testTag("settings_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MYRA Settings & Intelligence",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close settings",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. API Key Status & Override
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Key, contentDescription = "API Key", tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Gemini API Key", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceCard)
                    .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (hasBuildConfigKey || customApiKey.isNotBlank()) EmeraldGrounded else AccentCoral)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (hasBuildConfigKey) "Active via AI Studio Secrets" else if (customApiKey.isNotBlank()) "Active via Custom Key" else "No API Key configured",
                            color = if (hasBuildConfigKey || customApiKey.isNotBlank()) EmeraldGrounded else AccentCoral,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = { Text("Override with custom Gemini Key (optional)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_api_key_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                onCustomApiKeyChange(keyInput.trim())
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                        ) {
                            Text("Save Key", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Model Selection
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Psychology, contentDescription = "Model", tint = VioletNeon, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Gemini Intelligence Model", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))

            val models = listOf(
                "gemini-2.5-flash" to "Flash (Fast Live Audio & Q&A)",
                "gemini-3.5-flash" to "3.5 Flash (General Intelligence)",
                "gemini-3.1-pro-preview" to "Pro (Deep Reasoning & STEM)"
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                models.forEach { (id, label) ->
                    val isSelected = currentModel == id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DarkSurfaceVariant else DarkSurfaceCard)
                            .border(1.dp, if (isSelected) NeonCyan else CardBorder, RoundedCornerShape(8.dp))
                            .clickable { onModelChange(id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = id, color = if (isSelected) NeonCyan else TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(text = label, color = TextSecondary, fontSize = 11.sp)
                        }
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Audio & Voice Settings
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.RecordVoiceOver, contentDescription = "Voice", tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Voice Playback Protocol", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Auto-speak toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Auto-Voice Responses", color = TextPrimary, fontSize = 14.sp)
                    Text(text = "Read MYRA's answers aloud automatically", color = TextSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = autoSpeak,
                    onCheckedChange = onAutoSpeakChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = DarkSurfaceVariant),
                    modifier = Modifier.testTag("auto_speak_switch")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Speech Rate
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = "Speech Rate (Fast Audio Delivery)", color = TextPrimary, fontSize = 13.sp)
                    Text(text = String.format("%.2fx", currentRate), color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = currentRate,
                    onValueChange = {
                        currentRate = it
                        onSpeechRateChange(it)
                    },
                    valueRange = 0.8f..1.6f,
                    steps = 8,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan, inactiveTrackColor = DarkSurfaceVariant)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Background & Permissions Settings
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "बैकग्राउंड & परमिशन प्रबंधन", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenPermissions()
                },
                modifier = Modifier.fillMaxWidth().testTag("open_permissions_from_settings"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("माइक्रोफ़ोन और बैकग्राउंड अनुमतियाँ देखें", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Clear chat history
            OutlinedButton(
                onClick = {
                    onClearChatHistory()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("clear_history_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentCoral),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentCoral.copy(alpha = 0.5f))
            ) {
                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear All Messages", fontSize = 13.sp)
            }
        }
    }
}
