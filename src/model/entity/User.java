package model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * User.java
 *
 * Holds account details, role and calculated cycle averages for use by services and screens.
 * Passwords and password hashes are deliberately excluded from this data record.
 */
public record User(
        int id,
        String name,
        String email,
        BigDecimal avgCycleLength,
        BigDecimal avgPeriodLength,
        LocalDate createdAt,
        String role) {
    @Override
    public String toString() {
        return name + " (#" + id + ")";
    }
}
