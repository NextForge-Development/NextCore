package gg.nextforge.performance;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a performance context (e.g., per player, per world).
 */
public class PerformanceContext {

    @Getter
    private final String name;
    private final Map<String, PerformanceCollector> scopedCollectors = new HashMap<>();

    public PerformanceContext(String name) {
        this.name = name;
    }

    public PerformanceCollector getCollector(String key) {
        return scopedCollectors.computeIfAbsent(key, k -> new PerformanceCollector());
    }

    public Map<String, PerformanceCollector> getAllCollectors() {
        return scopedCollectors;
    }

    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Performance Context: ").append(name).append(" ===\n");
        for (Map.Entry<String, PerformanceCollector> entry : scopedCollectors.entrySet()) {
            sb.append("-- ").append(entry.getKey()).append(" --\n");
            sb.append(PerformanceReport.generate(entry.getValue())).append("\n");
        }
        return sb.toString();
    }
}
