package com.bestfriend.danjjak.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bestfriend.danjjak.config.RootConfig;
import com.bestfriend.danjjak.support.dto.SupportDtos.GuardianContactRequest;
import com.bestfriend.danjjak.support.mapper.SupportMapper;
import com.bestfriend.danjjak.support.service.GuardianNotificationService;
import com.bestfriend.danjjak.support.service.KakaoMessageClient.KakaoSendResult;
import com.bestfriend.danjjak.support.service.SupportService;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.TransferRequest;
import com.bestfriend.danjjak.transfer.service.TransferService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
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
class SupportDatabaseIntegrationTest {

    @Autowired private SupportService supportService;
    @Autowired private GuardianNotificationService notificationService;
    @Autowired private SupportMapper supportMapper;
    @Autowired private TransferService transferService;
    @Autowired private Clock clock;
    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void readsAndUpdatesGuardianContactFromSeedData() {
        var support = supportService.getSupport(1L);

        assertEquals("010-0000-1004", support.guardian().phoneNumber());
        assertEquals("1588-0000", support.customerCenterPhone());

        var updated =
                supportService.updateGuardian(
                        1L, new GuardianContactRequest("010-1111-2222"));

        assertEquals("010-1111-2222", updated.phoneNumber());
        assertEquals(1, guardianCount());
    }

    @Test
    void distinguishesNoTokenFailureAndActualSuccessWithoutPersistingFailures() {
        long anomalyEventId = createHighAnomaly();

        var noToken = notificationService.notify(1L, anomalyEventId, null);
        assertEquals("MOCKED_NO_TOKEN", noToken.result());
        assertNull(guardianNotifiedAt(anomalyEventId));

        GuardianNotificationService failingService =
                new GuardianNotificationService(
                        supportMapper,
                        (accessToken, message) ->
                                new KakaoSendResult(false, 503, "KAKAO_API_REJECTED"),
                        clock);
        var failed = failingService.notify(1L, anomalyEventId, "failure-token");
        assertEquals("MOCKED_AFTER_ACTUAL_FAILURE", failed.result());
        assertNull(guardianNotifiedAt(anomalyEventId));

        AtomicReference<String> sentMessage = new AtomicReference<>();
        GuardianNotificationService succeedingService =
                new GuardianNotificationService(
                        supportMapper,
                        (accessToken, message) -> {
                            sentMessage.set(message);
                            return new KakaoSendResult(true, 200, "KAKAO_SENT");
                        },
                        clock);
        var sent = succeedingService.notify(1L, anomalyEventId, "success-token");

        assertEquals("SENT", sent.result());
        assertTrue(sentMessage.get().contains("10,000,000원"));
        assertNotNull(guardianNotifiedAt(anomalyEventId));
    }

    private long createHighAnomaly() {
        transferService.transfer(
                1L,
                new TransferRequest(
                        1L, 3L, null, new BigDecimal("1000"), null, "1234"));
        transferService.transfer(
                1L,
                new TransferRequest(
                        1L, 3L, null, new BigDecimal("1000"), null, "1234"));
        return transferService
                .transfer(
                        1L,
                        new TransferRequest(
                                1L,
                                3L,
                                null,
                                new BigDecimal("10000000"),
                                null,
                                "1234"))
                .anomalyEventId();
    }

    private int guardianCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM guardian_contacts WHERE user_id = 1", Integer.class);
    }

    private LocalDateTime guardianNotifiedAt(long anomalyEventId) {
        return jdbcTemplate.queryForObject(
                "SELECT guardian_notified_at FROM anomaly_events WHERE anomaly_event_id = ?",
                LocalDateTime.class,
                anomalyEventId);
    }
}
