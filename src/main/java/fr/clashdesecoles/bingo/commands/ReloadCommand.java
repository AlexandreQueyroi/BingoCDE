package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.command.*;

public class ReloadCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public ReloadCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bingo.admin")) {
            sender.sendMessage(MessageUtil.get("commands.no-permission"));
            return true;
        }
        
        plugin.reloadPlugin();
        sender.sendMessage(MessageUtil.get("commands.reload-success"));
        
        return true;
    }
}
