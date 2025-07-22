package gg.nextforge.ui.component;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * A composite component that holds other components in specific slots.
 */
public class UIPanel implements UIComponent {

    private final Map<Integer, UIComponent> children = new HashMap<>();

    public void setComponent(int slot, UIComponent component) {
        children.put(slot, component);
    }

    public UIComponent getComponent(int slot) {
        return children.get(slot);
    }

    public Map<Integer, UIComponent> getChildren() {
        return children;
    }

    @Override
    public ItemStack render(Player viewer) {
        return null; // Panels don't render to a single item
    }

    @Override
    public void onClick(Player viewer, ClickType click) {
        // Panels don't handle clicks directly
    }

    public void renderToInventory(org.bukkit.inventory.Inventory inventory, Player viewer) {
        children.forEach((slot, component) -> {
            inventory.setItem(slot, component.render(viewer));
        });
    }

    public void handleClick(Player player, int slot, ClickType clickType) {
        UIComponent comp = children.get(slot);
        if (comp != null) comp.onClick(player, clickType);
    }
}
