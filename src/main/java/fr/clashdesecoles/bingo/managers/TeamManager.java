package fr.clashdesecoles.bingo.managers;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Objective;
import fr.clashdesecoles.bingo.models.Team;
import org.bukkit.*;
import org.bukkit.entity.Player;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TeamManager {
    private final BingoPlugin plugin;
    private final java.util.concurrent.ConcurrentMap<String, Team> teams;
    private final java.util.concurrent.ConcurrentMap<String, String> teamIdByName;
    private final java.util.concurrent.ConcurrentMap<UUID, String> playerTeams;
    private final java.util.concurrent.atomic.AtomicInteger nextTeamId;
    private final java.io.File dataFile;
    private volatile boolean loaded = false;
    
    public TeamManager(BingoPlugin plugin) {
        this.plugin = plugin;
        this.teams = new java.util.concurrent.ConcurrentHashMap<>();
        this.teamIdByName = new java.util.concurrent.ConcurrentHashMap<>();
        this.playerTeams = new java.util.concurrent.ConcurrentHashMap<>();
        this.nextTeamId = new java.util.concurrent.atomic.AtomicInteger(1);
        this.dataFile = new java.io.File(plugin.getDataFolder(), "teams.json");
    }
    
    public CompletableFuture<Void> loadTeamsFromAPI() {
        // API removed: no-op for backward compatibility
        return CompletableFuture.completedFuture(null);
    }

    private synchronized void ensureLoaded() {
        if (loaded) return;
        loadFromDisk();
        ensureAdminTeam();
        loaded = true;
    }

    public synchronized void loadFromDisk() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            if (!dataFile.exists()) {
                // Seed with an admin team
                Team admin = new Team("1", "admin", ChatColor.GOLD);
                admin.setAdmin(true);
                teams.put(admin.getId(), admin);
                teamIdByName.put("admin", admin.getId());
                nextTeamId.set(2);
                saveToDiskQuiet();
                return;
            }
            String json = new String(java.nio.file.Files.readAllBytes(dataFile.toPath()), StandardCharsets.UTF_8);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            int next = root.has("nextTeamId") ? root.get("nextTeamId").getAsInt() : 1;
            nextTeamId.set(next);
            teams.clear();
            teamIdByName.clear();
            playerTeams.clear();
            if (root.has("teams") && root.get("teams").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("teams")) {
                    JsonObject to = el.getAsJsonObject();
                    String id = to.get("id").getAsString();
                    String name = to.get("name").getAsString();
                    ChatColor color = ChatColor.valueOf(to.get("color").getAsString());
                    boolean admin = to.has("admin") && to.get("admin").getAsBoolean();
                    Team t = new Team(id, name, color);
                    t.setAdmin(admin);
                    if (to.has("players")) {
                        for (JsonElement pe : to.getAsJsonArray("players")) {
                            try {
                                UUID u = UUID.fromString(pe.getAsString());
                                t.addPlayer(u);
                                if (!admin) playerTeams.put(u, id);
                            } catch (Exception ignored) {}
                        }
                    }
                    teams.put(id, t);
                    teamIdByName.put(name.toLowerCase(), id);
                }
            }
            ensureAdminTeam();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load teams.json: " + e.getMessage());
        }
    }

    private synchronized void saveToDiskQuiet() {
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            JsonObject root = new JsonObject();
            root.addProperty("nextTeamId", nextTeamId.get());
            JsonArray arr = new JsonArray();
            for (Team t : teams.values()) {
                JsonObject to = new JsonObject();
                to.addProperty("id", t.getId());
                to.addProperty("name", t.getName());
                to.addProperty("color", t.getColor().name());
                to.addProperty("admin", t.isAdmin());
                JsonArray players = new JsonArray();
                for (UUID u : t.getPlayers()) players.add(u.toString());
                to.add("players", players);
                arr.add(to);
            }
            root.add("teams", arr);
            try (Writer w = new OutputStreamWriter(new FileOutputStream(dataFile), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, w);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save teams.json: " + e.getMessage());
        }
    }
    
    public CompletableFuture<Boolean> createTeam(String teamName, String teamNameDisplay, ChatColor color) {
        return CompletableFuture.supplyAsync(() -> {
            ensureLoaded();
            String teamIdStr = String.valueOf(nextTeamId.getAndIncrement());
            Team team = new Team(teamIdStr, teamNameDisplay, color);
            teams.put(teamIdStr, team);
            teamIdByName.put(teamName.toLowerCase(), teamIdStr);
            saveToDiskQuiet();
            return true;
        });
    }
    
    public CompletableFuture<Boolean> addPlayer(String teamNameOrId, UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureLoaded();
            String teamId = resolveTeamId(teamNameOrId);
            if (teamId == null || !teams.containsKey(teamId)) {
                return false;
            }
            // Prevent adding to admin team via generic add
            if (teams.get(teamId).isAdmin()) return false;
            // Remove from previous team if any
            String prev = playerTeams.get(playerUuid);
            if (prev != null) {
                Team t = teams.get(prev);
                if (t != null) t.removePlayer(playerUuid);
                playerTeams.remove(playerUuid);
            }
            teams.get(teamId).addPlayer(playerUuid);
            playerTeams.put(playerUuid, teamId);
            saveToDiskQuiet();
            return true;
        });
    }
    
    public CompletableFuture<Boolean> removePlayer(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureLoaded();
            String teamId = playerTeams.get(playerUuid);
            if (teamId == null) return false;
            teams.get(teamId).removePlayer(playerUuid);
            playerTeams.remove(playerUuid);
            saveToDiskQuiet();
            return true;
        });
    }
    
    private String resolveTeamId(String nameOrId) {
        if (teams.containsKey(nameOrId)) {
            return nameOrId;
        }
        return teamIdByName.get(nameOrId.toLowerCase());
    }
    
    public Team getTeam(String nameOrId) {
        ensureLoaded();
        String teamId = resolveTeamId(nameOrId);
        return teamId != null ? teams.get(teamId) : null;
    }
    
    public Team getPlayerTeam(UUID playerUuid) {
        ensureLoaded();
        String teamId = playerTeams.get(playerUuid);
        return teamId != null ? teams.get(teamId) : null;
    }

    public boolean isAdmin(UUID uuid) {
        Team admin = getAdminTeam();
        return admin != null && admin.getPlayers().contains(uuid);
    }

    public boolean hasAnyTeam(UUID uuid) {
        return isAdmin(uuid) || playerTeams.containsKey(uuid);
    }

    public Team getAdminTeam() {
        ensureLoaded();
        for (Team t : teams.values()) if (t.isAdmin()) return t;
        return null;
    }

    public CompletableFuture<Boolean> addAdmin(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureLoaded();
            Team admin = getAdminTeam();
            if (admin == null) return false;
            // remove from previous normal team
            String prev = playerTeams.get(uuid);
            if (prev != null) {
                Team t = teams.get(prev);
                if (t != null) t.removePlayer(uuid);
                playerTeams.remove(uuid);
            }
            admin.addPlayer(uuid);
            saveToDiskQuiet();
            return true;
        });
    }

    public CompletableFuture<Boolean> removeAdmin(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            ensureLoaded();
            Team admin = getAdminTeam();
            if (admin == null || !admin.getPlayers().contains(uuid)) return false;
            admin.removePlayer(uuid);
            saveToDiskQuiet();
            return true;
        });
    }

    private void ensureAdminTeam() {
        Team admin = null;
        for (Team t : teams.values()) if (t.isAdmin()) { admin = t; break; }
        if (admin == null) {
            String id = String.valueOf(nextTeamId.getAndIncrement());
            Team t = new Team(id, "admin", ChatColor.GOLD);
            t.setAdmin(true);
            teams.put(id, t);
            teamIdByName.put("admin", id);
        }
    }
    
    public Collection<Team> getAllTeams() {
        ensureLoaded();
        return teams.values();
    }
    
    public List<String> getTeamNames() {
        ensureLoaded();
        return teams.values().stream()
            .map(Team::getName)
            .collect(java.util.stream.Collectors.toList());
    }
    
    public CompletableFuture<Void> sendTeamMessage(String teamNameOrId, String message) {
        return CompletableFuture.runAsync(() -> {
            ensureLoaded();
            String teamId = resolveTeamId(teamNameOrId);
            if (teamId == null) return;
            Team team = teams.get(teamId);
            if (team != null) {
                String formatted = team.getColor() + "[" + team.getName() + "] " + ChatColor.RESET + message;
                Bukkit.getScheduler().runTask(plugin, () -> {
                    // Send to team members
                    for (UUID playerUuid : team.getPlayers()) {
                        Player player = Bukkit.getPlayer(playerUuid);
                        if (player != null && player.isOnline()) {
                            player.sendMessage(formatted);
                        }
                    }
                    // Mirror to spy-enabled admins not in the team
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (!team.getPlayers().contains(online.getUniqueId()) && online.hasPermission("bingo.admin")
                                && plugin.getSpyManager() != null && plugin.getSpyManager().isSpying(online)) {
                            online.sendMessage("§8[SPY] " + formatted);
                        }
                    }
                });
            }
        });
    }
    
    public CompletableFuture<Void> updateTeamPoints(String teamId, String gameId) {
        return CompletableFuture.runAsync(() -> {
            ensureLoaded();
            // Base points: 1 per completed objective
            java.util.List<Objective> grid = plugin.getObjectiveManager().getGameObjectives(gameId);
            int size = Math.min(25, grid != null ? grid.size() : 0);
            boolean[][] done = new boolean[5][5];
            int base = 0;
            for (int i = 0; i < size; i++) {
                Objective o = grid.get(i);
                if (plugin.getObjectiveManager().getObjectiveStatus(teamId, o.getId()) == Objective.ObjectiveStatus.SUCCESS) {
                    base++;
                    int r = i / 5, c = i % 5;
                    done[r][c] = true;
                }
            }
            int bonus = 0;
            // Rows and columns: +3 points each when fully completed
            for (int r = 0; r < 5; r++) {
                boolean full = true;
                for (int c = 0; c < 5; c++) full &= done[r][c];
                if (full) bonus += 3;
            }
            for (int c = 0; c < 5; c++) {
                boolean full = true;
                for (int r = 0; r < 5; r++) full &= done[r][c];
                if (full) bonus += 3;
            }
            // Diagonals: +5 points each when fully completed
            boolean diag1 = true, diag2 = true;
            for (int i = 0; i < 5; i++) {
                diag1 &= done[i][i];
                diag2 &= done[i][4 - i];
            }
            if (diag1) bonus += 5;
            if (diag2) bonus += 5;
            // Full Bingo (all 25): +10 points
            boolean fullBingo = true;
            outer: for (int r = 0; r < 5; r++) {
                for (int c = 0; c < 5; c++) if (!done[r][c]) { fullBingo = false; break outer; }
            }
            if (fullBingo && size >= 25) bonus += 10;
            int total = base + bonus;
            Team team = teams.get(teamId);
            if (team != null) {
                team.setPoints(total);
            }
        });
    }

    public void saveNow() { saveToDiskQuiet(); }
}
