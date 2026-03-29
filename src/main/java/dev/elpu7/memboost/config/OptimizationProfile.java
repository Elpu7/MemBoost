package dev.elpu7.memboost.config;

public enum OptimizationProfile {
    SAFE("safe", "Safe"),
    BALANCED("balanced", "Balanced"),
    AGGRESSIVE("aggressive", "Aggressive");

    private final String id;
    private final String displayName;

    OptimizationProfile(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return this.id;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public OptimizationProfile next() {
        OptimizationProfile[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public static OptimizationProfile fromId(String id) {
        if (id == null || id.isBlank()) {
            return BALANCED;
        }

        for (OptimizationProfile profile : values()) {
            if (profile.id.equalsIgnoreCase(id)) {
                return profile;
            }
        }

        return BALANCED;
    }
}
