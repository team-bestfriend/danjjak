package com.bestfriend.danjjak.tts.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bestfriend.danjjak.tts.dto.TtsDtos.TtsRequest;
import com.bestfriend.danjjak.tts.dto.TtsDtos.TtsSpeed;
import org.junit.jupiter.api.Test;

class TtsServiceTest {

    @Test
    void passesTrimmedTextAndMappedSpeedToClient() {
        TtsClient ttsClient = mock(TtsClient.class);
        when(ttsClient.createSpeech("안내 문구", 0.8)).thenReturn(new byte[] {1, 2});
        TtsService ttsService = new TtsService(ttsClient);

        byte[] result = ttsService.createSpeech(new TtsRequest(" 안내 문구 ", TtsSpeed.SLOW));

        assertArrayEquals(new byte[] {1, 2}, result);
        verify(ttsClient).createSpeech("안내 문구", 0.8);
    }
}
