package fr.clashdesecoles.bingo.commands;
import fr.clashdesecoles.bingo.BingoPlugin;
import fr.clashdesecoles.bingo.utils.MessageUtil;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public class BingoCommandRouter implements CommandExecutor, TabCompleter {
    private final BingoPlugin plugin;
    private final Map<String, CommandExecutor> subCommands;
    
    public BingoCommandRouter(BingoPlugin plugin) {
        this.plugin = plugin;
        this.subCommands = new HashMap<>();
        this.subCommands.put("team", new TeamCommand(plugin));
        this.subCommands.put("instance", new InstanceCommand(plugin));
        this.subCommands.put("roll", new RollCommand(plugin));
        this.subCommands.put("valid", new ValidCommand(plugin));
        this.subCommands.put("reject", new RejectCommand(plugin));
        this.subCommands.put("start", new StartCommand(plugin));
        this.subCommands.put("pause", new PauseCommand(plugin));
        this.subCommands.put("stop", new StopCommand(plugin));
        this.subCommands.put("show", new ShowCommand(plugin));
        this.subCommands.put("hide", new HideCommand(plugin));
        this.subCommands.put("tp", new TpCommand(plugin));
        this.subCommands.put("tphere", new TpHereCommand(plugin));
        this.subCommands.put("export", new ExportCommand(plugin));
        this.subCommands.put("say", new SayCommand(plugin));
        this.subCommands.put("reload", new ReloadCommand(plugin));
        this.subCommands.put("hub", new HubCommand(plugin));
        this.subCommands.put("world", new WorldCommand(plugin));
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Usage: /bingo <subcommand>");
            return true;
        }
        
        CommandExecutor executor = subCommands.get(args[0].toLowerCase());
        if (executor != null) {
            return executor.onCommand(sender, command, label, Arrays.copyOfRange(args, 1, args.length));
        }
        
        sender.sendMessage(MessageUtil.get("commands.unknown"));
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            list.addAll(subCommands.keySet());
        }
        return list.stream().filter(s -> s.startsWith(args[args.length - 1].toLowerCase()))
            .collect(java.util.stream.Collectors.toList());
    }
}
