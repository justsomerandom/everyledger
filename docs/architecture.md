# Architecture

Domain records remain framework-independent under the existing `asset`, `account`, `ledger`, and `transaction` packages. Each feature's `application` subpackage owns its use cases and persistence ports. Services are plain Java with constructor injection; there are no production repository implementations or Spring service annotations.

- `AssetTypeService.register` constructs and saves an asset type.
- `AccountService.create` loads the asset type before constructing and saving an account. `history` verifies the account exists and returns an immutable entry snapshot; `balance` sums that history.
- `LedgerTransactionService.post` verifies the expected asset exists, reloads every referenced account, checks its asset identifier, constructs a validated transaction, and saves it once. Domain constructors enforce amount and transaction invariants.

Posting accepts a list of existing domain `LedgerEntry` values to avoid introducing DTOs. Only account identifiers, directions, and amounts are taken from these inputs. Account names and asset membership are resolved through the account repository. The returned transaction contains these resolved accounts.

## Repository Contracts

`AssetTypeRepository` and `AccountRepository` expose `findById`, returning `Optional.empty()` for a missing identifier, and `save` for inserting a new domain object. `LedgerTransactionRepository` exposes an atomic insert of a complete transaction and `findEntriesByAccountId` for all its recorded entries. No port returns null. Inserts must reject existing identifiers rather than overwrite them.

History preserves repeated entries and orders them by transaction timestamp, canonical UUID string, then position within the transaction. Empty history is valid for an existing account. Missing accounts/assets produce explicit application exceptions. There is no pagination or stored balance.

## Future Persistence

PostgreSQL/JPA adapters must map immutable domain records, preserve entry position and duplicates, enforce references, and commit each transaction plus all its entries atomically. Database transaction boundaries and concurrency protection remain adapter/wiring work. History queries must provide a consistent snapshot of committed entries and honor the port's specified ordering.

Services generate UUIDs; posting timestamps come from an injected `Clock`. Only `RECORDED` transactions exist. No code uniqueness rule has been introduced. Accounts' asset memberships are immutable in the current model. Domain asset equality currently includes identifier, code, and display name, so adapters must hydrate consistent asset values across accounts. Exact BigDecimal values must be preserved without implicit rounding; database precision/scale limits must be decided explicitly before adding storage.
