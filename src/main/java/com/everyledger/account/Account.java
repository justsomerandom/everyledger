package com.everyledger.account;

import com.everyledger.asset.AssetType;
import java.util.Objects;
import java.util.UUID;

public record Account(UUID id, String name, AssetType assetType) {

  public Account {
    Objects.requireNonNull(id, "Account id must not be null");
    requireNotBlank(name, "Account name must not be blank");
    Objects.requireNonNull(assetType, "Account asset type must not be null");
  }

  private static void requireNotBlank(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
  }
}
