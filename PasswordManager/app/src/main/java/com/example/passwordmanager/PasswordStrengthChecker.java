package com.example.passwordmanager;

// import android.graphics.Color; // No longer needed for direct parsing here
import com.example.passwordmanager.R; // Import R file

public class PasswordStrengthChecker {

    public enum StrengthLevel {
        EMPTY("Trống", R.color.strength_empty),
        VERY_WEAK("Rất yếu", R.color.strength_very_weak),
        WEAK("Yếu", R.color.strength_weak),
        MEDIUM("Trung bình", R.color.strength_medium),
        STRONG("Mạnh", R.color.strength_strong),
        VERY_STRONG("Rất mạnh", R.color.strength_very_strong);

        private final String displayName;
        private final int colorResId; // Changed to color resource ID

        StrengthLevel(String displayName, int colorResId) {
            this.displayName = displayName;
            this.colorResId = colorResId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getColorResId() { // Getter for the resource ID
            return colorResId;
        }
    }

    public static StrengthLevel calculateStrength(String password) {
        if (password == null || password.isEmpty()) {
            return StrengthLevel.EMPTY;
        }

        int score = 0;
        int length = password.length();

        // Length score
        if (length < 8) {
            score += 0;
        } else if (length < 12) {
            score += 1;
        } else {
            score += 2;
        }

        // Uppercase letter score
        if (password.matches(".*[A-Z].*")) {
            score += 1;
        }

        // Lowercase letter score
        if (password.matches(".*[a-z].*")) {
            score +=1;
        }

        // Digit score
        if (password.matches(".*[0-9].*")) {
            score += 1;
        }

        // Special character score
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>/?].*")) {
            score += 1;
        }

        // Determine strength level based on score
        if (length < 6) return StrengthLevel.VERY_WEAK;

        if (score <= 1) { // Adjusted score threshold slightly
            return StrengthLevel.VERY_WEAK;
        } else if (score == 2) {
            return StrengthLevel.WEAK;
        } else if (score == 3) {
            return StrengthLevel.MEDIUM;
        } else if (score == 4) {
            return StrengthLevel.STRONG;
        } else { // score >= 5
            return StrengthLevel.VERY_STRONG;
        }
    }
}
