package com.bestfriend.danjjak.pattern;

import static org.junit.jupiter.api.Assertions.*;

import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.config.RootConfig;
import com.bestfriend.danjjak.pattern.dto.GuidanceDtos.GuidanceUpdateRequest;
import com.bestfriend.danjjak.pattern.dto.GuidanceDtos.VoiceMode;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternUpdateRequest;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.StepInstructionRequest;
import com.bestfriend.danjjak.pattern.mapper.GuidanceMapper;
import com.bestfriend.danjjak.pattern.service.GuidanceService;
import com.bestfriend.danjjak.pattern.service.PatternService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@EnabledIfEnvironmentVariable(named = "DANJJAK_DB_INTEGRATION_TEST", matches = "true")
@Transactional
class GuidanceDatabaseIntegrationTest {
    @Autowired GuidanceService guidance;
    @Autowired GuidanceMapper mapper;
    @Autowired PatternService patterns;

    @Test
    void startAndStepSelectionsPersistIndependentlyAndTextChangeMarksRecording() {
        var pattern = patterns.getPattern(1, patterns.getPatterns(1).get(0).patternId());
        long id = pattern.patternId();
        String step = pattern.steps().get(0).stepCode();
        assertEquals(7, guidance.getAll(1, id).size());
        assertNull(guidance.getAll(1, id).get(0).voiceMode());
        mapper.updateAudio(1, id, "start", "start-file", "audio/webm");
        mapper.updateAudio(1, id, step, "step-file", "audio/ogg");

        var start = guidance.update(1, id, "start", new GuidanceUpdateRequest(pattern.description(), VoiceMode.FAMILY));
        assertFalse(start.voiceScriptOutdated());
        var changed = guidance.update(1, id, step, new GuidanceUpdateRequest("다음 버튼을 눌러 주세요.", VoiceMode.TTS));
        assertTrue(changed.voiceScriptOutdated());
        assertEquals("TTS", changed.voiceMode());
        assertNotNull(changed.audioUrl());
        assertEquals("FAMILY", guidance.getAll(1, id).get(0).voiceMode());

        mapper.updateAudio(1, id, step, "replacement", "audio/mp4");
        assertFalse(guidance.getAll(1, id).stream().filter(g -> g.target().equals(step)).findFirst().orElseThrow().voiceScriptOutdated());
        assertThrows(ApiException.class, () -> guidance.getAll(999, id));
    }

    @Test
    void normalPatternEditorAlsoKeepsAudioAndMarksChangedScript() {
        var pattern = patterns.getPattern(1, patterns.getPatterns(1).get(0).patternId());
        long id = pattern.patternId();
        String step = pattern.steps().get(0).stepCode();
        mapper.updateAudio(1, id, "start", "start-file", "audio/webm");
        mapper.updateAudio(1, id, step, "step-file", "audio/webm");
        patterns.updatePattern(1, id, new PatternUpdateRequest(null, "새 시작 안내입니다.", null,
                List.of(new StepInstructionRequest(step, "새 단계 안내입니다."))));
        var saved = guidance.getAll(1, id);
        assertTrue(saved.get(0).voiceScriptOutdated());
        assertTrue(saved.stream().filter(g -> g.target().equals(step)).findFirst().orElseThrow().voiceScriptOutdated());
        assertNotNull(saved.get(0).audioUrl());
    }
}
