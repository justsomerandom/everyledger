package com.everyledger.ledger;

import com.everyledger.account.Account;
import com.everyledger.asset.AssetType;
import java.math.BigDecimal;
import java.util.Objects;

public record LedgerEntry(Account account, EntryDirection direction, BigDecimal amount) {

  public LedgerEntry {
    Objects.requireNonNull(account, "Ledger entry account must not be null");
    Objects.requireNonNull(direction, "Ledger entry direction must not be null");
    Objects.requireNonNull(amount, "Ledger entry amount must not be null");
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Ledger entry amount must be greater than zero");
    }
  }

  public AssetType assetType() {
    return account.assetType();
  }
}
