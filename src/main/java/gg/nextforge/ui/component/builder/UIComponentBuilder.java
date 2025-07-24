package gg.nextforge.ui.component.builder;

import gg.nextforge.ui.component.UIComponent;
import gg.nextforge.ui.component.impl.UIStaticComponent;
import org.bukkit.inventory.ItemStack;

/**
 * Builder for general UI components (non-clickable).
 */
public class UIComponentBuilder {

    protected int slot;
    protected ItemStack item;
    protected String id;

    public static UIComponentBuilder create() {
        return new UIComponentBuilder();
    }

    public UIClickableComponentBuilder clickable() {
        return UIClickableComponentBuilder.create();
    }

    public UIComponentBuilder slot(int slot) {
        this.slot = slot;
        return this;
    }

    public UIComponentBuilder item(ItemStack item) {
        this.item = item;
        return this;
    }

    public UIComponentBuilder id(String id) {
        this.id = id;
        return this;
    }

    public UIComponent build() {
        UIStaticComponent component = new UIStaticComponent(slot, item);
        component.setId(id);
        return component;
    }
}
