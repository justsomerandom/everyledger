package com.everyledger.account.application;

import com.everyledger.account.Account;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository {

  Optional<Account> findById(UUID id);

  /** Inserts a new account; an existing identifier must not be overwritten. */
  void save(Account account);
}
