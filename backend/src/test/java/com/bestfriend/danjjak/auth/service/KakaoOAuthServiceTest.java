package com.bestfriend.danjjak.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bestfriend.danjjak.auth.service.KakaoOAuthClient.KakaoTokens;
import com.bestfriend.danjjak.user.dto.UserDtos.AccessibilitySettings;
import com.bestfriend.danjjak.user.dto.UserDtos.ConsentSettings;
import com.bestfriend.danjjak.user.dto.UserDtos.CurrentUserResponse;
import com.bestfriend.danjjak.user.dto.UserDtos.FontSize;
import com.bestfriend.danjjak.user.dto.UserDtos.GuideVoiceType;
import com.bestfriend.danjjak.user.dto.UserDtos.VoiceSpeed;
import com.bestfriend.danjjak.user.service.UserService;
import org.junit.jupiter.api.Test;

class KakaoOAuthServiceTest {

    @Test
    void exchangesCodeAndBindsKakaoUser() {
        KakaoOAuthClient client = mock(KakaoOAuthClient.class);
        UserService userService = mock(UserService.class);
        KakaoOAuthService service = new KakaoOAuthService(client, userService);
        CurrentUserResponse user =
                new CurrentUserResponse(
                        1L,
                        "김단짝",
                        new ConsentSettings(true, true, false),
                        new AccessibilitySettings(
                                FontSize.NORMAL, VoiceSpeed.NORMAL, GuideVoiceType.TTS));
        when(client.exchangeAuthorizationCode("authorization-code"))
                .thenReturn(new KakaoTokens("access-token", "refresh-token"));
        when(client.getKakaoUserId("access-token")).thenReturn(987654321L);
        when(userService.findOrBindKakaoUser(987654321L)).thenReturn(user);

        var result = service.login("authorization-code");

        assertEquals(user, result.user());
        assertEquals("access-token", result.accessToken());
        assertEquals("refresh-token", result.refreshToken());
        verify(userService).findOrBindKakaoUser(987654321L);
    }
}
