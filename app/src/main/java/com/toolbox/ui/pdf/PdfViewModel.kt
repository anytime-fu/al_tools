package com.toolbox.ui.pdf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _pdfDocument = MutableStateFlow<PDDocument?>(null)
    val pdfDocument: StateFlow<PDDocument?> = _pdfDocument

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages

    private val _currentPageText = MutableStateFlow("")
    val currentPageText: StateFlow<String> = _currentPageText

    private val _allText = MutableStateFlow("")
    val allText: StateFlow<String> = _allText

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun loadPdf(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val document = PDDocument.load(inputStream)
                        _pdfDocument.value = document
                        _totalPages.value = document.numberOfPages
                        _currentPage.value = 1
                        _fileName.value = uri.lastPathSegment ?: "未知文件"
                        
                        extractAllText(document)
                        extractCurrentPageText(document, 1)
                    } else {
                        _error.value = "无法打开PDF文件"
                    }
                }
            } catch (e: Exception) {
                _error.value = "加载PDF失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun extractAllText(document: PDDocument) {
        withContext(Dispatchers.IO) {
            try {
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                _allText.value = text
            } catch (e: Exception) {
                _allText.value = ""
            }
        }
    }

    private suspend fun extractCurrentPageText(document: PDDocument, page: Int) {
        withContext(Dispatchers.IO) {
            try {
                val stripper = PDFTextStripper()
                stripper.startPage = page
                stripper.endPage = page
                val text = stripper.getText(document)
                _currentPageText.value = text
            } catch (e: Exception) {
                _currentPageText.value = ""
            }
        }
    }

    fun goToPage(page: Int) {
        val document = _pdfDocument.value ?: return
        if (page in 1.._totalPages.value) {
            _currentPage.value = page
            viewModelScope.launch {
                extractCurrentPageText(document, page)
            }
        }
    }

    fun nextPage() {
        goToPage(_currentPage.value + 1)
    }

    fun previousPage() {
        goToPage(_currentPage.value - 1)
    }

    fun getPageText(): String {
        return _currentPageText.value
    }

    fun getAllText(): String {
        return _allText.value
    }

    override fun onCleared() {
        super.onCleared()
        try {
            _pdfDocument.value?.close()
        } catch (e: Exception) {
            // 忽略关闭错误
        }
    }
}
