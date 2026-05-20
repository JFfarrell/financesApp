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

Replaced the flat user-managed `categories` system with a two-level predefined hierarchy: `ExpenseCategory` (Housing, Household, Transport, Lifestyle, Savings) → `ExpenseType` (24 subtypes, each implementing `TransactionType`). `*_OTHER` subtypes have editable descriptions. Added `HierarchicalTypeField` composable for the two-step picker. Savings goal `currentSaved` is now derived (`startingAmount + sum of SAVINGS expenses`) rather than manually entered. DB version bumped to 3.

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
**Status:** To do

- **Date picker:** replace the hardcoded `DateUtils.todayEpochMillis()` with a month/year picker so the user can log an expense for any month.
- **Edit expense:** tap an expense in the list to open a pre-populated sheet and update it.
- **Delete expense:** swipe-to-dismiss or a long-press menu on the expense list item.

**Complexity:** Medium — new UI, new `UpdateExpense` use case and DAO method, date picker dialog.

---

### 9. Delete Recurring Expense — Scope Dialog
**Status:** To do

When the user deletes a recurring expense, show a confirmation dialog offering:
1. **Delete this entry only** — removes just the tapped record.
2. **Delete all recurring entries** — removes every expense record that shares the same type + cadence group.

This requires a way to identify "linked" recurring entries (e.g. a shared `recurringGroupId` column, or matching on `type + cadence + isRecurring`).

**Complexity:** Medium — dialog UI, group identification strategy, new DAO delete query.

---

### 10. Bottom Nav "Dashboard" Label Overflow
**Status:** Done

Renamed label to "Home" and updated icon to `Icons.Default.Home`.

---

## Suggested Order
1 ✅ → 3 ✅ → 2 → 7 → 8 → 9 → 4 → 5 → 6 → 10
