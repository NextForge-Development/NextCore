package gg.nextforge.ui.component.action;

import gg.nextforge.ui.inventory.UI;
import gg.nextforge.ui.component.UIComponent;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Provides context for a UI click action.
 */
public record UIActionContext(Player player, UIComponent component, ClickType clickType,
                              InventoryClickEvent originalEvent, UI ui) {

}
