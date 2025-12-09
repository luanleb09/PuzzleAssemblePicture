package com.example.puzzle_assemble_picture;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DailyRewardManager {
    private static final String TAG = "DailyRewardManager";
    private static final String PREFS_NAME = "DailyRewards";

    // Keys
    private static final String KEY_LAST_CHECK_IN_DATE = "last_check_in_date";
    private static final String KEY_CHECK_IN_STREAK = "check_in_streak";
    private static final String KEY_TOTAL_CHECK_INS = "total_check_ins";
    private static final String KEY_AUTO_SOLVE_COUNT = "auto_solve_count";
    private static final String KEY_AUTO_SOLVE_RESET_DATE = "auto_solve_reset_date";
    private static final String KEY_SHUFFLE_COUNT = "shuffle_count";
    private static final String KEY_SHUFFLE_RESET_DATE = "shuffle_reset_date";

    // Limits
    public static final int MAX_AUTO_SOLVE_PER_DAY = 3;
    public static final int MAX_SHUFFLE_PER_DAY = 5;

    // Check-in rewards
    private static final int[] CHECK_IN_REWARDS = {10, 15, 20, 25, 30, 40, 50}; // Coins for day 1-7

    private SharedPreferences prefs;
    private SimpleDateFormat dateFormat;

    public DailyRewardManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

        // Auto reset if new day
        checkAndResetDaily();
    }

    /**
     * Get today's date string
     */
    private String getTodayDateString() {
        return dateFormat.format(new Date());
    }

    /**
     * Check if it's a new day and reset counters
     */
    private void checkAndResetDaily() {
        String today = getTodayDateString();
        String lastResetDate = prefs.getString(KEY_AUTO_SOLVE_RESET_DATE, "");

        if (!today.equals(lastResetDate)) {
            // New day - reset counters
            resetDailyCounters();
            Log.d(TAG, "✅ Daily counters reset for new day: " + today);
        }
    }

    /**
     * Reset daily counters
     */
    private void resetDailyCounters() {
        String today = getTodayDateString();
        SharedPreferences.Editor editor = prefs.edit();

        editor.putInt(KEY_AUTO_SOLVE_COUNT, 0);
        editor.putString(KEY_AUTO_SOLVE_RESET_DATE, today);

        editor.putInt(KEY_SHUFFLE_COUNT, 0);
        editor.putString(KEY_SHUFFLE_RESET_DATE, today);

        editor.apply();
    }

    // ===== AUTO-SOLVE METHODS =====

    /**
     * Get remaining auto-solve count for today
     */
    public int getRemainingAutoSolveCount() {
        checkAndResetDaily();
        int used = prefs.getInt(KEY_AUTO_SOLVE_COUNT, 0);
        return Math.max(0, MAX_AUTO_SOLVE_PER_DAY - used);
    }

    /**
     * Use one auto-solve
     */
    public boolean useAutoSolve() {
        checkAndResetDaily();

        int used = prefs.getInt(KEY_AUTO_SOLVE_COUNT, 0);
        if (used >= MAX_AUTO_SOLVE_PER_DAY) {
            Log.d(TAG, "❌ Auto-solve limit reached for today");
            return false;
        }

        prefs.edit().putInt(KEY_AUTO_SOLVE_COUNT, used + 1).apply();
        Log.d(TAG, "✅ Auto-solve used. Remaining: " + getRemainingAutoSolveCount());
        return true;
    }

    /**
     * Check if can use auto-solve
     */
    public boolean canUseAutoSolve() {
        return getRemainingAutoSolveCount() > 0;
    }

    // ===== SHUFFLE METHODS =====

    /**
     * Get remaining shuffle count for today
     */
    public int getRemainingShuffleCount() {
        checkAndResetDaily();
        int used = prefs.getInt(KEY_SHUFFLE_COUNT, 0);
        return Math.max(0, MAX_SHUFFLE_PER_DAY - used);
    }

    /**
     * Use one shuffle
     */
    public boolean useShuffle() {
        checkAndResetDaily();

        int used = prefs.getInt(KEY_SHUFFLE_COUNT, 0);
        if (used >= MAX_SHUFFLE_PER_DAY) {
            Log.d(TAG, "❌ Shuffle limit reached for today");
            return false;
        }

        prefs.edit().putInt(KEY_SHUFFLE_COUNT, used + 1).apply();
        Log.d(TAG, "✅ Shuffle used. Remaining: " + getRemainingShuffleCount());
        return true;
    }

    /**
     * Check if can use shuffle
     */
    public boolean canUseShuffle() {
        return getRemainingShuffleCount() > 0;
    }

    // ===== CHECK-IN METHODS =====

    /**
     * Check if user can check in today
     */
    public boolean canCheckInToday() {
        String today = getTodayDateString();
        String lastCheckIn = prefs.getString(KEY_LAST_CHECK_IN_DATE, "");
        return !today.equals(lastCheckIn);
    }

    /**
     * Perform daily check-in
     * Returns reward amount or -1 if already checked in
     */
    public int performCheckIn() {
        if (!canCheckInToday()) {
            Log.d(TAG, "❌ Already checked in today");
            return -1;
        }

        String today = getTodayDateString();
        String lastCheckIn = prefs.getString(KEY_LAST_CHECK_IN_DATE, "");

        int currentStreak = prefs.getInt(KEY_CHECK_IN_STREAK, 0);
        int totalCheckIns = prefs.getInt(KEY_TOTAL_CHECK_INS, 0);

        // Check if streak continues or breaks
        if (isConsecutiveDay(lastCheckIn, today)) {
            currentStreak++;
        } else {
            currentStreak = 1; // Reset streak
        }

        // Cap streak at 7 days (weekly cycle)
        if (currentStreak > 7) {
            currentStreak = 1;
        }

        totalCheckIns++;

        // Get reward for current day
        int reward = CHECK_IN_REWARDS[currentStreak - 1];

        // Save data
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LAST_CHECK_IN_DATE, today);
        editor.putInt(KEY_CHECK_IN_STREAK, currentStreak);
        editor.putInt(KEY_TOTAL_CHECK_INS, totalCheckIns);
        editor.apply();

        Log.d(TAG, "✅ Check-in successful! Day " + currentStreak + ", Reward: " + reward);
        return reward;
    }

    /**
     * Check if two dates are consecutive days
     */
    private boolean isConsecutiveDay(String lastDate, String currentDate) {
        if (lastDate.isEmpty()) return false;

        try {
            Date last = dateFormat.parse(lastDate);
            Date current = dateFormat.parse(currentDate);

            if (last == null || current == null) return false;

            Calendar lastCal = Calendar.getInstance();
            lastCal.setTime(last);
            lastCal.add(Calendar.DAY_OF_YEAR, 1);

            Calendar currentCal = Calendar.getInstance();
            currentCal.setTime(current);

            return lastCal.get(Calendar.YEAR) == currentCal.get(Calendar.YEAR) &&
                    lastCal.get(Calendar.DAY_OF_YEAR) == currentCal.get(Calendar.DAY_OF_YEAR);

        } catch (Exception e) {
            Log.e(TAG, "Error parsing dates", e);
            return false;
        }
    }

    /**
     * Get current check-in streak
     */
    public int getCheckInStreak() {
        String today = getTodayDateString();
        String lastCheckIn = prefs.getString(KEY_LAST_CHECK_IN_DATE, "");

        if (lastCheckIn.isEmpty()) return 0;

        // If today's check-in, return current streak
        if (today.equals(lastCheckIn)) {
            return prefs.getInt(KEY_CHECK_IN_STREAK, 0);
        }

        // If yesterday's check-in, streak is still active
        if (isConsecutiveDay(lastCheckIn, today)) {
            return prefs.getInt(KEY_CHECK_IN_STREAK, 0);
        }

        // Streak broken
        return 0;
    }

    /**
     * Get total check-ins
     */
    public int getTotalCheckIns() {
        return prefs.getInt(KEY_TOTAL_CHECK_INS, 0);
    }

    /**
     * Get reward for specific day
     */
    public int getRewardForDay(int day) {
        if (day < 1 || day > 7) return 0;
        return CHECK_IN_REWARDS[day - 1];
    }

    /**
     * Get last check-in date
     */
    public String getLastCheckInDate() {
        return prefs.getString(KEY_LAST_CHECK_IN_DATE, "Not yet");
    }

    /**
     * Debug: Reset everything
     */
    public void resetAll() {
        prefs.edit().clear().apply();
        Log.d(TAG, "🔄 All daily rewards data reset");
    }
}