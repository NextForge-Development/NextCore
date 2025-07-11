package gg.nextforge.textblockitemdisplay;

import org.bukkit.Material;

/**
 * Represents a hologram displaying a block.
 */
public interface BlockHologram extends Hologram {
    /**
     * @return block type displayed by this hologram.
     */
    Material getBlockType();

    /**
     * Sets block type.
     *
     * @param material material to display
     */
    void setBlockType(Material material);
}
