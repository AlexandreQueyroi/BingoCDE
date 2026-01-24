package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class ShowCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public ShowCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.get("commands.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        plugin.getScoreboardManager().showScoreboard(player);
        player.sendMessage(MessageUtil.get("display.show-success"));
        
        return true;
    }
}
