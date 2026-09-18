package com.everyledger.asset.application;

import com.everyledger.asset.AssetType;
import java.util.Objects;
import java.util.UUID;

public final class AssetTypeService {

  private final AssetTypeRepository assetTypes;

  public AssetTypeService(AssetTypeRepository assetTypes) {
    this.assetTypes = Objects.requireNonNull(assetTypes);
  }

  public AssetType register(String code, String displayName) {
    AssetType assetType = new AssetType(UUID.randomUUID(), code, displayName);
    assetTypes.save(assetType);
    return assetType;
  }
}
