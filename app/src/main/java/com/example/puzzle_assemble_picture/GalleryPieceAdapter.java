package com.example.puzzle_assemble_picture;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class GalleryPieceAdapter extends RecyclerView.Adapter<GalleryPieceAdapter.ViewHolder> {

    private final List<GalleryActivity.GalleryPieceItem> items;
    private final Context context;
    private final ImageManager imageManager;
    private OnPieceClickListener listener;

    public interface OnPieceClickListener {
        void onPieceClick(int pieceId);
    }

    public GalleryPieceAdapter(List<GalleryActivity.GalleryPieceItem> items, Context context) {
        this.items = items;
        this.context = context;
        this.imageManager = new ImageManager(context);
    }

    public void setOnPieceClickListener(OnPieceClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery_piece, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GalleryActivity.GalleryPieceItem item = items.get(position);
        int levelNumber = item.pieceId + 1; // Convert 0-based to 1-based

        if (item.unlocked) {
            // Load thumbnail (200x200 để tiết kiệm memory)
            Bitmap thumbnail = imageManager.loadThumbnail(levelNumber, 200, 200);

            if (thumbnail != null) {
                holder.pieceImageView.setImageBitmap(thumbnail);
            } else {
                // Fallback: màu tím nếu không load được
                holder.pieceImageView.setImageResource(android.R.color.holo_purple);
            }

            holder.lockedOverlay.setVisibility(View.GONE);
            holder.lockIcon.setVisibility(View.GONE);
            holder.card.setAlpha(1.0f);

            // Click to show full image
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPieceClick(levelNumber);
                }
            });
        } else {
            // Locked state
            holder.pieceImageView.setImageResource(android.R.color.darker_gray);
            holder.lockedOverlay.setVisibility(View.VISIBLE);
            holder.lockIcon.setVisibility(View.VISIBLE);
            holder.card.setAlpha(0.5f);
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView pieceImageView;
        View lockedOverlay;
        ImageView lockIcon;

        ViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.pieceCard);
            pieceImageView = itemView.findViewById(R.id.pieceImageView);
            lockedOverlay = itemView.findViewById(R.id.lockedOverlay);
            lockIcon = itemView.findViewById(R.id.lockIcon);
        }
    }
}