package github.vanes430.vcatchvote.task;

import github.vanes430.vcatchvote.VCatchVote;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class VoteReminderTask implements Runnable {

    private final VCatchVote plugin;

    public VoteReminderTask(VCatchVote plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("vote-reminder.enabled")) return;

        long intervalMillis = plugin.getConfig().getLong("vote-reminder.remind-every-minutes", 1440) * 60 * 1000L;
        long now = System.currentTimeMillis();

        for (Player player : Bukkit.getOnlinePlayers()) {
            // Check if player has permission to bypass
            if (player.hasPermission("vcatchvote.bypass.reminder")) continue;

            long lastVote = plugin.getDatabaseManager().getLastVote(player);

            if (now - lastVote > intervalMillis) {
                String msg = plugin.getConfig().getString("vote-reminder.message", "");
                if (!msg.isEmpty()) {
                    plugin.getMessageUtils().send(player, msg);
                }
            }
        }
    }
}
