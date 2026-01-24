package fr.clashdesecoles.bingo;

import fr.clashdesecoles.bingo.commands.*;
import fr.clashdesecoles.bingo.events.*;
import fr.clashdesecoles.bingo.managers.*;
import fr.clashdesecoles.bingo.utils.*;
import org.bukkit.plugin.java.JavaPlugin;

public class BingoPlugin extends JavaPlugin {
    
    private static BingoPlugin instance;
    
    private ApiClient apiClient;
    private TeamManager teamManager;
    private GameManager gameManager;
    private ObjectiveManager objectiveManager;
    private ScoreboardManager scoreboardManager;
    private TimerManager timerManager;
    private AdvancementManager advancementManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        MessageUtil.load(this);
        IconUtil.load(this);
        
        String baseUrl = getConfig().getString("api.baseUrl");
        String apiKey = getConfig().getString("api.key");
        int timeout = getConfig().getInt("api.timeout", 5000);
        
        apiClient = new ApiClient(baseUrl, apiKey, timeout, getLogger());
        
        teamManager = new TeamManager(this);
        gameManager = new GameManager(this);
        objectiveManager = new ObjectiveManager(this);
        scoreboardManager = new ScoreboardManager(this);
        timerManager = new TimerManager(this);
        advancementManager = new AdvancementManager(this);
        
        teamManager.loadTeamsFromAPI().thenRun(() -> {
            getLogger().info("Teams loaded from API");
        });
        
        getCommand("bingo").setExecutor(new BingoCommandRouter(this));
        getCommand("bingo").setTabCompleter(new BingoTabCompleter(this));
        
        getServer().getPluginManager().registerEvents(new ChatEvent(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinEvent(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitEvent(this), this);
        
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
    
    public ApiClient getApiClient() {
        return apiClient;
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
}
