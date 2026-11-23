package model.entity;

import java.time.LocalDate;

public record Cycle(
        int id, 
        int userId, 
        LocalDate start, 
        LocalDate end, 
        Integer length, 
        String notes) {}
