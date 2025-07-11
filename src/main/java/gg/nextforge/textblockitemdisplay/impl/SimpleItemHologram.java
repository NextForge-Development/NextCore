package gg.nextforge.textblockitemdisplay.impl;

import gg.nextforge.textblockitemdisplay.ItemHologram;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

/**
 * Basic implementation of an item hologram.
 */
public class SimpleItemHologram extends AbstractHologram implements ItemHologram {
    private ItemStack item;

    public SimpleItemHologram(String name, Location location, ItemStack item) {
        super(name, location);
        this.item = item.clone();
    }

    @Override
    public ItemStack getItem() {
        return item.clone();
    }

    @Override
    public void setItem(ItemStack item) {
        this.item = item.clone();
    }
}
