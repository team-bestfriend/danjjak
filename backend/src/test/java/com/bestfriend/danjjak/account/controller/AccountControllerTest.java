package com.bestfriend.danjjak.account.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bestfriend.danjjak.account.dto.AccountDtos.OwnedAccountResponse;
import com.bestfriend.danjjak.account.dto.AccountDtos.MockAccountImportOptionResponse;
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
    void returnsMockAccountImportOptions() throws Exception {
        when(accountService.getMockAccountImportOptions(1L))
                .thenReturn(
                        List.of(
                                new MockAccountImportOptionResponse(
                                        2L,
                                        "004",
                                        "국민은행",
                                        "123-000-000002",
                                        "저축 통장",
                                        new BigDecimal("30000000"),
                                        false,
                                        false)));

        mockMvc.perform(get("/api/accounts/import-options"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"imported\":false")));
    }

    @Test
    void importsSelectedMockAccount() throws Exception {
        when(accountService.importMockAccount(1L, 2L))
                .thenReturn(
                        new OwnedAccountResponse(
                                2L,
                                "004",
                                "국민은행",
                                "123-000-000002",
                                "저축 통장",
                                new BigDecimal("30000000"),
                                false));

        mockMvc.perform(post("/api/accounts/2/import"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"accountId\":2")));
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
                                "adult_man",
                                List.of()));

        mockMvc.perform(
                        post("/api/registered-persons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\"김민수\",\"relationship\":\"아들\","
                                                + "\"profileImageKey\":\"adult_man\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("김민수")));
    }

    @Test
    void rejectsRecipientAccountWithInvalidAccountNumber() throws Exception {
        mockMvc.perform(
                        post("/api/registered-persons/10/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"bankCode\":\"020\",\"bankName\":\"우리은행\","
                                                + "\"accountNumber\":\"ABC\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("\"code\":\"INVALID_REQUEST\"")));

        verifyNoInteractions(accountService);
    }

    @Test
    void addsRecipientAccountToExistingPerson() throws Exception {
        when(accountService.addRecipientAccount(
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.eq(10L),
                        any()))
                .thenReturn(
                        new RegisteredPersonResponse(
                                10L,
                                "김민수",
                                "아들",
                                "adult_man",
                                List.of(
                                        new RecipientAccountResponse(
                                                21L,
                                                "088",
                                                "신한은행",
                                                "110-222-333333",
                                                "생활비"))));

        mockMvc.perform(
                        post("/api/registered-persons/10/accounts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"bankCode\":\"088\",\"bankName\":\"신한은행\","
                                                + "\"accountNumber\":\"110-222-333333\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("\"accountId\":21")));
    }

    @Test
    void updatesOnlySelectedRecipientAccount() throws Exception {
        when(accountService.updateRecipientAccount(
                        org.mockito.ArgumentMatchers.eq(1L),
                        org.mockito.ArgumentMatchers.eq(10L),
                        org.mockito.ArgumentMatchers.eq(21L),
                        any()))
                .thenReturn(
                        new RegisteredPersonResponse(
                                10L,
                                "김민수",
                                "아들",
                                "adult_man",
                                List.of(
                                        new RecipientAccountResponse(
                                                21L,
                                                "088",
                                                "신한은행",
                                                "110-222-333333",
                                                "수정 계좌"))));

        mockMvc.perform(
                        put("/api/registered-persons/10/accounts/21")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"bankCode\":\"088\",\"bankName\":\"신한은행\","
                                                + "\"accountNumber\":\"110-222-333333\","
                                                + "\"accountAlias\":\"수정 계좌\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("수정 계좌")));
    }

    @Test
    void deletesSelectedRecipientAccount() throws Exception {
        mockMvc.perform(delete("/api/registered-persons/10/accounts/21"))
                .andExpect(status().isNoContent());

        verify(accountService).deleteRecipientAccount(1L, 10L, 21L);
    }

    @Test
    void deletesRegisteredPerson() throws Exception {
        mockMvc.perform(delete("/api/registered-persons/10"))
                .andExpect(status().isNoContent());

        verify(accountService).deleteRegisteredPerson(1L, 10L);
    }
}
