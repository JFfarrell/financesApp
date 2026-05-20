package com.example.personalfinances.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.personalfinances.ui.screen.auth.LoginScreen
import com.example.personalfinances.ui.screen.dashboard.DashboardScreen
import com.example.personalfinances.ui.screen.monthly.CalendarScreen
import com.example.personalfinances.ui.screen.savings.SavingsScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = AppDestination.Login.route
    ) {
        composable(AppDestination.Login.route) {
            LoginScreen(
                onAuthenticated = {
                    navController.navigate(AppDestination.Main.route) {
                        popUpTo(AppDestination.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(AppDestination.Main.route) {
            MainScaffold()
        }
    }
}

@Composable
fun MainScaffold() {
    val bottomNavController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavBar(navController = bottomNavController) }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppDestination.Home.route)     { DashboardScreen() }
            composable(AppDestination.Calendar.route) { CalendarScreen() }
            composable(AppDestination.Savings.route)  { SavingsScreen() }
        }
    }
}
