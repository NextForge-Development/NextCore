package gg.nextforge.ui.layout;

import gg.nextforge.ui.component.UIComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layout for paginating components across multiple pages.
 */
public class PaginationLayout implements UILayout {

    private final List<Integer> slotPositions;
    private final List<UIComponent> components;
    private final int page;

    public PaginationLayout(List<Integer> slotPositions, List<UIComponent> components, int page) {
        this.slotPositions = slotPositions;
        this.components = components;
        this.page = page;
    }

    @Override
    public Map<Integer, UIComponent> layout() {
        Map<Integer, UIComponent> result = new HashMap<>();

        int itemsPerPage = slotPositions.size();
        int startIndex = page * itemsPerPage;

        for (int i = 0; i < itemsPerPage; i++) {
            int componentIndex = startIndex + i;
            if (componentIndex >= components.size()) break;

            int slot = slotPositions.get(i);
            UIComponent component = components.get(componentIndex);

            result.put(slot, component);
        }

        return result;
    }
}
