package com.bestfriend.danjjak.pattern.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.ExecutionFinishRequest;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.ExecutionStatus;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.VisitStartRequest;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.VisitUpdateRequest;
import com.bestfriend.danjjak.pattern.mapper.PatternMapper;
import com.bestfriend.danjjak.pattern.model.StepVisitRecord;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class ExecutionLoggingServiceTest {

    private PatternMapper mapper;
    private PatternService service;
    private final LocalDateTime now = LocalDateTime.of(2026, 9, 6, 1, 0);

    @BeforeEach
    void setUp() {
        mapper = mock(PatternMapper.class);
        service = new PatternService(mapper, new PatternCatalog(),
                Clock.fixed(Instant.parse("2026-09-06T01:00:00Z"), ZoneOffset.UTC));
        when(mapper.isUsageLogAgreed(1L)).thenReturn(true);
        when(mapper.findExecutionStatusForUpdate(1L, 70L)).thenReturn("STARTED");
    }

    @ParameterizedTest
    @EnumSource(ExecutionStatus.class)
    void endsStartedExecutionWithTimestamp(ExecutionStatus status) {
        when(mapper.finishExecution(1L, 70L, status.name(), now)).thenReturn(1);
        var response = service.finishExecution(1L, 70L, new ExecutionFinishRequest(status));
        assertEquals(status, response.status());
        assertEquals(now, response.endedAt());
        verify(mapper).closeOpenVisits(70L, now);
    }

    @ParameterizedTest
    @EnumSource(ExecutionStatus.class)
    void rejectsEveryModificationOfFinishedExecution(ExecutionStatus status) {
        when(mapper.findExecutionStatusForUpdate(1L, 70L)).thenReturn(status.name());
        assertCode("EXECUTION_ALREADY_FINISHED", () -> service.finishExecution(
                1L, 70L, new ExecutionFinishRequest(status)));
        assertCode("EXECUTION_ALREADY_FINISHED", () -> service.startVisit(
                1L, 70L, new VisitStartRequest(41L)));
        assertCode("EXECUTION_ALREADY_FINISHED", () -> service.updateVisit(
                1L, 70L, 701L, new VisitUpdateRequest(null, null, null, null, true)));
        verify(mapper, never()).closeOpenVisits(anyLong(), any());
    }

    @Test
    void declinedConsentSkipsVisitCreationAndActions() {
        when(mapper.isUsageLogAgreed(1L)).thenReturn(false);
        assertNull(service.startVisit(1L, 70L, new VisitStartRequest(41L)));
        assertNull(service.updateVisit(1L, 70L, 701L,
                new VisitUpdateRequest(1, 1, 1, true, null)));
        verify(mapper, never()).insertStepVisit(any());
        verify(mapper, never()).findExecutionStatusForUpdate(anyLong(), anyLong());
        verify(mapper, never()).updateStepVisit(anyLong(), anyInt(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void rejectsMissingExecutionAndUnrelatedStepOrVisit() {
        assertCode("PATTERN_NOT_FOUND", () -> service.startExecution(1L, 99L, null));
        assertCode("PATTERN_EXECUTION_NOT_FOUND", () -> service.startVisit(
                1L, 99L, new VisitStartRequest(41L)));
        assertCode("INVALID_EXECUTION_STEP", () -> service.startVisit(
                1L, 70L, new VisitStartRequest(99L)));
        assertCode("STEP_VISIT_NOT_FOUND", () -> service.updateVisit(
                1L, 70L, 99L, new VisitUpdateRequest(1, null, null, null, null)));
        assertCode("PATTERN_EXECUTION_NOT_FOUND", () -> service.finishExecution(
                2L, 70L, new ExecutionFinishRequest(ExecutionStatus.COMPLETED)));
    }

    @Test
    void locksExecutionBeforeAllocatingVisitAndClosesPreviousVisit() {
        when(mapper.countExecutionStep(1L, 70L, 41L)).thenReturn(1);
        when(mapper.nextVisitNumber(70L, 41L)).thenReturn(null);
        doAnswer(call -> {
            StepVisitRecord visit = call.getArgument(0);
            assertEquals(1, visit.getVisitNumber());
            assertNull(visit.getEndedAt());
            visit.setVisitId(701L);
            return 1;
        }).when(mapper).insertStepVisit(any());
        when(mapper.findStepVisit(1L, 70L, 701L)).thenReturn(visit());
        service.startVisit(1L, 70L, new VisitStartRequest(41L));
        var order = inOrder(mapper);
        order.verify(mapper).findExecutionStatusForUpdate(1L, 70L);
        order.verify(mapper).nextVisitNumber(70L, 41L);
        order.verify(mapper).closeOpenVisits(70L, now);
        order.verify(mapper).insertStepVisit(any());
    }

    @Test
    void recordsActionsAndPreservesRouteDeviation() {
        StepVisitRecord visit = visit();
        visit.setRouteDeviation(true);
        when(mapper.findStepVisitForUpdate(1L, 70L, 701L)).thenReturn(visit);
        when(mapper.findStepVisit(1L, 70L, 701L)).thenReturn(visit);
        service.updateVisit(1L, 70L, 701L, new VisitUpdateRequest(2, 3, 4, false, null));
        verify(mapper).updateStepVisit(701L, 2, 3, 4, true, false, null);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void explicitCompletionClosesVisitAndClosedVisitRejectsChanges(boolean completed) {
        StepVisitRecord visit = visit();
        when(mapper.findStepVisitForUpdate(1L, 70L, 701L)).thenReturn(visit);
        when(mapper.findStepVisit(1L, 70L, 701L)).thenReturn(visit);
        service.updateVisit(1L, 70L, 701L, new VisitUpdateRequest(null, null, null, null, completed));
        verify(mapper).updateStepVisit(701L, 0, 0, 0, false, completed, now);
        visit.setEndedAt(now);
        visit.setCompleted(completed);
        assertCode("STEP_VISIT_ALREADY_FINISHED", () -> service.updateVisit(
                1L, 70L, 701L, new VisitUpdateRequest(1, null, null, null, true)));
    }

    @Test
    void rejectsNegativeCountsAndEmptyUpdate() {
        for (var request : new VisitUpdateRequest[] {
                new VisitUpdateRequest(-1, null, null, null, null),
                new VisitUpdateRequest(null, -1, null, null, null),
                new VisitUpdateRequest(null, null, -1, null, null),
                new VisitUpdateRequest(null, null, null, null, null)}) {
            assertCode("INVALID_REQUEST", () -> service.updateVisit(1L, 70L, 701L, request));
        }
    }

    private StepVisitRecord visit() {
        var visit = new StepVisitRecord();
        visit.setVisitId(701L);
        visit.setExecutionId(70L);
        visit.setStepId(41L);
        visit.setVisitNumber(1);
        visit.setStartedAt(now.minusSeconds(5));
        return visit;
    }

    private void assertCode(String code, org.junit.jupiter.api.function.Executable action) {
        assertEquals(code, assertThrows(ApiException.class, action).getCode());
    }
}
