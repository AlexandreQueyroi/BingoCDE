package fr.clashdesecoles.bingo.managers;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Objective;
import org.bukkit.*;
import org.bukkit.entity.Player;
import com.google.gson.*;
import java.util.*;

public class AdvancementManager {
    private final BingoPlugin plugin;
    private final Map<String, Set<NamespacedKey>> gameAdvancements;

    public AdvancementManager(BingoPlugin plugin) {
        this.plugin = plugin;
        this.gameAdvancements = new HashMap<>();
    }

    public void createAdvancementsForGame(String gameId, List<Objective> objectives) {
        // Kick off attempt 0 on main thread
        Bukkit.getScheduler().runTask(plugin, () -> createAdvancementsForGameAttempt(gameId, objectives, 0));
    }

    private void createAdvancementsForGameAttempt(String gameId, List<Objective> objectives, int attempt) {
        final int MAX_ATTEMPTS = 5;
        Set<NamespacedKey> keys = new HashSet<>();

        // Pre-clean any previous advancements for this gameId (handles fast re-rolls and untracked leftovers)
        // Remove children first (objectives), then legacy rows, then root to satisfy dependency constraints
        for (int i = 24; i >= 0; i--) {
            safeRemove(new NamespacedKey(plugin, "bingo_obj_" + gameId + "_" + i));
        }
        // Legacy rows cleanup (we no longer create rows but keep cleanup for backward compatibility)
        for (int r = 4; r >= 0; r--) {
            safeRemove(new NamespacedKey(plugin, "bingo_row_" + gameId + "_" + r));
        }
        NamespacedKey rootKey = new NamespacedKey(plugin, "bingo_root_" + gameId);
        safeRemove(rootKey);

        // If root still exists, delay and retry to allow registry to flush removals
        if (Bukkit.getAdvancement(rootKey) != null) {
            if (attempt < MAX_ATTEMPTS) {
                long delay = 2L; // 2 ticks backoff
                plugin.getLogger().warning("Advancement root still present after removal for game " + gameId + ", retry " + (attempt + 1));
                Bukkit.getScheduler().runTaskLater(plugin, () -> createAdvancementsForGameAttempt(gameId, objectives, attempt + 1), delay);
            } else {
                plugin.getLogger().severe("Could not recreate advancements for game " + gameId + ": root still exists after retries.");
            }
            return;
        }

        try {
            JsonObject rootAdv = new JsonObject();

            JsonObject display = new JsonObject();
            JsonObject icon = new JsonObject();
            icon.addProperty("id", "minecraft:nether_star");
            display.add("icon", icon);

            JsonObject title = new JsonObject();
            title.addProperty("text", "§6§lBINGO");
            display.add("title", title);

            JsonObject description = new JsonObject();
            description.addProperty("text", "Complete les objectifs!");
            display.add("description", description);

            display.addProperty("frame", "challenge");
            display.addProperty("show_toast", false);
            display.addProperty("announce_to_chat", false);
            display.addProperty("hidden", false);
            // Background texture for the Bingo page
            display.addProperty("background", "minecraft:textures/gui/advancements/backgrounds/stone.png");

            rootAdv.add("display", display);

            JsonObject criteria = new JsonObject();
            JsonObject impossible = new JsonObject();
            impossible.addProperty("trigger", "minecraft:impossible");
            criteria.add("impossible", impossible);
            rootAdv.add("criteria", criteria);

            Bukkit.getUnsafe().loadAdvancement(rootKey, rootAdv.toString());
            keys.add(rootKey);

            // Create 25 objectives directly under root; position them to form a 5x5 grid to the RIGHT of the nether star
            for (int i = 0; i < Math.min(25, objectives.size()); i++) {
                Objective obj = objectives.get(i);
                NamespacedKey objKey = new NamespacedKey(plugin, "bingo_obj_" + gameId + "_" + i);

                JsonObject objAdv = new JsonObject();
                int row = i / 5; int col = i % 5;
                objAdv.addProperty("parent", rootKey.toString());

                JsonObject objDisplay = new JsonObject();
                JsonObject objIcon = new JsonObject();

                Material mat = obj.getItem();
                if (mat == null) mat = Material.BARRIER;
                objIcon.addProperty("id", "minecraft:" + mat.name().toLowerCase());
                objDisplay.add("icon", objIcon);

                JsonObject objTitle = new JsonObject();
                objTitle.addProperty("text", "§e" + obj.getDisplayName());
                objDisplay.add("title", objTitle);

                JsonObject objDesc = new JsonObject();
                String quick = switch (obj.getAction()) {
                    case ITEM -> "Crafter/obtenir: " + (obj.getActionCheck() == null || obj.getActionCheck().isBlank() ? mat.name() : obj.getActionCheck());
                    case KILL -> "Élimination: " + (obj.getActionCheck() == null || obj.getActionCheck().isBlank() ? "PLAYER" : obj.getActionCheck());
                    case OBJECTIVE -> "Avancement: " + (obj.getActionCheck() == null ? "?" : obj.getActionCheck());
                };
                objDesc.addProperty("text", "§7" + quick);
                objDisplay.add("description", objDesc);

                objDisplay.addProperty("frame", "task");
                objDisplay.addProperty("show_toast", true);
                objDisplay.addProperty("announce_to_chat", false);
                objDisplay.addProperty("hidden", false);

                // Position relative to root: keep root at center-left (row index 2), grid starts one column to the right
                objDisplay.addProperty("x", (col + 1) * 2);            // shift right of root
                objDisplay.addProperty("y", (row - 2) * 2);            // center rows around root

                objAdv.add("display", objDisplay);

                JsonObject objCriteria = new JsonObject();
                JsonObject objImpossible = new JsonObject();
                objImpossible.addProperty("trigger", "minecraft:impossible");
                objCriteria.add("impossible", objImpossible);
                objAdv.add("criteria", objCriteria);

                Bukkit.getUnsafe().loadAdvancement(objKey, objAdv.toString());
                keys.add(objKey);
            }

            gameAdvancements.put(gameId, keys);
            plugin.getLogger().info("Created " + keys.size() + " advancements for game " + gameId);

            for (Player player : Bukkit.getOnlinePlayers()) {
                // Award root and row parents so the page and all rows are visible before completion
                var rootAdvancement = Bukkit.getAdvancement(rootKey);
                if (rootAdvancement != null) {
                    org.bukkit.advancement.AdvancementProgress pr = player.getAdvancementProgress(rootAdvancement);
                    for (String c : pr.getRemainingCriteria()) pr.awardCriteria(c);
                }
                for (int r = 0; r < 5; r++) {
                    NamespacedKey rk = new NamespacedKey(plugin, "bingo_row_" + gameId + "_" + r);
                    var rowAdv = Bukkit.getAdvancement(rk);
                    if (rowAdv != null) {
                        org.bukkit.advancement.AdvancementProgress pr2 = player.getAdvancementProgress(rowAdv);
                        for (String c : pr2.getRemainingCriteria()) pr2.awardCriteria(c);
                    }
                }
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error creating advancements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void grantObjectiveAdvancement(String gameId, int objectiveIndex, Player player) {
        NamespacedKey key = new NamespacedKey(plugin, "bingo_obj_" + gameId + "_" + objectiveIndex);

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                org.bukkit.advancement.Advancement advancement = Bukkit.getAdvancement(key);
                if (advancement != null) {
                    org.bukkit.advancement.AdvancementProgress progress = player.getAdvancementProgress(advancement);
                    for (String criterion : progress.getRemainingCriteria()) {
                        progress.awardCriteria(criterion);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not grant advancement: " + e.getMessage());
            }
        });
    }

    public void removeGameAdvancements(String gameId) {
        // Remove any tracked keys first
        Set<NamespacedKey> keys = gameAdvancements.remove(gameId);
        if (keys != null) {
            for (NamespacedKey key : keys) {
                safeRemove(key);
            }
        }
        // Fallback: deterministically remove expected keys in case tracking was missing (e.g., previous error)
        safeRemove(new NamespacedKey(plugin, "bingo_root_" + gameId));
        for (int r = 0; r < 5; r++) {
            safeRemove(new NamespacedKey(plugin, "bingo_row_" + gameId + "_" + r));
        }
        for (int i = 0; i < 25; i++) {
            safeRemove(new NamespacedKey(plugin, "bingo_obj_" + gameId + "_" + i));
        }
    }

    public void revokeObjectiveAdvancement(String gameId, int objectiveIndex, Player player) {
        NamespacedKey key = new NamespacedKey(plugin, "bingo_obj_" + gameId + "_" + objectiveIndex);
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                org.bukkit.advancement.Advancement advancement = Bukkit.getAdvancement(key);
                if (advancement != null) {
                    org.bukkit.advancement.AdvancementProgress progress = player.getAdvancementProgress(advancement);
                    for (String criterion : new java.util.HashSet<>(progress.getAwardedCriteria())) {
                        progress.revokeCriteria(criterion);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Could not revoke advancement: " + e.getMessage());
            }
        });
    }

    private void safeRemove(NamespacedKey key) {
        try {
            // Only attempt removal if it exists
            if (Bukkit.getAdvancement(key) != null) {
                Bukkit.getUnsafe().removeAdvancement(key);
            }
        } catch (Exception ignored) {
        }
    }

    public void clearAllAdvancements() {
        for (String gameId : new HashSet<>(gameAdvancements.keySet())) {
            removeGameAdvancements(gameId);
        }
    }

    public void revokeAllVanillaAdvancements(Player player) {
        try {
            java.util.Iterator<org.bukkit.advancement.Advancement> it = Bukkit.getServer().advancementIterator();
            while (it.hasNext()) {
                org.bukkit.advancement.Advancement adv = it.next();
                if (adv.getKey() != null && "minecraft".equalsIgnoreCase(adv.getKey().getNamespace())) {
                    org.bukkit.advancement.AdvancementProgress pr = player.getAdvancementProgress(adv);
                    for (String c : new java.util.HashSet<>(pr.getAwardedCriteria())) {
                        pr.revokeCriteria(c);
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}