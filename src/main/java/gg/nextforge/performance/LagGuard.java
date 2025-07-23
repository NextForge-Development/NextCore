package gg.nextforge.performance;

import java.util.function.Supplier;

/**
 * Utility to skip or throttle execution if lag threshold is exceeded.
 */
public class LagGuard {

    private static final long LAG_THRESHOLD_MS = 50;

    public static boolean isLagging() {
        TickProfiler profiler = PerformanceRegistry.getTickProfiler("main");
        return profiler.getLastTickDurationMillis() > LAG_THRESHOLD_MS;
    }

    public static void ifNotLagging(Runnable action) {
        if (!isLagging()) {
            action.run();
        }
    }

    public static <T> T supplyIfNotLagging(Supplier<T> supplier, T fallback) {
        return isLagging() ? fallback : supplier.get();
    }
}
