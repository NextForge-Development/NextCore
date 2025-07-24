package gg.nextforge.plugin;

import gg.nextforge.NextCorePlugin;
import gg.nextforge.command.CommandManager;
import gg.nextforge.config.ConfigManager;
import gg.nextforge.database.DatabaseManager;
import gg.nextforge.npc.NPCManager;
import gg.nextforge.performance.listener.TickListener;
import gg.nextforge.protocol.ProtocolManager;
import gg.nextforge.scheduler.CoreScheduler;
import gg.nextforge.text.TextManager;
import gg.nextforge.ui.UIManager;
import gg.nextforge.ui.listener.InventoryClickListener;
import gg.nextforge.ui.listener.InventoryCloseListener;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

@Getter
public abstract class NextForgePlugin extends JavaPlugin {

    @Getter
    private static NextForgePlugin instance;

    private ConfigManager configManager;
    private CoreScheduler scheduler;
    private CommandManager commandManager;
    private TextManager textManager;
    private NPCManager npcManager;
    private ProtocolManager protocolManager;
    private DatabaseManager databaseManager;
    private Metrics metrics;
    private UIManager uiManager;

    public abstract int getMetricsId();

    public abstract UUID getPluginId();

    public abstract void enable(boolean isReload);

    public abstract void disable();

    public boolean isCore() {
        Class<?> mainClass = getClass();
        return mainClass.equals(NextCorePlugin.class) || mainClass.getSuperclass().equals(NextCorePlugin.class);
    }

    public boolean isNextForgePlugin(Plugin plugin) {
        return plugin instanceof NextForgePlugin;
    }

    public String getPluginVersion() {
        return getDescription().getVersion();
    }

    @Override
    public void onEnable() {
        instance = this;

        // bStats
        this.metrics = new Metrics(this, getMetricsId());

        // Managers
        this.configManager = new ConfigManager(this);
        this.scheduler = new CoreScheduler();
        this.commandManager = new CommandManager(this);
        this.textManager = new TextManager(this);
        this.npcManager = new NPCManager(this);
        this.protocolManager = new ProtocolManager(this);

        // UI System
        this.uiManager = UIManager.getInstance(); // Singleton init (if not already done)
        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(), this);
        Bukkit.getPluginManager().registerEvents(new InventoryCloseListener(), this);
        Bukkit.getPluginManager().registerEvents(new TickListener(this), this);

        boolean isReload = getServer().getPluginManager().isPluginEnabled("NextForge");
        enable(isReload);
    }

    @Override
    public void onDisable() {
        if (instance != this) {
            getLogger().warning("Plugin instance mismatch! This should not happen.");
            return;
        }
        if (uiManager != null) {
            uiManager.closeAll();
        }

        instance = null;
        disable();
    }
}
