package gg.nextforge.ui.component.impl;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

/**
 * A clickable button component in the UI.
 */
@Getter
@Setter
public class UIButtonComponent extends UIClickableComponent {

    private ItemStack icon;
    private Component tooltip;

    public UIButtonComponent(int slot, ItemStack icon) {
        super(slot);
        this.icon = icon;
    }

    public UIButtonComponent(int slot, ItemStack icon, Component tooltip) {
        super(slot);
        this.icon = icon;
        this.tooltip = tooltip;
    }

    @Override
    public ItemStack render() {
        return icon;
    }

    @Override
    public Component getTooltip() {
        return tooltip;
    }
}
