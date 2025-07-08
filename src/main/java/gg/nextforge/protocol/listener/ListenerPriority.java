package gg.nextforge.protocol.listener;

/**
 * Enum representing the priority levels for event listeners.
 * Each level corresponds to a specific order in which listeners are executed.
 * The priorities are used to control the order of listener execution,
 * allowing for fine-tuned control over event handling.
 */
public enum ListenerPriority {
    LOWEST(0),
    LOW(1),
    NORMAL(2),
    HIGH(3),
    HIGHEST(4),
    MONITOR(5);

    private final int value;

    ListenerPriority(int value) {
        this.value = value;
    }

    /**
     * Retrieves the integer value associated with this priority level.
     *
     * @return The integer value of the priority.
     */
    public int getValue() {
        return value;
    }
}