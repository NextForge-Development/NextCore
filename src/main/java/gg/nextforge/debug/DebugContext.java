package gg.nextforge.debug;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a debug logging session for a specific topic or action.
 */
public class DebugContext {

    private final DebugScope scope;
    private final String label;
    private final List<String> lines = new ArrayList<>();

    public DebugContext(DebugScope scope, String label) {
        this.scope = scope;
        this.label = label;
    }

    public void add(String message) {
        lines.add(message);
    }

    public void logNow() {
        for (String line : lines) {
            DebugManager.log(scope, label + ": " + line);
        }
    }

    public void clear() {
        lines.clear();
    }

    public void logWithPrefix(String prefix) {
        for (String line : lines) {
            DebugManager.log(scope, prefix + " :: " + label + ": " + line);
        }
    }
}
