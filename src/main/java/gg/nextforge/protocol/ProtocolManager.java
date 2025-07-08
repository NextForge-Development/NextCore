package gg.nextforge.protocol;

import gg.nextforge.protocol.export.PCAPExporter;
import gg.nextforge.protocol.injector.PlayerInjector;
import gg.nextforge.protocol.listener.AnnotationProcessor;
import gg.nextforge.protocol.listener.PacketListener;
import gg.nextforge.protocol.listener.PacketListenerManager;
import gg.nextforge.protocol.logging.WebhookLogger;
import gg.nextforge.protocol.packet.PacketContainer;
import gg.nextforge.protocol.packet.PacketType;
import gg.nextforge.protocol.util.PacketEffects;
import gg.nextforge.version.MinecraftVersion;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages packet manipulation and injection for Minecraft players.
 *
 * The ProtocolManager is responsible for intercepting, modifying, and managing
 * packets sent and received by players. It provides utilities for packet listeners,
 * fake entities, rate limiting, and more.
 */
public class ProtocolManager implements Listener {

    private static ProtocolManager instance; // Singleton instance of the ProtocolManager
    private final Plugin plugin; // The plugin instance associated with this manager
    private final MinecraftVersion version; // The Minecraft version being used

    // Player injectors for managing packet streams
    private final Map<UUID, PlayerInjector> injectors = new ConcurrentHashMap<>();

    // Manages packet listeners
    private final PacketListenerManager listenerManager;

    // Tracks packet history for players
    private final Map<UUID, PacketHistory> histories = new ConcurrentHashMap<>();

    // Handles rate limiting for packets
    private final RateLimiter rateLimiter;

    // JavaScript engine for packet filters
    private final ScriptEngine scriptEngine;

    // Manages fake entities
    private final FakeEntityManager fakeEntityManager;

    // Manages fake blocks
    private final FakeBlockManager fakeBlockManager;

    // Schedules packet-related tasks
    private final PacketScheduler packetScheduler;

    // Provides utilities for packet effects
    private final PacketEffects effects;

    // Detects client capabilities
    private final ClientCapabilityDetector capabilityDetector;

    // Processes annotations for packet handlers
    private final AnnotationProcessor annotationProcessor;

    // Optional components for logging and exporting
    WebhookLogger webhookLogger;
    PCAPExporter pcapExporter;

    // Debug mode flag
    private boolean debugMode = false;

    /**
     * Constructs the ProtocolManager and initializes its subsystems.
     *
     * @param plugin The plugin instance associated with this manager.
     */
    public ProtocolManager(Plugin plugin) {
        this.plugin = plugin;
        this.version = MinecraftVersion.getCurrent();
        instance = this;

        // Initialize subsystems
        this.listenerManager = new PacketListenerManager(this);
        this.rateLimiter = new RateLimiter();
        this.scriptEngine = new ScriptEngine(plugin);
        this.fakeEntityManager = new FakeEntityManager(this);
        this.fakeBlockManager = new FakeBlockManager(this);
        this.packetScheduler = new PacketScheduler(this);
        this.effects = new PacketEffects(this);
        this.capabilityDetector = new ClientCapabilityDetector();
        this.annotationProcessor = new AnnotationProcessor(this);

        // Register events
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Inject into all online players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            injectPlayer(player);
        }

