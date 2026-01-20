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

        // %vcatchvote_player_votes% - The player viewing the placeholder
        if (params.equalsIgnoreCase("player_votes")) {
            if (player == null) return "0";
            return String.valueOf(plugin.getDatabaseManager().getVotes(player));
        }

        // %vcatchvote_player_votes_<name>% - Specific player votes
        if (params.startsWith("player_votes_")) {
            String targetName = params.substring("player_votes_".length());
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            // Warning: This could be slow if not cached and DB is large, but our manager caches hits
            return String.valueOf(plugin.getDatabaseManager().getVotes(target));
        }

        return null;
    }
}
