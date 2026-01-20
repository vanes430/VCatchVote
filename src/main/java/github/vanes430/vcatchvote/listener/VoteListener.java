package github.vanes430.vcatchvote.listener;

import com.vexsoftware.votifier.model.Vote;
import com.vexsoftware.votifier.model.VotifierEvent;
import github.vanes430.vcatchvote.VCatchVote;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class VoteListener implements Listener {

    private final VCatchVote plugin;

    public VoteListener(VCatchVote plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onVotifierEvent(VotifierEvent event) {
        Vote vote = event.getVote();
        plugin.getVoteManager().handleVote(vote.getUsername(), vote.getServiceName());
    }
}
