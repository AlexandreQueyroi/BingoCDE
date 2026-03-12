package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.*;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class StartCommand implements CommandExecutor {
    private final BingoPlugin plugin;
    
    public StartCommand(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bingo.admin")) {
            sender.sendMessage(MessageUtil.get("commands.no-permission"));
            return true;
        }
        
        String gameId = null;
        
        if (args.length > 0) {
            gameId = args[0];
        } else if (sender instanceof Player) {
            Player player = (Player) sender;
            Team team = plugin.getTeamManager().getPlayerTeam(player.getUniqueId());
            if (team != null) {
                Game game = plugin.getGameManager().getGameForTeam(team.getId());
                if (game != null) {
                    gameId = game.getId();
                }
            }
        }
        
        if (gameId == null) {
            sender.sendMessage("§cUsage: /bingo start [idPartie]");
            return true;
        }
        
        fr.clashdesecoles.bingo.models.Game game = plugin.getGameManager().getGame(gameId);
        if (game == null) {
            sender.sendMessage(MessageUtil.get("instance.no-game"));
            return true;
        }
        // If game is paused, just resume timer and broadcast without TP/countdown
        if (game.getState() == fr.clashdesecoles.bingo.models.Game.GameState.PAUSED) {
            if (plugin.getGameManager().startGame(gameId)) {
                plugin.getTimerManager().start();
                Bukkit.broadcastMessage("§6[§lBINGO§6] §aLa partie reprend !");
            } else {
                sender.sendMessage(MessageUtil.get("timer.start-error"));
            }
            return true;
        }
        if (plugin.getGameManager().startGame(gameId)) {
            // Sequential teleport of all non-admin players to their team worlds with action bar
            java.util.List<org.bukkit.entity.Player> toTeleport = new java.util.ArrayList<>();
            java.util.Map<java.util.UUID, org.bukkit.Location> destinations = new java.util.HashMap<>();

            fr.clashdesecoles.bingo.models.Team t1 = plugin.getTeamManager().getTeam(game.getTeam1Id());
            fr.clashdesecoles.bingo.models.Team t2 = plugin.getTeamManager().getTeam(game.getTeam2Id());

            if (t1 != null) {
                org.bukkit.World w1 = org.bukkit.Bukkit.getWorld(game.getWorld(t1.getId()));
                if (w1 != null) {
                    for (java.util.UUID u : t1.getPlayers()) {
                        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(u);
                        if (p != null && p.isOnline()) {
                            toTeleport.add(p);
                            destinations.put(u, w1.getSpawnLocation());
                        }
                    }
                }
            }
            if (t2 != null) {
                org.bukkit.World w2 = org.bukkit.Bukkit.getWorld(game.getWorld(t2.getId()));
                if (w2 != null) {
                    for (java.util.UUID u : t2.getPlayers()) {
                        org.bukkit.entity.Player p = org.bukkit.Bukkit.getPlayer(u);
                        if (p != null && p.isOnline()) {
                            toTeleport.add(p);
                            destinations.put(u, w2.getSpawnLocation());
                        }
                    }
                }
            }

            // Filter out admins (admin team players should not be teleported)
            fr.clashdesecoles.bingo.models.Team admin = plugin.getTeamManager().getAdminTeam();
            if (admin != null) {
                toTeleport.removeIf(p -> admin.getPlayers().contains(p.getUniqueId()));
            }

            // Set all to Adventure first
            for (org.bukkit.entity.Player p : toTeleport) {
                p.setGameMode(org.bukkit.GameMode.ADVENTURE);
            }

            Bukkit.broadcastMessage("§6[§lBINGO§6] §eTéléportation des joueurs...");

            final int[] idx = {0};
            final java.util.List<org.bukkit.entity.Player> list = toTeleport;
            int tpTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                if (idx[0] < list.size()) {
                    org.bukkit.entity.Player p = list.get(idx[0]++);
                    org.bukkit.Location dest = destinations.get(p.getUniqueId());
                    if (dest != null) {
                        p.teleport(dest);
                        // Action bar-like subtitle to all showing who is being teleported
                        String ab = "§eTP: §f" + p.getName();
                        for (org.bukkit.entity.Player online : Bukkit.getOnlinePlayers()) {
                            online.sendTitle("", ab, 0, 20, 0);
                        }
                    }
                }
            }, 0L, 20L);

            long afterTpDelay = (toTeleport.size() + 1) * 20L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try { Bukkit.getScheduler().cancelTask(tpTaskId); } catch (Exception ignored) {}

                // 10-second countdown, then set Survival and give starter kit
                final int[] seconds = {10};
                final java.util.concurrent.atomic.AtomicInteger cdTaskId = new java.util.concurrent.atomic.AtomicInteger(-1);
                int scheduledCd = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
                    if (seconds[0] > 0) {
                        String msgCd = "§6Départ dans §e" + seconds[0] + "§6s";
                        for (org.bukkit.entity.Player online : Bukkit.getOnlinePlayers()) {
                            online.sendTitle("§6BINGO", msgCd, 0, 20, 0);
                        }
                        seconds[0]--;
                    } else {
                        // Switch to survival and give kits
                        for (org.bukkit.entity.Player p : toTeleport) {
                            if (p.isOnline()) {
                                p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                                org.bukkit.inventory.ItemStack pick = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_PICKAXE, 1);
                                org.bukkit.inventory.ItemStack axe = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_AXE, 1);
                                org.bukkit.inventory.ItemStack shovel = new org.bukkit.inventory.ItemStack(org.bukkit.Material.IRON_SHOVEL, 1);
                                org.bukkit.inventory.ItemStack steak = new org.bukkit.inventory.ItemStack(org.bukkit.Material.COOKED_BEEF, 32);
                                p.getInventory().addItem(pick, axe, shovel, steak);
                            }
                        }
                        int id = cdTaskId.get();
                        if (id != -1) Bukkit.getScheduler().cancelTask(id);
                    }
                }, 0L, 20L);
                cdTaskId.set(scheduledCd);

                // Start timer and broadcast start after countdown finished (10s)
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getTimerManager().start();
                    Bukkit.broadcastMessage(MessageUtil.getRaw("timer.start-broadcast"));
                }, 10 * 20L);
            }, afterTpDelay);
        } else {
            sender.sendMessage(MessageUtil.get("timer.start-error"));
        }
        
        return true;
    }
}
