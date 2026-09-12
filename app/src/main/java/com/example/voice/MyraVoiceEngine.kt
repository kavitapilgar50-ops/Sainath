package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class MyraVoiceEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "MyraVoiceEngine"
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _speechRate = MutableStateFlow(1.15f) // Optimized for fast audio delivery
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    // Callbacks
    var onSpeechRecognized: ((String) -> Unit)? = null
    var onSpeechPartial: ((String) -> Unit)? = null
    var onSpeechError: ((String) -> Unit)? = null
    var onSpeakingStateChanged: ((Boolean) -> Unit)? = null

    init {
        initTts()
        initSpeechRecognizer()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            // Prefer Hindi (India) or English (India) for authentic Hinglish pronunciation
            val hindiLocale = Locale.forLanguageTag("hi-IN")
            val enInLocale = Locale.forLanguageTag("en-IN")

            val result = tts?.setLanguage(hindiLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                val enResult = tts?.setLanguage(enInLocale)
                if (enResult == TextToSpeech.LANG_MISSING_DATA || enResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.getDefault())
                }
            }

            tts?.setSpeechRate(_speechRate.value)
            tts?.setPitch(1.05f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    onSpeakingStateChanged?.invoke(true)
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    onSpeakingStateChanged?.invoke(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    onSpeakingStateChanged?.invoke(false)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    onSpeakingStateChanged?.invoke(false)
                }
            })
        } else {
            Log.e(TAG, "TTS Initialization failed")
        }
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "कोई आवाज़ नहीं पहचानी गई।"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "समय समाप्त हुआ।"
                            SpeechRecognizer.ERROR_AUDIO -> "ऑडियो रिकॉर्डिंग में त्रुटि।"
                            SpeechRecognizer.ERROR_NETWORK -> "नेटवर्क कनेक्टिविटी त्रुटि।"
                            else -> "माइक्रोफ़ोन त्रुटि: $error"
                        }
                        onSpeechError?.invoke(errorMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            onSpeechRecognized?.invoke(text)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim()
                        if (!text.isNullOrBlank()) {
                            onSpeechPartial?.invoke(text)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    fun startListening() {
        stopSpeech()
        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "MYRA को कुछ बोलिए (Speak to MYRA)...")
        }
        try {
            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recognition: ${e.message}")
            _isListening.value = false
            onSpeechError?.invoke("Voice recognition error: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
    }

    fun speak(text: String, onStart: () -> Unit = {}, onDone: () -> Unit = {}) {
        if (!isTtsReady || tts == null) {
            return
        }
        stopListening()
        stopSpeech()

        val cleanText = sanitizeForSpeech(text)
        if (cleanText.isBlank()) return

        val utteranceId = "myra_utterance_${System.currentTimeMillis()}"
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopSpeech() {
        if (tts != null && isTtsReady) {
            tts?.stop()
        }
        _isSpeaking.value = false
        onSpeakingStateChanged?.invoke(false)
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.7f, 2.0f)
        _speechRate.value = clamped
        tts?.setSpeechRate(clamped)
    }

    private fun sanitizeForSpeech(raw: String): String {
        return raw
            // Remove code blocks
            .replace(Regex("```[\\s\\S]*?```"), " कोड ब्लॉक छोड़ा गया ")
            // Remove inline code
            .replace(Regex("`[^`]+`"), "")
            // Remove markdown links [title](url) -> title
            .replace(Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)"), "$1")
            // Remove URLs
            .replace(Regex("https?://\\S+"), "")
            // Remove markdown bold/italic headers
            .replace(Regex("[#*~_>]"), " ")
            // Collapse multiple whitespaces/newlines
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun release() {
        stopSpeech()
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isTtsReady = false
    }
}
