package com.example.puzzle_assemble_picture;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.play.core.assetpacks.AssetPackManager;
import com.google.android.play.core.assetpacks.AssetPackManagerFactory;
import com.google.android.play.core.assetpacks.AssetPackState;
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener;
import com.google.android.play.core.assetpacks.model.AssetPackStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manager cho pre-download asset packs
 */
public class PreDownloadManager {
    private static final String TAG = "PreDownloadManager";
    private static final String PREFS_NAME = "PreDownloadPrefs";
    private static final String KEY_DOWNLOADED_PACKS = "downloaded_packs";
    private static final String KEY_AUTO_DOWNLOAD = "auto_download_enabled";

    private static final int BUNDLED_LEVELS = 10;
    private static final int LEVELS_PER_PACK = 20;
    private static final String PACK_PREFIX = "puzzlepack_";

    private final Context context;
    private final AssetPackManager assetPackManager;
    private final SharedPreferences prefs;
    private DownloadProgressListener progressListener;

    public interface DownloadProgressListener {
        void onDownloadStarted(String packName);
        void onDownloadProgress(String packName, int progress);
        void onDownloadCompleted(String packName);
        void onDownloadFailed(String packName, String error);
        void onAllDownloadsCompleted();
    }

    public PreDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.assetPackManager = AssetPackManagerFactory.getInstance(context);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setProgressListener(DownloadProgressListener listener) {
        this.progressListener = listener;
    }

    /**
     * Check if auto-download is enabled
     */
    public boolean isAutoDownloadEnabled() {
        return prefs.getBoolean(KEY_AUTO_DOWNLOAD, true);
    }

