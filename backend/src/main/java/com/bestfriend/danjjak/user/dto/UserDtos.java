package com.bestfriend.danjjak.user.dto;

import javax.validation.constraints.NotNull;

public final class UserDtos {

    private UserDtos() {}

    public record CurrentUserResponse(
            long userId,
            String name,
            ConsentSettings consents,
            AccessibilitySettings settings) {}

    public record ConsentSettings(
            boolean completed, boolean usageLogAgreed, boolean guardianShareAgreed) {}

    public record ConsentUpdateRequest(
            @NotNull Boolean usageLogAgreed, @NotNull Boolean guardianShareAgreed) {}

    public record AccessibilitySettings(
            @NotNull FontSize fontSize,
            @NotNull VoiceSpeed voiceSpeed,
            @NotNull GuideVoiceType guideVoiceType) {}

    public enum FontSize {
        SMALL,
        NORMAL,
        LARGE
    }

    public enum VoiceSpeed {
        SLOW,
        NORMAL,
        FAST
    }

    public enum GuideVoiceType {
        TTS,
        FAMILY
    }
}
