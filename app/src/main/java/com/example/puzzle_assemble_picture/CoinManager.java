package com.example.puzzle_assemble_picture;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.DecimalFormat;

public class CoinManager {
    private static final String TAG = "CoinManager";
    private static final String PREFS_NAME = "CoinPrefs";
    private static final String KEY_COINS = "total_coins";

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
        int current = getCoins();
        prefs.edit().putInt(KEY_COINS, current + amount).apply();
    }

    /**
     * Spend coins (returns true if successful)
     */
    public boolean spendCoins(int amount) {
        int current = getCoins();
        if (current >= amount) {
            prefs.edit().putInt(KEY_COINS, current - amount).apply();
            return true;
        }
        return false;
    }

    /**
     * Check if user can afford
     */
    public boolean canAfford(int amount) {
        return getCoins() >= amount;
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
     * - Dưới 1000: hiển thị nguyên số (0, 99, 999)
     * - 1000-9999: hiển thị dạng K (1.0K, 9.9K)
     * - 10000-999999: hiển thị dạng K (10K, 99K, 999K)
     * - 1000000+: hiển thị dạng M (1.0M, 9.9M, 99M)
     *
     * @return chuỗi đã format
     */
    public String getFormattedCoins() {
        return formatCoins(getCoins());
    }

    /**
     * Get formatted coin display with icon "🪙 XXX"
     *
     * @return chuỗi với icon
     */
    public String getFormattedCoinsWithIcon() {
        return "🪙 " + getFormattedCoins();
    }

    /**
     * Get short formatted coin display with icon for toasts
     * Example: "+50" or "+1.5K"
     *
     * @param amount coin amount
     * @return formatted string for reward display
     */
    public static String formatRewardDisplay(int amount) {
        return "+" + formatCoins(amount);
    }

    /**
     * Static method: Format coin number để hiển thị gọn gàng
     *
     * @param coins số coin cần format
     * @return chuỗi đã format
     */
    public static String formatCoins(int coins) {
        if (coins < 1000) {
            return String.valueOf(coins);
        } else if (coins < 10000) {
            // 1.0K - 9.9K
            double k = coins / 1000.0;
            DecimalFormat df = new DecimalFormat("#.#");
            return df.format(k) + "K";
        } else if (coins < 1000000) {
            // 10K - 999K
            int k = coins / 1000;
            return k + "K";
        } else if (coins < 10000000) {
            // 1.0M - 9.9M
            double m = coins / 1000000.0;
            DecimalFormat df = new DecimalFormat("#.#");
            return df.format(m) + "M";
        } else {
            // 10M+
            int m = coins / 1000000;
            return m + "M";
        }
    }

    /**
     * Static method: Format coin với icon để hiển thị trực tiếp trong TextView
     *
     * @param coins số coin cần format
     * @return chuỗi với icon "🪙 XXX"
     */
    public static String formatCoinsWithIcon(int coins) {
        return "🪙 " + formatCoins(coins);
    }
}