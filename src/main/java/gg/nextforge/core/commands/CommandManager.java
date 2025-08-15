// gg/nextforge/core/commands/CommandManager.java
package gg.nextforge.core.commands;

import gg.nextforge.core.NextCore;
import gg.nextforge.core.commands.annotation.Command;
import gg.nextforge.core.commands.annotation.Subcommand;
import gg.nextforge.core.commands.annotation.TabComplete;
import gg.nextforge.core.i18n.I18n;
import gg.nextforge.core.plugin.inject.Injector;
import gg.nextforge.core.plugin.inject.ServiceRegistry;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class CommandManager {

    private final JavaPlugin plugin;
    private final ServiceRegistry services;
    private final I18n i18n;
    private final Audience audiences;
    private final List<org.bukkit.command.Command> registered = new CopyOnWriteArrayList<>();

    public CommandManager(JavaPlugin plugin, ServiceRegistry services) {
        this.plugin = plugin;
        this.services = services;
        this.i18n = services.get(I18n.class).orElseThrow();
        this.audiences = Audience.audience(plugin.getServer());
    }

    public void register(Object commandInstance) {
        Class<?> clazz = commandInstance.getClass();
        gg.nextforge.core.commands.annotation.Command meta = clazz.getAnnotation(gg.nextforge.core.commands.annotation.Command.class);
        if (meta == null) throw new IllegalArgumentException("Missing @Command on " + clazz.getName());

        Injector.wire(commandInstance, services);

        Map<String, Method> subMap = new HashMap<>();
        Map<String, Method> tabMap = new HashMap<>();
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Subcommand.class) && Modifier.isPublic(m.getModifiers())) {
                subMap.put(m.getAnnotation(Subcommand.class).value().toLowerCase(Locale.ROOT), m);
            }
            if (m.isAnnotationPresent(TabComplete.class) && Modifier.isPublic(m.getModifiers())) {
                tabMap.put(m.getAnnotation(TabComplete.class).value().toLowerCase(Locale.ROOT), m);
            }
        }

        String desc = meta.descriptionKey().isBlank() ? "" : i18n.raw(null, meta.descriptionKey());
        ReflectiveCommand dyn = new ReflectiveCommand(
                meta.name(),
                desc,
                meta.permission(),
                Arrays.asList(meta.aliases()),
                (sender, label, args) -> execute(commandInstance, meta, subMap, sender, label, args),
                (sender, label, args) -> tabComplete(commandInstance, subMap, tabMap, sender, label, args)
        );
        dyn.setLabel(meta.name());

        CommandMap map = getCommandMap();
        map.register(plugin.getName().toLowerCase(Locale.ROOT), dyn);
        registered.add(dyn);
    }

    public void unregisterAll() {
        try {
            CommandMap map = getCommandMap();
            var known = map.getKnownCommands(); // Paper API; auf Spigot per Reflection ersetzbar
            for (org.bukkit.command.Command cmd : registered) {
                cmd.unregister(map);
                known.entrySet().removeIf(e -> e.getValue() == cmd);
            }
            registered.clear();
        } catch (Throwable t) {
            plugin.getSLF4JLogger().warn("Failed to unregister commands cleanly", t);
        }
    }

    /* ---------------- intern ---------------- */

    @SuppressWarnings("deprecation")
    private CommandMap getCommandMap() {
        try {
            var m = Bukkit.getServer().getClass().getMethod("getCommandMap");
            m.setAccessible(true);
            return (CommandMap) m.invoke(Bukkit.getServer());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot access CommandMap", e);
        }
    }

    private boolean execute(Object instance, Command meta, Map<String, Method> subs,
                            CommandSender sender, String label, String[] args) {
        if (!meta.permission().isBlank() && !sender.hasPermission(meta.permission())) {
            send(sender, i18n.component(sender, "command.no_permission"));
            return true;
        }
        if (args.length == 0) {
            send(sender, i18n.component(sender, "command.usage", I18n.vars("label", "/" + label)));
            return true;
        }
        String name = args[0].toLowerCase(Locale.ROOT);
        Method m = subs.get(name);
        if (m == null) {
            send(sender, i18n.component(sender, "command.unknown", I18n.vars("sub", name)));
            return true;
        }
        Subcommand sc = m.getAnnotation(Subcommand.class);
        if (sc != null && !sc.permission().isBlank() && !sender.hasPermission(sc.permission())) {
            send(sender, i18n.component(sender, "command.no_permission"));
            return true;
        }
        try {
            m.setAccessible(true);
            Class<?>[] pt = m.getParameterTypes();
            Object[] call;
            if (pt.length == 2 && pt[0].isAssignableFrom(Player.class) && sender instanceof Player p) {
                call = new Object[]{p, Arrays.copyOfRange(args, 1, args.length)};
            } else if (pt.length == 2 && pt[0].isAssignableFrom(CommandSender.class)) {
                call = new Object[]{sender, Arrays.copyOfRange(args, 1, args.length)};
            } else if (pt.length == 1 && pt[0].isAssignableFrom(Player.class) && sender instanceof Player p) {
                call = new Object[]{p};
            } else if (pt.length == 1 && pt[0].isAssignableFrom(CommandSender.class)) {
                call = new Object[]{sender};
            } else if (pt.length == 0) {
                call = new Object[]{};
            } else {
                send(sender, i18n.component(sender, "command.bad_signature"));
                return true;
            }
            m.invoke(instance, call);
        } catch (Exception ex) {
            plugin.getSLF4JLogger().error("Command execution failed", ex);
            send(sender, i18n.component(sender, "command.error"));
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<String> tabComplete(Object instance, Map<String, Method> subs, Map<String, Method> tabs,
                                     CommandSender sender, String label, String[] args) {
        try {
            if (args.length <= 1) {
                List<String> base = new ArrayList<>();
                for (var e : subs.entrySet()) {
                    Method m = e.getValue();
                    Subcommand sc = m.getAnnotation(Subcommand.class);
                    if (sc != null && !sc.permission().isBlank() && !sender.hasPermission(sc.permission())) continue;
                    base.add(e.getKey());
                }
                base.sort(String::compareToIgnoreCase);
                return filterPrefix(base, args.length == 0 ? "" : args[0]);
            }
            String key = args[0].toLowerCase(Locale.ROOT);
            Method t = tabs.getOrDefault(key, tabs.get(""));
            if (t == null) return Collections.emptyList();

            t.setAccessible(true);
            Class<?>[] pt = t.getParameterTypes();
            Object res;
            if (pt.length == 2 && pt[0].isAssignableFrom(Player.class) && sender instanceof Player p) {
                res = t.invoke(instance, p, args);
            } else if (pt.length == 2 && pt[0].isAssignableFrom(CommandSender.class)) {
                res = t.invoke(instance, sender, args);
            } else if (pt.length == 1 && pt[0].isAssignableFrom(Player.class) && sender instanceof Player p) {
                res = t.invoke(instance, p);
            } else if (pt.length == 1 && pt[0].isAssignableFrom(CommandSender.class)) {
                res = t.invoke(instance, sender);
            } else {
                res = Collections.emptyList();
            }
            return (res instanceof List) ? (List<String>) res : Collections.emptyList();
        } catch (Exception e) {
            plugin.getSLF4JLogger().warn("TabComplete error", e);
            return Collections.emptyList();
        }
    }

    private static List<String> filterPrefix(List<String> list, String prefix) {
        if (prefix == null || prefix.isEmpty()) return list;
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>(list.size());
        for (String s : list) if (s.toLowerCase(Locale.ROOT).startsWith(p)) out.add(s);
        return out;
    }

    private void send(CommandSender sender, Component component) {
        try {
            if (sender instanceof org.bukkit.command.ConsoleCommandSender) {
                sender.sendMessage(net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component));
            } else {
                audiences.sendMessage(component);
            }
        } catch (Throwable t) {
            sender.sendMessage(component.toString());
        }
    }

    /* ---------- reflective command ---------- */

    private static final class ReflectiveCommand extends org.bukkit.command.Command implements PluginIdentifiableCommand {
        private final String permission;
        private final Exec exec;
        private final Tab tab;
        private final Plugin plugin;

        interface Exec { boolean call(CommandSender sender, String label, String[] args); }
        interface Tab { List<String> call(CommandSender sender, String label, String[] args); }

        ReflectiveCommand(String name, String description, String permission, List<String> aliases,
                          Exec exec, Tab tab) {
            super(name);
            this.setDescription(description == null ? "" : description);
            this.setAliases(aliases == null ? List.of() : aliases);
            this.permission = (permission == null ? "" : permission);
            this.exec = exec;
            this.tab = tab;
            this.plugin = Bukkit.getPluginManager().getPlugin(Bukkit.getServer().getPluginManager().getPlugins()[0].getName()); // fallback; replaced below via setPlugin
        }

        @Override public boolean execute(CommandSender sender, String label, String[] args) {
            if (permission != null && !permission.isBlank() && !sender.hasPermission(permission)) {
                sender.sendMessage("No permission.");
                return true;
            }
            return exec.call(sender, label, args);
        }

        @Override public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
            return tab.call(sender, alias, args);
        }

        @Override public Plugin getPlugin() { return NextCore.getInstance(); }
    }
}
