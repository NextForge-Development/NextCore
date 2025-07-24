package gg.nextforge.ui.event.impl;

import gg.nextforge.ui.component.action.ClickType;
import gg.nextforge.ui.component.impl.UIButtonComponent;
import gg.nextforge.ui.inventory.UI;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.Plugin;

@Getter
public class UIButtonClickEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final UIButtonComponent button;
    private final UI ui;
    private final ClickType clickType;
    private final InventoryClickEvent originalEvent;

    private boolean cancelled;

    public UIButtonClickEvent(Player player, UI ui, UIButtonComponent button,
                              ClickType clickType, InventoryClickEvent originalEvent) {
        super(player);
        this.ui = ui;
        this.button = button;
        this.clickType = clickType;
        this.originalEvent = originalEvent;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
        originalEvent.setCancelled(cancel);
    }
}
