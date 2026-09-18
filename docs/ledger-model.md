# Ledger Model

An `AssetType` identifies a unit by UUID, code, and display name. An `Account` has a UUID, name, and exactly one asset type. Names and codes must be nonblank. Accounts contain no balance field.

Each immutable `LedgerEntry` references an account, a `DEBIT` or `CREDIT` direction, and a strictly positive `BigDecimal` amount. Its asset type comes from its account. A `LedgerTransaction` contains a UUID, timestamp, `RECORDED` status, and an immutable entry list. Construction requires at least two entries, equal asset types, and numerically equal debit/credit totals using `BigDecimal.compareTo`.

Posting checks that the expected asset type and all accounts exist and that every persisted account belongs to the expected asset identifier. It delegates balancing to the transaction constructor and records only a complete valid aggregate.

An account's balance is **sum of debits minus sum of credits**. Positive values therefore mean net debits, negative values mean net credits. This is a uniform sign convention for generic units; account categories or normal-balance rules are not modeled. Calculations use exact BigDecimal addition/subtraction without rounding or scale normalization.

Every balance query derives its result from persisted entries, including repeated entries, with zero for an existing account without history. Unknown accounts are rejected. History returns only entries for the requested account as an immutable snapshot; transaction metadata is not included in this entry-only view. No balances, projections, or snapshots are stored.
