package gg.nextforge.textblockitemdisplay;

import gg.nextforge.textblockitemdisplay.impl.SimpleBlockHologram;
import gg.nextforge.textblockitemdisplay.impl.SimpleItemHologram;
import gg.nextforge.textblockitemdisplay.impl.SimpleTextHologram;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager responsible for creating and storing holograms.
 */
public class HologramManager {
    private final Map<String, Hologram> holograms = new ConcurrentHashMap<>();

    /**
     * Creates a text hologram.
     *
     * @param name     hologram name
     * @param location spawn location
     * @return created hologram
     */
    public TextHologram createTextHologram(String name, Location location) {
        TextHologram holo = new SimpleTextHologram(name, location);
        holograms.put(name.toLowerCase(), holo);
        return holo;
    }

    /**
     * Creates an item hologram.
     *
     * @param name     hologram name
     * @param location spawn location
     * @param item     item to display
     * @return created hologram
     */
    public ItemHologram createItemHologram(String name, Location location, ItemStack item) {
        ItemHologram holo = new SimpleItemHologram(name, location, item);
        holograms.put(name.toLowerCase(), holo);
        return holo;
    }

    /**
     * Creates a block hologram.
     *
     * @param name     hologram name
     * @param location spawn location
     * @param material block material
     * @return created hologram
     */
    public BlockHologram createBlockHologram(String name, Location location, Material material) {
        BlockHologram holo = new SimpleBlockHologram(name, location, material);
        holograms.put(name.toLowerCase(), holo);
        return holo;
    }

    /**
     * Remove a hologram by name.
     */
    public void remove(String name) {
        Hologram h = holograms.remove(name.toLowerCase());
        if (h != null) {
            h.despawn();
        }
    }

    /**
     * Get a hologram by name.
     */
    public Hologram get(String name) {
        return holograms.get(name.toLowerCase());
    }

    /**
     * @return list of all holograms.
     */
    public List<Hologram> getHolograms() {
        return new ArrayList<>(holograms.values());
    }
}
