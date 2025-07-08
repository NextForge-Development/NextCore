package gg.nextforge.event;

/**
 * Base class for all core events in the NextForge event system.
 * <p>
 * This class provides a mechanism to check and set the cancellation state of events.
 * It is intended to be extended by specific event classes to provide additional context
 * and functionality as needed.
 */
public abstract class CoreEvent {

    private boolean cancelled = false;

    /**
     * Checks if this event is cancelled.
     *
     * @return true if the event is cancelled, false otherwise.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Sets the cancellation state of this event.
     * @param cancelled true to cancel the event, false to allow it to proceed.
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}