        plugin.getLogger().info("Protocol Manager initialized for " + version);
    }

    /**
     * Injects into a player's network channel to intercept packets.
     *
     * @param player The player to inject into.
     */
    public void injectPlayer(Player player) {
        try {
            PlayerInjector injector = new PlayerInjector(this, player);
            injector.inject();
            injectors.put(player.getUniqueId(), injector);

            if (debugMode) {
                plugin.getLogger().info("Injected into " + player.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to inject into player: " + player.getName(), e);
        }
    }

    /**
     * Removes injection from a player's network channel.
     *
     * @param player The player to uninject.
     */
    public void uninjectPlayer(Player player) {
        PlayerInjector injector = injectors.remove(player.getUniqueId());
        if (injector != null) {
            injector.uninject();
        }

        // Clean up associated data
        histories.remove(player.getUniqueId());
        capabilityDetector.removePlayer(player.getUniqueId());
        fakeEntityManager.clearPlayer(player);
        fakeBlockManager.clearPlayer(player);
        rateLimiter.clearPlayer(player.getUniqueId());
    }

    /**
     * Handles player join events by injecting into their network channel.
     *
     * @param event The PlayerJoinEvent.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            injectPlayer(event.getPlayer());
        }, 1L);
    }

    /**
     * Handles player quit events by removing their injection.
     *
     * @param event The PlayerQuitEvent.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        uninjectPlayer(event.getPlayer());
    }

    /**
     * Sends a packet directly to a player.
     *
     * @param player The player to send the packet to.
     * @param packet The packet to send.
     */
    public void sendPacket(Player player, PacketContainer packet) {
        PlayerInjector injector = injectors.get(player.getUniqueId());
        if (injector != null) {
            injector.sendPacket(packet);
        } else {
            throw new IllegalStateException(
                    "Player not injected: " + player.getName()
            );
        }
    }

    /**
     * Sends a packet to multiple players.
     *
     * @param players The collection of players to send the packet to.
     * @param packet The packet to send.
     */
    public void sendPacket(Collection<? extends Player> players, PacketContainer packet) {
        for (Player player : players) {
            sendPacket(player, packet);
        }
    }

    /**
     * Broadcasts a packet to all online players.
     *
     * @param packet The packet to broadcast.
     */
    public void broadcastPacket(PacketContainer packet) {
        sendPacket(plugin.getServer().getOnlinePlayers(), packet);
    }

    /**
     * Registers a packet listener.
     *
     * @param listener The packet listener to register.
     */
    public void registerListener(PacketListener listener) {
        listenerManager.register(listener);
    }

    /**
     * Unregisters a packet listener.
     *
     * @param listener The packet listener to unregister.
     */
    public void unregisterListener(PacketListener listener) {
        listenerManager.unregister(listener);
    }

    /**
     * Retrieves the packet history for a player.
     *
     * @param player The player whose packet history is requested.
     * @return The packet history for the player.
     */
    public PacketHistory getHistory(Player player) {
        return histories.computeIfAbsent(
                player.getUniqueId(),
                uuid -> new PacketHistory(100) // Keep last 100 packets
        );
    }

    /**
     * Checks if a packet should be rate limited for a player.
     *
     * @param player The player to check.
     * @param type The type of packet.
     * @return true if the packet should be rate limited, false otherwise.
     */
    public boolean shouldRateLimit(Player player, PacketType type) {
        return rateLimiter.shouldLimit(player.getUniqueId(), type);
    }

    /**
     * Retrieves the JavaScript engine for filters.
     *
     * @return The ScriptEngine instance.
     */
    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    /**
     * Retrieves the fake entity manager.
     *
     * @return The FakeEntityManager instance.
     */
    public FakeEntityManager getFakeEntityManager() {
        return fakeEntityManager;
    }

    /**
     * Retrieves the fake block manager.
     *
     * @return The FakeBlockManager instance.
     */
    public FakeBlockManager getFakeBlockManager() {
        return fakeBlockManager;
    }

    /**
     * Retrieves the packet scheduler.
     *
     * @return The PacketScheduler instance.
     */
    public PacketScheduler getPacketScheduler() {
        return packetScheduler;
    }

    /**
     * Retrieves the listener manager.
     *
     * @return The PacketListenerManager instance.
     */
    public PacketListenerManager getListenerManager() {
        return listenerManager;
    }

    /**
     * Retrieves the client capability detector.
     *
     * @return The ClientCapabilityDetector instance.
     */
    public ClientCapabilityDetector getCapabilityDetector() {
        return capabilityDetector;
    }

    /**
     * Retrieves the annotation processor.
     *
     * @return The AnnotationProcessor instance.
     */
    public AnnotationProcessor getAnnotationProcessor() {
        return annotationProcessor;
    }

    /**
     * Retrieves the rate limiter.
     *
     * @return The RateLimiter instance.
     */
    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    /**
     * Retrieves the packet effects utility.
     *
     * @return The PacketEffects instance.
     */
    public PacketEffects getEffects() {
        return effects;
    }

    /**
     * Sets the webhook logger URL. Pass null to disable webhook logging.
     *
     * @param webhookUrl The URL of the webhook.
     */
    public void setWebhookLogger(String webhookUrl) {
        if (webhookLogger != null) {
            webhookLogger.shutdown();
            webhookLogger = null;
        }

        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            webhookLogger = new WebhookLogger(webhookUrl);
        }
    }

    /**
     * Starts PCAP export to a file.
     *
     * @param file The file to export to.
     * @throws java.io.IOException If an error occurs during export.
     */
    public void startPCAPExport(java.io.File file) throws java.io.IOException {
        if (pcapExporter != null) {
            pcapExporter.close();
        }
        pcapExporter = new PCAPExporter(file);
    }

    /**
     * Stops PCAP export.
     */
    public void stopPCAPExport() {
        if (pcapExporter != null) {
            pcapExporter.close();
            pcapExporter = null;
        }
    }

    /**
     * Retrieves a new instance of the packet effects utility.
     *
     * @return A new PacketEffects instance.
     */
    public PacketEffects getPacketEffects() {
        return new PacketEffects(this);
    }

    /**
     * Enables or disables debug mode.
     *
     * @param debug true to enable debug mode, false to disable it.
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }

    /**
     * Checks if debug mode is enabled.
     *
     * @return true if debug mode is enabled, false otherwise.
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Retrieves the plugin instance associated with this manager.
     *
     * @return The Plugin instance.
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Retrieves the singleton instance of the ProtocolManager.
     *
     * @return The ProtocolManager instance.
     * @throws IllegalStateException If the ProtocolManager is not initialized.
     */
    public static ProtocolManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "ProtocolManager not initialized! Did NextForgeCore load properly?"
            );
        }
        return instance;
    }

    /**
     * Shuts down the ProtocolManager and cleans up resources.
     */
    public void shutdown() {
        // Uninject all players
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            uninjectPlayer(player);
        }

        // Clear all data
        injectors.clear();
        histories.clear();
        listenerManager.clear();
        scriptEngine.shutdown();

        // Shutdown optional components
        if (webhookLogger != null) {
            webhookLogger.shutdown();
        }
        if (pcapExporter != null) {
            pcapExporter.close();
        }

        // Clear packet scheduler
        packetScheduler.clear();

        // Clear fake entities and blocks
        fakeEntityManager.clearAll();
        fakeBlockManager.clearAll();
    }
}