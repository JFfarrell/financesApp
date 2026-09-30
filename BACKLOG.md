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

New screen showing a scrollable grid — rows = categories (grouped by transaction type), columns = Jan–Dec + Total + Average. Sections: Income, Expenses and Savings. Year navigation. New DAO queries aggregating by category and month for a full year.

**Complexity:** Medium-Large — custom grid layout, non-trivial queries. No schema changes. Written against the old Expense/Income model; now builds on `TransactionDao` and categories.

---

### 3. Expense Types + Savings Integration
**Status:** Done

Replaced the flat user-managed `categories` system with a two-level predefined hierarchy: `ExpenseCategory` (Housing, Household, Transport, Lifestyle, Savings) → `ExpenseType` (24 subtypes, each implementing `TransactionType`). `*_OTHER` subtypes have editable descriptions. Added `HierarchicalTypeField` composable for the two-step picker. Savings goal `currentSaved` is now derived (`startingAmount + sum of past/current-month SAVINGS expenses`) rather than manually entered. Future recurring savings entries are excluded until their month arrives. DB version bumped to 3.

---

### 4. Export
**Status:** To do

Export action (from Dashboard or a menu) that generates a CSV mirroring the spreadsheet: income, expenses and savings by category across months, totals. This is a report, not a backup: the monthly grid loses dates, merchants and tags (see the backup and restore discussion). Delivered via Android `FileProvider` + share intent. No schema changes.

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
**Status:** Done

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
- [x] `DashboardViewModel` — rewrite analytics to group by `category.name` instead of `ExpenseType.displayName`
- [x] `CalendarViewModel` (in `MonthlyViewModel.kt`) — migrate from `Expense`/`Income` to `Transaction`; state, events and dialog collapsed to one transaction path. Its screen and sheets won't compile until Layer 4
- [x] `SavingsViewModel` — switched to the transaction `GetSavingsTotalUseCase`

#### Layer 2b — Category & merchant plumbing (needed by the new UI)
- [x] `CategoryRepository` + `MerchantRepository` interfaces and implementations, bound in `RepositoryModule`
- [x] `GetCategoriesUseCase`, `AddCategoryUseCase`, `GetMerchantsUseCase`, `AddMerchantUseCase`
- [x] Seed default categories via `RoomDatabase.Callback` in `DatabaseModule`
- [x] Categories scoped by transaction type (`Category.type`, DB v8): separate expense, income and savings lists, seeded per type; the sheet filters the picker by the selected type
- [x] Enforcing `category.type == transactionType` in the add/update use cases — moved to item 15
- [x] `CalendarViewModel` exposes `categories` and `merchants`, with `AddCategory` / `AddMerchant` events

#### Layer 4 — UI
- [x] Remove `HierarchicalTypeField` component
- [x] New single category picker (dropdown with create-new inline; not searchable yet)
- [x] Tag input UI — moved to item 15
- [x] Merchant input — moved to item 15
- [x] Unified `TransactionListItem` replacing `ExpenseListItem` + `IncomeListItem`
- [x] `AddTransactionBottomSheet` replacing `AddExpenseBottomSheet` + `AddIncomeBottomSheet` (trimmed: type, amount, category, date, notes, recurring)
- [x] `MonthlyScreen` (`CalendarScreen`) migrated to the unified state, events and sheet

#### Cleanup (after all layers done)
- [x] Delete legacy files: `Expense`, `Income`, `ExpenseType`, `ExpenseCategory`, `IncomeType`, `LegacyTransactionType`
- [x] Delete legacy entities, DAOs, mappers, repositories, use cases and UI; `SavingsGoalEntity` and `SavingsGoalMapper` moved out of `legacy/`; `DatabaseModule`, `RepositoryModule` and `AppDatabase` updated (DB v9); `DateUtils` reduced to `monthDateRange`
- [x] Verified by building and running

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

When `updateTransactionSeriesFromDate` is called, it currently updates `cadence_unit` and `cadence_value` across all future instances in the series. Consider whether changing the cadence mid-series is a valid user action, and if so whether it should apply to all future instances or only from the edited entry onwards. May require a separate DAO query or UI confirmation dialog. Related: `CalendarEvent.ConfirmUpdate` with THIS_AND_FUTURE uses the edited transaction's own date as the cutoff, so changing the date while editing a series entry also moves the cutoff.

