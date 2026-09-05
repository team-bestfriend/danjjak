package com.bestfriend.danjjak.pattern.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.ExecutionStartRequest;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternCreateRequest;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternType;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.VisitStartRequest;
import com.bestfriend.danjjak.pattern.mapper.PatternMapper;
import com.bestfriend.danjjak.pattern.model.ExecutionCommand;
import com.bestfriend.danjjak.pattern.model.PatternCommand;
import com.bestfriend.danjjak.pattern.model.PatternRecord;
import com.bestfriend.danjjak.pattern.model.PatternStepRecord;
import com.bestfriend.danjjak.pattern.model.StepVisitRecord;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PatternServiceTest {

    private PatternMapper patternMapper;
    private PatternService patternService;

    @BeforeEach
    void setUp() {
        patternMapper = mock(PatternMapper.class);
        Clock clock =
                Clock.fixed(
                        Instant.parse("2026-09-06T01:00:00Z"),
                        ZoneId.of("Asia/Seoul"));
        patternService = new PatternService(patternMapper, new PatternCatalog(), clock);
    }

    @Test
    void createsTransferPatternAndCopiesAllTemplateSteps() {
        when(patternMapper.findActivePatternIdsForUpdate(1L)).thenReturn(List.of());
        when(patternMapper.countShortcut(1L, 9)).thenReturn(0);
        when(patternMapper.countRegisteredRecipientAccount(1L, 20L)).thenReturn(1);
        doAnswer(
                        invocation -> {
                            invocation.getArgument(0, PatternCommand.class).setPatternId(90L);
                            return 1;
                        })
                .when(patternMapper)
                .insertPattern(any(PatternCommand.class));
        when(patternMapper.findActivePattern(1L, 90L)).thenReturn(patternRecord(90L, "TRANSFER"));
        when(patternMapper.findPatternSteps(90L)).thenReturn(List.of(stepRecord(901L, 1)));

        var result =
                patternService.createPattern(
                        1L,
                        new PatternCreateRequest(
                                PatternType.TRANSFER,
                                9,
                                null,
                                null,
                                20L,
                                null));

        assertEquals(90L, result.patternId());
        ArgumentCaptor<com.bestfriend.danjjak.pattern.model.StepCommand> stepCaptor =
                ArgumentCaptor.forClass(
                        com.bestfriend.danjjak.pattern.model.StepCommand.class);
        verify(patternMapper, org.mockito.Mockito.times(6)).insertPatternStep(stepCaptor.capture());
        assertEquals("SELECT_SOURCE", stepCaptor.getAllValues().get(0).getStepCode());
        assertEquals("ENTER_PIN", stepCaptor.getAllValues().get(5).getStepCode());
    }

    @Test
    void rejectsTransferPatternWithoutRegisteredRecipient() {
        when(patternMapper.findActivePatternIdsForUpdate(1L)).thenReturn(List.of());
        when(patternMapper.countShortcut(1L, 9)).thenReturn(0);

        ApiException exception =
                assertThrows(
                        ApiException.class,
                        () ->
                                patternService.createPattern(
                                        1L,
                                        new PatternCreateRequest(
                                                PatternType.TRANSFER,
                                                9,
                                                null,
                                                null,
                                                null,
                                                null)));

        assertEquals("TRANSFER_RECIPIENT_REQUIRED", exception.getCode());
        verify(patternMapper, never()).insertPattern(any());
    }

    @Test
    void consentDeclineReturnsPatternWithoutCreatingExecution() {
        when(patternMapper.findActivePattern(1L, 4L)).thenReturn(patternRecord(4L, "BALANCE_CHECK"));
        when(patternMapper.findPatternSteps(4L)).thenReturn(List.of(stepRecord(41L, 1)));
        when(patternMapper.isUsageLogAgreed(1L)).thenReturn(false);

        var result = patternService.startExecution(1L, 4L, new ExecutionStartRequest(null));

        assertFalse(result.loggingEnabled());
        assertNull(result.executionId());
        verify(patternMapper, never()).insertExecution(any(ExecutionCommand.class));
    }

    @Test
    void reenteringSameStepUsesNextVisitNumber() {
        when(patternMapper.countExecutionStep(1L, 70L, 41L)).thenReturn(1);
        when(patternMapper.nextVisitNumber(70L, 41L)).thenReturn(2);
        doAnswer(
                        invocation -> {
                            invocation.getArgument(0, StepVisitRecord.class).setVisitId(702L);
                            return 1;
                        })
                .when(patternMapper)
                .insertStepVisit(any(StepVisitRecord.class));
        when(patternMapper.findStepVisit(1L, 70L, 702L)).thenAnswer(
                invocation -> {
                    StepVisitRecord visit = new StepVisitRecord();
                    visit.setVisitId(702L);
                    visit.setExecutionId(70L);
                    visit.setStepId(41L);
                    visit.setVisitNumber(2);
                    visit.setStartedAt(java.time.LocalDateTime.of(2026, 9, 6, 10, 0));
                    return visit;
                });

        var result = patternService.startVisit(1L, 70L, new VisitStartRequest(41L));

        assertEquals(2, result.visitNumber());
        assertEquals(702L, result.visitId());
    }

    private PatternRecord patternRecord(long id, String type) {
        PatternRecord record = new PatternRecord();
        record.setPatternId(id);
        record.setUserId(1L);
        record.setShortcutNumber(4);
        record.setPatternType(type);
        record.setTitle("잔액 확인");
        record.setDescription("잔액을 확인합니다.");
        return record;
    }

    private PatternStepRecord stepRecord(long id, int order) {
        PatternStepRecord step = new PatternStepRecord();
        step.setStepId(id);
        step.setPatternId(4L);
        step.setStepOrder(order);
        step.setStepCode("CHECK_RESULT");
        step.setStepName("잔액 확인");
        step.setInstructionText("잔액을 확인해 주세요.");
        step.setScreenCode("task-4");
        return step;
    }
}
