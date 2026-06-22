package com.govtprep.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.govtprep.app.ui.navigation.AppNavHost
import com.govtprep.app.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ThemeManager.init(this)
        LanguageManager.init(this)

        setContent {
            val isDark by ThemeManager.isDarkMode.collectAsState()
            val isHindi by LanguageManager.isHindi.collectAsState()
            GovtPrepTheme(isDarkTheme = isDark) {
                CompositionLocalProvider(LocalStrings provides if (isHindi) HI else EN) {
                    AppNavHost()
                }
            }
        }
    }
}
