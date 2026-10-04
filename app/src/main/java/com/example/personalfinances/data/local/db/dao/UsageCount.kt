package com.example.personalfinances.data.local.db.dao

/** Result row for "how many transactions use this category or merchant". Not a table. */
data class UsageCount(val id: String, val count: Int)
