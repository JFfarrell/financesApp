package com.example.personalfinances.domain.model.enums

/** How the app chooses between its light and dark themes. */
enum class ThemeMode(val displayName: String) {
    /** Follow the phone's dark mode setting. */
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}
