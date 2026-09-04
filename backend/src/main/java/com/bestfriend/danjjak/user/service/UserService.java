package com.bestfriend.danjjak.user.service;

import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.user.dto.UserDtos.AccessibilitySettings;
import com.bestfriend.danjjak.user.dto.UserDtos.ConsentSettings;
import com.bestfriend.danjjak.user.dto.UserDtos.ConsentUpdateRequest;
import com.bestfriend.danjjak.user.dto.UserDtos.CurrentUserResponse;
import com.bestfriend.danjjak.user.dto.UserDtos.FontSize;
import com.bestfriend.danjjak.user.dto.UserDtos.GuideVoiceType;
import com.bestfriend.danjjak.user.dto.UserDtos.VoiceSpeed;
import com.bestfriend.danjjak.user.mapper.UserMapper;
import com.bestfriend.danjjak.user.model.UserSettingsRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public CurrentUserResponse getCurrentUser(long userId) {
        return toResponse(requireUser(userId));
    }

    public ConsentSettings updateConsents(long userId, ConsentUpdateRequest request) {
        boolean usageLogAgreed = Boolean.TRUE.equals(request.usageLogAgreed());
        boolean guardianShareAgreed = Boolean.TRUE.equals(request.guardianShareAgreed());
        int updated = userMapper.updateConsents(userId, usageLogAgreed, guardianShareAgreed);
        requireUpdated(updated);
        return new ConsentSettings(true, usageLogAgreed, guardianShareAgreed);
    }

    public AccessibilitySettings updateSettings(
            long userId, AccessibilitySettings settings) {
        int updated =
                userMapper.updateSettings(
                        userId,
                        settings.fontSize().name(),
                        settings.voiceSpeed().name(),
                        settings.guideVoiceType().name());
        requireUpdated(updated);
        return settings;
    }

    private UserSettingsRecord requireUser(long userId) {
        UserSettingsRecord user = userMapper.findCurrentUser(userId);
        if (user == null) {
            throw userNotFound();
        }
        return user;
    }

    private void requireUpdated(int updated) {
        if (updated == 0) {
            throw userNotFound();
        }
    }

    private CurrentUserResponse toResponse(UserSettingsRecord user) {
        return new CurrentUserResponse(
                user.getUserId(),
                user.getName(),
                new ConsentSettings(
                        user.isConsentCompleted(),
                        user.isUsageLogAgreed(),
                        user.isGuardianShareAgreed()),
                new AccessibilitySettings(
                        FontSize.valueOf(user.getFontSize()),
                        VoiceSpeed.valueOf(user.getVoiceSpeed()),
                        GuideVoiceType.valueOf(user.getGuideVoiceType())));
    }

    private ApiException userNotFound() {
        return new ApiException(
                HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자 정보를 찾을 수 없습니다.");
    }
}
