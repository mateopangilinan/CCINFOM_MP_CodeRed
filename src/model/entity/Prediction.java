package model.entity;

import java.time.LocalDate;

/**
 * Prediction.java
 *
 * Holds a saved period estimate, its owner, the date it was made, the estimated dates, method
 * and confidence range. These values describe the estimate at the time it was saved.
 */
public record Prediction(
        int id,
        int userId,
        LocalDate date,
        LocalDate start,
        LocalDate end,
        String method,
        int range) {}
