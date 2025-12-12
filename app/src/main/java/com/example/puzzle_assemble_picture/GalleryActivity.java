package com.example.puzzle_assemble_picture;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.ads.AdView;

public class GalleryActivity extends AppCompatActivity {

    private static final String TAG = "GalleryActivity";

    private RecyclerView galleryRecyclerView;
    private GameProgressManager progressManager;
    private PuzzleImageLoader imageLoader;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_gallery);

            progressManager = new GameProgressManager(this);
            imageLoader = new PuzzleImageLoader(this);

            galleryRecyclerView = findViewById(R.id.galleryRecyclerView);

            if (galleryRecyclerView == null) {
                Log.e(TAG, "❌ galleryRecyclerView not found in layout");
                Toast.makeText(this, "Error: Gallery view not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            galleryRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
            galleryRecyclerView.setHasFixedSize(true);

            if (findViewById(R.id.backButton) != null) {
                findViewById(R.id.backButton).setOnClickListener(v -> finish());
            }

            List<GalleryItem> items = createGalleryItems();

            if (items == null || items.isEmpty()) {
                Log.w(TAG, "⚠️ No gallery items available");
                Toast.makeText(this, "No puzzle pieces unlocked yet", Toast.LENGTH_SHORT).show();
            }

            // ✅ FIX: Use GalleryPieceAdapter
            GalleryPieceAdapter adapter = new GalleryPieceAdapter(items, this, this::onPieceClicked);
            galleryRecyclerView.setAdapter(adapter);

            adView = findViewById(R.id.adView);
            if (adView != null) {
                AdMobHelper.loadBannerAd(adView);
            }

            Log.d(TAG, "✅ GalleryActivity created successfully with " + items.size() + " items");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error in GalleryActivity onCreate", e);
            Toast.makeText(this, "Error loading gallery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private List<GalleryItem> createGalleryItems() {
        List<GalleryItem> items = new ArrayList<>();

        try {
            int totalPieces = 100;

            for (int i = 0; i < totalPieces; i++) {
                boolean isUnlocked = progressManager.isGalleryPieceUnlocked(i);

                int imageResId = getResources().getIdentifier(
                        "level_" + (i + 1),
                        "drawable",
                        getPackageName()
                );

                if (imageResId != 0 || !isUnlocked) {
                    items.add(new GalleryItem(i, isUnlocked, imageResId));
                } else {
                    Log.w(TAG, "⚠️ Image resource not found for piece " + i);
                }
            }

            Log.d(TAG, "✅ Created " + items.size() + " gallery items");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error creating gallery items", e);
        }

        return items;
    }

    private void onPieceClicked(GalleryItem item) {
        try {
            if (item == null) {
                Log.e(TAG, "Clicked item is null");
                return;
            }

            if (!item.isUnlocked) {
                Toast.makeText(this,
                        "🔒 Complete Level " + (item.pieceId + 1) + " to unlock!",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            showFullImageDialog(item);

        } catch (Exception e) {
            Log.e(TAG, "Error handling piece click", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showFullImageDialog(GalleryItem item) {
        try {
            android.app.Dialog dialog = new android.app.Dialog(
                    this,
                    android.R.style.Theme_Black_NoTitleBar_Fullscreen
            );
            dialog.setContentView(R.layout.dialog_full_image);

            android.widget.ImageView fullImageView = dialog.findViewById(R.id.fullImageView);

            // ✅ FIX: Try different ID for close button
            android.widget.ImageView closeButton = dialog.findViewById(R.id.closeButton);
            if (closeButton == null) {
                closeButton = dialog.findViewById(R.id.btnClose);
            }

            if (fullImageView == null) {
                Log.e(TAG, "fullImageView not found in dialog");
                Toast.makeText(this, "Error loading image view", Toast.LENGTH_SHORT).show();
                return;
            }

            if (item.imageResId != 0) {
                fullImageView.setImageResource(item.imageResId);
            } else {
                int levelNumber = item.pieceId + 1;
                imageLoader.loadLevelImage(levelNumber, new PuzzleImageLoader.ImageLoadCallback() {
                    @Override
                    public void onSuccess(android.graphics.Bitmap bitmap) {
                        runOnUiThread(() -> {
                            if (bitmap != null) {
                                fullImageView.setImageBitmap(bitmap);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(GalleryActivity.this,
                                    "Failed to load image: " + error,
                                    Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        });
                    }

                    @Override
                    public void onDownloadProgress(int progress) {
                        // Silent
                    }
                });
            }

            if (closeButton != null) {
                closeButton.setOnClickListener(v -> dialog.dismiss());
            }

            fullImageView.setOnClickListener(v -> dialog.dismiss());

            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing full image dialog", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (adView != null) {
            adView.destroy();
        }

        if (imageLoader != null) {
            imageLoader.cancelDownloads();
        }
    }

    // ✅ Data classes
    public static class GalleryItem {
        public int pieceId;
        public boolean isUnlocked;
        public int imageResId;

        public GalleryItem(int pieceId, boolean isUnlocked, int imageResId) {
            this.pieceId = pieceId;
            this.isUnlocked = isUnlocked;
            this.imageResId = imageResId;
        }
    }

    // ✅ Backward compatibility
    public static class GalleryPieceItem extends GalleryItem {
        public GalleryPieceItem(int pieceId, boolean isUnlocked, int imageResId) {
            super(pieceId, isUnlocked, imageResId);
        }
    }

    public static class AchievementItem {
        public String id;
        public String icon;
        public String title;
        public String description;
        public int targetValue;

        public AchievementItem(String id, String icon, String title, String description, int targetValue) {
            this.id = id;
            this.icon = icon;
            this.title = title;
            this.description = description;
            this.targetValue = targetValue;
        }
    }
}