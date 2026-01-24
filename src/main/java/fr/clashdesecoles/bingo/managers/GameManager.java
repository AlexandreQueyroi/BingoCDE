package fr.clashdesecoles.bingo.managers;
import com.google.gson.*;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Game;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class GameManager {
    private final BingoPlugin plugin;
    private final Map<String, Game> games;
    
    public GameManager(BingoPlugin plugin) {
        this.plugin = plugin;
        this.games = new HashMap<>();
    }
    
    public CompletableFuture<Game> createGame(String gameId, String team1Id, String team2Id) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("team1", team1Id);
            body.addProperty("team2", team2Id);
            
            JsonElement response = plugin.getApiClient().post("/games/" + gameId, body);
            
            if (response != null) {
                Game game = new Game(gameId, team1Id, team2Id);
                games.put(gameId, game);
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    createGameWorlds(game);
                    if (plugin.getConfig().getBoolean("auto-teleport", true)) {
                        teleportTeams(game);
                    }
                });
                
                return game;
            }
            return null;
        });
    }
    
    private void createGameWorlds(Game game) {
        long seed = getRandomSeed();
        game.setSeed(seed);
        
        String world1Name = "game_" + game.getId() + "_" + game.getTeam1Id();
        World world1 = new WorldCreator(world1Name)
            .environment(World.Environment.NORMAL)
            .seed(seed)
            .createWorld();
        if (world1 != null) {
            game.setWorld(game.getTeam1Id(), world1Name);
        }
        
        String world2Name = "game_" + game.getId() + "_" + game.getTeam2Id();
        World world2 = new WorldCreator(world2Name)
            .environment(World.Environment.NORMAL)
            .seed(seed)
            .createWorld();
        if (world2 != null) {
            game.setWorld(game.getTeam2Id(), world2Name);
        }
    }
    
    private void teleportTeams(Game game) {
        var team1 = plugin.getTeamManager().getTeam(game.getTeam1Id());
        var team2 = plugin.getTeamManager().getTeam(game.getTeam2Id());
        
        if (team1 != null) {
            World world = Bukkit.getWorld(game.getWorld(team1.getId()));
            if (world != null) {
                for (UUID uuid : team1.getPlayers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.teleport(world.getSpawnLocation());
                }
            }
        }
        
        if (team2 != null) {
            World world = Bukkit.getWorld(game.getWorld(team2.getId()));
            if (world != null) {
                for (UUID uuid : team2.getPlayers()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) p.teleport(world.getSpawnLocation());
                }
            }
        }
    }
    
    private long getRandomSeed() {
        try {
            JsonElement response = plugin.getApiClient().get("/seeds/randomizer");
            if (response != null && response.isJsonObject()) {
                JsonObject seedObj = response.getAsJsonObject();
                if (seedObj.has("seed")) {
                    return seedObj.get("seed").getAsLong();
                }
            }
        } catch (Exception e) {
        }
        return System.currentTimeMillis();
    }
    
    public CompletableFuture<Boolean> setMatchName(String gameId, String matchName) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("name", matchName);
            
            JsonElement response = plugin.getApiClient().patch("/games/" + gameId, body);
            
            if (response != null) {
                Game game = games.get(gameId);
                if (game != null) game.setName(matchName);
                return true;
            }
            return false;
        });
    }
    
    public boolean startGame(String gameId) {
        Game game = games.get(gameId);
        if (game != null && game.getState() != Game.GameState.FINISHED) {
            game.start();
            return true;
        }
        return false;
    }
    
    public boolean pauseGame(String gameId) {
        Game game = games.get(gameId);
        if (game != null && game.getState() == Game.GameState.STARTED) {
            game.pause();
            return true;
        }
        return false;
    }
    
    public boolean stopGame(String gameId) {
        Game game = games.get(gameId);
        if (game != null) {
            game.stop();
            return true;
        }
        return false;
    }
    
    public Game getGame(String gameId) {
        return games.get(gameId);
    }
    
    public Game getGameForTeam(String teamId) {
        for (Game game : games.values()) {
            if (game.getTeam1Id().equals(teamId) || game.getTeam2Id().equals(teamId)) {
                return game;
            }
        }
        return null;
    }
    
    public Collection<Game> getAllGames() {
        return games.values();
    }
    
    public CompletableFuture<JsonElement> exportGameResults(String gameId) {
        return CompletableFuture.supplyAsync(() -> 
            plugin.getApiClient().get("/games/" + gameId + "/result")
        );
    }
}
