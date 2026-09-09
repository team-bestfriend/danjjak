package com.bestfriend.danjjak.chat.service;

import com.bestfriend.danjjak.chat.dto.ChatDtos.Action;
import com.bestfriend.danjjak.chat.dto.ChatDtos.ChatResponse;
import com.bestfriend.danjjak.pattern.service.PatternService;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private static final Pattern SENSITIVE = Pattern.compile(
            "비밀번호|비번|계좌\\s*번호|주민\\s*등록|인증\\s*번호|(?i:password|pin|otp)|(?:[0-9][ -]?){4,}");
    private final ChatIntentClient client;
    private final PatternService patterns;

    public ChatService(ChatIntentClient client, PatternService patterns) {
        this.client = client;
        this.patterns = patterns;
    }

    public ChatResponse reply(long userId, String message) {
        if (SENSITIVE.matcher(message).find()) {
            return new ChatResponse("비밀번호나 계좌번호는 채팅에 쓰지 마세요. 필요한 정보는 해당 금융 화면에서 입력해 주세요.",
                    Action.NONE, null, false);
        }
        try {
            Action action = Action.valueOf(client.classify(message));
            // 송금 대상은 추측하지 않고 기존 송금 화면에서 사용자가 직접 선택한다.
            Long patternId = switch (action) {
                case TRANSFER, APP_HELP, NONE -> null;
                default -> patterns.getPatterns(userId).stream()
                        .filter(pattern -> pattern.patternType().name().equals(action.name()))
                        .map(pattern -> pattern.patternId()).findFirst().orElse(null);
            };
            String text = switch (action) {
                case TRANSFER -> "돈 보내는 걸 도와드릴게요. 아래 버튼을 누른 뒤 받는 분을 골라 주세요.";
                case BALANCE_CHECK -> "통장에 얼마가 있는지 확인해 보세요. 아래 버튼을 눌러 주세요.";
                case PENSION_CHECK -> "연금이 들어왔는지 확인해 보세요. 아래 버튼을 눌러 주세요.";
                case MANAGEMENT_FEE_CHECK -> "관리비 내역을 확인해 보세요. 아래 버튼을 눌러 주세요.";
                case UTILITY_BILL_CHECK -> "공과금 내역을 확인해 보세요. 아래 버튼을 눌러 주세요.";
                case CUSTOMER_CENTER -> "고객센터 연결을 도와드릴게요. 아래 버튼을 눌러 주세요.";
                case APP_HELP -> "단짝 사용 방법을 알려드릴게요. 아래 버튼을 눌러 주세요.";
                case NONE -> "단짝 안에서 돈 보내기와 내역 확인을 도와드려요. 아래 메뉴에서 원하는 업무를 골라 주세요.";
            };
            return new ChatResponse(text, action, patternId, false);
        } catch (RuntimeException exception) {
            return new ChatResponse("지금은 답변을 준비하지 못했어요. 아래 메뉴에서 원하는 업무를 선택해 주세요.",
                    Action.NONE, null, true);
        }
    }
}
