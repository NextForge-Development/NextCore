package gg.nextforge.npc;

import gg.nextforge.npc.model.NPC;
import gg.nextforge.plugin.NextForgePlugin;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manager class for handling NPCs (Non-Player Characters) in the NextForge framework.
 * This class is responsible for managing the lifecycle and interactions of NPCs,
 * including their creation, updates, and removal. Transient NPCs are also supported,
 * allowing for temporary NPCs that do not persist across server restarts.
 */
@Getter
public class NPCManager {

    private final JavaPlugin plugin; // Reference to the main plugin instance
    private final Map<String, NPC> npcs = new HashMap<>(); // Map to store NPCs by their unique ID
    private FileConfiguration npcConfig; // Configuration file for saving and loading NPC data

    /**
     * Constructs an NPCManager instance and initializes the NPC configuration.
     *
     * @param plugin The plugin instance associated with this manager.
     */
    public NPCManager(JavaPlugin plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs(); // Create the data folder if it doesn't exist
        }
        npcConfig = YamlConfiguration.loadConfiguration(new File(dataFolder, "npcs.yml"));
        load(); // Load existing NPCs from the configuration file
    }

    /**
     * Loads NPCs from the configuration file into the manager.
     * This method reads the NPC data from the 'npcs.yml' file and populates the npcs map.
     */
    private void load() {
        ConfigurationSection root = npcConfig.getConfigurationSection("npcs");
        if (root != null) {
            for (String id : root.getKeys(false)) {
                ConfigurationSection sec = root.getConfigurationSection(id);
                NPC npc = loadFromSection(id, sec);
                if (npc != null) {
                    npcs.put(id, npc);
                    spawn(npc);
                }
            }
        }
    }

    /**
     * Registers a new NPC in the manager and spawns it.
     *
     * @param npc The NPC to register.
     */
    public void register(NPC npc) {
        if (npcs.containsKey(npc.getId())) {
            despawn(npcs.get(npc.getId())); // Despawn the existing NPC if it already exists
        }
        npcs.put(npc.getId(), npc);
        spawn(npc);
    }

    /**
     * Registers a new NPC using a builder.
     *
     * @param builder The NPC builder instance.
     */
    public void register(NPC.NPCBuilder builder) {
        NPC npc = builder.build();
        register(npc);
    }

    /**
     * Modifies an existing NPC using a provided modifier function.
     *
     * @param id       The ID of the NPC to modify.
     * @param modifier A function that modifies the NPC.
     */
    public void modify(String id, Function<NPC, NPC> modifier) {
        NPC npc = npcs.get(id);
        if (npc != null) {
            NPC modifiedNpc = modifier.apply(npc);
            if (modifiedNpc != null) {
                npcs.put(id, modifiedNpc);
                despawn(npc);
                spawn(modifiedNpc);
            }
        }
    }

    /**
     * Removes an NPC from the manager and despawns it.
     *
     * @param npc The NPC to be removed.
     */
    public void remove(NPC npc) {
        if (npcs.containsKey(npc.getId())) {
            despawn(npc);
            npcs.remove(npc.getId());
        }
    }

    /**
     * Saves the current state of NPCs to the configuration file.
     * This method writes all NPC data from the npcs map back to the 'npcs.yml' file.
     */
    public void save() {
        ConfigurationSection root = npcConfig.createSection("npcs");
        for (NPC npc : npcs.values()) {
            if (npc.isTransientNPC()) {
                continue; // Skip transient NPCs, they are not saved
            }
            ConfigurationSection sec = root.createSection(npc.getId());
            saveToSection(npc, sec);
        }
    }

    /**
     * Reloads the NPC configuration and clears the current NPCs.
     */
    public void reload() {
        npcs.clear();
        npcConfig = YamlConfiguration.loadConfiguration(new File(NextForgePlugin.getInstance().getDataFolder(), "npcs.yml"));
        load();
    }

    /**
     * Spawns an NPC in the world based on its configuration.
     *
     * @param npc The NPC to spawn.
     */
    private void spawn(NPC npc) {
        if (npc.getLocation() == null) return; // Ensure the NPC has a valid location before spawning
        EntityType type = EntityType.valueOf(npc.getType()); // Get the entity type from the NPC configuration
        Entity entity = npc.getLocation().getWorld().spawnEntity(npc.getLocation(), type); // Spawn the entity in the world
        npc.setEntity(entity); // Set the entity reference in the NPC object
        entity.customName(MiniMessage.miniMessage().deserialize(npc.getDisplayName())); // Set the NPC's display name
        entity.setCustomNameVisible(true); // Make the NPC's name visible
        entity.setGlowing(npc.isGlowing()); // Set glowing effect if enabled
        entity.setInvulnerable(true); // Make NPC invulnerable to prevent damage
        entity.setSilent(true); // Prevent NPC from making sounds
        entity.setGravity(false); // Disable gravity for the NPC
        if (entity instanceof LivingEntity livingEntity) {
            livingEntity.setCollidable(npc.isCollidable()); // Set collidable state based on NPC configuration
            livingEntity.setAI(false); // Disable AI to prevent NPC from moving on its own
        }
    }

    /**
     * Despawns an NPC from the world.
     *
     * @param npc The NPC to despawn.
     */
    private void despawn(NPC npc) {
        if (npc.getEntity() != null && npc.getEntity().isValid()) {
            npc.getEntity().remove();
        }
        npc.setEntity(null);
    }

    /**
     * Loads an NPC from a configuration section.
     *
     * @param id  The ID of the NPC.
     * @param sec The configuration section containing the NPC data.
     * @return The loaded NPC instance, or null if the section is invalid.
     */
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
                .turnToPlayer(sec.getBoolean("turnToPlayer", false))
                .turnToPlayerDistance(sec.getDouble("turnToPlayerDistance", 3.0))
                .actions(new EnumMap<>(NPC.ClickType.class))
                .build();

        // Load actions
        ConfigurationSection actionsSec = sec.getConfigurationSection("actions");
        if (actionsSec != null) {
            for (String clickKey : actionsSec.getKeys(false)) {
                try {
                    NPC.ClickType clickType = NPC.ClickType.valueOf(clickKey);
                    List<String> entries = actionsSec.getStringList(clickKey);
                    for (String entry : entries) {
                        String[] parts = entry.split(":", 2);
                        NPC.ActionType type = NPC.ActionType.valueOf(parts[0]);
                        String cmd = parts.length > 1 ? parts[1] : "";
                        npc.getActions().computeIfAbsent(clickType, k -> new ArrayList<>())
                                .add(new NPC.CommandAction(type, cmd));
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Load location
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

    /**
     * Clears transient NPCs from the manager.
     * Transient NPCs are temporary and do not persist across server restarts.
     */
    public void clearTransient() {
        for (Iterator<Map.Entry<String, NPC>> it = npcs.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, NPC> e = it.next();
            if (e.getValue().isTransientNPC()) {
                despawn(e.getValue());
                it.remove();
            }
        }
    }

    /**
     * Saves an NPC's data to a configuration section.
     *
     * @param npc The NPC to save.
     * @param sec The configuration section to save the data to.
     */
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

        // Save actions
        ConfigurationSection actionsSec = sec.createSection("actions");
        for (Map.Entry<NPC.ClickType, List<NPC.CommandAction>> e : npc.getActions().entrySet()) {
            List<String> list = e.getValue().stream()
                    .map(a -> a.getActionType().name() + ":" + a.getCommand())
                    .collect(Collectors.toList());
            actionsSec.set(e.getKey().name(), list);
        }

        // Save location
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

    public NPC getByEntity(Entity entity) {
        if (entity == null || !npcs.containsKey(entity.getUniqueId().toString())) {
            return null; // Return null if the entity is not an NPC
        }
        return npcs.get(entity.getUniqueId().toString()); // Get the NPC by its unique ID
    }

    public Player getNearestPlayer(NPC npc, double turnToPlayerDistance) {
        if (npc.getLocation() == null) return null; // Ensure the NPC has a valid location
        double closestDistance = turnToPlayerDistance;
        Player closestPlayer = null;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(npc.getLocation().getWorld())) {
                double distance = player.getLocation().distance(npc.getLocation());
                if (distance <= closestDistance) {
                    closestDistance = distance;
                    closestPlayer = player;
                }
            }
        }
        return closestPlayer; // Return the nearest player within the specified distance
    }

    public void rotateNpcHead(NPC npc, @NotNull Location location) {
        if (npc.getEntity() == null || !(npc.getEntity() instanceof LivingEntity livingEntity)) {
            return; // Ensure the NPC entity is valid and is a LivingEntity
        }
        Location npcLocation = npc.getLocation();
        if (npcLocation == null) return; // Ensure the NPC has a valid location

        double deltaX = location.getX() - npcLocation.getX();
        double deltaZ = location.getZ() - npcLocation.getZ();
        double yaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90; // Calculate yaw based on the target location

        livingEntity.setRotation((float) yaw, livingEntity.getLocation().getPitch()); // Set the NPC's head rotation
    }
}