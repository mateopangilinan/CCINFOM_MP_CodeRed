package model.entity;

/**
 * Choices.java
 *
 * Defines the allowed flow, mood and symptom choices shared by forms and validation. Keeping
 * these values together helps the interface and input checks agree.
 */
public final class Choices {
    private Choices() {}

    public static final String[] FLOWS = {"none", "light", "medium", "heavy"};
    public static final String[] MOODS = {"happy", "calm", "irritable", "anxious", "sad"};
    public static final String[] SYMPTOMS = {
        "cramps", "headache", "bloating", "fatigue", "nausea", "backache"
    };
}
