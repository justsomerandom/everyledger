package com.everyledger.transaction;

import com.everyledger.asset.AssetType;
import com.everyledger.ledger.EntryDirection;
import com.everyledger.ledger.LedgerEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record LedgerTransaction(
    UUID id, Instant timestamp, TransactionStatus status, List<LedgerEntry> entries) {

  public LedgerTransaction {
    Objects.requireNonNull(id, "Ledger transaction id must not be null");
    Objects.requireNonNull(timestamp, "Ledger transaction timestamp must not be null");
    Objects.requireNonNull(status, "Ledger transaction status must not be null");
    Objects.requireNonNull(entries, "Ledger transaction entries must not be null");

    entries = List.copyOf(entries);
    if (entries.size() < 2) {
      throw new IllegalArgumentException("Ledger transaction must contain at least two entries");
    }

    validateSingleAssetType(entries);
    validateBalanced(entries);
  }

  private static void validateSingleAssetType(List<LedgerEntry> entries) {
    AssetType assetType = entries.getFirst().assetType();
    for (LedgerEntry entry : entries) {
      if (!assetType.equals(entry.assetType())) {
        throw new IllegalArgumentException(
            "Ledger transaction entries must use accounts with the same asset type");
      }
    }
  }

  private static void validateBalanced(List<LedgerEntry> entries) {
    BigDecimal debits = BigDecimal.ZERO;
    BigDecimal credits = BigDecimal.ZERO;

    for (LedgerEntry entry : entries) {
      if (entry.direction() == EntryDirection.DEBIT) {
        debits = debits.add(entry.amount());
      } else if (entry.direction() == EntryDirection.CREDIT) {
        credits = credits.add(entry.amount());
      } else {
        throw new IllegalArgumentException("Ledger entry direction is not supported");
      }
    }

    if (debits.compareTo(credits) != 0) {
      throw new IllegalArgumentException("Ledger transaction debits must equal credits");
    }
  }
}
