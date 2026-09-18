package com.everyledger.transaction.application;

import com.everyledger.account.Account;
import com.everyledger.account.application.AccountNotFoundException;
import com.everyledger.account.application.AccountRepository;
import com.everyledger.asset.application.AssetTypeNotFoundException;
import com.everyledger.asset.application.AssetTypeRepository;
import com.everyledger.ledger.LedgerEntry;
import com.everyledger.transaction.LedgerTransaction;
import com.everyledger.transaction.TransactionStatus;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class LedgerTransactionService {

  private final AssetTypeRepository assetTypes;
  private final AccountRepository accounts;
  private final LedgerTransactionRepository transactions;
  private final Clock clock;

  public LedgerTransactionService(
      AssetTypeRepository assetTypes,
      AccountRepository accounts,
      LedgerTransactionRepository transactions,
      Clock clock) {
    this.assetTypes = Objects.requireNonNull(assetTypes);
    this.accounts = Objects.requireNonNull(accounts);
    this.transactions = Objects.requireNonNull(transactions);
    this.clock = Objects.requireNonNull(clock);
  }

  public LedgerTransaction post(UUID expectedAssetTypeId, List<LedgerEntry> entries) {
    Objects.requireNonNull(expectedAssetTypeId, "Expected asset type id must not be null");
    List<LedgerEntry> requestedEntries = List.copyOf(entries);
    assetTypes.findById(expectedAssetTypeId)
        .orElseThrow(() -> new AssetTypeNotFoundException(expectedAssetTypeId));

    List<LedgerEntry> resolvedEntries = new ArrayList<>();
    for (LedgerEntry entry : requestedEntries) {
      UUID accountId = entry.account().id();
      Account account = accounts.findById(accountId)
          .orElseThrow(() -> new AccountNotFoundException(accountId));
      if (!account.assetType().id().equals(expectedAssetTypeId)) {
        throw new IllegalArgumentException(
            "Account " + accountId + " does not use expected asset type " + expectedAssetTypeId);
      }
      resolvedEntries.add(new LedgerEntry(account, entry.direction(), entry.amount()));
    }

    LedgerTransaction transaction = new LedgerTransaction(
        UUID.randomUUID(), clock.instant(), TransactionStatus.RECORDED, resolvedEntries);
    transactions.save(transaction);
    return transaction;
  }
}
