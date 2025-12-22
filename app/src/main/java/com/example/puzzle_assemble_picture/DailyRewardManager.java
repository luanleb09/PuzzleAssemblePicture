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
    private static final String PREFS_NAME = "DailyRewardPrefs";
    private static final String KEY_CHECKED_IN_DATE = "checked_in_"; // + yyyy-MM-dd
    private static final String KEY_CURRENT_MONTH_YEAR = "current_month_year";

    // Rewards based on day of month
    private static final int BASE_REWARD = 50;
    private static final int REWARD_INCREMENT = 10;

    private final SharedPreferences prefs;
    private final CoinManager coinManager;

    public DailyRewardManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        coinManager = new CoinManager(context);
    }

    /**
     * Get current date as string (yyyy-MM-dd)
     */
    private String getDateString(Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(calendar.getTime());
    }

    /**
     * Get current month-year string (yyyy-MM)
     */
    private String getCurrentMonthYear() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM", Locale.US);
        return sdf.format(new Date());
    }

    /**
     * Get number of days in current month
     */
    public int getDaysInCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    /**
     * Check if a specific day is checked in
     */
    public boolean isDayCheckedIn(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, day);
        String dateKey = getDateString(calendar);
        return prefs.getBoolean(KEY_CHECKED_IN_DATE + dateKey, false);
    }

    /**
     * Check if user can check in today
     */
    public boolean canCheckInToday() {
        Calendar today = Calendar.getInstance();
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        return !isDayCheckedIn(currentDay);
    }

    /**
     * Get current day of month
     */
    public int getCurrentDay() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }

    /**
     * Get reward for specific day
     */
    public int getRewardForDay(int day) {
        // Reward increases each day: 50, 60, 70, 80...
        return BASE_REWARD + (day - 1) * REWARD_INCREMENT;
    }

    /**
     * Check in for today
     */
    public CheckInResult checkIn() {
        Calendar today = Calendar.getInstance();
        int currentDay = today.get(Calendar.DAY_OF_MONTH);
        String dateKey = getDateString(today);

        // ✅ Check if already checked in today
        if (prefs.getBoolean(KEY_CHECKED_IN_DATE + dateKey, false)) {
            Log.w(TAG, "Already checked in today!");
            return new CheckInResult(false, 0, currentDay, "Already checked in today!");
        }

        // Calculate reward
        int reward = getRewardForDay(currentDay);

        // Save check-in
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_CHECKED_IN_DATE + dateKey, true);
        editor.putString(KEY_CURRENT_MONTH_YEAR, getCurrentMonthYear());
        editor.commit(); // Use commit() for immediate save

        // Award coins
        coinManager.addCoins(reward);

        Log.d(TAG, "Check-in successful! Day: " + currentDay + ", Reward: " + reward);

        return new CheckInResult(true, reward, currentDay, "Check-in successful!");
    }

    /**
     * ✅ Check in for a past day using rewarded ad
     */
    public CheckInResult checkInPastDay(int day) {
        Calendar calendar = Calendar.getInstance();
        int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

        // Validate day
        if (day < 1 || day > getDaysInCurrentMonth()) {
            return new CheckInResult(false, 0, day, "Invalid day");
        }

        // Can't check in future days
        if (day > currentDay) {
            return new CheckInResult(false, 0, day, "Cannot check in future days");
        }

        // Check if already checked in
        if (isDayCheckedIn(day)) {
            return new CheckInResult(false, 0, day, "Already checked in for this day");
        }

        // Calculate reward
        int reward = getRewardForDay(day);

        // Save check-in
        calendar.set(Calendar.DAY_OF_MONTH, day);
        String dateKey = getDateString(calendar);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_CHECKED_IN_DATE + dateKey, true);
        editor.commit();

        // Award coins
        coinManager.addCoins(reward);

        Log.d(TAG, "Past day check-in successful! Day: " + day + ", Reward: " + reward);

        return new CheckInResult(true, reward, day, "Past day checked in!");
    }

    /**
     * Get total checked in days this month
     */
    public int getTotalCheckedInDays() {
        int count = 0;
        int daysInMonth = getDaysInCurrentMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            if (isDayCheckedIn(day)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Reset when new month starts
     */
    public void checkAndResetForNewMonth() {
        String savedMonthYear = prefs.getString(KEY_CURRENT_MONTH_YEAR, "");
        String currentMonthYear = getCurrentMonthYear();

        if (!savedMonthYear.equals(currentMonthYear)) {
            // New month - reset all check-ins
            Log.d(TAG, "New month detected - resetting check-ins");
            SharedPreferences.Editor editor = prefs.edit();

            // Clear all check-in data
            for (int day = 1; day <= 31; day++) {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.DAY_OF_MONTH, day);
                String dateKey = getDateString(calendar);
                editor.remove(KEY_CHECKED_IN_DATE + dateKey);
            }

            editor.putString(KEY_CURRENT_MONTH_YEAR, currentMonthYear);
            editor.apply();
        }
    }

    /**
     * Reset all data (for testing)
     */
    public void resetAll() {
        prefs.edit().clear().apply();
        Log.d(TAG, "All check-in data reset");
    }

    /**
     * Check-in result class
     */
    public static class CheckInResult {
        public boolean success;
        public int reward;
        public int day;
        public String message;

        public CheckInResult(boolean success, int reward, int day, String message) {
            this.success = success;
            this.reward = reward;
            this.day = day;
            this.message = message;
        }
    }
}