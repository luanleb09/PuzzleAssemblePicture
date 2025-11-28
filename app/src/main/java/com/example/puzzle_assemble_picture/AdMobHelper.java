package com.example.puzzle_assemble_picture;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;

/**
 * Helper class để quản lý AdMob ads
 */
public class AdMobHelper {

    private static final String TAG = "AdMobHelper";
    private static boolean isInitialized = false;

    // Test Banner Ad Unit ID - Thay bằng ID thật khi publish
    public static final String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";

    /**
     * Khởi tạo AdMob SDK (chỉ cần gọi 1 lần)
     */
    public static void initialize(Activity activity) {
        if (!isInitialized) {
            MobileAds.initialize(activity, initializationStatus -> {
                isInitialized = true;
                Log.d(TAG, "AdMob initialized successfully");
            });
        }
    }

    /**
     * Load banner ad vào AdView
     */
    public static void loadBannerAd(AdView adView) {
        if (adView == null) {
            Log.e(TAG, "AdView is null!");
            return;
        }

        AdRequest adRequest = new AdRequest.Builder().build();

        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                Log.d(TAG, "Banner ad loaded successfully");
                adView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                Log.e(TAG, "Banner ad failed to load: " + loadAdError.getMessage());
                // Ẩn container nếu load thất bại
                if (adView.getParent() instanceof View) {
                    ((View) adView.getParent()).setVisibility(View.GONE);
                }
            }

            @Override
            public void onAdOpened() {
                Log.d(TAG, "Banner ad opened");
            }

            @Override
            public void onAdClosed() {
                Log.d(TAG, "Banner ad closed");
            }
        });

        adView.loadAd(adRequest);
    }

    /**
     * Destroy ad khi activity bị destroy
     */
    public static void destroyAd(AdView adView) {
        if (adView != null) {
            adView.destroy();
            Log.d(TAG, "AdView destroyed");
        }
    }

    /**
     * Pause ad khi activity pause
     */
    public static void pauseAd(AdView adView) {
        if (adView != null) {
            adView.pause();
        }
    }

    /**
     * Resume ad khi activity resume
     */
    public static void resumeAd(AdView adView) {
        if (adView != null) {
            adView.resume();
        }
    }
}