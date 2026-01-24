package github.vanes430.vcatchvote;

import github.vanes430.vcatchvote.command.MainCommand;
import github.vanes430.vcatchvote.command.VoteCommand;
import github.vanes430.vcatchvote.listener.JoinListener;
import github.vanes430.vcatchvote.listener.VoteListener;
import github.vanes430.vcatchvote.manager.DataManager;
import github.vanes430.vcatchvote.manager.DatabaseManager;
import github.vanes430.vcatchvote.manager.VoteManager;
import github.vanes430.vcatchvote.manager.WaitingManager;
import github.vanes430.vcatchvote.task.VoteReminderTask;
import github.vanes430.vcatchvote.task.VoteResetTask;
import github.vanes430.vcatchvote.utils.DiscordWebhook;
import github.vanes430.vcatchvote.utils.MessageUtils;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;

public class VCatchVote extends JavaPlugin {

    private VoteManager voteManager;
    private DatabaseManager databaseManager;
    private WaitingManager waitingManager;
    private DataManager dataManager;
    private MessageUtils messageUtils;
    private DiscordWebhook discordWebhook;

    @Override
    public void onEnable() {
        if (!isSupported()) {
            getLogger().severe("=================================================");
            getLogger().severe("VCatchVote only supports Paper, Folia, or forks!");
            getLogger().severe("Plugin will be disabled.");
            getLogger().severe("=================================================");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        loadAndRepairConfig();
        
        this.messageUtils = new MessageUtils(this);
        this.databaseManager = new DatabaseManager(this);
        this.dataManager = new DataManager(this);
        this.waitingManager = new WaitingManager(this);
        this.discordWebhook = new DiscordWebhook(this);
        this.voteManager = new VoteManager(this);
        
        // Register Votifier Listener based on plugin name "Votifier"
        // This covers NuVotifier, VotifierPlus, and others that 'provide' Votifier
        if (getServer().getPluginManager().isPluginEnabled("Votifier") || 
            getServer().getPluginManager().isPluginEnabled("VotifierPlus")) {
            
            getServer().getPluginManager().registerEvents(new VoteListener(this), this);
            getLogger().info("Votifier hook initialized.");
        } else {
            getLogger().warning("Votifier plugin not found or not enabled. Plugin will run without vote catching.");
        }

        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        
        MainCommand mainCommand = new MainCommand(this);
        getCommand("vcatchvote").setExecutor(mainCommand);
        getCommand("vcatchvote").setTabCompleter(mainCommand);
        
        getCommand("vote").setExecutor(new VoteCommand(this));

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new VoteExpansion(this).register();
        }

        // Schedule Reminder Task (Asynchronously)
        long checkInterval = getConfig().getLong("vote-reminder.check-interval-minutes", 5) * 60 * 1000L;
        getServer().getAsyncScheduler().runAtFixedRate(this, task -> {
            new VoteReminderTask(this).run();
        }, 1L, checkInterval, TimeUnit.MILLISECONDS);

        // Schedule Vote Reset Task (Every 1 Hour)
        getServer().getAsyncScheduler().runAtFixedRate(this, task -> {
            new VoteResetTask(this).run();
        }, 1L, 1L, TimeUnit.HOURS);

        getLogger().info("VCatchVote has been enabled with Adventure & PlaceholderAPI support!");
    }

    private void loadAndRepairConfig() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    private boolean isSupported() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.destroystokyo.paper.PaperConfig");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

    @Override
    public void onDisable() {
        if (waitingManager != null) {
            waitingManager.saveSync();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        if (dataManager != null) {
            dataManager.saveSync();
        }
        getLogger().info("VCatchVote has been disabled!");
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public WaitingManager getWaitingManager() {
        return waitingManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public MessageUtils getMessageUtils() {
        return messageUtils;
    }

    public DiscordWebhook getDiscordWebhook() {
        return discordWebhook;
    }
}