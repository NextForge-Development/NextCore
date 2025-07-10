package gg.nextforge.fancynpc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic tab completion for /npc
 */
public class NPCTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(List.of("help", "create", "copy", "remove", "list", "info"));
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("copy"))) {
            completions.addAll(NPCManager.get().listNPCs());
        }
        return completions;
    }
}
