package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class HubCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public HubCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.get("commands.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        World hub = Bukkit.getWorld("hub");
        
        if (hub == null) {
            player.sendMessage("§cLe monde hub n'existe pas.");
            return true;
        }
        
        player.teleport(hub.getSpawnLocation());
        player.sendMessage("§aTeleporte au lobby.");
        
        return true;
    }
}
