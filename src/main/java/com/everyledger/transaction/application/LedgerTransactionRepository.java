package com.everyledger.transaction.application;

import com.everyledger.ledger.LedgerEntry;
import com.everyledger.transaction.LedgerTransaction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerTransactionRepository {

  /** Atomically inserts the transaction and all entries; never overwrites existing records. */
  void save(LedgerTransaction transaction);

  default Optional<LedgerTransaction> findById(UUID transactionId) {
    return Optional.empty();
  }

  default List<LedgerTransaction> findAll() {
    return List.of();
  }

  /**
   * Returns all recorded entries for this account, retaining duplicates, ordered by transaction
   * timestamp, canonical UUID string, then entry position. Returns an empty list when none exist.
   */
  List<LedgerEntry> findEntriesByAccountId(UUID accountId);
}
