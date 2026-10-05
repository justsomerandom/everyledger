package com.everyledger.config;

import com.everyledger.account.application.*;
import com.everyledger.asset.application.*;
import com.everyledger.persistence.*;
import com.everyledger.transaction.application.*;
import java.time.Clock;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration public class LedgerConfiguration {
  @Bean Clock ledgerClock(){return Clock.systemUTC();}
  @Bean AssetTypeRepository assetTypes(JdbcTemplate j){return new JdbcAssetTypeRepository(j);}
  @Bean AccountRepository accounts(JdbcTemplate j){return new JdbcAccountRepository(j);}
  @Bean LedgerTransactionRepository ledgerTransactions(JdbcTemplate j,AccountRepository a,ObjectMapper o){return new JdbcLedgerTransactionRepository(j,a,o);}
  @Bean IdempotencyRepository idempotency(JdbcTemplate j){return new JdbcIdempotencyRepository(j);}
  @Bean AssetTypeService assetTypeService(AssetTypeRepository r){return new AssetTypeService(r);}
  @Bean AccountService accountService(AccountRepository a,AssetTypeRepository t,LedgerTransactionRepository r){return new AccountService(a,t,r);}
  @Bean LedgerTransactionService transactionService(AssetTypeRepository a,AccountRepository c,LedgerTransactionRepository r,Clock k){return new LedgerTransactionService(a,c,r,k);}
  @Bean IdempotencyRequestFingerprint idempotencyRequestFingerprint(ObjectMapper objectMapper) { return new IdempotencyRequestFingerprint(objectMapper); }
  @Bean IdempotentPostingService postingService(LedgerTransactionService s,LedgerTransactionRepository r,IdempotencyRepository i,IdempotencyRequestFingerprint f){return new IdempotentPostingService(s,r,i,f);}
}
