package com.bestfriend.danjjak.support.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bestfriend.danjjak.common.error.GlobalExceptionHandler;
import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
import com.bestfriend.danjjak.support.dto.SupportDtos.GuardianContactResponse;
import com.bestfriend.danjjak.support.dto.SupportDtos.NotificationResponse;
import com.bestfriend.danjjak.support.dto.SupportDtos.SupportResponse;
import com.bestfriend.danjjak.support.service.GuardianNotificationService;
import com.bestfriend.danjjak.support.service.SupportService;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

class SupportControllerTest {

    private SupportService supportService;
    private GuardianNotificationService notificationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        supportService = mock(SupportService.class);
        notificationService = mock(GuardianNotificationService.class);
        DemoSessionUserResolver userResolver = mock(DemoSessionUserResolver.class);
        when(userResolver.resolveUserId(any(HttpSession.class))).thenReturn(1L);
        when(userResolver.resolveKakaoAccessToken(any(HttpSession.class)))
                .thenReturn("access-token");
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new SupportController(
                                        supportService, notificationService, userResolver))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .addFilters(
                                new CharacterEncodingFilter(
                                        StandardCharsets.UTF_8.name(), true))
                        .build();
    }

    @Test
    void returnsGuardianAndCustomerCenterPhone() throws Exception {
        when(supportService.getSupport(1L))
                .thenReturn(
                        new SupportResponse(
                                new GuardianContactResponse(10L, "010-1234-5678"),
                                "1588-0000"));

        mockMvc.perform(get("/api/support"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("010-1234-5678")))
                .andExpect(content().string(containsString("1588-0000")));
    }

    @Test
    void updatesGuardianContact() throws Exception {
        when(supportService.updateGuardian(org.mockito.ArgumentMatchers.eq(1L), any()))
                .thenReturn(new GuardianContactResponse(10L, "010-9999-0000"));

        mockMvc.perform(
                        put("/api/support/guardian")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"phoneNumber\":\"010-9999-0000\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("010-9999-0000")));
    }

    @Test
    void sendsSelectedHighAnomalyNotification() throws Exception {
        when(notificationService.notify(1L, 40L, "access-token"))
                .thenReturn(
                        new NotificationResponse(
                                40L, "ACTUAL", "SENT", true, true, "카카오 알림 전송 완료"));

        mockMvc.perform(post("/api/anomaly-events/40/guardian-notification"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"deliveryMode\":\"ACTUAL\"")))
                .andExpect(content().string(containsString("\"result\":\"SENT\"")));
    }
}
