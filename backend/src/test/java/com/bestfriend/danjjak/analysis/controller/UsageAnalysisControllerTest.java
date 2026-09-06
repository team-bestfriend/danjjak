package com.bestfriend.danjjak.analysis.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bestfriend.danjjak.analysis.mapper.UsageAnalysisMapper;
import com.bestfriend.danjjak.analysis.service.UsageAnalysisService;
import com.bestfriend.danjjak.common.error.GlobalExceptionHandler;
import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
import com.bestfriend.danjjak.user.mapper.UserMapper;
import com.bestfriend.danjjak.user.model.UserSettingsRecord;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UsageAnalysisControllerTest {

    private MockMvc mvc;
    private MockHttpSession session;
    private UsageAnalysisMapper mapper;
    private UserMapper userMapper;
    private UserSettingsRecord user;

    @BeforeEach
    void setUp() {
        mapper = mock(UsageAnalysisMapper.class);
        userMapper = mock(UserMapper.class);
        user = new UserSettingsRecord();
        user.setConsentCompleted(true);
        user.setUsageLogAgreed(true);
        when(userMapper.findCurrentUser(7L)).thenReturn(user);
        var objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mvc = MockMvcBuilders.standaloneSetup(new UsageAnalysisController(
                        new UsageAnalysisService(mapper, userMapper), new DemoSessionUserResolver()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper)).build();
        session = new MockHttpSession();
        session.setAttribute(DemoSessionUserResolver.USER_ID_ATTRIBUTE, 7L);
    }

    @Test
    void returnsExplicitEmptyStateAndIsoDatesUsingSessionUserOnly() throws Exception {
        mvc.perform(get("/api/usage-analysis").session(session)
                        .param("from", "2026-09-01").param("to", "2026-09-30").param("userId", "99"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"status\":\"NO_DATA\"")))
                .andExpect(content().string(containsString("\"from\":\"2026-09-01\"")))
                .andExpect(content().string(containsString("\"patterns\":[]")))
                .andExpect(content().string(containsString("\"difficultStep\":null")));
        verify(mapper).findPatternUsage(eq(7L), any(), any());
        verify(mapper, never()).findPatternUsage(eq(99L), any(), any());
    }

    @Test
    void consentResponsesAreSuccessfulAndDoNotReadLogs() throws Exception {
        user.setUsageLogAgreed(false);
        mvc.perform(get("/api/usage-analysis").session(session)
                        .param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CONSENT_DECLINED")));
        user.setConsentCompleted(false);
        mvc.perform(get("/api/usage-analysis").session(session)
                        .param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CONSENT_REQUIRED")));
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-date", "2026-02-30", ""})
    void rejectsInvalidDate(String from) throws Exception {
        mvc.perform(get("/api/usage-analysis").session(session)
                        .param("from", from).param("to", "2026-09-30"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsMissingAndReversedPeriod() throws Exception {
        mvc.perform(get("/api/usage-analysis").session(session).param("from", "2026-09-01"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/usage-analysis").session(session)
                        .param("from", "2026-10-01").param("to", "2026-09-30"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("INVALID_ANALYSIS_PERIOD")));
        verifyNoInteractions(mapper);
    }

    @Test
    void requiresSessionAndDistinguishesMissingUser() throws Exception {
        mvc.perform(get("/api/usage-analysis").param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isUnauthorized());
        verify(userMapper, never()).findCurrentUser(anyLong());
        session.setAttribute(DemoSessionUserResolver.USER_ID_ATTRIBUTE, 99L);
        mvc.perform(get("/api/usage-analysis").session(session)
                        .param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("USER_NOT_FOUND")));
    }
}
