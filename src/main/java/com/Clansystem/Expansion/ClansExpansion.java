package com.Clansystem.Expansion;

import com.Clansystem.ClansPlugin;
import com.Clansystem.Model.Clan;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * %clansystem%        - назва клану гравця (або порожньо)
 * %clansystem_name%   - назва клану
 * %clansystem_size%   - кількість членів
 * %clansystem_owner%  - власник клану
 * %clansystem_role%   - роль: Власник / Учасник
 */
public class ClansExpansion extends PlaceholderExpansion {

    private final ClansPlugin plugin;

    public ClansExpansion(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "clansystem"; }
    @Override public @NotNull String getAuthor() { return "YourName"; }
    @Override public @NotNull String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        Clan clan = plugin.getClanManager().getPlayerClan(player);
        String clanColor = plugin.getConfig().getString("clan-color", "&6");

        if (params.equals("") || params.equals("name")) {
            if (clan == null) return plugin.getConfig().getString("messages.no-clan-placeholder", "Без клану");
            return ChatColor.translateAlternateColorCodes('&', clanColor + clan.getName());
        }

        if (clan == null) return "";

        return switch (params) {
            case "size" -> String.valueOf(clan.getSize());
            case "owner" -> {
                org.bukkit.OfflinePlayer op = org.bukkit.Bukkit.getOfflinePlayer(clan.getOwner());
                yield op.getName() != null ? op.getName() : "Невідомо";
            }
            case "role" -> clan.isOwner(player.getUniqueId())
                    ? ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.role-owner", "&6Власник"))
                    : ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.role-member", "&7Учасник"));
            case "tag" -> ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("scoreboard.clan-format", "&8[{color}{clan}&8]")
                            .replace("{clan}", clan.getName())
                            .replace("{color}", clanColor));
            default -> "";
        };
    }
}
