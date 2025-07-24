package gg.nextforge.ui.animation;

import org.bukkit.entity.Player;

/**
 * Interface for UI components that support animations.
 */
public interface UIAnimated {
    void tick(UIAnimationContext context);
}
