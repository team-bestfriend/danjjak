package com.bestfriend.danjjak.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bestfriend.danjjak.account.dto.AccountDtos.RecipientAccountRequest;
import com.bestfriend.danjjak.account.dto.AccountDtos.RegisteredPersonRequest;
import com.bestfriend.danjjak.account.dto.AccountDtos.RegisteredPersonUpdateRequest;
import com.bestfriend.danjjak.account.mapper.AccountMapper;
import com.bestfriend.danjjak.account.model.AccountRecord;
import com.bestfriend.danjjak.account.model.RegisteredPersonAccountRecord;
import com.bestfriend.danjjak.account.model.RegisteredPersonCommand;
import com.bestfriend.danjjak.common.error.ApiException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountServiceTest {

    private AccountMapper accountMapper;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountMapper = mock(AccountMapper.class);
        accountService = new AccountService(accountMapper);
    }

    @Test
    void returnsOwnedAccountsWithPrimaryFlagAndBalance() {
        AccountRecord account = new AccountRecord();
        account.setAccountId(1L);
        account.setBankCode("088");
        account.setBankName("신한은행");
        account.setAccountNumber("110-000-000001");
        account.setAccountAlias("생활비 통장");
        account.setBalance(new BigDecimal("50000000"));
        account.setPrimary(true);
        when(accountMapper.findOwnedAccounts(1L)).thenReturn(List.of(account));

        var result = accountService.getOwnedAccounts(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).accountId());
        assertEquals(new BigDecimal("50000000"), result.get(0).balance());
        assertEquals(true, result.get(0).primary());
    }

    @Test
    void createsRegisteredPersonAndRecipientAccountTogether() {
        RegisteredPersonRequest request =
                new RegisteredPersonRequest(
                        "김민수", "아들", "020", "우리은행", "1002-000-000001", "민수 계좌");
        RegisteredPersonAccountRecord saved = registeredPersonRecord();
        doAnswer(
                        invocation -> {
                            RegisteredPersonCommand command = invocation.getArgument(0);
                            command.setRegisteredPersonId(10L);
                            return 1;
                        })
                .when(accountMapper)
                .insertRegisteredPerson(org.mockito.ArgumentMatchers.any());
        when(accountMapper.findRegisteredPerson(1L, 10L)).thenReturn(List.of(saved));

        var result = accountService.createRegisteredPerson(1L, request);

        assertEquals("김민수", result.name());
        assertEquals("우리은행", result.accounts().get(0).bankName());
        verify(accountMapper).insertRegisteredPerson(org.mockito.ArgumentMatchers.any());
        verify(accountMapper).insertRecipientAccount(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void groupsMultipleAccountsUnderOneRegisteredPerson() {
        RegisteredPersonAccountRecord first = registeredPersonRecord();
        RegisteredPersonAccountRecord second = registeredPersonRecord();
        second.setAccountId(21L);
        second.setBankCode("088");
        second.setBankName("신한은행");
        second.setAccountNumber("110-222-333333");
        when(accountMapper.findRegisteredPersons(1L)).thenReturn(List.of(first, second));

        var result = accountService.getRegisteredPersons(1L);

        assertEquals(1, result.size());
        assertEquals(2, result.get(0).accounts().size());
        assertEquals(21L, result.get(0).accounts().get(1).accountId());
    }

    @Test
    void updatesOnlyRegisteredPersonInformation() {
        RegisteredPersonAccountRecord current = registeredPersonRecord();
        when(accountMapper.findRegisteredPerson(1L, 10L)).thenReturn(List.of(current));

        accountService.updateRegisteredPerson(
                1L, 10L, new RegisteredPersonUpdateRequest("김민준", "보호자"));

        verify(accountMapper).updateRegisteredPerson(org.mockito.ArgumentMatchers.any());
        verify(accountMapper, never()).updateRecipientAccount(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void addsSecondAccountToExistingPerson() {
        RegisteredPersonAccountRecord first = registeredPersonRecord();
        RegisteredPersonAccountRecord second = registeredPersonRecord();
        second.setAccountId(21L);
        second.setBankCode("088");
        second.setBankName("신한은행");
        second.setAccountNumber("110-222-333333");
        when(accountMapper.findRegisteredPerson(1L, 10L))
                .thenReturn(List.of(first), List.of(first, second));
        when(accountMapper.countDuplicateRecipientAccounts(
                        1L, 10L, "088", "110222333333", null))
                .thenReturn(0);

        var result =
                accountService.addRecipientAccount(
                        1L,
                        10L,
                        new RecipientAccountRequest(
                                "088", "신한은행", "110-222-333333", "생활비"));

        assertEquals(2, result.accounts().size());
        verify(accountMapper).insertRecipientAccount(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsSameAccountNumberWithDifferentHyphenFormat() {
        RegisteredPersonAccountRecord current = registeredPersonRecord();
        when(accountMapper.findRecipientAccount(1L, 10L, 21L)).thenReturn(current);
        when(accountMapper.countDuplicateRecipientAccounts(
                        1L, 10L, "020", "1002000000001", 21L))
                .thenReturn(1);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                accountService.updateRecipientAccount(
                                        1L,
                                        10L,
                                        21L,
                                        new RecipientAccountRequest(
                                                "020", "우리은행", "1002-000-000001", null)));

        assertEquals("ACCOUNT_ALREADY_EXISTS", exception.getCode());
        verify(accountMapper, never()).updateRecipientAccount(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void hidesRecipientAccountOwnedByAnotherPerson() {
        when(accountMapper.findRecipientAccount(1L, 10L, 99L)).thenReturn(null);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                accountService.updateRecipientAccount(
                                        1L,
                                        10L,
                                        99L,
                                        new RecipientAccountRequest(
                                                "020", "우리은행", "1002-000-000001", null)));

        assertEquals("RECIPIENT_ACCOUNT_NOT_FOUND", exception.getCode());
    }

    @Test
    void rejectsUnsupportedTransactionCategory() {
        AccountRecord account = new AccountRecord();
        account.setAccountId(1L);
        account.setBalance(BigDecimal.TEN);
        when(accountMapper.findOwnedAccount(1L, 1L)).thenReturn(account);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> accountService.getTransactions(1L, 1L, "UNKNOWN"));

        assertEquals("INVALID_CATEGORY", exception.getCode());
    }

    @Test
    void hidesAccountsOwnedByAnotherUser() {
        when(accountMapper.findOwnedAccount(1L, 99L)).thenReturn(null);

        ApiException exception =
                assertThrows(ApiException.class, () -> accountService.getBalance(1L, 99L));

        assertEquals("ACCOUNT_NOT_FOUND", exception.getCode());
    }

    private RegisteredPersonAccountRecord registeredPersonRecord() {
        RegisteredPersonAccountRecord record = new RegisteredPersonAccountRecord();
        record.setRegisteredPersonId(10L);
        record.setName("김민수");
        record.setRelationship("아들");
        record.setAccountId(20L);
        record.setBankCode("020");
        record.setBankName("우리은행");
        record.setAccountNumber("1002-000-000001");
        record.setAccountAlias("민수 계좌");
        return record;
    }
}
