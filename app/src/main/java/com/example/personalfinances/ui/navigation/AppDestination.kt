package com.example.personalfinances.ui.navigation

sealed class AppDestination(val route: String) {
    object Login : AppDestination("login")
    object Main : AppDestination("main")
    object Expenses : AppDestination("expenses")
    object Monthly : AppDestination("monthly")
    object Income : AppDestination("income")
    object Dashboard : AppDestination("dashboard")
    object Savings : AppDestination("savings")
}
