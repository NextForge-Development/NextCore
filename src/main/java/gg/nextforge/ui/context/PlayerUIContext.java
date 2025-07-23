package gg.nextforge.ui.context;

import gg.nextforge.ui.component.UIComponent;
import gg.nextforge.ui.inventory.InventoryUI;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the UI state/session for a player.
 */
public class PlayerUIContext {

    private final UUID playerId;
    private InventoryUI currentInventory;
    private final Map<Integer, UIComponent> activeComponents = new HashMap<>();

    public PlayerUIContext(Player player) {
        this.playerId = player.getUniqueId();
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public InventoryUI getCurrentInventory() {
        return currentInventory;
    }

    public void setCurrentInventory(InventoryUI inventory) {
        this.currentInventory = inventory;
    }

    public void setComponent(int slot, UIComponent component) {
        activeComponents.put(slot, component);
    }

    public UIComponent getComponent(int slot) {
        return activeComponents.get(slot);
    }

    public Map<Integer, UIComponent> getActiveComponents() {
        return activeComponents;
    }

    public void clear() {
        currentInventory = null;
        activeComponents.clear();
    }
}
