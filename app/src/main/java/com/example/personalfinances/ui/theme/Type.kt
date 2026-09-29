package com.example.personalfinances.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.example.personalfinances.R

/**
 * Figtree (SIL Open Font License, see `third_party/figtree/OFL.txt`), bundled as one variable
 * font file; each weight is a variation of it. Variable fonts need API 26, the app's minimum.
 */
@OptIn(ExperimentalTextApi::class)
val Figtree = FontFamily(
    Font(R.font.figtree, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.figtree, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.figtree, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.figtree, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

private val base = Typography()

/** Material's default type scale with Figtree applied, and slightly heavier titles. */
val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Figtree),
    displayMedium = base.displayMedium.copy(fontFamily = Figtree),
    displaySmall = base.displaySmall.copy(fontFamily = Figtree),
    headlineLarge = base.headlineLarge.copy(fontFamily = Figtree, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Figtree, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(fontFamily = Figtree),
    bodyMedium = base.bodyMedium.copy(fontFamily = Figtree),
    bodySmall = base.bodySmall.copy(fontFamily = Figtree),
    labelLarge = base.labelLarge.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = Figtree, fontWeight = FontWeight.SemiBold),
    labelSmall = base.labelSmall.copy(fontFamily = Figtree, fontWeight = FontWeight.Medium)
)
