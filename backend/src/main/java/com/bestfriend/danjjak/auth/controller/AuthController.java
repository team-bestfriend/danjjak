package com.bestfriend.danjjak.auth.controller;

import com.bestfriend.danjjak.auth.dto.AuthDtos.SessionResponse;
import com.bestfriend.danjjak.auth.service.KakaoOAuthService;
import com.bestfriend.danjjak.auth.service.KakaoOAuthService.KakaoLoginResult;
import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
import com.bestfriend.danjjak.user.dto.UserDtos.CurrentUserResponse;
import com.bestfriend.danjjak.user.service.UserService;
import java.net.URI;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String OAUTH_STATE_ATTRIBUTE = "kakaoOAuthState";
    private static final int SESSION_TIMEOUT_SECONDS = 2 * 60 * 60;

    private final KakaoOAuthService kakaoOAuthService;
    private final UserService userService;
    private final String frontendUrl;

    public AuthController(
            KakaoOAuthService kakaoOAuthService,
            UserService userService,
            Environment environment) {
        this.kakaoOAuthService = kakaoOAuthService;
        this.userService = userService;
        this.frontendUrl =
                environment.getRequiredProperty("app.frontend-url").replaceAll("/+$", "");
    }

    @GetMapping("/kakao/start")
    public ResponseEntity<Void> startKakaoLogin(HttpSession session) {
        String state = UUID.randomUUID().toString();
        session.setAttribute(OAUTH_STATE_ATTRIBUTE, state);
        return redirect(kakaoOAuthService.createAuthorizationUri(state));
    }

    @GetMapping("/kakao/callback")
    public ResponseEntity<Void> handleKakaoCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (error != null) {
            clearOAuthState(session);
            String status = "access_denied".equals(error) ? "cancelled" : "failed";
            String errorCode =
                    "access_denied".equals(error)
                            ? "OAUTH_CANCELLED"
                            : "KAKAO_AUTHORIZATION_FAILED";
            return redirect(callbackUri(status, errorCode));
        }

        if (session == null || !matchesAndConsumeState(session, state)) {
            return redirect(callbackUri("failed", "OAUTH_STATE_INVALID"));
        }
        if (code == null || code.isBlank()) {
            return redirect(callbackUri("failed", "OAUTH_CODE_MISSING"));
        }

        try {
            KakaoLoginResult result = kakaoOAuthService.login(code);
            request.changeSessionId();
            session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
            session.setAttribute(
                    DemoSessionUserResolver.USER_ID_ATTRIBUTE, result.user().userId());
            session.setAttribute(
                    DemoSessionUserResolver.KAKAO_ACCESS_TOKEN_ATTRIBUTE,
                    result.accessToken());
            if (result.refreshToken() != null && !result.refreshToken().isBlank()) {
                session.setAttribute(
                        DemoSessionUserResolver.KAKAO_REFRESH_TOKEN_ATTRIBUTE,
                        result.refreshToken());
            }
            return redirect(callbackUri("success", null));
        } catch (ApiException exception) {
            return redirect(callbackUri("failed", exception.getCode()));
        }
    }

    @GetMapping("/session")
    public SessionResponse getSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return new SessionResponse(false, null);
        }
        Object userId = session.getAttribute(DemoSessionUserResolver.USER_ID_ATTRIBUTE);
        if (!(userId instanceof Number number)) {
            return new SessionResponse(false, null);
        }
        CurrentUserResponse user = userService.getCurrentUser(number.longValue());
        return new SessionResponse(true, user);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    private boolean matchesAndConsumeState(HttpSession session, String returnedState) {
        Object expected = session.getAttribute(OAUTH_STATE_ATTRIBUTE);
        session.removeAttribute(OAUTH_STATE_ATTRIBUTE);
        return expected instanceof String text
                && returnedState != null
                && text.equals(returnedState);
    }

    private void clearOAuthState(HttpSession session) {
        if (session != null) {
            session.removeAttribute(OAUTH_STATE_ATTRIBUTE);
        }
    }

    private URI callbackUri(String status, String errorCode) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromUriString(frontendUrl)
                        .path("/auth/callback")
                        .queryParam("status", status);
        if (errorCode != null) {
            builder.queryParam("code", errorCode);
        }
        return builder.build().encode().toUri();
    }

    private ResponseEntity<Void> redirect(URI location) {
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }
}
