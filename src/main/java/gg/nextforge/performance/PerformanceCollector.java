package gg.nextforge.performance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Collects performance snapshots and aggregates statistics per key.
 */
public class PerformanceCollector {

    private final ConcurrentMap<String, List<PerformanceSnapshot>> data = new ConcurrentHashMap<>();

    public void record(PerformanceSnapshot snapshot) {
        data.computeIfAbsent(snapshot.getKey(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(snapshot);
    }

    public List<PerformanceSnapshot> getSnapshots(String key) {
        return data.getOrDefault(key, List.of());
    }

    public long getAverageMillis(String key) {
        List<PerformanceSnapshot> snapshots = getSnapshots(key);
        if (snapshots.isEmpty()) return -1;

        long total = 0;
        for (PerformanceSnapshot snapshot : snapshots) {
            total += snapshot.getDurationMillis();
        }
        return total / snapshots.size();
    }

    public int getCount(String key) {
        return getSnapshots(key).size();
    }

    public void clear(String key) {
        data.remove(key);
    }

    public void clearAll() {
        data.clear();
    }

    public List<String> getTrackedKeys() {
        return new ArrayList<>(data.keySet());
    }
}
