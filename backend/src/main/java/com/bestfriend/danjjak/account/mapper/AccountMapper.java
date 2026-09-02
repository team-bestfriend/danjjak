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

    List<RegisteredPersonAccountRecord> findRegisteredPersons(long userId);

    RegisteredPersonAccountRecord findRegisteredPerson(
            @Param("userId") long userId,
            @Param("registeredPersonId") long registeredPersonId);

    int insertRegisteredPerson(RegisteredPersonCommand command);

    int insertRecipientAccount(RegisteredPersonCommand command);

    int updateRegisteredPerson(RegisteredPersonCommand command);

    int updateRecipientAccount(RegisteredPersonCommand command);

    List<TransactionRecord> findTransactions(
            @Param("userId") long userId,
            @Param("accountId") long accountId,
            @Param("category") String category);
}
