package fr.clashdesecoles.bingo.models;

import java.util.HashMap;
import java.util.Map;

public class Game {
    
    private final String id;
    private String name;
    private final String team1Id;
    private final String team2Id;
    private final Map<String, String> worlds;
    private long seed;
    private GameState state;
    private long startTime;
    private long pausedTime;
    private long elapsedTime;
    
    public enum GameState {
        CREATED,
        STARTED,
        PAUSED,
        FINISHED
    }
    
    public Game(String id, String team1Id, String team2Id) {
        this.id = id;
        this.team1Id = team1Id;
        this.team2Id = team2Id;
        this.worlds = new HashMap<>();
        this.state = GameState.CREATED;
        this.name = "Game " + id;
        this.elapsedTime = 0;
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
    
    public String getTeam1Id() {
        return team1Id;
    }
    
    public String getTeam2Id() {
        return team2Id;
    }
    
    public Map<String, String> getWorlds() {
        return worlds;
    }
    
    public void setWorld(String teamId, String worldName) {
        worlds.put(teamId, worldName);
    }
    
    public String getWorld(String teamId) {
        return worlds.get(teamId);
    }
    
    public long getSeed() {
        return seed;
    }
    
    public void setSeed(long seed) {
        this.seed = seed;
    }
    
    public GameState getState() {
        return state;
    }
    
    public void setState(GameState state) {
        this.state = state;
    }
    
    public void start() {
        if (state == GameState.CREATED || state == GameState.PAUSED) {
            state = GameState.STARTED;
            startTime = System.currentTimeMillis();
        }
    }
    
    public void pause() {
        if (state == GameState.STARTED) {
            state = GameState.PAUSED;
            pausedTime = System.currentTimeMillis();
            elapsedTime += (pausedTime - startTime);
        }
    }
    
    public void stop() {
        if (state == GameState.STARTED || state == GameState.PAUSED) {
            if (state == GameState.STARTED) {
                elapsedTime += (System.currentTimeMillis() - startTime);
            }
            state = GameState.FINISHED;
        }
    }
    
    public long getElapsedTime() {
        if (state == GameState.STARTED) {
            return elapsedTime + (System.currentTimeMillis() - startTime);
        }
        return elapsedTime;
    }
}
