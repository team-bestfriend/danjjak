package com.bestfriend.danjjak.account.mapper;

import com.bestfriend.danjjak.account.model.AccountRecord;
import com.bestfriend.danjjak.account.model.RegisteredPersonAccountRecord;
import com.bestfriend.danjjak.account.model.RegisteredPersonCommand;
import com.bestfriend.danjjak.account.model.TransactionRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AccountMapper {

    List<AccountRecord> findOwnedAccounts(long userId);

    AccountRecord findOwnedAccount(
            @Param("userId") long userId, @Param("accountId") long accountId);

    List<AccountRecord> findMockAccountImportOptions(long userId);

    AccountRecord findOwnedAccountImportOption(
            @Param("userId") long userId, @Param("accountId") long accountId);

    int countImportedOwnedAccounts(long userId);

    int markOwnedAccountImported(
            @Param("userId") long userId,
            @Param("accountId") long accountId,
            @Param("makePrimary") boolean makePrimary);

    List<RegisteredPersonAccountRecord> findRegisteredPersons(long userId);

    List<RegisteredPersonAccountRecord> findRegisteredPerson(
            @Param("userId") long userId,
            @Param("registeredPersonId") long registeredPersonId);

    RegisteredPersonAccountRecord findRecipientAccount(
            @Param("userId") long userId,
            @Param("registeredPersonId") long registeredPersonId,
            @Param("accountId") long accountId);

    int countDuplicateRecipientAccounts(
            @Param("userId") long userId,
            @Param("registeredPersonId") long registeredPersonId,
            @Param("bankCode") String bankCode,
            @Param("normalizedAccountNumber") String normalizedAccountNumber,
            @Param("excludedAccountId") Long excludedAccountId);

    int insertRegisteredPerson(RegisteredPersonCommand command);

    int insertRecipientAccount(RegisteredPersonCommand command);

    int updateRegisteredPerson(RegisteredPersonCommand command);

    int updateRecipientAccount(RegisteredPersonCommand command);

    int deleteRecipientAccountsForPerson(
            @Param("userId") long userId,
            @Param("registeredPersonId") long registeredPersonId);

    int deleteRegisteredPerson(
            @Param("userId") long userId,
            @Param("registeredPersonId") long registeredPersonId);

    List<TransactionRecord> findTransactions(
            @Param("userId") long userId,
            @Param("accountId") long accountId,
            @Param("category") String category);
}
