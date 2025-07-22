package gg.nextforge.ui.builder;

import gg.nextforge.ui.component.UIComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class InventoryBuilder {

    private final int size;
    private String title = "Inventory";
    private final Map<Integer, UIComponent> components = new HashMap<>();
    private Function<Player, String> dynamicTitle = null;

    public InventoryBuilder(int size) {
        this.size = size;
    }

    public InventoryBuilder title(String title) {
        this.title = title;
        return this;
    }

    public InventoryBuilder title(Function<Player, String> titleFunction) {
        this.dynamicTitle = titleFunction;
        return this;
    }

    public InventoryBuilder set(int slot, UIComponent component) {
        components.put(slot, component);
        return this;
    }

    public Inventory build(Player player) {
        String finalTitle = dynamicTitle != null ? dynamicTitle.apply(player) : title;
        Inventory inventory = Bukkit.createInventory(null, size, finalTitle);

        for (Map.Entry<Integer, UIComponent> entry : components.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().render(player));
        }

        return inventory;
    }

    public void open(Player player) {
        player.openInventory(build(player));
    }

    public Map<Integer, UIComponent> getComponents() {
        return components;
    }
}
