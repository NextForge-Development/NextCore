package gg.nextforge.textblockitemdisplay;

import gg.nextforge.NextCorePlugin;
import gg.nextforge.command.CommandContext;
import gg.nextforge.command.CommandManager;
import gg.nextforge.text.TextManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Command handling for hologram operations.
 */
public class HologramCommand {
    private final NextCorePlugin plugin;
    private final HologramManager manager;
    private final TextManager text;

    public HologramCommand(NextCorePlugin plugin, HologramManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.text = plugin.getTextManager();
        register();
    }

    private void register() {
        CommandManager cm = plugin.getCommandManager();
        cm.command("hologram")
                .permission("nextforge.command.hologram")
                .description("Manage holograms")
                .executor(this::handleHelp)
                .subcommand("help", this::handleHelp)
                .subcommand("list", this::handleList)
                .subcommand("nearby", this::handleNearby)
                .subcommand("create", this::handleCreate)
                .subcommand("remove", this::handleRemove)
                .subcommand("copy", this::handleCopy)
                .subcommand("info", this::handleInfo)
                .register();
    }

    private void handleHelp(CommandContext ctx) {
        text.send(ctx.sender(), "<gray>/hologram list</gray> - list holograms");
        text.send(ctx.sender(), "<gray>/hologram create (type) (name)</gray>");
    }

    private void handleList(CommandContext ctx) {
        for (Hologram h : manager.getHolograms()) {
            text.send(ctx.sender(), " - " + h.getName());
        }
    }

    private void handleNearby(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            text.send(ctx.sender(), "Only players");
            return;
        }
        double range = 10;
        if (ctx.args().length > 0) {
            range = Double.parseDouble(ctx.args()[0]);
        }
        Location loc = player.getLocation();
        for (Hologram h : manager.getHolograms()) {
            if (h.getLocation().getWorld().equals(loc.getWorld()) &&
                    h.getLocation().distance(loc) <= range) {
                text.send(player, " - " + h.getName());
            }
        }
    }

    private void handleCreate(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player player)) {
            text.send(ctx.sender(), "Only players");
            return;
        }
        if (ctx.args().length < 2) {
            text.send(ctx.sender(), "Usage: /hologram create (type) (name)");
            return;
        }
        String type = ctx.args()[0].toLowerCase();
        String name = ctx.args()[1];
        Location loc = player.getLocation();
        switch (type) {
            case "text" -> manager.createTextHologram(name, loc);
            case "item" -> manager.createItemHologram(name, loc, player.getInventory().getItemInMainHand());
            case "block" -> manager.createBlockHologram(name, loc, Material.STONE);
            default -> {
                text.send(ctx.sender(), "Unknown type");
                return;
            }
        }
        text.send(ctx.sender(), "Created hologram " + name);
    }

    private void handleRemove(CommandContext ctx) {
        if (ctx.args().length < 1) {
            text.send(ctx.sender(), "Usage: /hologram remove (name)");
            return;
        }
        manager.remove(ctx.args()[0]);
        text.send(ctx.sender(), "Removed hologram");
    }

    private void handleCopy(CommandContext ctx) {
        if (ctx.args().length < 2) {
            text.send(ctx.sender(), "Usage: /hologram copy (src) (dest)");
            return;
        }
        Hologram src = manager.get(ctx.args()[0]);
        if (src == null) {
            text.send(ctx.sender(), "Not found");
            return;
        }
        Location loc = src.getLocation();
        if (src instanceof TextHologram th) {
            TextHologram nh = manager.createTextHologram(ctx.args()[1], loc);
            th.getLines().forEach(nh::addLine);
        } else if (src instanceof ItemHologram ih) {
            manager.createItemHologram(ctx.args()[1], loc, ih.getItem());
        } else if (src instanceof BlockHologram bh) {
            manager.createBlockHologram(ctx.args()[1], loc, bh.getBlockType());
        }
        text.send(ctx.sender(), "Copied hologram");
    }

    private void handleInfo(CommandContext ctx) {
        if (ctx.args().length < 1) {
            text.send(ctx.sender(), "Usage: /hologram info (name)");
            return;
        }
        Hologram h = manager.get(ctx.args()[0]);
        if (h == null) {
            text.send(ctx.sender(), "Not found");
            return;
        }
        text.send(ctx.sender(), "Name: " + h.getName());
        text.send(ctx.sender(), "Location: " + h.getLocation().toVector());
        if (h instanceof TextHologram th) {
            text.send(ctx.sender(), "Lines: " + th.getLines().size());
        }
        if (h instanceof ItemHologram ih) {
            ItemStack item = ih.getItem();
            text.send(ctx.sender(), "Item: " + item.getType());
        }
        if (h instanceof BlockHologram bh) {
            text.send(ctx.sender(), "Block: " + bh.getBlockType());
        }
    }
}
