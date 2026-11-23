package model.entity;

import java.time.LocalDate;

public record Prediction(
        int id,
        int userId,
        LocalDate date,
        LocalDate start,
        LocalDate end,
        String method,
        int range) {}
