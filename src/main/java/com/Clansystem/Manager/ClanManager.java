package com.Clansystem.Manager;

import com.Clansystem.ClansPlugin;
import com.Clansystem.Model.Clan;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClanManager {

    private final ClansPlugin plugin;
    private final Map<String, Clan> clans = new HashMap<>();
    // ключ: uuid_world -> clanId
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

        ConfigurationSection clansSection = data.getConfigurationSection("clans");
        if (clansSection == null) return;

        // getKeys(false) на ConfigurationSection — не плутається з крапками в ключах
        for (String id : clansSection.getKeys(false)) {
            ConfigurationSection c = clansSection.getConfigurationSection(id);
            if (c == null) continue;

            String name = c.getString("name");
            String ownerStr = c.getString("owner");
            String world = c.getString("world");
            long createdAt = c.getLong("created-at", 0);

            if (name == null || ownerStr == null || world == null) {
                plugin.getLogger().warning("Пропускаємо клан з неповними даними: " + id);
                continue;
            }

            UUID owner;
            try { owner = UUID.fromString(ownerStr); }
            catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Невірний UUID власника для клану: " + id);
                continue;
            }

            Set<UUID> members = new HashSet<>();
            for (String m : c.getStringList("members")) {
                try { members.add(UUID.fromString(m)); }
                catch (IllegalArgumentException ignored) {}
            }

            Clan clan = new Clan(id, name, owner, world, members, createdAt);
            clans.put(id, clan);
            for (UUID member : members) {
                playerClanMap.put(playerKey(member, world), id);
            }
        }
        plugin.getLogger().info("Завантажено " + clans.size() + " кланів.");
    }

    public void saveAll() {
        // Очищаємо секцію
        data.set("clans", null);

        for (Clan clan : clans.values()) {
            // Використовуємо ConfigurationSection щоб уникнути проблем з крапками в ключах
            ConfigurationSection c = data.createSection("clans." + clan.getId());
            c.set("name", clan.getName());
            c.set("owner", clan.getOwner().toString());
            c.set("world", clan.getWorld());
            c.set("created-at", clan.getCreatedAt());
            List<String> members = new ArrayList<>();
            for (UUID m : clan.getMembers()) members.add(m.toString());
            c.set("members", members);
        }

        try { data.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

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
        // Спочатку точний збіг по світу
        String id = playerClanMap.get(playerKey(uuid, world));
        if (id != null) return clans.get(id);

        // Фолбек — будь-який клан цього UUID (для TAB/scoreboard)
        String prefix = uuid.toString() + "_";
        for (Map.Entry<String, String> entry : playerClanMap.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                return clans.get(entry.getValue());
            }
        }
        return null;
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
        for (UUID member : new HashSet<>(clan.getMembers())) {
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
        String oldId = clan.getId();
        clans.remove(oldId);

        String newId = clan.getWorld() + "_" + newName.toLowerCase();
        for (Map.Entry<String, String> entry : playerClanMap.entrySet()) {
            if (entry.getValue().equals(oldId)) {
                entry.setValue(newId);
            }
        }

        clan.setName(newName); // оновлює і id і name
        clans.put(clan.getId(), clan);
        saveAll();
    }

    public Collection<Clan> getAllClans() {
        return clans.values();
    }
}
