package com.vanvinh2402.tiltescape.auth;

import java.util.Arrays;

public final class PasswordPolicy {
    private static final int MIN_LENGTH = 6;

    public PasswordValidation validate(char[] password, char[] confirmation) {
        if (password == null || password.length == 0) {
            return PasswordValidation.invalid(PasswordValidation.Error.REQUIRED);
        }
        if (password.length < MIN_LENGTH) {
            return PasswordValidation.invalid(PasswordValidation.Error.TOO_SHORT);
        }
        if (!Arrays.equals(password, confirmation)) {
            return PasswordValidation.invalid(PasswordValidation.Error.CONFIRMATION_MISMATCH);
        }
        return PasswordValidation.valid();
    }
}
