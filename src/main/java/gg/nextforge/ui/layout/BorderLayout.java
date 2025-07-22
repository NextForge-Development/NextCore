package gg.nextforge.ui.layout;

import gg.nextforge.ui.component.UIComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple layout to fill border slots in an inventory.
 */
public class BorderLayout implements UILayout {

    private final int rows;
    private final UIComponent borderComponent;

    public BorderLayout(int rows, UIComponent borderComponent) {
        this.rows = rows;
        this.borderComponent = borderComponent;
    }

    @Override
    public Map<Integer, UIComponent> layout() {
        Map<Integer, UIComponent> result = new HashMap<>();
        int columns = 9;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                boolean isBorder = row == 0 || row == rows - 1 || col == 0 || col == columns - 1;
                if (isBorder) {
                    int slot = row * columns + col;
                    result.put(slot, borderComponent);
                }
            }
        }

        return result;
    }
}
