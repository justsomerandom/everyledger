# EveryLedger

EveryLedger is planned as a generic embeddable and integratable double-entry ledger service for tracking arbitrary user-defined assets or units, such as fiat currencies, loyalty points, game currencies, compute credits, inventory units, tokens, or domain-specific units.

This repository currently contains only the initial project foundation. Ledger behavior has not been implemented yet.

## Goals

- Model a correct, auditable double-entry ledger for arbitrary asset types.
- Emphasize correctness, traceability, and explicit transaction semantics over feature count.
- Demonstrate Java and Spring architecture with domain-oriented packaging.
- Provide integration-friendly APIs for other systems once core ledger rules are implemented.

## Planned Features

- Asset types.
- Accounts.
- Immutable ledger entries.
- Transactions.
- Double-entry validation.
- Derived balances.
- Transaction metadata.
- Idempotency keys.
- External references.
- Auditability.
- Reconciliation.
- Snapshots and projections.
- Exchanges between asset types.
- Explicit recording of both sides of an exchange.
- Domain events.
- API integration into other systems.

## Architecture

The intended architecture centers on a domain model that validates transactions before immutable ledger entries are recorded. API and persistence code should depend on the domain model rather than bypassing it. Projections and snapshots can optimize reads without becoming the source of truth.

```mermaid
flowchart LR
    Client[Integrating System] --> Api[API Layer]
    Api --> Domain[Ledger Domain]
    Domain --> Validation[Double-entry Validation]
    Validation --> Entries[Immutable Ledger Entries]
    Entries --> Database[(PostgreSQL)]
    Entries --> Projections[Balances / Snapshots]
    Domain --> Events[Domain Events - planned]
```

## Repository Structure

- `src/main/java/com/everyledger/account` - planned account domain behavior.
- `src/main/java/com/everyledger/asset` - planned asset-type definitions and constraints.
- `src/main/java/com/everyledger/ledger` - planned ledger entry model and recording logic.
- `src/main/java/com/everyledger/transaction` - planned transaction orchestration and validation.
- `src/main/java/com/everyledger/exchange` - planned cross-asset exchange recording.
- `src/main/java/com/everyledger/reconciliation` - planned reconciliation workflows.
- `src/main/java/com/everyledger/event` - planned domain event publishing boundaries.
- `src/main/java/com/everyledger/api` - planned HTTP API layer.
- `src/main/java/com/everyledger/config` - planned application configuration.
- `src/main/resources/db/migration` - future Flyway or Liquibase migrations.
- `docs` - architecture and ledger model notes.

## Technology Stack

- Java - primary language.
- Maven - build tool.
- Spring Boot - planned application framework.
- Spring Web - planned HTTP API.
- Spring Data - planned persistence integration.
- Spring Security - planned where appropriate.
- PostgreSQL - planned persistence layer.
- Flyway or Liquibase - planned database migrations.
- Testcontainers - planned integration testing.
- OpenAPI - planned API documentation.
- Kafka - planned later only if domain events justify it.

Only the Maven project skeleton and directory structure are currently present.

## Development

Setup instructions will be expanded as implementation begins. No Spring application code, generated dependencies, or framework boilerplate has been added.

## Roadmap

- [ ] Define asset, account, transaction, and ledger entry domain model.
- [ ] Establish double-entry validation rules.
- [ ] Add persistence migrations for immutable entries.
- [ ] Add idempotency-key handling.
- [ ] Add balance projections and snapshot strategy.
- [ ] Add reconciliation workflows.
- [ ] Add HTTP API and OpenAPI documentation.
- [ ] Add integration tests with PostgreSQL.

## License

MIT. See `LICENSE`.

EveryLedger is under active development.
