package fr.clashdesecoles.bingo.events;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Team;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatEvent implements Listener {
    private final BingoPlugin plugin;
    
    public ChatEvent(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        
        if (message.startsWith("!")) {
            if (player.hasPermission("bingo.admin")) {
                event.setCancelled(true);
                String adminMessage = message.substring(1);
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player admin : Bukkit.getOnlinePlayers()) {
                        if (admin.hasPermission("bingo.admin")) {
                            admin.sendMessage(MessageUtil.getRaw("chat.admin-prefix") + " " + 
                                player.getName() + ": §f" + adminMessage);
                        }
                    }
                });
            } else {
                event.setCancelled(true);
                player.sendMessage(MessageUtil.get("commands.no-permission"));
            }
            return;
        }
        
        if (message.startsWith(".")) {
            event.setMessage(message.substring(1));
            event.setFormat(MessageUtil.getRaw("chat.global-prefix") + " §f%s: %s");
            return;
        }
        
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team != null) {
            event.setCancelled(true);
            
            String teamMessage = team.getColor() + "[" + team.getName() + "] §f" + 
                player.getName() + ": §r" + message;
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (var playerUuid : team.getPlayers()) {
                    Player teamPlayer = Bukkit.getPlayer(playerUuid);
                    if (teamPlayer != null && teamPlayer.isOnline()) {
                        teamPlayer.sendMessage(teamMessage);
                    }
                }
                // Spy: Admins with spy enabled also receive team messages
                for (Player online : Bukkit.getOnlinePlayers()) {
                    if (!team.getPlayers().contains(online.getUniqueId()) && online.hasPermission("bingo.admin")
                            && plugin.getSpyManager() != null && plugin.getSpyManager().isSpying(online)) {
                        online.sendMessage("§8[SPY] " + teamMessage);
                    }
                }
                // Logging + stats
                var game = plugin.getGameManager().getGameForTeam(team.getId());
                if (game != null && plugin.getLoggingManager() != null) {
                    plugin.getLoggingManager().logTeamChat(game.getId(), team.getId(), player.getUniqueId(), message);
                }
                if (plugin.getStatsManager() != null) {
                    plugin.getStatsManager().increment(player.getUniqueId(), "team_messages");
                }
            });
        }
    }
}
