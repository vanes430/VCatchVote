package github.vanes430.vcatchvote.utils;

import github.vanes430.vcatchvote.VCatchVote;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class MessageUtils {

    private final VCatchVote plugin;
    private final MiniMessage miniMessage;

    public MessageUtils(VCatchVote plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public void broadcast(String message) {
        if (message == null || message.isEmpty()) return;
        Component component = parse(message);
        Bukkit.broadcast(component);
    }

    public void send(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(parse(message));
    }

    public Component parse(String message) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String fullMessage = prefix + message;
        
        // Support both legacy and MiniMessage
        if (fullMessage.contains("&") || fullMessage.contains("§")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(fullMessage);
        }
        return miniMessage.deserialize(fullMessage);
    }

    public Component parseWithoutPrefix(String message) {
         if (message.contains("&") || message.contains("§")) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
        }
        return miniMessage.deserialize(message);
    }
}
