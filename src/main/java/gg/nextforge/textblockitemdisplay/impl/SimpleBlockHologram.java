package gg.nextforge.textblockitemdisplay.impl;

import gg.nextforge.textblockitemdisplay.BlockHologram;
import org.bukkit.Location;
import org.bukkit.Material;

/**
 * Basic implementation of a block hologram.
 */
public class SimpleBlockHologram extends AbstractHologram implements BlockHologram {
    private Material material;

    public SimpleBlockHologram(String name, Location location, Material material) {
        super(name, location);
        this.material = material;
    }

    @Override
    public Material getBlockType() {
        return material;
    }

    @Override
    public void setBlockType(Material material) {
        this.material = material;
    }
}
