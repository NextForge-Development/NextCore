package gg.nextforge.debug;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Central entry point for debug logging and state management.
 */
public class DebugManager {

    private static final DebugFlagStore flagStore = new DebugFlagStore();
    private static final Map<DebugScope, Consumer<String>> scopeLoggers = new EnumMap<>(DebugScope.class);

    static {
        for (DebugScope scope : DebugScope.values()) {
            scopeLoggers.put(scope, msg -> Bukkit.getLogger().info("[Debug:" + scope.displayName() + "] " + msg));
        }
    }

    public static void log(DebugScope scope, String message) {
        Consumer<String> logger = scopeLoggers.getOrDefault(scope, System.out::println);
        logger.accept(message);
    }

    public static boolean isEnabled(Player player) {
        return flagStore.isEnabled(player);
    }

    public static void enable(Player player) {
        flagStore.enableFor(player);
    }

    public static void disable(Player player) {
        flagStore.disableFor(player);
    }

    public static void setGlobal(boolean enabled) {
        flagStore.setGlobal(enabled);
    }

    public static boolean isGlobalEnabled() {
        return flagStore.isGlobalEnabled();
    }

    public static DebugFlagStore getFlagStore() {
        return flagStore;
    }
}
