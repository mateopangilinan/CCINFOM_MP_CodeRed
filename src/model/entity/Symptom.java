package model.entity;

/**
 * Symptom.java
 *
 * Holds a symptom type and severity linked to a daily log. Ownership is determined through that
 * log rather than a separate user field.
 */
public record Symptom(int id, int logId, int userId, String type, int severity) {}
