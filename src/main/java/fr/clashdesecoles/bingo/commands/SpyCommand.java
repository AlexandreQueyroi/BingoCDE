package fr.clashdesecoles.bingo.commands;

import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpyCommand implements CommandExecutor {
    private final BingoPlugin plugin;

    public SpyCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cCette commande doit être exécutée par un joueur.");
            return true;
        }
        if (!player.hasPermission("bingo.admin")) {
            player.sendMessage(MessageUtil.get("commands.no-permission"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§eUsage: /bingo spy on|off|toggle");
            boolean state = plugin.getSpyManager().isSpying(player);
            player.sendMessage("§7Spy actuel: " + (state ? "§aON" : "§cOFF"));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "on":
                plugin.getSpyManager().setSpying(player.getUniqueId(), true);
                player.sendMessage("§aSpy activé. Vous verrez les messages d'équipes et privés.");
                break;
            case "off":
                plugin.getSpyManager().setSpying(player.getUniqueId(), false);
                player.sendMessage("§cSpy désactivé.");
                break;
            case "toggle":
            default:
                plugin.getSpyManager().toggle(player.getUniqueId());
                boolean state = plugin.getSpyManager().isSpying(player);
                player.sendMessage("§7Spy -> " + (state ? "§aON" : "§cOFF"));
                break;
        }
        return true;
    }
}
