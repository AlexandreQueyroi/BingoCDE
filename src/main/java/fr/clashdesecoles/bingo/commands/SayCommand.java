package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import java.util.*;

public class SayCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public SayCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bingo.admin")) {
            sender.sendMessage(MessageUtil.get("commands.no-permission"));
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.get("say.usage"));
            return true;
        }
        
        String message = String.join(" ", args);
        Bukkit.broadcastMessage("§6[ANNONCE] §f" + message);
        
        return true;
    }
}
