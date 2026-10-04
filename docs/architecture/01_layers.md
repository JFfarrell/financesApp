# Architecture - Layer Overview

High-level view of the four layers. Dependencies point inward only.

```mermaid
graph TD
    UI["UI Layer - Jetpack Compose"]
    VM["ViewModel Layer"]
    Domain["Domain Layer - pure Kotlin"]
    Data["Data Layer - Room and DataStore"]
    DB[("SQLite")]
    DS[("DataStore")]

    UI --> VM
    VM --> Domain
    Domain --> Data
    Data --> DB
    Data --> DS
```

## Rules

- UI knows about ViewModels only
- ViewModels know about use cases only
- Use cases know about repository interfaces only
- Repository implementations are the only place allowed to touch both domain models and storage
- Domain layer has zero imports from Android, Room, or Hilt
