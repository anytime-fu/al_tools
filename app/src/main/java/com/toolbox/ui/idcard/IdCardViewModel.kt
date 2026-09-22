package com.toolbox.ui.idcard

import androidx.lifecycle.ViewModel
import com.toolbox.util.IdCardUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class IdCardViewModel @Inject constructor() : ViewModel() {

    private val _idNumber = MutableStateFlow("")
    val idNumber: StateFlow<String> = _idNumber

    private val _idCardInfo = MutableStateFlow<IdCardUtil.IdCardInfo?>(null)
    val idCardInfo: StateFlow<IdCardUtil.IdCardInfo?> = _idCardInfo

    private val _history = MutableStateFlow<List<IdCardUtil.IdCardInfo>>(emptyList())
    val history: StateFlow<List<IdCardUtil.IdCardInfo>> = _history

    fun onIdNumberChange(number: String) {
        _idNumber.value = number
    }

    fun parseIdCard() {
        val number = _idNumber.value.trim()
        if (number.isNotEmpty()) {
            val info = IdCardUtil.parseIdCard(number)
            _idCardInfo.value = info
            if (info.isValid) {
                _history.value = listOf(info) + _history.value.filter { it.idNumber != info.idNumber }
            }
        }
    }

    fun clearResult() {
        _idCardInfo.value = null
        _idNumber.value = ""
    }

    fun removeFromHistory(info: IdCardUtil.IdCardInfo) {
        _history.value = _history.value.filter { it.idNumber != info.idNumber }
    }
}
