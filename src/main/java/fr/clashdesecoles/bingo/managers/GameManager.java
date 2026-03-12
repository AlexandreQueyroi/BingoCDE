package fr.clashdesecoles.bingo.managers;
import com.google.gson.*;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Game;
import fr.clashdesecoles.bingo.models.Team;
import fr.clashdesecoles.bingo.models.Objective;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class GameManager {
    private final BingoPlugin plugin;
    private final java.util.concurrent.ConcurrentMap<String, Game> games;
    private final java.io.File gamesFile;
    
    public GameManager(BingoPlugin plugin) {
        this.plugin = plugin;
        this.games = new java.util.concurrent.ConcurrentHashMap<>();
        this.gamesFile = new java.io.File(plugin.getDataFolder(), "games.json");
    }

    public void loadFromDisk() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            if (!gamesFile.exists()) return;
            String json = new String(java.nio.file.Files.readAllBytes(gamesFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            games.clear();
            if (root.has("games") && root.get("games").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("games")) {
                    JsonObject go = el.getAsJsonObject();
                    String id = go.get("id").getAsString();
                    String t1 = go.get("team1Id").getAsString();
                    String t2 = go.get("team2Id").getAsString();
                    Game g = new Game(id, t1, t2);
                    if (go.has("name")) g.setName(go.get("name").getAsString());
                    if (go.has("seed")) g.setSeed(go.get("seed").getAsLong());
                    if (go.has("state")) {
                        try { g.setState(Game.GameState.valueOf(go.get("state").getAsString())); } catch (Exception ignored) {}
                    }
                    if (go.has("worlds")) {
                        JsonObject ws = go.get("worlds").getAsJsonObject();
                        for (String key : ws.keySet()) {
                            g.setWorld(key, ws.get(key).getAsString());
                        }
                    }
                    games.put(id, g);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load games.json: " + e.getMessage());
        }
    }

    private synchronized void saveToDiskQuiet() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            JsonObject root = new JsonObject();
            JsonArray arr = new JsonArray();
            for (Game g : games.values()) {
                JsonObject go = new JsonObject();
                go.addProperty("id", g.getId());
                go.addProperty("name", g.getName());
                go.addProperty("team1Id", g.getTeam1Id());
                go.addProperty("team2Id", g.getTeam2Id());
                go.addProperty("seed", g.getSeed());
                go.addProperty("state", g.getState().name());
                JsonObject ws = new JsonObject();
                for (Map.Entry<String, String> e : g.getWorlds().entrySet()) {
                    ws.addProperty(e.getKey(), e.getValue());
                }
                go.add("worlds", ws);
                arr.add(go);
            }
            root.add("games", arr);
            try (java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(gamesFile), java.nio.charset.StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, w);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save games.json: " + e.getMessage());
        }
    }

    public void saveNow() { saveToDiskQuiet(); }
    
    public CompletableFuture<Game> createGame(String gameId, String team1Id, String team2Id) {
        return CompletableFuture.supplyAsync(() -> {
            Game game = new Game(gameId, team1Id, team2Id);
            games.put(gameId, game);
            Bukkit.getScheduler().runTask(plugin, () -> {
                createGameWorlds(game);
                // Auto-teleport removed: teleporting now happens on /bingo start only
                saveToDiskQuiet();
            });
            return game;
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
        return new java.util.Random().nextLong();
    }
    
    public CompletableFuture<Boolean> setMatchName(String gameId, String matchName) {
        return CompletableFuture.supplyAsync(() -> {
            Game game = games.get(gameId);
            if (game != null) {
                game.setName(matchName);
                saveToDiskQuiet();
                return true;
            }
            return false;
        });
    }
    
    public boolean startGame(String gameId) {
        Game game = games.get(gameId);
        if (game != null && game.getState() != Game.GameState.FINISHED) {
            game.start();
            saveToDiskQuiet();
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
        return CompletableFuture.supplyAsync(() -> {
            Game game = games.get(gameId);
            if (game == null) return null;
            JsonObject root = new JsonObject();
            root.addProperty("gameId", game.getId());
            root.addProperty("name", game.getName() == null ? "" : game.getName());
            root.addProperty("state", game.getState().name());
            root.addProperty("seed", game.getSeed());
            JsonArray teamsArr = new JsonArray();
            for (String tid : new String[]{game.getTeam1Id(), game.getTeam2Id()}) {
                Team t = plugin.getTeamManager().getTeam(tid);
                JsonObject to = new JsonObject();
                to.addProperty("id", tid);
                to.addProperty("name", t != null ? t.getName() : tid);
                to.addProperty("points", t != null ? t.getPoints() : 0);
                teamsArr.add(to);
            }
            root.add("teams", teamsArr);
            // Add objectives status summary per team
            JsonObject summary = new JsonObject();
            for (String tid : new String[]{game.getTeam1Id(), game.getTeam2Id()}) {
                int success = 0;
                for (Objective obj : plugin.getObjectiveManager().getGameObjectives(game.getId())) {
                    if (plugin.getObjectiveManager().getObjectiveStatus(tid, obj.getId()) == Objective.ObjectiveStatus.SUCCESS) {
                        success++;
                    }
                }
                summary.addProperty(tid, success);
            }
            root.add("successByTeam", summary);
            return root;
        });
    }
}
