# Architecture - Data Layer

Handles all storage. Repository implementations are the only place that touch both domain models and Room entities simultaneously.

```mermaid
graph TD

    subgraph Impls["Repository Implementations"]
        TRI["TransactionRepositoryImpl"]
        TRI ~~~ CRI["CategoryRepositoryImpl"]
        CRI ~~~ MRI["MerchantRepositoryImpl"]
        MRI ~~~ SGRI["SavingsGoalRepositoryImpl"]
        SGRI ~~~ BRI["BackupRepositoryImpl"]
        BRI ~~~ RRI["ReportRepositoryImpl"]
        RRI ~~~ ABS["AutoBackupScheduler and Worker"]
    end

    subgraph Mappers["Mappers"]
        TM["TransactionMapper"]
        TM ~~~ CM["CategoryMapper"]
        CM ~~~ MM["MerchantMapper"]
        MM ~~~ SGM["SavingsGoalMapper"]
    end

    subgraph DAOs["DAOs"]
        TD["TransactionDao"]
        TD ~~~ CD["CategoryDao"]
        CD ~~~ MD["MerchantDao"]
        MD ~~~ SGD["SavingsGoalDao"]
        SGD ~~~ BD["BackupDao"]
    end

    subgraph Entities["Room Entities"]
        TE["TransactionEntity"]
        TE ~~~ CE["CategoryEntity"]
        CE ~~~ ME["MerchantEntity"]
        ME ~~~ SGE["SavingsGoalEntity"]
    end

    DB[("SQLite - personal_finances.db")]

    TRI --> TD
    TRI --> CD
    TRI --> MD
    TRI --> TM
    TRI --> CM
    TRI --> MM
    CRI --> CD
    CRI --> CM
    MRI --> MD
    MRI --> MM
    RRI --> XW["AnnualReportWorkbook and XlsxWriter"]
    ABS --> BD
    ABS --> BRI
    BRI --> BD
    BRI --> BF["BackupFile - JSON"]
    SGRI --> SGD
    SGRI --> SGM

    TD --> TE
    CD --> CE
    MD --> ME
    SGD --> SGE

    TE --> DB
    CE --> DB
    ME --> DB
    SGE --> DB

    TM ~~~ TE
```

## Mapper conversions

| Entity field | Entity type | Domain field | Domain type |
|---|---|---|---|
| date | LocalDate (Room TypeConverter stores as Long) | date | LocalDate - no manual conversion |
| transactionType | String | transactionType | TransactionType enum |
| cadenceUnit | String | cadenceUnit | CadenceUnit enum |
| category transactionType | String | Category.type | TransactionType enum |
| tags | String JSON | tags | Set of String |
| categoryId | String FK | category | Category object - resolved in repo |
| merchantId | String FK nullable | merchant | Merchant object nullable - resolved in repo |

## Notes

- `Converters` is a concrete class registered with `@TypeConverters` on `AppDatabase`. It cannot be `AppDatabase` itself because Room instantiates the converter class and `AppDatabase` is abstract.
- There is no destructive-migration fallback. Version 9 is the baseline; later schema changes need migrations and a `MigrationTest`.
- `AutoBackupScheduler` watches the data tables and queues an `AutoBackupWorker` (WorkManager) after changes; the worker calls `BackupRepositoryImpl.runAutoBackup`, which writes a dated file into the user's chosen folder through `BackupFolder` (Storage Access Framework) and prunes old ones (`AutoBackupNaming`).
- `XlsxWriter` writes Excel files with no library (an .xlsx is a zip of XML); `AnnualReportWorkbook` lays out the five sheets of the user's budget template (Setup, Expenses, Income, Summary, Spendings), with Expenses holding recurring expenses and Spendings the once-off ones, `XlsxStyleSheet` generates their styles and `ReportFormats` the currency formats. All plain Kotlin with unit tests.
- `BackupDao` does bulk reads and `@Upsert` writes for backup and restore only. Backups use their own JSON shapes (`BackupFile`), separate from the Room entities, so the database can change without breaking old backups.
