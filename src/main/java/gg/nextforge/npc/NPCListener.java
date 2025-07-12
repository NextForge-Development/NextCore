package gg.nextforge.npc;

import gg.nextforge.npc.model.NPC;
import gg.nextforge.scheduler.CoreScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class NPCListener implements Listener {
    private final NPCManager manager;

    public NPCListener(NPCManager manager) {
        this.manager = manager;
        handleTurnToPlayer();
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
        NPC npc = manager.getByEntity(e.getRightClicked());
        if (npc == null) return;

        // any_click + right_click
        runActions(npc, e.getPlayer(), NPC.ClickType.ANY_CLICK);
        runActions(npc, e.getPlayer(), NPC.ClickType.RIGHT_CLICK);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        NPC npc = manager.getByEntity(e.getEntity());
        if (npc == null) return;

        // any_click + left_click
        runActions(npc, (Player)e.getDamager(), NPC.ClickType.ANY_CLICK);
        runActions(npc, (Player)e.getDamager(), NPC.ClickType.LEFT_CLICK);
        e.setCancelled(true); // Optional: NPC darf keinen Schaden bekommen
    }

    private void runActions(NPC npc, Player player, NPC.ClickType click) {
        for (NPC.CommandAction ca : npc.listActions(click)) {
            switch (ca.getActionType()) {
                case PLAYER_COMMAND:
                    player.performCommand(ca.getCommand());
                    break;
                case CONSOLE_COMMAND:
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ca.getCommand());
                    break;
            }
        }
    }

    // Dreh-Task (kann auch als Repeating Task in NPCManager laufen)
    public void handleTurnToPlayer() {
        CoreScheduler.runTimer(() -> {
            for (NPC npc : manager.getNpcs().values()) {
                if (!npc.isTurnToPlayer()) continue;
                Player nearest = manager.getNearestPlayer(npc, npc.getTurnToPlayerDistance());
                if (nearest != null) {
                    manager.rotateNpcHead(npc, nearest.getLocation());
                }
            }
        }, 0L, 2L);
    }
}

