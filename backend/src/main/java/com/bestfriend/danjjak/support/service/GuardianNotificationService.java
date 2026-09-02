package com.bestfriend.danjjak.support.service;

import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.support.dto.SupportDtos.NotificationResponse;
import com.bestfriend.danjjak.support.mapper.SupportMapper;
import com.bestfriend.danjjak.support.model.NotificationAnomalyRecord;
import com.bestfriend.danjjak.support.service.KakaoMessageClient.KakaoSendResult;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuardianNotificationService {

    private final SupportMapper supportMapper;
    private final KakaoMessageClient kakaoMessageClient;
    private final Clock clock;

    public GuardianNotificationService(
            SupportMapper supportMapper, KakaoMessageClient kakaoMessageClient, Clock clock) {
        this.supportMapper = supportMapper;
        this.kakaoMessageClient = kakaoMessageClient;
        this.clock = clock;
    }

    @Transactional
    public NotificationResponse notify(long userId, long anomalyEventId, String accessToken) {
        NotificationAnomalyRecord anomaly =
                supportMapper.findHighAnomaly(userId, anomalyEventId);
        if (anomaly == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "HIGH_ANOMALY_NOT_FOUND",
                    "알림 가능한 HIGH 이상거래를 찾을 수 없습니다.");
        }
        if (anomaly.getFinalAction() != null) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "ANOMALY_ALREADY_RESOLVED", "이미 처리된 이상거래입니다.");
        }

        if (accessToken == null || accessToken.isBlank()) {
            return new NotificationResponse(
                    anomalyEventId,
                    "MOCK",
                    "MOCKED_NO_TOKEN",
                    false,
                    false,
                    "카카오 토큰이 없어 Mock 알림으로 대체했습니다.");
        }

        KakaoSendResult result =
                kakaoMessageClient.sendToMe(accessToken, createMessage(anomaly));
        if (result.success()) {
            supportMapper.markGuardianNotified(
                    userId, anomalyEventId, LocalDateTime.now(clock).withNano(0));
            return new NotificationResponse(
                    anomalyEventId,
                    "ACTUAL",
                    "SENT",
                    true,
                    true,
                    "카카오 나에게 보내기 알림을 전송했습니다.");
        }
        return new NotificationResponse(
                anomalyEventId,
                "MOCK",
                "MOCKED_AFTER_ACTUAL_FAILURE",
                true,
                false,
                result.detail());
    }

    private String createMessage(NotificationAnomalyRecord anomaly) {
        String amount =
                NumberFormat.getNumberInstance(Locale.KOREA).format(anomaly.getAmount()) + "원";
        return "[단짝 이상거래 알림] "
                + anomaly.getRecipientName()
                + "님에게 "
                + amount
                + " 송금 시도가 감지되었습니다. 앱에서 내용을 확인해 주세요.";
    }
}
