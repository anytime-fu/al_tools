package com.toolbox.ui.calculator

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toolbox.data.remote.ExchangeRateResponse
import com.toolbox.di.EncryptedPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import javax.inject.Inject

data class UnitCategory(
    val name: String,
    val units: List<Unit>
)

data class Unit(
    val name: String,
    val symbol: String,
    val factor: Double
)

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    @EncryptedPrefs private val prefs: SharedPreferences,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : ViewModel() {

    private val _inputValue = MutableStateFlow("")
    val inputValue: StateFlow<String> = _inputValue

    private val _fromUnit = MutableStateFlow(0)
    val fromUnit: StateFlow<Int> = _fromUnit

    private val _toUnit = MutableStateFlow(1)
    val toUnit: StateFlow<Int> = _toUnit

    private val _result = MutableStateFlow("")
    val result: StateFlow<String> = _result

    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    val categories = listOf(
        UnitCategory(
            name = "长度",
            units = listOf(
                Unit("米", "m", 1.0),
                Unit("千米", "km", 1000.0),
                Unit("厘米", "cm", 0.01),
                Unit("毫米", "mm", 0.001),
                Unit("英里", "mile", 1609.344),
                Unit("英寸", "inch", 0.0254),
                Unit("英尺", "ft", 0.3048)
            )
        ),
        UnitCategory(
            name = "重量",
            units = listOf(
                Unit("千克", "kg", 1.0),
                Unit("克", "g", 0.001),
                Unit("毫克", "mg", 0.000001),
                Unit("吨", "t", 1000.0),
                Unit("磅", "lb", 0.453592),
                Unit("盎司", "oz", 0.0283495)
            )
        ),
        UnitCategory(
            name = "温度",
            units = listOf(
                Unit("摄氏度", "°C", 1.0),
                Unit("华氏度", "°F", 1.0),
                Unit("开尔文", "K", 1.0)
            )
        ),
        UnitCategory(
            name = "面积",
            units = listOf(
                Unit("平方米", "m²", 1.0),
                Unit("平方千米", "km²", 1000000.0),
                Unit("公顷", "ha", 10000.0),
                Unit("亩", "mu", 666.667),
                Unit("平方英里", "sq mi", 2589988.11),
                Unit("英亩", "acre", 4046.86)
            )
        ),
        UnitCategory(
            name = "体积",
            units = listOf(
                Unit("升", "L", 1.0),
                Unit("毫升", "mL", 0.001),
                Unit("立方米", "m³", 1000.0),
                Unit("加仑", "gal", 3.78541),
                Unit("夸脱", "qt", 0.946353)
            )
        ),
        UnitCategory(
            name = "汇率",
            units = listOf(
                Unit("人民币", "CNY", 1.0),
                Unit("美元", "USD", 1.0),
                Unit("欧元", "EUR", 1.0),
                Unit("日元", "JPY", 1.0),
                Unit("英镑", "GBP", 1.0),
                Unit("韩元", "KRW", 1.0)
            )
        )
    )

    private var selectedCategoryIndex = 0

    init {
        loadCachedRates()
    }

    private fun loadCachedRates() {
        val cachedRates = prefs.getString("exchange_rates", null)
        if (cachedRates != null) {
            try {
                val rates = gson.fromJson(cachedRates, Map::class.java) as? Map<String, Double>
                if (rates != null) {
                    _exchangeRates.value = rates
                }
            } catch (e: Exception) {
                // 忽略解析错误
            }
        }
    }

    fun refreshExchangeRates() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val response = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://api.exchangerate-api.com/v4/latest/CNY")
                        .build()
                    okHttpClient.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val rateResponse = gson.fromJson(responseBody, ExchangeRateResponse::class.java)
                    rateResponse.rates?.let { rates ->
                        _exchangeRates.value = rates
                        prefs.edit().putString("exchange_rates", gson.toJson(rates)).apply()
                    }
                }
            } catch (e: Exception) {
                // 网络错误，使用缓存数据
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun selectCategory(index: Int) {
        selectedCategoryIndex = index
        _fromUnit.value = 0
        _toUnit.value = 1
        if (categories[index].name == "汇率" && _exchangeRates.value.isEmpty()) {
            refreshExchangeRates()
        }
        convert()
    }

    fun onInputChange(value: String) {
        _inputValue.value = value
        convert()
    }

    fun onFromUnitChange(index: Int) {
        _fromUnit.value = index
        convert()
    }

    fun onToUnitChange(index: Int) {
        _toUnit.value = index
        convert()
    }

    private fun convert() {
        val input = _inputValue.value.toDoubleOrNull()
        if (input == null) {
            _result.value = ""
            return
        }

        val category = categories[selectedCategoryIndex]
        val from = category.units[_fromUnit.value]
        val to = category.units[_toUnit.value]

        val converted = when (category.name) {
            "温度" -> convertTemperature(input, from.name, to.name)
            "汇率" -> convertCurrency(input, from.symbol, to.symbol)
            else -> {
                val baseValue = input * from.factor
                baseValue / to.factor
            }
        }

        _result.value = String.format("%.4f %s", converted, to.symbol)
    }

    private fun convertTemperature(value: Double, from: String, to: String): Double {
        // 先转换为摄氏度
        val celsius = when (from) {
            "摄氏度" -> value
            "华氏度" -> (value - 32) * 5 / 9
            "开尔文" -> value - 273.15
            else -> value
        }

        // 再从摄氏度转换为目标单位
        return when (to) {
            "摄氏度" -> celsius
            "华氏度" -> celsius * 9 / 5 + 32
            "开尔文" -> celsius + 273.15
            else -> celsius
        }
    }

    private fun convertCurrency(value: Double, from: String, to: String): Double {
        val rates = _exchangeRates.value
        if (rates.isEmpty()) return value

        val fromRate = rates[from] ?: 1.0
        val toRate = rates[to] ?: 1.0

        // 先转换为CNY，再转换为目标货币
        val inCNY = value / fromRate
        return inCNY * toRate
    }
}
