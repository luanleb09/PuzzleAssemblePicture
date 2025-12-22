package com.example.puzzle_assemble_picture;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.play.core.assetpacks.AssetPackLocation;
import com.google.android.play.core.assetpacks.AssetPackManager;
import com.google.android.play.core.assetpacks.AssetPackManagerFactory;
import com.google.android.play.core.assetpacks.AssetPackState;
import com.google.android.play.core.assetpacks.AssetPackStateUpdateListener;
import com.google.android.play.core.assetpacks.model.AssetPackStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

/**
 * Helper class để load puzzle images từ:
 * 1. Bundled assets (Level 1-10): app/src/main/assets/puzzles_bundled/
 * 2. On-demand asset packs (Level 11+): puzzlepack_xxx/src/main/assets/puzzles/
 */
public class PuzzleImageLoader {
    private static final String TAG = "PuzzleImageLoader";

    // Cấu hình
    private static final int BUNDLED_LEVELS = 10; // Level 1-10 trong APK
    private static final int LEVELS_PER_PACK = 20; // Mỗi pack chứa 20 level
    private static final int MAX_IMAGE_SIZE = 1200; // Max size cho bitmap
    private static final String BUNDLED_PATH = "puzzles_bundled";
    private static final String PACK_PREFIX = "puzzlepack_";
    private static final String PACK_ASSET_PATH = "puzzles";

    private final Context context;
    private final AssetPackManager assetPackManager;
    private final Handler mainHandler;

    public interface ImageLoadCallback {
        void onSuccess(Bitmap bitmap);
        void onError(String error);
        void onDownloadProgress(int progress);
    }

