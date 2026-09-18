package com.everyledger.asset.application;

import java.util.UUID;

public final class AssetTypeNotFoundException extends IllegalArgumentException {

  public AssetTypeNotFoundException(UUID id) {
    super("Asset type not found: " + id);
  }
}
