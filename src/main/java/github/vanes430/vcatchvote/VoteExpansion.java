package github.vanes430.vcatchvote;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class VoteExpansion extends PlaceholderExpansion {

    private final VCatchVote plugin;

    public VoteExpansion(VCatchVote plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "vcatchvote";
    }

    @Override
    public @NotNull String getAuthor() {
        return "vanes430";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("current")) {
            return String.valueOf(plugin.getVoteManager().getCurrentVotes());
        }

        if (params.equalsIgnoreCase("target")) {
            return String.valueOf(plugin.getVoteManager().getVoteTarget());
        }

        if (params.equalsIgnoreCase("needed")) {
            return String.valueOf(plugin.getVoteManager().getVoteTarget() - plugin.getVoteManager().getCurrentVotes());
        }

        // %vcatchvote_player_votes%
        if (params.equalsIgnoreCase("player_votes")) {
            if (player == null) return "0";
            return String.valueOf(plugin.getDatabaseManager().getVotes(player));
        }

        // %vcatchvote_votes_weekly%
        if (params.equalsIgnoreCase("votes_weekly")) {
            if (player == null) return "0";
            return String.valueOf(plugin.getDatabaseManager().getWeeklyVotes(player));
        }

        // %vcatchvote_votes_monthly%
        if (params.equalsIgnoreCase("votes_monthly")) {
            if (player == null) return "0";
            return String.valueOf(plugin.getDatabaseManager().getMonthlyVotes(player));
        }
        
        // %vcatchvote_streak%
        if (params.equalsIgnoreCase("streak")) {
            if (player == null) return "0";
            return String.valueOf(plugin.getDatabaseManager().getStreak(player));
        }

        // %vcatchvote_player_votes_<name>%
        if (params.startsWith("player_votes_")) {
            String targetName = params.substring("player_votes_".length());
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            return String.valueOf(plugin.getDatabaseManager().getVotes(target));
        }

        return null;
    }
}
