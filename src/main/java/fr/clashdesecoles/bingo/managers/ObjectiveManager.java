package fr.clashdesecoles.bingo.managers;
import com.google.gson.*;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Objective;
import fr.clashdesecoles.bingo.utils.IconUtil;
import org.bukkit.Material;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ObjectiveManager {
    private final BingoPlugin plugin;
    private final Map<String, List<Objective>> gameObjectives;
    private final Map<String, Map<Integer, Objective.ObjectiveStatus>> teamObjectiveStatus;
    
    public ObjectiveManager(BingoPlugin plugin) {
        this.plugin = plugin;
        this.gameObjectives = new HashMap<>();
        this.teamObjectiveStatus = new HashMap<>();
    }
    
    public CompletableFuture<List<Objective>> rollObjectives(String gameId) {
        return CompletableFuture.supplyAsync(() -> {
            JsonElement response = plugin.getApiClient().get("/games/" + gameId + "/objectives/randomizer");
            if (response != null && response.isJsonArray()) {
                List<Objective> objectives = new ArrayList<>();
                for (JsonElement element : response.getAsJsonArray()) {
                    JsonObject obj = element.getAsJsonObject();
                    int id = obj.get("id").getAsInt();
                    String internalName = obj.get("objectiveName").getAsString();
                    String displayName = obj.get("name").getAsString();
                    String itemStr = obj.get("item").getAsString();
                    String actionStr = obj.get("action").getAsString();
                    String actionCheck = obj.get("actionCheck").getAsString();
                    
                    Material item = Material.matchMaterial(itemStr.toUpperCase());
                    if (item == null) item = Material.STICK;
                    
                    Objective.ObjectiveAction action = Objective.ObjectiveAction.valueOf(actionStr.toUpperCase());
                    objectives.add(new Objective(id, internalName, displayName, item, action, actionCheck));
                }
                if (objectives.size() > 25) objectives = objectives.subList(0, 25);
                gameObjectives.put(gameId, objectives);
                return objectives;
            }
            return Collections.emptyList();
        });
    }
    
    public CompletableFuture<Boolean> validateObjective(String gameId, String teamId, int objectiveId) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("objectiveId", objectiveId);
            body.addProperty("status", "success");
            JsonElement response = plugin.getApiClient().post("/stats/games/" + gameId + "/teams/" + teamId, body);
            if (response != null) {
                teamObjectiveStatus.computeIfAbsent(teamId, k -> new HashMap<>())
                    .put(objectiveId, Objective.ObjectiveStatus.SUCCESS);
                plugin.getTeamManager().updateTeamPoints(teamId, gameId);
                return true;
            }
            return false;
        });
    }
    
    public CompletableFuture<Boolean> rejectObjective(String gameId, String teamId, int objectiveId) {
        return CompletableFuture.supplyAsync(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("objectiveId", objectiveId);
            body.addProperty("status", "reject");
            JsonElement response = plugin.getApiClient().post("/stats/games/" + gameId + "/teams/" + teamId, body);
            if (response != null) {
                teamObjectiveStatus.computeIfAbsent(teamId, k -> new HashMap<>())
                    .put(objectiveId, Objective.ObjectiveStatus.REJECTED);
                return true;
            }
            return false;
        });
    }
    
    public List<Objective> getGameObjectives(String gameId) {
        return gameObjectives.getOrDefault(gameId, Collections.emptyList());
    }
    
    public Objective getObjective(String gameId, int objectiveId) {
        return getGameObjectives(gameId).stream()
            .filter(obj -> obj.getId() == objectiveId)
            .findFirst().orElse(null);
    }
    
    public Objective.ObjectiveStatus getObjectiveStatus(String teamId, int objectiveId) {
        Map<Integer, Objective.ObjectiveStatus> teamStatus = teamObjectiveStatus.get(teamId);
        return teamStatus != null ? teamStatus.getOrDefault(objectiveId, Objective.ObjectiveStatus.PENDING) 
            : Objective.ObjectiveStatus.PENDING;
    }
    
    public String generateBingoGrid(String gameId, String teamId) {
        List<Objective> objectives = gameObjectives.get(gameId);
        if (objectives == null || objectives.isEmpty()) return "Aucun objectif";
        
        StringBuilder grid = new StringBuilder();
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int index = row * 5 + col;
                if (index < objectives.size()) {
                    Objective obj = objectives.get(index);
                    Objective.ObjectiveStatus status = getObjectiveStatus(teamId, obj.getId());
                    String icon = IconUtil.getIcon(obj.getItem());
                    String color = status == Objective.ObjectiveStatus.SUCCESS ? "§a" : 
                                  status == Objective.ObjectiveStatus.REJECTED ? "§c" : "§7";
                    grid.append(color).append(icon);
                } else {
                    grid.append("§7");
                }
                if (col < 4) grid.append("§8│");
            }
            if (row < 4) grid.append("\n");
        }
        return grid.toString();
    }
}
