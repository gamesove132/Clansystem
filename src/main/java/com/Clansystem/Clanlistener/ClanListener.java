package com.clans.listeners;

import com.clans.ClansPlugin;
import com.clans.model.Clan;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class ClanListener implements Listener {

    private final ClansPlugin plugin;

    public ClanListener(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        Clan clan = plugin.getClanManager().getPlayerClan(player);
        if (clan == null) return;

        // Отримуємо повідомлення про смерть з vanilla
        String deathMsg = e.getDeathMessage();
        if (deathMsg == null) deathMsg = player.getName() + " загинув";

        String clanColor = plugin.getConfig().getString("clan-color", "&6");
        String prefix = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.death-prefix", "&8[&6Клан&8] "));

        String finalMsg = prefix + ChatColor.GRAY + deathMsg;

        // Повідомити всіх членів клану
        for (java.util.UUID memberUuid : clan.getMembers()) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null && !member.equals(player)) {
                member.sendMessage(finalMsg);
            }
        }
    }
}
