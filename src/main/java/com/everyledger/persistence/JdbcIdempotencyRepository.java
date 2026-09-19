package com.everyledger.persistence;

import com.everyledger.transaction.application.IdempotencyRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcIdempotencyRepository implements IdempotencyRepository {
  private final JdbcTemplate jdbc;
  public JdbcIdempotencyRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
  public boolean claim(String key,String hash) { return jdbc.query("insert into idempotency_keys(idempotency_key,request_hash,created_at) values (?,?,?) on conflict do nothing returning idempotency_key",(rs,n)->rs.getString(1),key,hash,Instant.now()).size()==1; }
  public Optional<Record> lock(String key) { return jdbc.query("select idempotency_key,request_hash,transaction_id from idempotency_keys where idempotency_key=? for update",(rs,n)->new Record(rs.getString(1),rs.getString(2),rs.getObject(3,UUID.class)),key).stream().findFirst(); }
  public void complete(String key,UUID transactionId) { jdbc.update("update idempotency_keys set transaction_id=? where idempotency_key=?",transactionId,key); }
}
