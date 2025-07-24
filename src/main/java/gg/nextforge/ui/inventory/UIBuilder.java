package gg.nextforge.ui.inventory;

import gg.nextforge.ui.component.UIComponent;
import gg.nextforge.ui.inventory.impl.StaticUI;
import gg.nextforge.ui.inventory.impl.paged.PaginatedUI;
import net.kyori.adventure.text.Component;

import java.util.*;

/**
 * Fluent builder for building static or paginated UIs.
 */
public class UIBuilder {

    private Component title;
    private int size;
    private boolean paginated = false;

    private final List<UIComponent> components = new ArrayList<>();
    private final Map<Integer, List<UIComponent>> paginatedComponents = new HashMap<>();

    public static UIBuilder create() {
        return new UIBuilder();
    }

    public UIBuilder title(Component title) {
        this.title = title;
        return this;
    }

    public UIBuilder size(int size) {
        this.size = size;
        return this;
    }

    public UIBuilder paginated(boolean value) {
        this.paginated = value;
        return this;
    }

    public UIBuilder addComponent(UIComponent component) {
        this.components.add(component);
        return this;
    }

    public UIBuilder addComponentToPage(int page, UIComponent component) {
        this.paginated = true;
        paginatedComponents.computeIfAbsent(page, k -> new ArrayList<>()).add(component);
        return this;
    }

    public UI build() {
        if (paginated) {
            PaginatedUI ui = new PaginatedUI(title, size);
            for (Map.Entry<Integer, List<UIComponent>> entry : paginatedComponents.entrySet()) {
                for (UIComponent component : entry.getValue()) {
                    ui.addComponent(entry.getKey(), component);
                }
            }
            return ui;
        } else {
            StaticUI ui = new StaticUI(title, size);
            for (UIComponent component : components) {
                ui.addComponent(component);
            }
            return ui;
        }
    }
}
