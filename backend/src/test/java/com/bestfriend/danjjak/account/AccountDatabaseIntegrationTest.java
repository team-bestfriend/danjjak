package com.bestfriend.danjjak.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bestfriend.danjjak.account.service.AccountService;
import com.bestfriend.danjjak.config.RootConfig;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@EnabledIfEnvironmentVariable(named = "DANJJAK_DB_INTEGRATION_TEST", matches = "true")
@Transactional
class AccountDatabaseIntegrationTest {

    @Autowired private AccountService accountService;

    @Test
    void readsSeededAccountsPeopleBalanceAndCategoryTransactions() {
        var accounts = accountService.getOwnedAccounts(1L);
        var people = accountService.getRegisteredPersons(1L);

        assertEquals(2, accounts.size());
        assertTrue(accounts.get(0).primary());
        assertEquals(new BigDecimal("50000000"), accountService.getBalance(1L, 1L).balance());
        assertEquals(2, people.size());
        assertEquals(
                Set.of("김민수", "김지영"),
                people.stream().map(person -> person.name()).collect(Collectors.toSet()));

        var allTransactions = accountService.getTransactions(1L, 1L, null);
        assertEquals(3, allTransactions.size());
        assertFalse(accountService.getTransactions(1L, 1L, "PENSION").isEmpty());
        assertFalse(accountService.getTransactions(1L, 1L, "MANAGEMENT_FEE").isEmpty());
        assertFalse(accountService.getTransactions(1L, 1L, "UTILITY_BILL").isEmpty());
    }
}
