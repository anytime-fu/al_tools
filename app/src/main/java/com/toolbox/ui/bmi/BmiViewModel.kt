package com.toolbox.ui.bmi

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BmiViewModel @Inject constructor() : ViewModel() {

    private val _weight = MutableStateFlow("")
    val weight: StateFlow<String> = _weight

    private val _height = MutableStateFlow("")
    val height: StateFlow<String> = _height

    private val _bmiResult = MutableStateFlow("")
    val bmiResult: StateFlow<String> = _bmiResult

    private val _bmiCategory = MutableStateFlow("")
    val bmiCategory: StateFlow<String> = _bmiCategory

    fun onWeightChange(value: String) {
        _weight.value = value
    }

    fun onHeightChange(value: String) {
        _height.value = value
    }

    fun calculateBmi() {
        val weightKg = _weight.value.toDoubleOrNull()
        val heightCm = _height.value.toDoubleOrNull()

        if (weightKg == null || heightCm == null || heightCm <= 0) {
            _bmiResult.value = "请输入有效数值"
            _bmiCategory.value = ""
            return
        }

        val heightM = heightCm / 100
        val bmi = weightKg / (heightM * heightM)

        _bmiResult.value = String.format("%.1f", bmi)
        _bmiCategory.value = getBmiCategory(bmi)
    }

    private fun getBmiCategory(bmi: Double): String {
        return when {
            bmi < 18.5 -> "偏瘦"
            bmi < 24.0 -> "正常"
            bmi < 28.0 -> "偏胖"
            else -> "肥胖"
        }
    }
}
