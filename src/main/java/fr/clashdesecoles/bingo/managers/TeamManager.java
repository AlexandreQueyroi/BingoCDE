package fr.clashdesecoles.bingo.managers;
import com.google.gson.*;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Team;
import org.bukkit.*;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TeamManager {
    private final BingoPlugin plugin;
    private final Map<String, Team> teams;
    private final Map<String, String> teamIdByName;
    private final Map<UUID, String> playerTeams;
    private int nextTeamId;
    
    public TeamManager(BingoPlugin plugin) {
        this.plugin = plugin;
        this.teams = new HashMap<>();
        this.teamIdByName = new HashMap<>();
        this.playerTeams = new HashMap<>();
        this.nextTeamId = 1;
    }
    
    public CompletableFuture<Void> loadTeamsFromAPI() {
        return CompletableFuture.runAsync(() -> {
            JsonElement response = plugin.getApiClient().get("/teams");
            
            if (response != null && response.isJsonArray()) {
                JsonArray teamsArray = response.getAsJsonArray();
                
                for (JsonElement elem : teamsArray) {
                    if (elem.isJsonObject()) {
                        JsonObject teamObj = elem.getAsJsonObject();
                        
                        String teamId = String.valueOf(teamObj.get("id").getAsInt());
                        String name = teamObj.get("name").getAsString();
                        String colorStr = teamObj.get("color").getAsString();
                        
                        ChatColor color;
                        try {
                            color = ChatColor.valueOf(colorStr);
                        } catch (IllegalArgumentException e) {
                            color = ChatColor.WHITE;
                        }
                        
                        Team team = new Team(teamId, name, color);
                        
                        if (teamObj.has("players") && teamObj.get("players").isJsonArray()) {
                            JsonArray playersArray = teamObj.get("players").getAsJsonArray();
                            for (JsonElement playerElem : playersArray) {
                                String uuidStr = playerElem.getAsString();
                                try {
                                    UUID uuid = UUID.fromString(uuidStr);
                                    team.addPlayer(uuid);
                                    playerTeams.put(uuid, teamId);
                                } catch (IllegalArgumentException e) {
                                }
                            }
                        }
                        
                        teams.put(teamId, team);
                        teamIdByName.put(name.toLowerCase(), teamId);
                        
                        int id = Integer.parseInt(teamId);
                        if (id >= nextTeamId) {
                            nextTeamId = id + 1;
                        }
                    }
                }
                
                Bukkit.getLogger().info("Loaded " + teams.size() + " teams from API");
            }
        });
    }
    
    public CompletableFuture<Boolean> createTeam(String teamName, String teamNameDisplay, ChatColor color) {
        return CompletableFuture.supplyAsync(() -> {
            String teamIdStr = String.valueOf(nextTeamId++);
            
            JsonObject body = new JsonObject();
            body.addProperty("name", teamNameDisplay);
            body.addProperty("color", color.name());
            
            JsonElement response = plugin.getApiClient().post("/teams/" + teamIdStr, body);
            
            if (response != null) {
                Team team = new Team(teamIdStr, teamNameDisplay, color);
                teams.put(teamIdStr, team);
                teamIdByName.put(teamName.toLowerCase(), teamIdStr);
                return true;
            }
            return false;
        });
    }
    
    public CompletableFuture<Boolean> addPlayer(String teamNameOrId, UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String teamId = resolveTeamId(teamNameOrId);
            if (teamId == null || !teams.containsKey(teamId) || playerTeams.containsKey(playerUuid)) {
                return false;
            }
            
            JsonObject body = new JsonObject();
            body.addProperty("uuid", playerUuid.toString());
            
            JsonElement response = plugin.getApiClient().post("/teams/" + teamId + "/players", body);
            
            if (response != null) {
                teams.get(teamId).addPlayer(playerUuid);
                playerTeams.put(playerUuid, teamId);
                return true;
            }
            return false;
        });
    }
    
    public CompletableFuture<Boolean> removePlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            String teamId = playerTeams.get(playerUuid);
            if (teamId == null) return false;
            
            JsonObject body = new JsonObject();
            body.addProperty("uuid", playerUuid.toString());
            
            JsonElement response = plugin.getApiClient().delete("/teams/" + teamId + "/players");
            
            if (response != null) {
                teams.get(teamId).removePlayer(playerUuid);
                playerTeams.remove(playerUuid);
                return true;
            }
            return false;
        });
    }
    
    private String resolveTeamId(String nameOrId) {
        if (teams.containsKey(nameOrId)) {
            return nameOrId;
        }
        return teamIdByName.get(nameOrId.toLowerCase());
    }
    
    public Team getTeam(String nameOrId) {
        String teamId = resolveTeamId(nameOrId);
        return teamId != null ? teams.get(teamId) : null;
    }
    
    public Team getPlayerTeam(UUID playerUuid) {
        String teamId = playerTeams.get(playerUuid);
        return teamId != null ? teams.get(teamId) : null;
    }
    
    public Collection<Team> getAllTeams() {
        return teams.values();
    }
    
    public List<String> getTeamNames() {
        return teams.values().stream()
            .map(Team::getName)
            .collect(java.util.stream.Collectors.toList());
    }
    
    public CompletableFuture<Void> sendTeamMessage(String teamNameOrId, String message) {
        return CompletableFuture.runAsync(() -> {
            String teamId = resolveTeamId(teamNameOrId);
            if (teamId == null) return;
            
            JsonObject body = new JsonObject();
            body.addProperty("message", message);
            body.addProperty("sender", "SERVER");
            
            plugin.getApiClient().post("/teams/" + teamId + "/chat", body);
            
            Team team = teams.get(teamId);
            if (team != null) {
                String formatted = team.getColor() + "[" + team.getName() + "] " + ChatColor.RESET + message;
                
                for (UUID playerUuid : team.getPlayers()) {
                    Player player = Bukkit.getPlayer(playerUuid);
                    if (player != null && player.isOnline()) {
                        player.sendMessage(formatted);
                    }
                }
            }
        });
    }
    
    public CompletableFuture<Void> updateTeamPoints(String teamId, String gameId) {
        return CompletableFuture.runAsync(() -> {
            JsonElement response = plugin.getApiClient().get("/games/" + gameId + "/teams/" + teamId + "/points");
            
            if (response != null && response.isJsonObject()) {
                JsonObject pointsObj = response.getAsJsonObject();
                int points = pointsObj.has("points") ? pointsObj.get("points").getAsInt() : 0;
                
                Team team = teams.get(teamId);
                if (team != null) {
                    team.setPoints(points);
                }
            }
        });
    }
}
