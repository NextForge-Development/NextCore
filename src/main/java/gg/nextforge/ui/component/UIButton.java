package gg.nextforge.ui.component;

import gg.nextforge.ui.action.ClickAction;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

public class UIButton implements UIComponent {

    private final Supplier<ItemStack> itemSupplier;
    private final ClickAction clickAction;

    public UIButton(Supplier<ItemStack> itemSupplier, ClickAction clickAction) {
        this.itemSupplier = itemSupplier;
        this.clickAction = clickAction;
    }

    @Override
    public ItemStack render(Player player) {
        return itemSupplier.get();
    }

    @Override
    public void onClick(Player player, ClickType clickType) {
        if (clickAction != null) {
            clickAction.execute(player, clickType);
        }
    }
}
