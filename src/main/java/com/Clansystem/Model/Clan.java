package com.Clansystem.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Clan {
    private final String name;
    private final UUID owner;
    private final List<UUID> members;
    private String tag;

    public Clan(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        this.members = new ArrayList<>();
        this.members.add(owner);
        this.tag = name; // за замовчуванням тег такий же як назва
    }

    // Геттери та сеттери
    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public List<UUID> getMembers() { return members; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public void addMember(UUID uuid) {
        if (!members.contains(uuid)) {
            members.add(uuid);
        }
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }
}
