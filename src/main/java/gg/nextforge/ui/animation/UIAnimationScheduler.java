package gg.nextforge.ui.animation;

import gg.nextforge.scheduler.CoreScheduler;
import gg.nextforge.scheduler.advanced.TaskBuilder;
import gg.nextforge.ui.UIManager;
import gg.nextforge.ui.inventory.UI;
import gg.nextforge.ui.component.UIComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class UIAnimationScheduler {

    private final Plugin plugin;
    private int tick = 0;

    private final Set<UIAnimated> registered = new HashSet<>();

    public UIAnimationScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        TaskBuilder<Void> task = TaskBuilder.<Void>create(CoreScheduler.getAdvancedScheduler())
                .async(false)
                .delay(0)
                .period(100L) // 2 Ticks ≈ 100 ms (1 Tick = 50ms)
                .supplier(() -> {
                    tick++;

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        UI ui = UIManager.getInstance().getCurrentUI(player);
                        if (ui == null) continue;

                        Inventory inv = player.getOpenInventory().getTopInventory();

                        if (ui instanceof Iterable<?> iterable) {
                            for (Object obj : iterable) {
                                if (obj instanceof UIComponent comp && comp instanceof UIAnimated animated) {
                                    UIAnimationContext ctx = new UIAnimationContext(player, ui, comp, tick);
                                    animated.tick(ctx);
                                    inv.setItem(comp.getSlot(), comp.render());
                                }
                            }
                        }
                    }

                    return null;
                })
                .onError(ex -> {
                    Bukkit.getLogger().warning("[UIAnimationScheduler] Fehler bei der Tick-Verarbeitung:");
                    ex.printStackTrace();
                });

        task.schedule();
    }

    public void register(UIAnimated component) {
        registered.add(component);
    }

    public void unregister(UIAnimated component) {
        registered.remove(component);
    }
}
