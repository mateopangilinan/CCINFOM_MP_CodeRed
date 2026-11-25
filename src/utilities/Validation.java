package utilities;

import exception.AppException;
import java.time.*;
import java.util.*;

/**
 * Validation.java
 *
 * Provides shared checks for required text, email, notes, dates, numbers and allowed choices.
 * Failed checks throw AppException messages that controllers can show in the interface.
 */
public final class Validation {
    private Validation() {}

    // Stop the current operation with this readable message when a rule is not satisfied.
    public static void require(boolean valid, String message) {
        if (!valid) throw new AppException(message);
    }

    // Trim required text and check its character limit before saving it.
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

    // Normalize the email to lower case and check its basic shape.
    public static String email(String value) {
        String s = text(value, "Email", 100).toLowerCase(Locale.ROOT);
        require(s.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"), "Enter a valid email address.");
        return s;
    }

    // Store blank optional notes as null; limit nonblank notes to the database column size.
    public static String notes(String value) {
        if (value == null || value.isBlank()) return null;
        require(
                value.codePointCount(0, value.length()) <= 255,
                "Notes must be at most 255 characters.");
        return value.trim();
    }

    // Allow blank optional dates and turn invalid date input into a friendly message.
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

    // Parse a whole number and replace parsing errors with a field-specific message.
    public static int number(String s, String label) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            throw new AppException(label + " must be a whole number.");
        }
    }

    // Reject missing or future dates for events that are supposed to have already happened.
    public static void actual(LocalDate d, Clock clock) {
        require(
                d != null && !d.isAfter(LocalDate.now(clock)),
                "Actual dates cannot be in the future.");
    }

    // Accept only one of the choices defined by the application.
    public static void option(String s, String[] values) {
        require(Arrays.asList(values).contains(s), "Select a valid option.");
    }
}
