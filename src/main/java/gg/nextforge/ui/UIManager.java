package gg.nextforge.ui;

import gg.nextforge.ui.inventory.UI;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UIManager {

    @Getter
    private static final UIManager instance = new UIManager();

    private final Map<UUID, UI> activeUIs = new HashMap<>();

    private UIManager() {
    }

    public void openUI(Player player, UI ui) {
        closeUI(player); // sicherstellen, dass vorherige UI geschlossen wird

        ui.open(player);
        activeUIs.put(player.getUniqueId(), ui);
    }

    public void closeUI(Player player) {
        UUID uuid = player.getUniqueId();
        UI current = activeUIs.remove(uuid);
        if (current != null) {
            current.close(player);
        }
    }

    public UI getCurrentUI(Player player) {
        return activeUIs.get(player.getUniqueId());
    }

    public boolean isViewingUI(Player player) {
        return activeUIs.containsKey(player.getUniqueId());
    }

    public void closeAll() {
        for (UUID uuid : activeUIs.keySet()) {
            Player player = getPlayer(uuid);
            if (player != null) {
                closeUI(player);
            }
        }
        activeUIs.clear();
    }

    private Player getPlayer(UUID uuid) {
        return org.bukkit.Bukkit.getPlayer(uuid);
    }
}