package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AssistantState
import com.example.ui.components.ChatMessageCard
import com.example.ui.components.NeuralOrbVisualizer
import com.example.ui.components.PermissionsDialog
import com.example.ui.components.QuickPromptCapsules
import com.example.ui.components.SettingsSheet
import com.example.ui.components.VoiceModeBottomSheet
import androidx.compose.material.icons.filled.Security
import com.example.ui.theme.AccentCoral
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.EmeraldGrounded
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon

@Composable
fun MyraScreen(viewModel: MyraViewModel) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val assistantState by viewModel.assistantState.collectAsState()
    val currentSpeakingId by viewModel.currentSpeakingId.collectAsState()
    val isSearchGroundingEnabled by viewModel.isSearchGroundingEnabled.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()
    val autoSpeak by viewModel.autoSpeak.collectAsState()
    val speechRate by viewModel.voiceEngine.speechRate.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val lastResponseText by viewModel.lastResponseText.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var inputPrompt by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showVoiceMode by remember { mutableStateOf(false) }
    var showPermissionsDialog by remember { mutableStateOf(false) }

    val isBackgroundActive by viewModel.isBackgroundServiceActive.collectAsState()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Show error snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.dismissError()
        }
    }

    // Audio permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startVoiceRecognition()
        } else {
            showPermissionsDialog = true
        }
    }

    fun requestMicAndListen() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        hasMicPermission = hasPermission

        if (hasPermission) {
            viewModel.toggleVoiceRecognition()
        } else {
            showPermissionsDialog = true
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        containerColor = DeepSpace,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // TOP BAR
            Surface(
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Orb + Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showVoiceMode = true }
                    ) {
                        NeuralOrbVisualizer(
                            state = assistantState,
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "MYRA",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (assistantState) {
                                                AssistantState.IDLE -> EmeraldGrounded
                                                AssistantState.LISTENING -> AccentCoral
                                                AssistantState.THINKING -> VioletNeon
                                                AssistantState.SPEAKING -> NeonCyan
                                            }
                                        )
                                )
                            }
                            Text(
                                text = when (assistantState) {
                                    AssistantState.IDLE -> "Gemini Live • Ready"
                                    AssistantState.LISTENING -> "Listening..."
                                    AssistantState.THINKING -> "Reasoning..."
                                    AssistantState.SPEAKING -> "Speaking audio..."
                                },
                                color = when (assistantState) {
                                    AssistantState.IDLE -> TextSecondary
                                    AssistantState.LISTENING -> AccentCoral
                                    AssistantState.THINKING -> VioletNeon
                                    AssistantState.SPEAKING -> NeonCyan
                                },
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Right Actions: Live Voice Mode, Grounding Toggle, Settings
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Live Voice Mode Button
                        IconButton(
                            onClick = { showVoiceMode = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .testTag("open_live_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Live Voice Mode",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Google Search Grounding Toggle Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSearchGroundingEnabled) EmeraldGrounded.copy(alpha = 0.15f)
                                    else DarkSurfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSearchGroundingEnabled) EmeraldGrounded else CardBorder,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.toggleSearchGrounding() }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("grounding_toggle_pill")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = "Search Grounding",
                                    tint = if (isSearchGroundingEnabled) EmeraldGrounded else TextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSearchGroundingEnabled) "Search ON" else "Search OFF",
                                    color = if (isSearchGroundingEnabled) EmeraldGrounded else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Permissions & Background Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isBackgroundActive) EmeraldGrounded.copy(alpha = 0.15f)
                                    else DarkSurfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isBackgroundActive) EmeraldGrounded else if (!hasMicPermission) AccentCoral else CardBorder,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { showPermissionsDialog = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("permissions_and_background_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Permissions & Background",
                                    tint = if (isBackgroundActive) EmeraldGrounded else if (!hasMicPermission) AccentCoral else NeonCyan,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBackgroundActive) "BG Active" else if (!hasMicPermission) "माइक अनुमति दें" else "अनुमतियाँ",
                                    color = if (isBackgroundActive) EmeraldGrounded else if (!hasMicPermission) AccentCoral else TextPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Settings Button
                        IconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission request banner if microphone permission is not granted
            if (!hasMicPermission) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentCoral.copy(alpha = 0.15f))
                        .clickable { showPermissionsDialog = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = AccentCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "माइक्रोफ़ोन और बैकग्राउंड अनुमति आवश्यक है — सेट करें",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentCoral)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("अनुमति दें", color = DeepSpace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Live Status Banner when speaking or listening
            AnimatedVisibility(
                visible = assistantState != AssistantState.IDLE,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when (assistantState) {
                                AssistantState.LISTENING -> AccentCoral.copy(alpha = 0.15f)
                                AssistantState.THINKING -> VioletNeon.copy(alpha = 0.15f)
                                AssistantState.SPEAKING -> NeonCyan.copy(alpha = 0.15f)
                                else -> Color.Transparent
                            }
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (assistantState == AssistantState.THINKING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = VioletNeon,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = when (assistantState) {
                                    AssistantState.LISTENING -> Icons.Default.Mic
                                    AssistantState.SPEAKING -> Icons.Default.GraphicEq
                                    else -> Icons.Default.Public
                                },
                                contentDescription = null,
                                tint = when (assistantState) {
                                    AssistantState.LISTENING -> AccentCoral
                                    AssistantState.SPEAKING -> NeonCyan
                                    else -> VioletNeon
                                },
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (assistantState) {
                                AssistantState.LISTENING -> "सुन रही हूँ... बोलिए"
                                AssistantState.THINKING -> "डीप रीज़निंग और वेब सर्च जारी है..."
                                AssistantState.SPEAKING -> "MYRA बोल रही है (Audio Active)"
                                else -> ""
                            },
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (assistantState == AssistantState.SPEAKING) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceVariant)
                                .clickable { viewModel.stopSpeaking() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("स्टॉप (Stop)", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Chat Messages / Hero
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    // EMPTY / HERO STATE
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        NeuralOrbVisualizer(
                            state = assistantState,
                            size = 120.dp,
                            onClick = { requestMicAndListen() }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "नमस्ते, I am MYRA",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Personal AI Assistant with Gemini Live Voice, Real-Time Google Search Grounding & Hinglish Intelligence.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 3 Capabilities cards
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CapabilityPill(
                                icon = "🌐",
                                title = "Google Search Grounding",
                                desc = "Real-time updates, news, weather & web facts automatically"
                            )
                            CapabilityPill(
                                icon = "🎙️",
                                title = "Fast Hinglish Voice Protocol",
                                desc = "Punchy audio modules: 'ओके बॉस...', 'आज की बड़ी खबरों में...'"
                            )
                            CapabilityPill(
                                icon = "🧠",
                                title = "Advanced Logical Processing",
                                desc = "Systematic multi-step reasoning for code, DNS, and STEM"
                            )
                        }
                    }
                } else {
                    // MESSAGES LIST
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            ChatMessageCard(
                                message = msg,
                                isCurrentlySpeaking = currentSpeakingId == msg.id,
                                onSpeakClick = { textToSpeak ->
                                    viewModel.speakMessage(msg.id, textToSpeak)
                                },
                                onStopSpeakClick = {
                                    viewModel.stopSpeaking()
                                }
                            )
                        }
                    }
                }
            }

            // Quick Prompt Capsules Row
            QuickPromptCapsules(
                onPromptClick = { prompt, forceSearch ->
                    inputPrompt = prompt
                    viewModel.sendMessage(prompt, forceSearch)
                }
            )

            // BOTTOM INPUT DOCK
            Surface(
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Microphone Voice Button
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (assistantState == AssistantState.LISTENING) AccentCoral
                                else DarkSurfaceVariant
                            )
                            .border(
                                1.5.dp,
                                if (assistantState == AssistantState.LISTENING) AccentCoral else CardBorder,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { requestMicAndListen() },
                            modifier = Modifier.size(46.dp).testTag("input_mic_button")
                        ) {
                            Icon(
                                imageVector = if (assistantState == AssistantState.LISTENING) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = if (assistantState == AssistantState.LISTENING) DeepSpace else NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Text Input Field
                    OutlinedTextField(
                        value = inputPrompt,
                        onValueChange = { inputPrompt = it },
                        placeholder = {
                            Text(
                                text = if (assistantState == AssistantState.LISTENING) "सुन रही हूँ... (Listening)"
                                else "MYRA से पूछें (Ask in Hindi/English)...",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan,
                            focusedContainerColor = DarkSurfaceCard,
                            unfocusedContainerColor = DarkSurfaceCard
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_text_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send or Stop Button
                    if (assistantState == AssistantState.SPEAKING) {
                        IconButton(
                            onClick = { viewModel.stopSpeaking() },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .border(1.dp, CardBorder, CircleShape)
                                .testTag("stop_speaking_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (inputPrompt.isNotBlank()) {
                                    val prompt = inputPrompt
                                    inputPrompt = ""
                                    viewModel.sendMessage(prompt)
                                }
                            },
                            enabled = inputPrompt.isNotBlank() && assistantState != AssistantState.THINKING,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (inputPrompt.isNotBlank()) NeonCyan else DarkSurfaceVariant)
                                .testTag("send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputPrompt.isNotBlank()) DeepSpace else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modals
    if (showSettings) {
        SettingsSheet(
            onDismiss = { showSettings = false },
            currentModel = selectedModel,
            onModelChange = { viewModel.setModel(it) },
            autoSpeak = autoSpeak,
            onAutoSpeakChange = { viewModel.setAutoSpeak(it) },
            speechRate = speechRate,
            onSpeechRateChange = { viewModel.setSpeechRate(it) },
            customApiKey = customApiKey,
            onCustomApiKeyChange = { viewModel.setCustomApiKey(it) },
            onOpenPermissions = { showPermissionsDialog = true },
            onClearChatHistory = { viewModel.clearChat() }
        )
    }

    if (showVoiceMode) {
        VoiceModeBottomSheet(
            onDismiss = { showVoiceMode = false },
            state = assistantState,
            liveTranscript = liveTranscript,
            lastMyraResponse = lastResponseText,
            onMicClick = { requestMicAndListen() },
            onStopSpeechClick = { viewModel.stopSpeaking() }
        )
    }

    if (showPermissionsDialog) {
        PermissionsDialog(
            onDismiss = { showPermissionsDialog = false },
            onBackgroundServiceToggle = { enable ->
                viewModel.toggleBackgroundService(enable)
            },
            isBackgroundActive = isBackgroundActive
        )
    }
}

@Composable
fun CapabilityPill(icon: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceCard)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(text = desc, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}
