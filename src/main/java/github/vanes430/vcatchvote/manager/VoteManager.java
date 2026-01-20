package github.vanes430.vcatchvote.manager;

import github.vanes430.vcatchvote.VCatchVote;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class VoteManager {

    private final VCatchVote plugin;
    private int voteTarget;

    public VoteManager(VCatchVote plugin) {
        this.plugin = plugin;
        loadVotes();
    }

    public void loadVotes() {
        this.voteTarget = plugin.getConfig().getInt("vote-party.target", 50);
    }

    public void handleVote(String username, String service) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(username);
        if (plugin.getConfig().getBoolean("waiting.enabled") && !player.isOnline()) {
            plugin.getWaitingManager().addVote(username, service);
            if (plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().info("[LOG] Player " + username + " is offline. Added to waiting list.");
            }
            return;
        }

        if (plugin.getConfig().getBoolean("debug")) {
            plugin.getLogger().info("[LOG] Vote: " + username + " | Service: " + service);
        }

        plugin.getDataManager().incrementVotes();
        int currentVotes = plugin.getDataManager().getCurrentVotes();

        if (player.hasPlayedBefore() || player.isOnline()) {
             plugin.getDatabaseManager().addVote(player.getName(), player.getUniqueId());
        }

        // Normal Rewards
        if (plugin.getConfig().getBoolean("normal-rewards.enabled")) {
            GlobalRegionScheduler scheduler = plugin.getServer().getGlobalRegionScheduler();
            scheduler.run(plugin, task -> {
                List<String> commands = plugin.getConfig().getStringList("normal-rewards.commands");
                for (String cmd : commands) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", username));
                }
            });
        }

        String msg = plugin.getConfig().getString("messages.vote-received", "")
                .replace("%player%", username)
                .replace("%service%", service)
                .replace("%current%", String.valueOf(currentVotes))
                .replace("%target%", String.valueOf(voteTarget));

        plugin.getMessageUtils().broadcast(msg);

        if (currentVotes >= voteTarget) {
            startVoteParty();
            plugin.getDataManager().setCurrentVotes(0);
        }
    }

    private void startVoteParty() {
        if (!plugin.getConfig().getBoolean("vote-party.enabled")) return;

        plugin.getMessageUtils().broadcast(plugin.getConfig().getString("messages.vote-party-reached", ""));

        GlobalRegionScheduler scheduler = plugin.getServer().getGlobalRegionScheduler();

        // Global rewards using GlobalRegionScheduler
        if (plugin.getConfig().getBoolean("vote-party.rewards.global.enabled")) {
            scheduler.run(plugin, task -> {
                List<String> commands = plugin.getConfig().getStringList("vote-party.rewards.global.commands");
                for (String cmd : commands) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            });
        }

        // Per-player rewards with delay
        if (plugin.getConfig().getBoolean("vote-party.rewards.per-player.enabled")) {
            List<String> commands = plugin.getConfig().getStringList("vote-party.rewards.per-player.commands");
            long delay = plugin.getConfig().getLong("vote-party.rewards.per-player.delay-ticks", 10);
            
            List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());

            if (delay <= 0) {
                scheduler.run(plugin, task -> {
                    for (Player player : players) {
                        giveRewards(player, commands);
                    }
                });
            } else {
                scheduler.runAtFixedRate(plugin, task -> {
                    if (players.isEmpty()) {
                        task.cancel();
                        return;
                    }
                    
                    Player player = players.remove(0);
                    if (player.isOnline()) {
                        giveRewards(player, commands);
                    }
                    
                }, 1L, delay);
            }
        }
    }

    private void giveRewards(Player player, List<String> commands) {
        for (String cmd : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
        }
    }

    public int getCurrentVotes() {
        return plugin.getDataManager().getCurrentVotes();
    }

    public int getVoteTarget() {
        return voteTarget;
    }
}
