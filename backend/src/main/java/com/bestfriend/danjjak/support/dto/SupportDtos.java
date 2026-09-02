package com.bestfriend.danjjak.support.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

public final class SupportDtos {

    private SupportDtos() {}

    public record GuardianContactResponse(long guardianContactId, String phoneNumber) {}

    public record GuardianContactRequest(
            @NotBlank
                    @Pattern(
                            regexp = "^[0-9-]{8,20}$",
                            message = "전화번호는 숫자와 하이픈만 사용할 수 있습니다.")
                    String phoneNumber) {}

    public record SupportResponse(
            GuardianContactResponse guardian, String customerCenterPhone) {}

    public record NotificationResponse(
            long anomalyEventId,
            String deliveryMode,
            String result,
            boolean actualAttempted,
            boolean actualSucceeded,
            String detail) {}
}
