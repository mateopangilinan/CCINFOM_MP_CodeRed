package model.entity;

import java.time.LocalDate;

public record DailyLog(
        int id, 
        int userId, 
        LocalDate date, 
        String flow, 
        String mood, 
        int pain, 
        String notes) {
    @Override
    public String toString() {
        return "Log #" + id + " | User #" + userId + " | " + date;
    }
}
