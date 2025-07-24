package gg.nextforge.ui.context;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.entity.Player;

/**
 * Represents a player-specific context for an open UI session.
 */
@Getter
@AllArgsConstructor
public class UIUserContext {
    private final Player player;
    private final UIContext context;
    private final int currentPage;
}
