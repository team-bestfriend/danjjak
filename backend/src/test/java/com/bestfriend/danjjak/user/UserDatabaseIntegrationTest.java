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
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@EnabledIfEnvironmentVariable(named = "DANJJAK_DB_INTEGRATION_TEST", matches = "true")
@Transactional
class UserDatabaseIntegrationTest {

    @Autowired private UserService userService;
    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

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

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    void storesEveryOptionalConsentCombination(
            boolean usageLogAgreed, boolean guardianShareAgreed) {
        var saved =
                userService.updateConsents(
                        1L,
                        new ConsentUpdateRequest(
                                usageLogAgreed, guardianShareAgreed));
        var currentUser = userService.getCurrentUser(1L);

        assertTrue(saved.completed());
        assertEquals(usageLogAgreed, saved.usageLogAgreed());
        assertEquals(guardianShareAgreed, saved.guardianShareAgreed());
        assertEquals(saved, currentUser.consents());
    }

    @Test
    void initialKakaoBindingAndReloginUseSameUserAndSettings() {
        long kakaoUserId = 987654321L;
        jdbcTemplate.update("UPDATE users SET kakao_user_id = NULL WHERE user_id = ?", 1L);
        userService.updateConsents(1L, new ConsentUpdateRequest(true, false));
        userService.updateSettings(
                1L,
                new AccessibilitySettings(
                        FontSize.LARGE, VoiceSpeed.SLOW, GuideVoiceType.TTS));

        var initialLogin = userService.findOrBindKakaoUser(kakaoUserId);
        var relogin = userService.findOrBindKakaoUser(kakaoUserId);

        assertEquals(1L, initialLogin.userId());
        assertEquals(initialLogin, relogin);
        assertTrue(relogin.consents().usageLogAgreed());
        assertFalse(relogin.consents().guardianShareAgreed());
        assertEquals(FontSize.LARGE, relogin.settings().fontSize());
        assertEquals(VoiceSpeed.SLOW, relogin.settings().voiceSpeed());
        assertEquals(
                1,
                jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM users WHERE kakao_user_id = ?",
                        Integer.class,
                        kakaoUserId));
    }
}
