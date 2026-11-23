package model.entity;

public final class Choices {
    private Choices() {}

    public static final String[] FLOWS = {"none", "light", "medium", "heavy"};
    public static final String[] MOODS = {"happy", "calm", "irritable", "anxious", "sad"};
    public static final String[] SYMPTOMS = {
        "cramps", "headache", "bloating", "fatigue", "nausea", "backache"
    };
}
