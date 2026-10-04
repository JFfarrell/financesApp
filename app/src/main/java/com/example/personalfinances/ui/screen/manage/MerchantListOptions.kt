package com.example.personalfinances.ui.screen.manage

/** How the merchants list on the manage screen is ordered. */
enum class MerchantSort(val label: String) {
    NAME("A–Z"),
    MOST_USED("Most used"),
    RECENTLY_USED("Recent")
}

/**
 * Applies the manage screen's merchant search, "unused only" filter and sort order.
 *
 * [query] matches anywhere in the name, ignoring case and surrounding spaces. Ties in every order
 * fall back to A–Z so the list never jumps around. For [MerchantSort.RECENTLY_USED], merchants
 * that have never been used come last.
 *
 * Kept as a plain function (no Android or state) so it can be unit tested.
 */
fun filterAndSortMerchants(
    merchants: List<ManagedMerchant>,
    query: String,
    sort: MerchantSort,
    unusedOnly: Boolean
): List<ManagedMerchant> {
    val needle = query.trim()
    val filtered = merchants.filter { managed ->
        (needle.isEmpty() || managed.merchant.name.contains(needle, ignoreCase = true)) &&
            (!unusedOnly || managed.usage == 0)
    }
    val byName = compareBy<ManagedMerchant> { it.merchant.name.lowercase() }
    return when (sort) {
        MerchantSort.NAME -> filtered.sortedWith(byName)
        MerchantSort.MOST_USED ->
            filtered.sortedWith(compareByDescending<ManagedMerchant> { it.usage }.then(byName))
        MerchantSort.RECENTLY_USED ->
            filtered.sortedWith(
                compareByDescending<ManagedMerchant> { it.lastUsed?.toEpochDay() ?: Long.MIN_VALUE }
                    .then(byName)
            )
    }
}
