package fr.clashdesecoles.bingo.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageUtil {
    private static FileConfiguration messages;
    
    public static void load(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }
    
    public static String get(String path) {
        return get(path, new HashMap<>());
    }
    
    public static String get(String path, Map<String, String> placeholders) {
        String msg = messages.getString(path, path);
        String prefix = messages.getString("prefix", "");
        if (!prefix.isEmpty()) {
            msg = prefix + " " + msg;
        }
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
    
    public static String getRaw(String path) {
        return ChatColor.translateAlternateColorCodes('&', messages.getString(path, path));
    }
    
    public static void reload(Plugin plugin) {
        load(plugin);
    }
}
