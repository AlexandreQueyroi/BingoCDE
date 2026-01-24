package fr.clashdesecoles.bingo.events;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Team;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import java.util.*;

public class PlayerJoinEvent implements Listener {
    private final BingoPlugin plugin;
    
    public PlayerJoinEvent(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        plugin.getScoreboardManager().updateScoreboard(player);
        plugin.getScoreboardManager().updateTablist(player);
        
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team != null) {
            var map = new HashMap<String, String>();
            map.put("player", team.getColor() + player.getName());
            map.put("team", team.getColoredName());
            event.setJoinMessage(MessageUtil.get("team.join-message", map));
        }
    }
}
