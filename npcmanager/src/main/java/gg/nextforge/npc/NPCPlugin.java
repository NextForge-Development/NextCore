package gg.nextforge.npc;

import gg.nextforge.plugin.NextForgePlugin;
import gg.nextforge.config.ConfigFile;
import lombok.Getter;

/**
 * Main class of the NPCManager plugin.
 */
@Getter
public class NPCPlugin extends NextForgePlugin {

    private ConfigFile storageFile;

    @Override
    public int getMetricsId() {
        return 0; // No bStats id
    }

    @Override
    public java.util.UUID getPluginId() {
        return java.util.UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    @Override
    public void enable(boolean isReload) {
        storageFile = getConfigManager().loadConfig("npc-storage.yml");
        NPCManager.init(this);
        NPCManager.get().load();
        new NPCCommand(this);
    }

    @Override
    public void disable() {
        NPCManager.get().saveAll();
    }
}
