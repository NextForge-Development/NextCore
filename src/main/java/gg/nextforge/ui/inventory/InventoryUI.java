package gg.nextforge.ui.inventory;

import gg.nextforge.ui.context.UIContextManager;
import gg.nextforge.ui.component.UIComponent;
import gg.nextforge.ui.context.PlayerUIContext;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public abstract class InventoryUI {

    private final int size;
    private final String title;

    public InventoryUI(int size, String title) {
        this.size = size;
        this.title = title;
    }

    /**
     * Called when the UI is opened. Use this to fill items.
     */
    protected abstract void render(Player player, Inventory inventory, PlayerUIContext context);

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, size, MiniMessage.miniMessage().deserialize(title));
        PlayerUIContext context = UIContextManager.get(player);
        context.setCurrentInventory(this);
        context.getActiveComponents().clear();

        render(player, inventory, context);
        player.openInventory(inventory);
    }

    public int getSize() {
        return size;
    }

    public String getTitle() {
        return title;
    }
}
