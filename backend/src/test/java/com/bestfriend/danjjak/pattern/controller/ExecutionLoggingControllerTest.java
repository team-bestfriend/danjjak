package com.bestfriend.danjjak.pattern.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.common.error.GlobalExceptionHandler;
import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.ExecutionStartResponse;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.StepVisitResponse;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.VisitStartRequest;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.VisitUpdateRequest;
import com.bestfriend.danjjak.pattern.service.PatternService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ExecutionLoggingControllerTest {

    private PatternService service;
    private MockMvc mvc;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        service = mock(PatternService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PatternController(service, new DemoSessionUserResolver()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        session = new MockHttpSession();
        session.setAttribute(DemoSessionUserResolver.USER_ID_ATTRIBUTE, 7L);
    }

    @Test
    void declinedConsentReturnsSuccessfulStartAndEmptyVisitResponses() throws Exception {
        when(service.startExecution(7L, 4L, null))
                .thenReturn(new ExecutionStartResponse(false, null, null, null));
        mvc.perform(post("/api/patterns/4/executions").session(session))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("\"loggingEnabled\":false")));
        mvc.perform(post("/api/pattern-executions/70/visits").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stepId\":41}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(patch("/api/pattern-executions/70/visits/701").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"routeDeviation\":true}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(service).startVisit(7L, 70L, new VisitStartRequest(41L));
        verify(service).updateVisit(7L, 70L, 701L, new VisitUpdateRequest(null, null, null, true, null));
    }

    @Test
    void returnsCreatedVisitAndUpdatedVisit() throws Exception {
        var response = new StepVisitResponse(701L, 70L, 41L, 1, 1, 2, 3, true, false,
                LocalDateTime.of(2026, 9, 6, 10, 0), null, null);
        when(service.startVisit(eq(7L), eq(70L), any())).thenReturn(response);
        when(service.updateVisit(eq(7L), eq(70L), eq(701L), any())).thenReturn(response);
        mvc.perform(post("/api/pattern-executions/70/visits").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stepId\":41}"))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("\"visitNumber\":1")));
        mvc.perform(patch("/api/pattern-executions/70/visits/701").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"retryCount\":1}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"routeDeviation\":true")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"retryCount", "backCount", "wrongTouchCount"})
    void rejectsNegativeCounts(String field) throws Exception {
        mvc.perform(patch("/api/pattern-executions/70/visits/701").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"" + field + "\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("INVALID_REQUEST")));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"STARTED", "UNKNOWN", ""})
    void rejectsNonTerminalStatus(String requestedStatus) throws Exception {
        mvc.perform(patch("/api/pattern-executions/70").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + requestedStatus + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void returnsConflictForClosedVisit() throws Exception {
        when(service.updateVisit(eq(7L), eq(70L), eq(701L), any()))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "STEP_VISIT_ALREADY_FINISHED", "이미 종료된 단계 방문입니다."));
        mvc.perform(patch("/api/pattern-executions/70/visits/701").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true}"))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("STEP_VISIT_ALREADY_FINISHED")));
    }

    @Test
    void requiresSessionUser() throws Exception {
        mvc.perform(post("/api/patterns/4/executions"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
}
