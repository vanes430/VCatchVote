package github.vanes430.vcatchvote.task;

import github.vanes430.vcatchvote.VCatchVote;

import java.util.Calendar;
import java.util.TimeZone;

public class VoteResetTask implements Runnable {

    private final VCatchVote plugin;

    public VoteResetTask(VCatchVote plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        checkWeeklyReset();
        checkMonthlyReset();
    }

    private void checkWeeklyReset() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If today is Monday, but we are before the reset time (impossible if set to 00:00:00 but safety check),
        // or if today is Sunday, we might need to look at "this week's Monday".
        // Actually, just getting "Start of this week" is enough.
        // Calendar.DAY_OF_WEEK is locale dependent, ensuring Monday is start:
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        
        // Reset to start of current week
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        
        long startOfWeek = calendar.getTimeInMillis();
        long lastReset = plugin.getDataManager().getLastWeeklyReset();

        // If the last reset was BEFORE the start of this week, we need to reset.
        if (lastReset < startOfWeek) {
            plugin.getLogger().info("Performing Weekly Vote Reset...");
            plugin.getDatabaseManager().resetWeeklyVotes();
            plugin.getDataManager().setLastWeeklyReset(System.currentTimeMillis());
            plugin.getLogger().info("Weekly votes have been reset.");
        }
    }

    private void checkMonthlyReset() {
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long startOfMonth = calendar.getTimeInMillis();
        long lastReset = plugin.getDataManager().getLastMonthlyReset();

        if (lastReset < startOfMonth) {
            plugin.getLogger().info("Performing Monthly Vote Reset...");
            plugin.getDatabaseManager().resetMonthlyVotes();
            plugin.getDataManager().setLastMonthlyReset(System.currentTimeMillis());
            plugin.getLogger().info("Monthly votes have been reset.");
        }
    }
}
