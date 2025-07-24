package gg.nextforge.ui.inventory.impl;

import gg.nextforge.ui.inventory.UI;
import gg.nextforge.ui.component.UIComponent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;

@Getter
@Setter
public class StaticUI implements UI {

    private final Map<Integer, UIComponent> components = new HashMap<>();
    private final Set<UUID> viewers = new HashSet<>();
    private Component title;
    private int size; // Must be multiple of 9 (e.g. 9, 18, 27, ..., 54)

    public StaticUI(Component title, int size) {
        this.title = title;
        this.size = size;
    }

    @Override
    public Component title(Component newTitle) {
        this.title = newTitle;
        return this.title;
    }

    @Override
    public Component title() {
        return this.title;
    }

    public void addComponent(UIComponent component) {
        components.put(component.getSlot(), component);
        component.onAttach(this);
    }

    public void removeComponent(int slot) {
        UIComponent comp = components.remove(slot);
        if (comp != null) comp.onDetach();
    }

    @Override
    public void open(Audience... audiences) {
        for (Audience audience : audiences) {
            if (audience instanceof Player player) {
                Inventory inv = Bukkit.createInventory((InventoryHolder) null, size, title);
                components.forEach((slot, comp) -> inv.setItem(slot, comp.render()));
                player.openInventory(inv);
                viewers.add(player.getUniqueId());
            }
        }
    }

    @Override
    public void close(Audience... audiences) {
        for (Audience audience : audiences) {
            if (audience instanceof Player player) {
                player.closeInventory();
                viewers.remove(player.getUniqueId());
            }
        }
    }

    @Override
    public void closeAll() {
        for (UUID uuid : new HashSet<>(viewers)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.closeInventory();
        }
        viewers.clear();
    }

    public void update() {
        for (UUID uuid : viewers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.getOpenInventory() != null) {
                Inventory inv = player.getOpenInventory().getTopInventory();
                components.forEach((slot, comp) -> inv.setItem(slot, comp.render()));
            }
        }
    }
}