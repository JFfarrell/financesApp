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
**Status:** To do

Update the default launcher icon to a custom design.

---

### 7. Auto-set Recurring When Savings Type Selected
**Status:** To do

In `AddExpenseBottomSheet`, when the user selects any `SAVINGS_*` type, automatically set the recurring toggle to `true`. The user can still override it manually. This reduces friction since savings contributions are almost always recurring.

**Complexity:** Small — a `LaunchedEffect` or `onTypeSelected` side-effect in the bottom sheet.

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

## Suggested Order
1 ✅ → 3 ✅ → 2 → 7 → 8 ✅ → 9 ✅ → 10 ✅ → 4 → 5 → 6
