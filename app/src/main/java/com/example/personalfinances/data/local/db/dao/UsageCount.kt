package com.example.personalfinances.data.local.db.dao

import java.time.LocalDate

/** Result row for "how many transactions use this category or merchant". Not a table. */
data class UsageCount(val id: String, val count: Int)

/** Result row for "when was this merchant last used". Not a table. */
data class MerchantLastUsed(val id: String, val lastUsed: LocalDate)
