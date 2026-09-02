package com.bestfriend.danjjak.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.config.RootConfig;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.DirectRecipientRequest;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.TransferRequest;
import com.bestfriend.danjjak.transfer.service.TransferService;
import java.math.BigDecimal;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@EnabledIfEnvironmentVariable(named = "DANJJAK_DB_INTEGRATION_TEST", matches = "true")
@Transactional
class TransferDatabaseIntegrationTest {

    @Autowired private TransferService transferService;
    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void updatesBalanceAndCreatesRegisteredRecipientTransactionAtomically() {
        BigDecimal balanceBefore = balance();
        int transactionCountBefore = transactionCount();

        var response =
                transferService.transfer(
                        1L,
                        new TransferRequest(
                                1L, 3L, null, new BigDecimal("100000"), null, "1234"));

        assertEquals(balanceBefore.subtract(new BigDecimal("100000")), balance());
        assertEquals(transactionCountBefore + 1, transactionCount());
        assertEquals("COMPLETED", response.status());
    }

    @Test
    void directTransferDoesNotRegisterRecipientAccount() {
        int recipientAccountCountBefore = recipientAccountCount();

        transferService.transfer(
                1L,
                new TransferRequest(
                        1L,
                        null,
                        new DirectRecipientRequest(
                                "박친구", "003", "기업은행", "000-000-000003"),
                        new BigDecimal("50000"),
                        null,
                        "1234"));

        assertEquals(recipientAccountCountBefore, recipientAccountCount());
    }

    @Test
    void wrongPinLeavesBalanceAndTransactionsUnchanged() {
        BigDecimal balanceBefore = balance();
        int transactionCountBefore = transactionCount();

        assertThrows(
                ApiException.class,
                () ->
                        transferService.transfer(
                                1L,
                                new TransferRequest(
                                        1L,
                                        3L,
                                        null,
                                        new BigDecimal("100000"),
                                        null,
                                        "0000")));

        assertEquals(balanceBefore, balance());
        assertEquals(transactionCountBefore, transactionCount());
    }

    private BigDecimal balance() {
        return jdbcTemplate.queryForObject(
                "SELECT balance FROM bank_accounts WHERE bank_account_id = 1",
                BigDecimal.class);
    }

    private int transactionCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM transactions WHERE user_id = 1", Integer.class);
    }

    private int recipientAccountCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM bank_accounts WHERE user_id = 1 "
                        + "AND registered_person_id IS NOT NULL",
                Integer.class);
    }
}
