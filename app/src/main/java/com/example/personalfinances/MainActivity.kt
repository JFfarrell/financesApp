package com.example.personalfinances

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.example.personalfinances.domain.model.enums.ThemeMode
import com.example.personalfinances.ui.app.AppViewModel
import com.example.personalfinances.ui.navigation.AppNavGraph
import com.example.personalfinances.ui.theme.PersonalFinancesTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single activity host for the app. Sets up edge-to-edge display and hands off to the Compose
 * navigation graph.
 *
 * [enableEdgeToEdge] makes the window draw behind the system bars and, critically, causes the
 * system to report IME (keyboard) height as a window inset. Without it, [imePadding] modifiers
 * have nothing to react to and the keyboard will cover text fields.
 *
 * The theme is chosen here from the user's saved [ThemeMode]. Because the app theme can differ
 * from the phone's, the system bar icon colours are re-applied whenever the choice changes,
 * otherwise light icons could end up on a light background.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by appViewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme }
                )
                onDispose { }
            }

            PersonalFinancesTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
