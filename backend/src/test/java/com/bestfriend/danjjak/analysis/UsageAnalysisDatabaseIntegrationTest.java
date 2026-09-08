package com.bestfriend.danjjak.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bestfriend.danjjak.analysis.dto.UsageAnalysisDtos.AnalysisStatus;
import com.bestfriend.danjjak.analysis.dto.UsageAnalysisDtos.UsageAnalysisResponse;
import com.bestfriend.danjjak.analysis.service.UsageAnalysisService;
import com.bestfriend.danjjak.config.RootConfig;
import java.math.BigDecimal;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mybatis.spring.SqlSessionTemplate;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@EnabledIfEnvironmentVariable(named = "DANJJAK_DB_INTEGRATION_TEST", matches = "true")
@Transactional
@Rollback
class UsageAnalysisDatabaseIntegrationTest {

    @Autowired private UsageAnalysisService service;
    @Autowired private DataSource dataSource;
    @Autowired private SqlSessionFactory sqlSessionFactory;

    private JdbcTemplate jdbc;
    private long userId;
    private long patternId;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        // 기존 시드와 분리한 테스트 데이터를 만들고 테스트 트랜잭션 전체를 롤백한다.
        userId = user();
        patternId = pattern(userId);
    }

    @Test
    void countsCompletedExecutionsOnceAndIncludesBothDateBoundaries() {
        long first = execution(patternId, "COMPLETED", "2026-09-01 00:00:00");
        execution(patternId, "COMPLETED", "2026-09-30 23:59:59");
        execution(patternId, "COMPLETED", "2026-08-31 23:59:59");
        execution(patternId, "COMPLETED", "2026-10-01 00:00:00");
        execution(patternId, "CANCELLED", "2026-09-02 00:00:00");
        execution(patternId, "FAILED", "2026-09-02 00:00:00");
        execution(patternId, "STARTED", "2026-09-02 00:00:00");
        long secondPattern = pattern(userId);
        execution(secondPattern, "COMPLETED", "2026-09-03 00:00:00");
        long thirdPattern = pattern(userId);
        execution(thirdPattern, "CANCELLED", "2026-09-03 00:00:00");
        long stepId = step(patternId, 1);
        visit(first, stepId, 1, 0, 0, 0, false, 10);
        visit(first, stepId, 2, 0, 0, 0, false, 20);
        long otherUser = user();
        long otherPattern = pattern(otherUser);
        long otherExecution = execution(otherPattern, "COMPLETED", "2026-09-03 00:00:00");
        visit(otherExecution, step(otherPattern, 1), 1, 999, 999, 999, true, 999);

        var result = analyze();
        Map<Long, Long> counts = result.patterns().stream().collect(Collectors.toMap(
                row -> row.patternId(), row -> row.completedCount()));
        assertEquals(Map.of(patternId, 2L, secondPattern, 1L, thirdPattern, 0L), counts);
        assertEquals(1, result.steps().size());
        assertEquals(stepId, result.difficultStep().stepId());
        assertEquals(2, result.difficultStep().visitCount());
        assertEquals(0, result.difficultStep().errorScore());
    }

    @Test
    void sumsEveryActionAcrossVisitsAndExcludesOpenVisitFromAverage() {
        long executionId = execution(patternId, "FAILED", "2026-09-05 00:00:00");
        long stepId = step(patternId, 1);
        visit(executionId, stepId, 1, 1, 2, 3, true, 10);
        visit(executionId, stepId, 2, 2, 1, 0, true, 21);
        visit(executionId, stepId, 3, 4, 0, 0, false, null);
        var result = analyze();
        assertEquals(0, result.patterns().get(0).completedCount());
        assertEquals(15, result.difficultStep().errorScore());
        assertEquals(7, result.difficultStep().retryCount());
        assertEquals(3, result.difficultStep().backCount());
        assertEquals(3, result.difficultStep().wrongTouchCount());
        assertEquals(2, result.difficultStep().routeDeviationCount());
        assertEquals(3, result.difficultStep().visitCount());
        assertEquals(0, new BigDecimal("15.5").compareTo(result.difficultStep().averageDurationSeconds()));
    }

    @Test
    void deterministicRankingUsesScoreThenAverageThenOrder() {
        long executionId = execution(patternId, "CANCELLED", "2026-09-05 00:00:00");
        long earlier = step(patternId, 1);
        long later = step(patternId, 2);
        visit(executionId, earlier, 1, 5, 0, 0, false, 10);
        visit(executionId, later, 1, 4, 0, 0, false, 100);
        assertEquals(earlier, analyze().difficultStep().stepId());
        visit(executionId, later, 2, 1, 0, 0, false, 100);
        assertEquals(later, analyze().difficultStep().stepId());
        visit(executionId, earlier, 2, 0, 0, 0, false, 190);
        assertEquals(earlier, analyze().difficultStep().stepId());
    }

    @Test
    void openVisitsHaveNullAverageAndScoresUseLong() {
        long executionId = execution(patternId, "FAILED", "2026-09-05 00:00:00");
        long stepId = step(patternId, 1);
        visit(executionId, stepId, 1, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, true, null);
        var result = analyze();
        assertEquals(6442450942L, result.difficultStep().errorScore());
        assertNull(result.difficultStep().averageDurationSeconds());
    }

    @Test
    void noDataAndConsentStatesDoNotManufactureAnalysis() {
        assertEquals(AnalysisStatus.NO_DATA, analyze().status());
        assertNull(analyze().difficultStep());
        execution(patternId, "COMPLETED", "2026-09-05 00:00:00");
        assertEquals(AnalysisStatus.AVAILABLE, analyze().status());
        assertNull(analyze().difficultStep());
        jdbc.update("UPDATE users SET usage_log_agreed = FALSE WHERE user_id = ?", userId);
        assertEquals(AnalysisStatus.CONSENT_DECLINED, analyze().status());
        assertTrue(analyze().patterns().isEmpty());
        jdbc.update("UPDATE users SET consent_completed = FALSE WHERE user_id = ?", userId);
        assertEquals(AnalysisStatus.CONSENT_REQUIRED, analyze().status());
        assertNull(analyze().difficultStep());
    }

    @Test
    void rejectsCrossPatternVisitFromAggregateAndIgnoresOutOfPeriodExecution() {
        long executionId = execution(patternId, "CANCELLED", "2026-09-05 00:00:00");
        long foreignStep = step(pattern(user()), 1);
        visit(executionId, foreignStep, 1, 99, 0, 0, true, 100);
        long old = execution(patternId, "COMPLETED", "2026-08-31 23:59:59");
        visit(old, step(patternId, 1), 1, 99, 0, 0, true, 100);
        assertTrue(analyze().steps().isEmpty());
        assertNull(analyze().difficultStep());
    }

    @Test
    void completionMonthControlsBothCountsAndStepAnalysis() {
        long stepId = step(patternId, 1);
        long september = execution(patternId, "COMPLETED",
                "2026-08-31 23:59:00", "2026-09-01 00:03:00");
        long october = execution(patternId, "COMPLETED",
                "2026-09-30 23:59:00", "2026-10-01 00:03:00");
        visit(september, stepId, 1, 2, 0, 0, false, 10);
        visit(october, stepId, 1, 9, 0, 0, true, 20);

        var septemberResult = analyze();
        assertEquals(1, septemberResult.patterns().get(0).completedCount());
        assertEquals(1, septemberResult.difficultStep().visitCount());
        assertEquals(2, septemberResult.difficultStep().errorScore());
        assertEquals(0, new BigDecimal("10").compareTo(
                septemberResult.difficultStep().averageDurationSeconds()));

        var octoberResult = service.getUsageAnalysis(userId,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        assertEquals(1, octoberResult.patterns().get(0).completedCount());
        assertEquals(1, octoberResult.difficultStep().visitCount());
        assertEquals(10, octoberResult.difficultStep().errorScore());
        assertEquals(0, new BigDecimal("20").compareTo(
                octoberResult.difficultStep().averageDurationSeconds()));
    }

    @Test
    void unfinishedExecutionAndNextMonthCompletionAreNotSeptemberData() {
        long stepId = step(patternId, 1);
        long active = execution(patternId, "STARTED", "2026-09-05 00:00:00");
        long october = execution(patternId, "COMPLETED",
                "2026-09-30 23:59:00", "2026-10-01 00:03:00");
        visit(active, stepId, 1, 5, 0, 0, true, null);
        visit(october, stepId, 1, 9, 0, 0, true, 20);

        var result = analyze();
        assertEquals(AnalysisStatus.NO_DATA, result.status());
        assertTrue(result.patterns().isEmpty());
        assertTrue(result.steps().isEmpty());
        assertNull(result.difficultStep());
    }

    private UsageAnalysisResponse analyze() {
        // JdbcTemplate으로 바꾼 테스트 데이터가 MyBatis 세션 캐시에 가려지지 않게 한다.
        new SqlSessionTemplate(sqlSessionFactory).clearCache();
        return service.getUsageAnalysis(userId, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
    }

    private long user() {
        return insert("INSERT INTO users (name, consent_completed, usage_log_agreed) VALUES (?, TRUE, TRUE)", "분석 테스트");
    }

    private long pattern(long owner) {
        return insert("INSERT INTO financial_patterns (user_id, pattern_type, title, description, is_active)"
                + " VALUES (?, 'BALANCE_CHECK', '잔액 확인', '분석 테스트', FALSE)", owner);
    }

    private long execution(long pattern, String status, String startedAt) {
        return execution(pattern, status, startedAt, "STARTED".equals(status) ? null : startedAt);
    }

    private long execution(long pattern, String status, String startedAt, String endedAt) {
        return insert("INSERT INTO pattern_executions (financial_pattern_id, status, started_at, ended_at)"
                + " VALUES (?, ?, ?, ?)", pattern, status, startedAt, endedAt);
    }

    private long step(long pattern, int order) {
        return insert("INSERT INTO pattern_steps (financial_pattern_id, step_order, step_code, step_name,"
                + " instruction_text, screen_code) VALUES (?, ?, ?, '확인', '확인해 주세요', 'test')",
                pattern, order, "STEP_" + order);
    }

    private void visit(long execution, long step, int number, int retry, int back, int wrong,
            boolean deviation, Integer duration) {
        var start = LocalDate.of(2026, 9, 6).atStartOfDay();
        insert("INSERT INTO step_execution_logs (pattern_execution_id, pattern_step_id, visit_number,"
                + " retry_count, back_count, wrong_touch_count, route_deviation, started_at, ended_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", execution, step, number, retry, back, wrong,
                deviation, start, duration == null ? null : start.plusSeconds(duration));
    }

    private long insert(String sql, Object... values) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }
}
