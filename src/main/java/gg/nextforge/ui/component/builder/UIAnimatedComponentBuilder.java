package gg.nextforge.ui.component.builder;

import gg.nextforge.ui.component.impl.AnimatedUIComponent;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class UIAnimatedComponentBuilder {

    private int slot;
    private String id;
    private Component tooltip;
    private int frameDelay = 5;

    private final List<ItemStack> frames = new ArrayList<>();

    public static UIAnimatedComponentBuilder create() {
        return new UIAnimatedComponentBuilder();
    }

    public UIAnimatedComponentBuilder slot(int slot) {
        this.slot = slot;
        return this;
    }

    public UIAnimatedComponentBuilder id(String id) {
        this.id = id;
        return this;
    }

    public UIAnimatedComponentBuilder tooltip(Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public UIAnimatedComponentBuilder frameDelay(int ticks) {
        this.frameDelay = ticks;
        return this;
    }

    public UIAnimatedComponentBuilder addFrame(ItemStack item) {
        this.frames.add(item);
        return this;
    }

    public UIAnimatedComponentBuilder addFrames(ItemStack... items) {
        this.frames.addAll(List.of(items));
        return this;
    }

    public AnimatedUIComponent build() {
        AnimatedUIComponent component = new AnimatedUIComponent(slot, frames);
        component.setTooltip(tooltip);
        component.setId(id);
        component.setFrameDelay(frameDelay);
        return component;
    }
}