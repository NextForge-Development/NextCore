package gg.nextforge.ui.component;

import gg.nextforge.ui.inventory.UI;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

@Getter
@Setter
public abstract class UIComponent {

    /**
     * Slot position in the inventory grid (0-53 for chest-type inventories).
     */
    private int slot;

    /**
     * Optional identifier for advanced tracking or events.
     */
    private String id;

    /**
     * Optional item stack representing the component's icon or content.
     */
    private Component tooltip;

    /**
     * The parent UI this component belongs to.
     */
    private transient UI parent;

    public UIComponent(int slot) {
        this.slot = slot;
    }

    /**
     * Generates the visual representation of this component.
     */
    public abstract ItemStack render();

    /**
     * Optional: Called when the component is added to a UI.
     */
    public void onAttach(UI parent) {
        this.parent = parent;
    }

    /**
     * Optional: Called when the component is removed or UI is closed.
     */
    public void onDetach() {
        this.parent = null;
    }

    /**
     * Called if this component needs to be updated manually.
     */
    public void update() {
        // Optional: override in subclasses
    }
}
