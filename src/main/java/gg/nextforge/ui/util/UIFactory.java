package gg.nextforge.ui.util;

import gg.nextforge.ui.component.builder.UIClickableComponentBuilder;
import gg.nextforge.ui.inventory.UI;
import gg.nextforge.ui.inventory.UIBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class UIFactory {

    public static UI createSimpleMenu(Component title, int size, Consumer<UIBuilder> builderConsumer) {
        UIBuilder builder = UIBuilder.create().title(title).size(size);
        builderConsumer.accept(builder);
        return builder.build();
    }
}
