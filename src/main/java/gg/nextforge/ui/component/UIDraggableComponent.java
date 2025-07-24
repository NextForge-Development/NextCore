package gg.nextforge.ui.component;

import gg.nextforge.ui.component.action.UIActionContext;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Marker interface for components that support drag events.
 */
public interface UIDraggableComponent {

    /**
     * Handle drag input for this component.
     */
    void handleDrag(UIActionContext context, InventoryDragEvent event);
}
