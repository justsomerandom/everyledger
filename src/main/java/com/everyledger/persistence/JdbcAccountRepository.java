package com.everyledger.persistence;

import com.everyledger.account.Account;
import com.everyledger.account.application.AccountRepository;
import com.everyledger.asset.AssetType;
import java.time.Instant;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcAccountRepository implements AccountRepository {
  private final JdbcTemplate jdbc;
  public JdbcAccountRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
  public Optional<Account> findById(UUID id) { return jdbc.query("select a.id,a.name,t.id asset_id,t.code,t.display_name from accounts a join asset_types t on t.id=a.asset_type_id where a.id=?", (rs,n) -> new Account(rs.getObject("id", UUID.class),rs.getString("name"),new AssetType(rs.getObject("asset_id",UUID.class),rs.getString("code"),rs.getString("display_name"))),id).stream().findFirst(); }
  public void save(Account account) { jdbc.update("insert into accounts(id,name,asset_type_id,created_at) values (?,?,?,?)",account.id(),account.name(),account.assetType().id(), Instant.now()); }
  public List<Account> findAll() { return jdbc.query("select a.id,a.name,t.id asset_id,t.code,t.display_name from accounts a join asset_types t on t.id=a.asset_type_id order by a.created_at,a.id",(rs,n)->new Account(rs.getObject("id",UUID.class),rs.getString("name"),new AssetType(rs.getObject("asset_id",UUID.class),rs.getString("code"),rs.getString("display_name")))); }
}
