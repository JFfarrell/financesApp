# Architecture - Domain Layer

Pure Kotlin. No Android, Room, or Hilt imports anywhere in this layer.

```mermaid
graph TD

    subgraph Models["Domain Models"]
        Transaction["Transaction"]
        Transaction ~~~ Category["Category"]
        Category ~~~ Merchant["Merchant"]
        Merchant ~~~ SavingsGoal["SavingsGoal"]
    end

    subgraph Enums["Enums"]
        TransactionType["TransactionType"]
        TransactionType ~~~ CadenceUnit["CadenceUnit"]
        CadenceUnit ~~~ ThemeMode["ThemeMode"]
    end

    subgraph Interfaces["Repository Interfaces"]
        TR["TransactionRepository"]
        TR ~~~ CR["CategoryRepository"]
        CR ~~~ MR["MerchantRepository"]
        MR ~~~ SGR["SavingsGoalRepository"]
        SGR ~~~ SR["SettingsRepository"]
    end

    subgraph UseCases["Use Cases"]
        TUC["Transaction Use Cases"]
        TUC ~~~ CUC["Category and Merchant Use Cases"]
        CUC ~~~ SUC["Savings Use Cases"]
        SUC ~~~ STUC["Settings Use Cases"]
    end

    Transaction --> TransactionType
    Transaction --> CadenceUnit
    Transaction --> Category
    Transaction --> Merchant
    Category --> TransactionType

    TUC --> TR
    CUC --> CR
    CUC --> MR
    SUC --> SGR
    STUC --> SR

    TR ~~~ Transaction
```

## Notes

- Each `Category` belongs to one `TransactionType`, so expense, income and savings each have their own category list.
- `ThemeMode` (System, Light, Dark) is the user's appearance choice, stored through `SettingsRepository` alongside the pay-cycle start day.
- Tags are free-form strings normalised by `normalizeTag` (lowercase, no spaces; inner spaces become hyphens).
- Savings is a `TransactionType`, not a separate model. The savings goal itself (`SavingsGoal`) holds only a target and a starting amount; the current total is derived from SAVING transactions.
