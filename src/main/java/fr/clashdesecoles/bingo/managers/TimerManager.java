package fr.clashdesecoles.bingo.managers;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.*;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class TimerManager {
    private final BingoPlugin plugin;
    private final Map<String, BukkitRunnable> timerTasks;
    private final Set<String> runningGames;
    
    public TimerManager(BingoPlugin plugin) {
        this.plugin = plugin;
        this.timerTasks = new HashMap<>();
        this.runningGames = new HashSet<>();
    }
    
    public void start() {
        for (Game game : plugin.getGameManager().getAllGames()) {
            if (game.getState() == Game.GameState.STARTED && !runningGames.contains(game.getId())) {
                startGameTimer(game.getId());
            }
        }
    }
    
    private void startGameTimer(String gameId) {
        if (runningGames.contains(gameId)) return;
        
        runningGames.add(gameId);
        long duration = plugin.getConfig().getInt("timer.durationSeconds") * 1000L;
        
        BukkitRunnable task = new BukkitRunnable() {
            public void run() {
                Game game = plugin.getGameManager().getGame(gameId);
                if (game == null || game.getState() == Game.GameState.FINISHED) {
                    stop();
                    return;
                }
                
                if (game.getState() != Game.GameState.STARTED) {
                    return;
                }
                
                long elapsed = game.getElapsedTime();
                long remaining = duration - elapsed;
                
                if (remaining <= 0) {
                    onTimerEnd(gameId);
                    stop();
                    return;
                }
                
                long remainingSeconds = remaining / 1000;
                if (remainingSeconds == 300) {
                    broadcastToGame(gameId, MessageUtil.getRaw("timer.warning-5min"));
                } else if (remainingSeconds == 60) {
                    broadcastToGame(gameId, MessageUtil.getRaw("timer.warning-1min"));
                } else if (remainingSeconds == 30) {
                    broadcastToGame(gameId, MessageUtil.getRaw("timer.warning-30sec"));
                } else if (remainingSeconds <= 10 && remainingSeconds > 0) {
                    broadcastToGame(gameId, "§c" + remainingSeconds + "...");
                }
            }
        };
        
        timerTasks.put(gameId, task);
        task.runTaskTimer(plugin, 0L, 20L);
    }
    
    public void pause() {
        for (String gameId : new HashSet<>(runningGames)) {
            stopGameTimer(gameId);
        }
    }
    
    public void stop() {
        for (String gameId : new HashSet<>(runningGames)) {
            stopGameTimer(gameId);
        }
    }
    
    private void stopGameTimer(String gameId) {
        BukkitRunnable task = timerTasks.remove(gameId);
        if (task != null) {
            task.cancel();
        }
        runningGames.remove(gameId);
    }
    
    public boolean isRunning() {
        return !runningGames.isEmpty();
    }
    
    private void onTimerEnd(String gameId) {
        plugin.getGameManager().stopGame(gameId);
        broadcastToGame(gameId, MessageUtil.getRaw("timer.ended"));
        broadcastToGame(gameId, MessageUtil.getRaw("timer.ended-subtitle"));
        
        Bukkit.getScheduler().runTaskLater(plugin, () -> displayResults(gameId), 40L);
    }
    
    private void displayResults(String gameId) {
        Game game = plugin.getGameManager().getGame(gameId);
        if (game == null) return;
        
        var team1 = plugin.getTeamManager().getTeam(game.getTeam1Id());
        var team2 = plugin.getTeamManager().getTeam(game.getTeam2Id());
        if (team1 == null || team2 == null) return;
        
        broadcastToGame(gameId, MessageUtil.getRaw("timer.results-header"));
        
        var map = new HashMap<String, String>();
        map.put("team", team1.getColoredName());
        map.put("points", String.valueOf(team1.getPoints()));
        broadcastToGame(gameId, MessageUtil.get("timer.results-team", map));
        
        map.put("team", team2.getColoredName());
        map.put("points", String.valueOf(team2.getPoints()));
        broadcastToGame(gameId, MessageUtil.get("timer.results-team", map));
        
        if (team1.getPoints() > team2.getPoints()) {
            map.put("team", team1.getColoredName());
            broadcastToGame(gameId, MessageUtil.get("timer.results-winner", map));
        } else if (team2.getPoints() > team1.getPoints()) {
            map.put("team", team2.getColoredName());
            broadcastToGame(gameId, MessageUtil.get("timer.results-winner", map));
        } else {
            broadcastToGame(gameId, MessageUtil.getRaw("timer.results-draw"));
        }
        
        broadcastToGame(gameId, MessageUtil.getRaw("timer.results-footer"));
    }
    
    private void broadcastToGame(String gameId, String message) {
        Game game = plugin.getGameManager().getGame(gameId);
        if (game == null) return;
        
        var team1 = plugin.getTeamManager().getTeam(game.getTeam1Id());
        var team2 = plugin.getTeamManager().getTeam(game.getTeam2Id());
        
        Set<UUID> players = new HashSet<>();
        if (team1 != null) players.addAll(team1.getPlayers());
        if (team2 != null) players.addAll(team2.getPlayers());
        
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.sendMessage(message);
            }
        }
    }
}
