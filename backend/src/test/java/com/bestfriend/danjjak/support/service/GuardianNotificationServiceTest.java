package com.bestfriend.danjjak.support.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.support.mapper.SupportMapper;
import com.bestfriend.danjjak.support.model.NotificationAnomalyRecord;
import com.bestfriend.danjjak.support.service.KakaoMessageClient.KakaoSendResult;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GuardianNotificationServiceTest {

    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-08-31T01:02:03Z"), ZoneId.of("Asia/Seoul"));

    private SupportMapper supportMapper;
    private KakaoMessageClient kakaoMessageClient;
    private GuardianNotificationService service;

    @BeforeEach
    void setUp() {
        supportMapper = mock(SupportMapper.class);
        kakaoMessageClient = mock(KakaoMessageClient.class);
        service = new GuardianNotificationService(supportMapper, kakaoMessageClient, CLOCK);
        when(supportMapper.hasGuardianShareConsent(1L)).thenReturn(true);
        when(supportMapper.findHighAnomaly(1L, 40L)).thenReturn(highAnomaly());
    }

    @Test
    void rejectsNotificationWithoutGuardianShareConsent() {
        when(supportMapper.hasGuardianShareConsent(1L)).thenReturn(false);

        ApiException exception =
                assertThrows(ApiException.class, () -> service.notify(1L, 40L, "access-token"));

        assertEquals("GUARDIAN_SHARE_CONSENT_REQUIRED", exception.getCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        verify(supportMapper, never()).findHighAnomaly(anyLong(), anyLong());
        verify(kakaoMessageClient, never()).sendToMe(any(), any());
    }

    @Test
    void returnsMockResultWithoutCallingKakaoWhenSessionHasNoToken() {
        var response = service.notify(1L, 40L, null);

        assertEquals("MOCK", response.deliveryMode());
        assertEquals("MOCKED_NO_TOKEN", response.result());
        assertFalse(response.actualAttempted());
        assertNull(response.sentAt());
        verify(kakaoMessageClient, never()).sendToMe(any(), any());
        verify(supportMapper, never()).markGuardianNotified(anyLong(), anyLong(), any());
    }

    @Test
    void storesNotificationTimeOnlyAfterActualSuccess() {
        when(kakaoMessageClient.sendToMe("access-token", expectedMessage()))
                .thenReturn(new KakaoSendResult(true, 200, "KAKAO_SENT"));
        when(supportMapper.markGuardianNotified(anyLong(), anyLong(), any())).thenReturn(1);

        var response = service.notify(1L, 40L, "access-token");

        assertEquals("ACTUAL", response.deliveryMode());
        assertEquals("SENT", response.result());
        assertTrue(response.actualAttempted());
        assertTrue(response.actualSucceeded());
        assertEquals("2026-08-31T10:02:03", response.sentAt());
        verify(supportMapper)
                .markGuardianNotified(
                        1L, 40L, LocalDateTime.of(2026, 8, 31, 10, 2, 3));
    }

    @Test
    void returnsStoredActualSuccessWithoutSendingAgain() {
        NotificationAnomalyRecord anomaly = highAnomaly();
        anomaly.setGuardianNotifiedAt(LocalDateTime.of(2026, 8, 31, 10, 2, 3));
        when(supportMapper.findHighAnomaly(1L, 40L)).thenReturn(anomaly);

        var response = service.notify(1L, 40L, "access-token");

        assertEquals("SENT", response.result());
        assertEquals("2026-08-31T10:02:03", response.sentAt());
        verify(kakaoMessageClient, never()).sendToMe(any(), any());
        verify(supportMapper, never()).markGuardianNotified(anyLong(), anyLong(), any());
    }

    @Test
    void replacesKakaoFailureWithMockResultWithoutThrowing() {
        when(kakaoMessageClient.sendToMe("access-token", expectedMessage()))
                .thenReturn(new KakaoSendResult(false, 503, "KAKAO_API_REJECTED"));

        var response = service.notify(1L, 40L, "access-token");

        assertEquals("MOCK", response.deliveryMode());
        assertEquals("MOCKED_AFTER_ACTUAL_FAILURE", response.result());
        assertTrue(response.actualAttempted());
        assertFalse(response.actualSucceeded());
        assertEquals(
                "카카오 실제 발송에 실패해 Mock 알림으로 대체했습니다.",
                response.detail());
        assertNull(response.sentAt());
        verify(supportMapper, never()).markGuardianNotified(anyLong(), anyLong(), any());
    }

    @Test
    void rejectsAnomalyThatIsNotHigh() {
        when(supportMapper.findHighAnomaly(1L, 40L)).thenReturn(null);

        ApiException exception =
                assertThrows(ApiException.class, () -> service.notify(1L, 40L, null));

        assertEquals("HIGH_ANOMALY_NOT_FOUND", exception.getCode());
    }

    private NotificationAnomalyRecord highAnomaly() {
        NotificationAnomalyRecord record = new NotificationAnomalyRecord();
        record.setAnomalyEventId(40L);
        record.setRecipientName("김민수");
        record.setAmount(new BigDecimal("10000000"));
        record.setRiskLevel("HIGH");
        return record;
    }

    private String expectedMessage() {
        return "[단짝 이상거래 알림] 김민수님에게 10,000,000원 송금 시도가 감지되었습니다. 앱에서 내용을 확인해 주세요.";
    }
}
