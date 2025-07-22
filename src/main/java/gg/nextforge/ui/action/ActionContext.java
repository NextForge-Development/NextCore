package gg.nextforge.ui.action;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/**
 * Encapsulates relevant data for a UI action event.
 */
public class ActionContext {

    private final Player player;
    private final int slot;
    private final ClickType clickType;

    /**
     * Constructs an ActionContext with the specified player, slot, and click type.
     *
     * @param player    The player who performed the action.
     * @param slot      The inventory slot that was interacted with.
     * @param clickType The type of click that triggered the action.
     */
    public ActionContext(Player player, int slot, ClickType clickType) {
        this.player = player;
        this.slot = slot;
        this.clickType = clickType;
    }

    /**
     * Gets the player who performed the action.
     *
     * @return The player associated with this action context.
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the inventory slot that was interacted with.
     *
     * @return The slot number associated with this action context.
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Gets the type of click that triggered the action.
     *
     * @return The ClickType associated with this action context.
     */
    public ClickType getClickType() {
        return clickType;
    }
}
