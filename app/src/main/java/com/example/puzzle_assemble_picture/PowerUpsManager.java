package com.example.puzzle_assemble_picture;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PowerUpsManager {
    private static final String TAG = "PowerUpsManager";
    private static final String PREFS_NAME = "PowerUpsPrefs";

    // Keys for SharedPreferences
    private static final String KEY_AUTO_SOLVE_COUNT = "autoSolveCount";
    private static final String KEY_SHUFFLE_COUNT = "shuffleCount";
    private static final String KEY_SOLVE_CORNERS_COUNT = "solveCornersCount";
    private static final String KEY_SOLVE_EDGES_COUNT = "solveEdgesCount";
    private static final String KEY_REVEAL_PREVIEW_COUNT = "revealPreviewCount";
    private static final String KEY_LAST_RESET_DATE = "lastResetDate";

    // ✅ Daily free uses for each power-up
    private static final int DAILY_AUTO_SOLVE = 3;
    private static final int DAILY_SHUFFLE = 5;
    private static final int DAILY_SOLVE_CORNERS = 2;
    private static final int DAILY_SOLVE_EDGES = 1;
    private static final int DAILY_REVEAL_PREVIEW = 2;

    // Rewarded ad unit ID (Test ID - replace with production ID)
    private static final String REWARDED_AD_ID = GameConfig.REWARDED_AD_ID;

    private final SharedPreferences prefs;
    private final Context context;
    private RewardedAd rewardedAd;
    private boolean isLoadingAd = false;
    private PowerUpType pendingPowerUp = null;
    private PowerUpCallback pendingCallback = null;

    // ✅ Enum for all 5 power-up types
    public enum PowerUpType {
        AUTO_SOLVE,
        SHUFFLE,
        SOLVE_CORNERS,
        SOLVE_EDGES,
        REVEAL_PREVIEW
    }

    public interface PowerUpCallback {
        void onSuccess();
        void onFailed(String reason);
    }

    public PowerUpsManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Check and reset daily if needed
        checkAndResetDaily();

        Log.d(TAG, "PowerUpsManager initialized (ads will load on demand)");
    }

    /**
     * ✅ Add uses to a power-up (e.g., from shop purchase or rewards)
     */
    public void addUses(PowerUpType type, int amount) {
        String key = getKeyForType(type);
        int currentUses = prefs.getInt(key, 0);
        int newUses = currentUses + amount;

        prefs.edit().putInt(key, newUses).apply();

        Log.d(TAG, "✅ Added " + amount + " uses to " + type + " (total: " + newUses + ")");
    }

    /**
     * ✅ Check if it's a new day and reset free uses
     */
    private void checkAndResetDaily() {
        String today = getTodayDate();
        String lastResetDate = prefs.getString(KEY_LAST_RESET_DATE, "");

        if (!today.equals(lastResetDate)) {
            Log.d(TAG, "📅 New day detected! Resetting free uses.");

            prefs.edit()
                    .putInt(KEY_AUTO_SOLVE_COUNT, DAILY_AUTO_SOLVE)
                    .putInt(KEY_SHUFFLE_COUNT, DAILY_SHUFFLE)
                    .putInt(KEY_SOLVE_CORNERS_COUNT, DAILY_SOLVE_CORNERS)
                    .putInt(KEY_SOLVE_EDGES_COUNT, DAILY_SOLVE_EDGES)
                    .putInt(KEY_REVEAL_PREVIEW_COUNT, DAILY_REVEAL_PREVIEW)
                    .putString(KEY_LAST_RESET_DATE, today)
                    .apply();

            Log.d(TAG, "✅ Daily reset complete!");
        }
    }

    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    /**
     * ✅ Use a power-up (Priority: Free → Coins → Ads)
     */
    public void usePowerUp(PowerUpType type, PowerUpCallback callback) {
        int remaining = getRemainingUses(type);

        if (remaining > 0) {
            // 1. Use free daily uses
            decrementCount(type);
            callback.onSuccess();
        } else {
            // 2. No free uses left, show options: Coins or Ads
            showPurchaseOptions(type, callback);
        }
    }

    /**
     * ✅ Show purchase options: Buy with coins or watch ad
     */
    private void showPurchaseOptions(PowerUpType type, PowerUpCallback callback) {
        if (!(context instanceof Activity)) {
            callback.onFailed("Cannot show dialog");
            return;
        }

        // Ensure ad is loading/loaded when needed
        ensureAdLoaded();

        CoinManager coinManager = new CoinManager(context);

        // ✅ FIX: Use switch-case instead of chained ternary
        int cost = getCostForType(type);
        String powerUpName = getNameForType(type);
        int currentCoins = coinManager.getCoins();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Use " + powerUpName + "?");
        builder.setMessage(
                "❌ No free uses left today!\n\n" +
                        "Choose an option:\n" +
                        "💰 Buy with " + cost + " coins (You have: " + currentCoins + ")\n" +
                        "📺 Watch an ad (Free)\n\n" +
                        "Daily free uses reset at midnight!"
        );

        // Option 1: Buy with coins
        if (coinManager.canAfford(cost)) {
            builder.setPositiveButton("💰 Buy (" + cost + " coins)", (dialog, which) -> {
                if (coinManager.spendCoins(cost)) {
                    Toast.makeText(context, "✅ Purchased! -" + cost + " coins",
                            Toast.LENGTH_SHORT).show();
                    callback.onSuccess();
                } else {
                    callback.onFailed("Not enough coins!");
                }
            });
        } else {
            // Not enough coins - show disabled button or hide it
            builder.setPositiveButton("💰 Buy (" + cost + " coins) - Not enough!", null);
        }

        // Option 2: Watch ad
        builder.setNegativeButton("📺 Watch Ad", (dialog, which) -> {
            showRewardedAdForPowerUp(type, callback);
        });

        // Option 3: Cancel
        builder.setNeutralButton("Cancel", (dialog, which) -> {
            callback.onFailed("Cancelled");
        });

        builder.setCancelable(false);
        builder.show();
    }

    /**
     * ✅ Get remaining free uses
     */
    public int getRemainingUses(PowerUpType type) {
        String key = getKeyForType(type);
        return prefs.getInt(key, 0);
    }

    /**
     * ✅ Decrement count
     */
    private void decrementCount(PowerUpType type) {
        String key = getKeyForType(type);
        int current = prefs.getInt(key, 0);
        if (current > 0) {
            prefs.edit().putInt(key, current - 1).apply();
            Log.d(TAG, type + " count: " + current + " → " + (current - 1));
        }
    }

    /**
     * ✅ Get cost for power-up type (in coins)
     */
    private int getCostForType(PowerUpType type) {
        switch (type) {
            case AUTO_SOLVE:
                return GameConfig.COST_AUTO_SOLVE;
            case SHUFFLE:
                return GameConfig.COST_SHUFFLE;
            case SOLVE_CORNERS:
                return GameConfig.COST_SOLVE_CORNERS;
            case SOLVE_EDGES:
                return GameConfig.COST_SOLVE_EDGES;
            case REVEAL_PREVIEW:
                return GameConfig.COST_REVEAL_PREVIEW;
            default:
                return 50;
        }
    }

    /**
     * ✅ Get display name for power-up type
     */
    private String getNameForType(PowerUpType type) {
        switch (type) {
            case AUTO_SOLVE:
                return "Auto-Solve";
            case SHUFFLE:
                return "Shuffle";
            case SOLVE_CORNERS:
                return "Solve Corners";
            case SOLVE_EDGES:
                return "Solve Edges";
            case REVEAL_PREVIEW:
                return "Reveal Preview";
            default:
                return "Power-Up";
        }
    }

    /**
     * ✅ Get SharedPreferences key for power-up type
     */
    private String getKeyForType(PowerUpType type) {
        switch (type) {
            case AUTO_SOLVE:
                return KEY_AUTO_SOLVE_COUNT;
            case SHUFFLE:
                return KEY_SHUFFLE_COUNT;
            case SOLVE_CORNERS:
                return KEY_SOLVE_CORNERS_COUNT;
            case SOLVE_EDGES:
                return KEY_SOLVE_EDGES_COUNT;
            case REVEAL_PREVIEW:
                return KEY_REVEAL_PREVIEW_COUNT;
            default:
                return "";
        }
    }

    /**
     * ✅ Ensure ad is loaded (call this when needed)
     */
    private void ensureAdLoaded() {
        if (rewardedAd == null && !isLoadingAd) {
            loadRewardedAd();
        }
    }

    /**
     * ✅ Load rewarded ad (MUST be called on main thread)
     */
    private void loadRewardedAd() {
        // Check if on main thread
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            // Not on main thread, post to main thread
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(this::loadRewardedAdInternal);
            }
            return;
        }

        loadRewardedAdInternal();
    }

    /**
     * ✅ Internal ad loading (guaranteed to be on main thread)
     */
    private void loadRewardedAdInternal() {
        if (isLoadingAd || rewardedAd != null) {
            return;
        }

        isLoadingAd = true;
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(context, REWARDED_AD_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                isLoadingAd = false;
                Log.d(TAG, "✅ Rewarded ad loaded successfully");
                setupAdCallbacks();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "❌ Ad load failed: " + loadAdError.getMessage());
                rewardedAd = null;
                isLoadingAd = false;
            }
        });
    }

    /**
     * ✅ Setup ad callbacks
     */
    private void setupAdCallbacks() {
        if (rewardedAd == null) return;

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "📺 Ad dismissed");
                rewardedAd = null;
                loadRewardedAd(); // Preload next ad
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "❌ Ad show failed: " + adError.getMessage());
                rewardedAd = null;
                if (pendingCallback != null) {
                    pendingCallback.onFailed("Ad failed to show");
                    pendingCallback = null;
                }
                loadRewardedAd();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "📺 Ad showed");
            }
        });
    }

    /**
     * ✅ Show rewarded ad for power-up
     */
    private void showRewardedAdForPowerUp(PowerUpType type, PowerUpCallback callback) {
        if (!(context instanceof Activity)) {
            callback.onFailed("Cannot show ad");
            return;
        }

        Activity activity = (Activity) context;

        if (rewardedAd != null) {
            pendingPowerUp = type;
            pendingCallback = callback;

            rewardedAd.show(activity, rewardItem -> {
                Log.d(TAG, "✅ User earned reward: " + rewardItem.getAmount());

                if (pendingCallback != null) {
                    pendingCallback.onSuccess();
                    Toast.makeText(context, "✨ Reward earned!", Toast.LENGTH_SHORT).show();
                }

                pendingPowerUp = null;
                pendingCallback = null;
            });
        } else {
            // Ad not ready, try loading
            if (!isLoadingAd) {
                pendingPowerUp = type;
                pendingCallback = callback;
                Toast.makeText(context, "⏳ Loading ad...", Toast.LENGTH_SHORT).show();
                loadRewardedAd();

                // Wait 2 seconds then try again
                new android.os.Handler().postDelayed(() -> {
                    if (rewardedAd != null && pendingCallback != null) {
                        showRewardedAdForPowerUp(pendingPowerUp, pendingCallback);
                    } else if (pendingCallback != null) {
                        pendingCallback.onFailed("Ad not available. Please try again later.");
                        pendingCallback = null;
                        pendingPowerUp = null;
                    }
                }, 2000);
            } else {
                callback.onFailed("Ad is loading, please wait...");
            }
        }
    }

    /**
     * ✅ Reload ad manually
     */
    public void reloadAd() {
        loadRewardedAd();
    }

    /**
     * ✅ Check if ad is ready
     */
    public boolean isAdReady() {
        return rewardedAd != null;
    }

    /**
     * ✅ Reset to initial daily values (for testing)
     */
    public void resetToInitial() {
        prefs.edit()
                .putInt(KEY_AUTO_SOLVE_COUNT, DAILY_AUTO_SOLVE)
                .putInt(KEY_SHUFFLE_COUNT, DAILY_SHUFFLE)
                .putInt(KEY_SOLVE_CORNERS_COUNT, DAILY_SOLVE_CORNERS)
                .putInt(KEY_SOLVE_EDGES_COUNT, DAILY_SOLVE_EDGES)
                .putInt(KEY_REVEAL_PREVIEW_COUNT, DAILY_REVEAL_PREVIEW)
                .putString(KEY_LAST_RESET_DATE, getTodayDate())
                .apply();
        Log.d(TAG, "✅ Reset to initial values");
    }

    /**
     * ✅ Get debug info (for testing)
     */
    public String getDebugInfo() {
        CoinManager coinManager = new CoinManager(context);
        return "=== Power-Ups Status ===\n" +
                "🎯 Auto-Solve: " + getRemainingUses(PowerUpType.AUTO_SOLVE) + "/" + DAILY_AUTO_SOLVE + "\n" +
                "🔀 Shuffle: " + getRemainingUses(PowerUpType.SHUFFLE) + "/" + DAILY_SHUFFLE + "\n" +
                "📐 Solve Corners: " + getRemainingUses(PowerUpType.SOLVE_CORNERS) + "/" + DAILY_SOLVE_CORNERS + "\n" +
                "🔲 Solve Edges: " + getRemainingUses(PowerUpType.SOLVE_EDGES) + "/" + DAILY_SOLVE_EDGES + "\n" +
                "👁️ Reveal Preview: " + getRemainingUses(PowerUpType.REVEAL_PREVIEW) + "/" + DAILY_REVEAL_PREVIEW + "\n\n" +
                "💰 Coins: " + coinManager.getCoins() + "\n" +
                "📅 Last Reset: " + prefs.getString(KEY_LAST_RESET_DATE, "Never") + "\n" +
                "📺 Ad Ready: " + (isAdReady() ? "✅ Yes" : "❌ No");
    }

    /**
     * ✅ Save power-ups state (called from GameActivity)
     */
    public void savePowerUps(Context context) {
        // Power-ups are automatically saved via SharedPreferences
        // This method exists for compatibility with existing code
        Log.d(TAG, "💾 Power-ups state saved");
    }
}