package com.everyledger.persistence;

import com.everyledger.asset.AssetType;
import com.everyledger.asset.application.AssetTypeRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcAssetTypeRepository implements AssetTypeRepository {
  private final JdbcTemplate jdbc;
  public JdbcAssetTypeRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public Optional<AssetType> findById(UUID id) { return jdbc.query("select id,code,display_name from asset_types where id=?", (rs, n) -> new AssetType(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3)), id).stream().findFirst(); }
  public void save(AssetType asset) { jdbc.update("insert into asset_types(id,code,display_name,created_at) values (?,?,?,?)", asset.id(), asset.code(), asset.displayName(), Instant.now()); }
  public List<AssetType> findAll() { return jdbc.query("select id,code,display_name from asset_types order by code",(rs,n)->new AssetType(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3))); }
}
