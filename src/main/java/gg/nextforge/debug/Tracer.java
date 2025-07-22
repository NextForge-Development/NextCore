package gg.nextforge.debug;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight tracer to measure and log elapsed time between steps.
 */
public class Tracer {

    private final DebugScope scope;
    private final String label;
    private final Map<String, Long> timestamps = new HashMap<>();

    public Tracer(DebugScope scope, String label) {
        this.scope = scope;
        this.label = label;
        mark("start");
    }

    public void mark(String step) {
        timestamps.put(step, System.nanoTime());
    }

    public void log(String step) {
        if (!timestamps.containsKey("start")) return;
        long now = System.nanoTime();
        long duration = now - timestamps.get("start");
        DebugManager.log(scope, label + " [" + step + "] took " + (duration / 1_000_000.0) + "ms");
    }

    public void logStep(String from, String to) {
        if (!timestamps.containsKey(from) || !timestamps.containsKey(to)) return;
        long duration = timestamps.get(to) - timestamps.get(from);
        DebugManager.log(scope, label + " [" + from + " -> " + to + "] " + (duration / 1_000_000.0) + "ms");
    }

    public void logAll() {
        String lastKey = null;
        for (String key : timestamps.keySet()) {
            if (lastKey != null) {
                logStep(lastKey, key);
            }
            lastKey = key;
        }
    }
}