package gg.nextforge.protocol;

import gg.nextforge.protocol.packet.PacketContainer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.mozilla.javascript.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JavaScript engine for packet filtering.
 *
 * This class provides functionality for compiling and executing JavaScript-based
 * packet filters using the Rhino engine. Filters can be used to allow or cancel
 * packets based on custom logic written in JavaScript.
 */
public class ScriptEngine {

    private final Plugin plugin; // The plugin instance for logging and context
    private final Context context; // Rhino context for script execution
    private final ScriptableObject scope; // Global scope for JavaScript execution
    private final Map<String, Script> compiledScripts = new ConcurrentHashMap<>(); // Cache for compiled scripts

    /**
     * Constructs a ScriptEngine instance.
     *
     * @param plugin The plugin instance for logging and context.
     */
    public ScriptEngine(Plugin plugin) {
        this.plugin = plugin;

        // Initialize Rhino context
        this.context = Context.enter();
        context.setLanguageVersion(Context.VERSION_ES6); // Use ES6 language version
        context.setOptimizationLevel(9); // Maximum optimization level for performance

        // Create global scope
        this.scope = context.initStandardObjects();

        // Add custom global objects
        ScriptableObject.putProperty(scope, "console", new ConsoleObject());
    }

    /**
     * Compiles a JavaScript filter.
     *
     * @param name   The unique name for the filter.
     * @param source The JavaScript source code for the filter.
     * @throws ScriptException If the script fails to compile.
     */
    public void compileFilter(String name, String source) throws ScriptException {
        try {
            Script script = context.compileString(source, name, 1, null);
            compiledScripts.put(name, script);
        } catch (Exception e) {
            throw new ScriptException("Failed to compile script: " + e.getMessage());
        }
    }

    /**
     * Executes a compiled JavaScript filter on a packet.
     *
     * @param name   The name of the filter to execute.
     * @param player The player associated with the packet.
     * @param packet The packet to filter.
     * @return true to allow the packet, false to cancel it.
     */
    public boolean executeFilter(String name, Player player, PacketContainer packet) {
        Script script = compiledScripts.get(name);
        if (script == null) {
            plugin.getLogger().warning("Unknown script filter: " + name);
            return true; // Allow by default
        }

        try {
            // Create a new scope for script execution
            Scriptable executionScope = context.newObject(scope);
            executionScope.setPrototype(scope);
            executionScope.setParentScope(null);

            // Add packet and player to the scope
            ScriptableObject.putProperty(executionScope, "packet",
                    Context.javaToJS(packet, executionScope));
            ScriptableObject.putProperty(executionScope, "player",
                    Context.javaToJS(player, executionScope));

            // Execute the script
            Object result = script.exec(context, executionScope);

            // Convert the result to a boolean
            if (result instanceof Boolean) {
                return (Boolean) result;
            } else if (result instanceof Number) {
                return ((Number) result).intValue() != 0;
            } else {
                return true; // Default to allow
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error executing script filter " + name + ": " + e.getMessage());
            return true; // Allow on error
        }
    }

    /**
     * Removes a compiled filter.
     *
     * @param name The name of the filter to remove.
     */
    public void removeFilter(String name) {
        compiledScripts.remove(name);
    }

    /**
     * Shuts down the script engine.
     * Cleans up the Rhino context and clears compiled scripts.
     */
    public void shutdown() {
        compiledScripts.clear();
        Context.exit();
    }

    /**
     * Custom exception for script errors.
     * Used to indicate issues during script compilation or execution.
     */
    public static class ScriptException extends Exception {
        /**
         * Constructs a ScriptException instance.
         *
         * @param message The error message describing the issue.
         */
        public ScriptException(String message) {
            super(message);
        }
    }

    /**
     * Fake console object for JavaScript.
     * Provides logging functionality for scripts using console.log(), console.error(), etc.
     */
    private class ConsoleObject extends ScriptableObject {
        @Override
        public String getClassName() {
            return "Console";
        }

        /**
         * Logs messages to the plugin's logger at the INFO level.
         *
         * @param args The arguments to log.
         */
        public void log(Object... args) {
            StringBuilder sb = new StringBuilder("[JS] ");
            for (Object arg : args) {
                sb.append(Context.toString(arg)).append(" ");
            }
            plugin.getLogger().info(sb.toString());
        }

        /**
         * Logs messages to the plugin's logger at the SEVERE level.
         *
         * @param args The arguments to log.
         */
        public void error(Object... args) {
            StringBuilder sb = new StringBuilder("[JS ERROR] ");
            for (Object arg : args) {
                sb.append(Context.toString(arg)).append(" ");
            }
            plugin.getLogger().severe(sb.toString());
        }

        /**
         * Logs messages to the plugin's logger at the WARNING level.
         *
         * @param args The arguments to log.
         */
        public void warn(Object... args) {
            StringBuilder sb = new StringBuilder("[JS WARN] ");
            for (Object arg : args) {
                sb.append(Context.toString(arg)).append(" ");
            }
            plugin.getLogger().warning(sb.toString());
        }
    }
}