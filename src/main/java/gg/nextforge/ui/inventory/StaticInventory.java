package gg.nextforge.ui.inventory;

import gg.nextforge.ui.component.UIComponent;
import gg.nextforge.ui.context.PlayerUIContext;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple inventory with fixed slot-to-component mapping.
 */
public class StaticInventory extends InventoryUI {

    private final Map<Integer, UIComponent> components = new HashMap<>();

    public StaticInventory(int size, String title) {
        super(size, title);
    }

    public StaticInventory set(int slot, UIComponent component) {
        components.put(slot, component);
        return this;
    }

    public Map<Integer, UIComponent> getComponents() {
        return components;
    }

    @Override
    protected void render(Player player, Inventory inventory, PlayerUIContext context) {
        for (Map.Entry<Integer, UIComponent> entry : components.entrySet()) {
            int slot = entry.getKey();
            UIComponent comp = entry.getValue();
            inventory.setItem(slot, comp.render(player));
            context.setComponent(slot, comp);
        }
    }
}
