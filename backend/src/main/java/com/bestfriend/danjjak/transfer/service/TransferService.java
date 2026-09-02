package com.bestfriend.danjjak.transfer.service;

import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.DirectRecipientRequest;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.TransferRequest;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.TransferResponse;
import com.bestfriend.danjjak.transfer.mapper.TransferMapper;
import com.bestfriend.danjjak.transfer.model.RecipientRecord;
import com.bestfriend.danjjak.transfer.model.TransactionCommand;
import com.bestfriend.danjjak.transfer.model.TransferAccountRecord;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final TransferMapper transferMapper;
    private final PinVerifier pinVerifier;
    private final Clock clock;

    public TransferService(TransferMapper transferMapper, PinVerifier pinVerifier, Clock clock) {
        this.transferMapper = transferMapper;
        this.pinVerifier = pinVerifier;
        this.clock = clock;
    }

    @Transactional
    public TransferResponse transfer(long userId, TransferRequest request) {
        RecipientSnapshot recipient = resolveRecipient(userId, request);
        TransferAccountRecord source =
                transferMapper.findSourceAccountForUpdate(userId, request.sourceAccountId());
        if (source == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "SOURCE_ACCOUNT_NOT_FOUND", "출금 계좌를 찾을 수 없습니다.");
        }
        validatePatternExecution(userId, request.patternExecutionId(), source.getAccountId());
        if (!pinVerifier.matches(request.pin(), source.getPinHash())) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "PIN_MISMATCH", "출금 계좌 PIN이 일치하지 않습니다.");
        }
        validateBalance(source.getBalance(), request.amount());

        LocalDateTime now = LocalDateTime.now(clock);
        TransactionCommand transaction =
                completeTransfer(
                        userId,
                        source,
                        recipient,
                        request.amount(),
                        request.patternExecutionId(),
                        now);
        return new TransferResponse(
                "COMPLETED", transaction.getTransactionId(), transaction.getBalanceAfter());
    }

    private RecipientSnapshot resolveRecipient(long userId, TransferRequest request) {
        boolean registered = request.registeredRecipientAccountId() != null;
        boolean direct = request.directRecipient() != null;
        if (registered == direct) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_RECIPIENT",
                    "등록 수취 계좌와 직접 입력 계좌 중 하나만 선택해야 합니다.");
        }
        if (registered) {
            RecipientRecord record =
                    transferMapper.findRegisteredRecipient(
                            userId, request.registeredRecipientAccountId());
            if (record == null) {
                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        "RECIPIENT_ACCOUNT_NOT_FOUND",
                        "등록 수취 계좌를 찾을 수 없습니다.");
            }
            return new RecipientSnapshot(
                    record.getAccountId(),
                    record.getName(),
                    record.getBankCode(),
                    record.getBankName(),
                    record.getAccountNumber());
        }

        DirectRecipientRequest directRecipient = request.directRecipient();
        return new RecipientSnapshot(
                null,
                directRecipient.name().trim(),
                directRecipient.bankCode().trim(),
                directRecipient.bankName().trim(),
                directRecipient.accountNumber().trim());
    }

    private void validatePatternExecution(
            long userId, Long patternExecutionId, long sourceAccountId) {
        if (patternExecutionId == null) {
            return;
        }
        int count =
                transferMapper.countAvailablePatternExecution(
                        userId, patternExecutionId, sourceAccountId);
        if (count != 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_PATTERN_EXECUTION",
                    "사용 가능한 패턴 실행 기록이 아닙니다.");
        }
    }

    private void validateBalance(BigDecimal balance, BigDecimal amount) {
        if (balance.compareTo(amount) < 0) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE", "계좌 잔액이 부족합니다.");
        }
    }

    private TransactionCommand completeTransfer(
            long userId,
            TransferAccountRecord source,
            RecipientSnapshot recipient,
            BigDecimal amount,
            Long patternExecutionId,
            LocalDateTime now) {
        int updated = transferMapper.debitAccount(userId, source.getAccountId(), amount);
        if (updated != 1) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE", "계좌 잔액이 부족합니다.");
        }

        TransactionCommand command = new TransactionCommand();
        command.setUserId(userId);
        command.setSourceAccountId(source.getAccountId());
        command.setRecipientAccountId(recipient.accountId());
        command.setPatternExecutionId(patternExecutionId);
        command.setAmount(amount);
        command.setRecipientName(recipient.name());
        command.setRecipientBankCode(recipient.bankCode());
        command.setRecipientBankName(recipient.bankName());
        command.setRecipientAccountNumber(recipient.accountNumber());
        command.setDescription(recipient.name() + " 송금");
        command.setBalanceAfter(source.getBalance().subtract(amount));
        command.setTransactionAt(now);
        transferMapper.insertTransaction(command);
        if (patternExecutionId != null) {
            transferMapper.finishPatternExecution(patternExecutionId, now);
        }
        return command;
    }

    private record RecipientSnapshot(
            Long accountId,
            String name,
            String bankCode,
            String bankName,
            String accountNumber) {}
}
