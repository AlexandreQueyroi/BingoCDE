package fr.clashdesecoles.bingo.events;

import fr.clashdesecoles.bingo.BingoPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class MessagesLogListener implements Listener {
    private final BingoPlugin plugin;

    public MessagesLogListener(BingoPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String msg = event.getMessage();
        if (msg == null) return;
        String lower = msg.toLowerCase();
        if (lower.startsWith("/msg ") || lower.startsWith("/tell ") || lower.startsWith("/w ")
                || lower.startsWith("/m ") || lower.startsWith("/whisper ")) {
            // Try to resolve target player (2nd token)
            String[] parts = msg.split(" ", 3);
            Player sender = event.getPlayer();
            Player target = null;
            if (parts.length >= 2) {
                target = Bukkit.getPlayerExact(parts[1]);
            }
            if (plugin.getLoggingManager() != null) {
                plugin.getLoggingManager().logPrivateMessage(sender, target, msg);
            }
        }
    }
}
