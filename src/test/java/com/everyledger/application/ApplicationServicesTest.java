package com.everyledger.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.everyledger.account.Account;
import com.everyledger.account.application.AccountNotFoundException;
import com.everyledger.account.application.AccountRepository;
import com.everyledger.account.application.AccountService;
import com.everyledger.asset.AssetType;
import com.everyledger.asset.application.AssetTypeNotFoundException;
import com.everyledger.asset.application.AssetTypeRepository;
import com.everyledger.asset.application.AssetTypeService;
import com.everyledger.ledger.EntryDirection;
import com.everyledger.ledger.LedgerEntry;
import com.everyledger.transaction.LedgerTransaction;
import com.everyledger.transaction.TransactionStatus;
import com.everyledger.transaction.application.LedgerTransactionRepository;
import com.everyledger.transaction.application.LedgerTransactionService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApplicationServicesTest {

  private final FakeAssetTypes assets = new FakeAssetTypes();
  private final FakeAccounts accounts = new FakeAccounts();
  private final FakeTransactions transactions = new FakeTransactions();
  private final Clock clock = Clock.fixed(Instant.parse("2026-09-18T12:00:00Z"), ZoneOffset.UTC);
  private final AssetTypeService assetService = new AssetTypeService(assets);
  private final AccountService accountService = new AccountService(accounts, assets, transactions);
  private final LedgerTransactionService transactionService =
      new LedgerTransactionService(assets, accounts, transactions, clock);
  private final AssetType points = assetService.register("PTS", "Points");
  private final Account first = accountService.create("First", points.id());
  private final Account second = accountService.create("Second", points.id());

  @Test
  void registersAssetType() {
    AssetType asset = assetService.register("CRD", "Credits");

    assertEquals("CRD", asset.code());
    assertEquals("Credits", asset.displayName());
    assertEquals(Optional.of(asset), assets.findById(asset.id()));
  }

  @Test
  void createsAccountForExistingAssetType() {
    Account account = accountService.create("Inventory", points.id());

    assertEquals("Inventory", account.name());
    assertEquals(points, account.assetType());
    assertEquals(Optional.of(account), accounts.findById(account.id()));
  }

  @Test
  void rejectsUnknownAssetWhenCreatingAccount() {
    UUID missingId = UUID.randomUUID();
    int before = accounts.values.size();

    var error = assertThrows(AssetTypeNotFoundException.class,
        () -> accountService.create("Unknown", missingId));

    assertTrue(error.getMessage().contains(missingId.toString()));
    assertEquals(before, accounts.values.size());
  }

  @Test
  void postsBalancedTransactionUsingRepositoryAccounts() {
    Account supplied = new Account(first.id(), "Caller supplied name", points);
    LedgerTransaction transaction = transactionService.post(points.id(), List.of(
        entry(supplied, EntryDirection.DEBIT, "10.0"),
        entry(second, EntryDirection.CREDIT, "10.00")));

    assertEquals(clock.instant(), transaction.timestamp());
    assertEquals(TransactionStatus.RECORDED, transaction.status());
    assertSame(first, transaction.entries().getFirst().account());
    assertEquals(List.of(transaction), List.copyOf(transactions.values.values()));
  }

  @Test
  void rejectsUnknownAccountWithoutSaving() {
    Account unknown = new Account(UUID.randomUUID(), "Unknown", points);

    var error = assertThrows(AccountNotFoundException.class,
        () -> post(first, unknown, "5"));

    assertTrue(error.getMessage().contains(unknown.id().toString()));
    assertTrue(transactions.values.isEmpty());
  }

  @Test
  void rejectsUnknownExpectedAssetWithoutSaving() {
    assertThrows(AssetTypeNotFoundException.class,
        () -> transactionService.post(UUID.randomUUID(), List.of(
            entry(first, EntryDirection.DEBIT, "5"),
            entry(second, EntryDirection.CREDIT, "5"))));
    assertTrue(transactions.values.isEmpty());
  }

  @Test
  void rejectsWrongAssetEvenWhenCallerSuppliesExpectedAsset() {
    AssetType credits = assetService.register("CRD", "Credits");
    Account other = accountService.create("Other", credits.id());
    Account forged = new Account(other.id(), other.name(), points);

    var error = assertThrows(IllegalArgumentException.class,
        () -> post(first, forged, "5"));

    assertTrue(error.getMessage().contains("expected asset type"));
    assertTrue(transactions.values.isEmpty());
  }

  @Test
  void rejectsBalancedTransactionWhenAllAccountsUseAnotherAsset() {
    AssetType credits = assetService.register("CRD", "Credits");

    assertThrows(IllegalArgumentException.class,
        () -> transactionService.post(credits.id(), List.of(
            entry(first, EntryDirection.DEBIT, "5"),
            entry(second, EntryDirection.CREDIT, "5"))));
    assertTrue(transactions.values.isEmpty());
  }

  @Test
  void delegatesTransactionValidationBeforeSaving() {
    assertThrows(IllegalArgumentException.class,
        () -> transactionService.post(points.id(), List.of(
            entry(first, EntryDirection.DEBIT, "5"),
            entry(second, EntryDirection.CREDIT, "4"))));
    assertThrows(IllegalArgumentException.class,
        () -> transactionService.post(points.id(), List.of()));
    assertTrue(transactions.values.isEmpty());
  }

  @Test
  void returnsAccountHistoryAsImmutableSnapshotWithRepeatedEntries() {
    LedgerEntry debit = entry(first, EntryDirection.DEBIT, "2");
    LedgerTransaction transaction = transactionService.post(points.id(), List.of(
        debit, debit, entry(second, EntryDirection.CREDIT, "4")));
    Account unrelated = accountService.create("Unrelated", points.id());
    post(unrelated, second, "7");

    List<LedgerEntry> history = accountService.history(first.id());

    assertEquals(transaction.entries().subList(0, 2), history);
    assertThrows(UnsupportedOperationException.class, history::clear);
    post(first, second, "1");
    assertEquals(2, history.size());
    assertEquals(3, accountService.history(first.id()).size());
  }

  @Test
  void calculatesPositiveBalanceWithExactArithmetic() {
    post(first, second, "12345678901234567890.123456789");
    post(second, first, "0.000000001");

    assertBalance("12345678901234567890.123456788", first);
  }

  @Test
  void returnsHistoryInTransactionOrderThenEntryPosition() {
    Instant earlier = clock.instant().minusSeconds(1);
    LedgerEntry one = entry(first, EntryDirection.DEBIT, "1");
    LedgerEntry two = entry(first, EntryDirection.DEBIT, "2");
    LedgerEntry three = entry(first, EntryDirection.CREDIT, "3");
    transactions.save(new LedgerTransaction(
        UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), earlier,
        TransactionStatus.RECORDED, List.of(three, entry(second, EntryDirection.DEBIT, "3"))));
    transactions.save(new LedgerTransaction(
        UUID.fromString("00000000-0000-0000-0000-000000000001"), earlier,
        TransactionStatus.RECORDED, List.of(one, two, entry(second, EntryDirection.CREDIT, "3"))));
    post(first, second, "4");

    assertEquals(List.of(one, two, three, entry(first, EntryDirection.DEBIT, "4")),
        accountService.history(first.id()));
  }

  @Test
  void calculatesNegativeBalance() {
    post(first, second, "0.1");
    post(second, first, "0.3");

    assertBalance("-0.2", first);
  }

  @Test
  void calculatesZeroWhenDebitsAndCreditsCancelAcrossScales() {
    post(first, second, "10.0");
    post(second, first, "10.00");

    assertBalance("0", first);
  }

  @Test
  void returnsEmptyHistoryAndZeroBalanceForUnusedAccount() {
    assertEquals(List.of(), accountService.history(first.id()));
    assertBalance("0", first);
  }

  @Test
  void derivesBalanceFromNewlyPersistedEntriesWithoutChangingAccount() {
    Account original = accounts.findById(first.id()).orElseThrow();
    assertBalance("0", first);
    post(first, second, "7");
    assertBalance("7", first);
    post(second, first, "9");
    assertBalance("-2", first);

    assertSame(original, accounts.findById(first.id()).orElseThrow());
    assertEquals(2, accounts.saveCount);
  }

  @Test
  void rejectsHistoryAndBalanceForUnknownAccount() {
    UUID unknown = UUID.randomUUID();

    assertThrows(AccountNotFoundException.class, () -> accountService.history(unknown));
    assertThrows(AccountNotFoundException.class, () -> accountService.balance(unknown));
  }

  @Test
  void delegatesNameValidationWithoutSavingInvalidObjects() {
    assertThrows(IllegalArgumentException.class, () -> assetService.register(" ", "Name"));
    assertThrows(IllegalArgumentException.class, () -> assetService.register("CODE", " "));
    assertThrows(IllegalArgumentException.class, () -> accountService.create(" ", points.id()));
    assertEquals(1, assets.values.size());
    assertEquals(2, accounts.values.size());
  }

  private void post(Account debit, Account credit, String amount) {
    transactionService.post(points.id(), List.of(
        entry(debit, EntryDirection.DEBIT, amount),
        entry(credit, EntryDirection.CREDIT, amount)));
  }

  private void assertBalance(String expected, Account account) {
    assertEquals(0, new BigDecimal(expected).compareTo(accountService.balance(account.id())));
  }

  private static LedgerEntry entry(Account account, EntryDirection direction, String amount) {
    return new LedgerEntry(account, direction, new BigDecimal(amount));
  }

  private static final class FakeAssetTypes implements AssetTypeRepository {
    private final Map<UUID, AssetType> values = new HashMap<>();

    @Override
    public Optional<AssetType> findById(UUID id) {
      return Optional.ofNullable(values.get(id));
    }

    @Override
    public void save(AssetType assetType) {
      if (values.putIfAbsent(assetType.id(), assetType) != null) {
        throw new IllegalArgumentException("Asset type already exists");
      }
    }
  }

  private static final class FakeAccounts implements AccountRepository {
    private final Map<UUID, Account> values = new HashMap<>();
    private int saveCount;

    @Override
    public Optional<Account> findById(UUID id) {
      return Optional.ofNullable(values.get(id));
    }

    @Override
    public void save(Account account) {
      if (values.putIfAbsent(account.id(), account) != null) {
        throw new IllegalArgumentException("Account already exists");
      }
      saveCount++;
    }
  }

  private static final class FakeTransactions implements LedgerTransactionRepository {
    private final Map<UUID, LedgerTransaction> values = new HashMap<>();

    @Override
    public void save(LedgerTransaction transaction) {
      if (values.putIfAbsent(transaction.id(), transaction) != null) {
        throw new IllegalArgumentException("Transaction already exists");
      }
    }

    @Override
    public List<LedgerEntry> findEntriesByAccountId(UUID accountId) {
      return new ArrayList<>(values.values().stream()
          .sorted(Comparator.comparing(LedgerTransaction::timestamp)
              .thenComparing(transaction -> transaction.id().toString()))
          .flatMap(transaction -> transaction.entries().stream())
          .filter(entry -> entry.account().id().equals(accountId))
          .toList());
    }
  }
}
