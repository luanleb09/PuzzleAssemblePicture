package com.example.puzzle_assemble_picture;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {

    private static final String TAG = "GalleryAdapter";

    private final List<GalleryActivity.GalleryItem> items;
    private final OnGalleryItemClickListener listener;
    private final PuzzleImageLoader imageLoader;
    private final Map<Integer, Bitmap> thumbnailCache = new HashMap<>();

    public interface OnGalleryItemClickListener {
        void onItemClick(GalleryActivity.GalleryItem item);
    }

    public GalleryAdapter(List<GalleryActivity.GalleryItem> items,
                          OnGalleryItemClickListener listener,
                          PuzzleImageLoader imageLoader) {
        this.items = items;
        this.listener = listener;
        this.imageLoader = imageLoader;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery_piece, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        GalleryActivity.GalleryItem item = items.get(position);
        holder.bind(item, listener, imageLoader, thumbnailCache);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void cleanup() {
        // Recycle cached bitmaps
        for (Bitmap bitmap : thumbnailCache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        thumbnailCache.clear();
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final ImageView imageView;
        private final ImageView lockIcon;
        private final TextView pieceNumberText;
        private final ProgressBar loadingBar;

        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.galleryCard);
            imageView = itemView.findViewById(R.id.pieceImage);
            lockIcon = itemView.findViewById(R.id.lockIcon);
            pieceNumberText = itemView.findViewById(R.id.pieceNumberText);
            loadingBar = itemView.findViewById(R.id.loadingBar);
        }

        public void bind(GalleryActivity.GalleryItem item,
                         OnGalleryItemClickListener listener,
                         PuzzleImageLoader imageLoader,
                         Map<Integer, Bitmap> thumbnailCache) {

            int levelNumber = item.pieceIndex + 1;
            pieceNumberText.setText(String.valueOf(levelNumber));

            if (item.isUnlocked) {
                // Show unlocked state
                lockIcon.setVisibility(View.GONE);
                imageView.setAlpha(1.0f);
                cardView.setAlpha(1.0f);

                // Check if thumbnail is already cached
                if (thumbnailCache.containsKey(item.pieceIndex)) {
                    Bitmap cachedBitmap = thumbnailCache.get(item.pieceIndex);
                    if (cachedBitmap != null && !cachedBitmap.isRecycled()) {
                        imageView.setImageBitmap(cachedBitmap);
                        loadingBar.setVisibility(View.GONE);
                    } else {
                        loadThumbnail(item, imageLoader, thumbnailCache);
                    }
                } else {
                    loadThumbnail(item, imageLoader, thumbnailCache);
                }

            } else {
                // Show locked state
                lockIcon.setVisibility(View.VISIBLE);
                imageView.setAlpha(0.3f);
                cardView.setAlpha(0.6f);
                loadingBar.setVisibility(View.GONE);

                // Gray background for locked
                imageView.setImageDrawable(null);
                imageView.setBackgroundColor(0xFF999999);
            }

            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }

        private void loadThumbnail(GalleryActivity.GalleryItem item,
                                   PuzzleImageLoader imageLoader,
                                   Map<Integer, Bitmap> thumbnailCache) {

            int levelNumber = item.pieceIndex + 1;

            loadingBar.setVisibility(View.VISIBLE);
            imageView.setImageDrawable(null);

            imageLoader.loadLevelImage(levelNumber, new PuzzleImageLoader.ImageLoadCallback() {
                @Override
                public void onSuccess(Bitmap bitmap) {
                    if (bitmap != null && !bitmap.isRecycled()) {
                        // Create thumbnail (scale down)
                        Bitmap thumbnail = createThumbnail(bitmap);

                        // Cache the thumbnail
                        thumbnailCache.put(item.pieceIndex, thumbnail);

                        // Update UI
                        imageView.post(() -> {
                            loadingBar.setVisibility(View.GONE);
                            imageView.setImageBitmap(thumbnail);
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e("GalleryAdapter", "Failed to load thumbnail for level " + levelNumber + ": " + error);
                    imageView.post(() -> {
                        loadingBar.setVisibility(View.GONE);
                        imageView.setBackgroundColor(0xFFCCCCCC);
                    });
                }

                @Override
                public void onDownloadProgress(int progress) {
                    // Optional: show progress
                }
            });
        }

        private Bitmap createThumbnail(Bitmap original) {
            // Scale down to thumbnail size (e.g., 200x200)
            int targetSize = 300;

            int width = original.getWidth();
            int height = original.getHeight();

            float scale = Math.min((float) targetSize / width, (float) targetSize / height);

            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);

            return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
        }
    }
}