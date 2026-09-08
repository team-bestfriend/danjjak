package com.bestfriend.danjjak.analysis.service;

import com.bestfriend.danjjak.analysis.dto.InstructionSuggestionDtos.ApplySuggestionRequest;
import com.bestfriend.danjjak.analysis.dto.InstructionSuggestionDtos.InstructionSuggestion;
import com.bestfriend.danjjak.analysis.dto.InstructionSuggestionDtos.SuggestionResponse;
import com.bestfriend.danjjak.analysis.dto.UsageAnalysisDtos.StepAnalysisResponse;
import com.bestfriend.danjjak.analysis.service.InstructionTextSuggestionClient.SuggestionContext;
import com.bestfriend.danjjak.common.error.ApiException;
import com.bestfriend.danjjak.pattern.dto.GuidanceDtos.GuidanceResponse;
import com.bestfriend.danjjak.pattern.dto.GuidanceDtos.GuidanceUpdateRequest;
import com.bestfriend.danjjak.pattern.dto.GuidanceDtos.VoiceMode;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternDetailResponse;
import com.bestfriend.danjjak.pattern.dto.PatternDtos.PatternStepResponse;
import com.bestfriend.danjjak.pattern.service.GuidanceService;
import com.bestfriend.danjjak.pattern.service.PatternService;
import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstructionSuggestionService {
    private static final Logger log = LogManager.getLogger(InstructionSuggestionService.class);
    private static final int MAX_SUGGESTION_LENGTH = 60;
    private static final Pattern MULTIPLE_SENTENCES = Pattern.compile("[.!?。！？].+\\S", Pattern.DOTALL);
    private static final Set<String> DISALLOWED_WORDS = Set.of("해당", "진행", "수행", "입력값", "인증", "경로");

    private final UsageAnalysisService analysis;
    private final PatternService patterns;
    private final GuidanceService guidance;
    private final InstructionTextSuggestionClient suggestionClient;

    public InstructionSuggestionService(UsageAnalysisService analysis, PatternService patterns, GuidanceService guidance,
            InstructionTextSuggestionClient suggestionClient) {
        this.analysis = analysis;
        this.patterns = patterns;
        this.guidance = guidance;
        this.suggestionClient = suggestionClient;
    }

    public SuggestionResponse get(long userId, LocalDate from, LocalDate to) {
        var report = analysis.getUsageAnalysis(userId, from, to);
        var difficult = report.difficultStep();
        if (difficult == null) return new SuggestionResponse(report.status(), from, to, null);
        var pattern = patterns.getPattern(userId, difficult.patternId());
        var step = findStep(pattern, difficult.stepId());
        var current = findGuidance(userId, pattern.patternId(), step.stepCode());
        String text = suggestedText(pattern, step, current.text(), difficult);
        var suggestion = text == null ? null : new InstructionSuggestion(pattern.patternId(), pattern.title(), step.stepId(),
                step.stepOrder(), step.stepCode(), step.stepName(), current.text(), text,
                current.audioUrl() != null, current.voiceScriptOutdated());
        return new SuggestionResponse(report.status(), from, to, suggestion);
    }

    @Transactional
    public GuidanceResponse apply(long userId, long patternId, long stepId, ApplySuggestionRequest request) {
        var pattern = patterns.getPattern(userId, patternId);
        var step = findStep(pattern, stepId);
        var current = findGuidance(userId, patternId, step.stepCode());
        if (!current.text().equals(request.expectedText())) {
            throw new ApiException(HttpStatus.CONFLICT, "INSTRUCTION_CHANGED", "안내 문구가 바뀌었어요. 다시 불러와 비교한 뒤 적용해 주세요.");
        }
        String text = request.suggestedText().trim();
        if (!text.equals(fallbackText(pattern, step)) && !isValidSuggestion(text, current.text(), pattern, step.stepCode())) {
            throw missing();
        }
        // 수동 편집과 같은 저장 경로를 사용해 음성 선택과 기존 녹음을 보존한다.
        return guidance.update(userId, patternId, step.stepCode(), new GuidanceUpdateRequest(text,
                current.voiceMode() == null ? null : VoiceMode.valueOf(current.voiceMode())));
    }

    private GuidanceResponse findGuidance(long userId, long patternId, String stepCode) {
        return guidance.getAll(userId, patternId).stream().filter(item -> item.target().equals(stepCode))
                .findFirst().orElseThrow(InstructionSuggestionService::missing);
    }

    private PatternStepResponse findStep(PatternDetailResponse pattern, long stepId) {
        return pattern.steps().stream().filter(step -> step.stepId() == stepId)
                .findFirst().orElseThrow(InstructionSuggestionService::missing);
    }

    private String suggestedText(PatternDetailResponse pattern, PatternStepResponse step, String currentText,
            StepAnalysisResponse difficult) {
        String fallback = fallbackText(pattern, step);
        if (fallback == null) return null;
        try {
            String suggested = suggestionClient.suggest(new SuggestionContext(
                    redactSensitiveText(pattern.title()), redactSensitiveText(step.stepName()), step.stepCode(),
                    redactSensitiveText(currentText),
                    difficult.retryCount(), difficult.backCount(), difficult.wrongTouchCount(),
                    difficult.routeDeviationCount(), difficult.visitCount(), difficult.averageDurationSeconds()));
            return isValidSuggestion(suggested, currentText, pattern, step.stepCode()) ? suggested.trim() : fallback;
        } catch (RuntimeException exception) {
            // 외부 AI 장애가 이용 분석과 기존 규칙 기반 제안을 막지 않게 한다.
            log.warn("AI 안내 문구 제안에 실패해 규칙 기반 문구를 사용합니다: stepCode={}", step.stepCode());
            return fallback;
        }
    }

    private boolean isValidSuggestion(String suggested, String currentText, PatternDetailResponse pattern, String stepCode) {
        if (suggested == null || suggested.isBlank()) return false;
        String text = suggested.trim();
        if (text.equals(currentText.trim()) || text.length() > MAX_SUGGESTION_LENGTH
                || text.length() > currentText.trim().length()
                || text.contains("\n") || MULTIPLE_SENTENCES.matcher(text).find()
                || DISALLOWED_WORDS.stream().anyMatch(text::contains)) return false;
        return matchesStepAction(text, pattern, stepCode);
    }

    private boolean matchesStepAction(String text, PatternDetailResponse pattern, String stepCode) {
        return switch (stepCode) {
            case "SELECT_SOURCE" -> containsAny(text, "통장", "계좌") && containsAny(text, "눌", "고르", "선택");
            case "SELECT_PERSON" -> containsAny(text, "사람", "가족", "이름", "받는 분") && containsAny(text, "눌", "고르", "선택");
            case "SELECT_ACCOUNT" -> containsAny(text, "통장", "계좌") && containsAny(text, "눌", "고르", "선택");
            case "INPUT_AMOUNT" -> text.contains("금액") && containsAny(text, "눌", "쓰", "적");
            case "CONFIRM_TRANSFER" -> containsAny(text, "받는", "사람", "분") && text.contains("금액") && containsAny(text, "보", "맞");
            case "ENTER_PIN" -> containsAny(text, "비밀번호", "비밀 번호") && containsAny(text, "눌", "쓰");
            case "CALL_SUPPORT" -> text.contains("전화") && containsAny(text, "눌", "걸");
            case "CHECK_RESULT" -> matchesResultAction(text, pattern);
            default -> false;
        };
    }

    private boolean matchesResultAction(String text, PatternDetailResponse pattern) {
        boolean action = containsAny(text, "보", "확인", "눌");
        if (!action) return false;
        return switch (pattern.patternType()) {
            case BALANCE_CHECK -> text.contains("잔액") || text.contains("남은 돈");
            case PENSION_CHECK -> text.contains("연금");
            case MANAGEMENT_FEE_CHECK -> text.contains("관리비");
            case UTILITY_BILL_CHECK -> text.contains("공과금");
            case CUSTOMER_CENTER -> text.contains("전화");
            default -> containsAny(text, "거래", "돈", "내역");
        };
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) if (text.contains(word)) return true;
        return false;
    }

    private String redactSensitiveText(String text) {
        // 사용자가 안내 문구에 넣은 계좌번호·비밀번호로 보이는 숫자는 외부 전송 전에 제거한다.
        return text.replaceAll("(?<!\\d)\\d(?:[ -]?\\d){3,}(?!\\d)", "[민감한 숫자 제외]");
    }

    private String fallbackText(PatternDetailResponse pattern, PatternStepResponse step) {
        return switch (step.stepCode()) {
            case "SELECT_SOURCE" -> "돈을 보낼 내 통장을 눌러 주세요. 고른 통장에서 돈이 나가요.";
            case "SELECT_PERSON" -> "돈을 받을 가족의 이름을 눌러 주세요.";
            case "SELECT_ACCOUNT" -> "돈을 받을 계좌를 눌러 주세요. 이름과 계좌번호를 함께 확인해요.";
            case "INPUT_AMOUNT" -> "보낼 금액을 숫자로 눌러 주세요. 다 입력하면 다음을 눌러 주세요.";
            case "CONFIRM_TRANSFER" -> "받는 분과 금액이 맞는지 천천히 확인해 주세요.";
            case "ENTER_PIN" -> "돈을 보낼 내 통장의 비밀번호 네 자리를 눌러 주세요.";
            case "CALL_SUPPORT" -> "도움이 필요하면 전화 연결하기를 눌러 주세요.";
            case "CHECK_RESULT" -> switch (pattern.patternType()) {
                case BALANCE_CHECK -> "잔액 보기를 누르면 이 통장에 남은 돈을 볼 수 있어요.";
                case PENSION_CHECK -> "이 계좌에 들어온 연금 내역을 천천히 확인해 주세요.";
                case MANAGEMENT_FEE_CHECK -> "이 계좌의 관리비 거래 내역을 천천히 확인해 주세요.";
                case UTILITY_BILL_CHECK -> "이 계좌의 공과금 거래 내역을 천천히 확인해 주세요.";
                case CUSTOMER_CENTER -> "도움이 필요하면 전화 연결하기를 눌러 주세요.";
                default -> "이 계좌에서 들어오고 나간 돈을 천천히 확인해 주세요.";
            };
            default -> null;
        };
    }

    private static ApiException missing() {
        return new ApiException(HttpStatus.NOT_FOUND, "SUGGESTION_NOT_FOUND", "이 단계의 문구 제안을 찾을 수 없습니다.");
    }
}
