package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.*;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class RejectCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public RejectCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bingo.admin")) {
            sender.sendMessage(MessageUtil.get("commands.no-permission"));
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.get("objectives.reject-usage"));
            return true;
        }
        
        // Accept numeric id or TEXT id (internalName/displayName)
        Integer objectiveId = null;
        String textId = null;
        try {
            objectiveId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            textId = args[0];
        }
        
        String teamId = null;
        Team team = null;
        if (args.length >= 2) {
            teamId = args[1];
            team = plugin.getTeamManager().getTeam(teamId);
        } else if (sender instanceof Player p) {
            Team senderTeam = plugin.getTeamManager().getPlayerTeam(p.getUniqueId());
            if (senderTeam != null && !senderTeam.isAdmin()) {
                team = senderTeam;
                teamId = senderTeam.getId();
            }
        }
        
        if (team == null) {
            sender.sendMessage(MessageUtil.get("team.team-not-found"));
            return true;
        }
        
        Game game = plugin.getGameManager().getGameForTeam(teamId);
        if (game == null) {
            var games = plugin.getGameManager().getAllGames();
            if (games.size() == 1) {
                game = games.iterator().next();
            }
        }
        if (game == null) {
            sender.sendMessage(MessageUtil.get("instance.no-game"));
            return true;
        }
        
        Objective objective;
        if (objectiveId != null) {
            objective = plugin.getObjectiveManager().getObjective(game.getId(), objectiveId);
        } else {
            objective = plugin.getObjectiveManager().getObjectiveByText(game.getId(), textId);
        }
        if (objective == null) {
            sender.sendMessage(MessageUtil.get("objectives.objective-not-found"));
            return true;
        }
        
        final Game fGame = game;
        final Team fTeam = team;
        final Objective fObjective = objective;
        final java.util.List<Objective> grid = plugin.getObjectiveManager().getGameObjectives(fGame.getId());
        final int objIndex = java.util.stream.IntStream.range(0, grid.size())
                .filter(i -> grid.get(i).getId() == fObjective.getId())
                .findFirst().orElse(-1);
        plugin.getObjectiveManager().rejectObjective(fGame.getId(), teamId, fObjective.getId()).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    var map = new HashMap<String, String>();
                    map.put("team", fTeam.getColoredName());
                    map.put("objective", fObjective.getDisplayName());
                    
                    sender.sendMessage(MessageUtil.get("objectives.reject-success", map));
                    Bukkit.broadcastMessage(MessageUtil.get("objectives.reject-broadcast", map));
                    
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        plugin.getScoreboardManager().updateScoreboard(player);
                        plugin.getScoreboardManager().updateTablist(player);
                    }
                    // Revoke advancement for team members if previously granted
                    if (objIndex >= 0) {
                        for (java.util.UUID u : fTeam.getPlayers()) {
                            Player tp = Bukkit.getPlayer(u);
                            if (tp != null) plugin.getAdvancementManager().revokeObjectiveAdvancement(fGame.getId(), objIndex, tp);
                        }
                    }
                    // Show summary of validated/refused objectives for this team
                    String summary = plugin.getObjectiveManager().getStatusSummary(fGame.getId(), fTeam.getId());
                    sender.sendMessage(summary);
                } else {
                    sender.sendMessage(MessageUtil.get("objectives.reject-error"));
                }
            });
        });
        
        return true;
    }
}
