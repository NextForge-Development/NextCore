package gg.nextforge.ui.action;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

/**
 * Represents an action that can be executed when a player clicks on a UI element.
 * This interface defines a single method to handle click events with the player and the type of click.
 */
@FunctionalInterface
public interface ClickAction {

    /**
     * Executes the action when a player clicks on a UI element.
     *
     * @param player the player who clicked
     * @param clickType the type of click (e.g., LEFT, RIGHT)
     */
    void execute(Player player, ClickType clickType);
}

