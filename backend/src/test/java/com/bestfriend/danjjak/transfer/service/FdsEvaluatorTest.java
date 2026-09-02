package com.bestfriend.danjjak.transfer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class FdsEvaluatorTest {

    private final FdsEvaluator evaluator = new FdsEvaluator();

    @Test
    void amountBelowThresholdIsNormal() {
        var result = evaluator.evaluate(new BigDecimal("9999999"), 1);

        assertEquals("NORMAL", result.riskLevel());
        assertFalse(result.anomalous());
    }

    @Test
    void thresholdAmountIsMedium() {
        var result = evaluator.evaluate(new BigDecimal("10000000"), 1);

        assertEquals("MEDIUM", result.riskLevel());
        assertEquals(List.of("HIGH_AMOUNT"), result.reasons());
    }

    @Test
    void twoRecentTransfersAreMedium() {
        var result = evaluator.evaluate(new BigDecimal("1000"), 2);

        assertEquals("MEDIUM", result.riskLevel());
        assertEquals(List.of("REPEATED_TRANSFER"), result.reasons());
    }

    @Test
    void bothRulesAreHigh() {
        var result = evaluator.evaluate(new BigDecimal("10000000"), 2);

        assertEquals("HIGH", result.riskLevel());
        assertTrue(result.highAmountDetected());
        assertTrue(result.repeatedTransferDetected());
    }
}
