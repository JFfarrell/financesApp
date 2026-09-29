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
- **Dates:** Domain and entities use `LocalDate`; the `Converters` class (registered on `AppDatabase`) stores it as epoch millis, so mappers and repositories never convert manually. `DateUtils.monthDateRange` builds the inclusive date range for a month, honouring the user's pay-cycle start day.
- **Keyboard / IME handling:** `enableEdgeToEdge()` is called in `MainActivity` so the system reports IME insets. Screens that use `Scaffold` get this for free via `innerPadding`. Screens without a `Scaffold` (e.g. `LoginScreen`) need `Modifier.systemBarsPadding().imePadding()` on their root. `ModalBottomSheet` content needs `.imePadding()` on its Column, placed before `verticalScroll()` so the keyboard pushes content up and the user can scroll to any field.
- **DB changes:** Bump `version` in `AppDatabase.kt`. `fallbackToDestructiveMigration()` is set — no migration SQL needed during development, but existing data will be wiped on upgrade.
- **Pre-populating data:** `DatabaseModule` already seeds default categories (per transaction type) in a `RoomDatabase.Callback`. Add further seed rows there with raw SQL in `onCreate`.

## Database
- Room SQLite, database name: `personal_finances.db`
- Entities: `TransactionEntity`, `CategoryEntity`, `MerchantEntity`, `SavingsGoalEntity`
- Current version: 9 (full history in `AppDatabase.kt`)
- `fallbackToDestructiveMigration()` wipes all tables on any version bump; real migrations are needed before the app holds real data (see `BACKLOG.md`)

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
