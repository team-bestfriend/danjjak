package com.bestfriend.danjjak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bestfriend.danjjak.config.RootConfig;
import com.bestfriend.danjjak.config.WebConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextHierarchy({
    @ContextConfiguration(name = "root", classes = RootConfig.class),
    @ContextConfiguration(name = "web", classes = WebConfig.class)
})
@EnabledIfEnvironmentVariable(named = "DANJJAK_DB_INTEGRATION_TEST", matches = "true")
@Transactional
class TransferFdsApiIntegrationTest {

    @Autowired private WebApplicationContext context;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.webAppContextSetup(context)
                        .addFilters(
                                new CharacterEncodingFilter(
                                        StandardCharsets.UTF_8.name(), true))
                        .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void reproducesHighRiskNotificationAndContinueFlowThroughHttpApi() throws Exception {
        JsonNode accounts = performAndRead(get("/api/accounts"));
        assertEquals(2, accounts.size());
        assertTrue(accounts.get(0).path("primary").asBoolean());

        JsonNode people = performAndRead(get("/api/registered-persons"));
        assertEquals(2, people.size());

        assertEquals("COMPLETED", performRegisteredTransfer("1000").path("status").asText());
        assertEquals("COMPLETED", performRegisteredTransfer("1000").path("status").asText());

        JsonNode high = performRegisteredTransfer("10000000");
        assertEquals("REQUIRES_REVIEW", high.path("status").asText());
        assertEquals("HIGH", high.path("riskLevel").asText());
        assertEquals(2, high.path("recentTransferCount").asInt());
        long anomalyEventId = high.path("anomalyEventId").asLong();

        JsonNode notification =
                performAndRead(
                        post(
                                "/api/anomaly-events/{anomalyEventId}/guardian-notification",
                                anomalyEventId));
        assertEquals("MOCK", notification.path("deliveryMode").asText());
        assertEquals("MOCKED_NO_TOKEN", notification.path("result").asText());

        JsonNode resolution =
                performAndRead(
                        post("/api/anomaly-events/{anomalyEventId}/resolve", anomalyEventId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"action\":\"CONTINUE\",\"rechecked\":true}"));
        assertEquals("CONTINUE", resolution.path("action").asText());
        assertTrue(resolution.path("transactionId").asLong() > 0);

        JsonNode transfers =
                performAndRead(
                        get("/api/accounts/1/transactions").param("category", "TRANSFER"));
        assertEquals(3, transfers.size());
    }

    private JsonNode performRegisteredTransfer(String amount) throws Exception {
        return performAndRead(
                post("/api/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"sourceAccountId\":1,\"registeredRecipientAccountId\":3,"
                                        + "\"amount\":"
                                        + amount
                                        + ",\"pin\":\"1234\"}"));
    }

    private JsonNode performAndRead(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
