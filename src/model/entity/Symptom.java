package model.entity;

public record Symptom(
    int id, 
    int logId, 
    int userId, 
    String type, 
    int severity) {}
