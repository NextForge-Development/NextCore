package gg.nextforge.textblockitemdisplay;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link HologramManager}.
 */
public class HologramManagerTest {

    private HologramManager manager;
    private World world;

    @BeforeEach
    void setUp() {
        manager = new HologramManager();
        world = mock(World.class);
        when(world.getName()).thenReturn("world");
    }

    @Test
    void createTextHologram() {
        Location loc = new Location(world, 0, 0, 0);
        TextHologram holo = manager.createTextHologram("test", loc);
        assertNotNull(holo);
        assertEquals(holo, manager.get("test"));
    }

    @Test
    void copyItemHologram() {
        Location loc = new Location(world, 1, 2, 3);
        ItemHologram ih = manager.createItemHologram("i", loc, new ItemStack(Material.STONE));
        manager.createItemHologram("copy", ih.getLocation(), ih.getItem());
        assertEquals(2, manager.getHolograms().size());
    }
}
