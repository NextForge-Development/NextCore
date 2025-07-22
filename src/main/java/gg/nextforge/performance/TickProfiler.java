package gg.nextforge.performance;

public class TickProfiler {

    private long lastTickTime = -1;
    private long lastTickDuration = -1;

    public void onTickStart() {
        lastTickTime = System.nanoTime();
    }

    public void onTickEnd() {
        if (lastTickTime > 0) {
            lastTickDuration = System.nanoTime() - lastTickTime;
        }
    }

    public long getLastTickDurationNanos() {
        return lastTickDuration;
    }

    public long getLastTickDurationMillis() {
        return lastTickDuration / 1_000_000;
    }

    public double getApproximateTPS() {
        if (lastTickDuration <= 0) return 20.0;
        double tickTimeMs = getLastTickDurationMillis();
        return Math.min(20.0, 1000.0 / tickTimeMs);
    }

    public String format() {
        return "Tick: " + getLastTickDurationMillis() + " ms | TPS: " + String.format("%.2f", getApproximateTPS());
    }
}
