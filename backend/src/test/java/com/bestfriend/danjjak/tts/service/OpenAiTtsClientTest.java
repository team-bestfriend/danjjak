package com.bestfriend.danjjak.tts.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.bestfriend.danjjak.common.error.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class OpenAiTtsClientTest {

    @Test
    void returnsCommonProviderErrorWhenApiKeyIsMissing() {
        OpenAiTtsClient client =
                new OpenAiTtsClient(mock(HttpClient.class), new ObjectMapper(), null);

        ApiException exception =
                assertThrows(ApiException.class, () -> client.createSpeech("안내 문구", 1.0));

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals("TTS_PROVIDER_ERROR", exception.getCode());
    }
}
