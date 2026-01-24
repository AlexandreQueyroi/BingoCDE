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
        Bukkit.getScheduler().runTask(plugin, () -> {
            Set<NamespacedKey> keys = new HashSet<>();

            try {
                NamespacedKey rootKey = new NamespacedKey(plugin, "bingo_root_" + gameId);

                JsonObject rootAdv = new JsonObject();

                JsonObject display = new JsonObject();
                JsonObject icon = new JsonObject();
                icon.addProperty("item", "minecraft:nether_star");
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

                rootAdv.add("display", display);

                JsonObject criteria = new JsonObject();
                JsonObject impossible = new JsonObject();
                impossible.addProperty("trigger", "minecraft:impossible");
                criteria.add("impossible", impossible);
                rootAdv.add("criteria", criteria);

                Bukkit.getUnsafe().loadAdvancement(rootKey, rootAdv.toString());
                keys.add(rootKey);

                for (int i = 0; i < objectives.size() && i < 25; i++) {
                    Objective obj = objectives.get(i);
                    NamespacedKey objKey = new NamespacedKey(plugin, "bingo_obj_" + gameId + "_" + i);

                    JsonObject objAdv = new JsonObject();
                    objAdv.addProperty("parent", rootKey.toString());

                    JsonObject objDisplay = new JsonObject();
                    JsonObject objIcon = new JsonObject();

                    Material mat = obj.getItem();
                    if (mat == null) mat = Material.BARRIER;
                    objIcon.addProperty("item", "minecraft:" + mat.name().toLowerCase());
                    objDisplay.add("icon", objIcon);

                    JsonObject objTitle = new JsonObject();
                    objTitle.addProperty("text", "§e" + obj.getDisplayName());
                    objDisplay.add("title", objTitle);

                    JsonObject objDesc = new JsonObject();
                    objDesc.addProperty("text", "§7" + obj.getAction().name());
                    objDisplay.add("description", objDesc);

                    objDisplay.addProperty("frame", "task");
                    objDisplay.addProperty("show_toast", true);
                    objDisplay.addProperty("announce_to_chat", false);
                    objDisplay.addProperty("hidden", false);

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
                    player.getAdvancementProgress(Bukkit.getAdvancement(rootKey)).awardCriteria("impossible");
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Error creating advancements: " + e.getMessage());
                e.printStackTrace();
            }
        });
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
        Set<NamespacedKey> keys = gameAdvancements.remove(gameId);
        if (keys != null) {
            for (NamespacedKey key : keys) {
                try {
                    Bukkit.getUnsafe().removeAdvancement(key);
                } catch (Exception e) {
                }
            }
        }
    }

    public void clearAllAdvancements() {
        for (String gameId : new HashSet<>(gameAdvancements.keySet())) {
            removeGameAdvancements(gameId);
        }
    }
}