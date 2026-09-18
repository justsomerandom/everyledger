package com.everyledger.account.application;

import java.util.UUID;

public final class AccountNotFoundException extends IllegalArgumentException {

  public AccountNotFoundException(UUID id) {
    super("Account not found: " + id);
  }
}
