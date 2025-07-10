package gg.nextforge.fancynpc;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles creation, persistence and lookup of NPCs.
 */
@Getter
public class NPCManager {

    private static NPCManager instance;

    private final FancyNPCPlugin plugin;
    private final Map<String, NPC> npcMap = new HashMap<>();
    private FileConfiguration config;
    private File configFile;

    private NPCManager(FancyNPCPlugin plugin) {
        this.plugin = plugin;
    }

    /** Initialise the singleton. */
    public static void init(FancyNPCPlugin plugin) {
        instance = new NPCManager(plugin);
        instance.reload();
    }

    /** Access singleton. */
    public static NPCManager get() {
        return instance;
    }

    /** Reload configuration and spawn NPCs. */
    public void reload() {
        npcMap.clear();
        configFile = new File(plugin.getDataFolder(), "npcs.yml");
        if (!configFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                configFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection root = config.getConfigurationSection("npcs");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection sec = root.getConfigurationSection(id);
                NPC npc = loadFromSection(id, sec);
                if (npc != null) {
                    npcMap.put(id, npc);
                    spawn(npc);
                }
            }
        }
    }

    /** Save all persistent NPCs to disk. */
    public void saveAll() {
        config.set("npcs", null);
        for (NPC npc : npcMap.values()) {
            if (npc.isTransientNPC()) continue;
            ConfigurationSection sec = config.createSection("npcs." + npc.getId());
            saveToSection(npc, sec);
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private NPC loadFromSection(String id, ConfigurationSection sec) {
        if (sec == null) return null;
        NPC npc = NPC.builder()
                .id(id)
                .type(sec.getString("type", EntityType.VILLAGER.name()))
                .displayName(sec.getString("displayName", id))
                .skin(sec.getString("skin"))
                .glowing(sec.getBoolean("glowing"))
                .showInTab(sec.getBoolean("showInTab"))
                .collidable(sec.getBoolean("collidable", true))
                .scale(sec.getDouble("scale", 1.0))
                .transientNPC(sec.getBoolean("transient", false))
                .interactionCooldown(sec.getInt("cooldown", 0))
                .turnToPlayer(sec.getBoolean("turnToPlayer"))
                .turnToPlayerDistance(sec.getDouble("turnToPlayerDistance", 3.0))
                .attributes(new HashMap<>())
                .actions(new ArrayList<>())
                .build();
        ConfigurationSection attr = sec.getConfigurationSection("attributes");
        if (attr != null) {
            for (String key : attr.getKeys(false)) {
                npc.getAttributes().put(key, attr.getString(key));
            }
        }
        npc.getActions().addAll(sec.getStringList("actions"));
        ConfigurationSection loc = sec.getConfigurationSection("location");
        if (loc != null) {
            World w = Bukkit.getWorld(loc.getString("world", "world"));
            if (w != null) {
                npc.setLocation(new Location(
                        w,
                        loc.getDouble("x"),
                        loc.getDouble("y"),
                        loc.getDouble("z"),
                        (float) loc.getDouble("yaw"),
                        (float) loc.getDouble("pitch")
                ));
            }
        }
        return npc;
    }

    private void saveToSection(NPC npc, ConfigurationSection sec) {
        sec.set("type", npc.getType());
        sec.set("displayName", npc.getDisplayName());
        sec.set("skin", npc.getSkin());
        sec.set("glowing", npc.isGlowing());
        sec.set("showInTab", npc.isShowInTab());
        sec.set("collidable", npc.isCollidable());
        sec.set("scale", npc.getScale());
        sec.set("transient", npc.isTransientNPC());
        sec.set("cooldown", npc.getInteractionCooldown());
        sec.set("turnToPlayer", npc.isTurnToPlayer());
        sec.set("turnToPlayerDistance", npc.getTurnToPlayerDistance());
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

    private void spawn(NPC npc) {
        if (npc.getLocation() == null) return;
        EntityType type = EntityType.valueOf(npc.getType());
        Entity entity = npc.getLocation().getWorld().spawnEntity(npc.getLocation(), type);
        npc.setEntity(entity);
        entity.setCustomName(npc.getDisplayName());
        entity.setCustomNameVisible(true);
        entity.setGlowing(npc.isGlowing());
        entity.setCollidable(npc.isCollidable());
        if (!npc.isShowInTab()) {
            entity.setMetadata("silent", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        }
    }

    private void despawn(NPC npc) {
        if (npc.getEntity() != null && npc.getEntity().isValid()) {
            npc.getEntity().remove();
        }
        npc.setEntity(null);
    }

    // API methods used by commands
    public void createNPC(String id, EntityType type, Location location) {
        if (npcMap.containsKey(id)) return;
        NPC npc = NPC.builder()
                .id(id)
                .type(type.name())
                .displayName(id)
                .location(location)
                .attributes(new HashMap<>())
                .actions(new ArrayList<>())
                .scale(1.0)
                .turnToPlayer(false)
                .turnToPlayerDistance(3.0)
                .build();
        npcMap.put(id, npc);
        spawn(npc);
        saveAll();
    }

    public void removeNPC(String id) {
        NPC npc = npcMap.remove(id);
        if (npc != null) {
            despawn(npc);
            saveAll();
        }
    }

    public List<String> listNPCs() {
        return new ArrayList<>(npcMap.keySet());
    }

    public NPC getNPC(String id) {
        return npcMap.get(id);
    }

    public void save(NPC npc) {
        if (npc.isTransientNPC()) return;
        saveAll();
    }

    /** Clear transient NPCs on shutdown. */
    public void clearTransient() {
        for (Iterator<Map.Entry<String, NPC>> it = npcMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, NPC> e = it.next();
            if (e.getValue().isTransientNPC()) {
                despawn(e.getValue());
                it.remove();
            }
        }
    }

    // Example movement API
    public void moveToLocation(NPC npc, Location loc) {
        npc.setLocation(loc);
        if (npc.getEntity() != null) {
            npc.getEntity().teleport(loc);
        } else {
            spawn(npc);
        }
        save(npc);
    }

    public List<NPC> getNearby(Location loc, double radius) {
        return npcMap.values().stream()
                .filter(n -> n.getLocation() != null && n.getLocation().getWorld().equals(loc.getWorld()) && n.getLocation().distance(loc) <= radius)
                .collect(Collectors.toList());
    }
}
