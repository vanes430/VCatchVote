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
    private final ConcurrentHashMap<UUID, Integer> weeklyVoteCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> monthlyVoteCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> streakCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastVoteCache = new ConcurrentHashMap<>();

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
            checkColumns();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS player_votes (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(16), " +
                "votes INT DEFAULT 0, " +
                "votes_weekly INT DEFAULT 0, " +
                "votes_monthly INT DEFAULT 0, " +
                "streak INT DEFAULT 0, " +
                "last_vote BIGINT DEFAULT 0" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create table: " + e.getMessage());
        }
    }

    private void checkColumns() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(player_votes)")) {
            boolean hasLastVote = false;
            boolean hasWeekly = false;
            boolean hasMonthly = false;
            boolean hasStreak = false;

            while (rs.next()) {
                String colName = rs.getString("name");
                if (colName.equalsIgnoreCase("last_vote")) hasLastVote = true;
                if (colName.equalsIgnoreCase("votes_weekly")) hasWeekly = true;
                if (colName.equalsIgnoreCase("votes_monthly")) hasMonthly = true;
                if (colName.equalsIgnoreCase("streak")) hasStreak = true;
            }
            
            if (!hasLastVote) {
                stmt.execute("ALTER TABLE player_votes ADD COLUMN last_vote BIGINT DEFAULT 0");
            }
            if (!hasWeekly) {
                stmt.execute("ALTER TABLE player_votes ADD COLUMN votes_weekly INT DEFAULT 0");
            }
            if (!hasMonthly) {
                stmt.execute("ALTER TABLE player_votes ADD COLUMN votes_monthly INT DEFAULT 0");
            }
            if (!hasStreak) {
                stmt.execute("ALTER TABLE player_votes ADD COLUMN streak INT DEFAULT 0");
                plugin.getLogger().info("Added missing column 'streak' to database.");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to check/update columns: " + e.getMessage());
        }
    }

    public void addVote(String name, UUID uuid) {
        // Calculate Streak BEFORE updating timestamps
        long now = System.currentTimeMillis();
        long lastVoteTime = getLastVote(Bukkit.getOfflinePlayer(uuid)); // Ensures loaded from DB if not in cache
        int currentStreak = getStreak(Bukkit.getOfflinePlayer(uuid));
        
        // Milliseconds in a day = 86400000
        long diff = now - lastVoteTime;
        int newStreak = currentStreak;

        if (lastVoteTime == 0) {
            newStreak = 1; // First vote ever
        } else if (diff > 86400000L * 2) {
            newStreak = 1; // Missed a day (over 48h), reset
        } else if (diff > 86400000L) {
            newStreak++; // Voted next day (between 24h and 48h), increment
        } else {
            // Voted same day (< 24h), keep streak (unless it was 0/1 logic, but assume keep)
            if (newStreak == 0) newStreak = 1;
        }

        voteCache.merge(uuid, 1, Integer::sum);
        weeklyVoteCache.merge(uuid, 1, Integer::sum);
        monthlyVoteCache.merge(uuid, 1, Integer::sum);
        streakCache.put(uuid, newStreak);
        lastVoteCache.put(uuid, now);
        
        final int finalStreak = newStreak;

        AsyncScheduler scheduler = plugin.getServer().getAsyncScheduler();
        scheduler.runNow(plugin, (task) -> {
            synchronized (connection) {
                String sql = "INSERT INTO player_votes (uuid, name, votes, votes_weekly, votes_monthly, streak, last_vote) VALUES(?, ?, 1, 1, 1, 1, ?) " +
                        "ON CONFLICT(uuid) DO UPDATE SET votes = votes + 1, votes_weekly = votes_weekly + 1, votes_monthly = votes_monthly + 1, streak = ?, name = ?, last_vote = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.setString(2, name);
                    pstmt.setLong(3, now);
                    pstmt.setInt(4, finalStreak);
                    pstmt.setString(5, name);
                    pstmt.setLong(6, now);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().severe("Could not add vote: " + e.getMessage());
                }
            }
        });
    }

    public void setVotes(OfflinePlayer player, int amount) {
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        
        voteCache.put(uuid, amount);

        AsyncScheduler scheduler = plugin.getServer().getAsyncScheduler();
        scheduler.runNow(plugin, (task) -> {
            synchronized (connection) {
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
            }
        });
    }

    public void resetVotes(OfflinePlayer player) {
        setVotes(player, 0);
    }
    
    public void resetWeeklyVotes() {
        weeklyVoteCache.clear();
        AsyncScheduler scheduler = plugin.getServer().getAsyncScheduler();
        scheduler.runNow(plugin, (task) -> {
             synchronized (connection) {
                 try (Statement stmt = connection.createStatement()) {
                     stmt.executeUpdate("UPDATE player_votes SET votes_weekly = 0");
                 } catch (SQLException e) {
                     plugin.getLogger().severe("Could not reset weekly votes: " + e.getMessage());
                 }
             }
        });
    }

    public void resetMonthlyVotes() {
        monthlyVoteCache.clear();
        AsyncScheduler scheduler = plugin.getServer().getAsyncScheduler();
        scheduler.runNow(plugin, (task) -> {
             synchronized (connection) {
                 try (Statement stmt = connection.createStatement()) {
                     stmt.executeUpdate("UPDATE player_votes SET votes_monthly = 0");
                 } catch (SQLException e) {
                     plugin.getLogger().severe("Could not reset monthly votes: " + e.getMessage());
                 }
             }
        });
    }

    public int getVotes(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        if (voteCache.containsKey(uuid)) return voteCache.get(uuid);
        loadData(uuid);
        return voteCache.getOrDefault(uuid, 0);
    }
    
    public int getWeeklyVotes(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        if (weeklyVoteCache.containsKey(uuid)) return weeklyVoteCache.get(uuid);
        loadData(uuid);
        return weeklyVoteCache.getOrDefault(uuid, 0);
    }

    public int getMonthlyVotes(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        if (monthlyVoteCache.containsKey(uuid)) return monthlyVoteCache.get(uuid);
        loadData(uuid);
        return monthlyVoteCache.getOrDefault(uuid, 0);
    }
    
    public int getStreak(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        if (streakCache.containsKey(uuid)) return streakCache.get(uuid);
        loadData(uuid);
        return streakCache.getOrDefault(uuid, 0);
    }

    public long getLastVote(OfflinePlayer player) {
        UUID uuid = player.getUniqueId();
        if (lastVoteCache.containsKey(uuid)) return lastVoteCache.get(uuid);
        loadData(uuid);
        return lastVoteCache.getOrDefault(uuid, 0L);
    }

    private void loadData(UUID uuid) {
        synchronized (connection) {
            String sql = "SELECT votes, votes_weekly, votes_monthly, streak, last_vote FROM player_votes WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    voteCache.put(uuid, rs.getInt("votes"));
                    weeklyVoteCache.put(uuid, rs.getInt("votes_weekly"));
                    monthlyVoteCache.put(uuid, rs.getInt("votes_monthly"));
                    streakCache.put(uuid, rs.getInt("streak"));
                    lastVoteCache.put(uuid, rs.getLong("last_vote"));
                } else {
                    voteCache.put(uuid, 0);
                    weeklyVoteCache.put(uuid, 0);
                    monthlyVoteCache.put(uuid, 0);
                    streakCache.put(uuid, 0);
                    lastVoteCache.put(uuid, 0L);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
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
