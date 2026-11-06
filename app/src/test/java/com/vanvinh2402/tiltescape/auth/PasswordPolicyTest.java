package com.vanvinh2402.tiltescape.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    public void rejectsMissingPassword() {
        PasswordValidation validation = policy.validate(null, new char[0]);

        assertFalse(validation.isValid());
        assertEquals(PasswordValidation.Error.REQUIRED, validation.getError());
    }

    @Test
    public void rejectsPasswordShorterThanSixCharacters() {
        PasswordValidation validation = policy.validate("12345".toCharArray(), "12345".toCharArray());

        assertFalse(validation.isValid());
        assertEquals(PasswordValidation.Error.TOO_SHORT, validation.getError());
    }

    @Test
    public void acceptsMatchingSixCharacterPassword() {
        PasswordValidation validation = policy.validate("123456".toCharArray(), "123456".toCharArray());

        assertTrue(validation.isValid());
        assertEquals(PasswordValidation.Error.NONE, validation.getError());
    }

    @Test
    public void rejectsMismatchedConfirmation() {
        PasswordValidation validation = policy.validate("secret1".toCharArray(), "secret2".toCharArray());

        assertFalse(validation.isValid());
        assertEquals(PasswordValidation.Error.CONFIRMATION_MISMATCH, validation.getError());
    }
}
