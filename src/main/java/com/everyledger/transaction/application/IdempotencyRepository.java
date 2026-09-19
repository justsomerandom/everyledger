package com.everyledger.transaction.application;

import java.util.Optional;
import java.util.UUID;

/** Database-backed claim storage; methods participate in the caller's transaction. */
public interface IdempotencyRepository {
  boolean claim(String key, String requestHash);
  Optional<Record> lock(String key);
  void complete(String key, UUID transactionId);

  record Record(String key, String requestHash, UUID transactionId) { }
}
