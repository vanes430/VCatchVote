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