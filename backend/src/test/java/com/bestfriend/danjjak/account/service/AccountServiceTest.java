package com.bestfriend.danjjak.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import java.time.LocalDateTime;
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
    void returnsImportedAndCandidateMockAccountOptions() {
        AccountRecord imported = ownedAccount(1L, true, LocalDateTime.now());
        AccountRecord candidate = ownedAccount(2L, false, null);
        when(accountMapper.findMockAccountImportOptions(1L))
                .thenReturn(List.of(imported, candidate));

        var result = accountService.getMockAccountImportOptions(1L);

        assertTrue(result.get(0).imported());
        assertFalse(result.get(1).imported());
    }

    @Test
    void importsFirstMockAccountAsPrimaryWithoutResettingItsData() {
        AccountRecord candidate = ownedAccount(2L, false, null);
        AccountRecord imported = ownedAccount(2L, true, LocalDateTime.now());
        when(accountMapper.findOwnedAccountImportOption(1L, 2L)).thenReturn(candidate);
        when(accountMapper.countImportedOwnedAccounts(1L)).thenReturn(0);
        when(accountMapper.findOwnedAccount(1L, 2L)).thenReturn(imported);

        var result = accountService.importMockAccount(1L, 2L);

        assertEquals(new BigDecimal("50000000"), result.balance());
        assertTrue(result.primary());
        verify(accountMapper).markOwnedAccountImported(1L, 2L, true);
    }

    @Test
    void repeatedImportReturnsExistingAccountWithoutUpdatingIt() {
        AccountRecord imported = ownedAccount(2L, false, LocalDateTime.now());
        when(accountMapper.findOwnedAccountImportOption(1L, 2L)).thenReturn(imported);
        when(accountMapper.findOwnedAccount(1L, 2L)).thenReturn(imported);

        accountService.importMockAccount(1L, 2L);

        verify(accountMapper, never())
                .markOwnedAccountImported(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void rejectsMockAccountOptionOwnedByAnotherUser() {
        when(accountMapper.findOwnedAccountImportOption(1L, 99L)).thenReturn(null);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> accountService.importMockAccount(1L, 99L));

        assertEquals("ACCOUNT_IMPORT_OPTION_NOT_FOUND", exception.getCode());
    }

    @Test
    void createsRegisteredPersonWithoutRecipientAccount() {
        RegisteredPersonRequest request =
                new RegisteredPersonRequest("김민수", "아들", "adult_man");
        RegisteredPersonAccountRecord saved = registeredPersonRecord();
        saved.setAccountId(null);
        saved.setBankCode(null);
        saved.setBankName(null);
        saved.setAccountNumber(null);
        saved.setAccountAlias(null);
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
        assertEquals("adult_man", result.profileImageKey());
        assertEquals(0, result.accounts().size());
        verify(accountMapper).insertRegisteredPerson(org.mockito.ArgumentMatchers.any());
        verify(accountMapper, never()).insertRecipientAccount(org.mockito.ArgumentMatchers.any());
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
                1L, 10L, new RegisteredPersonUpdateRequest("김민준", "보호자", "adult_woman"));

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
    void deletesRegisteredPersonAndRecipientAccounts() {
        RegisteredPersonAccountRecord current = registeredPersonRecord();
        when(accountMapper.findRegisteredPerson(1L, 10L)).thenReturn(List.of(current));

        accountService.deleteRegisteredPerson(1L, 10L);

        verify(accountMapper).deleteRecipientAccountsForPerson(1L, 10L);
        verify(accountMapper).deleteRegisteredPerson(1L, 10L);
    }

    @Test
    void rejectsDeletingRegisteredPersonOwnedByAnotherUser() {
        when(accountMapper.findRegisteredPerson(1L, 99L)).thenReturn(List.of());

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> accountService.deleteRegisteredPerson(1L, 99L));

        assertEquals("REGISTERED_PERSON_NOT_FOUND", exception.getCode());
        verify(accountMapper, never()).deleteRegisteredPerson(1L, 99L);
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
        record.setProfileImageKey("adult_man");
        record.setAccountId(20L);
        record.setBankCode("020");
        record.setBankName("우리은행");
        record.setAccountNumber("1002-000-000001");
        record.setAccountAlias("민수 계좌");
        return record;
    }

    private AccountRecord ownedAccount(
            long accountId, boolean primary, LocalDateTime importedAt) {
        AccountRecord account = new AccountRecord();
        account.setAccountId(accountId);
        account.setBankCode("088");
        account.setBankName("신한은행");
        account.setAccountNumber("110-000-000001");
        account.setAccountAlias("생활비 통장");
        account.setBalance(new BigDecimal("50000000"));
        account.setPrimary(primary);
        account.setImportedAt(importedAt);
        return account;
    }
}
