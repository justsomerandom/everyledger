package com.everyledger.transaction;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.everyledger.account.Account;
import com.everyledger.asset.AssetType;
import com.everyledger.ledger.EntryDirection;
import com.everyledger.ledger.LedgerEntry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LedgerTransactionTest {

  private static final Instant TIMESTAMP = Instant.parse("2026-09-18T12:00:00Z");

  @Test
  void acceptsValidBalancedTwoEntryTransaction() {
    AssetType assetType = assetType("USD");
    Account cash = account("Cash", assetType);
    Account revenue = account("Revenue", assetType);

    LedgerTransaction transaction =
        transaction(
            List.of(
                entry(cash, EntryDirection.DEBIT, "10.00"),
                entry(revenue, EntryDirection.CREDIT, "10.00")));

    assertEquals(2, transaction.entries().size());
  }

  @Test
  void acceptsBalancedTransactionWithMoreThanTwoEntries() {
    AssetType assetType = assetType("PTS");
    Account source = account("Source", assetType);
    Account targetA = account("Target A", assetType);
    Account targetB = account("Target B", assetType);

    LedgerTransaction transaction =
        transaction(
            List.of(
                entry(targetA, EntryDirection.DEBIT, "4"),
                entry(targetB, EntryDirection.DEBIT, "6"),
                entry(source, EntryDirection.CREDIT, "10")));

    assertEquals(3, transaction.entries().size());
  }

  @Test
  void acceptsDebitAndCreditAmountsWithDifferentScalesWhenNumericallyEqual() {
    AssetType assetType = assetType("CRD");
    Account debit = account("Debit", assetType);
    Account credit = account("Credit", assetType);

    assertDoesNotThrow(
        () ->
            transaction(
                List.of(
                    entry(debit, EntryDirection.DEBIT, "10.0"),
                    entry(credit, EntryDirection.CREDIT, "10.00"))));
  }

  @Test
  void rejectsZeroAmounts() {
    AssetType assetType = assetType("INV");
    Account account = account("Inventory", assetType);

    assertThrows(
        IllegalArgumentException.class,
        () -> entry(account, EntryDirection.DEBIT, "0"));
  }

  @Test
  void rejectsNegativeAmounts() {
    AssetType assetType = assetType("INV");
    Account account = account("Inventory", assetType);

    assertThrows(
        IllegalArgumentException.class,
        () -> entry(account, EntryDirection.DEBIT, "-1"));
  }

  @Test
  void rejectsFewerThanTwoEntries() {
    AssetType assetType = assetType("USD");
    Account account = account("Cash", assetType);

    assertThrows(
        IllegalArgumentException.class,
        () -> transaction(List.of(entry(account, EntryDirection.DEBIT, "10"))));
  }

  @Test
  void rejectsUnbalancedTransaction() {
    AssetType assetType = assetType("USD");
    Account debit = account("Debit", assetType);
    Account credit = account("Credit", assetType);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            transaction(
                List.of(
                    entry(debit, EntryDirection.DEBIT, "10"),
                    entry(credit, EntryDirection.CREDIT, "9"))));
  }

  @Test
  void rejectsEntriesUsingDifferentAssetTypes() {
    Account usdAccount = account("USD Account", assetType("USD"));
    Account pointsAccount = account("Points Account", assetType("PTS"));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            transaction(
                List.of(
                    entry(usdAccount, EntryDirection.DEBIT, "10"),
                    entry(pointsAccount, EntryDirection.CREDIT, "10"))));
  }

  @Test
  void transactionEntriesAreImmutableFromOutsideTheAggregate() {
    AssetType assetType = assetType("USD");
    Account debit = account("Debit", assetType);
    Account credit = account("Credit", assetType);
    List<LedgerEntry> entries = new ArrayList<>();
    entries.add(entry(debit, EntryDirection.DEBIT, "10"));
    entries.add(entry(credit, EntryDirection.CREDIT, "10"));

    LedgerTransaction transaction = transaction(entries);
    entries.add(entry(credit, EntryDirection.CREDIT, "1"));

    assertEquals(2, transaction.entries().size());
    assertThrows(
        UnsupportedOperationException.class,
        () -> transaction.entries().add(entry(debit, EntryDirection.DEBIT, "1")));
  }

  @Test
  void rejectsBlankRequiredNamesAndCodes() {
    AssetType assetType = assetType("USD");

    assertThrows(
        IllegalArgumentException.class,
        () -> new AssetType(UUID.randomUUID(), " ", "US Dollars"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new AssetType(UUID.randomUUID(), "USD", "\t"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new Account(UUID.randomUUID(), "\n", assetType));
  }

  private static AssetType assetType(String code) {
    return new AssetType(UUID.randomUUID(), code, code + " asset");
  }

  private static Account account(String name, AssetType assetType) {
    return new Account(UUID.randomUUID(), name, assetType);
  }

  private static LedgerEntry entry(Account account, EntryDirection direction, String amount) {
    return new LedgerEntry(account, direction, new BigDecimal(amount));
  }

  private static LedgerTransaction transaction(List<LedgerEntry> entries) {
    return new LedgerTransaction(UUID.randomUUID(), TIMESTAMP, TransactionStatus.RECORDED, entries);
  }
}
