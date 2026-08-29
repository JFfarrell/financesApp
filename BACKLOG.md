# Personal Finances App — Backlog

Features to bring the app in line with the reference spreadsheet (2026 Budget.xlsx).
Tackle one item at a time. Update status as work progresses.

---

## Items

### 1. Income Types (predefined, with descriptions)
**Status:** Done

Replaced the free-text `source` field with a fixed `IncomeType` enum (Salary, Stock/RSU, Bonus, Leftovers, Other). Each type has a built-in read-only description; Other requires a mandatory user-provided description. A shared `TransactionType` interface and reusable `TransactionTypeField` composable were introduced to support the same pattern for expense types in future.

---

### 2. Annual Overview
**Status:** To do

New screen showing a scrollable grid — rows = income/expense types, columns = Jan–Dec + Total + Average. Two sections: Income and Expenses. Year navigation. New DAO queries aggregating by type and month for a full year.

**Complexity:** Medium-Large — custom grid layout, non-trivial queries. No schema changes. Best done after items 1 and 3.

---

### 3. Expense Types + Savings Integration
**Status:** Done

Replaced the flat user-managed `categories` system with a two-level predefined hierarchy: `ExpenseCategory` (Housing, Household, Transport, Lifestyle, Savings) → `ExpenseType` (24 subtypes, each implementing `TransactionType`). `*_OTHER` subtypes have editable descriptions. Added `HierarchicalTypeField` composable for the two-step picker. Savings goal `currentSaved` is now derived (`startingAmount + sum of past/current-month SAVINGS expenses`) rather than manually entered. Future recurring savings entries are excluded until their month arrives. DB version bumped to 3.

---

### 4. Export
**Status:** To do

Export action (from Dashboard or a menu) that generates a CSV mirroring the spreadsheet: income by type across months, expenses by type across months, totals. Delivered via Android `FileProvider` + share intent. No schema changes.

**Complexity:** Medium — self-contained, no schema changes. Best done last.

---

### 5. Add Docstrings
**Status:** To do

Backfill KDoc docstrings across all pre-existing files (entities, DAOs, mappers, domain models, repositories, use cases, ViewModels, composables). New code already includes docstrings per CLAUDE.md.

---

### 6. App Icon
**Status:** Done

Replaced the default launcher icon with a custom design.

---

### 7. Auto-set Recurring When Savings Type Selected
**Status:** Dropped

Superseded by the explicit recurring toggle introduced in item 9. Users set the toggle themselves; auto-setting it adds complexity for marginal gain.

---

### 8. Expense Date Picker + Edit and Delete
**Status:** Done

Collapsed Expenses and Income into a redesigned Calendar screen. Entries for any month can be added via two inline buttons. Tap an item to edit (pre-populated sheet), swipe left to delete. Both expenses and income are fully editable. Nav reduced to Home / Calendar / Savings.

---

### 9. Recurring Transaction Series + Scope Dialog
**Status:** Done

When adding a recurring expense or income, the user is asked "For how many months?" and the app auto-creates entries for each month using a shared `recurringGroupId` (UUID). When deleting or editing a recurring entry, a dialog asks:
1. **This entry only** — affects only the tapped record.
2. **This & future** — affects this record and all future entries in the series (same `recurringGroupId`, date ≥ current).

Implemented via new DAO bulk-update/delete queries, four new use cases (`DeleteExpenseSeriesUseCase`, `UpdateExpenseSeriesUseCase`, `DeleteIncomeSeriesUseCase`, `UpdateIncomeSeriesUseCase`), `RecurringDialogState` sealed class in CalendarViewModel, and a `RecurringActionDialog` composable in CalendarScreen. `Income` gained an explicit `isRecurring: Boolean` field (matching `Expense`) so both transaction types behave identically. DB version bumped to 6.

---

### 10. Bottom Nav "Dashboard" Label Overflow
**Status:** Done

Renamed label to "Home" and updated icon to `Icons.Default.Home`.

---

---

### 11. Unified Transaction Data Model Refactor
**Status:** In progress

Replace the rigid two-entity model (Expense + Income with hardcoded enums) with a unified `Transaction` model. Categories and merchants become user-defined entities. Tags replace the enum hierarchy for flexible analytics.

See memory for full agreed data model spec.

