package com.vanvinh2402.tiltescape.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class UsernamePolicyTest {
    private final UsernamePolicy policy = new UsernamePolicy();

    @Test
    public void trimsAndNormalizesValidUsername() {
        UsernameValidation result = policy.validate("  Player_01  ");

        assertTrue(result.isValid());
        assertEquals(UsernameValidation.Error.NONE, result.getError());
        assertEquals("Player_01", result.getDisplayUsername());
        assertEquals("player_01", result.getNormalizedUsername());
    }

    @Test
    public void acceptsThreeAndTwentyAsciiCharacters() {
        assertTrue(policy.validate("Ab1").isValid());
        assertTrue(policy.validate("Abcdefghijklmnopq_12").isValid());
    }

    @Test
    public void rejectsRequiredAndLengthBoundaries() {
        assertInvalid(null, UsernameValidation.Error.REQUIRED);
        assertInvalid("   ", UsernameValidation.Error.REQUIRED);
        assertInvalid("A1", UsernameValidation.Error.INVALID_LENGTH);
        assertInvalid("Abcdefghijklmnopqr_12", UsernameValidation.Error.INVALID_LENGTH);
    }

    @Test
    public void rejectsSpacesPunctuationAndUnicodeLetters() {
        assertInvalid("Player 01", UsernameValidation.Error.INVALID_CHARACTERS);
        assertInvalid("Player-01", UsernameValidation.Error.INVALID_CHARACTERS);
        assertInvalid("Mạnh_01", UsernameValidation.Error.INVALID_CHARACTERS);
    }

    private void assertInvalid(String input, UsernameValidation.Error expected) {
        UsernameValidation result = policy.validate(input);
        assertFalse(result.isValid());
        assertEquals(expected, result.getError());
    }
}
