package gg.nextforge.ui.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class ComponentUtils {

    public static Component joinWithNewline(Component... components) {
        TextComponent.Builder builder = Component.text();

        for (int i = 0; i < components.length; i++) {
            builder.append(components[i]);
            if (i < components.length - 1) {
                builder.append(Component.newline());
            }
        }
        return builder.build();
    }
}