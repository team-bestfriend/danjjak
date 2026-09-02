package com.bestfriend.danjjak.support.controller;

import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
import com.bestfriend.danjjak.support.dto.SupportDtos.GuardianContactRequest;
import com.bestfriend.danjjak.support.dto.SupportDtos.GuardianContactResponse;
import com.bestfriend.danjjak.support.dto.SupportDtos.NotificationResponse;
import com.bestfriend.danjjak.support.dto.SupportDtos.SupportResponse;
import com.bestfriend.danjjak.support.service.GuardianNotificationService;
import com.bestfriend.danjjak.support.service.SupportService;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SupportController {

    private final SupportService supportService;
    private final GuardianNotificationService notificationService;
    private final DemoSessionUserResolver userResolver;

    public SupportController(
            SupportService supportService,
            GuardianNotificationService notificationService,
            DemoSessionUserResolver userResolver) {
        this.supportService = supportService;
        this.notificationService = notificationService;
        this.userResolver = userResolver;
    }

    @GetMapping("/support")
    public SupportResponse getSupport(HttpSession session) {
        return supportService.getSupport(userResolver.resolveUserId(session));
    }

    @PutMapping("/support/guardian")
    public GuardianContactResponse updateGuardian(
            @Valid @RequestBody GuardianContactRequest request, HttpSession session) {
        return supportService.updateGuardian(userResolver.resolveUserId(session), request);
    }

    @PostMapping("/anomaly-events/{anomalyEventId}/guardian-notification")
    public NotificationResponse notifyGuardian(
            @PathVariable long anomalyEventId, HttpSession session) {
        return notificationService.notify(
                userResolver.resolveUserId(session),
                anomalyEventId,
                userResolver.resolveKakaoAccessToken(session));
    }
}