#### Layer 1 — Domain models & data layer
- [x] `TransactionType` enum (EXPENSE, INCOME, SAVING)
- [x] `CadenceUnit` enum (DAYS, WEEKS, MONTHS, YEARS)
- [x] `Category` domain model
- [x] `Merchant` domain model
- [x] `Transaction` domain model
- [x] `TransactionEntity`, `CategoryEntity`, `MerchantEntity`
- [x] `TransactionDao`, `CategoryDao`, `MerchantDao`
- [x] `TransactionMapper`, `CategoryMapper`, `MerchantMapper`
- [x] Register new entities in `AppDatabase`, bump DB version

#### Layer 2 — Repository & use cases
- [x] `TransactionRepository` interface + implementation (replaces `ExpenseRepository` + `IncomeRepository`)
- [x] `GetTransactionsByMonthUseCase`
- [x] `AddTransactionUseCase`
- [x] `UpdateTransactionUseCase`
- [x] `DeleteTransactionUseCase`
- [x] `DeleteTransactionSeriesUseCase`
- [x] `UpdateTransactionSeriesUseCase`
- [x] `GetSavingsTotalUseCase` (rewritten to filter by `transactionType == SAVING`)

#### Layer 3 — ViewModels
- [ ] `DashboardViewModel` — rewrite analytics to group by `category.name` instead of `ExpenseType.displayName`
- [ ] `CalendarViewModel` — migrate from `Expense`/`Income` to `Transaction`
- [ ] `SavingsViewModel` — update to use new savings use case

#### Layer 4 — UI
- [ ] Remove `HierarchicalTypeField` component
- [ ] New single category picker (searchable, create-new inline)
- [ ] Tag input UI (add/remove tags, searchable existing tags)
- [ ] Merchant input (searchable, create-new inline)
- [ ] Unified `TransactionListItem` replacing `ExpenseListItem` + `IncomeListItem`
- [ ] `AddTransactionBottomSheet` replacing `AddExpenseBottomSheet` + `AddIncomeBottomSheet`

#### Cleanup (after all layers done)
- [ ] Delete legacy files: `Expense`, `Income`, `ExpenseType`, `ExpenseCategory`, `IncomeType`, `LegacyTransactionType`
- [ ] Delete legacy entities, DAOs, mappers

---

### 12. Date Range Utilities & Transaction History Metadata
**Status:** To do

Repository-layer helpers for working with date ranges and transaction history bounds. Useful for analytics screens, date range pickers, and understanding the full span of a user's data.

- `getByDateRange(start, end)` — repository wrapper around `TransactionDao.getByMonth` with a clearer name
- `getByTag(tag, start?, end?)` — repository-layer Kotlin filter on top of `getAll()` or `getByDateRange()`
- `getFirstTransactionDate()` — date of the user's earliest transaction (`SELECT MIN(date) FROM transactions`)
- `getLastTransactionDate()` — date of the user's most recent transaction (`SELECT MAX(date) FROM transactions`)

**Complexity:** Low — mostly thin wrappers and two simple DAO queries. Best done as part of item 11 Layer 2.

---

### 13. Review Series Update Cadence Behaviour
**Status:** To do

When `updateTransactionSeriesFromDate` is called, it currently updates `cadence_unit` and `cadence_value` across all future instances in the series. Consider whether changing the cadence mid-series is a valid user action, and if so whether it should apply to all future instances or only from the edited entry onwards. May require a separate DAO query or UI confirmation dialog.

**Complexity:** Low-Medium — design decision first, then a small DAO/use case change.

---

### 14. Tag-based Analytics
**Status:** To do

Filter and group transactions by tag for analytics screens. Requires `getByTag` query on `TransactionDao`. Initial implementation using SQL `LIKE` on serialised JSON tags field. Known limitation: fragile for partial tag name matches. Future improvement: junction table for reliable multi-tag AND/OR filtering.

**Complexity:** Medium — query complexity depends on whether junction table refactor is done first. Best tackled after item 11 is complete.

---

## Suggested Order
1 ✅ → 3 ✅ → 8 ✅ → 9 ✅ → 10 ✅ → 6 ✅ → 7 ~~dropped~~ → 11 (in progress) → 2 → 12 → 4 → 5
