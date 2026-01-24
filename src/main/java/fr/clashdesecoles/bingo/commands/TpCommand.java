package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class TpCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public TpCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bingo.admin")) {
            sender.sendMessage(MessageUtil.get("commands.no-permission"));
            return true;
        }
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.get("commands.player-only"));
            return true;
        }
        
        if (args.length < 1) {
            sender.sendMessage(MessageUtil.get("teleport.to-player-usage"));
            return true;
        }
        
        Player admin = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);
        
        if (target == null) {
            sender.sendMessage(MessageUtil.get("team.player-not-found"));
            return true;
        }
        
        admin.teleport(target.getLocation());
        var map = new HashMap<String, String>();
        map.put("player", target.getName());
        admin.sendMessage(MessageUtil.get("teleport.to-player-success", map));
        
        return true;
    }
}
