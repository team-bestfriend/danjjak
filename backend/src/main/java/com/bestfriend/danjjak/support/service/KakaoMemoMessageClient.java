package com.bestfriend.danjjak.support.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class KakaoMemoMessageClient implements KakaoMessageClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI messageUri;
    private final String linkUrl;

    public KakaoMemoMessageClient(Environment environment) {
        this.httpClient =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        this.objectMapper = new ObjectMapper();
        this.messageUri = URI.create(environment.getRequiredProperty("kakao.message-url"));
        this.linkUrl = environment.getRequiredProperty("kakao.message-link-url");
    }

    @Override
    public KakaoSendResult sendToMe(String accessToken, String message) {
        try {
            String template = createTextTemplate(message);
            String body =
                    "template_object="
                            + URLEncoder.encode(template, StandardCharsets.UTF_8);
            HttpRequest request =
                    HttpRequest.newBuilder(messageUri)
                            .timeout(Duration.ofSeconds(5))
                            .header("Authorization", "Bearer " + accessToken)
                            .header(
                                    "Content-Type",
                                    "application/x-www-form-urlencoded;charset=utf-8")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode responseBody = objectMapper.readTree(response.body());
            boolean success =
                    response.statusCode() >= 200
                            && response.statusCode() < 300
                            && responseBody.path("result_code").asInt(-1) == 0;
            return new KakaoSendResult(
                    success,
                    response.statusCode(),
                    success ? "KAKAO_SENT" : "KAKAO_API_REJECTED");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new KakaoSendResult(false, null, "KAKAO_REQUEST_INTERRUPTED");
        } catch (IOException | RuntimeException exception) {
            return new KakaoSendResult(false, null, "KAKAO_REQUEST_FAILED");
        }
    }

    private String createTextTemplate(String message) throws IOException {
        ObjectNode template = objectMapper.createObjectNode();
        template.put("object_type", "text");
        template.put("text", message);
        ObjectNode link = template.putObject("link");
        link.put("web_url", linkUrl);
        link.put("mobile_web_url", linkUrl);
        template.put("button_title", "단짝 확인");
        return objectMapper.writeValueAsString(template);
    }
}
