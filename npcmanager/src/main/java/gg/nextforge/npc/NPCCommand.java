package gg.nextforge.npc;

import gg.nextforge.command.CommandContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Command executor for /npc commands.
 */
public class NPCCommand {

    private final NPCPlugin plugin;

    public NPCCommand(NPCPlugin plugin) {
        this.plugin = plugin;
        register();
    }

    private void register() {
        plugin.getCommandManager().command("npc")
                .permission("npc.command")
                .description("Manage NPCs")
                .executor(this::handleRoot)
                .subcommand("create", this::handleCreate)
                .subcommand("copy", this::handleCopy)
                .subcommand("remove", this::handleRemove)
                .subcommand("list", this::handleList)
                .subcommand("info", this::handleInfo)
                .register();
    }

    private void handleRoot(CommandContext ctx) {
        ctx.replyMini("<gray>/npc create <id> [type]</gray>");
    }

    private void handleCreate(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.reply("Only players may create NPCs");
            return;
        }
        String id = ctx.getArg(0);
        if (id == null) {
            ctx.reply("Usage: /npc create <id> [type]");
            return;
        }
        var type = org.bukkit.entity.EntityType.VILLAGER;
        String t = ctx.getArg(1);
        if (t != null) {
            try {
                type = org.bukkit.entity.EntityType.valueOf(t.toUpperCase());
            } catch (IllegalArgumentException ex) {
                ctx.reply("Unknown entity type");
                return;
            }
        }
        Player player = ctx.getPlayer();
        Location loc = player.getLocation();
        NPCManager.get().createNPC(id, type, loc);
        ctx.reply("Created NPC " + id);
    }

    private void handleCopy(CommandContext ctx) {
        String source = ctx.getArg(0);
        String target = ctx.getArg(1);
        if (source == null || target == null) {
            ctx.reply("Usage: /npc copy <sourceId> <newId>");
            return;
        }
        NPCManager.get().copyNPC(source, target);
        ctx.reply("Copied NPC " + source + " -> " + target);
    }

    private void handleRemove(CommandContext ctx) {
        String id = ctx.getArg(0);
        if (id == null) {
            ctx.reply("Usage: /npc remove <id>");
            return;
        }
        NPCManager.get().removeNPC(id);
        ctx.reply("Removed NPC " + id);
    }

    private void handleList(CommandContext ctx) {
        ctx.reply("NPCs: " + String.join(", ", NPCManager.get().listNPCs()));
    }

    private void handleInfo(CommandContext ctx) {
        String id = ctx.getArg(0);
        if (id == null) {
            ctx.reply("Usage: /npc info <id>");
            return;
        }
        NPC npc = NPCManager.get().getNPC(id);
        if (npc == null) {
            ctx.reply("NPC not found");
            return;
        }
        ctx.reply("Type: " + npc.getType() + " Location: " +
                (npc.getLocation() != null ? npc.getLocation().toVector().toString() : "none"));
    }
}
