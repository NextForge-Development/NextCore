package gg.nextforge.npc;

import gg.nextforge.config.ConfigFile;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Singleton manager responsible for NPC creation, removal and persistence.
 */
@Getter
public class NPCManager {

    private static NPCManager instance;

    private final NPCPlugin plugin;
    private final Map<String, NPC> npcMap = new HashMap<>();

    private NPCManager(NPCPlugin plugin) {
        this.plugin = plugin;
    }

    /** Initialize the manager */
    public static void init(NPCPlugin plugin) {
        instance = new NPCManager(plugin);
    }

    /** Get manager instance */
    public static NPCManager get() {
        return instance;
    }

    /** Load NPCs from storage file */
    public void load() {
        ConfigFile file = plugin.getStorageFile();
        npcMap.clear();
        ConfigurationSection root = file.getRawConfig().getConfigurationSection("npcs");
        if (root == null) return;
        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            NPC npc = NPC.builder()
                    .id(id)
                    .type(sec.getString("type", "VILLAGER"))
                    .displayName(sec.getString("displayName", id))
                    .glowing(sec.getBoolean("glowing", false))
                    .showInTab(sec.getBoolean("showInTab", false))
                    .collidable(sec.getBoolean("collidable", true))
                    .transientNPC(sec.getBoolean("transient", false))
                    .skin(sec.getString("skin"))
                    .size(sec.getDouble("size", 1.0))
                    .interactionCooldown(sec.getInt("cooldown",0))
                    .attributes(new HashMap<>())
                    .actions(new ArrayList<>())
                    .build();

            ConfigurationSection attr = sec.getConfigurationSection("attributes");
            if (attr != null) {
                for (String key : attr.getKeys(false)) {
                    npc.getAttributes().put(key, attr.getString(key));
                }
            }

            List<String> actionList = sec.getStringList("actions");
            if (actionList != null) {
                npc.getActions().addAll(actionList);
            }

            ConfigurationSection loc = sec.getConfigurationSection("location");
            if (loc != null) {
                World w = Bukkit.getWorld(loc.getString("world", "world"));
                double x = loc.getDouble("x");
                double y = loc.getDouble("y");
                double z = loc.getDouble("z");
                float yaw = (float) loc.getDouble("yaw");
                float pitch = (float) loc.getDouble("pitch");
                if (w != null) {
                    Location l = new Location(w, x, y, z, yaw, pitch);
                    npc.setLocation(l);
                }
            }

            spawnNPC(npc);
            npcMap.put(id, npc);
        }
    }

    /** Save all persistent NPCs to disk */
    public void saveAll() {
        ConfigFile file = plugin.getStorageFile();
        file.getRawConfig().set("npcs", null);
        for (NPC npc : npcMap.values()) {
            if (npc.isTransientNPC()) continue;
            saveNPCToConfig(file, npc);
        }
        file.save();
    }

    private void saveNPCToConfig(ConfigFile file, NPC npc) {
        ConfigurationSection sec = file.getRawConfig().createSection("npcs." + npc.getId());
        sec.set("type", npc.getType());
        sec.set("displayName", npc.getDisplayName());
        sec.set("glowing", npc.isGlowing());
        sec.set("showInTab", npc.isShowInTab());
        sec.set("collidable", npc.isCollidable());
        sec.set("skin", npc.getSkin());
        sec.set("size", npc.getSize());
        sec.set("transient", npc.isTransientNPC());
        sec.set("cooldown", npc.getInteractionCooldown());
        sec.createSection("attributes").addDefaults(npc.getAttributes());
        sec.set("actions", npc.getActions());
        if (npc.getLocation() != null) {
            ConfigurationSection loc = sec.createSection("location");
            loc.set("world", npc.getLocation().getWorld().getName());
            loc.set("x", npc.getLocation().getX());
            loc.set("y", npc.getLocation().getY());
            loc.set("z", npc.getLocation().getZ());
            loc.set("yaw", npc.getLocation().getYaw());
            loc.set("pitch", npc.getLocation().getPitch());
        }
    }

    /** Spawn the NPC entity in the world */
    private void spawnNPC(NPC npc) {
        if (npc.getLocation() == null) return;
        EntityType type = EntityType.valueOf(npc.getType());
        Entity entity = npc.getLocation().getWorld().spawnEntity(npc.getLocation(), type);
        npc.setEntity(entity);
        entity.setCustomName(npc.getDisplayName());
        entity.setCustomNameVisible(true);
        entity.setGlowing(npc.isGlowing());
        entity.setCollidable(npc.isCollidable());
    }

    /** Despawn NPC entity */
    private void despawnNPC(NPC npc) {
        if (npc.getEntity() != null && npc.getEntity().isValid()) {
            npc.getEntity().remove();
        }
        npc.setEntity(null);
    }

    // Command APIs

    public void createNPC(String id, EntityType type, Location location) {
        if (npcMap.containsKey(id)) return;
        NPC npc = NPC.builder()
                .id(id)
                .type(type.name())
                .displayName(id)
                .location(location)
                .attributes(new HashMap<>())
                .actions(new ArrayList<>())
                .size(1.0)
                .build();
        spawnNPC(npc);
        npcMap.put(id, npc);
        saveAll();
    }

    public void copyNPC(String sourceId, String newId) {
        NPC source = npcMap.get(sourceId);
        if (source == null) return;
        NPC copy = NPC.builder()
                .id(newId)
                .type(source.getType())
                .displayName(source.getDisplayName())
                .glowing(source.isGlowing())
                .showInTab(source.isShowInTab())
                .collidable(source.isCollidable())
                .skin(source.getSkin())
                .size(source.getSize())
                .interactionCooldown(source.getInteractionCooldown())
                .attributes(new HashMap<>(source.getAttributes()))
                .actions(new ArrayList<>(source.getActions()))
                .location(source.getLocation() == null ? null : source.getLocation().clone())
                .transientNPC(source.isTransientNPC())
                .build();
        if (copy.getLocation() != null) {
            spawnNPC(copy);
        }
        npcMap.put(newId, copy);
        saveAll();
    }

    public void removeNPC(String id) {
        NPC npc = npcMap.remove(id);
        if (npc != null) {
            despawnNPC(npc);
            saveAll();
        }
    }

    public List<String> listNPCs() {
        return new ArrayList<>(npcMap.keySet());
    }

    public NPC getNPC(String id) {
        return npcMap.get(id);
    }

    public void setType(NPC npc, EntityType type) {
        despawnNPC(npc);
        npc.setType(type.name());
        if (npc.getLocation() != null) spawnNPC(npc);
        saveAll();
    }

    public void setDisplayName(NPC npc, String name) {
        npc.setDisplayName(name);
        if (npc.getEntity() != null) {
            npc.getEntity().setCustomName(name);
        }
        saveAll();
    }

    public void toggleGlowing(NPC npc) {
        npc.setGlowing(!npc.isGlowing());
        if (npc.getEntity() != null) npc.getEntity().setGlowing(npc.isGlowing());
        saveAll();
    }

    public void toggleCollidable(NPC npc) {
        npc.setCollidable(!npc.isCollidable());
        if (npc.getEntity() != null) npc.getEntity().setCollidable(npc.isCollidable());
        saveAll();
    }

    public void setLocation(NPC npc, Location loc) {
        despawnNPC(npc);
        npc.setLocation(loc);
        spawnNPC(npc);
        saveAll();
    }

    public void setRotation(NPC npc, float yaw, float pitch) {
        if (npc.getLocation() != null) {
            npc.getLocation().setYaw(yaw);
            npc.getLocation().setPitch(pitch);
            if (npc.getEntity() != null) {
                npc.getEntity().teleport(npc.getLocation());
            }
            saveAll();
        }
    }

    public void centerAt(NPC npc, Location loc) {
        loc.setX(loc.getBlockX() + 0.5);
        loc.setZ(loc.getBlockZ() + 0.5);
        setLocation(npc, loc);
    }

    public void moveToLocation(NPC npc, Location loc) {
        if (npc.getEntity() != null) {
            npc.getEntity().teleport(loc);
        }
        npc.setLocation(loc);
        saveAll();
    }

    public void moveToYourLocation(NPC npc, Location playerLoc) {
        moveToLocation(npc, playerLoc);
    }

    public void teleportPlayerToNPC(org.bukkit.entity.Player player, NPC npc) {
        if (npc.getLocation() != null) {
            player.teleport(npc.getLocation());
        }
    }

    public List<NPC> getNearby(Location loc, double radius) {
        return npcMap.values().stream()
                .filter(n -> n.getLocation() != null && n.getLocation().getWorld().equals(loc.getWorld())
                        && n.getLocation().distance(loc) <= radius)
                .collect(Collectors.toList());
    }

}