    public PuzzleImageLoader(Context context) {
        this.context = context.getApplicationContext();
        this.assetPackManager = AssetPackManagerFactory.getInstance(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Load puzzle image theo level number
     */
    public void loadLevelImage(int levelNumber, ImageLoadCallback callback) {
        if (levelNumber <= BUNDLED_LEVELS) {
            loadFromBundledAssets(levelNumber, MAX_IMAGE_SIZE, callback);
        } else {
            loadFromAssetPack(levelNumber, MAX_IMAGE_SIZE, callback);
        }
    }

    // Thêm constant
//    private static final int MAX_IMAGE_SIZE = 1200;

    /**
     * Load từ assets có sẵn trong APK (Level 1-10)
     */
    private void loadFromBundledAssets(int levelNumber, int maxSize, ImageLoadCallback callback) {
        new Thread(() -> {
            try {
                AssetManager assetManager = context.getAssets();
                String fileName = String.format("level_%d.webp", levelNumber);
                String fullPath = BUNDLED_PATH + "/" + fileName;

                Log.d(TAG, "Loading bundled asset: " + fullPath);

                InputStream inputStream = assetManager.open(fullPath);

                // Decode với size optimization
                Bitmap bitmap = decodeBitmapOptimized(inputStream, maxSize);
                inputStream.close();

                if (bitmap != null) {
                    postOnMain(() -> callback.onSuccess(bitmap));
                } else {
                    postOnMain(() -> callback.onError("Failed to decode bitmap from: " + fullPath));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading bundled asset for level " + levelNumber, e);
                postOnMain(() -> callback.onError("Cannot find image for level " + levelNumber + ": " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Load từ asset pack (Level 11+)
     */
    private void loadFromAssetPack(int levelNumber, int maxSize, ImageLoadCallback callback) {
        String packName = getPackNameForLevel(levelNumber);

        Log.d(TAG, "Loading from pack: " + packName + " for level " + levelNumber);

        assetPackManager.getPackStates(Collections.singletonList(packName))
                .addOnSuccessListener(assetPackStates -> {
                    AssetPackState state = assetPackStates.packStates().get(packName);

                    if (state == null) {
                        callback.onError("Asset pack " + packName + " not found");
                        return;
                    }

                    int status = state.status();

                    switch (status) {
                        case AssetPackStatus.COMPLETED:
                            loadImageFromDownloadedPack(packName, levelNumber, maxSize, callback);
                            break;

                        case AssetPackStatus.DOWNLOADING:
                        case AssetPackStatus.TRANSFERRING:
                            listenForPackDownload(packName, levelNumber, maxSize, callback);
                            break;

                        default:
                            downloadAndLoadPack(packName, levelNumber, maxSize, callback);
                            break;
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get pack states", e);
                    callback.onError("Failed to check asset pack: " + e.getMessage());
                });
    }

    /**
     * Tính tên pack dựa trên level number
     */
    private String getPackNameForLevel(int levelNumber) {
        int packNumber = ((levelNumber - BUNDLED_LEVELS - 1) / LEVELS_PER_PACK) + 1;
        return String.format("%s%03d", PACK_PREFIX, packNumber);
    }

    /**
     * Download asset pack
     */
    private void downloadAndLoadPack(String packName, int levelNumber, int maxSize, ImageLoadCallback callback) {
        Log.d(TAG, "Requesting download for: " + packName);

        assetPackManager.fetch(Collections.singletonList(packName))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Download request successful for: " + packName);
                    listenForPackDownload(packName, levelNumber, maxSize, callback);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to request download", e);
                    callback.onError("Failed to download asset pack: " + e.getMessage());
                });
    }

    /**
     * Lắng nghe tiến trình download
     */
    private void listenForPackDownload(String packName, int levelNumber, int maxSize, ImageLoadCallback callback) {
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
                        callback.onDownloadProgress(progress);
                        Log.d(TAG, "Download progress: " + progress + "%");
                        break;

                    case AssetPackStatus.COMPLETED:
                        assetPackManager.unregisterListener(this);
                        loadImageFromDownloadedPack(packName, levelNumber, maxSize, callback);
                        break;

                    case AssetPackStatus.FAILED:
                        assetPackManager.unregisterListener(this);
                        callback.onError("Download failed for pack: " + packName);
                        break;

                    case AssetPackStatus.CANCELED:
                        assetPackManager.unregisterListener(this);
                        callback.onError("Download canceled");
                        break;
                }
            }
        };

        assetPackManager.registerListener(listener);
    }

    /**
     * Load ảnh từ pack đã download
     */
    private void loadImageFromDownloadedPack(String packName, int levelNumber, int maxSize, ImageLoadCallback callback) {
        new Thread(() -> {
            try {
                AssetPackLocation location = assetPackManager.getPackLocation(packName);

                if (location == null) {
                    postOnMain(() -> callback.onError("Pack location not found: " + packName));
                    return;
                }

                String fileName = String.format("level_%d.webp", levelNumber);
                String assetPath = PACK_ASSET_PATH + "/" + fileName;
                String fullPath = location.assetsPath() + "/" + assetPath;
                File imageFile = new File(fullPath);

                Log.d(TAG, "Loading from path: " + fullPath);

                if (!imageFile.exists()) {
                    postOnMain(() -> callback.onError("Image file not found: " + fullPath));
                    return;
                }

                FileInputStream fis = new FileInputStream(imageFile);
                Bitmap bitmap = decodeBitmapOptimized(fis, maxSize);
                fis.close();

                if (bitmap != null) {
                    postOnMain(() -> callback.onSuccess(bitmap));
                } else {
                    postOnMain(() -> callback.onError("Failed to decode bitmap from pack"));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error loading from asset pack", e);
                postOnMain(() -> callback.onError("Error loading image: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Decode bitmap với optimization (tương tự code cũ)
     */
    private Bitmap decodeBitmapOptimized(InputStream inputStream, int maxSize) throws Exception {
        // First decode để lấy dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        // Mark và reset stream
        inputStream.mark(inputStream.available());
        BitmapFactory.decodeStream(inputStream, null, options);
        inputStream.reset();

        int imageWidth = options.outWidth;
        int imageHeight = options.outHeight;

        Log.d(TAG, "Original image size: " + imageWidth + "x" + imageHeight);

        // Calculate inSampleSize
        if (imageWidth > maxSize || imageHeight > maxSize) {
            options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize);
            Log.d(TAG, "Scaling down with inSampleSize: " + options.inSampleSize);
        }

        // Decode thật
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        return BitmapFactory.decodeStream(inputStream, null, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    /**
     * Post callback lên main thread
     */
    private void postOnMain(Runnable runnable) {
        mainHandler.post(runnable);
    }

    /**
     * Kiểm tra xem level có cần download không
     */
    public boolean needsDownload(int levelNumber) {
        return levelNumber > BUNDLED_LEVELS;
    }

    /**
     * Hủy tất cả downloads
     */
    public void cancelDownloads() {
        Log.d(TAG, "Canceling all downloads");
    }
}