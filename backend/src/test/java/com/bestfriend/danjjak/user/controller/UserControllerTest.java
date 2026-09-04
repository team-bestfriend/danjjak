package com.bestfriend.danjjak.user.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bestfriend.danjjak.common.error.GlobalExceptionHandler;
import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
import com.bestfriend.danjjak.user.dto.UserDtos.AccessibilitySettings;
import com.bestfriend.danjjak.user.dto.UserDtos.ConsentSettings;
import com.bestfriend.danjjak.user.dto.UserDtos.CurrentUserResponse;
import com.bestfriend.danjjak.user.dto.UserDtos.FontSize;
import com.bestfriend.danjjak.user.dto.UserDtos.GuideVoiceType;
import com.bestfriend.danjjak.user.dto.UserDtos.VoiceSpeed;
import com.bestfriend.danjjak.user.service.UserService;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

class UserControllerTest {

    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        DemoSessionUserResolver userResolver = mock(DemoSessionUserResolver.class);
        when(userResolver.resolveUserId(any(HttpSession.class))).thenReturn(1L);
        mockMvc =
                MockMvcBuilders.standaloneSetup(new UserController(userService, userResolver))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .addFilters(
                                new CharacterEncodingFilter(
                                        StandardCharsets.UTF_8.name(), true))
                        .build();
    }

    @Test
    void returnsCurrentUserWithConsentsAndSettings() throws Exception {
        when(userService.getCurrentUser(1L))
                .thenReturn(
                        new CurrentUserResponse(
                                1L,
                                "김단짝",
                                new ConsentSettings(false, false, false),
                                new AccessibilitySettings(
                                        FontSize.LARGE, VoiceSpeed.SLOW, GuideVoiceType.TTS)));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"name\":\"김단짝\"")))
                .andExpect(content().string(containsString("\"completed\":false")))
                .andExpect(content().string(containsString("\"fontSize\":\"LARGE\"")));
    }

    @Test
    void completesConsentStepWhenBothChoicesAreRejected() throws Exception {
        when(userService.updateConsents(eq(1L), any()))
                .thenReturn(new ConsentSettings(true, false, false));

        mockMvc.perform(
                        put("/api/users/me/consents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"usageLogAgreed\":false,"
                                                + "\"guardianShareAgreed\":false}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"completed\":true")))
                .andExpect(content().string(containsString("\"usageLogAgreed\":false")));
    }

    @Test
    void updatesAccessibilitySettings() throws Exception {
        AccessibilitySettings settings =
                new AccessibilitySettings(
                        FontSize.NORMAL, VoiceSpeed.FAST, GuideVoiceType.FAMILY);
        when(userService.updateSettings(eq(1L), any())).thenReturn(settings);

        mockMvc.perform(
                        put("/api/users/me/settings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"fontSize\":\"NORMAL\",\"voiceSpeed\":\"FAST\","
                                                + "\"guideVoiceType\":\"FAMILY\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"voiceSpeed\":\"FAST\"")))
                .andExpect(content().string(containsString("\"guideVoiceType\":\"FAMILY\"")));
    }

    @Test
    void rejectsMissingConsentChoice() throws Exception {
        mockMvc.perform(
                        put("/api/users/me/consents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"usageLogAgreed\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("\"code\":\"INVALID_REQUEST\"")));
    }
}
