package com.everyledger.asset.application;

import com.everyledger.asset.AssetType;
import java.util.Optional;
import java.util.UUID;

public interface AssetTypeRepository {

  Optional<AssetType> findById(UUID id);

  /** Inserts a new asset type; an existing identifier must not be overwritten. */
  void save(AssetType assetType);
}
