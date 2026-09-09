package com.bestfriend.danjjak.chat.service;

import com.bestfriend.danjjak.chat.dto.ChatDtos.Action;
import com.bestfriend.danjjak.chat.dto.ChatDtos.ChatResponse;
import com.bestfriend.danjjak.chat.service.ChatIntentClient.ChatIntent;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternSummaryResponse;
import com.bestfriend.danjjak.pattern.service.PatternService;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private static final Pattern SENSITIVE = Pattern.compile(
            "비밀번호|비번|계좌\\s*번호|주민\\s*등록|인증\\s*번호|(?i:password|pin|otp)|(?:[0-9][ -]?){4,}");
    private static final Pattern GREETING = Pattern.compile(
            "^(?:안녕(?:하세요)?|ㅎㅇ|반가워(?:요)?)[!?.~\\s]*$");
    private static final Pattern THANKS = Pattern.compile(
            "^(?:고마워(?:요)?|감사해(?:요)?|감사합니다)[!?.~\\s]*$");
    private static final Pattern WELLBEING = Pattern.compile(
            "^(?:잘\\s*지내(?:요)?|요즘\\s*어때|오늘\\s*기분\\s*어때)[!?.~\\s]*$");
    private static final Pattern MEAL = Pattern.compile(
            "^(?:밥\\s*먹었어|식사\\s*했어)[!?.~\\s]*$");
    private static final Pattern GOODBYE = Pattern.compile(
            "^(?:잘\\s*가|다음에\\s*봐|또\\s*보자)[!?.~\\s]*$");
    private static final Pattern IDENTITY = Pattern.compile(
            "^(?:너\\s*누구야|누구세요|이름(?:이)?\\s*뭐야|단짝이\\s*뭐야)[!?.~\\s]*$");
    private static final Pattern WAITING = Pattern.compile(
            "^(?:뭐\\s*하고\\s*있어|지금\\s*뭐\\s*해|심심해)[!?.~\\s]*$");
    private static final Pattern COMPLIMENT = Pattern.compile(
            "^(?:귀엽다|귀여워|멋지다|잘했어|최고야)[!?.~\\s]*$");
    private static final Pattern DIFFICULT_DAY = Pattern.compile(
            "^(?:피곤해|힘들어|속상해|외로워|기분이\\s*안\\s*좋아)[!?.~\\s]*$");
    private static final Pattern GOOD_DAY = Pattern.compile(
            "^(?:기분\\s*좋아|행복해|오늘\\s*좋은\\s*일이\\s*있었어)[!?.~\\s]*$");
    private static final Pattern TRANSFER = Pattern.compile(".*(?:송금|이체|돈\\s*(?:을\\s*)?(?:보내|부쳐)).*");
    private static final Pattern BALANCE = Pattern.compile(".*(?:잔액|통장에\\s*얼마|돈이?\\s*얼마\\s*남).*");
    private static final Pattern PENSION = Pattern.compile(".*연금.*(?:확인|들어|입금).*");
    private static final Pattern MANAGEMENT_FEE = Pattern.compile(".*관리비.*(?:확인|내역|얼마).*");
    private static final Pattern UTILITY_BILL = Pattern.compile(".*공과금.*(?:확인|내역|얼마).*");
    private static final Pattern SHORTCUT_COMMAND = Pattern.compile(
            "^(?<number>1[0-2]|[1-9])\\s*번?(?:\\s*(?:실행(?:해|해\\s*줘|해줘|해\\s*주세요|해주세요)?|해\\s*줘|해줘|해\\s*주세요|해주세요))?[!?.~\\s]*$");
    private final ChatIntentClient client;
    private final PatternService patterns;

    public ChatService(ChatIntentClient client, PatternService patterns) {
        this.client = client;
        this.patterns = patterns;
    }

    public ChatResponse reply(long userId, String message) {
        if (SENSITIVE.matcher(message).find()) {
            return new ChatResponse("비밀번호나 계좌번호는 채팅에 쓰지 마세요. 필요한 정보는 해당 금융 화면에서 입력해 주세요.",
                    Action.NONE, null, false, false);
        }
        String trimmed = message.trim();
        ChatResponse shortcutResponse = responseForShortcut(userId, trimmed);
        if (shortcutResponse != null) {
            return shortcutResponse;
        }
        try {
            return responseFor(userId, client.classify(message));
        } catch (RuntimeException exception) {
            ChatIntent fallback = fallbackIntent(trimmed);
            if (fallback == null) {
                return new ChatResponse("지금은 답변을 준비하지 못했어요. 다시 물어봐 주세요.",
                        Action.NONE, null, true, false);
            }
            try {
                return responseFor(userId, fallback);
            } catch (RuntimeException fallbackException) {
                return new ChatResponse("지금은 답변을 준비하지 못했어요. 다시 물어봐 주세요.",
                        Action.NONE, null, true, false);
            }
        }
    }

    private ChatResponse responseForShortcut(long userId, String message) {
        var matcher = SHORTCUT_COMMAND.matcher(message);
        if (!matcher.matches()) {
            return null;
        }
        int shortcutNumber = Integer.parseInt(matcher.group("number"));
        PatternSummaryResponse pattern = patterns.getPatterns(userId).stream()
                .filter(candidate -> candidate.shortcutNumber() == shortcutNumber)
                .findFirst()
                .orElse(null);
        if (pattern == null) {
            return new ChatResponse(shortcutNumber + "번에 등록된 금융 업무가 없어요. 다른 번호를 말씀해 주세요.",
                    Action.NONE, null, false, true);
        }
        return new ChatResponse(shortcutNumber + "번 " + pattern.title() + " 업무를 시작할까요?",
                Action.PATTERN, pattern.patternId(), false, false);
    }

    private ChatResponse responseFor(long userId, ChatIntent intent) {
        if (intent == null || intent.message() == null || intent.message().isBlank() || intent.action() == null) {
            throw new IllegalArgumentException("Invalid chat intent");
        }
        Action action = intent.action();
        // 송금 대상은 추측하지 않고 기존 송금 화면에서 사용자가 직접 선택한다.
        Long patternId = switch (action) {
            case TRANSFER, APP_HELP, NONE -> null;
            default -> patterns.getPatterns(userId).stream()
                    .filter(pattern -> pattern.patternType().name().equals(action.name()))
                    .map(pattern -> pattern.patternId()).findFirst().orElse(null);
        };
        boolean showRecommendations = action == Action.NONE && intent.showRecommendations();
        return new ChatResponse(intent.message(), action, patternId, false, showRecommendations);
    }

    private ChatIntent fallbackIntent(String message) {
        if (GREETING.matcher(message).matches()) {
            return new ChatIntent("안녕하세요! 무엇을 도와드릴까요?", Action.NONE, true);
        }
        if (THANKS.matcher(message).matches()) {
            return new ChatIntent("별말씀을요! 다른 도움이 필요하면 말씀해 주세요.", Action.NONE, true);
        }
        if (WELLBEING.matcher(message).matches()) {
            return new ChatIntent("네, 잘 지내고 있어요. 오늘도 천천히 도와드릴게요.", Action.NONE, false);
        }
        if (MEAL.matcher(message).matches()) {
            return new ChatIntent("저는 밥을 먹지는 않지만, 챙겨 주셔서 고마워요!", Action.NONE, false);
        }
        if (GOODBYE.matcher(message).matches()) {
            return new ChatIntent("네, 다음에 또 봐요! 편안한 하루 보내세요.", Action.NONE, false);
        }
        if (IDENTITY.matcher(message).matches()) {
            return new ChatIntent("저는 금융 업무를 천천히 도와드리는 단짝이에요.", Action.NONE, false);
        }
        if (WAITING.matcher(message).matches()) {
            return new ChatIntent("여기서 기다리고 있었어요. 필요한 게 있으면 편하게 말씀해 주세요.",
                    Action.NONE, false);
        }
        if (COMPLIMENT.matcher(message).matches()) {
            return new ChatIntent("고마워요! 더 편하게 도와드릴게요.", Action.NONE, false);
        }
        if (DIFFICULT_DAY.matcher(message).matches()) {
            return new ChatIntent("그러셨군요. 잠시 쉬어 가며 천천히 하셔도 괜찮아요.", Action.NONE, false);
        }
        if (GOOD_DAY.matcher(message).matches()) {
            return new ChatIntent("정말 잘됐어요! 좋은 기분이 오래 갔으면 좋겠어요.", Action.NONE, false);
        }
        if (TRANSFER.matcher(message).matches()) return new ChatIntent("송금을 도와드릴게요.", Action.TRANSFER, false);
        if (BALANCE.matcher(message).matches()) return new ChatIntent("잔액 확인을 도와드릴게요.", Action.BALANCE_CHECK, false);
        if (PENSION.matcher(message).matches()) return new ChatIntent("연금 입금 확인을 도와드릴게요.", Action.PENSION_CHECK, false);
        if (MANAGEMENT_FEE.matcher(message).matches()) return new ChatIntent("관리비 확인을 도와드릴게요.", Action.MANAGEMENT_FEE_CHECK, false);
        if (UTILITY_BILL.matcher(message).matches()) return new ChatIntent("공과금 확인을 도와드릴게요.", Action.UTILITY_BILL_CHECK, false);
        return null;
    }
}
