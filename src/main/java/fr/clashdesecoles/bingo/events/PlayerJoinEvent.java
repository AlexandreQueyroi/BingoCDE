package fr.clashdesecoles.bingo.events;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Game;
import fr.clashdesecoles.bingo.models.Objective;
import fr.clashdesecoles.bingo.models.Team;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
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

            // Default-enable spy for admins on join
            if (player.hasPermission("bingo.admin")) {
                plugin.getSpyManager().setDefaultIfAbsent(player.getUniqueId(), true);
            }

            // Auto-OP admins on join
            if (team.isAdmin() && !player.isOp()) {
                player.setOp(true);
            }

            // If player's team game is not STARTED, teleport to hub (0.5, 101, 0.5) in world "hub"
            Game game = plugin.getGameManager().getGameForTeam(team.getId());
            boolean started = game != null && game.getState() == Game.GameState.STARTED;
            if (!team.isAdmin() && !started) {
                World hub = Bukkit.getWorld("hub");
                if (hub == null) {
                    hub = new WorldCreator("hub").environment(World.Environment.NORMAL).createWorld();
                }
                if (hub != null) {
                    Location hubLoc = new Location(hub, 0.5, 101, 0.5);
                    Bukkit.getScheduler().runTask(plugin, () -> player.teleport(hubLoc));
                }
            }

            // Ensure Bingo advancements page is visible and sync current statuses for this player
            if (game != null) {
                String gameId = game.getId();
                // Award root so the page and children are visible
                NamespacedKey rootKey = new NamespacedKey(plugin, "bingo_root_" + gameId);
                awardIfExists(player, rootKey);
                // Sync each objective advancement to team status
                java.util.List<Objective> grid = plugin.getObjectiveManager().getGameObjectives(gameId);
                for (int i = 0; i < Math.min(25, grid.size()); i++) {
                    Objective obj = grid.get(i);
                    Objective.ObjectiveStatus status = plugin.getObjectiveManager().getObjectiveStatus(team.getId(), obj.getId());
                    if (status == Objective.ObjectiveStatus.SUCCESS) {
                        plugin.getAdvancementManager().grantObjectiveAdvancement(gameId, i, player);
                    } else {
                        plugin.getAdvancementManager().revokeObjectiveAdvancement(gameId, i, player);
                    }
                }
            }

            // Hide vanilla Minecraft advancements for a clean Bingo-only page
            plugin.getAdvancementManager().revokeAllVanillaAdvancements(player);
        }
    }

    private void awardIfExists(Player player, NamespacedKey key) {
        try {
            org.bukkit.advancement.Advancement adv = Bukkit.getAdvancement(key);
            if (adv != null) {
                org.bukkit.advancement.AdvancementProgress pr = player.getAdvancementProgress(adv);
                for (String c : pr.getRemainingCriteria()) pr.awardCriteria(c);
            }
        } catch (Exception ignored) {}
    }
}
