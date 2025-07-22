package gg.nextforge.debug;

/**
 * Represents a logical debug category or module.
 */
public enum DebugScope {
    PERFORMANCE,
    DATABASE,
    UI,
    NETWORK,
    SCHEDULER,
    COMMAND,
    EVENT,
    REDIS,
    MONGO,
    INVENTORY,
    SECURITY,
    OTHER;

    public String displayName() {
        return name().toLowerCase();
    }
}
