package com.everyledger.asset;

import java.util.Objects;
import java.util.UUID;

public record AssetType(UUID id, String code, String displayName) {

  public AssetType {
    Objects.requireNonNull(id, "Asset type id must not be null");
    requireNotBlank(code, "Asset type code must not be blank");
    requireNotBlank(displayName, "Asset type display name must not be blank");
  }

  private static void requireNotBlank(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
  }
}
