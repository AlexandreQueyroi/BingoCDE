package fr.clashdesecoles.bingo.events;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Team;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import java.util.*;

public class PlayerQuitEvent implements Listener {
    private final BingoPlugin plugin;
    
    public PlayerQuitEvent(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        plugin.getScoreboardManager().removeScoreboard(player);
        
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team != null) {
            var map = new HashMap<String, String>();
            map.put("player", team.getColor() + player.getName());
            map.put("team", team.getColoredName());
            event.setQuitMessage(MessageUtil.get("team.leave-message", map));
        }
    }
}
