package com.bestfriend.danjjak.analysis;

import static org.junit.jupiter.api.Assertions.*;

import com.bestfriend.danjjak.analysis.dto.InstructionSuggestionDtos.ApplySuggestionRequest;
import com.bestfriend.danjjak.analysis.dto.UsageAnalysisDtos.AnalysisStatus;
import com.bestfriend.danjjak.analysis.service.InstructionSuggestionService;
import com.bestfriend.danjjak.account.service.AccountService;
import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.config.RootConfig;
import com.bestfriend.danjjak.pattern.dto.GuidanceDtos.GuidanceUpdateRequest;
import com.bestfriend.danjjak.pattern.dto.GuidanceDtos.VoiceMode;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.*;
import com.bestfriend.danjjak.pattern.service.GuidanceService;
import com.bestfriend.danjjak.pattern.service.PatternService;
import com.bestfriend.danjjak.user.dto.UserDtos.ConsentUpdateRequest;
import com.bestfriend.danjjak.user.service.UserService;
import java.time.LocalDate;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@EnabledIfEnvironmentVariable(named = "DANJJAK_DB_INTEGRATION_TEST", matches = "true")
@Transactional
class InstructionSuggestionIntegrationTest {
    @Autowired PatternService patterns;
    @Autowired AccountService accounts;
    @Autowired GuidanceService guidance;
    @Autowired InstructionSuggestionService suggestions;
    @Autowired UserService users;
    @Autowired DataSource dataSource;

    @Test
    void recordedVisitToAnalysisToSuggestionToNextExecutionPreservesFamilyAudio() throws Exception {
        accounts.importMockAccount(1, 1);
        users.updateConsents(1, new ConsentUpdateRequest(true, false));
        var pattern = patterns.getPattern(1, patterns.getPatterns(1).get(0).patternId());
        var step = pattern.steps().get(0);
        guidance.update(1, pattern.patternId(), step.stepCode(), new GuidanceUpdateRequest(step.instructionText(), VoiceMode.FAMILY));
        byte[] original = {1, 2, 3, 4};
        guidance.upload(1, pattern.patternId(), step.stepCode(), new MockMultipartFile("file", "family.webm", "audio/webm", original));
        var started = patterns.startExecution(1, pattern.patternId(), new ExecutionStartRequest(1L));
        var visit = patterns.startVisit(1, started.executionId(), new VisitStartRequest(step.stepId()));
        patterns.updateVisit(1, started.executionId(), visit.visitId(), new VisitUpdateRequest(1, 2, 3, true, true));
        patterns.finishExecution(1, started.executionId(), new ExecutionFinishRequest(ExecutionStatus.CANCELLED));
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        long countBefore = jdbc.queryForObject("SELECT COUNT(*) FROM pattern_executions", Long.class);
        long visitsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM step_execution_logs", Long.class);

        LocalDate today = LocalDate.now();
        var suggestion = suggestions.get(1, today.minusDays(1), today.plusDays(1)).suggestion();
        assertNotNull(suggestion);
        assertEquals(step.stepId(), suggestion.stepId());
        assertEquals(step.instructionText(), suggestion.currentText());
        assertTrue(suggestion.hasFamilyAudio());
        assertEquals(step.instructionText(), patterns.getPattern(1, pattern.patternId()).steps().get(0).instructionText());

        var saved = suggestions.apply(1, pattern.patternId(), step.stepId(),
                new ApplySuggestionRequest(suggestion.currentText(), suggestion.suggestedText()));
        assertEquals(suggestion.suggestedText(), saved.text());
        assertEquals("FAMILY", saved.voiceMode());
        assertTrue(saved.voiceScriptOutdated());
        try (var audio = guidance.audio(1, pattern.patternId(), step.stepCode()).resource().getInputStream()) {
            assertArrayEquals(original, audio.readAllBytes());
        }
        assertEquals(countBefore, jdbc.queryForObject("SELECT COUNT(*) FROM pattern_executions", Long.class));
        assertEquals(visitsBefore, jdbc.queryForObject("SELECT COUNT(*) FROM step_execution_logs", Long.class));
        var next = patterns.startExecution(1, pattern.patternId(), new ExecutionStartRequest(1L));
        assertEquals(saved.text(), next.pattern().steps().get(0).instructionText());
        assertEquals(pattern.description(), next.pattern().description());
        assertEquals(pattern.steps().get(1).instructionText(), next.pattern().steps().get(1).instructionText());

        var replaced = guidance.upload(1, pattern.patternId(), step.stepCode(), new MockMultipartFile("file", "new.webm", "audio/webm", new byte[] {5, 6}));
        assertFalse(replaced.voiceScriptOutdated());
    }

    @Test
    void changedComparisonAndInvalidStepDoNotOverwriteCurrentText() {
        var pattern = patterns.getPattern(1, patterns.getPatterns(1).get(0).patternId());
        var step = pattern.steps().get(0);
        var error = assertThrows(ApiException.class, () -> suggestions.apply(1, pattern.patternId(), step.stepId(),
                new ApplySuggestionRequest("이전 문구", "돈을 보낼 내 통장을 눌러 주세요.")));
        assertEquals("INSTRUCTION_CHANGED", error.getCode());
        assertThrows(ApiException.class, () -> suggestions.apply(1, pattern.patternId(), Long.MAX_VALUE,
                new ApplySuggestionRequest(step.instructionText(), "돈을 보낼 내 통장을 눌러 주세요.")));
        assertEquals(step.instructionText(), patterns.getPattern(1, pattern.patternId()).steps().get(0).instructionText());
    }

    @Test
    void consentAndNoDataProduceNoInventedSuggestion() {
        LocalDate today = LocalDate.now();
        users.updateConsents(1, new ConsentUpdateRequest(false, false));
        var declined = suggestions.get(1, today, today);
        assertEquals(AnalysisStatus.CONSENT_DECLINED, declined.status());
        assertNull(declined.suggestion());
        users.updateConsents(1, new ConsentUpdateRequest(true, false));
        var empty = suggestions.get(1, today, today);
        assertEquals(AnalysisStatus.NO_DATA, empty.status());
        assertNull(empty.suggestion());
    }
}
