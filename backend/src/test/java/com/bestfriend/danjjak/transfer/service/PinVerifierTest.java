package com.bestfriend.danjjak.transfer.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PinVerifierTest {

    private static final String DEMO_PIN_HASH =
            "$2a$10$sjht3pDQhuydNc41VUyyQe9FTAXeG5Xup8z1b41I9LmnLyrVVvB.6";

    private final PinVerifier pinVerifier = new PinVerifier();

    @Test
    void matchesSeededDemoPin() {
        assertTrue(pinVerifier.matches("1234", DEMO_PIN_HASH));
        assertFalse(pinVerifier.matches("0000", DEMO_PIN_HASH));
    }
}
