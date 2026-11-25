package utilities;

import java.time.LocalDate;
import java.time.format.*;
import java.util.Locale;

/**
 * Dates.java
 *
 * Formats LocalDate values as readable English dates and parses form input. It accepts both
 * named-month dates and yyyy-MM-dd while database values remain actual dates.
 */
public final class Dates {
    private static final DateTimeFormatter WORDS =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("MMMM d, uuuu")
                    .toFormatter(Locale.ENGLISH)
                    .withResolverStyle(ResolverStyle.STRICT);

    private Dates() {}

    // Show an English month name; a missing date appears as an empty field.
    public static String input(LocalDate date) {
        if (date == null) {
            return "";
        } else {
            return WORDS.format(date);
        }
    }

    // Accept either yyyy-MM-dd or an English date such as September 29, 2026.
    public static LocalDate parse(String text) {
        String value = text.trim();
        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return LocalDate.parse(value);
        } else {
            return LocalDate.parse(value, WORDS);
        }
    }
}
