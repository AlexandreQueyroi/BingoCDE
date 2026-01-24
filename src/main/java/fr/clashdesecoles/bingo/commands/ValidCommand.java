package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.*;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class ValidCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public ValidCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bingo.admin")) {
            sender.sendMessage(MessageUtil.get("commands.no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(MessageUtil.get("objectives.validate-usage"));
            return true;
        }
        
        int objectiveId;
        try {
            objectiveId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtil.get("objectives.invalid-id"));
            return true;
        }
        
        String teamId = args[1];
        Team team = plugin.getTeamManager().getTeam(teamId);
        
        if (team == null) {
            sender.sendMessage(MessageUtil.get("team.team-not-found"));
            return true;
        }
        
        Game game = plugin.getGameManager().getGameForTeam(teamId);
        if (game == null) {
            sender.sendMessage(MessageUtil.get("instance.no-game"));
            return true;
        }
        
        Objective objective = plugin.getObjectiveManager().getObjective(game.getId(), objectiveId);
        if (objective == null) {
            sender.sendMessage(MessageUtil.get("objectives.objective-not-found"));
            return true;
        }
        
        plugin.getObjectiveManager().validateObjective(game.getId(), teamId, objectiveId).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    var map = new HashMap<String, String>();
                    map.put("team", team.getColoredName());
                    map.put("objective", objective.getDisplayName());
                    
                    sender.sendMessage(MessageUtil.get("objectives.validate-success", map));
                    Bukkit.broadcastMessage(MessageUtil.get("objectives.validate-broadcast", map));
                    
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        plugin.getScoreboardManager().updateScoreboard(player);
                        plugin.getScoreboardManager().updateTablist(player);
                    }
                } else {
                    sender.sendMessage(MessageUtil.get("objectives.validate-error"));
                }
            });
        });
        
        return true;
    }
}
