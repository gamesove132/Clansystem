package com.Clansystem.Model;

import java.util.*;

public class Clan {

    // id більше не final — бо може змінитись при rename
    private String id;
    private String name;
    private final UUID owner;
    private final String world;
    private final Set<UUID> members = new HashSet<>();
    private long createdAt;

    public Clan(String name, UUID owner, String world) {
        this.name = name;
        this.owner = owner;
        this.world = world;
        this.id = world + "_" + name.toLowerCase();
        this.members.add(owner);
        this.createdAt = System.currentTimeMillis();
    }

    // For loading from config
    public Clan(String id, String name, UUID owner, String world, Set<UUID> members, long createdAt) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.world = world;
        this.members.addAll(members);
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    // Оновлює і id і name при rename
    public void setName(String name) {
        this.name = name;
        this.id = world + "_" + name.toLowerCase();
    }
    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public String getWorld() { return world; }
    public Set<UUID> getMembers() { return members; }
    public long getCreatedAt() { return createdAt; }

    public boolean isMember(UUID uuid) { return members.contains(uuid); }
    public boolean isOwner(UUID uuid) { return owner.equals(uuid); }

    public void addMember(UUID uuid) { members.add(uuid); }
    public void removeMember(UUID uuid) { members.remove(uuid); }
    public int getSize() { return members.size(); }
}
