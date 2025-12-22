package com.example.puzzle_assemble_picture;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.android.play.core.assetpacks.AssetPackLocation;
import com.google.android.play.core.assetpacks.AssetPackManager;
import com.google.android.play.core.assetpacks.AssetPackManagerFactory;
import com.google.android.play.core.assetpacks.AssetPackState;

import java.io.File;
import java.io.InputStream;

public class ImageManager {
    private static final String TAG = "ImageManager";
    private static final int BUILTIN_LEVELS = 10;
    private static final int LEVELS_PER_PACK = 20;

    private final Context context;
    private final AssetPackManager assetPackManager;

    public ImageManager(Context context) {
        this.context = context;
        this.assetPackManager = AssetPackManagerFactory.getInstance(context);
    }

    /**
     * Load image cho level (1-based index)
     */
    public Bitmap loadLevelImage(int levelNumber) {
        Log.d(TAG, "Loading image for level: " + levelNumber);

        if (levelNumber <= BUILTIN_LEVELS) {
            // Level 1-10: từ app/src/main/assets/puzzles_bundled/
            return loadFromBundledAssets(levelNumber);
        } else {
            // Level 11+: từ asset pack
            return loadFromAssetPack(levelNumber);
        }
    }

    /**
     * Kiểm tra xem image cho level có tồn tại không
     */
    public boolean isImageAvailable(int levelNumber) {
        if (levelNumber <= BUILTIN_LEVELS) {
            return checkBundledAssetExists(levelNumber);
        } else {
            String packName = getPackNameForLevel(levelNumber);
            AssetPackLocation location = assetPackManager.getPackLocation(packName);
            boolean available = location != null;
            Log.d(TAG, "Pack " + packName + " available: " + available);
            return available;
        }
    }

    /**
     * Load từ app bundled assets (level 1-10)
     */
    private Bitmap loadFromBundledAssets(int levelNumber) {
        String assetPath = "puzzles_bundled/level_" + levelNumber + ".webp";

        try {
            AssetManager assets = context.getAssets();
            InputStream is = assets.open(assetPath);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();

            if (bitmap != null) {
                Log.d(TAG, "✅ Loaded from bundled assets: " + assetPath);
                return bitmap;
            } else {
                Log.e(TAG, "❌ Failed to decode: " + assetPath);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading bundled asset: " + assetPath, e);
        }

        return null;
    }

    /**
     * Check if bundled asset exists
     */
    private boolean checkBundledAssetExists(int levelNumber) {
        String assetPath = "puzzles_bundled/level_" + levelNumber + ".webp";

        try {
            AssetManager assets = context.getAssets();
            InputStream is = assets.open(assetPath);
            is.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Load từ asset pack (level 11+)
     */
    private Bitmap loadFromAssetPack(int levelNumber) {
        String packName = getPackNameForLevel(levelNumber);

        try {
            // Get pack location
            AssetPackLocation location = assetPackManager.getPackLocation(packName);

            if (location == null) {
                Log.e(TAG, "❌ Asset pack not available: " + packName);
                return null;
            }

            // Get asset path
            String assetPath = location.assetsPath();
            File assetFile = new File(assetPath, "puzzles/level_" + levelNumber + ".webp");

            if (!assetFile.exists()) {
                Log.e(TAG, "❌ File not found: " + assetFile.getAbsolutePath());
                return null;
            }

            // Load bitmap
            Bitmap bitmap = BitmapFactory.decodeFile(assetFile.getAbsolutePath());

            if (bitmap != null) {
                Log.d(TAG, "✅ Loaded from asset pack: " + packName + " → " + assetFile.getName());
                return bitmap;
            } else {
                Log.e(TAG, "❌ Failed to decode from pack: " + assetFile.getAbsolutePath());
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error loading from pack " + packName + " level " + levelNumber, e);
        }

        return null;
    }

    /**
     * Get pack name cho level
     */
    private String getPackNameForLevel(int levelNumber) {
        int packNumber = ((levelNumber - BUILTIN_LEVELS - 1) / LEVELS_PER_PACK) + 1;
        String packName = String.format("puzzlepack_%03d", packNumber);

        Log.d(TAG, "📦 Level " + levelNumber + " → " + packName);
        return packName;
    }

    /**
     * Load thumbnail (scaled down version)
     */
    public Bitmap loadThumbnail(int levelNumber, int targetWidth, int targetHeight) {
        Bitmap fullImage = loadLevelImage(levelNumber);

        if (fullImage == null) {
            Log.e(TAG, "❌ Cannot load image for thumbnail, level: " + levelNumber);
            return null;
        }

        try {
            int width = fullImage.getWidth();
            int height = fullImage.getHeight();

            float scale = Math.min(
                    (float) targetWidth / width,
                    (float) targetHeight / height
            );

            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);

            Bitmap thumbnail = Bitmap.createScaledBitmap(
                    fullImage, newWidth, newHeight, true);

            if (thumbnail != fullImage) {
                fullImage.recycle();
            }

            Log.d(TAG, "✅ Created thumbnail for level " + levelNumber);
            return thumbnail;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error creating thumbnail for level " + levelNumber, e);
            return fullImage;
        }
    }

    /**
     * Get pack states (for debugging)
     */
    public void debugPackStates() {
        Log.d(TAG, "=== Asset Pack States ===");

        for (int i = 1; i <= 15; i++) {
            String packName = getPackNameForLevel(i + BUILTIN_LEVELS);
            AssetPackLocation location = assetPackManager.getPackLocation(packName);

            if (location != null) {
                Log.d(TAG, "✅ " + packName + " → " + location.assetsPath());
            } else {
                Log.d(TAG, "❌ " + packName + " → NOT INSTALLED");
            }
        }
    }

    public void debugListAvailableLevels() {
        Log.d(TAG, "=== Debug: Available Levels ===");

        // Check bundled levels (1-10)
        for (int i = 1; i <= BUILTIN_LEVELS; i++) {
            boolean available = isImageAvailable(i);
            Log.d(TAG, "Level " + i + " (bundled): " + available);
        }

        // Check asset pack levels
        debugPackStates();
    }

    /**
     * Check pack status
     */
    public int getPackStatus(String packName) {
        AssetPackLocation location = assetPackManager.getPackLocation(packName);
//        return location != null ? AssetPackState.STATUS_COMPLETED : AssetPackState.STATUS_UNKNOWN;
        return location != null ? 4 : 0; // 4 = COMPLETED, 0 = UNKNOWN
    }
}