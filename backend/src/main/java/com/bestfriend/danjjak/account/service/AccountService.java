package com.bestfriend.danjjak.account.service;

import com.bestfriend.danjjak.account.dto.AccountDtos.BalanceResponse;
import com.bestfriend.danjjak.account.dto.AccountDtos.OwnedAccountResponse;
import com.bestfriend.danjjak.account.dto.AccountDtos.RecipientAccountRequest;
import com.bestfriend.danjjak.account.dto.AccountDtos.RecipientAccountResponse;
import com.bestfriend.danjjak.account.dto.AccountDtos.RegisteredPersonRequest;
import com.bestfriend.danjjak.account.dto.AccountDtos.RegisteredPersonResponse;
import com.bestfriend.danjjak.account.dto.AccountDtos.RegisteredPersonUpdateRequest;
import com.bestfriend.danjjak.account.dto.AccountDtos.TransactionResponse;
import com.bestfriend.danjjak.account.mapper.AccountMapper;
import com.bestfriend.danjjak.account.model.AccountRecord;
import com.bestfriend.danjjak.account.model.RegisteredPersonAccountRecord;
import com.bestfriend.danjjak.account.model.RegisteredPersonCommand;
import com.bestfriend.danjjak.common.error.ApiException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private static final Set<String> CATEGORIES =
            Set.of(
                    "GENERAL",
                    "TRANSFER",
                    "PENSION",
                    "MANAGEMENT_FEE",
                    "UTILITY_BILL",
                    "AUTO_TRANSFER",
                    "CARD");

    private final AccountMapper accountMapper;

    public AccountService(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    @Transactional(readOnly = true)
    public List<OwnedAccountResponse> getOwnedAccounts(long userId) {
        return accountMapper.findOwnedAccounts(userId).stream().map(this::toOwnedResponse).toList();
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(long userId, long accountId) {
        AccountRecord account = requireOwnedAccount(userId, accountId);
        return new BalanceResponse(account.getAccountId(), account.getBalance());
    }

    @Transactional(readOnly = true)
    public List<RegisteredPersonResponse> getRegisteredPersons(long userId) {
        return toRegisteredResponses(accountMapper.findRegisteredPersons(userId));
    }

    @Transactional
    public RegisteredPersonResponse createRegisteredPerson(
            long userId, RegisteredPersonRequest request) {
        RegisteredPersonCommand command = toCommand(userId, null, request);
        try {
            accountMapper.insertRegisteredPerson(command);
            accountMapper.insertRecipientAccount(command);
        } catch (DuplicateKeyException exception) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "ACCOUNT_ALREADY_EXISTS", "이미 등록된 계좌입니다.");
        }
        return requireRegisteredPerson(userId, command.getRegisteredPersonId());
    }

    @Transactional
    public RegisteredPersonResponse updateRegisteredPerson(
            long userId, long registeredPersonId, RegisteredPersonUpdateRequest request) {
        requireRegisteredPerson(userId, registeredPersonId);

        RegisteredPersonCommand command = new RegisteredPersonCommand();
        command.setUserId(userId);
        command.setRegisteredPersonId(registeredPersonId);
        command.setName(request.name().trim());
        command.setRelationship(request.relationship().trim());
        accountMapper.updateRegisteredPerson(command);
        return requireRegisteredPerson(userId, registeredPersonId);
    }

    @Transactional
    public RegisteredPersonResponse addRecipientAccount(
            long userId, long registeredPersonId, RecipientAccountRequest request) {
        requireRegisteredPerson(userId, registeredPersonId);
        RegisteredPersonCommand command = toAccountCommand(userId, registeredPersonId, null, request);
        rejectDuplicateRecipientAccount(command, null);
        try {
            accountMapper.insertRecipientAccount(command);
        } catch (DuplicateKeyException exception) {
            throw duplicateAccountException();
        }
        return requireRegisteredPerson(userId, registeredPersonId);
    }

    @Transactional
    public RegisteredPersonResponse updateRecipientAccount(
            long userId,
            long registeredPersonId,
            long accountId,
            RecipientAccountRequest request) {
        if (accountMapper.findRecipientAccount(userId, registeredPersonId, accountId) == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "RECIPIENT_ACCOUNT_NOT_FOUND", "받는 계좌를 찾을 수 없습니다.");
        }
        RegisteredPersonCommand command =
                toAccountCommand(userId, registeredPersonId, accountId, request);
        rejectDuplicateRecipientAccount(command, accountId);
        try {
            accountMapper.updateRecipientAccount(command);
        } catch (DuplicateKeyException exception) {
            throw duplicateAccountException();
        }
        return requireRegisteredPerson(userId, registeredPersonId);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(
            long userId, long accountId, String category) {
        requireOwnedAccount(userId, accountId);
        String normalizedCategory = normalizeCategory(category);
        return accountMapper.findTransactions(userId, accountId, normalizedCategory).stream()
                .map(
                        transaction ->
                                new TransactionResponse(
                                        transaction.getTransactionId(),
                                        transaction.getTransactionType(),
                                        transaction.getCategory(),
                                        transaction.getAmount(),
                                        transaction.getCounterpartyName(),
                                        transaction.getDescription(),
                                        transaction.getBalanceAfter(),
                                        transaction.getTransactionAt()))
                .toList();
    }

    private AccountRecord requireOwnedAccount(long userId, long accountId) {
        AccountRecord account = accountMapper.findOwnedAccount(userId, accountId);
        if (account == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "본인 계좌를 찾을 수 없습니다.");
        }
        return account;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        String normalized = category.trim().toUpperCase();
        if (!CATEGORIES.contains(normalized)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_CATEGORY", "지원하지 않는 거래 카테고리입니다.");
        }
        return normalized;
    }

    private OwnedAccountResponse toOwnedResponse(AccountRecord account) {
        return new OwnedAccountResponse(
                account.getAccountId(),
                account.getBankCode(),
                account.getBankName(),
                account.getAccountNumber(),
                account.getAccountAlias(),
                account.getBalance(),
                account.isPrimary());
    }

    private List<RegisteredPersonResponse> toRegisteredResponses(
            List<RegisteredPersonAccountRecord> records) {
        Map<Long, List<RegisteredPersonAccountRecord>> recordsByPerson = new LinkedHashMap<>();
        for (RegisteredPersonAccountRecord record : records) {
            recordsByPerson
                    .computeIfAbsent(record.getRegisteredPersonId(), ignored -> new ArrayList<>())
                    .add(record);
        }
        return recordsByPerson.values().stream().map(this::toRegisteredResponse).toList();
    }

    private RegisteredPersonResponse toRegisteredResponse(
            List<RegisteredPersonAccountRecord> records) {
        RegisteredPersonAccountRecord person = records.get(0);
        return new RegisteredPersonResponse(
                person.getRegisteredPersonId(),
                person.getName(),
                person.getRelationship(),
                records.stream()
                        .map(
                                record ->
                                        new RecipientAccountResponse(
                                                record.getAccountId(),
                                                record.getBankCode(),
                                                record.getBankName(),
                                                record.getAccountNumber(),
                                                record.getAccountAlias()))
                        .toList());
    }

    private RegisteredPersonCommand toCommand(
            long userId, Long registeredPersonId, RegisteredPersonRequest request) {
        RegisteredPersonCommand command = new RegisteredPersonCommand();
        command.setUserId(userId);
        command.setRegisteredPersonId(registeredPersonId);
        command.setName(request.name().trim());
        command.setRelationship(request.relationship().trim());
        command.setBankCode(request.bankCode().trim());
        command.setBankName(request.bankName().trim());
        command.setAccountNumber(request.accountNumber().trim());
        command.setAccountAlias(
                request.accountAlias() == null || request.accountAlias().isBlank()
                        ? null
                        : request.accountAlias().trim());
        return command;
    }

    private RegisteredPersonCommand toAccountCommand(
            long userId,
            long registeredPersonId,
            Long accountId,
            RecipientAccountRequest request) {
        RegisteredPersonCommand command = new RegisteredPersonCommand();
        command.setUserId(userId);
        command.setRegisteredPersonId(registeredPersonId);
        command.setAccountId(accountId);
        command.setBankCode(request.bankCode().trim());
        command.setBankName(request.bankName().trim());
        command.setAccountNumber(request.accountNumber().trim());
        command.setAccountAlias(
                request.accountAlias() == null || request.accountAlias().isBlank()
                        ? null
                        : request.accountAlias().trim());
        return command;
    }

    private RegisteredPersonResponse requireRegisteredPerson(long userId, long registeredPersonId) {
        List<RegisteredPersonAccountRecord> records =
                accountMapper.findRegisteredPerson(userId, registeredPersonId);
        if (records.isEmpty()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "REGISTERED_PERSON_NOT_FOUND", "등록 인물을 찾을 수 없습니다.");
        }
        return toRegisteredResponse(records);
    }

    private void rejectDuplicateRecipientAccount(
            RegisteredPersonCommand command, Long excludedAccountId) {
        String normalizedAccountNumber = command.getAccountNumber().replace("-", "");
        int duplicateCount =
                accountMapper.countDuplicateRecipientAccounts(
                        command.getUserId(),
                        command.getRegisteredPersonId(),
                        command.getBankCode(),
                        normalizedAccountNumber,
                        excludedAccountId);
        if (duplicateCount > 0) {
            throw duplicateAccountException();
        }
    }

    private ApiException duplicateAccountException() {
        return new ApiException(
                HttpStatus.CONFLICT, "ACCOUNT_ALREADY_EXISTS", "이미 등록된 계좌입니다.");
    }
}
