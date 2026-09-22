package com.toolbox.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VoiceRecognizerState(
    val isListening: Boolean = false,
    val recognizedText: String = "",
    val error: String? = null,
    val isAvailable: Boolean = true
)

class VoiceRecognizerHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow(VoiceRecognizerState())
    val state: StateFlow<VoiceRecognizerState> = _state.asStateFlow()

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun startListening() {
        if (!isRecognitionAvailable()) {
            _state.value = _state.value.copy(
                error = "设备未安装语音识别服务，将使用系统语音输入"
            )
            // 尝试使用系统 Intent
            startSystemVoiceRecognition()
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = "语音识别初始化失败: ${e.message}")
            startSystemVoiceRecognition()
        }
    }

    private fun startSystemVoiceRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "请说话...")
            }
            if (context is Activity) {
                context.startActivityForResult(intent, REQUEST_CODE_SPEECH)
            }
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = "无法启动语音识别: ${e.message}")
        }
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SPEECH && resultCode == Activity.RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = results?.firstOrNull() ?: ""
            _state.value = _state.value.copy(
                recognizedText = recognizedText,
                isListening = false
            )
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = _state.value.copy(
                isListening = true,
                recognizedText = "",
                error = null
            )
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(isListening = false)
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "音频录制错误"
                SpeechRecognizer.ERROR_CLIENT -> "客户端错误"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "权限不足"
                SpeechRecognizer.ERROR_NETWORK -> "网络错误"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                SpeechRecognizer.ERROR_NO_MATCH -> "未识别到语音"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别服务繁忙"
                SpeechRecognizer.ERROR_SERVER -> "服务器错误"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "语音输入超时"
                else -> "未知错误: $error"
            }
            _state.value = _state.value.copy(
                isListening = false,
                error = errorMessage
            )
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val recognizedText = matches?.firstOrNull() ?: ""
            _state.value = _state.value.copy(
                isListening = false,
                recognizedText = recognizedText
            )
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partialText = matches?.firstOrNull() ?: ""
            if (partialText.isNotEmpty()) {
                _state.value = _state.value.copy(recognizedText = partialText)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _state.value = _state.value.copy(isListening = false)
    }

    fun cancelListening() {
        speechRecognizer?.cancel()
        _state.value = _state.value.copy(isListening = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearRecognizedText() {
        _state.value = _state.value.copy(recognizedText = "")
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    companion object {
        const val REQUEST_CODE_SPEECH = 1001
    }
}
