package com.toolbox

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.toolbox.di.EncryptedPrefs
import com.toolbox.ui.navigation.NavGraph
import com.toolbox.ui.theme.ToolboxTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    
    @EncryptedPrefs
    @Inject
    lateinit var prefs: SharedPreferences
    
    private val darkModeState = mutableIntStateOf(0)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化读取当前值
        darkModeState.intValue = prefs.getInt("dark_mode", 0)
        
        // 注册监听器
        prefs.registerOnSharedPreferenceChangeListener(this)
        
        setContent {
            val darkMode by darkModeState
            
            ToolboxTheme(darkMode = darkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavGraph(navController = navController)
                }
            }
        }
    }
    
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "dark_mode") {
            darkModeState.intValue = prefs.getInt("dark_mode", 0)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }
}
