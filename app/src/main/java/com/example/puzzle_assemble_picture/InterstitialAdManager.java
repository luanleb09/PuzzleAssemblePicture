package com.example.puzzle_assemble_picture;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Manager for Interstitial Ads
 * Show full-screen ad after every 5 levels completed
 */
public class InterstitialAdManager {
    private static final String TAG = "InterstitialAdManager";

    // ✅ TEST AD ID - Thay bằng real ID khi release
    private static final String TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";

    // SharedPreferences
    private static final String PREFS_NAME = "InterstitialAdPrefs";
    private static final String KEY_LEVELS_COMPLETED = "levels_completed_count";
    private static final int SHOW_AD_EVERY_X_LEVELS = 5; // Hiển thị mỗi 5 level

    private final Context context;
    private final SharedPreferences prefs;
    private InterstitialAd interstitialAd;
    private boolean isLoadingAd = false;

    public interface AdCallback {
        void onAdShown();
        void onAdDismissed();
        void onAdFailedToShow();
    }

    public InterstitialAdManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Preload ad khi khởi tạo
        loadAd();
    }

    /**
     * Load interstitial ad
     */
    public void loadAd() {
        if (isLoadingAd || interstitialAd != null) {
            Log.d(TAG, "Ad already loaded or loading");
            return;
        }

        isLoadingAd = true;

        new Thread(() -> {
            AdRequest adRequest = new AdRequest.Builder().build();

            // ✅ Post back to main thread
            new Handler(Looper.getMainLooper()).post(() -> {
                InterstitialAd.load(context, TEST_AD_UNIT_ID, adRequest,
                        new InterstitialAdLoadCallback() {
                            @Override
                            public void onAdLoaded(@NonNull InterstitialAd ad) {
                                interstitialAd = ad;
                                isLoadingAd = false;
                                Log.d(TAG, "✅ Ad loaded");
                            }

                            @Override
                            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                                interstitialAd = null;
                                isLoadingAd = false;
                                Log.e(TAG, "❌ Ad failed: " + error.getMessage());
                            }
                        });
            });
        }).start();
    }

    /**
     * Gọi method này mỗi khi user hoàn thành 1 level
     */
    public void onLevelCompleted(Activity activity, AdCallback callback) {
        // Increment counter
        int count = prefs.getInt(KEY_LEVELS_COMPLETED, 0) + 1;
        prefs.edit().putInt(KEY_LEVELS_COMPLETED, count).apply();

        Log.d(TAG, "Levels completed: " + count);

        // Check if should show ad
        if (count % SHOW_AD_EVERY_X_LEVELS == 0) {
            showAd(activity, callback);
        } else {
            // Not time to show ad yet
            if (callback != null) {
                callback.onAdDismissed();
            }
        }
    }

    /**
     * Show interstitial ad
     */
    private void showAd(Activity activity, AdCallback callback) {
        if (interstitialAd == null) {
            Log.d(TAG, "Ad not ready, skipping");
            if (callback != null) {
                callback.onAdFailedToShow();
            }
            // Load ad for next time
            loadAd();
            return;
        }

        // Setup callbacks
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed");
                interstitialAd = null;

                if (callback != null) {
                    callback.onAdDismissed();
                }

                // Load next ad
                loadAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.e(TAG, "Ad failed to show: " + adError.getMessage());
                interstitialAd = null;

                if (callback != null) {
                    callback.onAdFailedToShow();
                }

                // Load next ad
                loadAd();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed");

                if (callback != null) {
                    callback.onAdShown();
                }
            }
        });

        // Show ad
        interstitialAd.show(activity);
    }

    /**
     * Reset counter (for testing)
     */
    public void resetCounter() {
        prefs.edit().putInt(KEY_LEVELS_COMPLETED, 0).apply();
        Log.d(TAG, "Counter reset");
    }

    /**
     * Get current count (for debugging)
     */
    public int getCompletedCount() {
        return prefs.getInt(KEY_LEVELS_COMPLETED, 0);
    }
}