package com.example.personalfinances

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
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
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PersonalFinancesTheme {
                val navController = rememberNavController()
                AppNavGraph(navController = navController)
            }
        }
    }
}
