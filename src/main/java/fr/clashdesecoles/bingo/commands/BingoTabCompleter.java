package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.models.Team;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.stream.Collectors;

public class BingoTabCompleter implements TabCompleter {
    private final BingoPlugin plugin;
    
    public BingoTabCompleter(BingoPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList(
                "team", "instance", "roll", "valid", "reject",
                "start", "pause", "stop", "show", "hide",
                "tp", "tphere", "export", "say", "reload",
                "hub", "world"
            ));
        } else if (args.length >= 2) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "team":
                    completions.addAll(completeTeamCommand(args));
                    break;
                case "instance":
                    completions.addAll(completeInstanceCommand(args));
                    break;
                case "valid":
                case "reject":
                    completions.addAll(completeObjectiveCommand(args));
                    break;
                case "tp":
                case "tphere":
                    completions.addAll(completePlayerCommand(args));
                    break;
                case "world":
                    completions.addAll(completeWorldCommand(args));
                    break;
            }
        }
        
        String partial = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(partial))
            .sorted()
            .collect(Collectors.toList());
    }
    
    private List<String> completeTeamCommand(String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 2) {
            completions.addAll(Arrays.asList("create", "add", "remove", "chat"));
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("chat")) {
                completions.addAll(getTeamNames());
            } else if (args[1].equalsIgnoreCase("remove")) {
                completions.addAll(getOnlinePlayerNames());
            } else if (args[1].equalsIgnoreCase("create")) {
                completions.add("<nom>");
            }
        } else if (args.length == 4) {
            if (args[1].equalsIgnoreCase("add")) {
                completions.addAll(getOnlinePlayerNames());
            } else if (args[1].equalsIgnoreCase("create")) {
                completions.addAll(Arrays.asList(
                    "RED", "BLUE", "GREEN", "YELLOW", "GOLD",
                    "AQUA", "DARK_RED", "DARK_BLUE", "DARK_GREEN",
                    "DARK_PURPLE", "LIGHT_PURPLE", "WHITE"
                ));
            }
        }
        
        return completions;
    }
    
    private List<String> completeInstanceCommand(String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 2) {
            completions.addAll(Arrays.asList("create", "match"));
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase("create")) {
                completions.addAll(getTeamNames());
            } else if (args[1].equalsIgnoreCase("match")) {
                completions.add("set");
            }
        } else if (args.length == 4) {
            if (args[1].equalsIgnoreCase("create")) {
                completions.addAll(getTeamNames());
            } else if (args[1].equalsIgnoreCase("match") && args[2].equalsIgnoreCase("set")) {
                completions.add("<nomDuMatch>");
            }
        }
        
        return completions;
    }
    
    private List<String> completeObjectiveCommand(String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 2) {
            for (int i = 1; i <= 25; i++) {
                completions.add(String.valueOf(i));
            }
        } else if (args.length == 3) {
            completions.addAll(getTeamNames());
        }
        
        return completions;
    }
    
    private List<String> completePlayerCommand(String[] args) {
        if (args.length == 2) {
            return getOnlinePlayerNames();
        }
        return new ArrayList<>();
    }
    
    private List<String> completeWorldCommand(String[] args) {
        if (args.length == 2) {
            return Bukkit.getWorlds().stream()
                .map(World::getName)
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    
    private List<String> getTeamNames() {
        return plugin.getTeamManager().getAllTeams().stream()
            .map(Team::getName)
            .collect(Collectors.toList());
    }
    
    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .collect(Collectors.toList());
    }
}
