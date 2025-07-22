package gg.nextforge.performance.listener;

import gg.nextforge.performance.PerformanceRegistry;
import gg.nextforge.performance.TickProfiler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;

/**
 * A fallback tick monitor for platforms without ServerTickEvents.
 */
public class TickListener implements Listener {

    private final TickProfiler profiler;
    private final Plugin plugin;

    public TickListener(Plugin plugin) {
        this.plugin = plugin;
        this.profiler = PerformanceRegistry.getTickProfiler("main");
        startLoop();
    }

    private void startLoop() {
        new BukkitRunnable() {
            long lastTick = System.nanoTime();

            @Override
            public void run() {
                long now = System.nanoTime();
                profiler.onTickStart();
                profiler.onTickEnd();

                long duration = now - lastTick;
                lastTick = now;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
