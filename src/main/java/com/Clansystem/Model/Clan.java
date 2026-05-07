package com.Clansystem.Model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Clan {
    private String id;
    private String name;
    private UUID owner;
    private String world;
    private Set<UUID> members;
    private long createdAt;

    // Конструктор для ClanManager (рядок 49 та 108)
    public Clan(String id, String name, UUID owner, String world, Set<UUID> members, long createdAt) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.world = world;
        this.members = members != null ? members : new HashSet<>();
        this.createdAt = createdAt;
    }

    // Додатковий конструктор для створення нового клану
    public Clan(String name, UUID owner, String world) {
        this(UUID.randomUUID().toString().substring(0, 8), name, owner, world, new HashSet<>(), System.currentTimeMillis());
    }

    // Геттери, які вимагає помилка
    public String getId() { return id; }
    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public String getWorld() { return world; }
    public long getCreatedAt() { return createdAt; }
    public Set<UUID> getMembers() { return members; }
    public int getSize() { return members.size() + 1; } // +1 для власника

    // Сеттери та логіка
    public void setName(String name) { this.name = name; }
    
    public boolean isOwner(UUID uuid) {
        return owner.equals(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid) || isOwner(uuid);
    }

    public void addMember(UUID uuid) {
        members.add(uuid);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }
}
