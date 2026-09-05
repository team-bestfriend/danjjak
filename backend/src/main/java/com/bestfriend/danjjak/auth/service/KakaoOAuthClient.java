package com.bestfriend.danjjak.auth.service;

import com.bestfriend.danjjak.common.error.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class KakaoOAuthClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String restApiKey;
    private final String clientSecret;
    private final String redirectUri;
    private final URI authorizationUri;
    private final URI tokenUri;
    private final URI userInfoUri;

    @Autowired
    public KakaoOAuthClient(Environment environment) {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                new ObjectMapper(),
                environment.getRequiredProperty("kakao.rest-api-key"),
                environment.getRequiredProperty("kakao.client-secret"),
                environment.getRequiredProperty("kakao.redirect-uri"),
                URI.create(environment.getRequiredProperty("kakao.authorization-url")),
                URI.create(environment.getRequiredProperty("kakao.token-url")),
                URI.create(environment.getRequiredProperty("kakao.user-info-url")));
    }

    KakaoOAuthClient(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            String restApiKey,
            String clientSecret,
            String redirectUri,
            URI authorizationUri,
            URI tokenUri,
            URI userInfoUri) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.restApiKey = restApiKey;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
    }

    public URI createAuthorizationUri(String state) {
        requireConfigured();
        return UriComponentsBuilder.fromUri(authorizationUri)
                .queryParam("response_type", "code")
                .queryParam("client_id", restApiKey)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "talk_message")
                .queryParam("state", state)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
    }

    public KakaoTokens exchangeAuthorizationCode(String code) {
        requireConfigured();
        String body =
                form("grant_type", "authorization_code")
                        + "&"
                        + form("client_id", restApiKey)
                        + "&"
                        + form("redirect_uri", redirectUri)
                        + "&"
                        + form("code", code)
                        + "&"
                        + form("client_secret", clientSecret);
        HttpRequest request =
                HttpRequest.newBuilder(tokenUri)
                        .timeout(Duration.ofSeconds(10))
                        .header(
                                "Content-Type",
                                "application/x-www-form-urlencoded;charset=utf-8")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
        JsonNode response = sendJson(request, "KAKAO_TOKEN_EXCHANGE_FAILED");
        String accessToken = response.path("access_token").asText("");
        if (accessToken.isBlank()) {
            throw providerError("KAKAO_TOKEN_EXCHANGE_FAILED");
        }
        return new KakaoTokens(accessToken, response.path("refresh_token").asText(null));
    }

    public long getKakaoUserId(String accessToken) {
        HttpRequest request =
                HttpRequest.newBuilder(userInfoUri)
                        .timeout(Duration.ofSeconds(10))
                        .header("Authorization", "Bearer " + accessToken)
                        .GET()
                        .build();
        JsonNode response = sendJson(request, "KAKAO_USER_INFO_FAILED");
        JsonNode id = response.path("id");
        if (!id.canConvertToLong()) {
            throw providerError("KAKAO_USER_INFO_FAILED");
        }
        return id.longValue();
    }

    private JsonNode sendJson(HttpRequest request, String errorCode) {
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw providerError(errorCode);
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw providerError(errorCode);
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof ApiException apiException) {
                throw apiException;
            }
            throw providerError(errorCode);
        }
    }

    private String form(String name, String value) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "="
                + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private void requireConfigured() {
        if (restApiKey == null
                || restApiKey.isBlank()
                || clientSecret == null
                || clientSecret.isBlank()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "KAKAO_NOT_CONFIGURED",
                    "카카오 로그인을 위한 로컬 설정이 필요합니다.");
        }
    }

    private ApiException providerError(String code) {
        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                code,
                "카카오 로그인 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
    }

    public record KakaoTokens(String accessToken, String refreshToken) {}
}