**Complexity:** Low-Medium — design decision first, then a small DAO/use case change.

---

### 14. Tag-based Analytics
**Status:** To do

Filter and group transactions by tag for analytics screens. Requires `getByTag` query on `TransactionDao`. Initial implementation using SQL `LIKE` on serialised JSON tags field. Known limitation: fragile for partial tag name matches. Future improvement: junction table for reliable multi-tag AND/OR filtering.

**Complexity:** Medium — query complexity depends on whether junction table refactor is done first. Best tackled after item 11 is complete.

---

### 15. Merchant Input, Tag Input and Category/Type Consistency
**Status:** To do

The data model supports merchants and tags, and the sheet now exposes both.
- [x] Merchant dropdown with inline create, using the shared `CreatablePicker` (also used for categories); typing an existing name selects it instead of duplicating.
- [x] Tag input: `TagInput` chip field, one tag at a time (Done key, Add button, space or comma). Tags are normalised by `normalizeTag` (lowercase, no `#`, inner spaces become hyphens). Suggestions from existing tags were deliberately left out.
- [ ] Optionally enforce `category.type == transactionType` in the add and update use cases, handling the error gracefully rather than crashing the ViewModel.

**Complexity:** Medium — the tag chip input is the new piece.

---

### 16. Real Room Migrations
**Status:** Done (needs a first real migration to exercise it)

