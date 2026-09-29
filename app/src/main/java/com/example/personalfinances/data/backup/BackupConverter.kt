package com.example.personalfinances.data.backup

import com.example.personalfinances.data.local.db.entity.CategoryEntity
import com.example.personalfinances.data.local.db.entity.MerchantEntity
import com.example.personalfinances.data.local.db.entity.SavingsGoalEntity
import com.example.personalfinances.data.local.db.entity.TransactionEntity
import com.example.personalfinances.domain.model.enums.CadenceUnit
import com.example.personalfinances.domain.model.enums.TransactionType
import com.example.personalfinances.domain.model.normalizeTag
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeParseException

/** Thrown when a backup file is well-formed JSON but its contents are not acceptable. */
class BackupFormatException(message: String) : Exception(message)

/** A backup whose contents have been validated and converted to database rows. */
class ParsedBackup(
    val categories: List<CategoryEntity>,
    val merchants: List<MerchantEntity>,
    val transactions: List<TransactionEntity>,
    val savingsGoal: SavingsGoalEntity?,
    val payCycleStartDay: Int?
)

/** Builds the file contents from the database rows. */
fun buildBackupFile(
    categories: List<CategoryEntity>,
    merchants: List<MerchantEntity>,
    transactions: List<TransactionEntity>,
    savingsGoal: SavingsGoalEntity?,
    payCycleStartDay: Int,
    exportedAt: String
) = BackupFile(
    exportedAt = exportedAt,
    settings = BackupSettings(payCycleStartDay),
    savingsGoal = savingsGoal?.let { BackupSavingsGoal(it.targetAmount, it.startingAmount) },
    categories = categories.map { BackupCategory(it.id, it.name, it.transactionType) },
    merchants = merchants.map { BackupMerchant(it.id, it.name) },
    transactions = transactions.map {
        BackupTransaction(
            id = it.id,
            type = it.transactionType,
            amount = it.amount,
            date = it.date.toString(),
            cadenceUnit = it.cadenceUnit,
            cadenceValue = it.cadenceValue,
            categoryId = it.categoryId,
            merchantId = it.merchantId,
            isRecurring = it.isRecurring,
            recurringGroupId = it.recurringGroupId,
            notes = it.notes,
            tags = Json.decodeFromString<List<String>>(it.tags)
        )
    }
)

/**
 * Validates [file] and converts it to database rows, throwing [BackupFormatException] with a
 * user-readable message at the first problem. Nothing is written here, so a bad file changes
 * nothing.
 *
 * [existingCategories] and [existingMerchants] are what is already in the database. They matter
 * for two reasons. A transaction may refer to a category or merchant that the file does not
 * repeat. And a fresh install seeds its own default categories with new ids, so a category or
 * merchant in the file that matches an existing one by name (and type, for categories) is not
 * added again: the file's transactions are pointed at the existing one instead. This keeps a
 * restore onto a new phone from duplicating "Groceries" and the other defaults.
 *
 * Tags are passed through [normalizeTag], so a hand-edited file cannot introduce tags the app
 * could not have made.
 */
fun parseBackup(
    file: BackupFile,
    existingCategories: List<CategoryEntity>,
    existingMerchants: List<MerchantEntity>
): ParsedBackup {
    if (file.app != BACKUP_APP_ID) {
        throw BackupFormatException("This file is not a Wallot backup.")
    }
    if (file.formatVersion < 1 || file.formatVersion > BACKUP_FORMAT_VERSION) {
        throw BackupFormatException(
            "This backup was made by a newer version of the app. Update the app and try again."
        )
    }

    val fileCategories = file.categories.map { c ->
        if (c.id.isBlank() || c.name.isBlank()) throw BackupFormatException("A category has no id or name.")
        val type = parseEnum<TransactionType>(c.type, "category \"${c.name}\"")
        CategoryEntity(id = c.id, name = c.name.trim(), transactionType = type.name)
    }
    val fileMerchants = file.merchants.map { m ->
        if (m.id.isBlank() || m.name.isBlank()) throw BackupFormatException("A merchant has no id or name.")
        MerchantEntity(id = m.id, name = m.name.trim())
    }

    // Reuse an existing category or merchant with the same name instead of adding a duplicate.
    val categoryRemap = mutableMapOf<String, String>()
    val categoryByKey = existingCategories.associateBy { categoryKey(it.transactionType, it.name) }
    val categories = fileCategories.filter { c ->
        val match = categoryByKey[categoryKey(c.transactionType, c.name)]
        if (match != null && match.id != c.id) {
            categoryRemap[c.id] = match.id
            false
        } else {
            true
        }
    }
    val merchantRemap = mutableMapOf<String, String>()
    val merchantByKey = existingMerchants.associateBy { it.name.trim().lowercase() }
    val merchants = fileMerchants.filter { m ->
        val match = merchantByKey[m.name.trim().lowercase()]
        if (match != null && match.id != m.id) {
            merchantRemap[m.id] = match.id
            false
        } else {
            true
        }
    }

    val categoryIds = existingCategories.map { it.id }.toSet() + categories.map { it.id }
    val merchantIds = existingMerchants.map { it.id }.toSet() + merchants.map { it.id }

    val transactions = file.transactions.map { t ->
        val label = "transaction ${t.id}"
        if (t.id.isBlank()) throw BackupFormatException("A transaction has no id.")
        if (!t.amount.isFinite()) throw BackupFormatException("The amount of $label is not a number.")
        if (t.cadenceValue < 0) throw BackupFormatException("The repeat interval of $label is negative.")
        val categoryId = categoryRemap[t.categoryId] ?: t.categoryId
        val merchantId = t.merchantId?.let { merchantRemap[it] ?: it }
        if (categoryId !in categoryIds) {
            throw BackupFormatException("$label refers to a category that is missing from the backup.")
        }
        if (merchantId != null && merchantId !in merchantIds) {
            throw BackupFormatException("$label refers to a merchant that is missing from the backup.")
        }
        val date = try {
            LocalDate.parse(t.date)
        } catch (e: DateTimeParseException) {
            throw BackupFormatException("The date of $label (\"${t.date}\") is not valid.")
        }
        val tags = t.tags.mapNotNull { normalizeTag(it) }.toSet()
        TransactionEntity(
            id = t.id,
            transactionType = parseEnum<TransactionType>(t.type, label).name,
            amount = t.amount,
            date = date,
            cadenceUnit = parseEnum<CadenceUnit>(t.cadenceUnit, label).name,
            cadenceValue = t.cadenceValue,
            categoryId = categoryId,
            merchantId = merchantId,
            isRecurring = t.isRecurring,
            recurringGroupId = t.recurringGroupId,
            notes = t.notes,
            tags = Json.encodeToString(tags)
        )
    }

    return ParsedBackup(
        categories = categories,
        merchants = merchants,
        transactions = transactions,
        savingsGoal = file.savingsGoal?.let {
            SavingsGoalEntity(id = 1, targetAmount = it.targetAmount, startingAmount = it.startingAmount)
        },
        payCycleStartDay = file.settings.payCycleStartDay.takeIf { it in 1..28 }
    )
}

/** Key for matching categories by meaning: the same type and the same name, ignoring case. */
private fun categoryKey(type: String, name: String) = "$type|${name.trim().lowercase()}"

private inline fun <reified E : Enum<E>> parseEnum(value: String, what: String): E =
    enumValues<E>().firstOrNull { it.name == value }
        ?: throw BackupFormatException("Unknown ${E::class.simpleName} \"$value\" in $what.")
