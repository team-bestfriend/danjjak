package com.bestfriend.danjjak.analysis.service;

import com.bestfriend.danjjak.analysis.service.InstructionTextSuggestionClient.SuggestionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class OpenAiInstructionTextSuggestionClient implements InstructionTextSuggestionClient {
    private static final Logger log = LogManager.getLogger(OpenAiInstructionTextSuggestionClient.class);
    private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");
    private static final String INSTRUCTIONS = """
        디지털 금융 사용이 익숙하지 않은 고령 사용자를 위한 안내 문구를 만드세요.

        목표는 사용자가 다음 행동을 바로 이해할 수 있도록
        현재 문구를 더 짧고, 쉽고, 단순하게 바꾸는 것입니다.

        반드시 아래 원칙을 지키세요.

        - 매우 쉬운 일상 한국어를 사용하세요.
        - 한 문장으로 짧게 작성하세요.
        - 한 문장에는 한 가지 행동만 안내하세요.
        - "~해 주세요" 형태로 직접 안내하세요.
        - 가능하면 20~30자 안팎으로 작성하세요.
        - 현재 문구보다 길거나 복잡하게 만들지 마세요.
        - 불필요한 배경 설명과 수식어는 최대한 제거하세요.
        - 행동에 꼭 필요한 핵심 정보는 유지하세요.
        - 숫자, 횟수, 대상처럼 행동 수행에 필요한 정보는 빼지 마세요.
        - 금융·IT 전문용어와 딱딱한 표현을 쉬운 말로 바꾸세요.
        - '해당', '진행', '수행', '입력값', '인증', '경로' 같은 표현은 쓰지 마세요.
        - '화면의', '차례대로', '확인 후'처럼 없어도 이해되는 표현은 최대한 줄이세요.
        - 현재 문구의 의미와 실제 금융 동작은 바꾸지 마세요.
        - 새로운 기능이나 사실을 만들지 마세요.
        - 사용자를 평가하거나 분석 수치를 문장에 드러내지 마세요.
        - 현재 문구를 그대로 복사하지 말고 더 쉬운 표현으로 바꾸세요.

        예시:

        현재:
        "돈을 보낼 내 통장의 비밀번호 네 자리를 눌러 주세요."

        좋은 제안:
        "통장 비밀번호 네 자리를 눌러 주세요."

        나쁜 제안:
        "돈을 보낼 때 비밀번호를 눌러 주세요."
        이유: '네 자리'라는 중요한 정보가 빠졌습니다.

        나쁜 제안:
        "화면의 숫자 버튼을 차례대로 눌러 통장 비밀번호 네 자리를 입력해 주세요."
        이유: 현재 문구보다 길고 복잡하며 불필요한 표현이 많습니다.

        제안 문장만 출력하세요.
        """;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public OpenAiInstructionTextSuggestionClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                new ObjectMapper(), System.getenv("OPENAI_API_KEY"));
    }

    OpenAiInstructionTextSuggestionClient(HttpClient httpClient, ObjectMapper objectMapper, String apiKey) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    @Override
    public String suggest(SuggestionContext context) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("OpenAI API key is missing");
        try {
            HttpRequest request = HttpRequest.newBuilder(RESPONSES_URI)
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(createRequestBody(context))).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logProviderError(response);
                throw new IllegalStateException("OpenAI suggestion request failed");
            }
            return outputText(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("OpenAI suggestion request interrupted", exception);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("OpenAI suggestion request failed", exception);
        }
    }

    private String createRequestBody(SuggestionContext context) throws IOException {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", "gpt-4o-mini");
        body.put("instructions", INSTRUCTIONS);
        body.put("input", objectMapper.writeValueAsString(context));
        body.put("max_output_tokens", 80);
        return objectMapper.writeValueAsString(body);
    }

    private String outputText(String responseBody) throws IOException {
        JsonNode output = objectMapper.readTree(responseBody).path("output");
        if (!output.isArray()) return null;
        for (JsonNode item : output) {
            for (JsonNode content : item.path("content")) {
                JsonNode text = content.path("text");
                if (text.isTextual()) return text.textValue();
            }
        }
        return null;
    }

    private void logProviderError(HttpResponse<String> response) {
        String code = null;
        String type = null;
        try {
            JsonNode error = objectMapper.readTree(response.body()).path("error");
            code = textValue(error, "code");
            type = textValue(error, "type");
        } catch (IOException | RuntimeException exception) {
            log.warn("OpenAI 문구 제안 오류 응답을 파싱하지 못했습니다: status={}", response.statusCode());
        }
        log.warn("OpenAI 문구 제안 요청 실패: status={}, code={}, type={}", response.statusCode(), code, type);
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isTextual() ? value.textValue() : null;
    }
}
