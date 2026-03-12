package fr.clashdesecoles.bingo.models;

import org.bukkit.ChatColor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Team {
    
    private final String id;
    private String name;
    private ChatColor color;
    private final Set<UUID> players;
    private volatile int points;
    private boolean admin;
    
    public Team(String id, String name, ChatColor color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.players = ConcurrentHashMap.newKeySet();
        this.points = 0;
        this.admin = false;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public ChatColor getColor() {
        return color;
    }
    
    public void setColor(ChatColor color) {
        this.color = color;
    }
    
    public String getColoredName() {
        return color + name;
    }
    
    public Set<UUID> getPlayers() {
        return players;
    }
    
    public void addPlayer(UUID playerUuid) {
        players.add(playerUuid);
    }
    
    public void removePlayer(UUID playerUuid) {
        players.remove(playerUuid);
    }
    
    public boolean hasPlayer(UUID playerUuid) {
        return players.contains(playerUuid);
    }
    
    public int getPoints() {
        return points;
    }
    
    public void setPoints(int points) {
        this.points = points;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }
}
