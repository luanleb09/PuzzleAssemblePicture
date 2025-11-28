package com.example.puzzle_assemble_picture;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

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
    private static final String KEY_LAST_RESET_DATE = "lastResetDate";

    // Initial free uses per day
    private static final int DAILY_AUTO_SOLVE = 3;
    private static final int DAILY_SHUFFLE = 5;

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
        SHUFFLE
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

        // Preload rewarded ad
        loadRewardedAd();
    }

    /**
     * Check if it's a new day and reset free uses
     */
    private void checkAndResetDaily() {
        String today = getTodayDate();
        String lastResetDate = prefs.getString(KEY_LAST_RESET_DATE, "");

        if (!today.equals(lastResetDate)) {
            // New day - reset free uses
            Log.d(TAG, "New day detected! Resetting free uses. Last: " + lastResetDate + ", Today: " + today);

            prefs.edit()
                    .putInt(KEY_AUTO_SOLVE_COUNT, DAILY_AUTO_SOLVE)
                    .putInt(KEY_SHUFFLE_COUNT, DAILY_SHUFFLE)
                    .putString(KEY_LAST_RESET_DATE, today)
                    .apply();

            Log.d(TAG, "✓ Reset complete: Auto-Solve=" + DAILY_AUTO_SOLVE + ", Shuffle=" + DAILY_SHUFFLE);
        } else {
            Log.d(TAG, "Same day, no reset needed");
        }
    }

    /**
     * Get today's date as string (yyyy-MM-dd)
     */
    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }

    /**
     * Use a power-up (free or ad-based)
     */
    public void usePowerUp(PowerUpType type, PowerUpCallback callback) {
        int remaining = getRemainingUses(type);

        if (remaining > 0) {
            // Has free uses
            Log.d(TAG, "Using free " + type + " (remaining: " + remaining + ")");
            decrementCount(type);
            callback.onSuccess();
        } else {
            // No free uses - need to watch ad
            Log.d(TAG, "No free " + type + " remaining. Showing rewarded ad...");
            showRewardedAdForPowerUp(type, callback);
        }
    }

    /**
     * Get remaining free uses for a power-up type
     */
    public int getRemainingUses(PowerUpType type) {
        String key = getKeyForType(type);
        return prefs.getInt(key, 0);
    }

    /**
     * Decrement the count for a power-up type
     */
    private void decrementCount(PowerUpType type) {
        String key = getKeyForType(type);
        int current = prefs.getInt(key, 0);
        if (current > 0) {
            prefs.edit().putInt(key, current - 1).apply();
            Log.d(TAG, type + " count decreased to: " + (current - 1));
        }
    }

    /**
     * Add uses (called after watching ad)
     */
    public void addUses(PowerUpType type, int amount) {
        String key = getKeyForType(type);
        int current = prefs.getInt(key, 0);
        prefs.edit().putInt(key, current + amount).apply();
        Log.d(TAG, type + " count increased by " + amount + " to: " + (current + amount));
    }

    /**
     * Get SharedPreferences key for power-up type
     */
    private String getKeyForType(PowerUpType type) {
        switch (type) {
            case AUTO_SOLVE:
                return KEY_AUTO_SOLVE_COUNT;
            case SHUFFLE:
                return KEY_SHUFFLE_COUNT;
            default:
                return "";
        }
    }

    /**
     * Load rewarded ad
     */
    private void loadRewardedAd() {
        if (isLoadingAd || rewardedAd != null) {
            Log.d(TAG, "Ad already loaded or loading");
            return;
        }

        isLoadingAd = true;
        Log.d(TAG, "Loading rewarded ad...");

        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(context, TEST_REWARDED_AD_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                isLoadingAd = false;
                Log.d(TAG, "✓ Rewarded ad loaded successfully");

                // Set up ad callbacks
                setupAdCallbacks();
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                Log.e(TAG, "❌ Failed to load rewarded ad: " + loadAdError.getMessage());
                rewardedAd = null;
                isLoadingAd = false;
            }
        });
    }

    /**
     * Setup callbacks for the rewarded ad
     */
    private void setupAdCallbacks() {
        if (rewardedAd == null) return;

        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed");
                rewardedAd = null;

                // Reload next ad
                loadRewardedAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "❌ Failed to show ad: " + adError.getMessage());
                rewardedAd = null;

                if (pendingCallback != null) {
                    pendingCallback.onFailed("Failed to show ad. Please try again.");
                    pendingCallback = null;
                }

                // Reload next ad
                loadRewardedAd();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed full screen");
            }
        });
    }

    /**
     * Show rewarded ad for power-up
     */
    private void showRewardedAdForPowerUp(PowerUpType type, PowerUpCallback callback) {
        if (!(context instanceof Activity)) {
            callback.onFailed("Cannot show ad: invalid context");
            return;
        }

        Activity activity = (Activity) context;

        if (rewardedAd != null) {
            Log.d(TAG, "Showing rewarded ad for " + type);

            pendingPowerUp = type;
            pendingCallback = callback;

            rewardedAd.show(activity, rewardItem -> {
                // User earned the reward
                Log.d(TAG, "✓ User earned reward: " + rewardItem.getAmount());

                // Grant 1 use
                addUses(type, 1);

                // Execute the power-up
                if (pendingCallback != null) {
                    pendingCallback.onSuccess();
                    Toast.makeText(context, "✨ Reward earned! +1 " + type, Toast.LENGTH_SHORT).show();
                }

                // Clear pending
                pendingPowerUp = null;
                pendingCallback = null;
            });
        } else {
            // Ad not ready
            Log.d(TAG, "Ad not ready, trying to load...");

            // Try to load ad immediately
            if (!isLoadingAd) {
                pendingPowerUp = type;
                pendingCallback = callback;

                Toast.makeText(context, "Loading ad, please wait...", Toast.LENGTH_SHORT).show();
                loadRewardedAd();

                // Wait 2 seconds and retry
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
                callback.onFailed("Ad is loading. Please try again in a moment.");
            }
        }
    }

    /**
     * Force reload ad (can be called manually if needed)
     */
    public void reloadAd() {
        loadRewardedAd();
    }

    /**
     * Check if ad is ready
     */
    public boolean isAdReady() {
        return rewardedAd != null;
    }

    /**
     * Reset to initial values (for testing)
     */
    public void resetToInitial() {
        prefs.edit()
                .putInt(KEY_AUTO_SOLVE_COUNT, DAILY_AUTO_SOLVE)
                .putInt(KEY_SHUFFLE_COUNT, DAILY_SHUFFLE)
                .putString(KEY_LAST_RESET_DATE, getTodayDate())
                .apply();
        Log.d(TAG, "✓ Reset to initial values");
    }

    /**
     * Get debug info
     */
    public String getDebugInfo() {
        return "Auto-Solve: " + getRemainingUses(PowerUpType.AUTO_SOLVE) + "/" + DAILY_AUTO_SOLVE + "\n" +
                "Shuffle: " + getRemainingUses(PowerUpType.SHUFFLE) + "/" + DAILY_SHUFFLE + "\n" +
                "Last Reset: " + prefs.getString(KEY_LAST_RESET_DATE, "Never") + "\n" +
                "Ad Ready: " + isAdReady();
    }
}