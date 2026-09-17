# AGENTS.md

## Project Purpose

EveryLedger is intended to become a generic double-entry ledger service for arbitrary user-defined assets or units, with an emphasis on correctness, auditability, and integration into other systems.

## Engineering Priorities

- Correctness of double-entry validation and immutable ledger recording.
- Traceability through metadata, references, and audit history.
- Domain-oriented Java packaging.
- Security and authorization for integration APIs once introduced.
- Testability of financial-style invariants and failure cases.
- Observability for transaction recording, reconciliation, and projection updates.
- Minimal unnecessary dependencies.

## Architecture Rules

- Keep domain behavior organized by responsibility under `com.everyledger`.
- Do not create global controller/service/repository layers by default.
- Keep validation rules close to transaction and ledger domain code.
- Treat immutable ledger entries as the source of truth.
- Treat balances, snapshots, and projections as derived data.
- Keep exchange behavior explicit about both sides of the exchange.
- Keep API models from bypassing domain validation.

## Coding Guidelines

- Use idiomatic Java and Spring conventions where they support the domain.
- Prefer explicit domain types over loosely structured maps for ledger concepts.
- Do not introduce unnecessary abstractions.
- Do not silently change architecture or package ownership.
- Do not add technologies merely for resume value.
- Prefer well-maintained libraries.
- Preserve backwards compatibility once public APIs exist.
- Validate external input from APIs, events, and configuration.
- Keep secrets out of the repository.
- Avoid generated code unless justified and documented.
- Add tests with meaningful behavior changes.
- Document non-obvious design decisions.

## Testing

Future tests should emphasize ledger invariants, double-entry validation, idempotency, persistence consistency, reconciliation cases, API contracts, and integration tests with PostgreSQL using Testcontainers where appropriate.

## Documentation

Update `README.md`, `docs/architecture.md`, and `docs/ledger-model.md` when architecture, ledger semantics, API behavior, or user-visible behavior changes.

## Agent Workflow

Before making significant changes:

1. Inspect the existing architecture.
2. Understand relevant domain code.
3. Make the smallest coherent change.
4. Run relevant formatting, linting, and tests.
5. Summarize architectural consequences.

`AGENTS.md` may be expanded as this project matures.
