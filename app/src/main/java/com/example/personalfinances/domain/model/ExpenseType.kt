package com.example.personalfinances.domain.model

/**
 * Full set of predefined expense subtypes, organised under a parent [ExpenseCategory].
 *
 * Each entry implements [TransactionType], providing a [displayName] and [defaultDescription]
 * shown in the UI. Entries ending in `_OTHER` have [isDescriptionEditable] set to true,
 * meaning the user must supply their own description when selecting that subtype.
 *
 * The [category] property links each subtype back to its parent [ExpenseCategory]. To get all
 * subtypes for a given category: `ExpenseType.entries.filter { it.category == myCategory }`.
 *
 * Values are persisted in the database as their [name] string (e.g. `"TRANSPORT_FUEL"`),
 * converted back via [ExpenseType.valueOf] in the data mapper.
 */
enum class ExpenseType(
    val category: ExpenseCategory,
    override val displayName: String,
    override val defaultDescription: String,
    override val isDescriptionEditable: Boolean = false
) : TransactionType {

    // Housing
    HOUSING_RENT_MORTGAGE(
        category = ExpenseCategory.HOUSING,
        displayName = "Rent / Mortgage",
        defaultDescription = "Monthly rent or mortgage payment"
    ),
    HOUSING_OTHER(
        category = ExpenseCategory.HOUSING,
        displayName = "Other",
        defaultDescription = "",
        isDescriptionEditable = true
    ),

    // Household
    HOUSEHOLD_GROCERIES(
        category = ExpenseCategory.HOUSEHOLD,
        displayName = "Groceries",
        defaultDescription = "Food and household supplies"
    ),
    HOUSEHOLD_UTILITIES(
        category = ExpenseCategory.HOUSEHOLD,
        displayName = "Utilities",
        defaultDescription = "Gas, electricity, and water bills"
    ),
    HOUSEHOLD_INTERNET_PHONE(
        category = ExpenseCategory.HOUSEHOLD,
        displayName = "Internet / Phone",
        defaultDescription = "Broadband and mobile phone bills"
    ),
    HOUSEHOLD_OTHER(
        category = ExpenseCategory.HOUSEHOLD,
        displayName = "Other",
        defaultDescription = "",
        isDescriptionEditable = true
    ),

    // Transport
    TRANSPORT_FUEL(
        category = ExpenseCategory.TRANSPORT,
        displayName = "Fuel / Petrol",
        defaultDescription = "Fuel or petrol costs"
    ),
    TRANSPORT_ROAD_TAX(
        category = ExpenseCategory.TRANSPORT,
        displayName = "Road Tax",
        defaultDescription = "Annual vehicle road tax"
    ),
    TRANSPORT_INSURANCE(
        category = ExpenseCategory.TRANSPORT,
        displayName = "Insurance",
        defaultDescription = "Vehicle insurance premium"
    ),
    TRANSPORT_PUBLIC(
        category = ExpenseCategory.TRANSPORT,
        displayName = "Public Transport",
        defaultDescription = "Bus, train, or metro fares"
    ),
    TRANSPORT_OTHER(
        category = ExpenseCategory.TRANSPORT,
        displayName = "Other",
        defaultDescription = "",
        isDescriptionEditable = true
    ),

    // Lifestyle
    LIFESTYLE_DINING_OUT(
        category = ExpenseCategory.LIFESTYLE,
        displayName = "Dining Out",
        defaultDescription = "Restaurants, takeaways, and coffee shops"
    ),
    LIFESTYLE_SUBSCRIPTIONS(
        category = ExpenseCategory.LIFESTYLE,
        displayName = "Subscriptions",
        defaultDescription = "Streaming, software, and music services"
    ),
    LIFESTYLE_CLOTHING(
        category = ExpenseCategory.LIFESTYLE,
        displayName = "Clothing",
        defaultDescription = "Clothes and footwear"
    ),
    LIFESTYLE_HOLIDAYS(
        category = ExpenseCategory.LIFESTYLE,
        displayName = "Holidays / Travel",
        defaultDescription = "Flights, hotels, and trips"
    ),
    LIFESTYLE_MEDICAL(
        category = ExpenseCategory.LIFESTYLE,
        displayName = "Medical / Healthcare",
        defaultDescription = "Doctor, dental, and pharmacy costs"
    ),
    LIFESTYLE_PERSONAL_CARE(
        category = ExpenseCategory.LIFESTYLE,
        displayName = "Personal Care",
        defaultDescription = "Haircuts, beauty, and grooming"
    ),
    LIFESTYLE_GYM(
        category = ExpenseCategory.LIFESTYLE,
        displayName = "Gym / Fitness",
        defaultDescription = "Gym membership and fitness classes"
    ),
    LIFESTYLE_OTHER(
        category = ExpenseCategory.LIFESTYLE,
        displayName = "Other",
        defaultDescription = "",
        isDescriptionEditable = true
    ),

    // Savings
    SAVINGS_MONTHLY(
        category = ExpenseCategory.SAVINGS,
        displayName = "Monthly Recurring",
        defaultDescription = "Regular monthly savings contribution"
    ),
    SAVINGS_EXTRA(
        category = ExpenseCategory.SAVINGS,
        displayName = "Extra",
        defaultDescription = "One-off or irregular savings deposit"
    )
}
