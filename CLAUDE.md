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
- **Adding a new entity:** Follow the existing category pattern — entity → DAO → mapper → domain model → repository interface + impl → use cases → ViewModel state + events → UI. Register the DAO in `DatabaseModule` and bind the repository in `RepositoryModule`.
- **Reactive data:** All data flows through Kotlin `Flow`. Use `combine()` in ViewModels when a screen needs multiple data sources (see `CalendarViewModel` which combines expenses + income).
- **Hierarchical type picker:** See `HierarchicalTypeField.kt` — two-step `ExposedDropdownMenuBox` (category → subtype). See `AddExpenseBottomSheet.kt` for usage. Mirror this for any new two-level picker.
- **Keyboard / IME handling:** `enableEdgeToEdge()` is called in `MainActivity` so the system reports IME insets. Screens that use `Scaffold` get this for free via `innerPadding`. Screens without a `Scaffold` (e.g. `LoginScreen`) need `Modifier.systemBarsPadding().imePadding()` on their root. `ModalBottomSheet` content needs `.imePadding()` on its Column, placed before `verticalScroll()` so the keyboard pushes content up and the user can scroll to any field.
- **DB changes:** Bump `version` in `AppDatabase.kt`. `fallbackToDestructiveMigration()` is set — no migration SQL needed during development, but existing data will be wiped on upgrade.
- **Pre-populating data:** Add a `RoomDatabase.Callback` in `DatabaseModule` and insert seed rows in `onCreate`.

## Database
- Room SQLite, database name: `personal_finances.db`
- Entities: `ExpenseEntity`, `IncomeEntity`, `SavingsGoalEntity`
- Current version: 6 (full history in `AppDatabase.kt`)

## Code Style
- Add KDoc docstrings to all classes and functions — including composables, ViewModels, use cases, DAOs, repositories, and mappers. Briefly explain what each does and, where non-obvious, why.

## Working Style
- Explain each step as you go in plain language — what the code does, why it's structured that way, and how it fits into the overall architecture. The goal is for the developer to understand the technology deeply, not just end up with working code.

## Before Starting Work
Check `BACKLOG.md` for the current list of planned features and their status. Pick up the next "To do" item unless directed otherwise.

## Testing
No automated tests currently exist. Verify changes manually by building and running the app on an emulator or device. Key flows to check after any change:
- Add / delete an expense (one-off and recurring series)
- Add / delete an income entry (one-off and recurring series)
- Calendar month navigation loads correct transactions
- Home screen summary updates correctly
- Existing data is unaffected by schema changes (or wipe is expected and noted)
