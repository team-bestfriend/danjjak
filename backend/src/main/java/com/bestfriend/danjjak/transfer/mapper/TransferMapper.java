package com.bestfriend.danjjak.transfer.mapper;

import com.bestfriend.danjjak.transfer.model.RecipientRecord;
import com.bestfriend.danjjak.transfer.model.TransactionCommand;
import com.bestfriend.danjjak.transfer.model.TransferAccountRecord;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TransferMapper {

    TransferAccountRecord findSourceAccountForUpdate(
            @Param("userId") long userId, @Param("accountId") long accountId);

    RecipientRecord findRegisteredRecipient(
            @Param("userId") long userId, @Param("accountId") long accountId);

    int countAvailablePatternExecution(
            @Param("userId") long userId,
            @Param("patternExecutionId") long patternExecutionId,
            @Param("sourceAccountId") long sourceAccountId);

    int debitAccount(
            @Param("userId") long userId,
            @Param("accountId") long accountId,
            @Param("amount") BigDecimal amount);

    int insertTransaction(TransactionCommand command);

    int finishPatternExecution(
            @Param("patternExecutionId") long patternExecutionId,
            @Param("endedAt") LocalDateTime endedAt);
}
