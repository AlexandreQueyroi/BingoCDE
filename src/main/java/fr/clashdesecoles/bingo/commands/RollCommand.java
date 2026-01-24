package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.*;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class RollCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public RollCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bingo.admin")) {
            sender.sendMessage(MessageUtil.get("commands.no-permission"));
            return true;
        }
        
        String gameId = null;
        
        if (args.length > 0) {
            gameId = args[0];
        } else if (sender instanceof Player) {
            Player player = (Player) sender;
            Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team != null) {
                Game game = plugin.getGameManager().getGameForTeam(team.getId());
                if (game != null) {
                    gameId = game.getId();
                }
            }
        }
        
        if (gameId == null) {
            sender.sendMessage("§cUsage: /bingo roll [idPartie]");
            return true;
        }
        
        final String finalGameId = gameId;
        plugin.getObjectiveManager().rollObjectives(finalGameId).thenAccept(objectives -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!objectives.isEmpty()) {
                    var map = new HashMap<String, String>();
                    map.put("count", String.valueOf(objectives.size()));
                    sender.sendMessage(MessageUtil.get("objectives.roll-success", map));
                    
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        plugin.getScoreboardManager().updateTablist(player);
                    }
                } else {
                    sender.sendMessage(MessageUtil.get("objectives.roll-error"));
                }
            });
        });
        
        return true;
    }
}
