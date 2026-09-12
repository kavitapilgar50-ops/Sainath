package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AssistantState
import com.example.data.model.ChatMessage
import com.example.data.repository.MyraRepository
import com.example.voice.MyraVoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MyraViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = MyraRepository(database.chatDao())
    val voiceEngine = MyraVoiceEngine(application)

    val messages: StateFlow<List<ChatMessage>> = repository.getMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _currentSpeakingId = MutableStateFlow<Long?>(null)
    val currentSpeakingId: StateFlow<Long?> = _currentSpeakingId.asStateFlow()

    private val _isSearchGroundingEnabled = MutableStateFlow(true)
    val isSearchGroundingEnabled: StateFlow<Boolean> = _isSearchGroundingEnabled.asStateFlow()

    private val _selectedModel = MutableStateFlow("gemini-2.5-flash")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _autoSpeak = MutableStateFlow(true)
    val autoSpeak: StateFlow<Boolean> = _autoSpeak.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _lastResponseText = MutableStateFlow("")
    val lastResponseText: StateFlow<String> = _lastResponseText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Wire voice engine callbacks
        voiceEngine.onSpeechRecognized = { recognizedText ->
            _liveTranscript.value = recognizedText
            _assistantState.value = AssistantState.IDLE
            sendMessage(recognizedText)
        }

        voiceEngine.onSpeechPartial = { partial ->
            _liveTranscript.value = partial
        }

        voiceEngine.onSpeechError = { err ->
            _assistantState.value = AssistantState.IDLE
            _errorMessage.value = err
        }

        voiceEngine.onSpeakingStateChanged = { isSpeaking ->
            if (isSpeaking) {
                _assistantState.value = AssistantState.SPEAKING
            } else {
                if (_assistantState.value == AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.IDLE
                }
                _currentSpeakingId.value = null
            }
        }
    }

    fun sendMessage(text: String, forceSearch: Boolean = false) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        // Stop any active speech before new query
        voiceEngine.stopSpeech()
        voiceEngine.stopListening()

        // Check if search grounding should be triggered
        val shouldSearch = forceSearch || _isSearchGroundingEnabled.value || detectSearchIntent(trimmed)

        viewModelScope.launch {
            _assistantState.value = AssistantState.THINKING
            _errorMessage.value = null

            // Save user message to DB
            repository.saveUserMessage(trimmed)

            // Prepare recent history
            val history = messages.value.takeLast(6).map {
                (if (it.role == ChatMessage.Role.USER) "user" else "model") to it.text
            }

            val result = repository.queryGemini(
                prompt = trimmed,
                apiKeyOverride = _customApiKey.value.ifBlank { null },
                modelName = _selectedModel.value,
                enableSearchGrounding = shouldSearch,
                recentHistory = history
            )

            result.fold(
                onSuccess = { geminiResponse ->
                    val assistantMsg = repository.saveAssistantMessage(geminiResponse, _selectedModel.value)
                    _lastResponseText.value = geminiResponse.text

                    if (_autoSpeak.value) {
                        _currentSpeakingId.value = assistantMsg.id
                        _assistantState.value = AssistantState.SPEAKING
                        voiceEngine.speak(geminiResponse.text)
                    } else {
                        _assistantState.value = AssistantState.IDLE
                    }
                },
                onFailure = { throwable ->
                    _assistantState.value = AssistantState.IDLE
                    _errorMessage.value = throwable.localizedMessage ?: "त्रुटि उत्पन्न हुई (Error occurred)."
                }
            )
        }
    }

    private fun detectSearchIntent(query: String): Boolean {
        val lower = query.lowercase()
        val searchKeywords = listOf(
            "today", "now", "news", "weather", "aaj", "khabar", "mausam", "score", "price",
            "latest", "current", "update", "kya chal raha", "who is", "match", "stock", "cricket"
        )
        return searchKeywords.any { lower.contains(it) }
    }

    fun startVoiceRecognition() {
        _liveTranscript.value = ""
        _assistantState.value = AssistantState.LISTENING
        voiceEngine.startListening()
    }

    fun stopVoiceRecognition() {
        voiceEngine.stopListening()
        _assistantState.value = AssistantState.IDLE
    }

    fun toggleVoiceRecognition() {
        if (voiceEngine.isListening.value) {
            stopVoiceRecognition()
        } else {
            startVoiceRecognition()
        }
    }

    fun speakMessage(messageId: Long, text: String) {
        _currentSpeakingId.value = messageId
        voiceEngine.speak(text)
    }

    fun stopSpeaking() {
        voiceEngine.stopSpeech()
        _currentSpeakingId.value = null
        if (_assistantState.value == AssistantState.SPEAKING) {
            _assistantState.value = AssistantState.IDLE
        }
    }

    fun clearChat() {
        stopSpeaking()
        viewModelScope.launch {
            repository.clearChat()
        }
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
    }

    fun setModel(model: String) {
        _selectedModel.value = model
    }

    fun setAutoSpeak(enabled: Boolean) {
        _autoSpeak.value = enabled
    }

    fun setSpeechRate(rate: Float) {
        voiceEngine.setSpeechRate(rate)
    }

    fun toggleSearchGrounding() {
        _isSearchGroundingEnabled.value = !_isSearchGroundingEnabled.value
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.release()
    }
}
