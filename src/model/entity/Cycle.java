package model.entity;

import java.time.LocalDate;

/**
 * Cycle.java
 *
 * Holds one user's saved cycle dates, optional notes and calculated cycle length. The newest
 * cycle has no length until a later start is recorded.
 */
public record Cycle(
        int id, int userId, LocalDate start, LocalDate end, Integer length, String notes) {}
