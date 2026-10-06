package com.everyledger.account.application;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read model for an account's derived balance.
 *
 * <p>The balance is never an independent ledger fact: implementations must derive it from recorded
 * entries or maintain a transactionally updated projection of those entries.</p>
 */
@FunctionalInterface
public interface AccountBalanceRepository {

  BigDecimal findBalance(UUID accountId);
}
