package com.bestfriend.danjjak.auth.service;

import com.bestfriend.danjjak.auth.service.KakaoOAuthClient.KakaoTokens;
import com.bestfriend.danjjak.user.dto.UserDtos.CurrentUserResponse;
import com.bestfriend.danjjak.user.service.UserService;
import java.net.URI;
import org.springframework.stereotype.Service;

@Service
public class KakaoOAuthService {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final UserService userService;

    public KakaoOAuthService(KakaoOAuthClient kakaoOAuthClient, UserService userService) {
        this.kakaoOAuthClient = kakaoOAuthClient;
        this.userService = userService;
    }

    public URI createAuthorizationUri(String state) {
        return kakaoOAuthClient.createAuthorizationUri(state);
    }

    public KakaoLoginResult login(String authorizationCode) {
        KakaoTokens tokens = kakaoOAuthClient.exchangeAuthorizationCode(authorizationCode);
        long kakaoUserId = kakaoOAuthClient.getKakaoUserId(tokens.accessToken());
        CurrentUserResponse user = userService.findOrBindKakaoUser(kakaoUserId);
        return new KakaoLoginResult(user, tokens.accessToken(), tokens.refreshToken());
    }

    public record KakaoLoginResult(
            CurrentUserResponse user, String accessToken, String refreshToken) {}
}
