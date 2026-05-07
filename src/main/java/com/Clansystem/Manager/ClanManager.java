package com.clans.managers;

import com.clans.ClansPlugin;
import com.Clansystem.Model.Clan;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClanManager {

    private final ClansPlugin plugin;
    // clanId -> Clan
    private final Map<String, Clan> clans = new HashMap<>();
    // UUID -> clanId (per world: uuid_world -> clanId)
    private final Map<String, String> playerClanMap = new HashMap<>();

    private File dataFile;
    private FileConfiguration data;

    public ClanManager(ClansPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "clans.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        if (!data.contains("clans")) return;

        for (String id : data.getConfigurationSection("clans").getKeys(false)) {
            String path = "clans." + id;
            String name = data.getString(path + ".name");
            UUID owner = UUID.fromString(data.getString(path + ".owner"));
            String world = data.getString(path + ".world");
            long createdAt = data.getLong(path + ".created-at", 0);

            Set<UUID> members = new HashSet<>();
            List<String> memberList = data.getStringList(path + ".members");
            for (String m : memberList) members.add(UUID.fromString(m));

            Clan clan = new Clan(id, name, owner, world, members, createdAt);
            clans.put(id, clan);

            for (UUID member : members) {
                playerClanMap.put(member.toString() + "_" + world, id);
            }
        }
    }

    public void saveAll() {
        data.set("clans", null);
        for (Clan clan : clans.values()) {
            String path = "clans." + clan.getId();
            data.set(path + ".name", clan.getName());
            data.set(path + ".owner", clan.getOwner().toString());
            data.set(path + ".world", clan.getWorld());
            data.set(path + ".created-at", clan.getCreatedAt());
            List<String> members = new ArrayList<>();
            for (UUID m : clan.getMembers()) members.add(m.toString());
            data.set(path + ".members", members);
        }
        try { data.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    // Ключ для мапи гравець+світ
    private String playerKey(UUID uuid, String world) {
        return uuid.toString() + "_" + world;
    }

    private String clanId(String world, String name) {
        return world + "_" + name.toLowerCase();
    }

    public boolean clanExists(String world, String name) {
        return clans.containsKey(clanId(world, name));
    }

    public Clan getClan(String world, String name) {
        return clans.get(clanId(world, name));
    }

    public Clan getClanById(String id) {
        return clans.get(id);
    }

    public Clan getPlayerClan(UUID uuid, String world) {
        String id = playerClanMap.get(playerKey(uuid, world));
        return id != null ? clans.get(id) : null;
    }

    public Clan getPlayerClan(Player player) {
        return getPlayerClan(player.getUniqueId(), player.getWorld().getName());
    }

    public boolean isInClan(UUID uuid, String world) {
        return playerClanMap.containsKey(playerKey(uuid, world));
    }

    public Clan createClan(String name, UUID owner, String world) {
        Clan clan = new Clan(name, owner, world);
        clans.put(clan.getId(), clan);
        playerClanMap.put(playerKey(owner, world), clan.getId());
        saveAll();
        return clan;
    }

    public void disbandClan(Clan clan) {
        for (UUID member : clan.getMembers()) {
            playerClanMap.remove(playerKey(member, clan.getWorld()));
        }
        clans.remove(clan.getId());
        saveAll();
    }

    public void addMember(Clan clan, UUID uuid) {
        clan.addMember(uuid);
        playerClanMap.put(playerKey(uuid, clan.getWorld()), clan.getId());
        saveAll();
    }

    public void removeMember(Clan clan, UUID uuid) {
        clan.removeMember(uuid);
        playerClanMap.remove(playerKey(uuid, clan.getWorld()));
        saveAll();
    }

    public void renameClan(Clan clan, String newName) {
        // Видаляємо старий id
        clans.remove(clan.getId());
        // Оновлюємо мапу гравців
        for (UUID member : clan.getMembers()) {
            playerClanMap.put(playerKey(member, clan.getWorld()), clan.getWorld() + "_" + newName.toLowerCase());
        }
        clan.setName(newName);
        clans.put(clan.getId(), clan);
        saveAll();
    }

    public Collection<Clan> getAllClans() {
        return clans.values();
    }
}
