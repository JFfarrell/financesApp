package com.example.personalfinances.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Maps the app palette onto Material's colour roles so stock components match the design. */
private fun WalletColors.toColorScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = selected,
        onPrimary = onSelected,
        primaryContainer = navIndicator,
        onPrimaryContainer = onNavIndicator,
        secondary = saving,
        tertiary = income,
        background = background,
        onBackground = text,
        surface = card,
        onSurface = text,
        surfaceVariant = cardTonal,
        onSurfaceVariant = muted,
        surfaceContainerLowest = card,
        surfaceContainerLow = card,
        surfaceContainer = card,
        surfaceContainerHigh = cardTonal,
        surfaceContainerHighest = cardTonal,
        outline = outline,
        outlineVariant = outline,
        scrim = scrim
    )
}

/** Rounder shapes than Material's defaults: fields 12dp, cards 28dp, sheets 32dp. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

/**
 * The app theme. Pass [darkTheme] to choose between the light and dark palettes; the caller
 * decides how (system setting or the user's saved choice).
 *
 * The content is wrapped in a full-screen [Surface] in the theme's background colour. The window
 * itself uses a light XML theme, so without this a screen with no background of its own (such as
 * login) would draw light text on white in dark mode.
 */
@Composable
fun PersonalFinancesTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val wallet = if (darkTheme) DarkWalletColors else LightWalletColors

    CompositionLocalProvider(LocalWalletColors provides wallet) {
        MaterialTheme(
            colorScheme = wallet.toColorScheme(darkTheme),
            typography = Typography,
            shapes = AppShapes
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                content = content
            )
        }
    }
}

/** The app's semantic colours for the current theme, e.g. `MaterialTheme.wallet.income`. */
val MaterialTheme.wallet: WalletColors
    @Composable
    @ReadOnlyComposable
    get() = LocalWalletColors.current
