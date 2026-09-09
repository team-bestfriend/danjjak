package com.bestfriend.danjjak.chat.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bestfriend.danjjak.chat.dto.ChatDtos.Action;
import com.bestfriend.danjjak.chat.service.ChatIntentClient.ChatIntent;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternSummaryResponse;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternType;
import com.bestfriend.danjjak.pattern.service.PatternService;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatServiceTest {
    private final PatternService patterns = mock(PatternService.class);

    @Test
    void shortcutCommandUsesCurrentActiveShortcutWithoutCallingProvider() {
        ChatIntentClient client = mock(ChatIntentClient.class);
        when(patterns.getPatterns(7)).thenReturn(List.of(
                new PatternSummaryResponse(91, 2, PatternType.TRANSFER, "딸에게 송금하기", "", null)));

        for (String message : List.of("2", "2번", "2번 실행해줘", "2 번 실행해 주세요")) {
            var reply = new ChatService(client, patterns).reply(7, message);
            assertEquals(Action.PATTERN, reply.action());
            assertEquals(91L, reply.patternId());
            assertTrue(reply.message().contains("2번 딸에게 송금하기"));
            assertFalse(reply.retryable());
            assertFalse(reply.showRecommendations());
        }
        verifyNoInteractions(client);
        verify(patterns, times(4)).getPatterns(7);
    }

    @Test
    void emptyShortcutDoesNotExecutePattern() {
        ChatIntentClient client = mock(ChatIntentClient.class);
        when(patterns.getPatterns(7)).thenReturn(List.of());

        var empty = new ChatService(client, patterns).reply(7, "12번 실행해줘");

        assertEquals(Action.NONE, empty.action());
        assertNull(empty.patternId());
        assertTrue(empty.message().contains("등록된 금융 업무가 없어요"));
        assertTrue(empty.showRecommendations());
        verifyNoInteractions(client);
    }

    @Test
    void transferUsesExistingFlowWithoutGuessingRecipient() {
        ChatService service = new ChatService(message -> {
            assertEquals("아들에게 돈 보내고 싶어", message);
            return intent("송금을 도와드릴게요.", Action.TRANSFER, false);
        }, patterns);
        var reply = service.reply(7, "아들에게 돈 보내고 싶어");
        assertEquals(Action.TRANSFER, reply.action());
        assertNull(reply.patternId());
        assertFalse(reply.retryable());
        assertFalse(reply.showRecommendations());
        verifyNoInteractions(patterns);
    }

    @Test
    void balanceUsesOwnedPatternInsteadOfShortcutNumber() {
        when(patterns.getPatterns(7)).thenReturn(List.of(
                new PatternSummaryResponse(42, 2, PatternType.BALANCE_CHECK, "잔액", "", null)));
        var reply = new ChatService(message -> intent("잔액 확인을 도와드릴게요.", Action.BALANCE_CHECK, false), patterns)
                .reply(7, "잔액 확인");
        assertEquals(Action.BALANCE_CHECK, reply.action());
        assertEquals(42L, reply.patternId());
        verify(patterns).getPatterns(7);
        verifyNoMoreInteractions(patterns);
    }

    @Test
    void missingPatternStillAllowsExistingInquiryScreen() {
        when(patterns.getPatterns(7)).thenReturn(List.of());
        var reply = new ChatService(message -> intent("연금 확인을 도와드릴게요.", Action.PENSION_CHECK, false), patterns)
                .reply(7, "연금 들어왔어?");
        assertEquals(Action.PENSION_CHECK, reply.action());
        assertNull(reply.patternId());
    }

    @Test
    void unsupportedQuestionReturnsNoneWithoutAdvice() {
        var reply = new ChatService(message -> intent("단짝에서 도와드릴 일을 골라 주세요.", Action.NONE, true), patterns)
                .reply(7, "주식 추천해 줘");
        assertEquals(Action.NONE, reply.action());
        assertFalse(reply.retryable());
        assertTrue(reply.showRecommendations());
        verifyNoInteractions(patterns);
    }

    @Test
    void providerFailureAndUnknownActionsReturnFallback() {
        for (ChatIntentClient client : List.<ChatIntentClient>of(
                message -> { throw new IllegalStateException("timeout"); },
                message -> { throw new IllegalArgumentException("unknown"); }, message -> null)) {
            var reply = new ChatService(client, patterns).reply(7, "도와줘");
            assertEquals(Action.NONE, reply.action());
            assertTrue(reply.retryable());
            assertNull(reply.patternId());
            assertFalse(reply.showRecommendations());
            assertTrue(reply.message().contains("다시 물어봐"));
        }
    }

    @Test
    void sensitiveInputNeverReachesProvider() {
        ChatIntentClient client = mock(ChatIntentClient.class);
        for (String message : List.of("비밀번호 알려줄게", "1234", "계좌번호 123-456-789", "PIN 입력할래")) {
            var reply = new ChatService(client, patterns).reply(7, message);
            assertEquals(Action.NONE, reply.action());
            assertFalse(reply.showRecommendations());
            assertTrue(reply.message().contains("채팅에 쓰지 마세요"));
        }
        verifyNoInteractions(client, patterns);
    }

    @Test
    void greetingReturnsNaturalReplyWithRecommendations() {
        ChatIntentClient client = message -> { throw new IllegalStateException("offline"); };
        for (String message : List.of("안녕", "안녕하세요", "ㅎㅇ", "반가워")) {
            var reply = new ChatService(client, patterns).reply(7, message);
            assertEquals("안녕하세요! 무엇을 도와드릴까요?", reply.message());
            assertEquals(Action.NONE, reply.action());
            assertTrue(reply.showRecommendations());
        }
        verifyNoInteractions(patterns);
    }

    @Test
    void shortDailyConversationDoesNotDependOnProvider() {
        ChatIntentClient client = message -> { throw new IllegalStateException("offline"); };
        var thanks = new ChatService(client, patterns).reply(7, "고마워");
        assertTrue(thanks.message().contains("별말씀"));
        assertTrue(thanks.showRecommendations());
        assertTrue(new ChatService(client, patterns).reply(7, "밥 먹었어?").message().contains("밥을 먹지는 않지만"));
        assertTrue(new ChatService(client, patterns).reply(7, "잘 지내?").message().contains("잘 지내고 있어요"));
        assertTrue(new ChatService(client, patterns).reply(7, "다음에 봐").message().contains("다음에 또 봐요"));
        assertTrue(new ChatService(client, patterns).reply(7, "너 누구야?").message().contains("단짝이에요"));
        assertTrue(new ChatService(client, patterns).reply(7, "힘들어").message().contains("천천히 하셔도 괜찮아요"));
        assertTrue(new ChatService(client, patterns).reply(7, "기분 좋아").message().contains("정말 잘됐어요"));
        verifyNoInteractions(patterns);
    }

    @Test
    void aiResponseIsPreferredForSimilarDailyExpressions() {
        var reply = new ChatService(message -> {
            assertEquals("오늘 너무 피곤하다", message);
            return intent("오늘 많이 피곤하셨군요. 잠시 쉬어 가세요.", Action.NONE, false);
        }, patterns).reply(7, "오늘 너무 피곤하다");
        assertEquals("오늘 많이 피곤하셨군요. 잠시 쉬어 가세요.", reply.message());
        assertFalse(reply.retryable());
    }

    @Test
    void providerFailureStillRecognizesCommonTransferExpressions() {
        ChatIntentClient client = message -> { throw new IllegalStateException("offline"); };
        for (String message : List.of("아들에게 송금", "아들에게 돈 보내고 싶어", "아들에게 이체해 줘")) {
            var reply = new ChatService(client, patterns).reply(7, message);
            assertEquals(Action.TRANSFER, reply.action());
            assertFalse(reply.retryable());
            assertNull(reply.patternId());
        }
        verifyNoInteractions(patterns);
    }

    @Test
    void recommendationsAreShownOnlyForNoneIntentThatRequestsChoices() {
        var help = new ChatService(message -> intent(
                "송금이나 잔액 확인을 도와드릴 수 있어요. 원하는 일을 골라 주세요.",
                Action.NONE, true), patterns).reply(7, "뭘 할 수 있어?");
        assertEquals(Action.NONE, help.action());
        assertTrue(help.showRecommendations());

        when(patterns.getPatterns(7)).thenReturn(List.of());
        var balance = new ChatService(message -> intent("잔액 확인을 도와드릴게요.",
                Action.BALANCE_CHECK, true), patterns).reply(7, "잔액 보고 싶어");
        assertEquals(Action.BALANCE_CHECK, balance.action());
        assertFalse(balance.showRecommendations());
    }

    private ChatIntent intent(String message, Action action, boolean showRecommendations) {
        return new ChatIntent(message, action, showRecommendations);
    }
}
