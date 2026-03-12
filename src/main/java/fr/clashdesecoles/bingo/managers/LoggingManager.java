package fr.clashdesecoles.bingo.managers;

import fr.clashdesecoles.bingo.BingoPlugin;
import org.bukkit.entity.Player;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoggingManager {
    private final BingoPlugin plugin;
    private final File baseDir;

    public LoggingManager(BingoPlugin plugin) {
        this.plugin = plugin;
        this.baseDir = new File(plugin.getDataFolder(), "logs");
        if (!baseDir.exists()) baseDir.mkdirs();
    }

    private String ts() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date());
    }

    private synchronized void appendJsonLine(File file, Map<String, Object> map) {
        try {
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            try (Writer w = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)) {
                String json = new com.google.gson.Gson().toJson(map);
                w.write(json);
                w.write("\n");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to append log to " + file.getName() + ": " + e.getMessage());
        }
    }

    public void logTeamChat(String gameId, String teamId, UUID from, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("ts", ts());
        m.put("type", "team_chat");
        m.put("gameId", gameId);
        m.put("teamId", teamId);
        m.put("from", from.toString());
        m.put("message", message);
        File file = new File(baseDir, "teams/" + gameId + "/" + teamId + ".jsonl");
        appendJsonLine(file, m);
    }

    public void logObjective(String gameId, String teamId, int objectiveId, String status, String by, String details) {
        Map<String, Object> m = new HashMap<>();
        m.put("ts", ts());
        m.put("type", "objective");
        m.put("gameId", gameId);
        m.put("teamId", teamId);
        m.put("objectiveId", objectiveId);
        m.put("status", status);
        if (by != null) m.put("by", by);
        if (details != null) m.put("details", details);
        File file = new File(baseDir, "teams/" + gameId + "/" + teamId + ".jsonl");
        appendJsonLine(file, m);
    }

    public void logDeath(String gameId, UUID victim, UUID killer) {
        Map<String, Object> m = new HashMap<>();
        m.put("ts", ts());
        m.put("type", "death");
        m.put("gameId", gameId);
        m.put("victim", victim != null ? victim.toString() : null);
        if (killer != null) m.put("killer", killer.toString());
        File file = new File(baseDir, "deaths.jsonl");
        appendJsonLine(file, m);
    }

    public void logTeamEvent(String gameId, String teamId, String type, Map<String, Object> payload) {
        Map<String, Object> m = new HashMap<>();
        m.put("ts", ts());
        m.put("type", type);
        m.put("gameId", gameId);
        m.put("teamId", teamId);
        if (payload != null) m.putAll(payload);
        File file = new File(baseDir, "teams/" + gameId + "/" + teamId + ".jsonl");
        appendJsonLine(file, m);
    }

    public void logPrivateMessage(Player from, Player to, String rawCommand) {
        Map<String, Object> m = new HashMap<>();
        m.put("ts", ts());
        m.put("type", "pm");
        m.put("from", from != null ? from.getUniqueId().toString() : null);
        m.put("to", to != null ? to.getUniqueId().toString() : null);
        m.put("raw", rawCommand);
        File file = new File(baseDir, "private_messages.jsonl");
        appendJsonLine(file, m);
    }
}
