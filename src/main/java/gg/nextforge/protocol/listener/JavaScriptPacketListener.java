package gg.nextforge.protocol.listener;

import gg.nextforge.protocol.ScriptEngine;
import gg.nextforge.protocol.packet.PacketContainer;
import gg.nextforge.protocol.packet.PacketType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Set;

/**
 * JavaScript-based packet filter listener.<br>
 *<br>
 * This class allows filtering of packets using JavaScript code. The JavaScript code<br>
 * is executed through a script engine and determines whether to allow or cancel packets.<br>
 *<br>
 * Features:<br>
 * - Supports filtering both incoming and outgoing packets.<br>
 * - Uses a script engine to execute JavaScript filters.<br>
 * - Provides access to player and packet objects in the JavaScript code.<br>
 */
public class JavaScriptPacketListener extends PacketListener {

    private final ScriptEngine scriptEngine; // The script engine used to execute JavaScript filters
    private final String filterName; // The name of the JavaScript filter

    /**
     * Constructs a JavaScriptPacketListener instance.
     *
     * @param plugin The plugin instance associated with this listener.
     * @param scriptEngine The script engine used to execute JavaScript filters.
     * @param filterName The name of the JavaScript filter.
     * @param priority The priority of the listener.
     * @param sendingTypes The packet types to listen for when sending.
     * @param receivingTypes The packet types to listen for when receiving.
     */
    public JavaScriptPacketListener(Plugin plugin, ScriptEngine scriptEngine,
                                    String filterName, ListenerPriority priority,
                                    Set<PacketType> sendingTypes,
                                    Set<PacketType> receivingTypes) {
        super(plugin, priority, sendingTypes, receivingTypes);
        this.scriptEngine = scriptEngine;
        this.filterName = filterName;
    }

    /**
     * Called when a packet is being sent to a player.
     * Executes the JavaScript filter to determine whether to allow or cancel the packet.
     *
     * @param player The player receiving the packet.
     * @param packet The packet being sent.
     * @return true to allow the packet, false to cancel it.
     */
    @Override
    public boolean onPacketSending(Player player, PacketContainer packet) {
        return scriptEngine.executeFilter(filterName, player, packet);
    }

    /**
     * Called when a packet is received from a player.
     * Executes the JavaScript filter to determine whether to allow or cancel the packet.
     *
     * @param player The player sending the packet.
     * @param packet The packet being received.
     * @return true to allow the packet, false to cancel it.
     */
    @Override
    public boolean onPacketReceiving(Player player, PacketContainer packet) {
        return scriptEngine.executeFilter(filterName, player, packet);
    }

    /**
     * Gets the name of the JavaScript filter associated with this listener.
     *
     * @return The name of the JavaScript filter.
     */
    public String getFilterName() {
        return filterName;
    }
}