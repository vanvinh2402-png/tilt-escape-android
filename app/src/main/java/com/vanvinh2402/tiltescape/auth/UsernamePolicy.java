package com.vanvinh2402.tiltescape.auth;

import java.util.Locale;
import java.util.regex.Pattern;

public final class UsernamePolicy {
    private static final int MINIMUM_LENGTH = 3;
    private static final int MAXIMUM_LENGTH = 20;
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile("[A-Za-z0-9_]+");

    public UsernameValidation validate(String rawUsername) {
        String display = rawUsername == null ? "" : rawUsername.trim();
        if (display.isEmpty()) {
            return UsernameValidation.invalid(UsernameValidation.Error.REQUIRED);
        }
        if (display.length() < MINIMUM_LENGTH || display.length() > MAXIMUM_LENGTH) {
            return UsernameValidation.invalid(UsernameValidation.Error.INVALID_LENGTH);
        }
        if (!ALLOWED_CHARACTERS.matcher(display).matches()) {
            return UsernameValidation.invalid(UsernameValidation.Error.INVALID_CHARACTERS);
        }
        return UsernameValidation.valid(display, display.toLowerCase(Locale.ROOT));
    }
}
