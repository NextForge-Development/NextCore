package gg.nextforge.ui.listener;

import gg.nextforge.ui.component.UIComponent;
import gg.nextforge.ui.component.action.ClickType;
import gg.nextforge.ui.component.action.UIActionContext;
import gg.nextforge.ui.component.impl.UIButtonComponent;
import gg.nextforge.ui.event.impl.UIButtonClickEvent;
import gg.nextforge.ui.inventory.UI;
import gg.nextforge.ui.inventory.impl.StaticUI;
import gg.nextforge.ui.inventory.impl.paged.PaginatedUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.UUID;

public class InventoryClickListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || !event.getView().getTopInventory().equals(event.getClickedInventory())) return;

        UUID uuid = player.getUniqueId();
        UI currentUI = getCurrentUI(player); // <- diese Methode musst du ggf. via UIManager bereitstellen

        if (currentUI == null) return;

        int slot = event.getSlot();
        UIComponent component = null;

        if (currentUI instanceof StaticUI staticUI) {
            component = staticUI.getComponents().get(slot);
        } else if (currentUI instanceof PaginatedUI paginatedUI) {
            int currentPage = paginatedUI.getCurrentPage(player);
            component = paginatedUI.getPage(currentPage).getComponent(slot);
        }

        if (component == null) return;

        event.setCancelled(true); // UI = immer canceln (Standardverhalten)

        if (component instanceof UIButtonComponent button) {
            ClickType clickType = mapClickType(event);

            UIButtonClickEvent clickEvent = new UIButtonClickEvent(player, currentUI, button, clickType, event);
            Bukkit.getPluginManager().callEvent(clickEvent);

            if (!clickEvent.isCancelled()) {
                button.handleClick(new UIActionContext(player, button, clickType, event, currentUI));
            }
        }
    }

    private ClickType mapClickType(InventoryClickEvent event) {
        return switch (event.getClick()) {
            case LEFT -> ClickType.LEFT;
            case RIGHT -> ClickType.RIGHT;
            case SHIFT_LEFT -> ClickType.SHIFT_LEFT;
            case SHIFT_RIGHT -> ClickType.SHIFT_RIGHT;
            case MIDDLE -> ClickType.MIDDLE;
            case DROP -> ClickType.DROP;
            case DOUBLE_CLICK -> ClickType.DOUBLE_CLICK;
            default -> ClickType.UNKNOWN;
        };
    }

    private UI getCurrentUI(Player player) {
        // Muss via UIManager oder anderer globaler Zuordnung implementiert werden
        // Beispiel:
        // return UIManager.getInstance().getUIForPlayer(player);
        return null;
    }
}