`fallbackToDestructiveMigration()` wipes every table on any version bump. Before the app holds real data, replace it with explicit `Migration` objects, tested with `MigrationTestHelper` against the schema files in `app/schemas/` (keep every shipped version's file).

Done: version 9 is the baseline, the destructive fallback is removed (a missing migration now fails loudly), and `MigrationTest` plus the schema-asset setup are in place. The procedure is in `CLAUDE.md` ("DB changes"). The test scaffold has not been run yet: `./gradlew connectedDebugAndroidTest` on a device or emulator.

**Complexity:** Small per migration; a habit change more than a feature.

---

### 17. Backup and Restore
**Status:** Done (needs on-device check)

A lossless, versioned export of all data (transactions, categories, merchants, savings goal) as JSON or CSV, saved to a location the user picks (Storage Access Framework), plus an Import that merges it back. Ids are UUIDs and inserts use REPLACE, so re-importing the same file is safe. The spreadsheet export (item 4) is a report layered on the same data, not a substitute for this.

Import matches categories (by type and name) and merchants (by name) against existing ones, so restoring onto a fresh install does not duplicate the seeded default categories.

Done: Export and Import in Settings (`BackupRepository`, `BackupDao`, `BackupFile`). The file holds transactions, categories, merchants, the savings goal and the pay-cycle day, and is validated before anything is written; import is all-or-nothing and never deletes. Settings shows the last backup time. Possible later additions: a periodic reminder if the last backup is old, scheduled automatic exports to a chosen folder, and an option to restore by replacing everything rather than merging.

**Complexity:** Medium.

---

### 18. Auto Backup Rules
**Status:** Done

Auto Backup was already on (`allowBackup="true"`). Added explicit `backup_rules.xml` and `data_extraction_rules.xml`: everything is backed up (including the Room database) except `auth_prefs`, which holds the password hash, from cloud backups. Note the pay-cycle setting shares that store, so it is not restored from cloud backups either.

---

### 19. Password Hashing
**Status:** Done (needs on-device check)

`PasswordHasher` uses an unsalted SHA-256, which is weak against guessing for short passwords. Replace with a salted, slow key-derivation function (e.g. PBKDF2, or Argon2/bcrypt via a library), with a per-user salt and a stored parameter version so existing hashes can be upgraded on next login. Consider splitting the pay-cycle setting into its own DataStore so backup rules can treat the two separately.

Done: `PasswordHasher` uses PBKDF2-HMAC-SHA256 with a random salt and 600,000 iterations, with the parameters stored in the hash string so they can be raised later. Existing passwords keep working: the old unsalted hash is still accepted and is replaced with a new one on the next successful login. `PasswordHasherTest` covers it (`./gradlew :app:testDebugUnitTest`). Not done: the pay-cycle setting still shares the `auth_prefs` store, so it is excluded from cloud backup along with the hash.

**Complexity:** Small-Medium.

---

### 20. UI Refresh: Light and Dark Themes
**Status:** Done (needs on-device check)

One layout with two palettes (light "Calm", dark "Focus"), chosen from design mockups.
- [x] Theme foundation: `WalletColors` (light and dark), Material colour scheme mapping, rounder shapes, Figtree variable font
- [x] Theme mode (System, Light, Dark): `ThemeMode`, stored in DataStore, `AppViewModel`, applied in `MainActivity` with system bar styling
- [x] Floating bottom bar with three tabs; the add button sits at the top right of the Transactions screen
- [x] Month pill showing the pay-cycle date range
- [x] Home: hero card, spending donut, savings goal card
- [x] Transactions: filter chips, day-grouped cards, new row with category avatar, merchant, tags and repeat icon
- [x] Add sheet: type control, keypad, category chips, detail chips with inline editors
- [x] Savings screen restyled; progress arc uses theme colours
- [x] Login screen keeps its old layout; the theme now supplies a background so the password text is readable in dark mode
- [x] Log out button in Settings: returns to the login screen (the saved password is kept)
- [ ] Optional: show/hide toggle on the password field
- [ ] Extra themes later: add a palette to `WalletColors.kt` and extend `ThemeMode`

**Notes:** the add sheet's keypad is a custom composable; the phone keyboard is still used for notes, merchant names, tags and new categories. Category colours come from hashing the name, so two categories can share a colour.

---

### 21. Timezone-safe Dates, App Identity and Release Signing
**Status:** Done (needs on-device check)

Preparation for using the app permanently.
- [x] Dates are stored as epoch days instead of local-midnight milliseconds, so they cannot shift by a day when the phone changes time zone. Database version 10 with `Migrations.MIGRATION_9_10` (converts existing rows) and a `MigrationTest` for it.
- [x] `applicationId` is now `com.personalwallot`, app name "Personal Wallot"; debug builds are `com.personalwallot.debug` ("Personal Wallot (dev)") with separate data.
- [x] Release signing configured from `keystore.properties`; without it the release build is signed with the debug key (fine for personal use; see CLAUDE.md).
- [ ] Build the release APK and install it; uninstall the old `com.example.personalfinances` app.
- [ ] Optional: create your own keystore (worth it if you build on several computers, share the app, or want to avoid the reinstall cycle).
- [ ] Commit the generated `app/schemas/.../10.json`.

---

### 22. Undo, Backup Reminder and Category/Merchant Management
**Status:** Done (needs on-device check)

- [x] Undo after deleting a transaction or a "this and future" series (restores the whole set)
- [x] Backup reminder card on Home (no backup yet, or last backup 14 or more days ago), with "Back up now" and "Later"
- [x] Manage screen (Settings, Data, then "Categories & merchants"): rename and delete, with usage counts. Blank and duplicate names refused; items in use cannot be deleted.
- [ ] Merge two categories or merchants (move a category's transactions to another, then delete it)
- [ ] Undo for other destructive actions (deleting a category or merchant is not undoable, but is only allowed when unused)

---

### 23. Currency Setting and Settings Screen
**Status:** Done (needs on-device check)

- [x] User-chosen currency (any ISO 4217 code, default the phone's), stored in DataStore, shown everywhere through `LocalMoneyFormatter`; included in the backup file. Display only: amounts are not converted.
- [x] Keypad decimals follow the currency (none for yen)
- [x] Settings moved from a crowded bottom sheet to a full screen of grouped cards (General, Appearance, Data, Account) with small pickers in dialogs; a searchable currency picker
- [x] Backup logic split into `BackupViewModel`, shared by Home (reminder) and Settings; `DashboardViewModel` slimmed down
- [x] Unit tests for money formatting and for the backup import rules
- [ ] Possible: per-transaction or per-account currencies (not supported: one currency for all amounts)

---

## Suggested Order
1 ✅ → 3 ✅ → 8 ✅ → 9 ✅ → 10 ✅ → 6 ✅ → 7 ~~dropped~~ → 11 ✅ → 18 ✅ → 15 → 16 → 17 → 19 → 20 ✅ → 2 → 12 → 13 → 14 → 4 → 5
