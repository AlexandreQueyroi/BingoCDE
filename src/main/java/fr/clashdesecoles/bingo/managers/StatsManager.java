package fr.clashdesecoles.bingo.managers;

import fr.clashdesecoles.bingo.BingoPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StatsManager {
    private final BingoPlugin plugin;
    private final File baseDir;

    public StatsManager(BingoPlugin plugin) {
        this.plugin = plugin;
        this.baseDir = new File(plugin.getDataFolder(), "stats");
        if (!baseDir.exists()) baseDir.mkdirs();
    }

    private synchronized Map<String, Integer> load(UUID uuid) {
        File f = new File(baseDir, uuid.toString() + ".json");
        Map<String, Integer> map = new HashMap<>();
        if (!f.exists()) return map;
        try {
            String json = new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            for (String k : o.keySet()) {
                try { map.put(k, o.get(k).getAsInt()); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return map;
    }

    private synchronized void save(UUID uuid, Map<String, Integer> map) {
        File f = new File(baseDir, uuid.toString() + ".json");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
            gson.toJson(map, w);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save stats for " + uuid + ": " + e.getMessage());
        }
    }

    public void increment(UUID uuid, String key) {
        if (uuid == null || key == null) return;
        Map<String, Integer> map = load(uuid);
        map.put(key, map.getOrDefault(key, 0) + 1);
        save(uuid, map);
    }
}
