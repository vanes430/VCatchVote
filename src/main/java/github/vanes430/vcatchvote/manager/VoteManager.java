package github.vanes430.vcatchvote.manager;

import github.vanes430.vcatchvote.VCatchVote;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(username);
        
        // 1. Data Processing
        plugin.getDataManager().incrementVotes();
        int currentVotes = plugin.getDataManager().getCurrentVotes();

        if (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline()) {
             plugin.getDatabaseManager().addVote(offlinePlayer.getName(), offlinePlayer.getUniqueId());
        }

        // 2. Broadcast to everyone
        if (plugin.getConfig().getBoolean("messages.broadcast.enabled")) {
            String broadcastMsg = plugin.getConfig().getString("messages.broadcast.message", "")
                    .replace("%player%", username)
                    .replace("%service%", service)
                    .replace("%current%", String.valueOf(currentVotes))
                    .replace("%target%", String.valueOf(voteTarget));
            plugin.getMessageUtils().broadcast(broadcastMsg);
        }

        // 3. Check Waiting System for offline players (Rewards)
        if (plugin.getConfig().getBoolean("waiting.enabled") && !offlinePlayer.isOnline()) {
            plugin.getWaitingManager().addVote(username, service);
            if (plugin.getConfig().getBoolean("debug")) {
                plugin.getLogger().info("[LOG] Player " + username + " is offline. Added to waiting list.");
            }
        } else {
            // Normal Rewards (Only if online or processed from waiting)
            executeNormalRewards(username);
            
            // Private message to the online player
            if (plugin.getConfig().getBoolean("messages.private.enabled") && offlinePlayer.isOnline()) {
                Player onlinePlayer = offlinePlayer.getPlayer();
                if (onlinePlayer != null) {
                    String privateMsg = plugin.getConfig().getString("messages.private.message", "")
                            .replace("%player%", username)
                            .replace("%service%", service)
                            .replace("%current%", String.valueOf(currentVotes))
                            .replace("%target%", String.valueOf(voteTarget));
                    plugin.getMessageUtils().send(onlinePlayer, privateMsg);
                }
            }
        }

        // Discord Webhook
        if (plugin.getConfig().getBoolean("discord-webhook.enabled")) {
            String webhookMsg = plugin.getConfig().getString("discord-webhook.messages.vote", "")
                    .replace("%player%", username)
                    .replace("%service%", service)
                    .replace("%votes%", String.valueOf(plugin.getDatabaseManager().getVotes(offlinePlayer)));
            plugin.getDiscordWebhook().send(webhookMsg);
        }

        if (currentVotes >= voteTarget) {
            startVoteParty();
            plugin.getDataManager().setCurrentVotes(0);
        }
        
        checkStreakRewards(offlinePlayer);
    }

    private void executeNormalRewards(String username) {
        if (!plugin.getConfig().getBoolean("normal-rewards.enabled")) return;
        
        GlobalRegionScheduler scheduler = plugin.getServer().getGlobalRegionScheduler();
        scheduler.run(plugin, task -> {
            List<Map<?, ?>> rewards = plugin.getConfig().getMapList("normal-rewards.rewards");
            
            // Legacy Support
            if (rewards.isEmpty()) {
                List<String> oldCommands = plugin.getConfig().getStringList("normal-rewards.commands");
                for (String cmd : oldCommands) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", username));
                }
            }

            for (Map<?, ?> reward : rewards) {
                double chance = 0.0;
                if (reward.get("chance") instanceof Number) {
                    chance = ((Number) reward.get("chance")).doubleValue();
                }

                if (ThreadLocalRandom.current().nextDouble(100.0) < chance) {
                    Object cmdsObj = reward.get("commands");
                    if (cmdsObj instanceof List) {
                        for (Object cmdObj : (List<?>) cmdsObj) {
                            String cmd = String.valueOf(cmdObj).replace("%player%", username);
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                        }
                    }
                }
            }
        });
    }

    private void checkStreakRewards(OfflinePlayer player) {
        if (!plugin.getConfig().getBoolean("vote-streak.enabled")) return;
        
        // Use cached streak value which was just updated in addVote
        int streak = plugin.getDatabaseManager().getStreak(player);
        List<String> commands = plugin.getConfig().getStringList("vote-streak.rewards." + streak);
        
        if (commands != null && !commands.isEmpty()) {
            GlobalRegionScheduler scheduler = plugin.getServer().getGlobalRegionScheduler();
            scheduler.run(plugin, task -> {
               for (String cmd : commands) {
                   Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%player%", player.getName()));
               }
            });
        }
    }

    private void startVoteParty() {
        if (!plugin.getConfig().getBoolean("vote-party.enabled")) return;

        plugin.getMessageUtils().broadcast(plugin.getConfig().getString("messages.vote-party-reached", ""));
        
        // Discord Webhook Party Start
        if (plugin.getConfig().getBoolean("discord-webhook.enabled")) {
             plugin.getDiscordWebhook().send(plugin.getConfig().getString("discord-webhook.messages.party-start", ""));
        }

        GlobalRegionScheduler scheduler = plugin.getServer().getGlobalRegionScheduler();

        // Global rewards
        if (plugin.getConfig().getBoolean("vote-party.rewards.global.enabled")) {
            scheduler.run(plugin, task -> {
                List<String> commands = plugin.getConfig().getStringList("vote-party.rewards.global.commands");
                for (String cmd : commands) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            });
        }

        // Per-player rewards
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