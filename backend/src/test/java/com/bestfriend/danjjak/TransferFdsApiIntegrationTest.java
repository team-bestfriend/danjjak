package com.bestfriend.danjjak;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bestfriend.danjjak.config.RootConfig;
import com.bestfriend.danjjak.config.WebConfig;
import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
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
import org.springframework.mock.web.MockHttpSession;
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
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.webAppContextSetup(context)
                        .addFilters(
                                new CharacterEncodingFilter(
                                        StandardCharsets.UTF_8.name(), true))
                        .build();
        objectMapper = new ObjectMapper();
        session = new MockHttpSession();
        session.setAttribute(DemoSessionUserResolver.USER_ID_ATTRIBUTE, 1L);
    }

    @Test
    void reproducesHighRiskNotificationAndContinueFlowThroughHttpApi() throws Exception {
        JsonNode consents =
                performAndRead(
                        put("/api/users/me/consents")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"usageLogAgreed\":true,"
                                                + "\"guardianShareAgreed\":true}"));
        assertTrue(consents.path("guardianShareAgreed").asBoolean());

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
        assertTrue(transfers.get(0).path("transactionAt").isTextual());
    }

    @Test
    void reproducesDirectTransferAndAtomicErrorRecoveryThroughHttpApi() throws Exception {
        JsonNode peopleBefore = performAndRead(get("/api/registered-persons"));
        JsonNode balanceBefore = performAndRead(get("/api/accounts/1/balance"));
        JsonNode transfersBefore =
                performAndRead(
                        get("/api/accounts/1/transactions").param("category", "TRANSFER"));

        JsonNode completed =
                performAndRead(
                        post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"sourceAccountId\":1,"
                                                + "\"directRecipient\":{"
                                                + "\"name\":\"박친구\","
                                                + "\"bankCode\":\"003\","
                                                + "\"bankName\":\"기업은행\","
                                                + "\"accountNumber\":\"000-000-000003\"},"
                                                + "\"amount\":50000,\"pin\":\"1234\"}"));

        assertEquals("COMPLETED", completed.path("status").asText());
        assertTrue(completed.path("transactionId").asLong() > 0);
        assertEquals(
                balanceBefore.path("balance").asLong() - 50000,
                completed.path("balanceAfter").asLong());

        JsonNode balanceAfter = performAndRead(get("/api/accounts/1/balance"));
        JsonNode transfersAfter =
                performAndRead(
                        get("/api/accounts/1/transactions").param("category", "TRANSFER"));
        JsonNode peopleAfter = performAndRead(get("/api/registered-persons"));
        assertEquals(completed.path("balanceAfter").asLong(), balanceAfter.path("balance").asLong());
        assertEquals(transfersBefore.size() + 1, transfersAfter.size());
        assertEquals("박친구", transfersAfter.get(0).path("counterpartyName").asText());
        assertEquals(peopleBefore.size(), peopleAfter.size());

        JsonNode wrongPin =
                performAndReadError(
                        post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"sourceAccountId\":1,"
                                                + "\"registeredRecipientAccountId\":3,"
                                                + "\"amount\":1000,\"pin\":\"0000\"}"));
        assertEquals("PIN_MISMATCH", wrongPin.path("code").asText());

        long insufficientAmount = balanceAfter.path("balance").asLong() + 1;
        JsonNode insufficient =
                performAndReadError(
                        post("/api/transfers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"sourceAccountId\":1,"
                                                + "\"registeredRecipientAccountId\":3,"
                                                + "\"amount\":"
                                                + insufficientAmount
                                                + ",\"pin\":\"1234\"}"));
        assertEquals("INSUFFICIENT_BALANCE", insufficient.path("code").asText());

        JsonNode finalBalance = performAndRead(get("/api/accounts/1/balance"));
        JsonNode finalTransfers =
                performAndRead(
                        get("/api/accounts/1/transactions").param("category", "TRANSFER"));
        assertEquals(balanceAfter.path("balance").asLong(), finalBalance.path("balance").asLong());
        assertEquals(transfersAfter.size(), finalTransfers.size());
    }

    @Test
    void reproducesInquiryGuardianAndMediumCancellationThroughHttpApi() throws Exception {
        JsonNode supportBefore = performAndRead(get("/api/support"));
        assertEquals("1588-0000", supportBefore.path("customerCenterPhone").asText());
        assertTrue(supportBefore.path("guardian").path("phoneNumber").isTextual());

        JsonNode guardian =
                performAndRead(
                        put("/api/support/guardian")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"phoneNumber\":\"010-1111-2222\"}"));
        JsonNode supportAfter = performAndRead(get("/api/support"));
        assertEquals("010-1111-2222", guardian.path("phoneNumber").asText());
        assertEquals(
                guardian.path("phoneNumber").asText(),
                supportAfter.path("guardian").path("phoneNumber").asText());

        JsonNode pension =
                performAndRead(
                        get("/api/accounts/1/transactions").param("category", "PENSION"));
        JsonNode managementFee =
                performAndRead(
                        get("/api/accounts/1/transactions")
                                .param("category", "MANAGEMENT_FEE"));
        JsonNode utility =
                performAndRead(
                        get("/api/accounts/1/transactions")
                                .param("category", "UTILITY_BILL"));
        assertTrue(pension.size() > 0);
        assertTrue(managementFee.size() > 0);
        assertTrue(utility.size() > 0);
        assertTrue(pension.get(0).path("transactionType").isTextual());
        assertTrue(pension.get(0).path("amount").isNumber());
        assertTrue(pension.get(0).path("transactionAt").isTextual());
        assertTrue(pension.get(0).path("balanceAfter").isNumber());

        JsonNode balanceBefore = performAndRead(get("/api/accounts/1/balance"));
        JsonNode transfersBefore =
                performAndRead(
                        get("/api/accounts/1/transactions").param("category", "TRANSFER"));
        JsonNode medium = performRegisteredTransfer("10000000");
        JsonNode repeatedAttempt = performRegisteredTransfer("10000000");
        assertEquals("REQUIRES_REVIEW", medium.path("status").asText());
        assertEquals("MEDIUM", medium.path("riskLevel").asText());
        assertEquals("HIGH_AMOUNT", medium.path("reasons").get(0).asText());
        assertEquals(
                medium.path("anomalyEventId").asLong(),
                repeatedAttempt.path("anomalyEventId").asLong());

        long anomalyEventId = medium.path("anomalyEventId").asLong();
        JsonNode cancelled =
                performAndRead(
                        post("/api/anomaly-events/{anomalyEventId}/resolve", anomalyEventId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"action\":\"CANCEL\",\"rechecked\":true}"));
        JsonNode repeatedDecision =
                performAndRead(
                        post("/api/anomaly-events/{anomalyEventId}/resolve", anomalyEventId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"action\":\"CONTINUE\",\"rechecked\":false}"));
        assertEquals("CANCEL", cancelled.path("action").asText());
        assertEquals("CANCEL", repeatedDecision.path("action").asText());
        assertTrue(cancelled.path("transactionId").isNull());

        JsonNode finalBalance = performAndRead(get("/api/accounts/1/balance"));
        JsonNode finalTransfers =
                performAndRead(
                        get("/api/accounts/1/transactions").param("category", "TRANSFER"));
        assertEquals(balanceBefore.path("balance").asLong(), finalBalance.path("balance").asLong());
        assertEquals(transfersBefore.size(), finalTransfers.size());
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
        MvcResult result =
                mockMvc.perform(request.session(session)).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode performAndReadError(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result =
                mockMvc.perform(request.session(session))
                        .andExpect(status().isUnprocessableEntity())
                        .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
