package gg.nextforge.textblockitemdisplay;

import org.bukkit.Location;

/**
 * Base interface for all hologram types.
 */
public interface Hologram {
    /**
     * @return name of the hologram.
     */
    String getName();

    /**
     * Get the current location of the hologram.
     *
     * @return hologram location
     */
    Location getLocation();

    /**
     * Sets the location of this hologram.
     *
     * @param location new location
     */
    void setLocation(Location location);

    /**
     * Spawns the hologram.
     */
    void spawn();

    /**
     * Removes the hologram from the world.
     */
    void despawn();

    /**
     * @return whether the hologram is currently spawned
     */
    boolean isSpawned();
}
