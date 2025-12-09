package com.example.puzzle_assemble_picture;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.app.Dialog;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import android.graphics.Bitmap;

public class GalleryActivity extends AppCompatActivity {

    private RecyclerView galleryRecyclerView;
    private RecyclerView achievementsRecyclerView;
    private TextView galleryTitle;
    private TextView achievementsTitle;
    private GameProgressManager progressManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        progressManager = new GameProgressManager(this);

        // DEBUG: Test ImageManager
        ImageManager imageManager = new ImageManager(this);
//        imageManager.debugListAvailableLevels();

        // DEBUG: Test unlock một vài pieces
        // Uncomment để test
        /*
        for (int i = 0; i < 15; i++) {
            progressManager.unlockGalleryPiece(i);
        }
        */

        // DEBUG: Log unlocked pieces
        List<Integer> unlockedPieces = progressManager.getGalleryPieces();
        Log.d("GalleryActivity", "📊 Total unlocked pieces: " + unlockedPieces.size());
        Log.d("GalleryActivity", "📊 Unlocked: " + unlockedPieces.toString());

        galleryTitle = findViewById(R.id.galleryTitle);
        achievementsTitle = findViewById(R.id.achievementsTitle);
        galleryRecyclerView = findViewById(R.id.galleryRecyclerView);
        achievementsRecyclerView = findViewById(R.id.achievementsRecyclerView);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        // Test long-press để unlock pieces
        findViewById(R.id.backButton).setOnLongClickListener(v -> {
            Log.d("GalleryActivity", "🔓 Force unlocking 15 pieces...");
            for (int i = 0; i < 15; i++) {
                progressManager.unlockGalleryPiece(i);
            }
            Toast.makeText(this, "Unlocked 15 pieces!", Toast.LENGTH_SHORT).show();
            recreate();
            return true;
        });

        galleryRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        List<Integer> galleryPieces = progressManager.getGalleryPieces();
        int totalPieces = GameProgressManager.MAX_LEVEL;

        List<GalleryPieceItem> pieceItems = new ArrayList<>();
        for (int i = 0; i < totalPieces; i++) {
            boolean unlocked = galleryPieces.contains(i);
            pieceItems.add(new GalleryPieceItem(i, unlocked));
        }

        GalleryPieceAdapter pieceAdapter = new GalleryPieceAdapter(pieceItems, this);
        pieceAdapter.setOnPieceClickListener(pieceId -> {
            Log.d("GalleryActivity", "🖼️ Clicked piece: " + pieceId);
            showFullImage(pieceId);
        });
        galleryRecyclerView.setAdapter(pieceAdapter);

        // ... rest of code ...
    }

    public static class GalleryPieceItem {
        public int pieceId;
        public boolean unlocked;

        public GalleryPieceItem(int pieceId, boolean unlocked) {
            this.pieceId = pieceId;
            this.unlocked = unlocked;
        }
    }

    private void showFullImage(int pieceId) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_image);

        ImageView fullImageView = dialog.findViewById(R.id.fullImageView);
        ImageView closeButton = dialog.findViewById(R.id.closeButton);

        try {
            ImageManager imageManager = new ImageManager(this);
            Bitmap fullImage = imageManager.loadLevelImage(pieceId);

            if (fullImage != null && !fullImage.isRecycled()) {
                fullImageView.setImageBitmap(fullImage);
            } else {
                Log.e("GalleryActivity", "Failed to load image for piece: " + pieceId);
                fullImageView.setImageResource(android.R.drawable.ic_menu_gallery);
                Toast.makeText(this, "Image not available", Toast.LENGTH_SHORT).show();
            }

            closeButton.setOnClickListener(v -> {
                try {
                    dialog.dismiss();
                    if (fullImage != null && !fullImage.isRecycled()) {
                        fullImage.recycle();
                    }
                } catch (Exception e) {
                    Log.e("GalleryActivity", "Error closing dialog", e);
                }
            });

            fullImageView.setOnClickListener(v -> {
                try {
                    dialog.dismiss();
                    if (fullImage != null && !fullImage.isRecycled()) {
                        fullImage.recycle();
                    }
                } catch (Exception e) {
                    Log.e("GalleryActivity", "Error closing dialog", e);
                }
            });

            dialog.show();

        } catch (Exception e) {
            Log.e("GalleryActivity", "Error showing full image", e);
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
        }
    }

    public static class AchievementItem {
        public int achievementId;
        public boolean unlocked;
        public int startPiece;
        public int endPiece;

        public AchievementItem(int achievementId, boolean unlocked, int startPiece, int endPiece) {
            this.achievementId = achievementId;
            this.unlocked = unlocked;
            this.startPiece = startPiece;
            this.endPiece = endPiece;
        }
    }
}