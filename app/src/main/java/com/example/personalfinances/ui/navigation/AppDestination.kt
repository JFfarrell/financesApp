package com.example.personalfinances.ui.navigation

sealed class AppDestination(val route: String) {
    object Login    : AppDestination("login")
    object Main     : AppDestination("main")
    object Home     : AppDestination("home")
    object Calendar : AppDestination("calendar")
    object Savings  : AppDestination("savings")
}
