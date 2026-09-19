package com.everyledger.transaction.application;

import com.everyledger.ledger.LedgerEntry;
import com.everyledger.transaction.LedgerTransaction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.transaction.annotation.Transactional;

public final class IdempotentPostingService {
  private final LedgerTransactionService transactions; private final LedgerTransactionRepository repository; private final IdempotencyRepository idempotency;
  public IdempotentPostingService(LedgerTransactionService t,LedgerTransactionRepository r,IdempotencyRepository i){transactions=t;repository=r;idempotency=i;}
  @Transactional public LedgerTransaction post(UUID asset,List<LedgerEntry> entries,String reference,Map<String,Object> metadata,String key){if(key==null||key.isBlank()||key.length()>255)throw new IllegalArgumentException("Idempotency-Key is required");String hash=hash(asset,entries,reference,metadata);if(!idempotency.claim(key,hash)){IdempotencyRepository.Record prior=idempotency.lock(key).orElseThrow();if(!prior.requestHash().equals(hash))throw new IdempotencyConflictException(key);return repository.findById(prior.transactionId()).orElseThrow();} LedgerTransaction tx=transactions.post(asset,entries,reference,metadata);idempotency.complete(key,tx.id());return tx;}
  private String hash(UUID asset,List<LedgerEntry> entries,String ref,Map<String,Object> metadata){try{String v=asset+"|"+ref+"|"+new TreeMap<>(metadata==null?Map.of():metadata)+"|"+entries.stream().map(e->e.account().id()+":"+e.direction()+":"+e.amount().stripTrailingZeros()).sorted().toList();return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalArgumentException("invalid request",e);}}
  public static final class IdempotencyConflictException extends RuntimeException { public IdempotencyConflictException(String k){super("idempotency key reused with a different request: "+k);} }
}
