package fr.clashdesecoles.bingo.models;

import org.bukkit.ChatColor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Team {
    
    private final String id;
    private String name;
    private ChatColor color;
    private final Set<UUID> players;
    private int points;
    
    public Team(String id, String name, ChatColor color) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.players = new HashSet<>();
        this.points = 0;
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
}
