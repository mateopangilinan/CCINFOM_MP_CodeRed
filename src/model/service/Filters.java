package model.service;

import utilities.Validation;

import java.time.*;

/**
 * Filters.java
 *
 * Validates the date and owner filters passed to services. It converts a complete month and year
 * into a starting date and rejects requests for another user's data when access is restricted.
 */
final class Filters {
    private Filters() {}

    // No date filter returns null; otherwise require a complete month/year and return its first
    // day.
    static LocalDate month(Integer year, Integer month) {
        if (year == null && month == null) return null;
        Validation.require(year != null && month != null, "Select both a month and a year.");
        Validation.require(
                year >= 1 && year <= 9998 && month >= 1 && month <= 12,
                "Select a valid month and year.");
        return LocalDate.of(year, month, 1);
    }

    // Reject a filter that asks for someone else's records unless this session is an admin.
    static void owner(Integer owner, int scope) {
        Validation.require(
                owner == null || scope == 0 || owner == scope, "Access denied for this user.");
    }
}
