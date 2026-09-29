# PersonalFinances — Claude Code Guide

## Project Overview
Android personal finance tracker built with Jetpack Compose, Room, Hilt, and Kotlin Coroutines. Follows Clean Architecture with MVVM. All data is stored locally (no network).

## Architecture
- **UI layer:** Jetpack Compose screens + ViewModels (`ui/screen/`, `ui/component/`)
- **Domain layer:** Use cases + repository interfaces + domain models (`domain/`)
- **Data layer:** Room entities, DAOs, mappers, repository implementations (`data/`)
- **DI:** Hilt modules in `di/` — `DatabaseModule`, `RepositoryModule`, `DataStoreModule`
- **Navigation:** `ui/navigation/NavGraph.kt` + `AppDestination.kt`

## Key Patterns
- **Adding a new entity:** Follow the existing category pattern — entity → DAO → mapper → domain model → repository interface + impl → use cases → ViewModel state + events → UI. Add the entity to `AppDatabase`, provide the DAO in `DatabaseModule` and bind the repository in `RepositoryModule`.
- **Unified transaction model:** Expenses, income and savings are all `Transaction`s distinguished by `TransactionType` (EXPENSE, INCOME, SAVING). Categories are user-defined entities scoped to one type; merchants are optional entities; tags are a `Set<String>` stored as JSON. `TransactionRepositoryImpl` resolves category and merchant ids into full objects before mapping to the domain model.
- **Reactive data:** All data flows through Kotlin `Flow`. Use `combine()` in ViewModels when a screen needs multiple data sources (see `SavingsViewModel`, which combines the savings goal with the savings total).
- **Theming:** `ui/theme/WalletColors.kt` holds one semantic palette per theme (`LightWalletColors`, `DarkWalletColors`); `PersonalFinancesTheme` maps it onto Material's colour scheme and exposes it as `MaterialTheme.wallet`. Screens take colours from there (income, saving, card, muted, hero, category colours), never hard-coded, so a new theme is just a new palette. Category colours are chosen by hashing the category name (`categoryColor`), so a category looks the same everywhere. The font is Figtree, bundled as a variable font in `res/font/` (licence in `third_party/figtree/`).
- **Light/dark mode:** the user's `ThemeMode` (System, Light, Dark) is stored in `SettingsDataStore` and read by `AppViewModel`; `MainActivity` picks the palette and re-applies system bar styling when it changes. It is chosen in the Settings sheet on Home, which also holds the pay-cycle day and Log out. Sign-in state is not stored anywhere: the app always starts at Login, and Log out just navigates back to it, popping the main screens.
- **Navigation and add button:** `BottomNavBar` is a floating three-tab bar. Adding a transaction is done from the + button at the top right of the Transactions screen, which owns the add sheet.
- **Pickers with inline create:** `ui/component/CreatablePicker.kt` is a generic dropdown with an optional "None" entry and a "+ New ..." option that reveals a name field. `AddTransactionBottomSheet.kt` uses it for the optional merchant editor; the category is chosen from chips (filtered to the selected transaction type) with an inline "+ New" option. Reuse `CreatablePicker` for any new pick-or-create field.
- **Tags:** entered with `ui/component/TagInput.kt` (removable chips, one at a time) and always passed through `normalizeTag` in the domain layer, so they are lowercase with no spaces. Tags are compared by exact match, so any new entry point must normalise too.
- **Dates:** Domain and entities use `LocalDate`; the `Converters` class (registered on `AppDatabase`) stores it as its epoch day, a plain day number with no time zone, so mappers and repositories never convert manually and stored dates cannot shift when the phone changes time zone. `DateUtils.monthDateRange` builds the inclusive date range for a month, honouring the user's pay-cycle start day.
- **Keyboard / IME handling:** `enableEdgeToEdge()` is called in `MainActivity` so the system reports IME insets. Screens that use `Scaffold` get this for free via `innerPadding`. Screens without a `Scaffold` (e.g. `LoginScreen`) need `Modifier.systemBarsPadding().imePadding()` on their root. `ModalBottomSheet` content needs `.imePadding()` on its Column, placed before `verticalScroll()` so the keyboard pushes content up and the user can scroll to any field.
- **DB changes:** Version 9 is the baseline for real data and there is no destructive fallback, so any schema change (entity fields, tables, indices, foreign keys) must: (1) bump `version` in `AppDatabase.kt`; (2) add a migration, either `@Database(autoMigrations = [AutoMigration(from = N, to = N + 1)])` for simple changes such as adding a column with a default, or a hand-written `Migration(N, N + 1)` added with `.addMigrations(...)` in `DatabaseModule`; (3) add a test to `app/src/androidTest/.../MigrationTest.kt` (see its notes) and run `./gradlew connectedDebugAndroidTest`; (4) commit the new `app/schemas/.../N+1.json` and never edit or delete an older one. Without a migration the app refuses to open the database rather than wiping it. On a throwaway test device you can uninstall the app instead.
- **Pre-populating data:** `DatabaseModule` already seeds default categories (per transaction type) in a `RoomDatabase.Callback`. Add further seed rows there with raw SQL in `onCreate`.

