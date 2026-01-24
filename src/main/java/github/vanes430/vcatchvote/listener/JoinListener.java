package github.vanes430.vcatchvote.listener;

import github.vanes430.vcatchvote.VCatchVote;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinListener implements Listener {

    private final VCatchVote plugin;

    public JoinListener(VCatchVote plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Preload database data asynchronously
        plugin.getServer().getAsyncScheduler().runNow(plugin, task -> {
             plugin.getDatabaseManager().getLastVote(event.getPlayer());
             plugin.getDatabaseManager().getVotes(event.getPlayer());
        });

        if (!plugin.getConfig().getBoolean("waiting.enabled")) return;

        long delay = plugin.getConfig().getLong("waiting.join-delay-ticks", 60);

        // Folia/Paper: Use Entity Scheduler for player-specific tasks
        event.getPlayer().getScheduler().runDelayed(plugin, (task) -> {
            if (event.getPlayer().isOnline()) {
                plugin.getWaitingManager().processVotes(event.getPlayer());
            }
        }, null, delay);
    }
}