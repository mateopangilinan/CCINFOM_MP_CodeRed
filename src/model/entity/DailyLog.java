package model.entity;

import java.time.LocalDate;

/**
 * DailyLog.java
 *
 * Holds one daily check-in, including its owner, date, flow, mood, pain score and notes.
 * Symptoms refer to this record through its log ID.
 */
public record DailyLog(
        int id, int userId, LocalDate date, String flow, String mood, int pain, String notes) {
    @Override
    public String toString() {
        return "Log #" + id + " | User #" + userId + " | " + date;
    }
}
