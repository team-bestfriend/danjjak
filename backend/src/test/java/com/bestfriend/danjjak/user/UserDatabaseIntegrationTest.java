package com.bestfriend.danjjak.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bestfriend.danjjak.config.RootConfig;
import com.bestfriend.danjjak.user.dto.UserDtos.AccessibilitySettings;
import com.bestfriend.danjjak.user.dto.UserDtos.ConsentUpdateRequest;
import com.bestfriend.danjjak.user.dto.UserDtos.FontSize;
import com.bestfriend.danjjak.user.dto.UserDtos.GuideVoiceType;
import com.bestfriend.danjjak.user.dto.UserDtos.VoiceSpeed;
import com.bestfriend.danjjak.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@EnabledIfEnvironmentVariable(named = "DANJJAK_DB_INTEGRATION_TEST", matches = "true")
@Transactional
class UserDatabaseIntegrationTest {

    @Autowired private UserService userService;

    @Test
    void storesCurrentConsentAndAccessibilitySettings() {
        var consents =
                userService.updateConsents(1L, new ConsentUpdateRequest(false, false));
        var settings =
                userService.updateSettings(
                        1L,
                        new AccessibilitySettings(
                                FontSize.SMALL, VoiceSpeed.FAST, GuideVoiceType.FAMILY));
        var currentUser = userService.getCurrentUser(1L);

        assertTrue(consents.completed());
        assertFalse(consents.usageLogAgreed());
        assertFalse(consents.guardianShareAgreed());
        assertEquals(FontSize.SMALL, settings.fontSize());
        assertTrue(currentUser.consents().completed());
        assertEquals(VoiceSpeed.FAST, currentUser.settings().voiceSpeed());
        assertEquals(GuideVoiceType.FAMILY, currentUser.settings().guideVoiceType());
    }
}
