package com.everyledger.transaction.application;

import com.everyledger.ledger.LedgerEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Produces a stable fingerprint for the complete client-visible posting request. */
public final class IdempotencyRequestFingerprint {

  private final ObjectMapper json;

  public IdempotencyRequestFingerprint(ObjectMapper json) {
    this.json = json;
  }

  public String fingerprint(
      UUID assetTypeId,
      List<LedgerEntry> entries,
      String externalReference,
      Map<String, Object> metadata) {
    ObjectNode request = JsonNodeFactory.instance.objectNode();
    request.put("assetTypeId", assetTypeId.toString());
    if (externalReference == null) {
      request.putNull("externalReference");
    } else {
      request.put("externalReference", externalReference);
    }
    request.set("metadata", canonicalize(json.valueToTree(metadata == null ? Map.of() : metadata)));

    ArrayNode requestedEntries = request.putArray("entries");
    for (LedgerEntry entry : entries) {
      ObjectNode requestedEntry = requestedEntries.addObject();
      requestedEntry.put("accountId", entry.account().id().toString());
      requestedEntry.put("direction", entry.direction().name());
      requestedEntry.put("amount", normalize(entry.amount()).toPlainString());
    }

    try {
      return HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256")
              .digest(json.writeValueAsBytes(request)));
    } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
      throw new IllegalArgumentException("request must be JSON serializable", exception);
    }
  }

  private JsonNode canonicalize(JsonNode node) {
    if (node.isObject()) {
      ObjectNode sorted = JsonNodeFactory.instance.objectNode();
      List<Map.Entry<String, JsonNode>> fields = new ArrayList<>();
      Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
      iterator.forEachRemaining(fields::add);
      fields.stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(field -> sorted.set(field.getKey(), canonicalize(field.getValue())));
      return sorted;
    }
    if (node.isArray()) {
      ArrayNode array = JsonNodeFactory.instance.arrayNode();
      node.forEach(value -> array.add(canonicalize(value)));
      return array;
    }
    if (node.isNumber()) {
      return DecimalNode.valueOf(normalize(node.decimalValue()));
    }
    return node;
  }

  private BigDecimal normalize(BigDecimal value) {
    BigDecimal normalized = value.stripTrailingZeros();
    return normalized.signum() == 0 ? BigDecimal.ZERO : normalized;
  }
}
