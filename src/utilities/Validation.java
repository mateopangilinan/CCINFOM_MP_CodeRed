package utilities;

import exception.AppException;

import java.time.*;
import java.util.*;

public final class Validation {
    private Validation() {}

    public static void require(boolean valid, String message) {
        if (!valid) throw new AppException(message);
    }

    public static String text(String value, String label, int max) {
        String s = "";
        if (value != null) {
            s = value.trim();
        }
        require(
                !s.isBlank() && s.codePointCount(0, s.length()) <= max,
                label + " is required (maximum " + max + " characters).");
        return s;
    }

    public static String email(String value) {
        String s = text(value, "Email", 100).toLowerCase(Locale.ROOT);
        require(s.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "Enter a valid email address.");
        return s;
    }

    public static String notes(String value) {
        if (value == null || value.isBlank()) return null;
        require(
                value.codePointCount(0, value.length()) <= 255,
                "Notes must be at most 255 characters.");
        return value.trim();
    }

    public static LocalDate date(String s, boolean optional) {
        if (optional && (s == null || s.isBlank())) return null;
        try {
            require(s != null && !s.isBlank(), "Enter a date such as September 29, 2026.");
            return Dates.parse(s);
        } catch (DateTimeException e) {
            throw new AppException(
                    "Enter a valid date such as September 29, 2026 (or yyyy-MM-dd).");
        }
    }

    public static int number(String s, String label) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            throw new AppException(label + " must be a whole number.");
        }
    }

    public static void actual(LocalDate d, Clock clock) {
        require(
                d != null && !d.isAfter(LocalDate.now(clock)),
                "Actual dates cannot be in the future.");
    }

    public static void option(String s, String[] values) {
        require(Arrays.asList(values).contains(s), "Select a valid option.");
    }
}
