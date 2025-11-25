package com.example.puzzle_assemble_picture;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {

    private final List<GalleryActivity.AchievementItem> items;
    private final GameProgressManager progressManager;

    public AchievementAdapter(List<GalleryActivity.AchievementItem> items, GameProgressManager progressManager) {
        this.items = items;
        this.progressManager = progressManager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_achievement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GalleryActivity.AchievementItem item = items.get(position);

        holder.titleText.setText("Achievement #" + (item.achievementId + 1));

        if (item.unlocked) {
            holder.card.setAlpha(1.0f);
            holder.lockIcon.setVisibility(View.GONE);
            holder.statusText.setText("✓ Unlocked!");
            holder.statusText.setTextColor(Color.parseColor("#4CAF50"));
        } else {
            holder.card.setAlpha(0.6f);
            holder.lockIcon.setVisibility(View.VISIBLE);

            List<Integer> galleryPieces = progressManager.getGalleryPieces();
            int collected = 0;
            for (int i = item.startPiece; i <= item.endPiece; i++) {
                if (galleryPieces.contains(i)) {
                    collected++;
                }
            }
            int total = item.endPiece - item.startPiece + 1;
            holder.statusText.setText(String.format("Progress: %d/%d pieces", collected, total));
            holder.statusText.setTextColor(Color.parseColor("#FF9800"));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView titleText;
        TextView statusText;
        ImageView lockIcon;

        ViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.achievementCard);
            titleText = itemView.findViewById(R.id.achievementTitle);
            statusText = itemView.findViewById(R.id.achievementStatus);
            lockIcon = itemView.findViewById(R.id.lockIcon);
        }
    }
}