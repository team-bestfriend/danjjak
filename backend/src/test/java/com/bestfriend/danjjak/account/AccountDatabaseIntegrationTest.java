package com.bestfriend.danjjak.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bestfriend.danjjak.account.dto.AccountDtos.RecipientAccountRequest;
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
        accountService.importMockAccount(1L, 1L);

        var accounts = accountService.getOwnedAccounts(1L);
        var people = accountService.getRegisteredPersons(1L);

        assertEquals(1, accounts.size());
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

    @Test
    void importsCandidateOnceWithoutResettingBalance() {
        var before = accountService.getMockAccountImportOptions(1L);
        assertEquals(0, accountService.getOwnedAccounts(1L).size());
        var candidate = before.stream()
                .filter(option -> !option.imported())
                .findFirst()
                .orElseThrow();

        var imported = accountService.importMockAccount(1L, candidate.accountId());
        var repeated = accountService.importMockAccount(1L, candidate.accountId());
        var after = accountService.getMockAccountImportOptions(1L);

        assertEquals(candidate.balance(), imported.balance());
        assertEquals(imported, repeated);
        assertEquals(1, accountService.getOwnedAccounts(1L).size());
        assertTrue(imported.primary());
        assertTrue(
                after.stream()
                        .filter(option -> option.accountId() == candidate.accountId())
                        .findFirst()
                        .orElseThrow()
                        .imported());
    }

    @Test
    void addsUpdatesAndReloadsSecondRecipientAccount() {
        var person = accountService.getRegisteredPersons(1L).stream()
                .filter(item -> item.name().equals("김민수"))
                .findFirst()
                .orElseThrow();
        int previousCount = person.accounts().size();

        var added =
                accountService.addRecipientAccount(
                        1L,
                        person.registeredPersonId(),
                        new RecipientAccountRequest(
                                "088", "신한은행", "999-88-777777", "추가 계좌"));
        long addedAccountId = added.accounts().get(added.accounts().size() - 1).accountId();

        accountService.updateRecipientAccount(
                1L,
                person.registeredPersonId(),
                addedAccountId,
                new RecipientAccountRequest(
                        "088", "신한은행", "999-88-777777", "수정된 계좌"));
        var reloaded = accountService.getRegisteredPersons(1L).stream()
                .filter(item -> item.registeredPersonId() == person.registeredPersonId())
                .findFirst()
                .orElseThrow();

        assertEquals(previousCount + 1, reloaded.accounts().size());
        assertEquals(
                "수정된 계좌",
                reloaded.accounts().stream()
                        .filter(account -> account.accountId() == addedAccountId)
                        .findFirst()
                        .orElseThrow()
                        .accountAlias());
    }
}
