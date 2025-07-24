package gg.nextforge.ui.listener;

import gg.nextforge.ui.UIManager;
import gg.nextforge.ui.component.UIComponent;
import gg.nextforge.ui.inventory.UI;
import gg.nextforge.ui.inventory.impl.StaticUI;
import gg.nextforge.ui.inventory.impl.paged.PaginatedUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Map;

public class InventoryCloseListener implements Listener {

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        UI ui = UIManager.getInstance().getCurrentUI(player);
        switch (ui) {
            case null -> {
                return;
            }
            case StaticUI staticUI -> staticUI.getComponents().values().forEach(UIComponent::onDetach);
            case PaginatedUI paginatedUI -> {
                int page = paginatedUI.getCurrentPage(player);
                Map<Integer, UIComponent> comps = paginatedUI.getPage(page).getAll();
                comps.values().forEach(UIComponent::onDetach);
            }
            default -> {
            }
        }

        UIManager.getInstance().closeUI(player);
    }
}
