package com.everyledger.transaction.application;

import com.everyledger.ledger.LedgerEntry;
import com.everyledger.transaction.LedgerTransaction;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;

public final class IdempotentPostingService {
  private final LedgerTransactionService transactions;
  private final LedgerTransactionRepository repository;
  private final IdempotencyRepository idempotency;
  private final IdempotencyRequestFingerprint fingerprint;

  public IdempotentPostingService(
      LedgerTransactionService transactions,
      LedgerTransactionRepository repository,
      IdempotencyRepository idempotency,
      IdempotencyRequestFingerprint fingerprint) {
    this.transactions = transactions;
    this.repository = repository;
    this.idempotency = idempotency;
    this.fingerprint = fingerprint;
  }

  @Transactional
  public LedgerTransaction post(
      UUID asset,
      List<LedgerEntry> entries,
      String reference,
      Map<String, Object> metadata,
      String key) {
    if (key == null || key.isBlank() || key.length() > 255) {
      throw new IllegalArgumentException("Idempotency-Key is required");
    }
    String hash = fingerprint.fingerprint(asset, entries, reference, metadata);
    if (!idempotency.claim(key, hash)) {
      IdempotencyRepository.Record prior = idempotency.lock(key).orElseThrow();
      if (!prior.requestHash().equals(hash)) {
        throw new IdempotencyConflictException(key);
      }
      return repository.findById(prior.transactionId()).orElseThrow();
    }
    LedgerTransaction transaction = transactions.post(asset, entries, reference, metadata);
    idempotency.complete(key, transaction.id());
    return transaction;
  }

  public static final class IdempotencyConflictException extends RuntimeException { public IdempotencyConflictException(String k){super("idempotency key reused with a different request: "+k);} }
}
