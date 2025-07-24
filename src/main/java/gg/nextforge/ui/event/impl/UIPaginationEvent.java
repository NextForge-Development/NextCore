package gg.nextforge.ui.event.impl;

import gg.nextforge.ui.inventory.impl.paged.PaginatedUI;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

@Getter
public class UIPaginationEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();

    private final PaginatedUI ui;
    private final int fromPage;
    private final int toPage;

    public UIPaginationEvent(Player player, PaginatedUI ui, int fromPage, int toPage) {
        super(player);
        this.ui = ui;
        this.fromPage = fromPage;
        this.toPage = toPage;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}