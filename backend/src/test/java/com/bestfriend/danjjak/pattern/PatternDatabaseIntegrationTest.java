package com.bestfriend.danjjak.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bestfriend.danjjak.config.RootConfig;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.ExecutionFinishRequest;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.ExecutionStartRequest;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.ExecutionStatus;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternOrderItem;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternOrderRequest;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.VisitStartRequest;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.VisitUpdateRequest;
import com.bestfriend.danjjak.pattern.service.PatternService;
import com.bestfriend.danjjak.user.dto.UserDtos.ConsentUpdateRequest;
import com.bestfriend.danjjak.user.service.UserService;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@EnabledIfEnvironmentVariable(named = "DANJJAK_DB_INTEGRATION_TEST", matches = "true")
@Transactional
class PatternDatabaseIntegrationTest {

    @Autowired private PatternService patternService;
    @Autowired private UserService userService;
    @Autowired private DataSource dataSource;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void readsEightSeededPatternsAndPersistsConfirmedSwap() {
        var patterns = patternService.getPatterns(1L);

        assertEquals(8, patterns.size());
        assertEquals("아들에게 송금", patterns.get(0).title());
        assertEquals("김민수", patterns.get(0).linkedAccount().registeredPersonName());
        var transferDetail = patternService.getPattern(1L, patterns.get(0).patternId());
        assertEquals(6, transferDetail.steps().size());
        assertTrue(
                transferDetail.steps().stream()
                        .noneMatch(step -> "TRANSFER_COMPLETE".equals(step.stepCode())));

        List<PatternOrderItem> swapped =
                patterns.stream()
                        .map(
                                pattern ->
                                        new PatternOrderItem(
                                                pattern.patternId(),
                                                pattern.shortcutNumber() == 1
                                                        ? 2
                                                        : pattern.shortcutNumber() == 2
                                                                ? 1
                                                                : pattern.shortcutNumber()))
                        .toList();
        var saved = patternService.reorderPatterns(1L, new PatternOrderRequest(swapped));

        assertEquals(1, saved.get(0).shortcutNumber());
        assertEquals("연금 입금 확인", saved.get(0).title());
        assertEquals(2, saved.get(1).shortcutNumber());
        assertEquals("아들에게 송금", saved.get(1).title());
    }

    @Test
    void consentControlsExecutionAndReentryCreatesSeparateVisit() {
        userService.updateConsents(1L, new ConsentUpdateRequest(false, false));
        long patternId = patternService.getPatterns(1L).get(3).patternId();
        var declined =
                patternService.startExecution(1L, patternId, new ExecutionStartRequest(1L));
        assertFalse(declined.loggingEnabled());
        assertNull(declined.executionId());

        userService.updateConsents(1L, new ConsentUpdateRequest(true, false));
        var started =
                patternService.startExecution(1L, patternId, new ExecutionStartRequest(1L));
        assertTrue(started.loggingEnabled());
        assertNotNull(started.executionId());

        long stepId = started.pattern().steps().get(0).stepId();
        var first =
                patternService.startVisit(
                        1L, started.executionId(), new VisitStartRequest(stepId));
        var closed =
                patternService.updateVisit(
                        1L,
                        started.executionId(),
                        first.visitId(),
                        new VisitUpdateRequest(1, 1, 2, true, true));
        var second =
                patternService.startVisit(
                        1L, started.executionId(), new VisitStartRequest(stepId));

        assertEquals(1, first.visitNumber());
        assertEquals(2, second.visitNumber());
        assertEquals(2, closed.wrongTouchCount());
        assertTrue(closed.routeDeviation());
        assertNotNull(closed.durationSeconds());

        var finished =
                patternService.finishExecution(
                        1L,
                        started.executionId(),
                        new ExecutionFinishRequest(ExecutionStatus.CANCELLED));
        assertEquals(ExecutionStatus.CANCELLED, finished.status());
        assertNotNull(finished.endedAt());
        assertEquals(
                "CANCELLED",
                jdbcTemplate.queryForObject(
                        "SELECT status FROM pattern_executions WHERE pattern_execution_id = ?",
                        String.class,
                        started.executionId()));
    }
}
