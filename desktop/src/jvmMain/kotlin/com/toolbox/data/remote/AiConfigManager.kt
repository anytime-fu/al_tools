package com.toolbox.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AiConfigManager {
    private val _config = MutableStateFlow(AiConfig())
    val config: StateFlow<AiConfig> = _config
    
    fun updateConfig(newConfig: AiConfig) {
        _config.value = newConfig
    }
    
    fun getApiKey(): String = _config.value.apiKey
    fun getModel(): String = _config.value.model
    fun getCustomApiUrl(): String? = _config.value.customApiUrl
}
