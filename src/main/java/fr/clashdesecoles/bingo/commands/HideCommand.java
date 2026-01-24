package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class HideCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public HideCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.get("commands.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        plugin.getScoreboardManager().hideScoreboard(player);
        player.sendMessage(MessageUtil.get("display.hide-success"));
        
        return true;
    }
}
