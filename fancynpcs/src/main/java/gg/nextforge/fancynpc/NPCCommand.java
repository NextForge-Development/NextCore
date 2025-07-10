package gg.nextforge.fancynpc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.List;

/**
 * Command executor handling /npc commands.
 */
public class NPCCommand implements CommandExecutor {

    private final FancyNPCPlugin plugin;

    public NPCCommand(FancyNPCPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("nextcore.npc")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§6FancyNPCs commands:");
            sender.sendMessage("§e/npc create <name>");
            sender.sendMessage("§e/npc copy <npc> <new_name>");
            sender.sendMessage("§e/npc remove <npc>");
            sender.sendMessage("§e/npc list");
            sender.sendMessage("§e/npc info <npc>");
            return true;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create":
                return handleCreate(sender, args);
            case "copy":
                return handleCopy(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender, args);
            default:
                sender.sendMessage(plugin.msg("error"));
                return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(plugin.msg("error"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("usage_create"));
            return true;
        }
        String name = args[1];
        Location loc = p.getLocation();
        NPCManager.get().createNPC(name, EntityType.PLAYER, loc);
        sender.sendMessage(plugin.msg("npc_created").replace("{name}", name));
        return true;
    }

    private boolean handleCopy(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.msg("usage_copy"));
            return true;
        }
        NPC src = NPCManager.get().getNPC(args[1]);
        if (src == null) {
            sender.sendMessage(plugin.msg("error"));
            return true;
        }
        NPCManager.get().createNPC(args[2], EntityType.valueOf(src.getType()), src.getLocation());
        sender.sendMessage(plugin.msg("npc_created").replace("{name}", args[2]));
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("usage_remove"));
            return true;
        }
        NPCManager.get().removeNPC(args[1]);
        sender.sendMessage(plugin.msg("npc_removed").replace("{name}", args[1]));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        List<String> ids = NPCManager.get().listNPCs();
        sender.sendMessage("NPCs: " + String.join(", ", ids));
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("usage_info"));
            return true;
        }
        NPC npc = NPCManager.get().getNPC(args[1]);
        if (npc == null) {
            sender.sendMessage(plugin.msg("error"));
            return true;
        }
        sender.sendMessage("Type: " + npc.getType());
        if (npc.getLocation() != null) {
            sender.sendMessage("Location: " + npc.getLocation().toVector().toString());
        }
        return true;
    }
}
