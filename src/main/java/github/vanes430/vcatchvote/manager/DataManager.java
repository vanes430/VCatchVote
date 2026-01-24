package github.vanes430.vcatchvote.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import github.vanes430.vcatchvote.VCatchVote;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class DataManager {

    private final VCatchVote plugin;
    private final File file;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private Data data;

    public DataManager(VCatchVote plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.json");
        load();
    }

    private void load() {
        if (!file.exists()) {
            data = new Data();
            save();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            data = gson.fromJson(reader, Data.class);
            if (data == null) {
                data = new Data();
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Could not load data.json: " + e.getMessage());
            data = new Data();
        }
    }

    public void save() {
        AsyncScheduler scheduler = plugin.getServer().getAsyncScheduler();
        scheduler.runNow(plugin, task -> {
            saveSync();
        });
    }

    public void saveSync() {
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.json: " + e.getMessage());
        }
    }

    public int getCurrentVotes() {
        return data.currentVotes;
    }

    public void setCurrentVotes(int currentVotes) {
        this.data.currentVotes = currentVotes;
        save();
    }

    public void incrementVotes() {
        this.data.currentVotes++;
        save();
    }

    public long getLastWeeklyReset() {
        return data.lastWeeklyReset;
    }

    public void setLastWeeklyReset(long lastWeeklyReset) {
        this.data.lastWeeklyReset = lastWeeklyReset;
        save();
    }

    public long getLastMonthlyReset() {
        return data.lastMonthlyReset;
    }

    public void setLastMonthlyReset(long lastMonthlyReset) {
        this.data.lastMonthlyReset = lastMonthlyReset;
        save();
    }

    private static class Data {
        int currentVotes = 0;
        long lastWeeklyReset = 0;
        long lastMonthlyReset = 0;
    }
}
