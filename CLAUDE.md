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
- **Reactive data:** All data flows through Kotlin `Flow`. Use `combine()` in ViewModels when a screen needs multiple data sources (see `ExpensesViewModel`).
- **Category dropdown UI:** See `AddExpenseBottomSheet.kt` — `ExposedDropdownMenuBox` with an inline "+ Add new category" dialog. Mirror this for any new category-like picker.
- **DB changes:** Bump `version` in `AppDatabase.kt`. `fallbackToDestructiveMigration()` is set — no migration SQL needed during development, but existing data will be wiped on upgrade.
- **Pre-populating data:** Add a `RoomDatabase.Callback` in `DatabaseModule` and insert seed rows in `onCreate`.

## Database
- Room SQLite, database name: `personal_finances.db`
- Entities: `ExpenseEntity`, `IncomeEntity`, `CategoryEntity`, `SavingsGoalEntity`
- Current version: 1

## Code Style
- Add KDoc docstrings to all classes and functions — including composables, ViewModels, use cases, DAOs, repositories, and mappers. Briefly explain what each does and, where non-obvious, why.

## Working Style
- Explain each step as you go in plain language — what the code does, why it's structured that way, and how it fits into the overall architecture. The goal is for the developer to understand the technology deeply, not just end up with working code.

## Before Starting Work
Check `BACKLOG.md` for the current list of planned features and their status. Pick up the next "To do" item unless directed otherwise.

## Testing
No automated tests currently exist. Verify changes manually by building and running the app on an emulator or device. Key flows to check after any change:
- Add / delete an expense with a category
- Add / delete an income entry
- Dashboard monthly summary updates correctly
- Existing data is unaffected by schema changes (or wipe is expected and noted)
