package gg.nextforge.npc.model;

import lombok.*;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Data holder for a single NPC.
 */
@Getter
@Setter
@Builder
public class NPC {
    private String id;
    private String type;
    private String displayName;
    private String skin;
    private boolean glowing;
    private ChatColor glowColor;
    private boolean showInTab;
    private boolean collidable;
    private double scale;
    private boolean transientNPC;
    private int interactionCooldown;
    private boolean turnToPlayer;
    private double turnToPlayerDistance;
    private Map<String, String> attributes;
    private Map<ClickType, List<CommandAction>> actions;
    private Location location;
    private Entity entity; // runtime only

    @Getter @Setter @NoArgsConstructor
    @AllArgsConstructor
    public static class CommandAction {
        private ActionType actionType;    // PLAYER_COMMAND vs CONSOLE_COMMAND
        private String command;           // Befehl ohne Slash
    }

    public enum ClickType { ANY_CLICK, LEFT_CLICK, RIGHT_CLICK }
    public enum ActionType { PLAYER_COMMAND, CONSOLE_COMMAND }

    // Methods für action add/remove/clear/list
    public void addAction(ClickType click, CommandAction action) {
        actions.computeIfAbsent(click, k -> new ArrayList<>()).add(action);
    }

    public boolean removeAction(ClickType click, int index) {
        List<CommandAction> list = actions.get(click);
        return list != null && list.remove(index) != null;
    }

    public void clearActions(ClickType click) {
        actions.remove(click);
    }

    public List<CommandAction> listActions(ClickType click) {
        return actions.getOrDefault(click, Collections.emptyList());
    }
}
