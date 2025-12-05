package com.example.puzzle_assemble_picture;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Loading Activity - Pre-download asset packs và initialize resources
 * Flow: Splash (logo) -> Loading (puzzle_sample.png + download) -> MainActivity
 */
public class LoadingActivity extends AppCompatActivity {

    private static final String TAG = "LoadingActivity";
    private static final int MIN_LOADING_TIME = 1500; // Minimum 1.5s để user thấy loading screen

    private TextView loadingText;
    private ProgressBar progressBar;
    private PreDownloadManager preDownloadManager;
    private Handler handler;
    private long startTime;

    private boolean adMobInitialized = false;
    private boolean packDownloadComplete = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // Reuse layout với puzzle_sample.png

        startTime = System.currentTimeMillis();
        handler = new Handler(Looper.getMainLooper());

        loadingText = findViewById(R.id.loadingText);
        progressBar = findViewById(R.id.progressBar);

        // Make progress bar horizontal for download progress
        progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        progressBar.setProgress(0);

        preDownloadManager = new PreDownloadManager(this);

        // Start loading tasks in parallel
        initializeAdMob();
        downloadNextMissingPack();
    }

    /**
     * Initialize AdMob in background
     */
    private void initializeAdMob() {
        updateLoadingText("Initializing ads...");

        new Thread(() -> {
            try {
                AdMobHelper.initialize(LoadingActivity.this);
                Log.d(TAG, "AdMob initialized");

                // ✅ THÊM: Preload interstitial ad ngay sau khi init
                runOnUiThread(() -> {
                    try {
                        InterstitialAdManager adManager = new InterstitialAdManager(LoadingActivity.this);
                        adManager.loadAd();
                        Log.d(TAG, "Interstitial ad preloading started");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to preload interstitial ad", e);
                    }

                    adMobInitialized = true;
                    checkIfReadyToNavigate();
                });

            } catch (Exception e) {
                Log.e(TAG, "AdMob init failed", e);
                runOnUiThread(() -> {
                    adMobInitialized = true; // Continue anyway
                    checkIfReadyToNavigate();
                });
            }
        }).start();
    }

    /**
     * Download next missing asset pack (pack_001, pack_002, ...)
     */
    private void downloadNextMissingPack() {
        // Get all packs
        java.util.List<String> allPacks = PreDownloadManager.getAllPackNames();

        // Find first missing pack
        String packToDownload = null;
        for (String pack : allPacks) {
            if (!preDownloadManager.isPackDownloaded(pack)) {
                packToDownload = pack;
                break;
            }
        }

        if (packToDownload == null) {
            // All packs downloaded
            Log.d(TAG, "All asset packs already downloaded");
            updateLoadingText("Ready!");
            packDownloadComplete = true;
            checkIfReadyToNavigate();
            return;
        }

        // Download the missing pack
        final String finalPackName = packToDownload;
        updateLoadingText("Downloading " + finalPackName + "...");

        preDownloadManager.setProgressListener(new PreDownloadManager.DownloadProgressListener() {
            @Override
            public void onDownloadStarted(String packName) {
                Log.d(TAG, "Started downloading: " + packName);
                runOnUiThread(() -> {
                    updateLoadingText("Downloading " + packName + "...");
                    progressBar.setProgress(0);
                });
            }

            @Override
            public void onDownloadProgress(String packName, int progress) {
                runOnUiThread(() -> {
                    progressBar.setProgress(progress);
                    updateLoadingText("Downloading " + packName + "... " + progress + "%");
                });
            }

            @Override
            public void onDownloadCompleted(String packName) {
                Log.d(TAG, "Download completed: " + packName);
                runOnUiThread(() -> {
                    updateLoadingText("Download complete!");
                    progressBar.setProgress(100);
                    packDownloadComplete = true;
                    checkIfReadyToNavigate();
                });
            }

            @Override
            public void onDownloadFailed(String packName, String error) {
                Log.e(TAG, "Download failed: " + packName + " - " + error);
                runOnUiThread(() -> {
                    updateLoadingText("Download failed, continuing...");
                    packDownloadComplete = true; // Continue anyway
                    checkIfReadyToNavigate();
                });
            }

            @Override
            public void onAllDownloadsCompleted() {
                // Not used in this flow
            }
        });

        preDownloadManager.downloadPack(finalPackName);
    }

    /**
     * Check if both tasks completed and minimum time elapsed
     */
    private void checkIfReadyToNavigate() {
        if (!adMobInitialized || !packDownloadComplete) {
            return; // Wait for both tasks
        }

        long elapsed = System.currentTimeMillis() - startTime;
        long remainingTime = MIN_LOADING_TIME - elapsed;

        if (remainingTime > 0) {
            // Wait minimum time for UX
            updateLoadingText("Ready!");
            handler.postDelayed(this::navigateToMain, remainingTime);
        } else {
            navigateToMain();
        }
    }

    /**
     * Navigate to MainActivity
     */
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * Update loading text
     */
    private void updateLoadingText(String text) {
        if (loadingText != null) {
            loadingText.setText(text);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onBackPressed() {
        // Disable back button during loading
    }
}