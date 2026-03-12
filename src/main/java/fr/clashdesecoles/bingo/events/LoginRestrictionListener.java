package fr.clashdesecoles.bingo.events;

import fr.clashdesecoles.bingo.BingoPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class LoginRestrictionListener implements Listener {
    private final BingoPlugin plugin;

    public LoginRestrictionListener(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        // Deny players that are not in any team (nor admin)
        try {
            if (!plugin.getTeamManager().hasAnyTeam(event.getUniqueId())) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        "§cVous devez être assigné à une équipe pour rejoindre ce serveur.");
            }
        } catch (Exception ignored) {
        }
    }
}
