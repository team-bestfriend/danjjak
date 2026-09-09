package com.bestfriend.danjjak.chat.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public final class ChatDtos {
    private ChatDtos() {}

    public enum Action {
        TRANSFER, BALANCE_CHECK, PENSION_CHECK, MANAGEMENT_FEE_CHECK,
        UTILITY_BILL_CHECK, CUSTOMER_CENTER, PATTERN, APP_HELP, NONE
    }

    public record ChatRequest(@NotBlank @Size(max = 500) String message) {}
    public record ChatResponse(
            String message, Action action, Long patternId, boolean retryable, boolean showRecommendations) {}
}
