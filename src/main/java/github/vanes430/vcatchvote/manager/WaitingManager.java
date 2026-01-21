package github.vanes430.vcatchvote.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import github.vanes430.vcatchvote.VCatchVote;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class WaitingManager {

    private final VCatchVote plugin;
    private final Map<String, List<WaitingVote>> waitingVotes = new ConcurrentHashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final File file;

    public WaitingManager(VCatchVote plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "waiting.json");
        
        load();
        startAutoClear();
    }

    public void addVote(String username, String service) {
        waitingVotes.computeIfAbsent(username.toLowerCase(), k -> new ArrayList<>())
                .add(new WaitingVote(username, service));
        
        if (isJsonMode()) {
            save();
        }
    }

    public void processVotes(Player player) {
        String username = player.getName().toLowerCase();
        if (!waitingVotes.containsKey(username)) return;

        List<WaitingVote> votes = waitingVotes.remove(username);
        
        if (votes != null && !votes.isEmpty()) {
            plugin.getLogger().info("Processing " + votes.size() + " waiting votes for " + player.getName());
            
            for (WaitingVote vote : votes) {
                plugin.getVoteManager().handleVote(player.getName(), vote.getService());
            }
        }

        if (isJsonMode()) {
            save();
        }
    }

    public int getWaitingCount() {
        return waitingVotes.values().stream().mapToInt(List::size).sum();
    }

    public void clear() {
        waitingVotes.clear();
        if (isJsonMode()) {
            save();
        }
    }

    private void startAutoClear() {
        int minutes = plugin.getConfig().getInt("waiting.auto-clear-minutes", 60);
        if (minutes <= 0) return;

        plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, (task) -> {
            if (getWaitingCount() > 0) {
                plugin.getLogger().info("Auto-clearing waiting votes...");
                clear();
            }
        }, minutes, minutes, TimeUnit.MINUTES);
    }

    private boolean isJsonMode() {
        return plugin.getConfig().getString("waiting.storage", "JSON").equalsIgnoreCase("JSON");
    }

    private void load() {
        if (!isJsonMode() || !file.exists()) return;

        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<Map<String, List<WaitingVote>>>() {}.getType();
            Map<String, List<WaitingVote>> data = gson.fromJson(reader, type);
            if (data != null) {
                waitingVotes.putAll(data);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not load waiting.json: " + e.getMessage());
        }
    }

    public void save() {
        if (!isJsonMode()) return;

        plugin.getServer().getAsyncScheduler().runNow(plugin, (task) -> {
            saveSync();
        });
    }

    public void saveSync() {
        if (!isJsonMode()) return;
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(waitingVotes, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save waiting.json: " + e.getMessage());
        }
    }

    public Map<String, List<WaitingVote>> getWaitingVotes() {
        return waitingVotes;
    }
}
