package com.bestfriend.danjjak.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.user.dto.UserDtos.AccessibilitySettings;
import com.bestfriend.danjjak.user.dto.UserDtos.ConsentUpdateRequest;
import com.bestfriend.danjjak.user.dto.UserDtos.FontSize;
import com.bestfriend.danjjak.user.dto.UserDtos.GuideVoiceType;
import com.bestfriend.danjjak.user.dto.UserDtos.VoiceSpeed;
import com.bestfriend.danjjak.user.mapper.UserMapper;
import com.bestfriend.danjjak.user.model.UserSettingsRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserServiceTest {

    private UserMapper userMapper;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        userService = new UserService(userMapper);
    }

    @Test
    void returnsCurrentUserWithMappedSettings() {
        when(userMapper.findCurrentUser(1L)).thenReturn(userRecord());

        var result = userService.getCurrentUser(1L);

        assertEquals(1L, result.userId());
        assertEquals("김단짝", result.name());
        assertFalse(result.consents().completed());
        assertEquals(FontSize.LARGE, result.settings().fontSize());
        assertEquals(VoiceSpeed.SLOW, result.settings().voiceSpeed());
    }

    @Test
    void storesRejectedChoicesAsCompletedConsent() {
        when(userMapper.updateConsents(1L, false, false)).thenReturn(1);

        var result =
                userService.updateConsents(1L, new ConsentUpdateRequest(false, false));

        assertTrue(result.completed());
        assertFalse(result.usageLogAgreed());
        assertFalse(result.guardianShareAgreed());
        verify(userMapper).updateConsents(1L, false, false);
    }

    @Test
    void storesAccessibilitySettings() {
        AccessibilitySettings settings =
                new AccessibilitySettings(
                        FontSize.SMALL, VoiceSpeed.FAST, GuideVoiceType.FAMILY);
        when(userMapper.updateSettings(1L, "SMALL", "FAST", "FAMILY")).thenReturn(1);

        var result = userService.updateSettings(1L, settings);

        assertEquals(settings, result);
        verify(userMapper).updateSettings(1L, "SMALL", "FAST", "FAMILY");
    }

    @Test
    void rejectsMissingUser() {
        when(userMapper.findCurrentUser(99L)).thenReturn(null);

        ApiException exception =
                assertThrows(ApiException.class, () -> userService.getCurrentUser(99L));

        assertEquals("USER_NOT_FOUND", exception.getCode());
    }

    @Test
    void returnsAlreadyBoundKakaoUserWithoutMovingAssociation() {
        UserSettingsRecord existing = userRecord();
        existing.setKakaoUserId(987654321L);
        when(userMapper.findByKakaoUserId(987654321L)).thenReturn(existing);

        var result = userService.findOrBindKakaoUser(987654321L);

        assertEquals(1L, result.userId());
        verify(userMapper).findByKakaoUserId(987654321L);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void bindsFirstUnlinkedSeedUserOnInitialKakaoLogin() {
        UserSettingsRecord candidate = userRecord();
        UserSettingsRecord bound = userRecord();
        bound.setKakaoUserId(987654321L);
        when(userMapper.findByKakaoUserId(987654321L)).thenReturn(null);
        when(userMapper.findFirstUnlinkedUser()).thenReturn(candidate);
        when(userMapper.bindKakaoUserId(1L, 987654321L)).thenReturn(1);
        when(userMapper.findCurrentUser(1L)).thenReturn(bound);

        var result = userService.findOrBindKakaoUser(987654321L);

        assertEquals(1L, result.userId());
        verify(userMapper).bindKakaoUserId(1L, 987654321L);
    }

    @Test
    void rejectsInitialKakaoLoginWhenNoSeedUserIsAvailable() {
        when(userMapper.findByKakaoUserId(987654321L)).thenReturn(null);
        when(userMapper.findFirstUnlinkedUser()).thenReturn(null);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () -> userService.findOrBindKakaoUser(987654321L));

        assertEquals("DEMO_USER_CAPACITY_EXCEEDED", exception.getCode());
    }

    private UserSettingsRecord userRecord() {
        UserSettingsRecord record = new UserSettingsRecord();
        record.setUserId(1L);
        record.setName("김단짝");
        record.setUsageLogAgreed(false);
        record.setGuardianShareAgreed(false);
        record.setConsentCompleted(false);
        record.setFontSize("LARGE");
        record.setVoiceSpeed("SLOW");
        record.setGuideVoiceType("TTS");
        return record;
    }
}
