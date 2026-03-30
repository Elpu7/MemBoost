package dev.elpu7.memboost.config;

public enum MemBoostPreset {
    PLAY("play", "Play"),
    OBSERVE("observe", "Observe"),
    STRESS("stress", "Stress");

    private final String id;
    private final String displayName;

    MemBoostPreset(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public static MemBoostPreset fromId(String id) {
        if (id == null || id.isBlank()) {
            return PLAY;
        }

        for (MemBoostPreset preset : values()) {
            if (preset.id.equalsIgnoreCase(id)) {
                return preset;
            }
        }

        return PLAY;
    }
}
