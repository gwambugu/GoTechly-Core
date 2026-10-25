package com.dotechly.gotechly.core.service.partner;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates cryptographically random salt strings containing
 * uppercase, lowercase, digits, and special characters.
 */
@Component
public class SaltGenerator {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()-_=+[]{}|;:',.<>?";
    private static final String ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;

    private static final int DEFAULT_LENGTH = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a random salt of default length (32 characters).
     * Guarantees at least one character from each character class.
     */
    public String generate() {
        return generate(DEFAULT_LENGTH);
    }

    /**
     * Generates a random salt of the specified length.
     * Guarantees at least one character from each character class (uppercase, lowercase, digit, special).
     *
     * @param length the desired salt length (minimum 4)
     * @return the generated salt string
     */
    public String generate(int length) {
        if (length < 4) {
            throw new IllegalArgumentException("Salt length must be at least 4");
        }

        char[] salt = new char[length];

        // Guarantee at least one from each class
        salt[0] = randomChar(UPPERCASE);
        salt[1] = randomChar(LOWERCASE);
        salt[2] = randomChar(DIGITS);
        salt[3] = randomChar(SPECIAL);

        // Fill remaining positions from full character set
        for (int i = 4; i < length; i++) {
            salt[i] = randomChar(ALL_CHARS);
        }

        // Shuffle to avoid predictable positions for guaranteed characters
        for (int i = length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = salt[i];
            salt[i] = salt[j];
            salt[j] = temp;
        }

        return new String(salt);
    }

    private char randomChar(String chars) {
        return chars.charAt(secureRandom.nextInt(chars.length()));
    }
}
