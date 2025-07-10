package gg.nextforge.fancynpc;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Bukkit plugin entry point for FancyNPCs.
 */
public class FancyNPCPlugin extends JavaPlugin {

    private FileConfiguration messages;

    @Override
    public void onEnable() {
        NPCManager.init(this);
        loadMessages();
        getCommand("npc").setExecutor(new NPCCommand(this));
        getCommand("npc").setTabCompleter(new NPCTabCompleter());
    }

    @Override
    public void onDisable() {
        NPCManager.get().clearTransient();
        NPCManager.get().saveAll();
    }

    /** Load messages.yml from disk */
    private void loadMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public String msg(String key) {
        return messages.getString(key, key);
    }
}
