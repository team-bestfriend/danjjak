package com.bestfriend.danjjak.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bestfriend.danjjak.auth.service.KakaoOAuthService;
import com.bestfriend.danjjak.auth.service.KakaoOAuthService.KakaoLoginResult;
import com.bestfriend.danjjak.common.error.GlobalExceptionHandler;
import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
import com.bestfriend.danjjak.user.dto.UserDtos.AccessibilitySettings;
import com.bestfriend.danjjak.user.dto.UserDtos.ConsentSettings;
import com.bestfriend.danjjak.user.dto.UserDtos.CurrentUserResponse;
import com.bestfriend.danjjak.user.dto.UserDtos.FontSize;
import com.bestfriend.danjjak.user.dto.UserDtos.GuideVoiceType;
import com.bestfriend.danjjak.user.dto.UserDtos.VoiceSpeed;
import com.bestfriend.danjjak.user.service.UserService;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

class AuthControllerTest {

    private KakaoOAuthService kakaoOAuthService;
    private UserService userService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        kakaoOAuthService = mock(KakaoOAuthService.class);
        userService = mock(UserService.class);
        MockEnvironment environment =
                new MockEnvironment().withProperty("app.frontend-url", "http://localhost:5173");
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new AuthController(kakaoOAuthService, userService, environment))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .addFilters(
                                new CharacterEncodingFilter(
                                        StandardCharsets.UTF_8.name(), true))
                        .build();
    }

    @Test
    void startsKakaoAuthorizationWithServerState() throws Exception {
        when(kakaoOAuthService.createAuthorizationUri(anyString()))
                .thenReturn(URI.create("https://kauth.kakao.com/oauth/authorize?state=test"));
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/api/auth/kakao/start").session(session))
                .andExpect(status().isFound())
                .andExpect(
                        header()
                                .string(
                                        "Location",
                                        "https://kauth.kakao.com/oauth/authorize?state=test"));

        verify(kakaoOAuthService).createAuthorizationUri(anyString());
    }

    @Test
    void createsAuthenticatedSessionAfterValidCallback() throws Exception {
        CurrentUserResponse user = user();
        when(kakaoOAuthService.login("authorization-code"))
                .thenReturn(new KakaoLoginResult(user, "access-token", "refresh-token"));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("kakaoOAuthState", "expected-state");

        mockMvc.perform(
                        get("/api/auth/kakao/callback")
                                .session(session)
                                .param("code", "authorization-code")
                                .param("state", "expected-state"))
                .andExpect(status().isFound())
                .andExpect(
                        header()
                                .string(
                                        "Location",
                                        "http://localhost:5173/auth/callback?status=success"));

        assertEquals(1L, session.getAttribute(DemoSessionUserResolver.USER_ID_ATTRIBUTE));
        assertEquals(
                "access-token",
                session.getAttribute(DemoSessionUserResolver.KAKAO_ACCESS_TOKEN_ATTRIBUTE));
    }

    @Test
    void distinguishesUserCancellation() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("kakaoOAuthState", "expected-state");

        mockMvc.perform(
                        get("/api/auth/kakao/callback")
                                .session(session)
                                .param("error", "access_denied"))
                .andExpect(status().isFound())
                .andExpect(
                        header()
                                .string(
                                        "Location",
                                        containsString("status=cancelled")));

        verify(kakaoOAuthService, never()).login(anyString());
    }

    @Test
    void rejectsCallbackWithMismatchedState() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("kakaoOAuthState", "expected-state");

        mockMvc.perform(
                        get("/api/auth/kakao/callback")
                                .session(session)
                                .param("code", "authorization-code")
                                .param("state", "wrong-state"))
                .andExpect(status().isFound())
                .andExpect(
                        header()
                                .string(
                                        "Location",
                                        containsString("code=OAUTH_STATE_INVALID")));

        verify(kakaoOAuthService, never()).login(anyString());
    }

    @Test
    void reportsAnonymousSessionWithoutCreatingLogin() throws Exception {
        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"authenticated\":false,\"user\":null}"));
    }

    @Test
    void reportsAuthenticatedSessionWithCurrentUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(DemoSessionUserResolver.USER_ID_ATTRIBUTE, 1L);
        when(userService.getCurrentUser(1L)).thenReturn(user());

        mockMvc.perform(get("/api/auth/session").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"authenticated\":true")))
                .andExpect(content().string(containsString("\"name\":\"김단짝\"")));
    }

    @Test
    void invalidatesSessionOnLogout() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(DemoSessionUserResolver.USER_ID_ATTRIBUTE, 1L);

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());
    }

    private CurrentUserResponse user() {
        return new CurrentUserResponse(
                1L,
                "김단짝",
                new ConsentSettings(false, false, false),
                new AccessibilitySettings(
                        FontSize.NORMAL, VoiceSpeed.NORMAL, GuideVoiceType.TTS));
    }
}
