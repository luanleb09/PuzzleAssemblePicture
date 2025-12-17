package com.example.puzzle_assemble_picture;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private static final String TAG = "GalleryActivity";

    private RecyclerView galleryRecyclerView;
    private TextView progressText;
    private ProgressBar progressBar;
    private GameProgressManager progressManager;
    private PuzzleImageLoader imageLoader;
    private GalleryAdapter galleryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        try {
            progressManager = new GameProgressManager(this);
            imageLoader = new PuzzleImageLoader(this);

            // Initialize views with null checks
            galleryRecyclerView = findViewById(R.id.galleryRecyclerView);
            progressText = findViewById(R.id.progressText);
            progressBar = findViewById(R.id.progressBar);

            // Verify all views are found
            if (galleryRecyclerView == null) {
                Log.e(TAG, "❌ galleryRecyclerView not found in layout!");
                Toast.makeText(this, "Layout error: RecyclerView missing", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            if (progressText == null) {
                Log.e(TAG, "❌ progressText not found in layout!");
                Toast.makeText(this, "Layout error: ProgressText missing", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            if (progressBar == null) {
                Log.e(TAG, "❌ progressBar not found in layout!");
                Toast.makeText(this, "Layout error: ProgressBar missing", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            findViewById(R.id.backButton).setOnClickListener(v -> finish());

            // Setup grid layout (3 columns)
            galleryRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));
            galleryRecyclerView.setHasFixedSize(true);

            // Load gallery
            loadGallery();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
            Toast.makeText(this, "Error loading gallery: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadGallery() {
        try {
            List<GalleryItem> galleryItems = createGalleryItems();

            // ✅ Pass imageLoader to adapter
            galleryAdapter = new GalleryAdapter(galleryItems, this::onGalleryItemClick, imageLoader);
            galleryRecyclerView.setAdapter(galleryAdapter);

            updateProgress(galleryItems);
        } catch (Exception e) {
            Log.e(TAG, "Error loading gallery", e);
            Toast.makeText(this, "Failed to load gallery", Toast.LENGTH_SHORT).show();
        }
    }

    private List<GalleryItem> createGalleryItems() {
        List<GalleryItem> items = new ArrayList<>();

        // Total 100 pieces (levels 1-100, can be expanded to 300 later)
        int totalPieces = 100;

        for (int i = 0; i < totalPieces; i++) {
            boolean isUnlocked = progressManager.isGalleryPieceUnlocked(i);
            items.add(new GalleryItem(i, isUnlocked));
        }

        return items;
    }

    private void onGalleryItemClick(GalleryItem item) {
        if (!item.isUnlocked) {
            showLockedDialog(item);
            return;
        }

        // Show full image
        showFullImage(item.pieceIndex + 1); // Level = pieceIndex + 1
    }

    private void showLockedDialog(GalleryItem item) {
        int requiredLevel = item.pieceIndex + 1;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔒 Locked");
        builder.setMessage("Complete Level " + requiredLevel + " in any mode to unlock this image!");
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void showFullImage(int level) {
        try {
            // Create fullscreen dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_full_image, null);

            ImageView fullImageView = dialogView.findViewById(R.id.fullImageView);
            ProgressBar loadingBar = dialogView.findViewById(R.id.loadingProgressBar);
            TextView levelText = dialogView.findViewById(R.id.levelText);

            if (fullImageView == null || loadingBar == null || levelText == null) {
                Toast.makeText(this, "Dialog layout error", Toast.LENGTH_SHORT).show();
                return;
            }

            levelText.setText("Level " + level);

            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            // Load full resolution image
            loadingBar.setVisibility(View.VISIBLE);
            fullImageView.setVisibility(View.GONE);

            imageLoader.loadLevelImage(level, new PuzzleImageLoader.ImageLoadCallback() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    runOnUiThread(() -> {
                        if (dialog.isShowing()) {
                            loadingBar.setVisibility(View.GONE);
                            fullImageView.setVisibility(View.VISIBLE);
                            fullImageView.setImageBitmap(bitmap);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        if (dialog.isShowing()) {
                            loadingBar.setVisibility(View.GONE);
                            levelText.setText("Failed to load image");
                        }
                    });
                }

                @Override
                public void onDownloadProgress(int progress) {
                    runOnUiThread(() -> {
                        if (dialog.isShowing() && loadingBar.getVisibility() == View.VISIBLE) {
                            // Can update loading text if needed
                            levelText.setText("Loading... " + progress + "%");
                        }
                    });
                }
            });

            // Click to dismiss
            dialogView.setOnClickListener(v -> dialog.dismiss());

            dialog.show();

        } catch (Exception e) {
            Log.e(TAG, "Error showing full image", e);
            Toast.makeText(this, "Failed to show image", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProgress(List<GalleryItem> items) {
        try {
            int unlockedCount = 0;
            int totalCount = items.size();

            for (GalleryItem item : items) {
                if (item.isUnlocked) {
                    unlockedCount++;
                }
            }

            int percentage = totalCount > 0 ? (unlockedCount * 100) / totalCount : 0;

            if (progressText != null) {
                progressText.setText(unlockedCount + "/" + totalCount + " unlocked");
            }

            if (progressBar != null) {
                progressBar.setProgress(percentage);
            }

            Log.d(TAG, "Gallery progress: " + unlockedCount + "/" + totalCount + " (" + percentage + "%)");

        } catch (Exception e) {
            Log.e(TAG, "Error updating progress", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Cleanup adapter bitmaps
        if (galleryAdapter != null) {
            galleryAdapter.cleanup();
        }

        // Cleanup image loader
        if (imageLoader != null) {
            imageLoader.cancelDownloads();
        }
    }

    // Gallery Item class
    public static class GalleryItem {
        public int pieceIndex;
        public boolean isUnlocked;

        public GalleryItem(int pieceIndex, boolean isUnlocked) {
            this.pieceIndex = pieceIndex;
            this.isUnlocked = isUnlocked;
        }
    }
}