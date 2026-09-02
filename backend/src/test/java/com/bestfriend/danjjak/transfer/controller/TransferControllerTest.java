package com.bestfriend.danjjak.transfer.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bestfriend.danjjak.common.error.GlobalExceptionHandler;
import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.TransferResponse;
import com.bestfriend.danjjak.transfer.service.TransferService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

class TransferControllerTest {

    private TransferService transferService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        transferService = mock(TransferService.class);
        DemoSessionUserResolver userResolver = mock(DemoSessionUserResolver.class);
        when(userResolver.resolveUserId(any(HttpSession.class))).thenReturn(1L);
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new TransferController(transferService, userResolver))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .addFilters(
                                new CharacterEncodingFilter(
                                        StandardCharsets.UTF_8.name(), true))
                        .build();
    }

    @Test
    void completesRegisteredRecipientTransfer() throws Exception {
        when(transferService.transfer(org.mockito.ArgumentMatchers.eq(1L), any()))
                .thenReturn(
                        new TransferResponse("COMPLETED", 30L, new BigDecimal("49900000")));

        mockMvc.perform(
                        post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"sourceAccountId\":1,\"registeredRecipientAccountId\":20,"
                                                + "\"amount\":100000,\"pin\":\"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"status\":\"COMPLETED\"")))
                .andExpect(content().string(containsString("\"transactionId\":30")));
    }
}
