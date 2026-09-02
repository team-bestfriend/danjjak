package com.bestfriend.danjjak.transfer.controller;

import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.TransferRequest;
import com.bestfriend.danjjak.transfer.dto.TransferDtos.TransferResponse;
import com.bestfriend.danjjak.transfer.service.TransferService;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TransferController {

    private final TransferService transferService;
    private final DemoSessionUserResolver userResolver;

    public TransferController(
            TransferService transferService, DemoSessionUserResolver userResolver) {
        this.transferService = transferService;
        this.userResolver = userResolver;
    }

    @PostMapping("/transfers")
    public TransferResponse attempt(
            @Valid @RequestBody TransferRequest request, HttpSession session) {
        return transferService.transfer(userResolver.resolveUserId(session), request);
    }
}
