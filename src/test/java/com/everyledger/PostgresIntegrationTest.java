package com.everyledger;

import static org.assertj.core.api.Assertions.assertThat;
import com.everyledger.account.application.*;
import com.everyledger.asset.application.*;
import com.everyledger.ledger.*;
import com.everyledger.transaction.application.*;
import java.math.BigDecimal; import java.util.*;
import org.junit.jupiter.api.Test; import org.springframework.beans.factory.annotation.Autowired; import org.springframework.boot.test.context.SpringBootTest; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.test.context.DynamicPropertyRegistry; import org.springframework.test.context.DynamicPropertySource; import org.testcontainers.containers.PostgreSQLContainer; import org.testcontainers.junit.jupiter.*;

@Testcontainers(disabledWithoutDocker = true) @SpringBootTest
class PostgresIntegrationTest {
  @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");
  @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);}
  @Autowired AssetTypeService assets; @Autowired AccountService accounts; @Autowired IdempotentPostingService posting; @Autowired JdbcTemplate jdbc;
  @Test void migratesPostsAtomicallyAndIsIdempotent(){var asset=assets.register("PTS"+UUID.randomUUID().toString().substring(0,6),"Points");var a=accounts.create("A",asset.id());var b=accounts.create("B",asset.id());var entries=List.of(new LedgerEntry(a,EntryDirection.DEBIT,BigDecimal.TEN),new LedgerEntry(b,EntryDirection.CREDIT,BigDecimal.TEN));var first=posting.post(asset.id(),entries,"ref",Map.of(),"key-"+UUID.randomUUID());var again=posting.post(asset.id(),entries,"ref",Map.of(),jdbc.queryForObject("select idempotency_key from idempotency_keys where transaction_id=?",String.class,first.id()));assertThat(again.id()).isEqualTo(first.id());assertThat(accounts.balance(a.id())).isEqualByComparingTo("10");}
}
