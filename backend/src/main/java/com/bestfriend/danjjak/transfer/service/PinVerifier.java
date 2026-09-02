package com.bestfriend.danjjak.transfer.service;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class PinVerifier {

    public boolean matches(String rawPin, String pinHash) {
        if (rawPin == null || pinHash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPin, pinHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
