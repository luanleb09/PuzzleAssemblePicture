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

    // Keys
    private static final String KEY_AUTO_SOLVE_COUNT = "autoSolveCount";
    private static final String KEY_SHUFFLE_COUNT = "shuffleCount";
    private static final String KEY_SOLVE_CORNERS_COUNT = "solveCornersCount";
    private static final String KEY_SOLVE_EDGES_COUNT = "solveEdgesCount";
    private static final String KEY_REVEAL_PREVIEW_COUNT = "revealPreviewCount";
    private static final String KEY_LAST_RESET_DATE = "lastResetDate";

    // Initial free uses per day
    private static final int DAILY_AUTO_SOLVE = 3;
    private static final int DAILY_SHUFFLE = 5;
    private static final int DAILY_SOLVE_CORNERS = 2;
    private static final int DAILY_SOLVE_EDGES = 1;
    private static final int DAILY_REVEAL_PREVIEW = 2;

    // Rewarded ad unit IDs (Test IDs)
    private static final String TEST_REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917";

    private SharedPreferences prefs;
    private Context context;
    private RewardedAd rewardedAd;
    private boolean isLoadingAd = false;
    private PowerUpType pendingPowerUp = null;
    private PowerUpCallback pendingCallback = null;

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

        // ❌ XÓA: Không load ad trong constructor
        // loadRewardedAd();

        // ✅ THAY ĐỔI: Ad sẽ được load lazily khi cần
        Log.d(TAG, "PowerUpsManager initialized (ads will load on demand)");
    }

    public void addUses(PowerUpType type, int amount) {
        String key = getKeyForType(type);
        int currentUses = prefs.getInt(key, 0);
        int newUses = currentUses + amount;

        prefs.edit().putInt(key, newUses).apply();

        Log.d(TAG, "✅ Added " + amount + " uses to " + type + " (total: " + newUses + ")");
    }

    /**
     * Check if it's a new day and reset free uses
     */
    private void checkAndResetDaily() {
        String today = getTodayDate();
        String lastResetDate = prefs.getString(KEY_LAST_RESET_DATE, "");

        if (!today.equals(lastResetDate)) {
            Log.d(TAG, "New day detected! Resetting free uses.");

            prefs.edit()
                    .putInt(KEY_AUTO_SOLVE_COUNT, DAILY_AUTO_SOLVE)
                    .putInt(KEY_SHUFFLE_COUNT, DAILY_SHUFFLE)
                    .putInt(KEY_SOLVE_CORNERS_COUNT, DAILY_SOLVE_CORNERS)
                    .putInt(KEY_SOLVE_EDGES_COUNT, DAILY_SOLVE_EDGES)
                    .putInt(KEY_REVEAL_PREVIEW_COUNT, DAILY_REVEAL_PREVIEW)
                    .putString(KEY_LAST_RESET_DATE, today)
                    .apply();

            Log.d(TAG, "✓ Reset complete");
        }
    }

    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    /**
     * Use a power-up (Priority: Free → Coins → Ads)
     */
    public void usePowerUp(PowerUpType type, PowerUpCallback callback) {
        int remaining = getRemainingUses(type);

        if (remaining > 0) {
            // 1. Dùng free daily uses
            decrementCount(type);
            callback.onSuccess();
        } else {
            // 2. Không còn free, show options: Coins hoặc Ads
            showPurchaseOptions(type, callback);
        }
    }

    /**
     * Show purchase options: Buy with coins or watch ad
     */
    private void showPurchaseOptions(PowerUpType type, PowerUpCallback callback) {
        if (!(context instanceof Activity)) {
            callback.onFailed("Cannot show dialog");
            return;
        }

        // ✅ THÊM: Ensure ad is loading/loaded khi cần
        ensureAdLoaded();

        CoinManager coinManager = new CoinManager(context);
        int cost = (type == PowerUpType.AUTO_SOLVE) ? GameConfig.COST_AUTO_SOLVE : GameConfig.COST_SHUFFLE;
        String powerUpName = (type == PowerUpType.AUTO_SOLVE) ? "Auto-Solve" : "Shuffle";
        int currentCoins = coinManager.getCoins();

        AlertDialog.Builder builder = new AlertDialog.Builder((Activity) context);
        builder.setTitle("Use " + powerUpName + "?");
        builder.setMessage(
                "No free uses left today!\n\n" +
                        "Options:\n" +
                        "• Buy: " + cost + " coins (You have: " + currentCoins + ")\n" +
                        "• Watch an ad (Free)"
        );

        // Option 1: Buy with coins
        if (coinManager.canAfford(cost)) {
            builder.setPositiveButton("💰 Buy (" + cost + " coins)", (dialog, which) -> {
                if (coinManager.spendCoins(cost)) {
                    Toast.makeText(context, "✅ Purchased! -" + cost + " coins", Toast.LENGTH_SHORT).show();
                    callback.onSuccess();
                } else {
                    callback.onFailed("Not enough coins!");
                }
            });
        }

        // Option 2: Watch ad
        builder.setNegativeButton("📺 Watch Ad", (dialog, which) -> {
            showRewardedAdForPowerUp(type, callback);
        });

        // Option 3: Cancel
        builder.setNeutralButton("Cancel", null);

        builder.show();
    }

    /**
     * Get remaining free uses
     */
    public int getRemainingUses(PowerUpType type) {
        String key = getKeyForType(type);
        return prefs.getInt(key, 0);
    }

    /**
     * Decrement count
     */
    private void decrementCount(PowerUpType type) {
        String key = getKeyForType(type);
        int current = prefs.getInt(key, 0);
        if (current > 0) {
            prefs.edit().putInt(key, current - 1).apply();
            Log.d(TAG, type + " count: " + (current - 1));
        }
    }

    private int getCostForType(PowerUpType type) {
        switch (type) {
            case AUTO_SOLVE:
                return GameConfig.COST_AUTO_SOLVE;
            case SHUFFLE:
                return GameConfig.COST_SHUFFLE;
            case SOLVE_CORNERS:
                return 40; // Match ShopConfig
            case SOLVE_EDGES:
                return 60; // Match ShopConfig
            default:
                return 50;
        }
    }

    /**
     * ✅ NEW: Get name for power-up type
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
                return  "Reveal Preview 10 seconds";
            default:
                return "Power-Up";
        }
    }

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
     * ✅ THÊM: Ensure ad is loaded (call this when needed)
     */
    private void ensureAdLoaded() {
        if (rewardedAd == null && !isLoadingAd) {
            loadRewardedAd();
        }
    }

    /**
     * Load rewarded ad (MUST be called on main thread)
     */
    private void loadRewardedAd() {
        // ✅ CHECK: Must be on main thread
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
     * ✅ PRIVATE: Internal ad loading (guaranteed to be on main thread)
     */
    private void loadRewardedAdInternal() {
        if (isLoadingAd || rewardedAd != null) {
            return;
        }

        isLoadingAd = true;
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(context, TEST_REWARDED_AD_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                isLoadingAd = false;
                Log.d(TAG, "✓ Rewarded ad loaded");
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

    private void setupAdCallbacks() {
        if (rewardedAd == null) return;

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                loadRewardedAd();
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
                Log.d(TAG, "Ad showed");
            }
        });
    }

    /**
     * Show rewarded ad
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
                Log.d(TAG, "✓ User earned reward");

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
                Toast.makeText(context, "Loading ad...", Toast.LENGTH_SHORT).show();
                loadRewardedAd();

                new android.os.Handler().postDelayed(() -> {
                    if (rewardedAd != null && pendingCallback != null) {
                        showRewardedAdForPowerUp(pendingPowerUp, pendingCallback);
                    } else if (pendingCallback != null) {
                        pendingCallback.onFailed("Ad not available");
                        pendingCallback = null;
                        pendingPowerUp = null;
                    }
                }, 2000);
            } else {
                callback.onFailed("Ad is loading, please wait");
            }
        }
    }

    public void reloadAd() {
        loadRewardedAd();
    }

    public boolean isAdReady() {
        return rewardedAd != null;
    }

    public void resetToInitial() {
        prefs.edit()
                .putInt(KEY_AUTO_SOLVE_COUNT, DAILY_AUTO_SOLVE)
                .putInt(KEY_SHUFFLE_COUNT, DAILY_SHUFFLE)
                .putString(KEY_LAST_RESET_DATE, getTodayDate())
                .apply();
        Log.d(TAG, "✓ Reset to initial");
    }

    public String getDebugInfo() {
        CoinManager coinManager = new CoinManager(context);
        return "Auto-Solve: " + getRemainingUses(PowerUpType.AUTO_SOLVE) + "/" + DAILY_AUTO_SOLVE + "\n" +
                "Shuffle: " + getRemainingUses(PowerUpType.SHUFFLE) + "/" + DAILY_SHUFFLE + "\n" +
                "Coins: " + coinManager.getCoins() + "\n" +
                "Last Reset: " + prefs.getString(KEY_LAST_RESET_DATE, "Never") + "\n" +
                "Ad Ready: " + isAdReady();
    }
}