package gg.nextforge.ui.context;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active UI contexts for all players.
 */
public class UIContextManager {

    private static final Map<UUID, PlayerUIContext> contextMap = new ConcurrentHashMap<>();

    public static PlayerUIContext get(Player player) {
        return contextMap.computeIfAbsent(player.getUniqueId(), id -> new PlayerUIContext(player));
    }

    public static void clear(Player player) {
        PlayerUIContext ctx = contextMap.remove(player.getUniqueId());
        if (ctx != null) ctx.clear();
    }

    public static void clearAll() {
        contextMap.values().forEach(PlayerUIContext::clear);
        contextMap.clear();
    }

    public static boolean has(Player player) {
        return contextMap.containsKey(player.getUniqueId());
    }

    public static Map<UUID, PlayerUIContext> getAll() {
        return contextMap;
    }
}
