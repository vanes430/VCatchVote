package github.vanes430.vcatchvote.command;

import github.vanes430.vcatchvote.VCatchVote;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final VCatchVote plugin;

    public MainCommand(VCatchVote plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix("<red>Usage: /vcatchvote <reload|fakevote|set|reset|waiting>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("vcatchvote.admin")) {
                sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix(plugin.getConfig().getString("messages.no-permission", "")));
                return true;
            }
            plugin.reloadConfig();
            plugin.getVoteManager().loadVotes();
            plugin.getMessageUtils().send(sender, plugin.getConfig().getString("messages.reload", ""));
            return true;
        }

        if (args[0].equalsIgnoreCase("fakevote")) {
            if (!sender.hasPermission("vcatchvote.admin")) {
                sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix(plugin.getConfig().getString("messages.no-permission", "")));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix("<red>Usage: /vcatchvote fakevote <player> <service>"));
                return true;
            }
            String target = args[1];
            String service = args[2];
            plugin.getVoteManager().handleVote(target, service);
            
            String msg = plugin.getConfig().getString("messages.fake-vote", "")
                    .replace("%player%", target)
                    .replace("%service%", service);
            plugin.getMessageUtils().send(sender, msg);
            return true;
        }

        // WAITING COMMAND
        if (args[0].equalsIgnoreCase("waiting")) {
             if (!sender.hasPermission("vcatchvote.admin")) {
                sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix(plugin.getConfig().getString("messages.no-permission", "")));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix("<red>Usage: /vcatchvote waiting <list|clear>"));
                return true;
            }
            if (args[1].equalsIgnoreCase("list")) {
                int count = plugin.getWaitingManager().getWaitingCount();
                sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix("<green>Total pending offline votes: <gold>" + count));
                return true;
            }
            if (args[1].equalsIgnoreCase("clear")) {
                plugin.getWaitingManager().clear();
                sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix("<green>Cleared all pending offline votes."));
                return true;
            }
        }

        // SET COMMAND
        if (args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("vcatchvote.admin")) {
                sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix(plugin.getConfig().getString("messages.no-permission", "")));
                return true;
            }
            if (args.length < 3) {
                sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix("<red>Usage: /vcatchvote set <player> <amount>"));
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix("<red>Invalid number."));
                return true;
            }

            plugin.getDatabaseManager().setVotes(target, amount);
            sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix("<green>Set votes for <yellow>" + target.getName() + "</yellow> to <gold>" + amount + "</gold>."));
            return true;
        }

        // RESET COMMAND
        if (args[0].equalsIgnoreCase("reset")) {
             if (!sender.hasPermission("vcatchvote.admin")) {
                sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix(plugin.getConfig().getString("messages.no-permission", "")));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix("<red>Usage: /vcatchvote reset <player>"));
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            plugin.getDatabaseManager().resetVotes(target);
            sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix("<green>Reset votes for <yellow>" + target.getName() + "</yellow> to 0."));
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "fakevote", "set", "reset", "waiting").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("waiting")) {
             return Arrays.asList("list", "clear").stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("fakevote") || args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("reset"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
