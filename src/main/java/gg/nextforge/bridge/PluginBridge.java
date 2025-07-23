package gg.nextforge.bridge;

import gg.nextforge.bridge.BridgeStatus;
import org.bukkit.plugin.Plugin;

/**
 * Base class for defining plugin bridges.
 * Handles lifecycle and dependency check.
 */
public abstract class PluginBridge {

    private BridgeStatus status = BridgeStatus.UNINITIALIZED;

    /**
     * Name of the external plugin this bridge supports.
     */
    public abstract String getPluginName();

    /**
     * Called if the plugin is available and the bridge is loaded.
     * @param plugin the detected plugin instance
     */
    public abstract void onEnable(Plugin plugin);

    /**
     * Called when the bridge should shut down or disconnect.
     */
    public void onDisable() {}

    public final void setStatus(BridgeStatus status) {
        this.status = status;
    }

    public final BridgeStatus getStatus() {
        return status;
    }

    public final boolean isEnabled() {
        return status == BridgeStatus.ENABLED;
    }

    public final boolean isFailed() {
        return status == BridgeStatus.FAILED;
    }
}
