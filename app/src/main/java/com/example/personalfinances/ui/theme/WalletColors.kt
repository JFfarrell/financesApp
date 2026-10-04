package com.example.personalfinances.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colours for one category: [chart] is the solid colour used in charts and legends, [tint] is the
 * soft background of the category avatar and [onTint] is the letter colour drawn on it.
 */
@Immutable
data class CategoryColor(val chart: Color, val tint: Color, val onTint: Color)

/**
 * The app's semantic colour palette, in addition to the standard Material colour scheme.
 *
 * There is one instance per theme ([LightWalletColors], [DarkWalletColors]); every screen reads
 * colours from here (via `MaterialTheme.wallet`) instead of hard-coding them, so a new theme is
 * just a new palette. [toColorScheme] maps the palette onto Material's own roles so stock
 * components (dialogs, sheets, text fields) pick up the same look.
 *
 * @property background Screen background.
 * @property text Primary text colour; [muted] is secondary text.
 * @property card Surface of cards and list groups; [cardTonal] is for chips, keys and tracks.
 * @property hero Colour of the "left this month" card, with [onHero] text and the three bar
 *   segments [heroSpent], [heroSaved] and [heroLeft].
 * @property selected Fill of selected chips, segments and primary buttons, with [onSelected] text.
 * @property navContainer Floating navigation bar; [navIndicator] and [onNavIndicator] mark the
 *   active item; [addButton] and [onAddButton] style the add button on the Transactions screen.
 * @property income Colour for income amounts; [saving] for savings amounts and goal progress.
 * @property categories Palette assigned to categories by [categoryColor].
 */
@Immutable
data class WalletColors(
    val background: Color,
    val text: Color,
    val muted: Color,
    val card: Color,
    val cardTonal: Color,
    val outline: Color,
    val hero: Color,
    val onHero: Color,
    val heroSpent: Color,
    val heroSaved: Color,
    val heroLeft: Color,
    val selected: Color,
    val onSelected: Color,
    val navContainer: Color,
    val navIndicator: Color,
    val onNavIndicator: Color,
    val addButton: Color,
    val onAddButton: Color,
    val income: Color,
    val saving: Color,
    val tagContainer: Color,
    val onTag: Color,
    val sheet: Color,
    val scrim: Color,
    val handle: Color,
    val categories: List<CategoryColor>
) {
    /**
     * Returns a colour for [name] that is stable across screens and sessions, chosen by hashing
     * the name. Categories are user-defined, so colours cannot be assigned by identity.
     */
    fun categoryColor(name: String): CategoryColor =
        categories[(name.lowercase().hashCode() and Int.MAX_VALUE) % categories.size]
}

val LightWalletColors = WalletColors(
    background = Color(0xFFF8F6FB),
    text = Color(0xFF1D1B22),
    muted = Color(0xFF5F5B6B),
    card = Color(0xFFFFFFFF),
    cardTonal = Color(0xFFEFEAF6),
    outline = Color(0xFFB9B2C8),
    hero = Color(0xFF5A4A99),
    onHero = Color(0xFFFFFFFF),
    heroSpent = Color(0xFFFFB4A8),
    heroSaved = Color(0xFFA9D4FF),
    heroLeft = Color(0xFFB7F0D2),
    selected = Color(0xFF5A4A99),
    onSelected = Color(0xFFFFFFFF),
    navContainer = Color(0xFFEDE7F6),
    navIndicator = Color(0xFFDCD2F3),
    onNavIndicator = Color(0xFF2B1F5C),
    addButton = Color(0xFF5A4A99),
    onAddButton = Color(0xFFFFFFFF),
    income = Color(0xFF1E6B4A),
    saving = Color(0xFF245581),
    tagContainer = Color(0xFFECE7F5),
    onTag = Color(0xFF3C3554),
    sheet = Color(0xFFFBF9FE),
    scrim = Color(0xFF3B3646),
    handle = Color(0xFFCAC4D0),
    categories = listOf(
        CategoryColor(Color(0xFF7C6BC4), Color(0xFFE9E3F7), Color(0xFF4C3D8A)),
        CategoryColor(Color(0xFF2A5F8F), Color(0xFFDCE9F7), Color(0xFF245581)),
        CategoryColor(Color(0xFF4FA07A), Color(0xFFDDF1E6), Color(0xFF1E6B4A)),
        CategoryColor(Color(0xFFD9A03C), Color(0xFFFBEBCB), Color(0xFF7A4E00)),
        CategoryColor(Color(0xFFD9705F), Color(0xFFFBE1DC), Color(0xFF8F3229)),
        CategoryColor(Color(0xFF5B9BD5), Color(0xFFE2ECF8), Color(0xFF2F5F8F))
    )
)

val DarkWalletColors = WalletColors(
    background = Color(0xFF0E1014),
    text = Color(0xFFEEF0F5),
    muted = Color(0xFFA6ACBB),
    card = Color(0xFF171A21),
    cardTonal = Color(0xFF20242E),
    outline = Color(0xFF3A4052),
    hero = Color(0xFF7DE3B0),
    onHero = Color(0xFF08251A),
    heroSpent = Color(0xFF08251A),
    heroSaved = Color(0xFF3E8E6B),
    heroLeft = Color(0x3308251A),
    selected = Color(0xFF7DE3B0),
    onSelected = Color(0xFF08251A),
    navContainer = Color(0xFF20242E),
    navIndicator = Color(0xFF2C3140),
    onNavIndicator = Color(0xFF7DE3B0),
    addButton = Color(0xFF7DE3B0),
    onAddButton = Color(0xFF08251A),
    income = Color(0xFF7DE3B0),
    saving = Color(0xFFB7A6FF),
    tagContainer = Color(0xFF20242E),
    onTag = Color(0xFFA6ACBB),
    sheet = Color(0xFF171A21),
    scrim = Color(0xFF06070A),
    handle = Color(0xFF3A4052),
    categories = listOf(
        CategoryColor(Color(0xFFB7A6FF), Color(0x29B7A6FF), Color(0xFFB7A6FF)),
        CategoryColor(Color(0xFF7DE3B0), Color(0x297DE3B0), Color(0xFF7DE3B0)),
        CategoryColor(Color(0xFFFFD27A), Color(0x29FFD27A), Color(0xFFFFD27A)),
        CategoryColor(Color(0xFFFF9C8A), Color(0x29FF9C8A), Color(0xFFFF9C8A)),
        CategoryColor(Color(0xFF7CC4FF), Color(0x297CC4FF), Color(0xFF7CC4FF)),
        CategoryColor(Color(0xFFF3A6E3), Color(0x29F3A6E3), Color(0xFFF3A6E3))
    )
)

/** Provides the active [WalletColors]; set by [PersonalFinancesTheme]. */
val LocalWalletColors = staticCompositionLocalOf { LightWalletColors }
