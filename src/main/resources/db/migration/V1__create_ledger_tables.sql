CREATE TABLE asset_types (
  id UUID PRIMARY KEY,
  code VARCHAR(128) NOT NULL UNIQUE,
  display_name VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE accounts (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  asset_type_id UUID NOT NULL REFERENCES asset_types(id),
  created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE ledger_transactions (
  id UUID PRIMARY KEY,
  recorded_at TIMESTAMPTZ NOT NULL,
  status VARCHAR(32) NOT NULL CHECK (status = 'RECORDED'),
  external_reference VARCHAR(255),
  metadata JSONB NOT NULL DEFAULT '{}'::jsonb
);
CREATE TABLE ledger_entries (
  transaction_id UUID NOT NULL REFERENCES ledger_transactions(id),
  position INTEGER NOT NULL CHECK (position >= 0),
  account_id UUID NOT NULL REFERENCES accounts(id),
  direction VARCHAR(16) NOT NULL CHECK (direction IN ('DEBIT', 'CREDIT')),
  amount NUMERIC(38,18) NOT NULL CHECK (amount > 0),
  PRIMARY KEY (transaction_id, position)
);
CREATE INDEX ledger_entries_account_history_idx ON ledger_entries(account_id, transaction_id, position);
CREATE INDEX ledger_transactions_recorded_idx ON ledger_transactions(recorded_at, id);
CREATE TABLE idempotency_keys (
  idempotency_key VARCHAR(255) PRIMARY KEY,
  request_hash CHAR(64) NOT NULL,
  transaction_id UUID UNIQUE REFERENCES ledger_transactions(id),
  created_at TIMESTAMPTZ NOT NULL
);
