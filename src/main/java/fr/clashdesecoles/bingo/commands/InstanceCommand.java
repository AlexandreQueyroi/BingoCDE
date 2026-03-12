package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Team;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class InstanceCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public InstanceCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bingo.admin")) {
            sender.sendMessage(MessageUtil.get("commands.no-permission"));
            return true;
        }
        
        if (args.length < 1) return true;
        
        if (args[0].equalsIgnoreCase("create") && args.length >= 3) {
            Team team1 = plugin.getTeamManager().getTeam(args[1]);
            Team team2 = plugin.getTeamManager().getTeam(args[2]);
            
            if (team1 == null) {
                sender.sendMessage("§cEquipe '" + args[1] + "' introuvable.");
                listTeams(sender);
                return true;
            }
            if (team1.isAdmin()) {
                sender.sendMessage("§cL'équipe admin ne peut pas jouer.");
                return true;
            }
            
            if (team2 == null) {
                sender.sendMessage("§cEquipe '" + args[2] + "' introuvable.");
                listTeams(sender);
                return true;
            }
            if (team2.isAdmin()) {
                sender.sendMessage("§cL'équipe admin ne peut pas jouer.");
                return true;
            }
            
            String gameId = System.currentTimeMillis() + "";
            plugin.getGameManager().createGame(gameId, team1.getId(), team2.getId()).thenAccept(game -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (game != null) {
                        var map = new HashMap<String, String>();
                        map.put("id", gameId);
                        sender.sendMessage(MessageUtil.get("instance.create-success", map));
                        sender.sendMessage("§e" + team1.getColoredName() + " §eVS " + team2.getColoredName());
                    } else {
                        sender.sendMessage(MessageUtil.get("instance.create-error"));
                    }
                });
            });
        } else if (args[0].equalsIgnoreCase("match") && args.length >= 3 && args[1].equalsIgnoreCase("set")) {
            String gameId = null;
            
            if (args.length >= 4) {
                gameId = args[3];
            } else if (sender instanceof Player) {
                Player player = (Player) sender;
                var team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
                if (team != null) {
                    var game = plugin.getGameManager().getGameForTeam(team.getId());
                    if (game != null) {
                        gameId = game.getId();
                    }
                }
            }
            
            if (gameId == null) {
                sender.sendMessage(MessageUtil.get("instance.no-game"));
                return true;
            }
            
            String name = args[2];
            final String finalGameId = gameId;
            
            plugin.getGameManager().setMatchName(finalGameId, name).thenAccept(success -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    var map = new HashMap<String, String>();
                    map.put("name", name);
                    sender.sendMessage(success ? MessageUtil.get("instance.match-set-success", map) : MessageUtil.get("instance.match-set-error"));
                });
            });
        } else {
            sender.sendMessage(MessageUtil.get("instance.create-usage"));
        }
        
        return true;
    }
    
    private void listTeams(CommandSender sender) {
        sender.sendMessage("§eEquipes disponibles:");
        for (Team team : plugin.getTeamManager().getAllTeams()) {
            sender.sendMessage("§7- " + team.getColoredName() + " §7(nom: " + team.getName() + ")");
        }
    }
}
