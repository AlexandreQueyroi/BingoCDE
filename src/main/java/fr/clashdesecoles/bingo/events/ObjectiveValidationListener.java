package fr.clashdesecoles.bingo.events;

import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Game;
import fr.clashdesecoles.bingo.models.Objective;
import fr.clashdesecoles.bingo.models.Team;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class ObjectiveValidationListener implements Listener {
    private final BingoPlugin plugin;

    public ObjectiveValidationListener(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        HumanEntity who = event.getWhoClicked();
        if (!(who instanceof Player player)) return;

        Material result = event.getRecipe() != null && event.getRecipe().getResult() != null
                ? event.getRecipe().getResult().getType()
                : null;
        if (result == null) return;

        validateForPlayer(player, obj -> obj.getAction() == Objective.ObjectiveAction.ITEM &&
                (obj.getItem() == result || equalsIgnoreCase(obj.getActionCheck(), result.name())),
                "craft: " + result.name());
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Log death and stats
        try {
            Team kt = plugin.getTeamManager().getPlayerTeam(killer.getUniqueId());
            Game g = (kt != null) ? plugin.getGameManager().getGameForTeam(kt.getId()) : null;
            if (plugin.getLoggingManager() != null) {
                plugin.getLoggingManager().logDeath(g != null ? g.getId() : null,
                        event.getEntity().getUniqueId(), killer.getUniqueId());
            }
            if (plugin.getStatsManager() != null) {
                plugin.getStatsManager().increment(killer.getUniqueId(), "kills");
                plugin.getStatsManager().increment(event.getEntity().getUniqueId(), "deaths");
            }
        } catch (Exception ignored) {}

        validateForPlayer(killer, obj -> {
            if (obj.getAction() != Objective.ObjectiveAction.KILL) return false;
            String check = obj.getActionCheck();
            // If check is empty or equals PLAYER, consider valid on any player kill
            return check == null || check.isBlank() || equalsIgnoreCase(check, "PLAYER");
        }, "kill: PLAYER");
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        NamespacedKey key = event.getAdvancement().getKey();

        // Ignore our own custom Bingo advancements to avoid loops
        if (key.getNamespace().equalsIgnoreCase(plugin.getName())) {
            return;
        }

        String full = key.toString();            // e.g. minecraft:story/mine_stone
        String path = key.getKey();              // e.g. story/mine_stone
        String withNs = "minecraft:" + path;    // convenience match

        validateForPlayer(player, obj -> obj.getAction() == Objective.ObjectiveAction.OBJECTIVE && (
                equalsIgnoreCase(obj.getActionCheck(), full) ||
                equalsIgnoreCase(obj.getActionCheck(), path) ||
                equalsIgnoreCase(obj.getActionCheck(), withNs)
        ), "advancement: " + full);
    }

    private void validateForPlayer(Player player, Predicate<Objective> matcher, String triggerInfo) {
        Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
        if (team == null) return;

        Game game = plugin.getGameManager().getGameForTeam(team.getId());
        if (game == null || game.getState() != Game.GameState.STARTED) return;

        List<Objective> objectives = plugin.getObjectiveManager().getGameObjectives(game.getId());
        if (objectives == null || objectives.isEmpty()) return;

        for (int index = 0; index < objectives.size(); index++) {
            Objective obj = objectives.get(index);
            if (!matcher.test(obj)) continue;

            // Skip if already validated or rejected
            Objective.ObjectiveStatus status = plugin.getObjectiveManager().getObjectiveStatus(team.getId(), obj.getId());
            if (status == Objective.ObjectiveStatus.SUCCESS || status == Objective.ObjectiveStatus.REJECTED) continue;

            final int objIndex = index;
            plugin.getObjectiveManager().validateObjective(game.getId(), team.getId(), obj.getId()).thenAccept(success -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (success) {
                        Map<String, String> map = new HashMap<>();
                        map.put("team", team.getColoredName());
                        map.put("objective", obj.getDisplayName());

                        // Broadcast and update UIs
                        Bukkit.broadcastMessage(fr.clashdesecoles.bingo.utils.MessageUtil.get("objectives.validate-broadcast", map));
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            plugin.getScoreboardManager().updateScoreboard(p);
                            plugin.getScoreboardManager().updateTablist(p);
                        }

                        // Grant the visual Bingo advancement for this objective to all members of the team
                        for (java.util.UUID u : team.getPlayers()) {
                            org.bukkit.entity.Player tp = org.bukkit.Bukkit.getPlayer(u);
                            if (tp != null) {
                                plugin.getAdvancementManager().grantObjectiveAdvancement(game.getId(), objIndex, tp);
                            }
                        }
                        // Logging + stats for the triggering player
                        if (plugin.getLoggingManager() != null) {
                            plugin.getLoggingManager().logObjective(game.getId(), team.getId(), obj.getId(), "SUCCESS", player.getName(), triggerInfo);
                        }
                        if (plugin.getStatsManager() != null) {
                            plugin.getStatsManager().increment(player.getUniqueId(), "objectives_validated");
                        }
                    } else {
                        plugin.getLogger().warning("Objective validation failed for team=" + team.getId() +
                                ", game=" + game.getId() + ", objId=" + obj.getId() + " (trigger=" + triggerInfo + ")");
                    }
                });
            });
        }
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }
}
