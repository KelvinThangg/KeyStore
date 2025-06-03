package com.example.passwordmanager;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PasswordGenerator {

    private static final String LOWERCASE_CHARACTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMERIC_CHARACTERS = "0123456789";
    private static final String SPECIAL_CHARACTERS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    public static String generatePassword(int length, boolean useUppercase, boolean useNumeric, boolean useSpecial) {
        if (length <= 0) {
            return "";
        }

        StringBuilder charPool = new StringBuilder(LOWERCASE_CHARACTERS);
        List<Character> passwordChars = new ArrayList<>();
        SecureRandom random = new SecureRandom();

        // Ensure at least one lowercase character
        passwordChars.add(LOWERCASE_CHARACTERS.charAt(random.nextInt(LOWERCASE_CHARACTERS.length())));

        if (useUppercase) {
            charPool.append(UPPERCASE_CHARACTERS);
            passwordChars.add(UPPERCASE_CHARACTERS.charAt(random.nextInt(UPPERCASE_CHARACTERS.length())));
        }
        if (useNumeric) {
            charPool.append(NUMERIC_CHARACTERS);
            passwordChars.add(NUMERIC_CHARACTERS.charAt(random.nextInt(NUMERIC_CHARACTERS.length())));
        }
        if (useSpecial) {
            charPool.append(SPECIAL_CHARACTERS);
            passwordChars.add(SPECIAL_CHARACTERS.charAt(random.nextInt(SPECIAL_CHARACTERS.length())));
        }

        if (passwordChars.size() > length) {
            // This case should ideally not happen if length is reasonable (e.g., > 4)
            // Or, adjust logic to ensure minimums are met within the length.
            // For now, let's just take the first 'length' characters if over.
            while(passwordChars.size() > length) {
                passwordChars.remove(passwordChars.size() -1);
            }
        }


        for (int i = passwordChars.size(); i < length; i++) {
            passwordChars.add(charPool.charAt(random.nextInt(charPool.length())));
        }

        Collections.shuffle(passwordChars, random);

        StringBuilder password = new StringBuilder(length);
        for (Character ch : passwordChars) {
            password.append(ch);
        }

        return password.toString();
    }
}
