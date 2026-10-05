package com.everyledger.transaction.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.everyledger.account.Account;
import com.everyledger.asset.AssetType;
import com.everyledger.ledger.EntryDirection;
import com.everyledger.ledger.LedgerEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdempotencyRequestFingerprintTest {

  private final IdempotencyRequestFingerprint fingerprint =
      new IdempotencyRequestFingerprint(new ObjectMapper());
  private final UUID assetTypeId = UUID.randomUUID();
  private final Account debit = account("Debit");
  private final Account credit = account("Credit");

  @Test
  void treatsMetadataObjectKeyOrderAndNumericScaleAsEquivalent() {
    Map<String, Object> firstMetadata = new LinkedHashMap<>();
    firstMetadata.put("source", "checkout");
    firstMetadata.put("details", Map.of("count", new BigDecimal("1.0"), "attempt", 2));
    Map<String, Object> secondMetadata = new LinkedHashMap<>();
    secondMetadata.put("details", Map.of("attempt", new BigDecimal("2.00"), "count", 1));
    secondMetadata.put("source", "checkout");

    String first = fingerprint.fingerprint(
        assetTypeId, entries("10.0"), "order-42", firstMetadata);
    String second = fingerprint.fingerprint(
        assetTypeId, entries("10.00"), "order-42", secondMetadata);

    assertEquals(first, second);
  }

  @Test
  void preservesEntryAndArrayOrderBecauseTheyAreRecordedLedgerFacts() {
    String original = fingerprint.fingerprint(
        assetTypeId, entries("10"), "order-42", Map.of("tags", List.of("first", "second")));
    String reorderedEntries = fingerprint.fingerprint(
        assetTypeId, List.of(entries("10").get(1), entries("10").get(0)), "order-42",
        Map.of("tags", List.of("first", "second")));
    String reorderedArray = fingerprint.fingerprint(
        assetTypeId, entries("10"), "order-42", Map.of("tags", List.of("second", "first")));

    assertNotEquals(original, reorderedEntries);
    assertNotEquals(original, reorderedArray);
  }

  private List<LedgerEntry> entries(String amount) {
    return List.of(
        new LedgerEntry(debit, EntryDirection.DEBIT, new BigDecimal(amount)),
        new LedgerEntry(credit, EntryDirection.CREDIT, new BigDecimal(amount)));
  }

  private Account account(String name) {
    return new Account(UUID.randomUUID(), name,
        new AssetType(assetTypeId, "PTS", "Points"));
  }
}
