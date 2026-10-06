package com.everyledger.account.application;

import com.everyledger.account.Account;
import com.everyledger.asset.AssetType;
import com.everyledger.asset.application.AssetTypeNotFoundException;
import com.everyledger.asset.application.AssetTypeRepository;
import com.everyledger.ledger.LedgerEntry;
import com.everyledger.transaction.application.LedgerTransactionRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AccountService {

  private final AccountRepository accounts;
  private final AssetTypeRepository assetTypes;
  private final LedgerTransactionRepository transactions;
  private final AccountBalanceRepository balances;

  public AccountService(
      AccountRepository accounts,
      AssetTypeRepository assetTypes,
      LedgerTransactionRepository transactions) {
    this(accounts, assetTypes, transactions,
        accountId -> deriveBalance(transactions.findEntriesByAccountId(accountId)));
  }

  public AccountService(
      AccountRepository accounts,
      AssetTypeRepository assetTypes,
      LedgerTransactionRepository transactions,
      AccountBalanceRepository balances) {
    this.accounts = Objects.requireNonNull(accounts);
    this.assetTypes = Objects.requireNonNull(assetTypes);
    this.transactions = Objects.requireNonNull(transactions);
    this.balances = Objects.requireNonNull(balances);
  }

  public Account create(String name, UUID assetTypeId) {
    Objects.requireNonNull(assetTypeId, "Asset type id must not be null");
    AssetType assetType = assetTypes.findById(assetTypeId)
        .orElseThrow(() -> new AssetTypeNotFoundException(assetTypeId));
    Account account = new Account(UUID.randomUUID(), name, assetType);
    accounts.save(account);
    return account;
  }

  public List<LedgerEntry> history(UUID accountId) {
    Objects.requireNonNull(accountId, "Account id must not be null");
    accounts.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
    return List.copyOf(transactions.findEntriesByAccountId(accountId));
  }

  /** Returns total debits minus total credits without rounding. */
  public BigDecimal balance(UUID accountId) {
    Objects.requireNonNull(accountId, "Account id must not be null");
    accounts.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
    return balances.findBalance(accountId);
  }

  private static BigDecimal deriveBalance(List<LedgerEntry> entries) {
    BigDecimal balance = BigDecimal.ZERO;
    for (LedgerEntry entry : entries) {
      balance = switch (entry.direction()) {
        case DEBIT -> balance.add(entry.amount());
        case CREDIT -> balance.subtract(entry.amount());
      };
    }
    return balance;
  }
}
