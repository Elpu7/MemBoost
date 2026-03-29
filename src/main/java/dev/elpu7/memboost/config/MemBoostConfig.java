package dev.elpu7.memboost.config;

public final class MemBoostConfig {

    public static final int[] SAMPLE_INTERVAL_OPTIONS = {5, 10, 20, 40, 100};
    public static final int[] WARNING_THRESHOLD_OPTIONS = {60, 70, 75, 80, 85, 90, 95};

    private OptimizationProfile profile = OptimizationProfile.BALANCED;
    private boolean hudEnabled = false;
    private int sampleIntervalTicks = 20;
    private int warningThresholdPercent = 80;

    public OptimizationProfile getProfile() {
        return this.profile;
    }

    public void setProfile(OptimizationProfile profile) {
        this.profile = profile == null ? OptimizationProfile.BALANCED : profile;
    }

    public boolean isHudEnabled() {
        return this.hudEnabled;
    }

    public void setHudEnabled(boolean hudEnabled) {
        this.hudEnabled = hudEnabled;
    }

    public int getSampleIntervalTicks() {
        return this.sampleIntervalTicks;
    }

    public void setSampleIntervalTicks(int sampleIntervalTicks) {
        this.sampleIntervalTicks = clamp(sampleIntervalTicks, 1, 1200);
    }

    public int getWarningThresholdPercent() {
        return this.warningThresholdPercent;
    }

    public void setWarningThresholdPercent(int warningThresholdPercent) {
        this.warningThresholdPercent = clamp(warningThresholdPercent, 50, 95);
    }

    public void cycleProfile() {
        this.profile = this.profile.next();
    }

    public void cycleSampleIntervalTicks() {
        this.sampleIntervalTicks = nextOption(this.sampleIntervalTicks, SAMPLE_INTERVAL_OPTIONS);
    }

    public void cycleWarningThresholdPercent() {
        this.warningThresholdPercent = nextOption(this.warningThresholdPercent, WARNING_THRESHOLD_OPTIONS);
    }

    public void sanitize() {
        setProfile(this.profile);
        setSampleIntervalTicks(this.sampleIntervalTicks);
        setWarningThresholdPercent(this.warningThresholdPercent);
    }

    private static int nextOption(int currentValue, int[] options) {
        for (int i = 0; i < options.length; i++) {
            if (options[i] == currentValue) {
                return options[(i + 1) % options.length];
            }
        }

        return options[0];
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
