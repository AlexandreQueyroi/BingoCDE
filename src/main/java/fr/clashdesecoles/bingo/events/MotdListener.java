package fr.clashdesecoles.bingo.events;

import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.managers.GameManager;
import fr.clashdesecoles.bingo.models.Game;
import fr.clashdesecoles.bingo.models.Team;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.Comparator;
import java.util.Optional;

public class MotdListener implements Listener {
    private final BingoPlugin plugin;

    public MotdListener(BingoPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        Game game = pickActiveGame(plugin.getGameManager());
        if (game != null) {
            Team t1 = plugin.getTeamManager().getTeam(game.getTeam1Id());
            Team t2 = plugin.getTeamManager().getTeam(game.getTeam2Id());
            String team1 = t1 != null ? stripColors(t1.getName()) : game.getTeam1Id();
            String team2 = t2 != null ? stripColors(t2.getName()) : game.getTeam2Id();
            String match = game.getName() != null ? game.getName() : ("Game " + game.getId());
            String motd = "CDE BINGO : [" + team1 + "] VS [" + team2 + "] (" + match + ")";
            event.setMotd(motd);
        }
    }

    private Game pickActiveGame(GameManager gm) {
        // 1) Prefer STARTED
        Optional<Game> started = gm.getAllGames().stream()
                .filter(g -> g.getState() == Game.GameState.STARTED)
                .findFirst();
        if (started.isPresent()) return started.get();
        // 2) Else most recent CREATED (best-effort using numeric id when possible)
        Optional<Game> created = gm.getAllGames().stream()
                .filter(g -> g.getState() == Game.GameState.CREATED)
                .max(Comparator.comparingLong(g -> parseLongSafe(g.getId())));
        if (created.isPresent()) return created.get();
        // 3) Fallback to any existing
        return gm.getAllGames().stream().findFirst().orElse(null);
    }

    private long parseLongSafe(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return Long.MIN_VALUE; // ensure non-numeric ids sort last
        }
    }

    private String stripColors(String s) {
        return s == null ? "" : s.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }
}
