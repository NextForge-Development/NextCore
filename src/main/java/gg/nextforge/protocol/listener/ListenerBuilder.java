package gg.nextforge.protocol.listener;

 import gg.nextforge.protocol.packet.PacketContainer;
 import gg.nextforge.protocol.packet.PacketType;
 import org.bukkit.entity.Player;
 import org.bukkit.plugin.Plugin;
 import gg.nextforge.protocol.ProtocolManager;
 import org.jetbrains.annotations.ApiStatus;

 import java.util.Arrays;
 import java.util.HashSet;
 import java.util.Set;
 import java.util.concurrent.TimeUnit;
 import java.util.function.BiFunction;
 import java.util.function.Predicate;

 /**
  * Fluent builder for packet listeners.
  */
 public class ListenerBuilder {

     private final Plugin plugin;
     private ListenerPriority priority = ListenerPriority.NORMAL;
     private final Set<PacketType> sendingTypes = new HashSet<>();
     private final Set<PacketType> receivingTypes = new HashSet<>();
     private BiFunction<Player, PacketContainer, Boolean> sendHandler;
     private BiFunction<Player, PacketContainer, Boolean> receiveHandler;
     private Predicate<Player> playerFilter;
     private Long expirationTime;
     private Integer maxPackets;
     private Runnable onExpire;

     private ListenerBuilder(Plugin plugin) {
         this.plugin = plugin;
     }

     public static ListenerBuilder create(Plugin plugin) {
         return new ListenerBuilder(plugin);
     }

     public ListenerBuilder priority(ListenerPriority priority) {
         this.priority = priority;
         return this;
     }

     public ListenerBuilder packets(PacketType... types) {
         for (PacketType type : types) {
             if (type.getDirection() == PacketType.Direction.SERVERBOUND) {
                 receivingTypes.add(type);
             } else {
                 sendingTypes.add(type);
             }
         }
         return this;
     }

     public ListenerBuilder sending(PacketType... types) {
         sendingTypes.addAll(Arrays.asList(types));
         return this;
     }

     public ListenerBuilder receiving(PacketType... types) {
         receivingTypes.addAll(Arrays.asList(types));
         return this;
     }

     public ListenerBuilder onSend(BiFunction<Player, PacketContainer, Boolean> handler) {
         this.sendHandler = handler;
         return this;
     }

     public ListenerBuilder onReceive(BiFunction<Player, PacketContainer, Boolean> handler) {
         this.receiveHandler = handler;
         return this;
     }

     public ListenerBuilder forPlayer(Predicate<Player> filter) {
         this.playerFilter = filter;
         return this;
     }

     public ListenerBuilder expireAfter(long duration, TimeUnit unit) {
         this.expirationTime = System.currentTimeMillis() + unit.toMillis(duration);
         return this;
     }

     public ListenerBuilder expireAfterPackets(int count) {
         this.maxPackets = count;
         return this;
     }

     public ListenerBuilder onExpire(Runnable callback) {
         this.onExpire = callback;
         return this;
     }

     public PacketListener register(ProtocolManager protocol) {
         PacketListener listener = (expirationTime != null || maxPackets != null)
                 ? createExpiringListener()
                 : createNormalListener();

         protocol.registerListener(listener);
         return listener;
     }

     @ApiStatus.Internal
     private PacketListener createExpiringListener() {
         return new ExpiringPacketListener(plugin,
                 expirationTime != null ? expirationTime - System.currentTimeMillis() : Long.MAX_VALUE,
                 TimeUnit.MILLISECONDS,
                 sendingTypes, receivingTypes) {

             @Override
             protected boolean handlePacket(Player player, PacketContainer packet, boolean sending) {
                 if (playerFilter != null && !playerFilter.test(player)) return true;
                 return sending ? handleSend(player, packet) : handleReceive(player, packet);
             }
         };
     }

     @ApiStatus.Internal
     private PacketListener createNormalListener() {
         return new PacketAdapter(plugin, priority, sendingTypes, receivingTypes) {
             @Override
             public boolean onPacketSending(Player player, PacketContainer packet) {
                 return playerFilter != null && !playerFilter.test(player) ? true : handleSend(player, packet);
             }

             @Override
             public boolean onPacketReceiving(Player player, PacketContainer packet) {
                 return playerFilter != null && !playerFilter.test(player) ? true : handleReceive(player, packet);
             }
         };
     }

     @ApiStatus.Internal
     private boolean handleSend(Player player, PacketContainer packet) {
         return sendHandler != null ? sendHandler.apply(player, packet) : true;
     }

     @ApiStatus.Internal
     private boolean handleReceive(Player player, PacketContainer packet) {
         return receiveHandler != null ? receiveHandler.apply(player, packet) : true;
     }
 }