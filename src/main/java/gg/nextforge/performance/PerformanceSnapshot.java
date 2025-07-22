package gg.nextforge.performance;

/**
 * Immutable data holder representing a single measurement.
 */
public class PerformanceSnapshot {

    private final String key;
    private final long startTimeNanos;
    private final long durationNanos;

    public PerformanceSnapshot(String key, long startTimeNanos, long durationNanos) {
        this.key = key;
        this.startTimeNanos = startTimeNanos;
        this.durationNanos = durationNanos;
    }

    public String getKey() {
        return key;
    }

    public long getStartTimeNanos() {
        return startTimeNanos;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public long getDurationMillis() {
        return durationNanos / 1_000_000;
    }

    @Override
    public String toString() {
        return key + " - " + getDurationMillis() + " ms";
    }
}