    /**
     * Enable/disable auto-download
     */
    public void setAutoDownloadEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_AUTO_DOWNLOAD, enabled).apply();
    }

    /**
     * Get pack name for a level
     */
    public static String getPackNameForLevel(int levelNumber) {
        if (levelNumber <= BUNDLED_LEVELS) {
            return null; // Bundled, no pack needed
        }
        int packNumber = ((levelNumber - BUNDLED_LEVELS - 1) / LEVELS_PER_PACK) + 1;
        return String.format("%s%03d", PACK_PREFIX, packNumber);
    }

    /**
     * Get all pack names (puzzlepack_001 to puzzlepack_005 for 100 levels)
     */
    public static List<String> getAllPackNames() {
        List<String> packs = new ArrayList<>();
        int totalPacks = (GameProgressManager.MAX_LEVEL - BUNDLED_LEVELS + LEVELS_PER_PACK - 1) / LEVELS_PER_PACK;

        for (int i = 1; i <= totalPacks; i++) {
            packs.add(String.format("%s%03d", PACK_PREFIX, i));
        }
        return packs;
    }

    /**
     * Check if a pack is downloaded
     */
    public boolean isPackDownloaded(String packName) {
        Set<String> downloaded = prefs.getStringSet(KEY_DOWNLOADED_PACKS, new HashSet<>());
        return downloaded.contains(packName);
    }

    /**
     * Mark pack as downloaded
     */
    private void markPackDownloaded(String packName) {
        Set<String> downloaded = new HashSet<>(prefs.getStringSet(KEY_DOWNLOADED_PACKS, new HashSet<>()));
        downloaded.add(packName);
        prefs.edit().putStringSet(KEY_DOWNLOADED_PACKS, downloaded).apply();
        Log.d(TAG, "Marked pack as downloaded: " + packName);
    }

    /**
     * Download a specific pack
     */
    public void downloadPack(String packName) {
        if (packName == null) return;

        Log.d(TAG, "Requesting download for pack: " + packName);

        // Check current status first
        assetPackManager.getPackStates(Arrays.asList(packName))
                .addOnSuccessListener(assetPackStates -> {
                    AssetPackState state = assetPackStates.packStates().get(packName);

                    if (state == null) {
                        notifyFailed(packName, "Pack not found");
                        return;
                    }

                    int status = state.status();

                    if (status == AssetPackStatus.COMPLETED) {
                        markPackDownloaded(packName);
                        notifyCompleted(packName);
                        return;
                    }

                    if (status == AssetPackStatus.DOWNLOADING || status == AssetPackStatus.TRANSFERRING) {
                        listenForPackDownload(packName);
                        return;
                    }

                    // Start download
                    startDownload(packName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check pack status", e);
                    notifyFailed(packName, e.getMessage());
                });
    }

    /**
     * Download all packs
     */
    public void downloadAllPacks() {
        List<String> allPacks = getAllPackNames();
        Log.d(TAG, "Starting download for all packs: " + allPacks.size());

        downloadPacksSequentially(allPacks, 0);
    }

    /**
     * Download packs one by one
     */
    private void downloadPacksSequentially(List<String> packs, int index) {
        if (index >= packs.size()) {
            if (progressListener != null) {
                progressListener.onAllDownloadsCompleted();
            }
            return;
        }

        String packName = packs.get(index);

        if (isPackDownloaded(packName)) {
            Log.d(TAG, "Pack already downloaded: " + packName);
            downloadPacksSequentially(packs, index + 1);
            return;
        }

        downloadPack(packName);
    }

    /**
     * Auto-download next pack based on current level
     */
    public void autoDownloadNextPack(int currentLevel) {
        if (!isAutoDownloadEnabled()) {
            Log.d(TAG, "Auto-download disabled");
            return;
        }

        int nextLevel = currentLevel + 1;
        String nextPackName = getPackNameForLevel(nextLevel);

        if (nextPackName == null) {
            Log.d(TAG, "Next level is bundled, no download needed");
            return;
        }

        if (isPackDownloaded(nextPackName)) {
            Log.d(TAG, "Next pack already downloaded: " + nextPackName);
            return;
        }

        Log.d(TAG, "Auto-downloading next pack: " + nextPackName);
        downloadPack(nextPackName);
    }

    /**
     * Start downloading a pack
     */
    private void startDownload(String packName) {
        notifyStarted(packName);

        assetPackManager.fetch(Arrays.asList(packName))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Download request successful for: " + packName);
                    listenForPackDownload(packName);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to request download", e);
                    notifyFailed(packName, e.getMessage());
                });
    }

    /**
     * Listen for download progress
     */
    private void listenForPackDownload(String packName) {
        AssetPackStateUpdateListener listener = new AssetPackStateUpdateListener() {
            @Override
            public void onStateUpdate(AssetPackState state) {
                if (!state.name().equals(packName)) return;

                int status = state.status();
                long downloaded = state.bytesDownloaded();
                long total = state.totalBytesToDownload();

                switch (status) {
                    case AssetPackStatus.DOWNLOADING:
                    case AssetPackStatus.TRANSFERRING:
                        int progress = total > 0 ? (int) ((downloaded * 100) / total) : 0;
                        notifyProgress(packName, progress);
                        Log.d(TAG, packName + " download progress: " + progress + "%");
                        break;

                    case AssetPackStatus.COMPLETED:
                        assetPackManager.unregisterListener(this);
                        markPackDownloaded(packName);
                        notifyCompleted(packName);
                        break;

                    case AssetPackStatus.FAILED:
                        assetPackManager.unregisterListener(this);
                        notifyFailed(packName, "Download failed");
                        break;

                    case AssetPackStatus.CANCELED:
                        assetPackManager.unregisterListener(this);
                        notifyFailed(packName, "Download canceled");
                        break;
                }
            }
        };

        assetPackManager.registerListener(listener);
    }

    // Notification helpers
    private void notifyStarted(String packName) {
        if (progressListener != null) {
            progressListener.onDownloadStarted(packName);
        }
    }

    private void notifyProgress(String packName, int progress) {
        if (progressListener != null) {
            progressListener.onDownloadProgress(packName, progress);
        }
    }

    private void notifyCompleted(String packName) {
        if (progressListener != null) {
            progressListener.onDownloadCompleted(packName);
        }
    }

    private void notifyFailed(String packName, String error) {
        if (progressListener != null) {
            progressListener.onDownloadFailed(packName, error);
        }
    }
}