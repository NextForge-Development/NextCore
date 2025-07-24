package gg.nextforge.ui.animation;

import gg.nextforge.ui.inventory.UI;
import gg.nextforge.ui.component.UIComponent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

@Getter
@AllArgsConstructor
public class UIAnimationContext {
    private final Player player;
    private final UI ui;
    private final UIComponent component;
    private final int tick;
}
