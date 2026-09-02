package com.bestfriend.danjjak.transfer.service;

import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.DirectRecipientRequest;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.ResolveAnomalyRequest;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.ResolveAnomalyResponse;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.TransferRequest;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.TransferResponse;
import com.bestfriend.danjjak.transfer.mapper.TransferMapper;
import com.bestfriend.danjjak.transfer.model.AnomalyCommand;
import com.bestfriend.danjjak.transfer.model.AnomalyRecord;
import com.bestfriend.danjjak.transfer.model.RecipientRecord;
import com.bestfriend.danjjak.transfer.model.TransactionCommand;
import com.bestfriend.danjjak.transfer.model.TransferAccountRecord;
import com.bestfriend.danjjak.transfer.service.FdsEvaluator.FdsResult;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final TransferMapper transferMapper;
    private final FdsEvaluator fdsEvaluator;
    private final PinVerifier pinVerifier;
    private final Clock clock;

    public TransferService(
            TransferMapper transferMapper,
            FdsEvaluator fdsEvaluator,
            PinVerifier pinVerifier,
            Clock clock) {
        this.transferMapper = transferMapper;
        this.fdsEvaluator = fdsEvaluator;
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

        LocalDateTime now = currentTime();
        int recentTransferCount =
                transferMapper.countRecentTransfers(userId, now.minusMinutes(10), now);
        FdsResult fdsResult = fdsEvaluator.evaluate(request.amount(), recentTransferCount);

        if (!pinVerifier.matches(request.pin(), source.getPinHash())) {
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "PIN_MISMATCH", "출금 계좌 PIN이 일치하지 않습니다.");
        }
        validateBalance(source.getBalance(), request.amount());

        if (fdsResult.anomalous()) {
            AnomalyCommand anomaly =
                    createAnomalyCommand(userId, request, recipient, fdsResult, now);
            transferMapper.insertAnomaly(anomaly);
            return new TransferResponse(
                    "REQUIRES_REVIEW",
                    fdsResult.riskLevel(),
                    fdsResult.reasons(),
                    recentTransferCount,
                    anomaly.getAnomalyEventId(),
                    null,
                    source.getBalance());
        }

        TransactionCommand transaction =
                completeTransfer(
                        userId,
                        source,
                        recipient,
                        request.amount(),
                        request.patternExecutionId(),
                        now);
        return new TransferResponse(
                "COMPLETED",
                fdsResult.riskLevel(),
                fdsResult.reasons(),
                recentTransferCount,
                null,
                transaction.getTransactionId(),
                transaction.getBalanceAfter());
    }

    @Transactional
    public ResolveAnomalyResponse resolve(
            long userId, long anomalyEventId, ResolveAnomalyRequest request) {
        AnomalyRecord anomaly = transferMapper.findAnomalyForUpdate(userId, anomalyEventId);
        if (anomaly == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "ANOMALY_NOT_FOUND", "이상거래 기록을 찾을 수 없습니다.");
        }
        if (anomaly.getFinalAction() != null) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "ANOMALY_ALREADY_RESOLVED", "이미 처리된 이상거래입니다.");
        }

        String action = request.action().trim().toUpperCase();
        LocalDateTime now = currentTime();
        if ("CANCEL".equals(action)) {
            transferMapper.resolveAnomalyAsCancelled(
                    userId, anomalyEventId, request.rechecked(), now);
            finishPatternExecution(anomaly.getPatternExecutionId(), "CANCELLED", now);
            return new ResolveAnomalyResponse(anomalyEventId, action, null, null);
        }
        if (!"CONTINUE".equals(action)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "INVALID_FINAL_ACTION", "최종 행동은 CONTINUE 또는 CANCEL이어야 합니다.");
        }

        TransferAccountRecord source =
                transferMapper.findSourceAccountForUpdate(userId, anomaly.getSourceAccountId());
        if (source == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND, "SOURCE_ACCOUNT_NOT_FOUND", "출금 계좌를 찾을 수 없습니다.");
        }
        validateBalance(source.getBalance(), anomaly.getAmount());

        RecipientSnapshot recipient =
                new RecipientSnapshot(
                        anomaly.getRecipientAccountId(),
                        anomaly.getRecipientName(),
                        anomaly.getRecipientBankCode(),
                        anomaly.getRecipientBankName(),
                        anomaly.getRecipientAccountNumber());
        TransactionCommand transaction =
                completeTransfer(
                        userId,
                        source,
                        recipient,
                        anomaly.getAmount(),
                        anomaly.getPatternExecutionId(),
                        now);
        transferMapper.resolveAnomalyAsContinued(
                userId,
                anomalyEventId,
                transaction.getTransactionId(),
                request.rechecked(),
                now);
        return new ResolveAnomalyResponse(
                anomalyEventId,
                action,
                transaction.getTransactionId(),
                transaction.getBalanceAfter());
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
        finishPatternExecution(patternExecutionId, "COMPLETED", now);
        return command;
    }

    private AnomalyCommand createAnomalyCommand(
            long userId,
            TransferRequest request,
            RecipientSnapshot recipient,
            FdsResult fdsResult,
            LocalDateTime now) {
        AnomalyCommand command = new AnomalyCommand();
        command.setUserId(userId);
        command.setPatternExecutionId(request.patternExecutionId());
        command.setSourceAccountId(request.sourceAccountId());
        command.setRecipientAccountId(recipient.accountId());
        command.setRecipientName(recipient.name());
        command.setRecipientBankCode(recipient.bankCode());
        command.setRecipientBankName(recipient.bankName());
        command.setRecipientAccountNumber(recipient.accountNumber());
        command.setAmount(request.amount());
        command.setHighAmountDetected(fdsResult.highAmountDetected());
        command.setRepeatedTransferDetected(fdsResult.repeatedTransferDetected());
        command.setRecentTransferCount(fdsResult.recentTransferCount());
        command.setRiskLevel(fdsResult.riskLevel());
        command.setDetectedAt(now);
        return command;
    }

    private void finishPatternExecution(
            Long patternExecutionId, String status, LocalDateTime endedAt) {
        if (patternExecutionId != null) {
            transferMapper.finishPatternExecution(patternExecutionId, status, endedAt);
        }
    }

    private LocalDateTime currentTime() {
        // 스키마의 DATETIME(0) 반올림으로 최근 송금이 조회 상한보다 미래가 되는 것을 막는다.
        return LocalDateTime.now(clock).withNano(0);
    }

    private record RecipientSnapshot(
            Long accountId,
            String name,
            String bankCode,
            String bankName,
            String accountNumber) {}
}
