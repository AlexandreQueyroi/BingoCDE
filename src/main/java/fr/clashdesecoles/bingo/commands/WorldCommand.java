package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.*;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class WorldCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public WorldCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtil.get("commands.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length < 1) {
            player.sendMessage("§cUsage: /bingo world <nomDuMonde>");
            player.sendMessage("§eMondes disponibles:");
            for (World world : Bukkit.getWorlds()) {
                player.sendMessage("§7- " + world.getName());
            }
            return true;
        }
        
        String worldName = args[0];
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            player.sendMessage("§cLe monde '" + worldName + "' n'existe pas.");
            player.sendMessage("§eMondes disponibles:");
            for (World w : Bukkit.getWorlds()) {
                player.sendMessage("§7- " + w.getName());
            }
            return true;
        }
        
        player.teleport(world.getSpawnLocation());
        player.sendMessage("§aTeleporte dans le monde: §f" + worldName);
        
        return true;
    }
}
