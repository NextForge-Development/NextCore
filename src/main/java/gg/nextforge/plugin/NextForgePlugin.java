package gg.nextforge.plugin;

import gg.nextforge.NextCorePlugin;
import gg.nextforge.command.CommandManager;
import gg.nextforge.config.ConfigManager;
import gg.nextforge.npc.NPCManager;
import gg.nextforge.protocol.ProtocolManager;
import gg.nextforge.scheduler.CoreScheduler;
import gg.nextforge.text.TextManager;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

@Getter
public abstract class NextForgePlugin extends JavaPlugin {

    @Getter
    private static NextForgePlugin instance;

    ConfigManager configManager;
    CoreScheduler scheduler;
    CommandManager commandManager;
    TextManager textManager;
    NPCManager npcManager;
    ProtocolManager protocolManager;
    Metrics metrics;

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

        this.metrics = new Metrics(this, getMetricsId());

        this.configManager = new ConfigManager(this);
        this.scheduler = new CoreScheduler();
        this.commandManager = new CommandManager(this);
        this.textManager = new TextManager(this);
        this.npcManager = new NPCManager(this);
        this.protocolManager = new ProtocolManager(this);

        boolean isReload = getServer().getPluginManager().isPluginEnabled("NextForge");
        enable(isReload);
    }

    @Override
    public void onDisable() {
        disable();
    }
}
