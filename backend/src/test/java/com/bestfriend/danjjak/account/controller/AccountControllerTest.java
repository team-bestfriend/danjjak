package com.bestfriend.danjjak.account.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bestfriend.danjjak.account.dto.AccountDtos.OwnedAccountResponse;
import com.bestfriend.danjjak.account.dto.AccountDtos.RecipientAccountResponse;
import com.bestfriend.danjjak.account.dto.AccountDtos.RegisteredPersonResponse;
import com.bestfriend.danjjak.account.service.AccountService;
import com.bestfriend.danjjak.common.error.GlobalExceptionHandler;
import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

class AccountControllerTest {

    private AccountService accountService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        DemoSessionUserResolver userResolver = mock(DemoSessionUserResolver.class);
        when(userResolver.resolveUserId(any(HttpSession.class))).thenReturn(1L);
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new AccountController(accountService, userResolver))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .addFilters(
                                new CharacterEncodingFilter(
                                        StandardCharsets.UTF_8.name(), true))
                        .build();
    }

    @Test
    void returnsOwnedAccounts() throws Exception {
        when(accountService.getOwnedAccounts(1L))
                .thenReturn(
                        List.of(
                                new OwnedAccountResponse(
                                        1L,
                                        "088",
                                        "신한은행",
                                        "110-000-000001",
                                        "생활비 통장",
                                        new BigDecimal("50000000"),
                                        true)));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"primary\":true")))
                .andExpect(content().string(containsString("신한은행")));
    }

    @Test
    void createsRegisteredPerson() throws Exception {
        when(accountService.createRegisteredPerson(
                        org.mockito.ArgumentMatchers.eq(1L), any()))
                .thenReturn(
                        new RegisteredPersonResponse(
                                10L,
                                "김민수",
                                "아들",
                                new RecipientAccountResponse(
                                        20L,
                                        "020",
                                        "우리은행",
                                        "1002-000-000001",
                                        "민수 계좌")));

        mockMvc.perform(
                        post("/api/registered-persons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\"김민수\",\"relationship\":\"아들\","
                                                + "\"bankCode\":\"020\",\"bankName\":\"우리은행\","
                                                + "\"accountNumber\":\"1002-000-000001\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("김민수")));
    }
}
