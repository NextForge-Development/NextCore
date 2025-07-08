package gg.nextforge.protocol.listener;

/**
 * Interface for hot-reloadable listeners in the protocol system.
 * This interface defines methods for enabling, disabling,
 * reloading, and checking the status of listeners
 * that can be dynamically reloaded
 * without restarting the server.
 */
public interface HotReloadable {

    /**
     * Enable this listener.
     * Called when the listener should start processing packets.
     */
    void enable();

    /**
     * Disable this listener.
     * Called when the listener should stop processing packets.
     */
    void disable();

    /**
     * Reload this listener's configuration.
     * This method is called to refresh the listener's settings
     * without needing to disable and re-enable it.
     */
    void reload();

    /**
     * Check if this listener is currently enabled.
     *
     * @return true if the listener is enabled, false otherwise.
     */
    boolean isEnabled();
}