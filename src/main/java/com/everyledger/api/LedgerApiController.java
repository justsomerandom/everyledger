package com.everyledger.api;

import com.everyledger.account.*; import com.everyledger.account.application.*; import com.everyledger.asset.*; import com.everyledger.asset.application.*; import com.everyledger.ledger.*; import com.everyledger.transaction.*; import com.everyledger.transaction.application.*;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.util.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1") public class LedgerApiController {
  private final AssetTypeService assets; private final AssetTypeRepository assetRepo; private final AccountService accountService; private final AccountRepository accountRepo; private final IdempotentPostingService posting; private final LedgerTransactionRepository tx;
  public LedgerApiController(AssetTypeService a,AssetTypeRepository ar,AccountService as,AccountRepository ac,IdempotentPostingService p,LedgerTransactionRepository t){assets=a;assetRepo=ar;accountService=as;accountRepo=ac;posting=p;tx=t;}
  @PostMapping("/asset-types") @ResponseStatus(HttpStatus.CREATED) public AssetType createAsset(@Valid @RequestBody AssetRequest r){return assets.register(r.code(),r.displayName());}
  @GetMapping("/asset-types") public List<AssetType> assets(){return assetRepo.findAll();}
  @PostMapping("/accounts") @ResponseStatus(HttpStatus.CREATED) public Account createAccount(@Valid @RequestBody AccountRequest r){return accountService.create(r.name(),r.assetTypeId());}
  @GetMapping("/accounts") public List<Account> accounts(){return accountRepo.findAll();}
  @PostMapping("/transactions") @ResponseStatus(HttpStatus.CREATED) public LedgerTransaction post(@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody TransactionRequest r){List<LedgerEntry> entries=r.entries().stream().map(e->new LedgerEntry(accountRepo.findById(e.accountId()).orElseThrow(()->new AccountNotFoundException(e.accountId())),e.direction(),e.amount())).toList();return posting.post(r.assetTypeId(),entries,r.externalReference(),r.metadata(),key);}
  @GetMapping("/transactions/{id}") public LedgerTransaction transaction(@PathVariable UUID id){return tx.findById(id).orElseThrow(()->new NoSuchElementException("transaction not found: "+id));}
  @GetMapping("/transactions") public List<LedgerTransaction> transactions(){return tx.findAll();}
  @GetMapping("/accounts/{id}/balance") public Map<String,Object> balance(@PathVariable UUID id){return Map.of("accountId",id,"balance",accountService.balance(id));}
  @GetMapping("/accounts/{id}/entries") public List<LedgerEntry> entries(@PathVariable UUID id){return accountService.history(id);}
  record AssetRequest(@NotBlank String code,@NotBlank String displayName){} record AccountRequest(@NotBlank String name,@NotNull UUID assetTypeId){} record TransactionRequest(@NotNull UUID assetTypeId,String externalReference,Map<String,Object> metadata,@NotEmpty @Valid List<EntryRequest> entries){} record EntryRequest(@NotNull UUID accountId,@NotNull EntryDirection direction,@NotNull @DecimalMin(value="0",inclusive=false) BigDecimal amount){}
}
