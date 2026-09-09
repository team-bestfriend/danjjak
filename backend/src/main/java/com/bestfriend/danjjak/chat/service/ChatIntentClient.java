package com.bestfriend.danjjak.chat.service;

import com.bestfriend.danjjak.chat.dto.ChatDtos.Action;

public interface ChatIntentClient {
    ChatIntent classify(String message);

    record ChatIntent(String message, Action action, boolean showRecommendations) {}
}
