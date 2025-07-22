package gg.nextforge.ui.support;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Provides temporary hotbar-based interaction.
 */
public class HotbarUI {

    private static final Map<UUID, Map<Integer, BiConsumer<Player, ItemStack>>> hotbarActions = new HashMap<>();

    public static void setSlot(Player player, int slot, ItemStack item, BiConsumer<Player, ItemStack> action) {
        player.getInventory().setItem(slot, item);
        hotbarActions.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>())
                .put(slot, action);
    }

    public static void registerListener(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {

            @org.bukkit.event.EventHandler
            public void onInteract(PlayerInteractEvent event) {
                Player player = event.getPlayer();
                int slot = player.getInventory().getHeldItemSlot();
                Map<Integer, BiConsumer<Player, ItemStack>> actions = hotbarActions.get(player.getUniqueId());
                if (actions != null && actions.containsKey(slot)) {
                    event.setCancelled(true);
                    ItemStack item = player.getInventory().getItem(slot);
                    actions.get(slot).accept(player, item);
                }
            }

            @org.bukkit.event.EventHandler
            public void onSwitch(PlayerItemHeldEvent event) {
                // Optional: clear hotbar on slot switch if desired
            }

        }, plugin);
    }

    public static void clear(Player player) {
        hotbarActions.remove(player.getUniqueId());
    }
}
