package com.toolbox.ui.document

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.toolbox.data.local.entity.OcrHistory
import com.toolbox.data.repository.OcrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class DocumentViewModel @Inject constructor(
    private val ocrRepository: OcrRepository
) : ViewModel() {

    private val _recognizedText = MutableStateFlow("")
    val recognizedText: StateFlow<String> = _recognizedText

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val recognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    fun processImage(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null

            try {
                val image = InputImage.fromFilePath(context, imageUri)
                val result = recognizer.process(image).await()
                val text = result.text
                _recognizedText.value = text

                if (text.isNotEmpty()) {
                    ocrRepository.insertHistory(
                        OcrHistory(
                            imagePath = imageUri.toString(),
                            recognizedText = text
                        )
                    )
                }
            } catch (e: Exception) {
                _error.value = "识别失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearResult() {
        _recognizedText.value = ""
        _error.value = null
    }
}
