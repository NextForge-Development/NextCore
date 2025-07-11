package gg.nextforge.textblockitemdisplay;

import org.bukkit.inventory.ItemStack;

/**
 * Represents a hologram displaying an item.
 */
public interface ItemHologram extends Hologram {
    /**
     * @return item displayed by the hologram.
     */
    ItemStack getItem();

    /**
     * Sets the item to display.
     *
     * @param item new item
     */
    void setItem(ItemStack item);
}
