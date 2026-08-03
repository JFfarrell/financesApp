# Architecture - Data Layer

Handles all storage. Repository implementations are the only place that touch both domain models and Room entities simultaneously.

```mermaid
graph TD

    subgraph Impls["Repository Implementations"]
        TRI["TransactionRepositoryImpl - new"]
        TRI ~~~ ERI["ExpenseRepositoryImpl - legacy"]
        ERI ~~~ IRI["IncomeRepositoryImpl - legacy"]
        IRI ~~~ SGRI["SavingsGoalRepositoryImpl"]
    end

    subgraph Mappers["Mappers"]
        TM["TransactionMapper - new"]
        TM ~~~ CM["CategoryMapper - new"]
        CM ~~~ MM["MerchantMapper - new"]
        MM ~~~ EM["ExpenseMapper - legacy"]
        EM ~~~ IM["IncomeMapper - legacy"]
    end

    subgraph DAOs["DAOs"]
        TD["TransactionDao - new"]
        TD ~~~ CD["CategoryDao - new"]
        CD ~~~ MD["MerchantDao - new"]
        MD ~~~ ED["ExpenseDao - legacy"]
        ED ~~~ ID2["IncomeDao - legacy"]
    end

    subgraph Entities["Room Entities"]
        TE["TransactionEntity - new"]
        TE ~~~ CE["CategoryEntity - new"]
        CE ~~~ ME["MerchantEntity - new"]
        ME ~~~ EE["ExpenseEntity - legacy"]
        EE ~~~ IE["IncomeEntity - legacy"]
    end

    DB[("SQLite - personal_finances.db")]

    TRI --> TD
    TRI --> CD
    TRI --> MD
    TRI --> TM
    TRI --> CM
    TRI --> MM
    ERI --> ED
    ERI --> EM
    IRI --> ID2
    IRI --> IM

    TD --> TE
    CD --> CE
    MD --> ME
    ED --> EE
    ID2 --> IE

    TE --> DB
    CE --> DB
    ME --> DB
    EE --> DB
    IE --> DB

    TM ~~~ TE
```

## Mapper conversions

| Entity field | Entity type | Domain field | Domain type |
|---|---|---|---|
| date | Long | date | LocalDate |
| transactionType | String | transactionType | TransactionType enum |
| cadenceUnit | String | cadenceUnit | CadenceUnit enum |
| tags | String JSON | tags | Set of String |
| categoryId | String FK | category | Category object - resolved in repo |
| merchantId | String FK nullable | merchant | Merchant object nullable - resolved in repo |
