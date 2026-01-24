package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;

public class ExportCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public ExportCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bingo.admin")) {
            sender.sendMessage(MessageUtil.get("commands.no-permission"));
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.get("export.usage"));
            return true;
        }
        
        String gameId = args[0];
        plugin.getGameManager().exportGameResults(gameId).thenAccept(result -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result != null) {
                    sender.sendMessage(MessageUtil.get("export.success"));
                    sender.sendMessage(result.toString());
                } else {
                    sender.sendMessage(MessageUtil.get("export.error"));
                }
            });
        });
        
        return true;
    }
}
