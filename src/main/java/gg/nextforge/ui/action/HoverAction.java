package gg.nextforge.ui.action;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Optional interface for implementing item hover behavior,
 * such as tooltip updates based on player state or localization.
 */
@FunctionalInterface
public interface HoverAction {

    /**
     * Render the item stack for the given player.
     * @param viewer the player viewing the item
     * @return the ItemStack to be displayed
     */
    ItemStack render(Player viewer);
}

