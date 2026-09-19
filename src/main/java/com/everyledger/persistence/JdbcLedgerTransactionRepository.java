package com.everyledger.persistence;

import com.everyledger.account.application.AccountRepository;
import com.everyledger.ledger.*;
import com.everyledger.transaction.*;
import com.everyledger.transaction.application.LedgerTransactionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcLedgerTransactionRepository implements LedgerTransactionRepository {
  private final JdbcTemplate jdbc; private final AccountRepository accounts; private final ObjectMapper json;
  public JdbcLedgerTransactionRepository(JdbcTemplate jdbc,AccountRepository accounts,ObjectMapper json){this.jdbc=jdbc;this.accounts=accounts;this.json=json;}
  public void save(LedgerTransaction t){jdbc.update("insert into ledger_transactions(id,recorded_at,status,external_reference,metadata) values (?,?,?,?,cast(? as jsonb))",t.id(),t.timestamp(),t.status().name(),t.externalReference(),write(t.metadata())); for(int i=0;i<t.entries().size();i++){LedgerEntry e=t.entries().get(i);jdbc.update("insert into ledger_entries(transaction_id,position,account_id,direction,amount) values (?,?,?,?,?)",t.id(),i,e.account().id(),e.direction().name(),e.amount());}}
  public Optional<LedgerTransaction> findById(UUID id){return jdbc.query("select * from ledger_transactions where id=?",(rs,n)->map(rs.getObject("id",UUID.class),rs.getTimestamp("recorded_at").toInstant(),rs.getString("status"),rs.getString("external_reference"),rs.getString("metadata")),id).stream().findFirst();}
  public List<LedgerTransaction> findAll(){return jdbc.query("select * from ledger_transactions order by recorded_at,id",(rs,n)->map(rs.getObject("id",UUID.class),rs.getTimestamp("recorded_at").toInstant(),rs.getString("status"),rs.getString("external_reference"),rs.getString("metadata")));}
  public List<LedgerEntry> findEntriesByAccountId(UUID id){return jdbc.query("select e.* from ledger_entries e join ledger_transactions t on t.id=e.transaction_id where e.account_id=? order by t.recorded_at,t.id,e.position",(rs,n)->new LedgerEntry(accounts.findById(id).orElseThrow(),EntryDirection.valueOf(rs.getString("direction")),rs.getBigDecimal("amount")),id);}
  private LedgerTransaction map(UUID id,java.time.Instant ts,String status,String ref,String metadata){List<LedgerEntry> e=jdbc.query("select * from ledger_entries where transaction_id=? order by position",(rs,n)->{UUID account=rs.getObject("account_id",UUID.class);return new LedgerEntry(accounts.findById(account).orElseThrow(),EntryDirection.valueOf(rs.getString("direction")),rs.getBigDecimal("amount"));},id);return new LedgerTransaction(id,ts,TransactionStatus.valueOf(status),ref,read(metadata),e);}
  private String write(Map<String,Object> m){try{return json.writeValueAsString(m);}catch(Exception e){throw new IllegalArgumentException("metadata must be JSON serializable",e);}}
  private Map<String,Object> read(String s){try{return json.readValue(s,new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException(e);}}
}
