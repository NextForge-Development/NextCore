package gg.nextforge.bridge.impl;

import gg.nextforge.bridge.PluginBridge;
import gg.nextforge.bridge.annotations.Bridge;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Integration bridge for Vault (Economy, Chat, Permissions).
 */
@Bridge(description = "Integration with Vault")
public class VaultBridge extends PluginBridge {

    private Economy economy;
    private Permission permission;
    private Chat chat;

    @Override
    public String getPluginName() {
        return "Vault";
    }

    @Override
    public void onEnable(Plugin plugin) {
        this.economy = getProvider(Economy.class);
        this.permission = getProvider(Permission.class);
        this.chat = getProvider(Chat.class);
    }

    private <T> T getProvider(Class<T> clazz) {
        RegisteredServiceProvider<T> provider = Bukkit.getServicesManager().getRegistration(clazz);
        return provider != null ? provider.getProvider() : null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public Permission getPermission() {
        return permission;
    }

    public Chat getChat() {
        return chat;
    }

    public boolean isEconomyAvailable() {
        return economy != null;
    }

    public boolean isPermissionAvailable() {
        return permission != null;
    }

    public boolean isChatAvailable() {
        return chat != null;
    }
}
