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
        assertFalse(body.has("tools"));
    }

    @Test
    void parsesOnlyCompletedStructuredAction() throws Exception {
        assertEquals("TRANSFER", client.parseResponse(output("{\\\"action\\\":\\\"TRANSFER\\\"}")));
        assertThrows(IllegalArgumentException.class,
                () -> client.parseResponse(output("{\\\"action\\\":\\\"BUY\\\"}")));
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
