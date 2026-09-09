package com.bestfriend.danjjak.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import org.junit.jupiter.api.Test;

class OpenAiChatIntentClientTest {
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = mock(HttpClient.class);
    private final OpenAiChatIntentClient client = new OpenAiChatIntentClient(http, json, "test-key");

    @Test
    void schemaUsesStrictEnumAndDisablesStorage() throws Exception {
        var body = json.readTree(client.requestBody("돈 보내고 싶어"));
        assertFalse(body.path("store").asBoolean());
        var format = body.path("text").path("format");
        assertEquals("json_schema", format.path("type").asText());
        assertTrue(format.path("strict").asBoolean());
        assertEquals(8, format.path("schema").path("properties").path("action").path("enum").size());
        assertEquals(3, format.path("schema").path("required").size());
        assertFalse(body.has("tools"));
    }

    @Test
    void promptShowsRecommendationsWhenReplyOffersMoreHelp() throws Exception {
        String instructions = json.readTree(client.requestBody("안녕")).path("instructions").asText();

        assertTrue(instructions.contains("무엇을 도와드릴까요?\", NONE, true"));
        assertTrue(instructions.contains("다른 도움이 필요하면 말씀해 주세요.\", NONE, true"));
        assertTrue(instructions.contains("피곤하다 → \"많이 피곤하셨군요. 잠시 쉬어 가세요.\", NONE, false"));
    }

    @Test
    void parsesOnlyCompletedStructuredAction() throws Exception {
        var parsed = client.parseResponse(output(
                "{\\\"message\\\":\\\"송금을 도와드릴게요.\\\",\\\"action\\\":\\\"TRANSFER\\\",\\\"showRecommendations\\\":false}"));
        assertEquals("송금을 도와드릴게요.", parsed.message());
        assertEquals(com.bestfriend.danjjak.chat.dto.ChatDtos.Action.TRANSFER, parsed.action());
        assertFalse(parsed.showRecommendations());
        assertThrows(IllegalArgumentException.class,
                () -> client.parseResponse(output(
                        "{\\\"message\\\":\\\"주식\\\",\\\"action\\\":\\\"BUY\\\",\\\"showRecommendations\\\":true}")));
        assertThrows(IllegalStateException.class, () -> client.parseResponse("{\"status\":\"incomplete\"}"));
        assertThrows(IllegalStateException.class, () -> client.parseResponse("{\"status\":\"completed\",\"output\":[]}"));
    }

    @Test
    void timeoutAndMissingKeyDoNotCallRealProvider() throws Exception {
        when(http.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new HttpTimeoutException("timeout"));
        assertThrows(IllegalStateException.class, () -> client.classify("잔액 확인"));
        assertThrows(IllegalStateException.class,
                () -> new OpenAiChatIntentClient(http, json, null).classify("잔액 확인"));
    }

    private String output(String text) {
        return "{\"status\":\"completed\",\"output\":[{\"content\":[{\"type\":\"output_text\",\"text\":\"" + text + "\"}]}]}";
    }
}
