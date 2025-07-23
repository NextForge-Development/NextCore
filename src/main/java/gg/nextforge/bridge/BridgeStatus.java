package gg.nextforge.bridge;

/**
 * Represents the current status of a plugin bridge.
 */
public enum BridgeStatus {

    /** Plugin not found or not supported */
    MISSING,

    /** Plugin found but initialization failed */
    FAILED,

    /** Plugin found and bridge initialized successfully */
    ENABLED,

    /** Bridge not yet initialized */
    UNINITIALIZED;

    public boolean isReady() {
        return this == ENABLED;
    }

    public boolean isMissingOrFailed() {
        return this == MISSING || this == FAILED;
    }
}
