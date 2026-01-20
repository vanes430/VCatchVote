package github.vanes430.vcatchvote.manager;

import github.vanes430.vcatchvote.VCatchVote;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private final VCatchVote plugin;
    private Connection connection;
    private final ConcurrentHashMap<UUID, Integer> voteCache = new ConcurrentHashMap<>();

    public DatabaseManager(VCatchVote plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        File dataFolder = new File(plugin.getDataFolder(), "database.db");
        try {
            if (!dataFolder.exists()) {
                dataFolder.createNewFile();
            }
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
            createTable();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS player_votes (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(16), " +
                "votes INT DEFAULT 0" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create table: " + e.getMessage());
        }
    }

    public void addVote(String name, UUID uuid) {
        voteCache.merge(uuid, 1, Integer::sum);

        AsyncScheduler scheduler = plugin.getServer().getAsyncScheduler();
        scheduler.runNow(plugin, (task) -> {
            String sql = "INSERT INTO player_votes (uuid, name, votes) VALUES(?, ?, 1) " +
                    "ON CONFLICT(uuid) DO UPDATE SET votes = votes + 1, name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, name);
                pstmt.setString(3, name);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not add vote: " + e.getMessage());
            }
        });
    }

    public void setVotes(OfflinePlayer player, int amount) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        
        voteCache.put(uuid, amount);

        AsyncScheduler scheduler = plugin.getServer().getAsyncScheduler();
        scheduler.runNow(plugin, (task) -> {
            String sql = "INSERT INTO player_votes (uuid, name, votes) VALUES(?, ?, ?) " +
                    "ON CONFLICT(uuid) DO UPDATE SET votes = ?, name = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, name);
                pstmt.setInt(3, amount);
                pstmt.setInt(4, amount);
                pstmt.setString(5, name);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not set votes: " + e.getMessage());
            }
        });
    }

    public void resetVotes(OfflinePlayer player) {
        setVotes(player, 0);
    }

    public int getVotes(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        
        if (voteCache.containsKey(uuid)) {
            return voteCache.get(uuid);
        }

        String sql = "SELECT votes FROM player_votes WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int votes = rs.getInt("votes");
                voteCache.put(uuid, votes);
                return votes;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        voteCache.put(uuid, 0);
        return 0;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
