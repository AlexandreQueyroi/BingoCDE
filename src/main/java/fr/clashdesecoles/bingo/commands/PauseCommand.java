package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.*;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class PauseCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public PauseCommand(BingoPlugin plugin) {
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
            sender.sendMessage("§cUsage: /bingo pause [idPartie]");
            return true;
        }
        
        if (plugin.getGameManager().pauseGame(gameId)) {
            Bukkit.broadcastMessage(MessageUtil.getRaw("timer.pause-broadcast"));
        } else {
            sender.sendMessage(MessageUtil.get("timer.pause-error"));
        }
        
        return true;
    }
}
