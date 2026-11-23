package utilities;

import java.time.LocalDate;
import java.time.format.*;
import java.util.Locale;

/** English UI dates; persistence continues to use LocalDate and SQL DATE. */
public final class Dates {
    private static final DateTimeFormatter WORDS =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("MMMM d, uuuu")
                    .toFormatter(Locale.ENGLISH)
                    .withResolverStyle(ResolverStyle.STRICT);

    private Dates() {}

    public static String input(LocalDate date) {
        if (date == null) {
            return "";
        } else {
            return WORDS.format(date);
        }
    }

    public static LocalDate parse(String text) {
        String value = text.trim();
        if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return LocalDate.parse(value);
        } else {
            return LocalDate.parse(value, WORDS);
        }
    }
}
