package com.bestfriend.danjjak.analysis.service;

import java.math.BigDecimal;

public interface InstructionTextSuggestionClient {
    String suggest(SuggestionContext context);

    record SuggestionContext(
            String patternTitle, String stepName, String stepCode, String currentText,
            long retryCount, long backCount, long wrongTouchCount, long routeDeviationCount,
            long visitCount, BigDecimal averageDurationSeconds) {}
}
