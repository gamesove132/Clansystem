package com.Clansystem.Command;

import com.clans.ClansPlugin;
import com.Clansystem.Model.Clan;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class ClansCommand implements CommandExecutor, TabCompleter {

    private final ClansPlugin plugin;

    public ClansCommand(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    private String c(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private String msg(String key, String... replacements) {
        String m = plugin.getConfig().getString("messages." + key, "&c[Клани] Повідомлення не знайдено: " + key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            m = m.replace(replacements[i], replacements[i + 1]);
        }
        return c(m);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cТільки для гравців!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            // /clans create <назва>
            case "create" -> {
                if (args.length < 2) { player.sendMessage(msg("usage-create")); return true; }
                String name = args[1];

                if (name.length() < 2 || name.length() > 16) {
                    player.sendMessage(msg("name-length")); return true;
                }
                if (!name.matches("[a-zA-ZА-Яа-яЄєІіЇїҐґ0-9_]+")) {
                    player.sendMessage(msg("name-invalid")); return true;
                }
                if (plugin.getClanManager().isInClan(player.getUniqueId(), player.getWorld().getName())) {
                    player.sendMessage(msg("already-in-clan")); return true;
                }
                if (plugin.getClanManager().clanExists(player.getWorld().getName(), name)) {
                    player.sendMessage(msg("clan-exists")); return true;
                }

                // Перевірка балансу
                double cost = plugin.getConfig().getDouble("create-cost", 3000);
                Economy eco = plugin.getEconomy();
                if (eco != null && cost > 0) {
                    if (!eco.has(player, cost)) {
                        player.sendMessage(msg("not-enough-money",
                                "{cost}", String.valueOf((int) cost),
                                "{currency}", eco.currencyNamePlural()));
                        return true;
                    }
                    eco.withdrawPlayer(player, cost);
                }

                plugin.getClanManager().createClan(name, player.getUniqueId(), player.getWorld().getName());
                player.sendMessage(msg("clan-created", "{clan}", name));
            }

            // /clans invite <гравець>
            case "invite" -> {
                if (args.length < 2) { player.sendMessage(msg("usage-invite")); return true; }
                Clan clan = plugin.getClanManager().getPlayerClan(player);
                if (clan == null) { player.sendMessage(msg("not-in-clan")); return true; }
                if (!clan.isOwner(player.getUniqueId())) { player.sendMessage(msg("not-owner")); return true; }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null || !target.isOnline()) { player.sendMessage(msg("player-offline")); return true; }
                if (target.equals(player)) { player.sendMessage(msg("cant-invite-yourself")); return true; }
                if (plugin.getClanManager().isInClan(target.getUniqueId(), player.getWorld().getName())) {
                    player.sendMessage(msg("already-in-clan-other", "{player}", target.getName())); return true;
                }
                if (plugin.getInviteManager().hasInvite(target.getUniqueId())) {
                    player.sendMessage(msg("already-invited")); return true;
                }

                plugin.getInviteManager().sendInvite(clan, player, target);
                player.sendMessage(msg("invite-sent", "{player}", target.getName()));
            }

            // /clans accept
            case "accept" -> {
                if (!plugin.getInviteManager().hasInvite(player.getUniqueId())) {
                    player.sendMessage(msg("no-invite")); return true;
                }
                String clanId = plugin.getInviteManager().getInviteClanId(player.getUniqueId());
                Clan clan = plugin.getClanManager().getClanById(clanId);
                if (clan == null) { player.sendMessage(msg("clan-not-found")); return true; }

                plugin.getInviteManager().removeInvite(player.getUniqueId());
                plugin.getClanManager().addMember(clan, player.getUniqueId());

                player.sendMessage(msg("joined-clan", "{clan}", clan.getName()));

                // Сповістити всіх членів
                broadcastToClan(clan, msg("member-joined", "{player}", player.getName(), "{clan}", clan.getName()));
            }

            // /clans deny
            case "deny" -> {
                if (!plugin.getInviteManager().hasInvite(player.getUniqueId())) {
                    player.sendMessage(msg("no-invite")); return true;
                }
                plugin.getInviteManager().removeInvite(player.getUniqueId());
                player.sendMessage(msg("invite-denied"));
            }

            // /clans leave
            case "leave" -> {
                Clan clan = plugin.getClanManager().getPlayerClan(player);
                if (clan == null) { player.sendMessage(msg("not-in-clan")); return true; }
                if (clan.isOwner(player.getUniqueId())) {
                    player.sendMessage(msg("owner-cant-leave")); return true;
                }
                plugin.getClanManager().removeMember(clan, player.getUniqueId());
                player.sendMessage(msg("left-clan", "{clan}", clan.getName()));
                broadcastToClan(clan, msg("member-left", "{player}", player.getName(), "{clan}", clan.getName()));
            }

            // /clans disband
            case "disband" -> {
                Clan clan = plugin.getClanManager().getPlayerClan(player);
                if (clan == null) { player.sendMessage(msg("not-in-clan")); return true; }
                if (!clan.isOwner(player.getUniqueId())) { player.sendMessage(msg("not-owner")); return true; }
                broadcastToClan(clan, msg("clan-disbanded", "{clan}", clan.getName()));
                plugin.getClanManager().disbandClan(clan);
            }

            // /clans rename <нова назва>
            case "rename" -> {
                if (args.length < 2) { player.sendMessage(msg("usage-rename")); return true; }
                Clan clan = plugin.getClanManager().getPlayerClan(player);
                if (clan == null) { player.sendMessage(msg("not-in-clan")); return true; }
                if (!clan.isOwner(player.getUniqueId())) { player.sendMessage(msg("not-owner")); return true; }

                String newName = args[1];
                if (newName.length() < 2 || newName.length() > 16) { player.sendMessage(msg("name-length")); return true; }
                if (!newName.matches("[a-zA-ZА-Яа-яЄєІіЇїҐґ0-9_]+")) { player.sendMessage(msg("name-invalid")); return true; }
                if (plugin.getClanManager().clanExists(player.getWorld().getName(), newName)) {
                    player.sendMessage(msg("clan-exists")); return true;
                }

                String oldName = clan.getName();
                plugin.getClanManager().renameClan(clan, newName);
                broadcastToClan(clan, msg("clan-renamed", "{old}", oldName, "{new}", newName));
            }

            // /clans global <повідомлення>
            case "global", "g" -> {
                Clan clan = plugin.getClanManager().getPlayerClan(player);
                if (clan == null) { player.sendMessage(msg("not-in-clan")); return true; }
                if (args.length < 2) { player.sendMessage(msg("usage-global")); return true; }

                String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                String clanColor = plugin.getConfig().getString("clan-color", "&6");
                String formatted = c(plugin.getConfig().getString("messages.global-format",
                        "&8[&6Клан {clan}&8] &f{player}&7: &f{message}")
                        .replace("{clan}", clanColor + clan.getName())
                        .replace("{player}", player.getName())
                        .replace("{message}", message));

                broadcastToClan(clan, formatted);
            }

            // /clans info [назва]
            case "info" -> {
                Clan clan;
                if (args.length >= 2) {
                    clan = plugin.getClanManager().getClan(player.getWorld().getName(), args[1]);
                    if (clan == null) { player.sendMessage(msg("clan-not-found")); return true; }
                } else {
                    clan = plugin.getClanManager().getPlayerClan(player);
                    if (clan == null) { player.sendMessage(msg("not-in-clan")); return true; }
                }
                String clanColor = plugin.getConfig().getString("clan-color", "&6");
                OfflinePlayer owner = Bukkit.getOfflinePlayer(clan.getOwner());
                player.sendMessage(c("&8&m-------------------"));
                player.sendMessage(c("&6§lКлан: " + clanColor + clan.getName()));
                player.sendMessage(c("&7Власник: &f" + (owner.getName() != null ? owner.getName() : "Невідомо")));
                player.sendMessage(c("&7Учасників: &f" + clan.getSize()));
                player.sendMessage(c("&7Світ: &f" + clan.getWorld()));
                player.sendMessage(c("&8&m-------------------"));
            }

            // /clans kick <гравець>
            case "kick" -> {
                if (args.length < 2) { player.sendMessage(msg("usage-kick")); return true; }
                Clan clan = plugin.getClanManager().getPlayerClan(player);
                if (clan == null) { player.sendMessage(msg("not-in-clan")); return true; }
                if (!clan.isOwner(player.getUniqueId())) { player.sendMessage(msg("not-owner")); return true; }

                Player target = Bukkit.getPlayer(args[1]);
                UUID targetUuid = target != null ? target.getUniqueId() : null;
                if (targetUuid == null) {
                    // Пошук офлайн
                    OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
                    if (!clan.isMember(op.getUniqueId())) { player.sendMessage(msg("not-in-your-clan")); return true; }
                    targetUuid = op.getUniqueId();
                }
                if (targetUuid.equals(player.getUniqueId())) { player.sendMessage(msg("cant-kick-yourself")); return true; }
                if (!clan.isMember(targetUuid)) { player.sendMessage(msg("not-in-your-clan")); return true; }

                plugin.getClanManager().removeMember(clan, targetUuid);
                String kickedName = target != null ? target.getName() : args[1];
                broadcastToClan(clan, msg("member-kicked", "{player}", kickedName, "{clan}", clan.getName()));
                if (target != null) target.sendMessage(msg("you-were-kicked", "{clan}", clan.getName()));
                player.sendMessage(msg("kicked-player", "{player}", kickedName));
            }

            // /clans reload — тільки для власника або адміна
            case "reload" -> {
                if (!player.hasPermission("clans.admin")) {
                    player.sendMessage(msg("no-permission")); return true;
                }
                plugin.reloadConfig();
                player.sendMessage(msg("reloaded"));
            }

            default -> sendHelp(player);
        }
        return true;
    }

    private void broadcastToClan(Clan clan, String message) {
        for (UUID memberUuid : clan.getMembers()) {
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null) member.sendMessage(message);
        }
    }

    private void sendHelp(Player player) {
        String clanColor = plugin.getConfig().getString("clan-color", "&6");
        player.sendMessage(c("&8&m--------------------"));
        player.sendMessage(c(clanColor + "§l✦ Система Кланів ✦"));
        player.sendMessage(c("&8&m--------------------"));
        player.sendMessage(c("&e/clans create <назва> &7- створити клан"));
        player.sendMessage(c("&e/clans invite <гравець> &7- запросити"));
        player.sendMessage(c("&e/clans accept &7- прийняти запрошення"));
        player.sendMessage(c("&e/clans deny &7- відхилити запрошення"));
        player.sendMessage(c("&e/clans leave &7- вийти з клану"));
        player.sendMessage(c("&e/clans disband &7- розпустити клан"));
        player.sendMessage(c("&e/clans rename <назва> &7- перейменувати"));
        player.sendMessage(c("&e/clans kick <гравець> &7- вигнати"));
        player.sendMessage(c("&e/clans global <текст> &7- повідомлення клану"));
        player.sendMessage(c("&e/clans info [назва] &7- інформація"));
        player.sendMessage(c("&8&m--------------------"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = Arrays.asList("create","invite","accept","deny","leave","disband","rename","kick","global","info","reload");
            List<String> res = new ArrayList<>();
            for (String s : subs) if (s.startsWith(args[0].toLowerCase())) res.add(s);
            return res;
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        return Collections.emptyList();
    }
}
