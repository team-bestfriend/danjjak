package com.bestfriend.danjjak.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bestfriend.danjjak.chat.dto.ChatDtos.Action;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternSummaryResponse;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternType;
import com.bestfriend.danjjak.pattern.service.PatternService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatServiceTest {
    private final PatternService patterns = mock(PatternService.class);

    @Test
    void transferUsesExistingFlowWithoutGuessingRecipient() {
        ChatService service = new ChatService(message -> {
            assertEquals("아들에게 돈 보내고 싶어", message);
            return "TRANSFER";
        }, patterns);
        var reply = service.reply(7, "아들에게 돈 보내고 싶어");
        assertEquals(Action.TRANSFER, reply.action());
        assertNull(reply.patternId());
        assertFalse(reply.retryable());
        verifyNoInteractions(patterns);
    }

    @Test
    void balanceUsesOwnedPatternInsteadOfShortcutNumber() {
        when(patterns.getPatterns(7)).thenReturn(List.of(
                new PatternSummaryResponse(42, 2, PatternType.BALANCE_CHECK, "잔액", "", null)));
        var reply = new ChatService(message -> "BALANCE_CHECK", patterns).reply(7, "잔액 확인");
        assertEquals(Action.BALANCE_CHECK, reply.action());
        assertEquals(42L, reply.patternId());
        verify(patterns).getPatterns(7);
        verifyNoMoreInteractions(patterns);
    }

    @Test
    void missingPatternStillAllowsExistingInquiryScreen() {
        when(patterns.getPatterns(7)).thenReturn(List.of());
        var reply = new ChatService(message -> "PENSION_CHECK", patterns).reply(7, "연금 들어왔어?");
        assertEquals(Action.PENSION_CHECK, reply.action());
        assertNull(reply.patternId());
    }

    @Test
    void unsupportedQuestionReturnsNoneWithoutAdvice() {
        var reply = new ChatService(message -> "NONE", patterns).reply(7, "주식 추천해 줘");
        assertEquals(Action.NONE, reply.action());
        assertFalse(reply.retryable());
        verifyNoInteractions(patterns);
    }

    @Test
    void providerFailureAndUnknownActionsReturnFallback() {
        for (ChatIntentClient client : List.<ChatIntentClient>of(
                message -> { throw new IllegalStateException("timeout"); },
                message -> "BUY_STOCK", message -> null, message -> "/transfer/confirm")) {
            var reply = new ChatService(client, patterns).reply(7, "도와줘");
            assertEquals(Action.NONE, reply.action());
            assertTrue(reply.retryable());
            assertNull(reply.patternId());
            assertTrue(reply.message().contains("아래 메뉴"));
        }
    }

    @Test
    void sensitiveInputNeverReachesProvider() {
        ChatIntentClient client = mock(ChatIntentClient.class);
        for (String message : List.of("비밀번호 알려줄게", "1234", "계좌번호 123-456-789", "PIN 입력할래")) {
            var reply = new ChatService(client, patterns).reply(7, message);
            assertEquals(Action.NONE, reply.action());
            assertTrue(reply.message().contains("채팅에 쓰지 마세요"));
        }
        verifyNoInteractions(client, patterns);
    }
}
