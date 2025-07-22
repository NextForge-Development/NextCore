package gg.nextforge.bridge;

import gg.nextforge.bridge.annotations.Bridge;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Integration bridge for LuckPerms.
 */
@Bridge(description = "Integration with LuckPerms")
public class LuckPermsBridge extends PluginBridge {

    private LuckPerms api;

    @Override
    public String getPluginName() {
        return "LuckPerms";
    }

    @Override
    public void onEnable(Plugin plugin) {
        this.api = LuckPermsProvider.get();
    }

    public LuckPerms getApi() {
        return api;
    }

    public boolean hasPermission(Player player, String permissionNode) {
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user == null) return false;

        return user.getCachedData().getPermissionData().checkPermission(permissionNode).asBoolean();
    }

    public void addPermission(Player player, String permissionNode) {
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            user.data().add(Node.builder(permissionNode).build());
            api.getUserManager().saveUser(user);
        }
    }

    public void removePermission(Player player, String permissionNode) {
        User user = api.getUserManager().getUser(player.getUniqueId());
        if (user != null) {
            user.data().remove(Node.builder(permissionNode).build());
            api.getUserManager().saveUser(user);
        }
    }
}
