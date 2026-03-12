package fr.clashdesecoles.bingo.commands;

import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Team;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class AdminCommand implements CommandExecutor {
    private final BingoPlugin plugin;

    public AdminCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bingo.admin")) {
            sender.sendMessage(MessageUtil.get("commands.no-permission"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§eUsage: /bingo admin <add|remove|list> [player]");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "add":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /bingo admin add <player>");
                    return true;
                }
                handleAdd(sender, args[1]);
                break;
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /bingo admin remove <player>");
                    return true;
                }
                handleRemove(sender, args[1]);
                break;
            case "list":
                handleList(sender);
                break;
            default:
                sender.sendMessage("§eUsage: /bingo admin <add|remove|list> [player]");
        }

        return true;
    }

    private void handleAdd(CommandSender sender, String playerName) {
        OfflinePlayer off = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = off.getUniqueId();
        plugin.getTeamManager().addAdmin(uuid).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    Player p = off.getPlayer();
                    if (p != null) {
                        if (!p.isOp()) p.setOp(true);
                        p.sendMessage("§aVous avez été ajouté à l'équipe admin et promu OP.");
                    }
                    sender.sendMessage("§aAjouté à l'équipe admin: " + playerName);
                } else {
                    sender.sendMessage("§cImpossible d'ajouter le joueur à l'équipe admin.");
                }
            });
        });
    }

    private void handleRemove(CommandSender sender, String playerName) {
        OfflinePlayer off = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = off.getUniqueId();
        plugin.getTeamManager().removeAdmin(uuid).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    Player p = off.getPlayer();
                    if (p != null) {
                        if (p.isOp()) p.setOp(false);
                        p.sendMessage("§eVous avez été retiré de l'équipe admin et retiré OP.");
                    }
                    sender.sendMessage("§aRetiré de l'équipe admin: " + playerName);
                } else {
                    sender.sendMessage("§cLe joueur n'est pas dans l'équipe admin.");
                }
            });
        });
    }

    private void handleList(CommandSender sender) {
        Team admin = plugin.getTeamManager().getAdminTeam();
        if (admin == null) {
            sender.sendMessage("§cAucune équipe admin trouvée.");
            return;
        }
        sender.sendMessage("§6Membres admin (" + admin.getPlayers().size() + "):");
        for (UUID u : admin.getPlayers()) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(u);
            sender.sendMessage("§7- " + (off.getName() != null ? off.getName() : u));
        }
    }
}
