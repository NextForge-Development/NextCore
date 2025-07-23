package gg.nextforge.performance;

import java.util.List;
import java.util.StringJoiner;

/**
 * Utility to format and present performance data.
 */
public class PerformanceReport {

    public static String generate(PerformanceCollector collector) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Performance Report ===\n");

        List<String> keys = collector.getTrackedKeys();
        if (keys.isEmpty()) {
            sb.append("No data collected.\n");
            return sb.toString();
        }

        for (String key : keys) {
            int count = collector.getCount(key);
            long avg = collector.getAverageMillis(key);
            sb.append("- ").append(key)
                    .append(": ").append(count).append(" runs, ")
                    .append("avg ").append(avg).append(" ms\n");
        }

        return sb.toString();
    }

    public static String json(PerformanceCollector collector) {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        for (String key : collector.getTrackedKeys()) {
            joiner.add("\"" + key + "\":" + collector.getAverageMillis(key));
        }
        return joiner.toString();
    }
}
