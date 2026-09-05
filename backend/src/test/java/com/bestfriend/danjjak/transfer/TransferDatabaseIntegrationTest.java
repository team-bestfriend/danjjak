package com.bestfriend.danjjak.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.config.RootConfig;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.DirectRecipientRequest;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.ResolveAnomalyRequest;
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

        var response =
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
        assertEquals("박친구", transactionValue(response.transactionId(), "counterparty_name"));
        assertEquals("003", transactionValue(response.transactionId(), "counterparty_bank_code"));
        assertEquals("기업은행", transactionValue(response.transactionId(), "counterparty_bank_name"));
        assertEquals(
                "000-000-000003",
                transactionValue(response.transactionId(), "counterparty_account_number"));
    }

    @Test
    void wrongPinLeavesBalanceAndTransactionsUnchanged() {
        BigDecimal balanceBefore = balance();
        int transactionCountBefore = transactionCount();
        int anomalyCountBefore = anomalyCount();

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
        assertEquals(anomalyCountBefore, anomalyCount());
    }

    @Test
    void insufficientBalanceLeavesAllFinancialRecordsUnchanged() {
        BigDecimal balanceBefore = balance();
        int transactionCountBefore = transactionCount();
        int anomalyCountBefore = anomalyCount();

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                transferService.transfer(
                                        1L,
                                        new TransferRequest(
                                                1L,
                                                3L,
                                                null,
                                                new BigDecimal("50000001"),
                                                null,
                                                "1234")));

        assertEquals("INSUFFICIENT_BALANCE", exception.getCode());
        assertEquals(balanceBefore, balance());
        assertEquals(transactionCountBefore, transactionCount());
        assertEquals(anomalyCountBefore, anomalyCount());
    }

    @Test
    void mediumAnomalyCancellationCreatesNoTransaction() {
        int transactionCountBefore = transactionCount();
        int anomalyCountBefore = anomalyCount();
        var attempt =
                transferService.transfer(
                        1L,
                        new TransferRequest(
                                1L,
                                3L,
                                null,
                                new BigDecimal("10000000"),
                                null,
                                "1234"));

        assertEquals("MEDIUM", attempt.riskLevel());
        assertEquals("REQUIRES_REVIEW", attempt.status());
        assertEquals(transactionCountBefore, transactionCount());
        assertEquals(anomalyCountBefore + 1, anomalyCount());

        var repeatedAttempt =
                transferService.transfer(
                        1L,
                        new TransferRequest(
                                1L,
                                3L,
                                null,
                                new BigDecimal("10000000"),
                                null,
                                "1234"));
        assertEquals(attempt.anomalyEventId(), repeatedAttempt.anomalyEventId());
        assertEquals(anomalyCountBefore + 1, anomalyCount());

        var resolution =
                transferService.resolve(
                        1L,
                        attempt.anomalyEventId(),
                        new ResolveAnomalyRequest("CANCEL", true));

        assertEquals("CANCEL", resolution.action());
        assertEquals(transactionCountBefore, transactionCount());
        assertEquals("CANCEL", anomalyFinalAction(attempt.anomalyEventId()));

        var repeatedResolution =
                transferService.resolve(
                        1L,
                        attempt.anomalyEventId(),
                        new ResolveAnomalyRequest("CONTINUE", false));
        assertEquals("CANCEL", repeatedResolution.action());
        assertEquals(transactionCountBefore, transactionCount());
        assertEquals(anomalyCountBefore + 1, anomalyCount());
    }

    @Test
    void repeatedAndHighAmountAnomalyContinuesWithLinkedTransaction() {
        int anomalyCountBefore = anomalyCount();
        transferService.transfer(
                1L,
                new TransferRequest(
                        1L, 3L, null, new BigDecimal("1000"), null, "1234"));
        transferService.transfer(
                1L,
                new TransferRequest(
                        1L, 3L, null, new BigDecimal("1000"), null, "1234"));

        var attempt =
                transferService.transfer(
                        1L,
                        new TransferRequest(
                                1L,
                                3L,
                                null,
                                new BigDecimal("10000000"),
                                null,
                                "1234"));

        assertEquals("HIGH", attempt.riskLevel());
        assertEquals(2, attempt.recentTransferCount());
        assertEquals(anomalyCountBefore + 1, anomalyCount());
        var resolution =
                transferService.resolve(
                        1L,
                        attempt.anomalyEventId(),
                        new ResolveAnomalyRequest("CONTINUE", true));

        assertEquals("CONTINUE", resolution.action());
        assertEquals(resolution.transactionId(), anomalyTransactionId(attempt.anomalyEventId()));

        var repeatedResolution =
                transferService.resolve(
                        1L,
                        attempt.anomalyEventId(),
                        new ResolveAnomalyRequest("CANCEL", false));
        assertEquals("CONTINUE", repeatedResolution.action());
        assertEquals(resolution.transactionId(), repeatedResolution.transactionId());
        assertEquals(resolution.balanceAfter(), repeatedResolution.balanceAfter());
        assertEquals(anomalyCountBefore + 1, anomalyCount());
    }

    @Test
    void repeatedTransferAloneCreatesMediumAnomalyWithoutThirdTransaction() {
        int transactionCountBefore = transactionCount();
        int anomalyCountBefore = anomalyCount();
        transferService.transfer(
                1L,
                new TransferRequest(
                        1L, 3L, null, new BigDecimal("1000"), null, "1234"));
        transferService.transfer(
                1L,
                new TransferRequest(
                        1L, 3L, null, new BigDecimal("1000"), null, "1234"));

        var attempt =
                transferService.transfer(
                        1L,
                        new TransferRequest(
                                1L, 3L, null, new BigDecimal("1000"), null, "1234"));

        assertEquals("MEDIUM", attempt.riskLevel());
        assertEquals(2, attempt.recentTransferCount());
        assertTrue(attempt.reasons().contains("REPEATED_TRANSFER"));
        assertEquals(transactionCountBefore + 2, transactionCount());
        assertEquals(anomalyCountBefore + 1, anomalyCount());
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

    private int anomalyCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM anomaly_events WHERE user_id = 1", Integer.class);
    }

    private String anomalyFinalAction(long anomalyEventId) {
        return jdbcTemplate.queryForObject(
                "SELECT final_action FROM anomaly_events WHERE anomaly_event_id = ?",
                String.class,
                anomalyEventId);
    }

    private Long anomalyTransactionId(long anomalyEventId) {
        return jdbcTemplate.queryForObject(
                "SELECT transaction_id FROM anomaly_events WHERE anomaly_event_id = ?",
                Long.class,
                anomalyEventId);
    }

    private String transactionValue(long transactionId, String column) {
        return jdbcTemplate.queryForObject(
                "SELECT " + column + " FROM transactions WHERE transaction_id = ?",
                String.class,
                transactionId);
    }
}
