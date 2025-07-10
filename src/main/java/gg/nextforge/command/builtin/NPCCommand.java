package gg.nextforge.command.builtin;

import gg.nextforge.NextCorePlugin;
import gg.nextforge.command.CommandContext;
import gg.nextforge.command.CommandManager;
import gg.nextforge.npc.NPCManager;
import gg.nextforge.npc.model.NPC;
import gg.nextforge.text.TextManager;
import gg.nextforge.utility.ChatPagination;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NPCCommand {

    private final NextCorePlugin plugin;
    private final NPCManager npcManager;
    private final TextManager textManager;

    public NPCCommand(NextCorePlugin plugin, NPCManager npcManager) {
        this.plugin = plugin;
        this.npcManager = npcManager;
        this.textManager = plugin.getTextManager();
        registerCommands();
    }

    private void registerCommands() {
        plugin.getCommandManager().command("npc")
                .permission("nextforge.command.npc")
                .description("Manage your npc's with ease.")
                .aliases("npcs", "npcmanager")
                .executor(this::handleHelp)
                .subcommand("help", this::handleHelp)
                .subcommand("create", this::handleCreate)
                .subcommand("copy", this::handleCopy)
                .subcommand("remove", this::handleRemove)
                .subcommand("list", this::handleList)
                .subcommand("info", this::handleInfo)
                .subcommand("type", this::handleType)
                .subcommand("displayname", this::handleDisplayName)
                .subcommand("skin", this::handleSkin)
                .subcommand("equipment", this::handleEquipment)
                .subcommand("glowing", this::handleGlowing)
                .subcommand("show_in_tab", this::handleShowInTab)
                .subcommand("collidable", this::handleCollidable)
                .subcommand("scale", this::handleScale)
                .subcommand("attribute", this::handleAttribute)
                .subcommand("turn_to_player", this::handleTurnToPlayer)
                .subcommand("turn_to_player_distance", this::handleTurnToPlayerDistance)
                .subcommand("move_here", this::handleMoveHere)
                .subcommand("move_to", this::handleMoveTo)
                .subcommand("center", this::handleCenter)
                .subcommand("nearby", this::handleNearby)
                .subcommand("teleport", this::handleTeleport)
                .subcommand("action", this::handleAction)
                .subcommand("interaction_cooldown", this::handleInteractionCooldown)
                .register();
    }

    private void handleHelp(CommandContext ctx) {
        int page = 1;
        if (ctx.args().length != 0) {
            try {
                page = Integer.parseInt(ctx.args()[0]);
            } catch (NumberFormatException e) {
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.help.invalid_page", "%prefix% <red>Invalid page number. Please enter a valid number.</red>"));
                return;
            }
        }
        ChatPagination pagination = new ChatPagination(6);
        for (String line : plugin.getMessagesFile().getStringList("commands.npc.help.lines")) {
            line = line.replace("%prefix%", plugin.getMessagesFile().getString("general.prefix", "<dark_gray>[<gradient:aqua:dark_aqua>ɴᴇxᴛғᴏʀɢᴇ<dark_gray>]</gradient></dark_gray>"));
            pagination.addLine(line);
        }
        if (pagination.getTotalPages() < page || page < 1) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.help.invalid_page", "%prefix% <red>Invalid page number. Please enter a valid number.</red>"));
            return;
        }
        for (String header : plugin.getMessagesFile().getStringList("commands.npc.help.header")) {
            header = header.replace("%prefix%", plugin.getMessagesFile().getString("general.prefix", "<dark_gray>[<gradient:aqua:dark_aqua>ɴᴇxᴛғᴏʀɢᴇ<dark_gray>]</gradient></dark_gray>"));
            header = header.replace("%page%", String.valueOf(page));
            header = header.replace("%max%", String.valueOf(pagination.getTotalPages()));
            textManager.send(ctx.sender(), header);
        }
        for (String line : pagination.getPage((page-1))) {
            textManager.send(ctx.sender(), line);
        }
        for (String footer : plugin.getMessagesFile().getStringList("commands.npc.help.footer")) {
            footer = footer.replace("%prefix%", plugin.getMessagesFile().getString("general.prefix", "<dark_gray>[<gradient:aqua:dark_aqua>ɴᴇxᴛғᴏʀɢᴇ<dark_gray>]</gradient></dark_gray>"));
            footer = footer.replace("%page%", String.valueOf(page));
            footer = footer.replace("%max%", String.valueOf(pagination.getTotalPages()));
            textManager.send(ctx.sender(), footer);
        }
    }

    private void handleCreate(CommandContext ctx) {
        if (ctx.args().length < 1) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.create.usage", "%prefix% <red>Usage: /npc create (name)</red>"));
            return;
        }
        String id = ctx.args()[0];
        if (npcManager.getNpcs().containsKey(id)) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.create.already-exists", "%prefix% <red>NPC with ID '%name%' already exists.</red>").replace("%name%", id));
            return;
        }
        if (!(ctx.sender() instanceof Player)) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("general.only-players", "%prefix% <red>This command can only be used by players.</red>"));
            return;
        }
        Player player = (Player) ctx.sender();
        Location loc = player.getLocation();
        NPC npc = NPC.builder()
                .id(id)
                .type(EntityType.VILLAGER.name())
                .displayName(id)
                .skin(null)
                .glowing(false)
                .showInTab(true)
                .collidable(true)
                .scale(1.0)
                .transientNPC(false)
                .interactionCooldown(0)
                .turnToPlayer(false)
                .turnToPlayerDistance(3.0)
                .attributes(new java.util.HashMap<>())
                .actions(new java.util.ArrayList<>())
                .location(loc)
                .build();
        npcManager.register(npc);
        npcManager.save();
        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.create.success", "%prefix% <green>NPC '%name%' created successfully at your location.</green>").replace("%name%", id));
    }

    private void handleCopy(CommandContext ctx) {
        if (ctx.args().length < 2) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.copy.usage", "%prefix% <red>Usage: /npc copy (npc) (new_name)</red>"));
            return;
        }
        String src = ctx.args()[0], dest = ctx.args()[1];
        NPC orig = npcManager.getNpcs().get(src);
        if (orig == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.copy.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", src));
            return;
        }
        if (npcManager.getNpcs().containsKey(dest)) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.copy.already-exists", "%prefix% <red>NPC with ID '%new_name%' already exists.</red>").replace("%new_name%", dest));
            return;
        }
        NPC copy = NPC.builder()
                .id(dest)
                .type(orig.getType())
                .displayName(orig.getDisplayName())
                .skin(orig.getSkin())
                .glowing(orig.isGlowing())
                .showInTab(orig.isShowInTab())
                .collidable(orig.isCollidable())
                .scale(orig.getScale())
                .transientNPC(orig.isTransientNPC())
                .interactionCooldown(orig.getInteractionCooldown())
                .turnToPlayer(orig.isTurnToPlayer())
                .turnToPlayerDistance(orig.getTurnToPlayerDistance())
                .attributes(new java.util.HashMap<>(orig.getAttributes()))
                .actions(new java.util.ArrayList<>(orig.getActions()))
                .location(orig.getLocation())
                .build();
        npcManager.register(copy);
        npcManager.save();
        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.copy.success", "%prefix% <green>NPC '%name%' copied to '%new_name%' successfully.</green>").replace("%name%", src).replace("%new_name%", dest));
    }

    private void handleRemove(CommandContext ctx) {
        if (ctx.args().length < 1) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.remove.usage", "%prefix% <red>Usage: /npc remove <id></red>"));
            return;
        }
        String id = ctx.args()[0];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.remove.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id));
            return;
        }
        npcManager.remove(npc);
        npcManager.save();
        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.remove.success", "%prefix% <green>NPC '%name%' removed successfully.</green>").replace("%name%", id));
    }


    private void handleList(CommandContext ctx) {
        List<NPC> list = npcManager.getNpcs().values().stream().toList();
        if (list.isEmpty()) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.list.empty", "%prefix% <yellow>No NPCs found.</yellow>"));
            return;
        }
        ChatPagination pagination = new ChatPagination(5);
        for (NPC npc : list) {
            String line = plugin.getMessagesFile().getString("commands.npc.list.line", "<gold>│</gold>  <hover:show_text:'Click to teleport to %name%'><click:run_command='/npc info %name%'><gray>%id% <yellow>%name%</yellow> <dark_gray>- </dark_gray>%type% <gray>(%skin%)</gray></click></hover>")
                    .replace("%id%", npc.getId())
                    .replace("%name%", npc.getDisplayName())
                    .replace("%type%", npc.getType())
                    .replace("%skin%", npc.getSkin() != null ? npc.getSkin() : "default");
            line = line.replace("%prefix%", plugin.getMessagesFile().getString("general.prefix", "<dark_gray>[<gradient:aqua:dark_aqua>ɴᴇxᴛғᴏʀɢᴇ<dark_gray>]</gradient></dark_gray>"));
            pagination.addLine(line);
        }
        int page = 1;
        if (ctx.args().length > 0) {
            try { page = Integer.parseInt(ctx.args()[0]); } catch (NumberFormatException ignored) {}
        }
        if (page < 1 || page > pagination.getTotalPages()) page = 1;
        int finalPage = page;
        plugin.getMessagesFile().getStringList("commands.npc.list.header").forEach(line -> {
            line = line.replace("%prefix%", plugin.getMessagesFile().getString("general.prefix", "<dark_gray>[<gradient:aqua:dark_aqua>ɴᴇxᴛғᴏʀɢᴇ<dark_gray>]</gradient></dark_gray>"));
            line = line.replace("%page%", String.valueOf(finalPage));
            line = line.replace("%max%", String.valueOf(pagination.getTotalPages()));
            textManager.send(ctx.sender(), line);
        });
        pagination.getPage(page - 1).forEach(line -> textManager.send(ctx.sender(), line));
    }

    private void handleInfo(CommandContext ctx) {
        if (ctx.args().length < 1) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.info.usage", "%prefix% <red>Usage: /npc info (npc)</red>"));
            return;
        }
        NPC npc = npcManager.getNpcs().get(ctx.args()[0]);
        if (npc == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.info.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", ctx.args()[0]));
            return;
        }
        for (String header : plugin.getMessagesFile().getStringList("commands.npc.info.header")) {
            header = header.replace("%name%", npc.getDisplayName());
            textManager.send(ctx.sender(), header);
        }
        for (String line : plugin.getMessagesFile().getStringList("commands.npc.info.lines")) {
            line = line.replace("%id%", npc.getId())
                    .replace("%name%", npc.getDisplayName())
                    .replace("%type%", npc.getType())
                    .replace("%skin%", npc.getSkin() != null ? npc.getSkin() : "default")
                    .replace("%glowing%", npc.isGlowing() ? "Yes" : "No")
                    .replace("%show_in_tab%", npc.isShowInTab() ? "Yes" : "No")
                    .replace("%collidable%", npc.isCollidable() ? "Yes" : "No")
                    .replace("%scale%", String.valueOf(npc.getScale()))
                    .replace("%interaction_cooldown%", String.valueOf(npc.getInteractionCooldown()))
                    .replace("%turn_to_player%", npc.isTurnToPlayer() ? "Yes" : "No")
                    .replace("%turn_to_player_distance%", String.valueOf(npc.getTurnToPlayerDistance()))
                    .replace("%transient%", npc.isTransientNPC() ? "Yes" : "No")
                    .replace("%x%", String.valueOf(npc.getLocation().getX()))
                    .replace("%y%", String.valueOf(npc.getLocation().getY()))
                    .replace("%z%", String.valueOf(npc.getLocation().getZ()))
                    .replace("%world%", npc.getLocation().getWorld().getName());
            textManager.send(ctx.sender(), line);
        }
        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.info.actions.header", "<gold>│</gold>  <gray>Actions:</gray>"));
        if (npc.getActions().isEmpty()) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.info.actions.empty", "<gold>│</gold>  <gray>No actions defined.</gray>"));
        } else {
            for (String action : npc.getActions()) {
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.info.actions.line", "<gold>│</gold>  <gray>%action% (%trigger%)</gray>").replace("%action%", action)
                        .replace("%trigger%", action.split(" ")[0])  // Assuming the trigger is the first word in the action string
                );
            }
        }
        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.info.attributes.header", "<gold>│</gold>  <gray>Attributes:</gray>"));
        if (npc.getAttributes().isEmpty()) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.info.attributes.empty", "<gold>│</gold>  <gray>No attributes defined.</gray>"));
        } else {
            for (String key : npc.getAttributes().keySet()) {
                String value = npc.getAttributes().get(key);
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.info.attributes.line", "<gold>│</gold>  <gray>%key%: %value%</gray>").replace("%key%", key).replace("%value%", value));
            }
        }
        for (String footer : plugin.getMessagesFile().getStringList("commands.npc.info.footer")) {
            footer = footer.replace("%name%", npc.getDisplayName());
            textManager.send(ctx.sender(), footer);
        }
    }

    private void handleType(CommandContext ctx) {
        if (ctx.args().length < 2) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.type.usage", "%prefix% <red>Usage: /npc type (npc) (type)</red>"));
            return;
        }
        String id = ctx.args()[0], type = ctx.args()[1];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.type.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id));
            return;
        }
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.type.invalid-type", "%prefix% <red>Invalid NPC type '%type%'. Available Types: %valid_types%</red>").replace("%type%", type)
                    .replace("%valid_types%", Arrays.stream(EntityType.values()).map(Enum::name).collect(Collectors.joining(", ")))
            );
            return;
        }
        npcManager.modify(id, npcBuilder -> {
            npcBuilder.setType(entityType.name().toUpperCase());
            return npcBuilder;
        });
        npcManager.save();
        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.type.success", "%prefix% <green>NPC '%name%' type changed to '%type%' successfully.</green>").replace("%name%", id).replace("%type%", entityType.name()));
    }

    private void handleDisplayName(CommandContext ctx) {
        if (ctx.args().length < 2) {
            textManager.send(ctx.sender(), "Usage: /npc displayname <id> <name|@none>");
            return;
        }
        String id = ctx.args()[0];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.displayname.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id));
            return;
        }
        String displayName = ctx.args()[1];
        if (displayName.equalsIgnoreCase("@none")) {
            displayName = null; // Reset to default
        } else if (displayName.startsWith("@")) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.displayname.invalid", "%prefix% <red>Invalid display name '%name%'. Use '@none' to reset.</red>").replace("%name%", displayName));
            return;
        }
        String finalDisplayName = displayName;
        npcManager.modify(id, npcBuilder -> {
            npcBuilder.setDisplayName(finalDisplayName);
            return npcBuilder;
        });
        if (finalDisplayName == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.displayname.success-cleared", "%prefix% <green>NPC '%name%' display name reset to default.</green>").replace("%name%", id));
        } else {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.displayname.success-set", "%prefix% <green>NPC '%name%' display name changed to '%displayName%'.</green>").replace("%name%", id).replace("%displayName%", finalDisplayName));
        }
    }

    private void handleSkin(CommandContext ctx) {
        if (ctx.args().length < 2) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.skin.usage", "%prefix% <red>Usage: /npc skin (npc) (skin_value | @none | @mirror) [--slim]</red>"));
            return;
        }
        String id = ctx.args()[0];
        NPC existing = npcManager.getNpcs().get(id);
        if (existing == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.skin.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id));
            return;
        }
        String option = ctx.args()[1];
        boolean slim = false;
        for (String arg : ctx.args()) {
            if ("--slim".equalsIgnoreCase(arg)) {
                slim = true;
                break;
            }
        }
        String skinValue;
        switch (option.toLowerCase()) {
            case "@none":
                skinValue = null;
                break;
            case "@mirror":
                if (ctx.sender() instanceof Player player) {
                    skinValue = player.getName();
                } else {
                    textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.skin.mirror-only-players", "%prefix% <red>NPC skin type '@mirror' can only be used on player NPCs and by a player.</red>"));
                    return;
                }
                break;
            default:
                if (option.startsWith("http://") || option.startsWith("https://")) {
                    skinValue = "url:" + option; // e.g., url:http://example.com/skin.png
                } else if (option.contains("/")) {
                    skinValue = "file:" + option; // e.g., file:/path/to/skin.png
                } else if (option.length() > 16 && option.matches("[a-fA-F0-9_]")) {
                    skinValue = "name:" + option; // assume player name
                } else {
                    skinValue = null; // Invalid skin type
                    textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.skin.invalid", "%prefix% <red>Invalid skin type '%skin_type%'. Valid types are: @none, @mirror, name, url, file.</red>").replace("%skin_type%", option));
                    return;
                }
        }
        if (skinValue == null) return;
        String finalValue = (slim ? "slim:" : "classic:") + skinValue;
        npcManager.modify(id, npc -> { npc.setSkin(finalValue); return npc; });
        npcManager.save();
        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.skin.success", "%prefix% <green>NPC '%name%' skin set to '%skin_value%'.</green>").replace("%name%", id).replace("%skin_value%", finalValue + " (slim: " + slim + ")"));
    }

    private void handleEquipment(CommandContext ctx) {
        if (ctx.args().length < 2) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.usage", "%prefix% <red>Usage: /npc equipment (npc) [list | set <slot> <item> | clear]</red>"));
            return;
        }
        String id = ctx.args()[0];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id));
            return;
        }
        Entity ent = npc.getEntity();
        if (ent == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.not-spawned", "%prefix% <red>NPC '%name%' is not spawned in the world.</red>").replace("%name%", id));
            return;
        }
        if (!(ent instanceof org.bukkit.entity.LivingEntity livingEntity)) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.not-living-entity", "%prefix% <red>NPC '%name%' is not a living entity and cannot have equipment.</red>").replace("%name%", id));
            return;
        }
        EntityEquipment eq = livingEntity.getEquipment();
        if (eq == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.no-equipment", "%prefix% <red>NPC '%name%'s equipment is null.</red>").replace("%name%", id));
            return;
        }
        String sub = ctx.args()[1].toLowerCase();
        switch (sub) {
            case "list": {
                //Check if equipment is all empty:
                if (eq.getItemInMainHand().getType().isAir() && eq.getItemInOffHand().getType().isAir() && eq.getHelmet().getType().isAir() && eq.getChestplate().getType().isAir() && eq.getLeggings().getType().isAir() && eq.getBoots().getType().isAir()) {
                    textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.list.empty", "%prefix% <yellow>NPC '%name%' has no equipment set.</yellow>").replace("%name%", id));
                    return;
                }
                for (String line : plugin.getMessagesFile().getStringList("commands.npc.equipment.list.header")) {
                    line = line.replace("%name%", id);
                    textManager.send(ctx.sender(), line);
                }
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("list.line", "<gold>│</gold>  <gray>%slot%: %item%</gray>")
                        .replace("%slot%", "Helmet")
                        .replace("%item%", !eq.getItemInMainHand().getType().isAir() ? eq.getItemInMainHand().getType().name() : "None")
                );
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("list.line", "<gold>│</gold>  <gray>%slot%: %item%</gray>")
                        .replace("%slot%", "Offhand")
                        .replace("%item%", !eq.getItemInOffHand().getType().isAir() ? eq.getItemInOffHand().getType().name() : "None")
                );
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("list.line", "<gold>│</gold>  <gray>%slot%: %item%</gray>")
                        .replace("%slot%", "Helmet")
                        .replace("%item%", !eq.getHelmet().getType().isAir() ? eq.getHelmet().getType().name() : "None")
                );
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("list.line", "<gold>│</gold>  <gray>%slot%: %item%</gray>")
                        .replace("%slot%", "Chestplate")
                        .replace("%item%", !eq.getChestplate().getType().isAir() ? eq.getChestplate().getType().name() : "None")
                );
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("list.line", "<gold>│</gold>  <gray>%slot%: %item%</gray>")
                        .replace("%slot%", "Leggings")
                        .replace("%item%", !eq.getLeggings().getType().isAir() ? eq.getLeggings().getType().name() : "None")
                );
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("list.line", "<gold>│</gold>  <gray>%slot%: %item%</gray>")
                        .replace("%slot%", "Boots")
                        .replace("%item%", !eq.getBoots().getType().isAir() ? eq.getBoots().getType().name() : "None")
                );
                for (String line : plugin.getMessagesFile().getStringList("commands.npc.equipment.list.footer")) {
                    line = line.replace("%name%", id);
                    textManager.send(ctx.sender(), line);
                }
                break;
            }
            case "clear": {
                eq.setItemInMainHand(null);
                eq.setItemInOffHand(null);
                eq.setHelmet(null);
                eq.setChestplate(null);
                eq.setLeggings(null);
                eq.setBoots(null);
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.clear-success", "%prefix% <green>Cleared all equipment of NPC '%name%'.</green>").replace("%name%", id));
                break;
            }
            case "set": {
                if (ctx.args().length < 4) {
                    textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.usage", "%prefix% <red>Usage: /npc equipment set <slot> <item></red>"));
                    return;
                }
                String slot = ctx.args()[2].toLowerCase();
                String itemName = ctx.args()[3].toUpperCase();
                Material mat = Material.matchMaterial(itemName);
                if (mat == null) {
                    textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.invalid-material", "%prefix% <red>Invalid material '%material%'.</red>").replace("%material%", itemName));
                    return;
                }
                ItemStack item = new ItemStack(mat);
                switch (slot) {
                    case "mainhand":
                    case "hand":
                        eq.setItemInMainHand(item);
                        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.set-success", "%prefix% <green>NPC '%name%' equipment slot %slot% set to %material%.</green>").replace("%name%", id).replace("%slot%", "MAINHAND").replace("%material%", mat.name()));
                        break;
                    case "offhand":
                        eq.setItemInOffHand(item);
                        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.set-success", "%prefix% <green>NPC '%name%' equipment slot %slot% set to %material%.</green>").replace("%name%", id).replace("%slot%", "OFFHAND").replace("%material%", mat.name()));
                        break;
                    case "helmet":
                        eq.setHelmet(item);
                        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.set-success", "%prefix% <green>NPC '%name%' equipment slot %slot% set to %material%.</green>").replace("%name%", id).replace("%slot%", "HELMET").replace("%material%", mat.name()));
                        break;
                    case "chestplate":
                        eq.setChestplate(item);
                        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.set-success", "%prefix% <green>NPC '%name%' equipment slot %slot% set to %material%.</green>").replace("%name%", id).replace("%slot%", "CHESTPLATE").replace("%material%", mat.name()));
                        break;
                    case "leggings":
                        eq.setLeggings(item);
                        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.set-success", "%prefix% <green>NPC '%name%' equipment slot %slot% set to %material%.</green>").replace("%name%", id).replace("%slot%", "LEGGINGS").replace("%material%", mat.name()));
                        break;
                    case "boots":
                        eq.setBoots(item);
                        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.set-success", "%prefix% <green>NPC '%name%' equipment slot %slot% set to %material%.</green>").replace("%name%", id).replace("%slot%", "BOOTS").replace("%material%", mat.name()));
                        break;
                    default:
                        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.invalid-slot", "%prefix% <red>Invalid equipment slot '%slot%'. Valid slots are: %valid_slots%</red>").replace("%slot%", slot).replace("%valid_slots%", "mainhand, offhand, helmet, chestplate, leggings, boots"));
                        return;
                }
                break;
            }
            default:
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.equipment.usage", "%prefix% <red>Usage: /npc equipment (npc) (set | clear | list)</red>"));
        }
    }


    private void handleGlowing(CommandContext ctx) {
        if (ctx.args().length < 2) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.glowing.usage", "%prefix% <red>Usage: /npc glowing (npc) [disabled | color]</red>"));
            return;
        }
        String id = ctx.args()[0];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.glowing.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id));
            return;
        }
        String option = ctx.args()[1].toLowerCase();
        if (option.equalsIgnoreCase("disabled")) {
            npcManager.modify(id, npcBuilder -> {
                npcBuilder.setGlowing(false);
                return npcBuilder;
            });
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.glowing.success", "%prefix% <green>NPC '%name%' glowing effect set to '%state%'.</green>").replace("%name%", id).replace("%state%", "disabled"));
        } else {
            ChatColor color;
            try {
                color = ChatColor.valueOf(option.toUpperCase());
            } catch (IllegalArgumentException e) {
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.glowing.invalid-color", "%prefix% <red>Invalid color '%color%'. Available colors: %valid_colors%</red>")
                        .replace("%color%", option)
                        .replace("%valid_colors%", Arrays.stream(ChatColor.values()).filter(ChatColor::isColor).map(Enum::name).collect(Collectors.joining(", "))));
                return;
            }
            npcManager.modify(id, npcBuilder -> {
                npcBuilder.setGlowing(true);
                npcBuilder.setGlowColor(color);
                return npcBuilder;
            });
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.glowing.success", "%prefix% <green>NPC '%name%' glowing effect set to '%state%'.</green>").replace("%name%", id).replace("%state%", color.name()));
        }
    }

    private void handleShowInTab(CommandContext ctx) {
        if (ctx.args().length < 2) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.show_in_tab.usage", "%prefix% <red>Usage: /npc show_in_tab (npc) [true | false]</red>"));
            return;
        }
        if (!ctx.args()[1].equalsIgnoreCase("true") && !ctx.args()[1].equalsIgnoreCase("false")) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.show_in_tab.invalid-state", "%prefix% <red>Invalid state '%state%'. Use 'true' or 'false'.</red>").replace("%state%", ctx.args()[1]));
            return;
        }
        String id = ctx.args()[0];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.show_in_tab.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id));
            return;
        }
        boolean showInTab = Boolean.parseBoolean(ctx.args()[1]);
        npcManager.modify(id, npcBuilder -> {
            npcBuilder.setShowInTab(showInTab);
            return npcBuilder;
        });
        npcManager.save();
        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.show_in_tab.success", "%prefix% <green>NPC '%name%' show in tab state set to '%state%'.</green>").replace("%name%", id).replace("%state%", String.valueOf(showInTab)));
    }

    private void handleCollidable(CommandContext ctx) {
        if (ctx.args().length < 2) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.collidable.usage", "%prefix% <red>Usage: /npc collidable (npc) [true | false]</red>"));
            return;
        }
        if (!ctx.args()[1].equalsIgnoreCase("true") && !ctx.args()[1].equalsIgnoreCase("false")) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.collidable.invalid-state", "%prefix% <red>Invalid state '%state%'. Use 'true' or 'false'.</red>").replace("%state%", ctx.args()[1]));
            return;
        }
        String id = ctx.args()[0];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.collidable.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id));
            return;
        }
        boolean col = Boolean.parseBoolean(ctx.args()[1]);
        npcManager.modify(id, npcBuilder -> { npcBuilder.setCollidable(col); return npcBuilder; });
        npcManager.save();
        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.collidable.success", "%prefix% <green>NPC '%name%' collidable state set to '%state%'.</green>").replace("%name%", id).replace("%state%", String.valueOf(col)));
    }

    private void handleScale(CommandContext ctx) {
        if (ctx.args().length < 2) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.scale.usage", "%prefix% <red>Usage: /npc scale (npc) <scale></red>"));
            return;
        }
        String id = ctx.args()[0];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.scale.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id));
            return;
        }
        try {
            double scale = Double.parseDouble(ctx.args()[1]);
            if (scale <= 0) {
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.scale.invalid-factor", "%prefix% <red>Scale must be a positive number.</red>"));
                return;
            }
            npcManager.modify(id, npcBuilder -> { npcBuilder.setScale(scale); return npcBuilder; });
            npcManager.save();
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.scale.success", "%prefix% <green>NPC '%name%' scale set to %scale%.</green>").replace("%name%", id).replace("%scale%", String.valueOf(scale)));
        } catch (NumberFormatException e) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.scale.invalid-factor", "%prefix% <red>Invalid scale value '%value%'.</red>").replace("%value%", ctx.args()[1]));
        }
    }

    private void handleAttribute(CommandContext ctx) {
        if (ctx.args().length < 2) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.attribute.usage", "%prefix% <red>Usage: /npc attribute (npc) (list | set <key> <value>)</red>"));
            return;
        }
        String id = ctx.args()[0];
        String sub = ctx.args()[1];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) { textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.attribute.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id)); return; }
        if (sub.equalsIgnoreCase("list")) {
            if (npc.getAttributes().isEmpty()) {
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.attribute.list.empty", "%prefix% <yellow>NPC '%name%' has no attributes.</yellow>").replace("%name%", id));
            } else {
                for (String header : plugin.getMessagesFile().getStringList("commands.npc.attribute.list.header")) {
                    header = header.replace("%name%", id);
                    textManager.send(ctx.sender(), header);
                }
                for (Map.Entry<String, String> entry : npc.getAttributes().entrySet()) {
                    textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.attribute.list.line", "<gold>│</gold>  <gray>%attribute%: %value%</gray>").replace("%attribute%", entry.getKey()).replace("%value%", entry.getValue()));
                }
                for (String footer : plugin.getMessagesFile().getStringList("commands.npc.attribute.list.footer")) {
                    footer = footer.replace("%name%", id);
                    textManager.send(ctx.sender(), footer);
                }
            }
            return;
        } else if (sub.equalsIgnoreCase("set") && ctx.args().length >= 4) {
            String key = ctx.args()[2], val = ctx.args()[3];
            npcManager.modify(id, n -> { n.getAttributes().put(key, val); return n; });
            npcManager.save();
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.attribute.set.success", "%prefix% <green>NPC '%name%' attribute '%attribute%' set to %value%.</green>").replace("%name%", id).replace("%attribute%", key).replace("%value%", val));
        } else if (sub.equalsIgnoreCase("remove") && ctx.args().length >= 3) {
            String key = ctx.args()[2];
            if (npc.getAttributes().remove(key) != null) {
                npcManager.save();
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.attribute.remove.success", "%prefix% <green>NPC '%name%' attribute '%attribute%' removed successfully.</green>").replace("%name%", id).replace("%attribute%", key));
            } else {
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.attribute.remove.not-found", "%prefix% <red>NPC '%name%' does not have attribute '%attribute%'.</red>").replace("%name%", id).replace("%attribute%", key));
            }
        } else {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.attribute.usage", "%prefix% <red>Usage: /npc attribute (npc) (list | set <key> <value>)</red>"));
        }
    }

    private void handleTurnToPlayer(CommandContext ctx) {
        if (ctx.args().length < 2) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.turn_to_player.usage", "%prefix% <red>Usage: /npc turn_to_player (npc) [true | false]</red>"));
            return;
        }
        String id = ctx.args()[0];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.turn_to_player.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id));
            return;
        }
        if (!ctx.args()[1].equalsIgnoreCase("true") && !ctx.args()[1].equalsIgnoreCase("false")) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.turn_to_player.invalid-state", "%prefix% <red>Invalid state '%state%'. Use 'true' or 'false'.</red>").replace("%state%", ctx.args()[1]));
            return;
        }
        boolean ttp = Boolean.parseBoolean(ctx.args()[1]);
        npcManager.modify(id, npcBuilder -> { npcBuilder.setTurnToPlayer(ttp); return npcBuilder; });
        npcManager.save();
        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.turn_to_player.success", "%prefix% <green>NPC '%name%' turn to player state set to '%state%'.</green>").replace("%name%", id).replace("%state%", String.valueOf(ttp)));
    }

    private void handleTurnToPlayerDistance(CommandContext ctx) {
        if (ctx.args().length < 2) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.turn_to_player_distance.usage", "%prefix% <red>Usage: /npc turn_to_player_distance (npc) [distance]</red>"));
            return;
        }
        String id = ctx.args()[0];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.turn_to_player_distance.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id));
            return;
        }
        try {
            double d = Double.parseDouble(ctx.args()[1]);
            if (d < 0) {
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.turn_to_player_distance.invalid-distance", "%prefix% <red>Invalid distance '%distance%'. Must be a positive number.</red>").replace("%distance%", ctx.args()[1]));
                return;
            }
            npcManager.modify(id, npcBuilder -> { npcBuilder.setTurnToPlayerDistance(d); return npcBuilder; });
            npcManager.save();
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.turn_to_player_distance.success", "%prefix% <green>NPC '%name%' turn to player distance set to %distance% blocks.</green>").replace("%name%", id).replace("%distance%", String.valueOf(d)));
        } catch (NumberFormatException e) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.turn_to_player_distance.invalid-distance", "%prefix% <red>Invalid distance '%distance%'. Must be a number.</red>").replace("%distance%", ctx.args()[1]));
        }
    }

    private void handleMoveHere(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player)) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("general.only-players", "%prefix% <red>This command can only be used by players.</red>"));
            return;
        }
        if (ctx.args().length < 1) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.move_here.usage", "%prefix% <red>Usage: /npc move_here (npc)</red>"));
            return;
        }
        String id = ctx.args()[0];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.move_here.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id));
            return;
        }
        Location loc = ((Player)ctx.sender()).getLocation();
        npcManager.modify(id, npcBuilder -> { npcBuilder.setLocation(loc); return npcBuilder; });
        npcManager.save();
        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.move_here.success", "%prefix% <green>NPC '%name%' moved to your current location.</green>").replace("%name%", id));
    }

    private void handleMoveTo(CommandContext ctx) {
        if (ctx.args().length < 4) {
            textManager.send(ctx.sender(), "Usage: /npc move_to <id> <x> <y> <z> [world] [--look-in-my-direction]");
            return;
        }
        String id = ctx.args()[0];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) { textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.move_to.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id)); return; }
        try {
            double x = Double.parseDouble(ctx.args()[1]);
            double y = Double.parseDouble(ctx.args()[2]);
            double z = Double.parseDouble(ctx.args()[3]);
            String worldName = ctx.args().length > 4 && !ctx.args()[4].startsWith("--") ? ctx.args()[4] : Bukkit.getWorlds().getFirst().getName();
            World w = Bukkit.getWorld(worldName);
            if (w == null) { textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.move_to.invalid-world", "%prefix% <red>World not found.</red>")); return; }
            Location loc = new Location(w, x, y, z);
            npcManager.modify(id, npcBuilder -> { npcBuilder.setLocation(loc); return npcBuilder; });
            npcManager.save();
            if (ctx.args().length > 4 && "--look-in-my-direction".equalsIgnoreCase(ctx.args()[ctx.args().length-1]) && ctx.sender() instanceof Player) {
                Location playerLoc = ((Player)ctx.sender()).getLocation();
                loc.setYaw(playerLoc.getYaw());
                loc.setPitch(playerLoc.getPitch());
                npcManager.modify(id, npcBuilder -> { npcBuilder.setLocation(loc); return npc; });
                npcManager.save();
            }
            textManager.send(ctx.sender(),  plugin.getMessagesFile().getString("commands.npc.move_to.success", "%prefix% <green>NPC '%name%' moved to location (%x%, %y%, %z%) in world '%world%'.</green>")
                    .replace("%name%", id)
                    .replace("%x%", String.valueOf(x))
                    .replace("%y%", String.valueOf(y))
                    .replace("%z%", String.valueOf(z))
                    .replace("%world%", worldName));
        } catch (NumberFormatException e) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.move_to.invalid-coordinates", "%prefix% <red>Invalid coordinates. Use /npc move_to (npc) x y z [world] [--look-in-my-direction].</red>"));
            return;
        }
    }

    private void handleCenter(CommandContext ctx) {
        if (ctx.args().length < 1) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.center.usage", "%prefix% <red>Usage: /npc center (npc)</red>"));
            return;
        }
        String id = ctx.args()[0];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) { textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.center.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id)); return; }
        Location l = npc.getLocation();
        Block b = l.getBlock();
        Location centered = new Location(b.getWorld(), b.getX()+0.5, b.getY(), b.getZ()+0.5, l.getYaw(), l.getPitch());
        npcManager.modify(id, n -> { n.setLocation(centered); return n; });
        npcManager.save();
        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.center.success", "%prefix% <green>NPC '%name%' centered to block (%x%, %y%, %z%) in world '%world%'.</green>")
                .replace("%name%", id));
    }

    private void handleNearby(CommandContext ctx) {
        double radius = 10.0; //
        if (ctx.args().length >= 2 && ctx.args()[0].equalsIgnoreCase("--radius")) {
            try { radius = Double.parseDouble(ctx.args()[1]); } catch (NumberFormatException ignored) {
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.nearby.invalid-radius", "%prefix% <red>Invalid radius '%radius%'. Must be a positive number.</red>").replace("%radius%", ctx.args()[1]));
                return;
            }
        }
        double finalRadius = radius;
        List<NPC> nearby = npcManager.getNpcs().values().stream()
                .filter(n -> n.getLocation() != null && n.getLocation().distance(((Player)ctx.sender()).getLocation()) <= finalRadius)
                .sorted(Comparator.comparing(n -> n.getLocation().distance(((Player)ctx.sender()).getLocation())))
                .toList();
        if (nearby.isEmpty()) { textManager.send(ctx.sender(), plugin.getMessagesFile().getString("command.npc.nearby.empty", "%prefix% <red>No nearby NPCs found.</red>")); return; }
        for (String header : plugin.getMessagesFile().getStringList("commands.npc.nearby.header")) {
            textManager.send(ctx.sender(), header.replace("%radius%", String.valueOf(finalRadius)));
        }
        nearby.forEach(n -> textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.nearby.line", "<gold>│</gold>  <hover:show_text:'Click to teleport to %name%'><click:run_command='/npc info %name%'><gray>%id% <yellow>%name%</yellow> <dark_gray>- </dark_gray>%away% blocks away</click></hover>")
                .replace("%id%", n.getId())
                .replace("%name%", n.getId())
                .replace("%away%", String.format("%.2f", n.getLocation().distance(((Player)ctx.sender()).getLocation())))
        ));
        for (String footer : plugin.getMessagesFile().getStringList("commands.npc.nearby.footer")) {
            textManager.send(ctx.sender(), footer.replace("%radius%", String.valueOf(finalRadius)));
        }
    }

    private void handleTeleport(CommandContext ctx) {
        if (!(ctx.sender() instanceof Player) || ctx.args().length < 1) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.teleport.usage", "%prefix% <red>Usage: /npc teleport (npc)</red>"));
            return;
        }
        Player p = (Player)ctx.sender();
        String id = ctx.args()[0];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) { textManager.send(p,
                plugin.getMessagesFile().getString("commands.npc.teleport.not-found", "%prefix% <red>NPC '%name%' not found.</red>").replace("%name%", id)
                ); return; }
        p.teleport(npc.getEntity().getLocation());
        textManager.send(p, plugin.getMessagesFile().getString("commands.npc.teleport.success", "%prefix% <green>Teleported to NPC '%name%' at (%x%, %y%, %z%) in world '%world%'.</green>")
                .replace("%name%", id)
                .replace("%x%", String.valueOf(npc.getLocation().getX()))
                .replace("%y%", String.valueOf(npc.getLocation().getY()))
                .replace("%z%", String.valueOf(npc.getLocation().getZ()))
                .replace("%world%", npc.getLocation().getWorld().getName())
        );
    }

    private void handleAction(CommandContext ctx) {
        if (ctx.args().length < 3) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.action.usage", "%prefix% <red>Usage: /npc action (npc) add|remove|clear|list [params]</red>"));
            return;
        }
        String id = ctx.args()[0];
        String sub = ctx.args()[1];
        NPC npc = npcManager.getNpcs().get(id);
        if (npc == null) { textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.action.not-found", "%prefix% <red>NPC '%name%' not found.</red>")); return; }
        switch (sub.toLowerCase()) {
            case "list":
                if (npc.getActions().isEmpty()) {
                    textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.action.list.empty", "%prefix% <yellow>NPC '%name%' has no actions.</yellow>").replace("%name%", id));
                } else {
                    for (String header : plugin.getMessagesFile().getStringList("commands.npc.action.list.header")) {
                        header = header.replace("%name%", id);
                        textManager.send(ctx.sender(), header);
                    }
                    for (String action : npc.getActions()) {
                        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.action.list.line", "<gold>│</gold>  <gray>%action%</gray>").replace("%action%", action));
                    }
                    for (String footer : plugin.getMessagesFile().getStringList("commands.npc.action.list.footer")) {
                        footer = footer.replace("%name%", id);
                        textManager.send(ctx.sender(), footer);
                    }
                }
                break;
            case "clear":
                npcManager.modify(id, n -> { n.getActions().clear(); return n; });
                npcManager.save();
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.action.clear.success", "%prefix% <green>All actions cleared for NPC '%name%'.</green>").replace("%name%", id));
                break;
            case "add":
                String action = ctx.args()[2];
                npcManager.modify(id, n -> { n.getActions().add(action); return n; });
                npcManager.save();
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.action.add.success", "%prefix% <green>Action '%action%' added to NPC '%name%'.</green>").replace("%action%", action).replace("%name%", id));
                break;
            case "remove":
                String rem = ctx.args()[2];
                npcManager.modify(id, n -> { n.getActions().remove(rem); return n; });
                npcManager.save();
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.action.remove.success", "%prefix% <green>Action '%action%' removed from NPC '%name%'.</green>").replace("%action%", rem).replace("%name%", id));
                break;
            default:
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.action.usage", "%prefix% <red>Usage: /npc action (npc) add|remove|clear|list [params]</red>"));
                break;
        }
    }

    private void handleInteractionCooldown(CommandContext ctx) {
        if (ctx.args().length < 2) {
            textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.interaction_cooldown.usage", "%prefix% <red>Usage: /npc interaction_cooldown (npc) [disabled | ticks]</red>"));
            return;
        }
        String id = ctx.args()[0];
        String arg = ctx.args()[1];
        int ticks = arg.equalsIgnoreCase("disabled") ? 0 : 20; // Default to 20 ticks (1 second) if not specified
        if (!arg.equalsIgnoreCase("disabled")) {
            try { ticks = Integer.parseInt(arg); } catch (NumberFormatException e) {
                textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.interaction_cooldown.invalid-ticks", "%prefix% <red>Invalid duration '%ticks%'. Must be a positive number or 'disabled'.</red>").replace("%ticks%", arg));
                return;
            }
        }
        int finalTicks = ticks;
        npcManager.modify(id, n -> { n.setInteractionCooldown(finalTicks); return n; });
        npcManager.save();
        textManager.send(ctx.sender(), plugin.getMessagesFile().getString("commands.npc.interaction_cooldown.success", "%prefix% <green>NPC '%name%' interaction cooldown set to %ticks% ticks.</green>")
                .replace("%name%", id)
                .replace("%ticks%", finalTicks == 0 ? "disabled" : String.valueOf(finalTicks)));
    }
}
