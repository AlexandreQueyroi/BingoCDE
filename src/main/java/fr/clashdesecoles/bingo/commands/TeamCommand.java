package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Team;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class TeamCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public TeamCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bingo.admin")) {
            sender.sendMessage(MessageUtil.get("commands.no-permission"));
            return true;
        }
        
        if (args.length < 1) return true;
        
        String action = args[0].toLowerCase();
        
        switch (action) {
            case "create":
                if (args.length < 3) {
                    sender.sendMessage(MessageUtil.get("team.create-usage"));
                    return true;
                }
                handleCreate(sender, args[1], args[2]);
                break;
            case "add":
                if (args.length < 3) {
                    sender.sendMessage(MessageUtil.get("team.add-usage"));
                    return true;
                }
                handleAdd(sender, args[1], args[2]);
                break;
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage(MessageUtil.get("team.remove-usage"));
                    return true;
                }
                handleRemove(sender, args[1]);
                break;
            case "chat":
                if (args.length < 3) {
                    sender.sendMessage(MessageUtil.get("team.chat-usage"));
                    return true;
                }
                handleChat(sender, args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                break;
        }
        
        return true;
    }
    
    private void handleCreate(CommandSender sender, String name, String colorStr) {
        ChatColor color;
        try {
            color = ChatColor.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(MessageUtil.get("team.invalid-color"));
            return;
        }
        
        plugin.getTeamManager().createTeam(name, name, color).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    var map = new HashMap<String, String>();
                    map.put("team", color + name);
                    sender.sendMessage(MessageUtil.get("team.create-success", map));
                } else {
                    sender.sendMessage(MessageUtil.get("team.create-error"));
                }
            });
        });
    }
    
    private void handleAdd(CommandSender sender, String teamName, String playerName) {
        // Try online first
        Player online = Bukkit.getPlayer(playerName);
        java.util.UUID targetUuid = null;
        String targetName = playerName;
        if (online != null) {
            targetUuid = online.getUniqueId();
            targetName = online.getName();
        } else {
            // Resolve offline player by name, even if never joined
            org.bukkit.OfflinePlayer off = Bukkit.getOfflinePlayer(playerName);
            if (off != null) {
                targetUuid = off.getUniqueId();
                if (off.getName() != null) targetName = off.getName();
            }
        }
        if (targetUuid == null) {
            sender.sendMessage(MessageUtil.get("team.player-not-found"));
            return;
        }

        Team team = plugin.getTeamManager().getTeam(teamName);
        if (team == null) {
            sender.sendMessage(MessageUtil.get("team.team-not-found"));
            sender.sendMessage("§eEquipes disponibles:");
            for (Team t : plugin.getTeamManager().getAllTeams()) {
                sender.sendMessage("§7- " + t.getName() + " (ID: " + t.getId() + ")");
            }
            return;
        }
        final String fTargetName = targetName;
        final java.util.UUID fTargetUuid = targetUuid;
        plugin.getTeamManager().addPlayer(teamName, fTargetUuid).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    var map = new HashMap<String, String>();
                    map.put("player", fTargetName);
                    map.put("team", team.getColoredName());
                    sender.sendMessage(MessageUtil.get("team.add-success", map));
                    if (online != null) {
                        online.sendMessage(MessageUtil.get("team.join-message", map));
                    }
                } else {
                    sender.sendMessage(MessageUtil.get("team.add-error"));
                }
            });
        });
    }
    
    private void handleRemove(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(MessageUtil.get("team.player-not-found"));
            return;
        }
        
        Team team = plugin.getTeamManager().getPlayerTeam(target.getUniqueId());
        if (team == null) {
            sender.sendMessage(MessageUtil.get("team.player-no-team"));
            return;
        }
        
        plugin.getTeamManager().removePlayer(target.getUniqueId()).thenAccept(success -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                var map = new HashMap<String, String>();
                map.put("player", target.getName());
                map.put("team", team.getColoredName());
                sender.sendMessage(success ? MessageUtil.get("team.remove-success", map) : MessageUtil.get("team.remove-error"));
            });
        });
    }
    
    private void handleChat(CommandSender sender, String teamName, String message) {
        Team team = plugin.getTeamManager().getTeam(teamName);
        if (team == null) {
            sender.sendMessage(MessageUtil.get("team.team-not-found"));
            return;
        }
        
        // Prefix with Admin label as requested
        String msg = "§cAdmin: " + message;
        plugin.getTeamManager().sendTeamMessage(teamName, msg);
        var map = new HashMap<String, String>();
        map.put("team", team.getColoredName());
        sender.sendMessage(MessageUtil.get("team.chat-sent", map));
    }
}
