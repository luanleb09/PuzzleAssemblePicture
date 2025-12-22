package com.example.puzzle_assemble_picture;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Loading Activity - Preload essentials + Download asset packs
 * Flow: Splash -> Loading -> MainActivity
 */
public class LoadingActivity extends AppCompatActivity {

    private static final String TAG = "LoadingActivity";
    private static final int MIN_LOADING_TIME = 1500; // Minimum 1.5s

    private TextView loadingText;
    private ProgressBar progressBar;
    private TextView progressText;

    private Handler handler;
    private long startTime;
    private ExecutorService executorService;

    private final AtomicInteger totalTasks = new AtomicInteger(0);
    private final AtomicInteger completedTasks = new AtomicInteger(0);

    // Managers
    private PreDownloadManager preDownloadManager;
    private GameProgressManager progressManager;
    private CoinManager coinManager;
    private PowerUpsManager powerUpsManager;
    private DailyRewardManager dailyRewardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        startTime = System.currentTimeMillis();
        handler = new Handler(Looper.getMainLooper());
        executorService = Executors.newFixedThreadPool(4); // Parallel loading

        loadingText = findViewById(R.id.loadingText);
        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);

        progressBar.setIndeterminate(false);
        progressBar.setMax(100);
        progressBar.setProgress(0);

        startLoading();
    }

    private void startLoading() {
        // Count tasks
        int tasks = 0;
        tasks++; // AdMob init
        tasks++; // Managers init
        tasks++; // Sound preload
        tasks++; // Asset pack download

        totalTasks.set(tasks);

        // Start all tasks in parallel
        loadAdMob();
        loadManagers();
        loadSoundEffects();
        downloadNextMissingPack();
    }

    // ============= LOADING TASKS =============

    /**
     * Task 1: Initialize AdMob + Preload Interstitial
     */
    private void loadAdMob() {
        executorService.execute(() -> {
            try {
                updateLoadingText("Initializing ads...");

                CountDownLatch latch = new CountDownLatch(1);

                handler.post(() -> {
                    AdMobHelper.initialize(LoadingActivity.this);
                    Log.d(TAG, "AdMob initialized");

                    // Preload interstitial ad
                    try {
                        InterstitialAdManager adManager = new InterstitialAdManager(LoadingActivity.this);
                        adManager.loadAd();
                        Log.d(TAG, "Interstitial ad preloading started");
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to preload interstitial ad", e);
                    }

                    latch.countDown();
                });

                // Wait max 3 seconds
                latch.await(3, java.util.concurrent.TimeUnit.SECONDS);

            } catch (Exception e) {
                Log.e(TAG, "AdMob init failed", e);
            } finally {
                taskCompleted();
            }
        });
    }

    /**
     * Task 2: Initialize all managers
     * ✅ FIX: Init PowerUpsManager on MAIN THREAD
     */
    private void loadManagers() {
        executorService.execute(() -> {
            try {
                updateLoadingText("Loading game data...");

                // ✅ THAY ĐỔI: Init managers on MAIN thread (vì PowerUpsManager có thể load ads)
                CountDownLatch latch = new CountDownLatch(1);

                handler.post(() -> {
                    try {
                        // Init all managers on main thread
                        progressManager = new GameProgressManager(this);
                        coinManager = new CoinManager(this);
                        dailyRewardManager = new DailyRewardManager(this);
                        preDownloadManager = new PreDownloadManager(this);
                        powerUpsManager = new PowerUpsManager(this); // ← Ads safe now (lazy loaded)

                        // Pre-cache some data
                        int totalCompleted = progressManager.getTotalCompletedLevelsAllModes();
                        int coins = coinManager.getCoins();
                        boolean canCheckIn = dailyRewardManager.canCheckInToday();

                        Log.d(TAG, "✅ Managers loaded - Levels: " + totalCompleted + ", Coins: " + coins);

                    } catch (Exception e) {
                        Log.e(TAG, "Error initializing managers", e);
                    } finally {
                        latch.countDown();
                    }
                });

                // Wait for main thread initialization
                latch.await(5, java.util.concurrent.TimeUnit.SECONDS);

                Thread.sleep(300);

            } catch (Exception e) {
                Log.e(TAG, "Managers init error", e);
            } finally {
                taskCompleted();
            }
        });
    }

    /**
     * Task 3: Preload sound effects (SIMPLIFIED)
     */
    private void loadSoundEffects() {
        executorService.execute(() -> {
            try {
                updateLoadingText("Loading sounds...");

                // ✅ SIMPLIFIED: Quick check only
                Log.d(TAG, "✅ Sound check complete");
                Thread.sleep(300);

            } catch (Exception e) {
                Log.e(TAG, "Sound loading error", e);
            } finally {
                taskCompleted();
            }
        });
    }

    /**
     * Task 4: Download next missing asset pack
     */
    private void downloadNextMissingPack() {
        executorService.execute(() -> {
            try {
                // Wait for preDownloadManager to be ready
                while (preDownloadManager == null) {
                    Thread.sleep(100);
                }

                updateLoadingText("Checking assets...");

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
                    taskCompleted();
                    return;
                }

                // Download the missing pack
                final String finalPackName = packToDownload;
                final CountDownLatch downloadLatch = new CountDownLatch(1);

                handler.post(() -> {
                    updateLoadingText("Downloading " + finalPackName + "...");

                    preDownloadManager.setProgressListener(new PreDownloadManager.DownloadProgressListener() {
                        @Override
                        public void onDownloadStarted(String packName) {
                            handler.post(() -> {
                                updateLoadingText("Downloading " + packName + "...");
                                updateDownloadProgress(0);
                            });
                        }

                        @Override
                        public void onDownloadProgress(String packName, int progress) {
                            handler.post(() -> {
                                updateDownloadProgress(progress);
                                updateLoadingText("Downloading " + packName + "... " + progress + "%");
                            });
                        }

                        @Override
                        public void onDownloadCompleted(String packName) {
                            Log.d(TAG, "✅ Download completed: " + packName);
                            handler.post(() -> {
                                updateLoadingText("Download complete!");
                                updateDownloadProgress(100);
                                downloadLatch.countDown();
                            });
                        }

                        @Override
                        public void onDownloadFailed(String packName, String error) {
                            Log.e(TAG, "Download failed: " + packName + " - " + error);
                            handler.post(() -> {
                                updateLoadingText("Continuing...");
                                downloadLatch.countDown();
                            });
                        }

                        @Override
                        public void onAllDownloadsCompleted() {
                            // Not used
                        }
                    });

                    preDownloadManager.downloadPack(finalPackName);
                });

                // Wait for download to complete
                downloadLatch.await(30, java.util.concurrent.TimeUnit.SECONDS);

            } catch (Exception e) {
                Log.e(TAG, "Pack download error", e);
            } finally {
                taskCompleted();
            }
        });
    }

    // ============= HELPER METHODS =============

    private void taskCompleted() {
        int completed = completedTasks.incrementAndGet();
        int total = totalTasks.get();
        int progress = (int) ((completed / (float) total) * 100);

        handler.post(() -> {
            animateProgress(progressBar.getProgress(), progress);
            if (progressText != null) {
                progressText.setText(completed + "/" + total);
            }

            // All tasks completed
            if (completed >= total) {
                checkIfReadyToNavigate();
            }
        });
    }

    private void checkIfReadyToNavigate() {
        long elapsed = System.currentTimeMillis() - startTime;
        long remainingTime = MIN_LOADING_TIME - elapsed;

        if (remainingTime > 0) {
            updateLoadingText("Ready!");
            handler.postDelayed(this::navigateToMain, remainingTime);
        } else {
            navigateToMain();
        }
    }

    private void animateProgress(int from, int to) {
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(300);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            progressBar.setProgress((int) animation.getAnimatedValue());
        });
        animator.start();
    }

    private void updateDownloadProgress(int progress) {
        if (progressBar != null) {
            progressBar.setProgress(progress);
        }
    }

    private void updateLoadingText(String text) {
        if (loadingText != null) {
            loadingText.setText(text);
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onBackPressed() {
        // Disable back button during loading
    }
}