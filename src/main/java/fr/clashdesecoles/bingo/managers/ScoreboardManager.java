package fr.clashdesecoles.bingo.managers;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.*;
import fr.clashdesecoles.bingo.models.Team;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.scoreboard.Objective;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ScoreboardManager {
    private final BingoPlugin plugin;
    private final Map<Player, Scoreboard> playerScoreboards;
    private final Map<Player, Boolean> scoreboardHidden;
    private BukkitRunnable updater;
    
    public ScoreboardManager(BingoPlugin plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new HashMap<>();
        this.scoreboardHidden = new HashMap<>();
    }
    
    public void start() {
        if (updater != null) updater.cancel();
        updater = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!scoreboardHidden.getOrDefault(player, false)) {
                        updateScoreboard(player);
                        updateTablist(player);
                    }
                }
            }
        };
        updater.runTaskTimer(plugin, 0L, 20L);
    }
    
    public void stop() {
        if (updater != null) {
            updater.cancel();
            updater = null;
        }
    }
    
    public void reload() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player);
            updateTablist(player);
        }
    }
    
    public void updateScoreboard(Player player) {
        Team playerTeam = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        
        Scoreboard scoreboard = playerScoreboards.computeIfAbsent(player, p -> {
            Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
            p.setScoreboard(sb);
            return sb;
        });
        
        Objective objective = scoreboard.getObjective("bingo");
        if (objective != null) objective.unregister();
        
        String title = ChatColor.translateAlternateColorCodes('&', 
            plugin.getConfig().getString("scoreboard.title", "BINGO"));
        objective = scoreboard.registerNewObjective("bingo", "dummy", title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        Map<String, String> placeholders = createPlaceholders(player, playerTeam);
        
        int score = lines.size();
        for (String line : lines) {
            String processed = replacePlaceholders(line, placeholders);
            processed = ChatColor.translateAlternateColorCodes('&', processed);
            if (processed.length() > 40) processed = processed.substring(0, 40);
            
            String uniqueLine = processed;
            int attempt = 0;
            while (scoreboard.getEntries().contains(uniqueLine) && attempt < 10) {
                uniqueLine = processed + ChatColor.values()[attempt % 16];
                attempt++;
            }
            
            objective.getScore(uniqueLine).setScore(score--);
        }
    }
    
    private Map<String, String> createPlaceholders(Player player, Team playerTeam) {
        Map<String, String> placeholders = new HashMap<>();
        
        if (playerTeam != null) {
            placeholders.put("PLAYERTEAM", playerTeam.getColoredName());
            placeholders.put("TEAMPOINT", String.valueOf(playerTeam.getPoints()));
        } else {
            placeholders.put("PLAYERTEAM", "§cAucune equipe");
            placeholders.put("TEAMPOINT", "0");
        }
        
        Game currentGame = null;
        if (playerTeam != null) {
            currentGame = plugin.getGameManager().getGameForTeam(playerTeam.getId());
        }
        if (currentGame != null) {
            placeholders.put("GAMENAME", currentGame.getName());
            long duration = plugin.getConfig().getInt("timer.durationSeconds") * 1000L;
            long elapsed = currentGame.getElapsedTime();
            long remaining = Math.max(0, duration - elapsed);
            placeholders.put("TIMELEFT", formatTime(remaining));
            placeholders.put("TIME", formatTime(duration));
            
            if (playerTeam != null) {
                String adversaryId = currentGame.getTeam1Id().equals(playerTeam.getId()) 
                    ? currentGame.getTeam2Id() : currentGame.getTeam1Id();
                Team adversaryTeam = plugin.getTeamManager().getTeam(adversaryId);
                placeholders.put("ADVERSAIRE", adversaryTeam != null ? adversaryTeam.getColoredName() : "N/A");
            } else {
                placeholders.put("ADVERSAIRE", "N/A");
            }
        } else {
            placeholders.put("GAMENAME", "Aucune partie");
            placeholders.put("TIMELEFT", "00:00:00");
            placeholders.put("TIME", "00:00:00");
            placeholders.put("ADVERSAIRE", "N/A");
        }
        
        placeholders.put("DATE", new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        return placeholders;
    }
    
    private String replacePlaceholders(String text, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }
    
    private String formatTime(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
    
    public void hideScoreboard(Player player) {
        scoreboardHidden.put(player, true);
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
    }
    
    public void showScoreboard(Player player) {
        scoreboardHidden.put(player, false);
        updateScoreboard(player);
        updateTablist(player);
    }
    
    public void removeScoreboard(Player player) {
        playerScoreboards.remove(player);
        scoreboardHidden.remove(player);
    }
    
    public void updateTablist(Player player) {
        Team playerTeam = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        Game currentGame = null;
        if (playerTeam != null) {
            currentGame = plugin.getGameManager().getGameForTeam(playerTeam.getId());
        }
        
        Map<String, String> placeholders = createPlaceholders(player, playerTeam);
        
        String bingoGrid = "";
        if (currentGame != null && playerTeam != null) {
            bingoGrid = plugin.getObjectiveManager().generateBingoGrid(currentGame.getId(), playerTeam.getId());
        } else {
            bingoGrid = "§7Aucune grille disponible";
        }
        placeholders.put("BINGO", bingoGrid);
        
        List<String> headerLines = plugin.getConfig().getStringList("tablist.header");
        StringBuilder header = new StringBuilder();
        for (String line : headerLines) {
            String processed = replacePlaceholders(line, placeholders);
            processed = ChatColor.translateAlternateColorCodes('&', processed);
            header.append(processed).append("\n");
        }
        
        List<String> footerLines = plugin.getConfig().getStringList("tablist.footer");
        StringBuilder footer = new StringBuilder();
        for (String line : footerLines) {
            String processed = replacePlaceholders(line, placeholders);
            processed = ChatColor.translateAlternateColorCodes('&', processed);
            footer.append(processed).append("\n");
        }
        
        player.setPlayerListHeaderFooter(header.toString(), footer.toString());
    }
}
