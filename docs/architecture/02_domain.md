# Architecture - Domain Layer

Pure Kotlin. No Android, Room, or Hilt imports anywhere in this layer.

```mermaid
graph TD

    subgraph Models["Domain Models"]
        Transaction["Transaction - new"]
        Transaction ~~~ Category["Category - new"]
        Category ~~~ Merchant["Merchant - new"]
        Merchant ~~~ Expense["Expense - legacy"]
        Expense ~~~ Income["Income - legacy"]
        Income ~~~ SavingsGoal["SavingsGoal"]
    end

    subgraph Enums["Enums"]
        TransactionType["TransactionType - new"]
        TransactionType ~~~ CadenceUnit["CadenceUnit - new"]
        CadenceUnit ~~~ ExpenseType["ExpenseType - legacy"]
        ExpenseType ~~~ IncomeType["IncomeType - legacy"]
    end

    subgraph Interfaces["Repository Interfaces"]
        TR["TransactionRepository - new"]
        TR ~~~ ER["ExpenseRepository - legacy"]
        ER ~~~ IR["IncomeRepository - legacy"]
        IR ~~~ SGR["SavingsGoalRepository"]
        SGR ~~~ SR["SettingsRepository"]
    end

    subgraph UseCases["Use Cases"]
        TUC["Transaction Use Cases - new"]
        TUC ~~~ EUC["Expense Use Cases - legacy"]
        EUC ~~~ IUC["Income Use Cases - legacy"]
        IUC ~~~ SUC["Savings Use Cases"]
        SUC ~~~ STUC["Settings Use Cases"]
    end

    Transaction --> TransactionType
    Transaction --> CadenceUnit
    Transaction --> Category
    Transaction --> Merchant
    Expense --> ExpenseType

    TUC --> TR
    EUC --> ER
    IUC --> IR
    SUC --> SGR
    STUC --> SR

    TR ~~~ Transaction
```

## Legend

- new: Part of the new unified Transaction model
- legacy: To be deleted after migration is complete
