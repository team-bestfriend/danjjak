package com.bestfriend.danjjak.chat.controller;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bestfriend.danjjak.chat.dto.ChatDtos.Action;
import com.bestfriend.danjjak.chat.dto.ChatDtos.ChatResponse;
import com.bestfriend.danjjak.chat.service.ChatService;
import com.bestfriend.danjjak.common.error.GlobalExceptionHandler;
import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ChatControllerTest {
    private final ChatService service = mock(ChatService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new ChatController(service, new DemoSessionUserResolver()))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void requiresSessionAndValidMessage() throws Exception {
        mvc.perform(post("/api/chat/messages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"잔액 확인\"}")).andExpect(status().isUnauthorized());
        for (String message : new String[] { "", "   ", "가".repeat(501) }) {
            mvc.perform(post("/api/chat/messages").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"message\":\"" + message + "\"}")).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void returnsStructuredResponseForSessionUser() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 7L);
        when(service.reply(7L, "잔액 확인")).thenReturn(
                new ChatResponse("잔액을 확인해 보세요.", Action.BALANCE_CHECK, 42L, false, false));
        var result = mvc.perform(post("/api/chat/messages").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"잔액 확인\"}"))
                .andExpect(status().isOk())
                .andReturn();
        var body = new ObjectMapper().readTree(result.getResponse().getContentAsString());
        assertEquals("BALANCE_CHECK", body.path("action").asText());
        assertEquals(42L, body.path("patternId").asLong());
        assertEquals(false, body.path("retryable").asBoolean());
        assertEquals(false, body.path("showRecommendations").asBoolean());
        verify(service).reply(7L, "잔액 확인");
    }
}
