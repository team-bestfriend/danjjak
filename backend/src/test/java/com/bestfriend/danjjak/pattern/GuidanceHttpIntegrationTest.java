package com.bestfriend.danjjak.pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bestfriend.danjjak.common.error.GlobalExceptionHandler;
import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
import com.bestfriend.danjjak.config.RootConfig;
import com.bestfriend.danjjak.pattern.controller.GuidanceController;
import com.bestfriend.danjjak.pattern.service.GuidanceService;
import com.bestfriend.danjjak.pattern.service.PatternService;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RootConfig.class)
@EnabledIfEnvironmentVariable(named = "DANJJAK_DB_INTEGRATION_TEST", matches = "true")
@Transactional
class GuidanceHttpIntegrationTest {
    @Autowired GuidanceService guidance;
    @Autowired PatternService patterns;
    @Autowired DataSource dataSource;

    @Test
    void authenticatedMultipartRoundTripDoesNotCreateExecutionLogs() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new GuidanceController(guidance, new DemoSessionUserResolver()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        long id = patterns.getPatterns(1).get(0).patternId();
        String path = "/api/patterns/" + id + "/guidance";
        var session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM pattern_executions", Long.class);
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(put(path + "/start").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Start guidance\",\"voiceMode\":\"FAMILY\"}"))
                .andExpect(status().isOk());
        byte[] bytes = {1, 2, 3, 4};
        mvc.perform(multipart(path + "/start/audio").file(new MockMultipartFile("file", "recording.webm", "audio/webm", bytes)).session(session))
                .andExpect(status().isOk());
        mvc.perform(get(path + "/start/audio").session(session))
                .andExpect(status().isOk()).andExpect(content().contentType("audio/webm"))
                .andExpect(header().string("Cache-Control", "no-store")).andExpect(content().bytes(bytes));
        mvc.perform(multipart(path + "/start/audio").file(new MockMultipartFile("file", "bad.html", "text/html", bytes)).session(session))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(get(path + "/start/audio").session(session)).andExpect(content().bytes(bytes));
        var other = new MockHttpSession();
        other.setAttribute("userId", 999L);
        mvc.perform(get(path + "/start/audio").session(other)).andExpect(status().isNotFound());
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM pattern_executions", Long.class));
    }
}
