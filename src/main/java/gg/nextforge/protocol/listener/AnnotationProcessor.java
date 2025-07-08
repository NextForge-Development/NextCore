package gg.nextforge.protocol.listener;

import gg.nextforge.plugin.NextForgePlugin;
import gg.nextforge.protocol.packet.PacketContainer;
import gg.nextforge.protocol.packet.PacketType;
import gg.nextforge.protocol.ProtocolManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Processes <code>@PacketHandler</code> annotations to automatically register methods as packet listeners.
 *
 * This class scans objects for methods annotated with <code>@PacketHandler</code> and registers them
 * as packet listeners using reflection. It supports validation of method signatures,
 * parsing packet types, and applying JavaScript filters for additional control.
 *
 * @see gg.nextforge.protocol.listener.PacketAdapter.PacketHandler
 */
public class AnnotationProcessor {

    private final ProtocolManager protocol; // The protocol manager used for listener registration
    private final Map<Object, List<PacketListener>> registeredListeners = new HashMap<>(); // Tracks registered listeners for each object

    /**
     * Constructs an AnnotationProcessor instance.
     *
     * @param protocol The ProtocolManager instance used for listener registration.
     */
    public AnnotationProcessor(ProtocolManager protocol) {
        this.protocol = protocol;
    }

    /**
     * Registers all @PacketHandler methods in the given object.
     *
     * This method scans the object's methods using reflection, validates their signatures,
     * parses packet types, and registers them as listeners. It logs warnings for invalid
     * method signatures or unknown packet types.
     *
     * @param plugin The plugin instance associated with the listeners.
     * @param handler The object containing methods annotated with @PacketHandler.
     */
    public void registerHandlers(Plugin plugin, Object handler) {
        List<PacketListener> listeners = new ArrayList<>();

        // Scan all methods in the object
        for (Method method : handler.getClass().getDeclaredMethods()) {
            PacketAdapter.PacketHandler annotation = method.getAnnotation(PacketAdapter.PacketHandler.class);
            if (annotation == null) continue;

            // Validate method signature
            Class<?>[] params = method.getParameterTypes();
            if (params.length != 2 || params[0] != Player.class || params[1] != PacketContainer.class) {
                plugin.getLogger().warning(
                        "Invalid @PacketHandler method signature: " + method.getName() +
                        " - Expected (Player, PacketContainer)"
                );
                continue;
            }

            // Parse packet types from the annotation
            Set<PacketType> types = new HashSet<>();
            for (String typeName : annotation.types()) {
                try {
                    types.add(PacketType.valueOf(typeName));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Unknown packet type: " + typeName);
                }
            }

            // Create and register the listener
            PacketListener listener = new MethodPacketListener(plugin, annotation, types, handler, method);
            protocol.registerListener(listener);
            listeners.add(listener);
        }

        // Store registered listeners for the object
        if (!listeners.isEmpty()) {
            registeredListeners.put(handler, listeners);
        }
    }

    /**
     * Unregisters all @PacketHandler methods for the given object.
     *
     * This method removes all listeners associated with the object and unregisters them
     * from the protocol manager to prevent memory leaks.
     *
     * @param handler The object whose listeners should be unregistered.
     */
    public void unregisterHandlers(Object handler) {
        List<PacketListener> listeners = registeredListeners.remove(handler);
        if (listeners != null) {
            listeners.forEach(protocol::unregisterListener);
        }
    }

    /**
     * Internal listener class that delegates packet handling to annotated methods.
     */
    private static class MethodPacketListener extends PacketListener {
        private final Object handler; // The object containing the annotated method
        private final Method method; // The annotated method to invoke
        private final boolean sending; // Whether the listener handles sending packets
        private final boolean receiving; // Whether the listener handles receiving packets
        private final String filter; // Optional JavaScript filter for packet handling

        /**
         * Constructs a MethodPacketListener instance.
         *
         * @param plugin The plugin instance associated with the listener.
         * @param annotation The @PacketHandler annotation on the method.
         * @param types The packet types to listen for.
         * @param handler The object containing the annotated method.
         * @param method The annotated method to invoke.
         */
        MethodPacketListener(Plugin plugin, PacketAdapter.PacketHandler annotation, Set<PacketType> types, Object handler, Method method) {
            super(plugin, annotation.priority(),
                  annotation.sending() ? types : Collections.emptySet(),
                  annotation.receiving() ? types : Collections.emptySet());

            this.handler = handler;
            this.method = method;
            this.sending = annotation.sending();
            this.receiving = annotation.receiving();
            this.filter = annotation.filter();

            method.setAccessible(true); // Allows access to private methods
        }

        /**
         * Handles outgoing packets by invoking the annotated method.
         *
         * @param player The player receiving the packet.
         * @param packet The packet being sent.
         * @return true to allow the packet, false to cancel it.
         */
        @Override
        public boolean onPacketSending(Player player, PacketContainer packet) {
            if (!sending) return true;
            return invokeMethod(player, packet);
        }

        /**
         * Handles incoming packets by invoking the annotated method.
         *
         * @param player The player sending the packet.
         * @param packet The packet being received.
         * @return true to allow the packet, false to cancel it.
         */
        @Override
        public boolean onPacketReceiving(Player player, PacketContainer packet) {
            if (!receiving) return true;
            return invokeMethod(player, packet);
        }

        /**
         * Invokes the annotated method to handle the packet.
         *
         * This method applies an optional JavaScript filter before invoking the method.
         * If the filter rejects the packet, the method is not invoked.
         *
         * @param player The player associated with the packet.
         * @param packet The packet being handled.
         * @return true to allow the packet, false to cancel it.
         */
        private boolean invokeMethod(Player player, PacketContainer packet) {
            try {
                // Apply JavaScript filter if specified
                if (filter != null && !filter.isEmpty()) {
                    var scriptEngine = NextForgePlugin.getInstance().getProtocolManager().getScriptEngine();
                    String tempName = "temp_" + System.nanoTime();
                    scriptEngine.compileFilter(tempName, filter);
                    boolean filterResult = scriptEngine.executeFilter(tempName, player, packet);
                    scriptEngine.removeFilter(tempName);

                    if (!filterResult) {
                        return false; // Filter rejected the packet
                    }
                }

                // Invoke the annotated method
                Object result = method.invoke(handler, player, packet);

                // Use the method's return value if it is a boolean
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }

                return true; // Default to allow the packet

            } catch (Exception e) {
                getPlugin().getLogger().severe(
                        "Error in @PacketHandler method " + method.getName() + ": " + e.getMessage()
                );
                return true;
            }
        }
    }
}