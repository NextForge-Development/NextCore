package gg.nextforge.ui.support;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class ChatPromptUI {

    private static final Map<UUID, BiConsumer<Player, String>> prompts = new ConcurrentHashMap<>();

    public static void requestInput(Plugin plugin, Player player, String prompt, BiConsumer<Player, String> onInput) {
        player.sendMessage(prompt);
        prompts.put(player.getUniqueId(), onInput);
    }

    public static void registerListener(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {

            @org.bukkit.event.EventHandler
            public void onChat(AsyncChatEvent event) {
                Player player = event.getPlayer();
                if (prompts.containsKey(player.getUniqueId())) {
                    event.setCancelled(true);
                    BiConsumer<Player, String> handler = prompts.remove(player.getUniqueId());
                    if (handler != null) {
                        handler.accept(player, PlainTextComponentSerializer.plainText().serialize(event.message()));
                    }
                }
            }

            @org.bukkit.event.EventHandler
            public void onQuit(PlayerQuitEvent event) {
                prompts.remove(event.getPlayer().getUniqueId());
            }

        }, plugin);
    }
}
