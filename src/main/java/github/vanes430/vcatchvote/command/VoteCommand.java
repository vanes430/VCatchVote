package github.vanes430.vcatchvote.command;

import github.vanes430.vcatchvote.VCatchVote;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class VoteCommand implements CommandExecutor {

    private final VCatchVote plugin;

    public VoteCommand(VCatchVote plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        sendVoteLinks(sender);
        return true;
    }

    private void sendVoteLinks(CommandSender sender) {
        sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix(plugin.getConfig().getString("messages.vote-list-header", "")));
        
        List<?> links = plugin.getConfig().getList("vote-links");
        String format = plugin.getConfig().getString("messages.vote-list-format", "");
        
        if (links != null) {
            for (Object obj : links) {
                if (obj instanceof Map) {
                    Map<?, ?> link = (Map<?, ?>) obj;
                    String name = String.valueOf(link.get("name"));
                    String url = String.valueOf(link.get("url"));
                    
                    String formatted = format.replace("%name%", name).replace("%url%", url);
                    sender.sendMessage(plugin.getMessageUtils().parseWithoutPrefix(formatted));
                }
            }
        }
    }
}
