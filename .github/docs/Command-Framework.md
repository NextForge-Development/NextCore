# ⚔️ NextForge Command Framework (Dynamic Registration)

A lightweight, annotation-driven command system for Bukkit/Paper that **does not require `plugin.yml`**.
Commands are **registered dynamically** into the server `CommandMap`, integrate with **DI (ServiceRegistry)** and
use **i18n (MiniMessage + YAML)** for messages. Includes **auto tab-completion** and per-subcommand permissions.

---

## ✨ Features

- **No `plugin.yml` required** – dynamic registration via `CommandMap`
- **Annotations**: `@Command`, `@Subcommand`, `@TabComplete`
- **Auto Tab-Completion** (root + subcommand-specific)
- **DI integration** – command instances get injected (`Injector.wire`)
- **i18n integration** – user-facing messages come from `I18n`
- **Per-command & per-subcommand permissions**
- Safe, reflection-based execution signatures

---

## 📦 Package & Classes

```
gg.nextforge.core.commands
├─ Command.java        (@Command on classes)
├─ Subcommand.java     (@Subcommand on methods)
├─ TabComplete.java    (@TabComplete on methods)
└─ CommandManager.java (dynamic registration & dispatch)
```

---

## 🔖 Annotations

### `@Command`
```java
@Documented @Retention(RUNTIME) @Target(TYPE)
public @interface Command {
  String name();                 // primary command label
  String[] aliases() default {}; // alternative labels
  String permission() default ""; // base permission (optional)
  String descriptionKey() default ""; // i18n key for description (optional)
}
```

### `@Subcommand`
```java
@Documented @Retention(RUNTIME) @Target(METHOD)
public @interface Subcommand {
  String value();                 // e.g. "reload", "hello"
  String permission() default ""; // optional per-sub permission
  String descriptionKey() default ""; // i18n key (optional)
}
```

### `@TabComplete`
```java
@Documented @Retention(RUNTIME) @Target(METHOD)
public @interface TabComplete {
  String value() default "";      // "" = root completer, or a sub-name like "hello"
}
```

---

## 🧠 Supported Method Signatures

For `@Subcommand` methods:
- `(CommandSender sender, String[] args)`
- `(Player player, String[] args)`
- `(CommandSender sender)`
- `(Player player)`
- `()`

For `@TabComplete` methods:
- `(CommandSender sender, String[] args)`
- `(Player player, String[] args)`
- `(CommandSender sender)`
- `(Player player)`
- `()` → returns `List<String>`

If the signature does not match these forms, a localized error (`command.bad_signature`) is returned.

---

## 🚀 Usage

### 1) Create a command class
```java
package gg.nextforge.core.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Command(name = "nextforge", aliases = {"nf"}, descriptionKey = "command.nextforge.desc")
public class NextForgeCommand {

  @Subcommand("reload")
  public void reload(CommandSender sender) {
    sender.sendMessage("Reloading…");
  }

  @Subcommand("hello")
  public void hello(Player player, String[] args) {
    String who = args.length > 0 ? args[0] : player.getName();
    player.sendMessage("Hello, " + who + "!");
  }

  @TabComplete("") // root suggestions
  public java.util.List<String> rootTabs(CommandSender sender, String[] args) {
    return java.util.List.of("reload", "hello");
  }

  @TabComplete("hello")
  public java.util.List<String> helloTabs(Player player, String[] args) {
    return java.util.List.of("Steve", "Alex", "Max");
  }
}
```

### 2) Register dynamically in your plugin
```java
@Override
protected void beforeEnable(gg.nextforge.core.plugin.inject.ServiceRegistry services) {
  var cmdMgr = new gg.nextforge.core.commands.CommandManager(this, services);
  services.register(gg.nextforge.core.commands.CommandManager.class, cmdMgr);

  cmdMgr.register(new gg.nextforge.core.commands.NextForgeCommand());
}

@Override
public void disable() {
  services().get(gg.nextforge.core.commands.CommandManager.class)
            .ifPresent(gg.nextforge.core.commands.CommandManager::unregisterAll);
}
```

> `CommandManager` wires your command instance via **DI** (`Injector.wire`) and registers a reflective `Command` in the server `CommandMap`.  
> `unregisterAll()` removes dynamically registered commands on plugin disable (Paper has direct API; on Spigot it falls back to reflection).

---

## 🧩 i18n Integration

- Command descriptions (`descriptionKey`) and messages (e.g., `command.usage`, `command.unknown`, `command.no_permission`, `command.error`, `command.bad_signature`) are read via the `I18n` service.  
- Ensure your YAML contains these keys, e.g. in `en.yml`:

```yaml
command:
  usage: "<gray>Usage: <yellow><label></yellow></gray>"
  unknown: "<red>Unknown subcommand: <yellow><sub></yellow></red>"
  error: "<red>An internal error occurred.</red>"
  no_permission: "<red>You don't have permission.</red>"
  bad_signature: "<red>Unsupported command method signature.</red>"
  nextforge:
    desc: "Core commands for NextForge."
```

---

## 🧰 Tab Completion

- Root completion: method annotated with `@TabComplete("")` (no sub value).  
- Sub-specific completion: `@TabComplete("subname")`.  
- If no completer exists, an empty list is returned.  
- The framework automatically filters root subcommands by **permission**.

---

## 🔐 Permissions

- Base permission from `@Command(permission="...")` is checked first.  
- Subcommand permission from `@Subcommand(permission="...")` is checked per invocation.  
- If missing, access is allowed by default.

---

## 🧪 Tips & Best Practices

- Keep command methods tiny — delegate heavy work to services (DI).  
- Use i18n `MiniMessage` for colorful, consistent messages.  
- Avoid blocking operations on the main thread; move to your **Scheduler** and return to main for output.  
- Group related subcommands in the same class to keep discovery cheap.

---

## ❓ FAQ

**Do I still need `plugin.yml`?**  
No. This system registers commands directly against the `CommandMap` at runtime.

**Is it Paper-only?**  
It works on Paper and Spigot. Unregister uses Paper API when available; on Spigot it falls back to reflection.

**Can I scan a package and auto-register all commands?**  
Yes — add a simple classpath scanner and call `cmdMgr.register(instance)` for each discovered class. (Ask if you want a ready-made helper.)

---

## 🔗 Related

- `gg.nextforge.core.i18n.*` – YAML + MiniMessage i18n system
- `gg.nextforge.core.plugin.inject.*` – DI container & Injector
- `gg.nextforge.core.scheduler.*` – Custom scheduler for async/sync tasks

Happy commanding!
