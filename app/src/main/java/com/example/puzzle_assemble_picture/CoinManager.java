package com.example.puzzle_assemble_picture;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.text.DecimalFormat;

public class CoinManager {
    private static final String TAG = "CoinManager";
    private static final String PREFS_NAME = "CoinPrefs";
    private static final String KEY_COINS = "total_coins";
    private static final String KEY_TOTAL_EARNED = "total_earned";
    private static final String KEY_TOTAL_SPENT = "total_spent";

    private final SharedPreferences prefs;

    public CoinManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize with starting coins if first time
        if (!prefs.contains(KEY_COINS)) {
            prefs.edit().putInt(KEY_COINS, GameConfig.INITIAL_COINS).apply();
        }
    }

    /**
     * Get current coin balance
     */
    public int getCoins() {
        return prefs.getInt(KEY_COINS, 0);
    }

    /**
     * Add coins
     */
    public void addCoins(int amount) {
        if (amount <= 0) return;

        int current = getCoins();
        int newBalance = current + amount;

        int totalEarned = prefs.getInt(KEY_TOTAL_EARNED, 0) + amount;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_COINS, newBalance);
        editor.putInt(KEY_TOTAL_EARNED, totalEarned);
        editor.apply();

        Log.d(TAG, "✅ Added " + amount + " coins. New balance: " + newBalance);
    }

    /**
     * Spend coins (returns true if successful)
     */
    public boolean spendCoins(int amount) {
        if (amount <= 0) return false;

        int current = getCoins();
        if (current < amount) {
            Log.d(TAG, "❌ Insufficient coins. Need: " + amount + ", Have: " + current);
            return false;
        }

        int newBalance = current - amount;
        int totalSpent = prefs.getInt(KEY_TOTAL_SPENT, 0) + amount;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_COINS, newBalance);
        editor.putInt(KEY_TOTAL_SPENT, totalSpent);
        editor.apply();

        Log.d(TAG, "✅ Spent " + amount + " coins. New balance: " + newBalance);
        return true;
    }

    /**
     * Check if user can afford
     */
    public boolean canAfford(int amount) {
        return getCoins() >= amount;
    }

    /**
     * Get total earned coins
     */
    public int getTotalEarned() {
        return prefs.getInt(KEY_TOTAL_EARNED, 0);
    }

    /**
     * Get total spent coins
     */
    public int getTotalSpent() {
        return prefs.getInt(KEY_TOTAL_SPENT, 0);
    }

    /**
     * Get coin reward for level completion
     */
    public static int getRewardForLevel(String mode) {
        switch (mode) {
            case GameMode.MODE_EASY:
                return GameConfig.COINS_PER_LEVEL_EASY;
            case GameMode.MODE_NORMAL:
                return GameConfig.COINS_PER_LEVEL_NORMAL;
            case GameMode.MODE_HARD:
                return GameConfig.COINS_PER_LEVEL_HARD;
            case GameMode.MODE_INSANE:
                return GameConfig.COINS_PER_LEVEL_INSANE;
            default:
                return 10;
        }
    }

    // ============= COIN FORMATTING METHODS =============

    /**
     * Get formatted coin display (without icon)
     */
    public String getFormattedCoins() {
        return formatCoins(getCoins());
    }

    /**
     * Get formatted coin display with icon "🪙 XXX"
     */
    public String getFormattedCoinsWithIcon() {
        return "🪙 " + getFormattedCoins();
    }

    /**
     * Get short formatted coin display for rewards
     */
    public static String formatRewardDisplay(int amount) {
        return "+" + formatCoins(amount);
    }

    /**
     * Format coin number
     */
    public static String formatCoins(int coins) {
        if (coins < 1000) {
            return String.valueOf(coins);
        } else if (coins < 10000) {
            double k = coins / 1000.0;
            DecimalFormat df = new DecimalFormat("#.#");
            return df.format(k) + "K";
        } else if (coins < 1000000) {
            int k = coins / 1000;
            return k + "K";
        } else if (coins < 10000000) {
            double m = coins / 1000000.0;
            DecimalFormat df = new DecimalFormat("#.#");
            return df.format(m) + "M";
        } else {
            int m = coins / 1000000;
            return m + "M";
        }
    }

    /**
     * Format coin with icon
     */
    public static String formatCoinsWithIcon(int coins) {
        return "🪙 " + formatCoins(coins);
    }

    /**
     * Debug: Add test coins
     */
    public void addTestCoins() {
        addCoins(1000);
    }

    /**
     * Debug: Reset all coins
     */
    public void resetCoins() {
        prefs.edit().clear().apply();
        // Reinitialize with starting coins
        prefs.edit().putInt(KEY_COINS, GameConfig.INITIAL_COINS).apply();
        Log.d(TAG, "🔄 Coins reset to " + GameConfig.INITIAL_COINS);
    }
}