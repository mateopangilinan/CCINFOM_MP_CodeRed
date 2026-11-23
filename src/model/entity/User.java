package model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

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