## Database
- Room SQLite, database name: `personal_finances.db`
- Entities: `TransactionEntity`, `CategoryEntity`, `MerchantEntity`, `SavingsGoalEntity`
- Current version: 10 (full history in `AppDatabase.kt`); dates are stored as epoch days, so they do not depend on the phone's time zone
- No destructive-migration fallback: schema changes need migrations (see "DB changes" above)
- **Password:** stored only as a PBKDF2-HMAC-SHA256 hash with a per-password salt (`PasswordHasher`), inside `auth_prefs`. Hashing is slow on purpose, so `AuthRepositoryImpl` runs it off the main thread. Old unsalted hashes are accepted once and upgraded on the next login. To strengthen it later, raise `ITERATIONS`; existing hashes are re-hashed automatically at the next login.
- **Undo on delete:** deleting a transaction (or a recurring series from a date) shows a snackbar with Undo for about ten seconds. `CalendarViewModel` keeps the removed transactions in `PendingUndo`; Undo re-adds them with their original ids. For a "this and future" delete the whole series is fetched first (the screen only holds the current month).
- **Backup reminder:** Home shows a card when there is data and no backup, or the last one is 14 or more days old (`REMINDER_AFTER_DAYS` in `DashboardViewModel`). "Back up now" opens the file picker directly; "Later" hides it until the next launch.
- **Categories and merchants:** Settings, then "Categories & merchants" opens `ManageScreen` to rename and delete them. The rules live in the use cases: names cannot be blank or duplicate (categories only clash within the same type), and an item that transactions use cannot be deleted. Renaming is safe because transactions reference the id.
- **Backup and restore:** Settings on Home has Export and Import. A backup is a versioned JSON file (`BackupFile`) written through the system file picker, containing transactions, categories, merchants, the savings goal and the pay-cycle day, never the password. Import validates everything first, then upserts inside one database transaction, and adds or replaces by id without deleting; a category (same type and name) or merchant (same name) that already exists is reused rather than duplicated, because a fresh install seeds its own default categories with new ids. Keep `BACKUP_FORMAT_VERSION` readable forever: raise it only for a change old readers cannot handle, and keep parsing older versions. Android Auto Backup (see `res/xml/`) also copies the database to the user's Google account, excluding the `auth_prefs` store.

## Identity, signing and releases
- `applicationId` is `com.personalwallot` (the app's identity on the phone); the source package `com.example.personalfinances` is only the `namespace`. **Never change `applicationId` once real data exists**: Android would treat it as a different app and the data would stay behind.
- Debug builds install as `com.personalwallot.debug` ("Personal Wallot (dev)") with separate data, so development can never touch the real data in the release app.
- Release builds are signed with your own key if `keystore.properties` exists (git-ignored; template in `keystore.properties.example`), otherwise with the debug key. **The debug key is tied to the computer that made it**: on a different machine (or after a reinstall) Android refuses to update the installed app in place. The recovery is Export, uninstall, reinstall, Import, which is safe because backup and restore work, but the password must be set again. A own keystore is optional and worth adding if you build on several computers, share the app, or want to avoid that cycle. If you do: **losing the keystore or its passwords brings back the same refusal**, so back up the `.jks` file and both passwords apart from this repo. Switching from the debug key to a keystore later needs the same Export, uninstall, reinstall, Import cycle once.
- Create a key (optional): `keytool -genkeypair -v -keystore personal-wallot.jks -alias personalwallot -keyalg RSA -keysize 4096 -validity 10000`
- Build: `./gradlew :app:assembleRelease` produces `app/build/outputs/apk/release/app-release.apk`; install or update with `adb install -r`. Raise `versionCode` (and `versionName`) in `app/build.gradle.kts` for each release you install over another.

## Code Style
- Add KDoc docstrings to all classes and functions — including composables, ViewModels, use cases, DAOs, repositories, and mappers. Briefly explain what each does and, where non-obvious, why.

## Working Style
- Explain each step as you go in plain language — what the code does, why it's structured that way, and how it fits into the overall architecture. The goal is for the developer to understand the technology deeply, not just end up with working code.

## Before Starting Work
Check `BACKLOG.md` for the current list of planned features and their status. Pick up the next "To do" item unless directed otherwise.

## Documentation
Keep all documentation in sync with the current state of the codebase at all times. This includes:
- `BACKLOG.md` — mark items as done when complete, update in-progress checklists as tasks are finished
- `docs/architecture/` — update diagrams when new files, layers, or relationships are added or removed
- `ARCHITECTURE.md` — update if the layer structure itself changes

## Testing
No automated tests currently exist. Verify changes manually by building and running the app on an emulator or device. Key flows to check after any change:
- Add / edit / delete a transaction of each type: expense, income and savings (one-off and recurring series)
- Create a category inline from the add sheet; the picker only shows categories for the selected type
- Calendar month navigation loads correct transactions
- Home screen summary and the Savings screen total update correctly
- Existing data is unaffected by schema changes (or wipe is expected and noted)
