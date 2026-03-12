package fr.clashdesecoles.bingo;

import fr.clashdesecoles.bingo.commands.*;
import fr.clashdesecoles.bingo.events.*;
import fr.clashdesecoles.bingo.managers.*;
import fr.clashdesecoles.bingo.utils.*;
import org.bukkit.plugin.java.JavaPlugin;

public class BingoPlugin extends JavaPlugin {
    
    private static BingoPlugin instance;
    
    private TeamManager teamManager;
    private GameManager gameManager;
    private ObjectiveManager objectiveManager;
    private ScoreboardManager scoreboardManager;
    private TimerManager timerManager;
    private AdvancementManager advancementManager;
    private SpyManager spyManager;
    private LoggingManager loggingManager;
    private StatsManager statsManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        MessageUtil.load(this);
        IconUtil.load(this);
        
        teamManager = new TeamManager(this);
        gameManager = new GameManager(this);
        gameManager.loadFromDisk();
        objectiveManager = new ObjectiveManager(this);
        scoreboardManager = new ScoreboardManager(this);
        timerManager = new TimerManager(this);
        advancementManager = new AdvancementManager(this);
        spyManager = new SpyManager();
        loggingManager = new LoggingManager(this);
        statsManager = new StatsManager(this);
        
        getCommand("bingo").setExecutor(new BingoCommandRouter(this));
        getCommand("bingo").setTabCompleter(new BingoTabCompleter(this));
        
        getServer().getPluginManager().registerEvents(new ChatEvent(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinEvent(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitEvent(this), this);
        getServer().getPluginManager().registerEvents(new ObjectiveValidationListener(this), this);
        getServer().getPluginManager().registerEvents(new LoginRestrictionListener(this), this);
        getServer().getPluginManager().registerEvents(new MessagesLogListener(this), this);
        getServer().getPluginManager().registerEvents(new MotdListener(this), this);
        
        // Load teams from disk and ensure admin team exists
        teamManager.loadFromDisk();
        
        // OP online admins on reload and default-enable spy for admins
        getServer().getScheduler().runTask(this, () -> {
            var admin = teamManager.getAdminTeam();
            if (admin != null) {
                for (var uuid : admin.getPlayers()) {
                    var p = getServer().getPlayer(uuid);
                    if (p != null) {
                        if (!p.isOp()) p.setOp(true);
                        spyManager.setDefaultIfAbsent(p.getUniqueId(), true);
                    }
                }
            }
            for (var p : getServer().getOnlinePlayers()) {
                if (p.hasPermission("bingo.admin")) {
                    spyManager.setDefaultIfAbsent(p.getUniqueId(), true);
                }
            }
        });
        
        scoreboardManager.start();
        
        getLogger().info("BingoPlugin enabled");
    }
    
    @Override
    public void onDisable() {
        if (advancementManager != null) {
            advancementManager.clearAllAdvancements();
        }
        
        if (timerManager != null) {
            timerManager.stop();
        }
        
        if (scoreboardManager != null) {
            scoreboardManager.stop();
        }

        // Persist data
        if (teamManager != null) teamManager.saveNow();
        if (objectiveManager != null) objectiveManager.saveNow();
        if (gameManager != null) gameManager.saveNow();
        
        getLogger().info("BingoPlugin disabled");
    }
    
    public void reloadPlugin() {
        reloadConfig();
        MessageUtil.reload(this);
        scoreboardManager.reload();
    }
    
    public static BingoPlugin getInstance() {
        return instance;
    }
    
    public TeamManager getTeamManager() {
        return teamManager;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public ObjectiveManager getObjectiveManager() {
        return objectiveManager;
    }
    
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    
    public TimerManager getTimerManager() {
        return timerManager;
    }
    
    public AdvancementManager getAdvancementManager() {
        return advancementManager;
    }

    public SpyManager getSpyManager() {
        return spyManager;
    }

    public LoggingManager getLoggingManager() { return loggingManager; }

    public StatsManager getStatsManager() { return statsManager; }
}
