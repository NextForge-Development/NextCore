package gg.nextforge.ui.component.impl;

import gg.nextforge.ui.animation.UIAnimated;
import gg.nextforge.ui.animation.UIAnimationContext;
import gg.nextforge.ui.component.impl.UIStaticComponent;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Setter
public class AnimatedUIComponent extends UIStaticComponent implements UIAnimated {

    private List<ItemStack> frames;
    private int frameDelay = 5; // in ticks (default: alle 5 Ticks)
    private boolean looping = true;

    public AnimatedUIComponent(int slot, List<ItemStack> frames) {
        super(slot, frames.getFirst());
        this.frames = frames;
    }

    @Override
    public void tick(UIAnimationContext context) {
        if (frames == null || frames.isEmpty()) return;

        int frameIndex = (context.getTick() / frameDelay) % frames.size();
        setItem(frames.get(frameIndex));
    }
}
