package fr.clashdesecoles.bingo.utils;
import fr.clashdesecoles.bingo.BingoPlugin;
import org.bukkit.Material;
import java.io.*;
import java.util.*;

public class IconUtil {
    private static final Map<String, String> icons = new HashMap<>();
    
    public static void load(BingoPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "item_icons.properties");
        if (!file.exists()) {
            plugin.saveResource("item_icons.properties", false);
        }
        
        try (InputStream input = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(new InputStreamReader(input, "UTF-8"));
            for (String key : props.stringPropertyNames()) {
                icons.put(key.toLowerCase(), props.getProperty(key));
            }
            plugin.getLogger().info("Loaded " + icons.size() + " item icons");
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load item_icons.properties: " + e.getMessage());
        }
    }
    
    public static String getIcon(Material material) {
        if (material == null) return "§7?";
        
        String materialName = material.name().toLowerCase();
        String icon = icons.get(materialName);
        
        if (icon == null) {
            materialName = materialName.replace("_", "");
            icon = icons.get(materialName);
        }
        
        return icon != null ? icon : "§7" + material.name().charAt(0);
    }
    
    public static String getLargeIcon(Material material) {
        String icon = getIcon(material);
        // Strip color codes to keep width predictable
        String clean = icon.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
        if (clean.isEmpty()) clean = "?";
        // Return a doubled glyph sequence without colors; caller applies colors/styles
        return clean + clean;
    }
}
