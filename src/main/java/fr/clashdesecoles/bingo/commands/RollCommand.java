package fr.clashdesecoles.bingo.commands;

import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Game;
import fr.clashdesecoles.bingo.models.Team;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class RollCommand implements CommandExecutor {
    private final BingoPlugin plugin;

    public RollCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bingo.admin")) {
            String msg = MessageUtil.get("commands.no-permission");
            sender.sendMessage(msg != null ? msg : "§cVous n'avez pas la permission.");
            return true;
        }

        String gameId = null;

        if (args.length > 0 && args[0] != null && !args[0].isBlank()) {
            gameId = args[0];
        } else if (sender instanceof Player player) {
            Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team != null) {
                Game game = plugin.getGameManager().getGameForTeam(team.getId());
                if (game != null) {
                    gameId = game.getId();
                }
            }
        }

        if (gameId == null) {
            sender.sendMessage("§cUsage: /bingo roll [idPartie]");
            return true;
        }

        String finalGameId = gameId;

        plugin.getLogger().info("[RollCommand] roll demandé par " + sender.getName() + " pour gameId=" + finalGameId);

        // Staged roll: choose 25 random objectives, reveal one per second to everyone, then persist and build UI
        plugin.getObjectiveManager().chooseRandomObjectives().whenComplete((objectives, throwable) -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (throwable != null || objectives == null || objectives.isEmpty()) {
                    String msg = MessageUtil.get("objectives.roll-error");
                    sender.sendMessage(msg != null ? msg : "§cErreur pendant le roll des objectifs.");
                    return;
                }

                // Announce start of roll
                Bukkit.broadcastMessage("§6[§lBINGO§6] §eTirage des objectifs en cours... (§f" + objectives.size() + "§e)");

                final int total = Math.min(25, objectives.size());
                // Reveal task: every 20 ticks (1 second)
                final int[] index = {0};
                final java.util.concurrent.atomic.AtomicInteger taskId = new java.util.concurrent.atomic.AtomicInteger(-1);
                int scheduledId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    if (index[0] < total) {
                        var obj = objectives.get(index[0]);
                        String name = obj.getDisplayName();
                        String icon = fr.clashdesecoles.bingo.utils.IconUtil.getIcon(obj.getItem());
                        // Clean colors in icon
                        icon = icon.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
                        Bukkit.broadcastMessage("§7[§e" + (index[0] + 1) + "/" + total + "§7] §f" + icon + " §b" + name);
                        index[0]++;
                    } else {
                        // Stop repeating
                        int id = taskId.get();
                        if (id != -1) Bukkit.getScheduler().cancelTask(id);
                    }
                }, 0L, 20L);
                taskId.set(scheduledId);

                // After total seconds + small buffer, apply objectives and build UIs
                long delay = (total + 1) * 20L;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // Cancel repeating task if still running
                    try { int id = taskId.get(); if (id != -1) Bukkit.getScheduler().cancelTask(id); } catch (Exception ignored) {}

                    plugin.getObjectiveManager().applyRolledObjectives(finalGameId, objectives).thenAccept(ok -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (!ok) {
                                String msg2 = MessageUtil.get("objectives.roll-error");
                                sender.sendMessage(msg2 != null ? msg2 : "§cEchec de la sauvegarde des objectifs.");
                                return;
                            }
                            // Export grid file before UI refresh
                            plugin.getObjectiveManager().exportGridFile(finalGameId, objectives);
                            // Rebuild advancements only now
                            plugin.getAdvancementManager().removeGameAdvancements(finalGameId);
                            plugin.getAdvancementManager().createAdvancementsForGame(finalGameId, objectives);

                            Bukkit.broadcastMessage("§6[§lBINGO§6] §aTirage terminé ! Ouvrez vos avancées: §eEchap → Avancements → Bingo");

                            for (Player p : Bukkit.getOnlinePlayers()) {
                                plugin.getScoreboardManager().updateScoreboard(p);
                                plugin.getScoreboardManager().updateTablist(p);
                            }
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                for (Player p : Bukkit.getOnlinePlayers()) {
                                    plugin.getScoreboardManager().updateScoreboard(p);
                                    plugin.getScoreboardManager().updateTablist(p);
                                }
                            }, 10L);
                        });
                    });
                }, delay);
            });
        });

        return true;
    }
}
