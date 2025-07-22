package gg.nextforge.debug;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages which players or scopes have debugging enabled.
 */
public class DebugFlagStore {

    private final Set<UUID> enabledPlayers = new HashSet<>();
    private boolean globalDebugEnabled = false;

    public boolean isEnabled(Player player) {
        return globalDebugEnabled || enabledPlayers.contains(player.getUniqueId());
    }

    public void enableFor(Player player) {
        enabledPlayers.add(player.getUniqueId());
    }

    public void disableFor(Player player) {
        enabledPlayers.remove(player.getUniqueId());
    }

    public void setGlobal(boolean enabled) {
        globalDebugEnabled = enabled;
    }

    public boolean isGlobalEnabled() {
        return globalDebugEnabled;
    }

    public void clear() {
        enabledPlayers.clear();
        globalDebugEnabled = false;
    }
}
