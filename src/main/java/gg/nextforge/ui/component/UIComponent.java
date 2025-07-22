package gg.nextforge.ui.component;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

public interface UIComponent {

    ItemStack render(Player viewer);

    void onClick(Player viewer, ClickType click);

}
