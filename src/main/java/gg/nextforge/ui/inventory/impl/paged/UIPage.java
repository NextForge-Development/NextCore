package gg.nextforge.ui.inventory.impl.paged;

import gg.nextforge.ui.component.UIComponent;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single page in a PaginatedUI.
 */
@Getter
@Setter
public class UIPage {

    private final int index;
    private final Map<Integer, UIComponent> components = new HashMap<>();

    public UIPage(int index) {
        this.index = index;
    }

    public void addComponent(UIComponent component) {
        components.put(component.getSlot(), component);
        component.onAttach(null);
    }

    public void removeComponent(int slot) {
        UIComponent removed = components.remove(slot);
        if (removed != null) removed.onDetach();
    }

    public UIComponent getComponent(int slot) {
        return components.get(slot);
    }

    public Map<Integer, UIComponent> getAll() {
        return components;
    }
}
