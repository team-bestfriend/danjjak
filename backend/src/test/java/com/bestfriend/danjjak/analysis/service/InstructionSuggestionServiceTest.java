package com.bestfriend.danjjak.analysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bestfriend.danjjak.analysis.dto.InstructionSuggestionDtos.ApplySuggestionRequest;
import com.bestfriend.danjjak.analysis.dto.UsageAnalysisDtos.AnalysisStatus;
import com.bestfriend.danjjak.analysis.dto.UsageAnalysisDtos.StepAnalysisResponse;
import com.bestfriend.danjjak.analysis.dto.UsageAnalysisDtos.UsageAnalysisResponse;
import com.bestfriend.danjjak.pattern.dto.GuidanceDtos.GuidanceResponse;
import com.bestfriend.danjjak.pattern.dto.GuidanceDtos.GuidanceUpdateRequest;
import com.bestfriend.danjjak.pattern.dto.GuidanceDtos.VoiceMode;
import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternDetailResponse;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternStepResponse;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternType;
import com.bestfriend.danjjak.pattern.service.GuidanceService;
import com.bestfriend.danjjak.pattern.service.PatternService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InstructionSuggestionServiceTest {
    private static final LocalDate FROM = LocalDate.of(2026, 9, 1);
    private static final LocalDate TO = LocalDate.of(2026, 9, 7);
    private static final String CURRENT = "돈을 받을 가족이 누구인지 이름을 골라서 눌러 주세요.";
    private static final String FALLBACK = "돈을 받을 가족의 이름을 눌러 주세요.";

    private UsageAnalysisService analysis;
    private PatternService patterns;
    private GuidanceService guidance;
    private InstructionTextSuggestionClient client;
    private InstructionSuggestionService service;

    @BeforeEach
    void setUp() {
        analysis = mock(UsageAnalysisService.class);
        patterns = mock(PatternService.class);
        guidance = mock(GuidanceService.class);
        client = mock(InstructionTextSuggestionClient.class);
        service = new InstructionSuggestionService(analysis, patterns, guidance, client);

        var difficult = new StepAnalysisResponse(3, 20, "SELECT_PERSON", "받는 사람 선택", 2,
                6, 10, 4, 2, 3, 1, new BigDecimal("12.5"));
        when(analysis.getUsageAnalysis(7, FROM, TO)).thenReturn(new UsageAnalysisResponse(
                AnalysisStatus.AVAILABLE, FROM, TO, List.of(), List.of(difficult), difficult));
        var step = new PatternStepResponse(20, 2, "SELECT_PERSON", "받는 사람 선택", CURRENT,
                "PERSON", "person-list", null, null);
        when(patterns.getPattern(7, 3)).thenReturn(new PatternDetailResponse(
                3, 1, PatternType.TRANSFER, "아들에게 송금", "설명", null, List.of(step)));
        when(guidance.getAll(7, 3)).thenReturn(List.of(
                new GuidanceResponse("SELECT_PERSON", CURRENT, "TTS", null, null, false)));
    }

    @Test
    void returnsAiSuggestion() {
        when(client.suggest(any())).thenReturn("받는 사람 이름을 눌러 주세요.");

        assertEquals("받는 사람 이름을 눌러 주세요.", service.get(7, FROM, TO).suggestion().suggestedText());
    }

    @Test
    void fallsBackWhenAiRepeatsCurrentText() {
        when(client.suggest(any())).thenReturn("  " + CURRENT + "  ");
        assertEquals(FALLBACK, service.get(7, FROM, TO).suggestion().suggestedText());
    }

    @Test
    void fallsBackWhenAiReturnsBlank() {
        when(client.suggest(any())).thenReturn("  ");
        assertEquals(FALLBACK, service.get(7, FROM, TO).suggestion().suggestedText());
    }

    @Test
    void fallsBackWhenAiFails() {
        when(client.suggest(any())).thenThrow(new IllegalStateException("timeout"));
        assertEquals(FALLBACK, service.get(7, FROM, TO).suggestion().suggestedText());
    }

    @Test
    void fallsBackWhenAiResponseIsTooLong() {
        when(client.suggest(any())).thenReturn("받는 사람 이름을 눌러 주세요" + "아".repeat(61));
        assertEquals(FALLBACK, service.get(7, FROM, TO).suggestion().suggestedText());
    }

    @Test
    void keepsEmptyStateWhenThereIsNoDifficultStep() {
        when(analysis.getUsageAnalysis(7, FROM, TO)).thenReturn(new UsageAnalysisResponse(
                AnalysisStatus.NO_DATA, FROM, TO, List.of(), List.of(), null));

        assertNull(service.get(7, FROM, TO).suggestion());
        verifyNoInteractions(client);
    }

    @Test
    void appliesTheSuggestionThatTheUserCompared() {
        String suggested = "받는 사람 이름을 눌러 주세요.";
        var saved = new GuidanceResponse("SELECT_PERSON", suggested, "TTS", null, null, false);
        when(guidance.update(7, 3, "SELECT_PERSON", new GuidanceUpdateRequest(suggested, VoiceMode.TTS)))
                .thenReturn(saved);

        assertEquals(saved, service.apply(7, 3, 20, new ApplySuggestionRequest(CURRENT, suggested)));
        verifyNoInteractions(client);
    }

    @Test
    void staleCurrentTextStillPreventsApply() {
        ApiException exception = assertThrows(ApiException.class,
                () -> service.apply(7, 3, 20, new ApplySuggestionRequest("이전 문구", "받는 사람 이름을 눌러 주세요.")));

        assertEquals("INSTRUCTION_CHANGED", exception.getCode());
        verifyNoInteractions(client);
    }
}
