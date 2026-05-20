package com.example.personalfinances.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

data class BottomNavItem(
    val destination: AppDestination,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(AppDestination.Expenses, Icons.Default.Receipt, "Expenses"),
    BottomNavItem(AppDestination.Monthly, Icons.Default.CalendarMonth, "Monthly"),
    BottomNavItem(AppDestination.Income, Icons.Default.AttachMoney, "Income"),
    BottomNavItem(AppDestination.Dashboard, Icons.Default.Home, "Home"),
    BottomNavItem(AppDestination.Savings, Icons.Default.Savings, "Savings")
)

@Composable
fun BottomNavBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.destination.route,
                onClick = {
                    navController.navigate(item.destination.route) {
                        popUpTo(AppDestination.Expenses.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
