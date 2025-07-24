package gg.nextforge.ui.component.impl;

import gg.nextforge.ui.component.UIComponent;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

/**
 * A static UI component that is not interactable.
 */
@Getter
@Setter
public class UIStaticComponent extends UIComponent {

    private ItemStack item;

    public UIStaticComponent(int slot, ItemStack item) {
        super(slot);
        this.item = item;
    }

    @Override
    public ItemStack render() {
        return item;
    }
}
