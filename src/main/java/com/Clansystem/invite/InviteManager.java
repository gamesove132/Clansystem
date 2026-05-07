package com.Clansystem.invite;

import com.Clansystem.ClansPlugin;
import com.Clansystem.Model.Clan;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class InviteManager {

    private final ClansPlugin plugin;
    // UUID запрошеного -> clanId
    private final Map<UUID, String> pendingInvites = new HashMap<>();

    public InviteManager(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean hasInvite(UUID uuid) {
        return pendingInvites.containsKey(uuid);
    }

    public void sendInvite(Clan clan, Player inviter, Player target) {
        pendingInvites.put(target.getUniqueId(), clan.getId());

        String clanColor = plugin.getConfig().getString("clan-color", "&6");
        String clanName = ChatColor.translateAlternateColorCodes('&', clanColor + clan.getName());

        // Текстовий заголовок
        target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.invite-received",
                        "&7[&6Клани&7] &f{inviter} &7запрошує вас у клан {clan}&7!")
                        .replace("{inviter}", inviter.getName())
                        .replace("{clan}", clanName)));

        // Кнопки ✔ і ✘ через clickable chat
        TextComponent accept = new TextComponent("  §a§l[✔ Прийняти]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clans accept"));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§aПрийняти запрошення").create()));

        TextComponent space = new TextComponent("   ");

        TextComponent deny = new TextComponent("§c§l[✘ Відхилити]");
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/clans deny"));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§cВідхилити запрошення").create()));

        target.spigot().sendMessage(accept, space, deny);

        // Автоматично скасовуємо через 60 секунд
        int expiry = plugin.getConfig().getInt("invite-expiry-seconds", 60);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingInvites.containsKey(target.getUniqueId())) {
                    pendingInvites.remove(target.getUniqueId());
                    if (target.isOnline()) {
                        target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                plugin.getConfig().getString("messages.invite-expired",
                                        "&c[Клани] Запрошення в клан &6{clan} &cзакінчилось!")
                                        .replace("{clan}", clan.getName())));
                    }
                }
            }
        }.runTaskLater(plugin, expiry * 20L);
    }

    public String getInviteClanId(UUID uuid) {
        return pendingInvites.get(uuid);
    }

    public void removeInvite(UUID uuid) {
        pendingInvites.remove(uuid);
    }
}
