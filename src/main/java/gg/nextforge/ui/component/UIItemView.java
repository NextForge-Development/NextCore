package gg.nextforge.ui.component;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.function.Function;

/**
 * A non-interactive UI component used to display static or dynamic items.
 */
public class UIItemView implements UIComponent {

    private final Function<Player, ItemStack> itemProvider;

    public UIItemView(ItemStack item) {
        this(p -> item);
    }

    public UIItemView(Function<Player, ItemStack> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public ItemStack render(Player viewer) {
        return itemProvider.apply(viewer);
    }

    @Override
    public void onClick(Player viewer, ClickType click) {
        // Intentionally no-op (passive component)
    }
}
