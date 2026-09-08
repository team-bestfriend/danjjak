package com.bestfriend.danjjak.analysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bestfriend.danjjak.analysis.dto.UsageAnalysisDtos.AnalysisStatus;
import com.bestfriend.danjjak.analysis.mapper.UsageAnalysisMapper;
import com.bestfriend.danjjak.analysis.model.PatternUsageRecord;
import com.bestfriend.danjjak.analysis.model.StepAnalysisRecord;
import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.user.mapper.UserMapper;
import com.bestfriend.danjjak.user.model.UserSettingsRecord;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UsageAnalysisServiceTest {

    private final LocalDate from = LocalDate.of(2026, 9, 1);
    private final LocalDate to = LocalDate.of(2026, 9, 30);
    private UsageAnalysisMapper mapper;
    private UserMapper userMapper;
    private UsageAnalysisService service;
    private UserSettingsRecord user;

    @BeforeEach
    void setUp() {
        mapper = mock(UsageAnalysisMapper.class);
        userMapper = mock(UserMapper.class);
        service = new UsageAnalysisService(mapper, userMapper);
        user = new UserSettingsRecord();
        user.setConsentCompleted(true);
        user.setUsageLogAgreed(true);
        when(userMapper.findCurrentUser(7L)).thenReturn(user);
    }

    @Test
    void returnsPatternCountsWithoutInventingUnvisitedStep() {
        withPatterns();
        var result = service.getUsageAnalysis(7L, from, to);
        assertEquals(AnalysisStatus.AVAILABLE, result.status());
        assertEquals(3, result.patterns().get(0).completedCount());
        assertEquals(0, result.patterns().get(1).completedCount());
        assertNull(result.difficultStep());
        assertTrue(result.steps().isEmpty());
    }

    @Test
    void highestScoreWinsBeforeDurationAndOrder() {
        assertWinner(step(11, 3, 9, "1"), step(12, 1, 8, "1000"), 11);
    }

    @Test
    void scoreTieUsesLongerAverageDuration() {
        assertWinner(step(11, 1, 8, "10.5"), step(12, 3, 8, "10.6"), 12);
    }

    @Test
    void durationTieUsesEarlierStepOrder() {
        assertWinner(step(11, 3, 8, "10.5"), step(12, 1, 8, "10.50"), 12);
    }

    @Test
    void completeTieUsesStepIdRegardlessOfInputOrder() {
        assertWinner(step(12, 1, 8, "10"), step(11, 1, 8, "10"), 11);
    }

    @Test
    void missingDurationIsNotFabricatedOrPreferredToMeasuredDuration() {
        assertWinner(step(11, 1, 8, null), step(12, 3, 8, "0"), 12);
        assertWinner(step(11, 1, 9, null), step(12, 3, 8, "100"), 11);
    }

    @Test
    void onlyOpenVisitsStillHaveScoreAndNullAverage() {
        withPatterns();
        when(mapper.findStepAnalysis(eq(7L), any(), any())).thenReturn(List.of(step(11, 1, 2, null)));
        var result = service.getUsageAnalysis(7L, from, to);
        assertEquals(11, result.difficultStep().stepId());
        assertNull(result.difficultStep().averageDurationSeconds());
    }

    @Test
    void mapsIndividualActionCountsWithoutChangingErrorScore() {
        withPatterns();
        var step = step(11, 1, 10, "12");
        step.setRetryCount(1);
        step.setBackCount(2);
        step.setWrongTouchCount(3);
        step.setRouteDeviationCount(4);
        when(mapper.findStepAnalysis(eq(7L), any(), any())).thenReturn(List.of(step));

        var result = service.getUsageAnalysis(7L, from, to).difficultStep();
        assertEquals(10, result.errorScore());
        assertEquals(1, result.retryCount());
        assertEquals(2, result.backCount());
        assertEquals(3, result.wrongTouchCount());
        assertEquals(4, result.routeDeviationCount());
    }

    @Test
    void noExecutionsReturnsExplicitEmptyState() {
        var result = service.getUsageAnalysis(7L, from, to);
        assertEquals(AnalysisStatus.NO_DATA, result.status());
        assertTrue(result.patterns().isEmpty());
        assertTrue(result.steps().isEmpty());
        assertNull(result.difficultStep());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void incompleteConsentTakesPrecedenceOverAgreement(boolean agreed) {
        user.setConsentCompleted(false);
        user.setUsageLogAgreed(agreed);
        var result = service.getUsageAnalysis(7L, from, to);
        assertEquals(AnalysisStatus.CONSENT_REQUIRED, result.status());
        assertNull(result.difficultStep());
        verifyNoInteractions(mapper);
    }

    @Test
    void declinedConsentDoesNotReadHistoricalLogs() {
        user.setUsageLogAgreed(false);
        var result = service.getUsageAnalysis(7L, from, to);
        assertEquals(AnalysisStatus.CONSENT_DECLINED, result.status());
        assertTrue(result.patterns().isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void missingUserAndInvalidPeriodsHaveDistinctErrors() {
        assertEquals("USER_NOT_FOUND", assertThrows(ApiException.class,
                () -> service.getUsageAnalysis(99L, from, to)).getCode());
        assertEquals("INVALID_ANALYSIS_PERIOD", assertThrows(ApiException.class,
                () -> service.getUsageAnalysis(7L, to, from)).getCode());
        assertEquals("INVALID_ANALYSIS_PERIOD", assertThrows(ApiException.class,
                () -> service.getUsageAnalysis(7L, null, to)).getCode());
        assertEquals("INVALID_ANALYSIS_PERIOD", assertThrows(ApiException.class,
                () -> service.getUsageAnalysis(7L, from, LocalDate.of(10000, 1, 1))).getCode());
        verifyNoInteractions(mapper);
    }

    private void withPatterns() {
        var first = new PatternUsageRecord();
        first.setPatternId(1);
        first.setPatternType("BALANCE_CHECK");
        first.setTitle("잔액 확인");
        first.setCompletedCount(3);
        var second = new PatternUsageRecord();
        second.setPatternId(2);
        second.setPatternType("TRANSFER");
        second.setTitle("송금");
        when(mapper.findPatternUsage(7L, from.atStartOfDay(), to.atTime(23, 59, 59)))
                .thenReturn(List.of(first, second));
    }

    private StepAnalysisRecord step(long id, int order, long score, String average) {
        var row = new StepAnalysisRecord();
        row.setPatternId(1);
        row.setStepId(id);
        row.setStepOrder(order);
        row.setStepCode("STEP_" + id);
        row.setStepName("단계 " + id);
        row.setVisitCount(2);
        row.setErrorScore(score);
        row.setAverageDurationSeconds(average == null ? null : new BigDecimal(average));
        return row;
    }

    private void assertWinner(StepAnalysisRecord first, StepAnalysisRecord second, long expected) {
        withPatterns();
        when(mapper.findStepAnalysis(eq(7L), any(), any())).thenReturn(List.of(first, second));
        assertEquals(expected, service.getUsageAnalysis(7L, from, to).difficultStep().stepId());
        when(mapper.findStepAnalysis(eq(7L), any(), any())).thenReturn(List.of(second, first));
        assertEquals(expected, service.getUsageAnalysis(7L, from, to).difficultStep().stepId());
    }
}
