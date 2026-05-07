package com.Clansystem;

import com.Clansystem.Command.ClansCommand;
import com.Clansystem.Clanlistener.ClanListener;
import com.Clansystem.Manager.ClanManager;
import com.Clansystem.invite.InviteManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class ClansPlugin extends JavaPlugin {

    private static ClansPlugin instance;
    private ClanManager clanManager;
    private InviteManager inviteManager;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        clanManager = new ClanManager(this);
        inviteManager = new InviteManager(this);

        // Vault/XConomy інтеграція
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp =
                    getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                economy = rsp.getProvider();
                getLogger().info("Vault economy знайдено!");
            }
        } else {
            getLogger().warning("Vault не знайдено — створення кланів без оплати!");
        }

        // Реєстрація команд
        ClansCommand cmd = new ClansCommand(this);
        getCommand("clans").setExecutor(cmd);
        getCommand("clans").setTabCompleter(cmd);
        getCommand("cc").setExecutor(cmd);
        getCommand("cc").setTabCompleter(cmd);

        // Listeners
        getServer().getPluginManager().registerEvents(new ClanListener(this), this);

        // PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.Clansystem.Expansion.ClansExpansion(this).register();
            getLogger().info("PlaceholderAPI — плейсхолдери зареєстровано!");
        }

        getLogger().info("ClansPlugin увімкнено!");
    }

    @Override
    public void onDisable() {
        if (clanManager != null) clanManager.saveAll();
        getLogger().info("ClansPlugin вимкнено!");
    }

    public static ClansPlugin getInstance() { return instance; }
    public ClanManager getClanManager() { return clanManager; }
    public InviteManager getInviteManager() { return inviteManager; }
    public Economy getEconomy() { return economy; }
}
