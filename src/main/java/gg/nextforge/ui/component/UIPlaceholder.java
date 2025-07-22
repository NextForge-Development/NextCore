package gg.nextforge.ui.component;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * A static, non-interactive filler slot.
 */
public class UIPlaceholder implements UIComponent {

    private final ItemStack placeholder;

    public UIPlaceholder(Material material) {
        this.placeholder = new ItemStack(material);
        ItemMeta meta = this.placeholder.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            this.placeholder.setItemMeta(meta);
        }
    }

    public UIPlaceholder(ItemStack customItem) {
        this.placeholder = customItem;
    }

    @Override
    public ItemStack render(Player viewer) {
        return placeholder;
    }

    @Override
    public void onClick(Player viewer, ClickType click) {
        // No-op
    }
}
