package com.bestfriend.danjjak.tts.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bestfriend.danjjak.common.error.GlobalExceptionHandler;
import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
import com.bestfriend.danjjak.tts.dto.TtsDtos.TtsRequest;
import com.bestfriend.danjjak.tts.service.TtsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import javax.servlet.http.HttpSession;

class TtsControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TtsService ttsService = mock(TtsService.class);
        DemoSessionUserResolver userResolver = mock(DemoSessionUserResolver.class);
        when(userResolver.resolveUserId(any(HttpSession.class))).thenReturn(1L);
        when(ttsService.createSpeech(any(TtsRequest.class))).thenReturn(new byte[] {1, 2, 3});
        mockMvc =
                MockMvcBuilders.standaloneSetup(new TtsController(ttsService, userResolver))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void returnsMpegAudio() throws Exception {
        mockMvc.perform(
                        post("/api/tts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"text\":\"안내 문구\",\"speed\":\"NORMAL\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("audio/mpeg"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    void rejectsBlankText() throws Exception {
        mockMvc.perform(
                        post("/api/tts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"text\":\" \",\"speed\":\"NORMAL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("\"code\":\"INVALID_REQUEST\"")));
    }

    @Test
    void rejectsUnsupportedSpeed() throws Exception {
        mockMvc.perform(
                        post("/api/tts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"text\":\"안내 문구\",\"speed\":\"VERY_FAST\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("\"code\":\"INVALID_REQUEST\"")));
    }
}
