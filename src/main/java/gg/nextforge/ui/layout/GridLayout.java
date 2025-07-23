package gg.nextforge.ui.layout;

import gg.nextforge.ui.component.UIComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Places components in a grid layout row-by-row.
 */
public class GridLayout implements UILayout {

    private final int startSlot;
    private final int columns;
    private final List<UIComponent> components;

    public GridLayout(int startSlot, int columns, List<UIComponent> components) {
        this.startSlot = startSlot;
        this.columns = columns;
        this.components = components;
    }

    @Override
    public Map<Integer, UIComponent> layout() {
        Map<Integer, UIComponent> result = new HashMap<>();
        int index = 0;

        for (UIComponent component : components) {
            int row = index / columns;
            int col = index % columns;
            int slot = startSlot + row * 9 + col;

            result.put(slot, component);
            index++;
        }

        return result;
    }
}
