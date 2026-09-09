package com.bestfriend.danjjak.chat.controller;

import com.bestfriend.danjjak.chat.dto.ChatDtos.ChatRequest;
import com.bestfriend.danjjak.chat.dto.ChatDtos.ChatResponse;
import com.bestfriend.danjjak.chat.service.ChatService;
import com.bestfriend.danjjak.common.session.DemoSessionUserResolver;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private final ChatService chat;
    private final DemoSessionUserResolver users;

    public ChatController(ChatService chat, DemoSessionUserResolver users) {
        this.chat = chat;
        this.users = users;
    }

    @PostMapping("/api/chat/messages")
    public ChatResponse send(@Valid @RequestBody ChatRequest request, HttpSession session) {
        return chat.reply(users.resolveUserId(session), request.message());
    }
}
