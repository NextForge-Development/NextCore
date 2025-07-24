package gg.nextforge.ui.listener;

import gg.nextforge.ui.UIManager;
import gg.nextforge.ui.component.UIComponent;
import gg.nextforge.ui.component.UIDraggableComponent;
import gg.nextforge.ui.component.action.ClickType;
import gg.nextforge.ui.component.action.UIActionContext;
import gg.nextforge.ui.inventory.UI;
import gg.nextforge.ui.inventory.impl.StaticUI;
import gg.nextforge.ui.inventory.impl.paged.PaginatedUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;

public class InventoryDragListener implements Listener {

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        UI ui = UIManager.getInstance().getCurrentUI(player);
        if (ui == null) return;

        event.setCancelled(true);

        for (int slot : event.getRawSlots()) {
            UIComponent component = null;

            if (ui instanceof StaticUI staticUI) {
                component = staticUI.getComponents().get(slot);
            } else if (ui instanceof PaginatedUI paginatedUI) {
                int page = paginatedUI.getCurrentPage(player);
                component = paginatedUI.getPage(page).getComponent(slot);
            }

            if (component instanceof UIDraggableComponent draggable) {
                UIActionContext ctx = new UIActionContext(player, component, ClickType.UNKNOWN, null, ui);
                draggable.handleDrag(ctx, event);
            }
        }
    }
}
