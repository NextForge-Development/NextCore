package gg.nextforge.ui.inventory;

import gg.nextforge.ui.action.ClickAction;
import gg.nextforge.ui.component.UIButton;
import gg.nextforge.ui.component.UIComponent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Utility class to build navigation bars (prev/next/back).
 */
public class NavigationBar {

    private final Map<Integer, UIComponent> buttons = new HashMap<>();

    public NavigationBar backButton(int slot, BiConsumer<Player, ClickType> handler) {
        buttons.put(slot, new UIButton(() -> createItem(Material.ARROW, "§cBack"), handler::accept));
        return this;
    }

    public NavigationBar nextButton(int slot, BiConsumer<Player, ClickType> handler) {
        buttons.put(slot, new UIButton(() -> createItem(Material.ARROW, "§aNext"), handler::accept));
        return this;
    }

    public NavigationBar closeButton(int slot, BiConsumer<Player, ClickType> handler) {
        buttons.put(slot, new UIButton(() -> createItem(Material.BARRIER, "§4Close"), handler::accept));
        return this;
    }

    public Map<Integer, UIComponent> build() {
        return buttons;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }
}
