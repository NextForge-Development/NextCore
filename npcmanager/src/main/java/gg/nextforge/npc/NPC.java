package gg.nextforge.npc;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.Map;

/**
 * Data class representing an NPC definition.
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
    private boolean showInTab;
    private boolean collidable;
    private double size;
    private boolean transientNPC;
    private int interactionCooldown;
    private Map<String, String> attributes;
    private List<String> actions;
    private Location location;
    private Entity entity; // runtime entity reference (not saved)
}
