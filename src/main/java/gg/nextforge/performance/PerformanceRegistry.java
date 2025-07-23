package gg.nextforge.performance;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all performance tracking components.
 */
public class PerformanceRegistry {

    private static final Map<String, PerformanceCollector> collectors = new ConcurrentHashMap<>();
    private static final Map<String, TickProfiler> tickProfilers = new ConcurrentHashMap<>();
    private static final Map<String, TaskProfiler> taskProfilers = new ConcurrentHashMap<>();

    public static PerformanceCollector getCollector(String key) {
        return collectors.computeIfAbsent(key, k -> new PerformanceCollector());
    }

    public static TickProfiler getTickProfiler(String key) {
        return tickProfilers.computeIfAbsent(key, k -> new TickProfiler());
    }

    public static TaskProfiler getTaskProfiler(String key) {
        return taskProfilers.computeIfAbsent(key, k -> new TaskProfiler(getCollector(key)));
    }

    public static void clearAll() {
        collectors.clear();
        tickProfilers.clear();
        taskProfilers.clear();
    }
}
