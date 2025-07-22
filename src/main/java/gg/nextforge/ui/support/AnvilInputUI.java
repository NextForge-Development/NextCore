package gg.nextforge.ui.support;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.function.BiConsumer;

public class AnvilInputUI {

    private final Plugin plugin;
    private final String title;
    private final ItemStack insertItem;
    private final BiConsumer<Player, String> onComplete;

    public AnvilInputUI(Plugin plugin, String title, ItemStack insertItem, BiConsumer<Player, String> onComplete) {
        this.plugin = plugin;
        this.title = title;
        this.insertItem = insertItem;
        this.onComplete = onComplete;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, InventoryType.ANVIL, title);
        if (insertItem != null) {
            inventory.setItem(0, insertItem);
        }

        player.openInventory(inventory);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onClose(InventoryCloseEvent event) {
                    if (event.getPlayer().equals(player) && event.getInventory().getType() == InventoryType.ANVIL) {
                        String text = null;
                        if (event.getInventory() instanceof AnvilInventory anvil) {
                            ItemStack result = anvil.getItem(2);
                            if (result != null && result.hasItemMeta() && result.getItemMeta().hasDisplayName()) {
                                text = result.getItemMeta().getDisplayName();
                            }
                        }
                        onComplete.accept(player, text);
                        InventoryCloseEvent.getHandlerList().unregister(this);
                    }
                }
            }, plugin);
        }, 2L);
    }
}
