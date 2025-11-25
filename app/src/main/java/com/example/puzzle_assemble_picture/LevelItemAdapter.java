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

public class LevelItemAdapter extends RecyclerView.Adapter<LevelItemAdapter.ViewHolder> {

    private final List<LevelSelectionActivity.LevelItem> items;
    private final OnLevelClickListener listener;

    public interface OnLevelClickListener {
        void onLevelClick(LevelSelectionActivity.LevelItem item);
    }

    public LevelItemAdapter(List<LevelSelectionActivity.LevelItem> items, OnLevelClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_level, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LevelSelectionActivity.LevelItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView levelNumber;
        TextView gridSize;
        ImageView lockIcon;
        ImageView checkIcon;
        ImageView saveIcon;
        View overlay;

        ViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.levelCard);
            levelNumber = itemView.findViewById(R.id.levelNumber);
            gridSize = itemView.findViewById(R.id.gridSize);
            lockIcon = itemView.findViewById(R.id.lockIcon);
            checkIcon = itemView.findViewById(R.id.checkIcon);
            saveIcon = itemView.findViewById(R.id.saveIcon);
            overlay = itemView.findViewById(R.id.overlay);
        }

        void bind(LevelSelectionActivity.LevelItem item, OnLevelClickListener listener) {
            levelNumber.setText(String.valueOf(item.levelNumber));
            gridSize.setText(item.gridSize + "x" + item.gridSize);

            if (item.isCompleted) {
                // Completed level - green with check mark
                card.setCardBackgroundColor(Color.parseColor("#4CAF50"));
                card.setAlpha(1.0f);
                levelNumber.setTextColor(Color.WHITE);
                gridSize.setTextColor(Color.WHITE);
                lockIcon.setVisibility(View.GONE);
                checkIcon.setVisibility(View.VISIBLE);
                saveIcon.setVisibility(item.hasSave ? View.VISIBLE : View.GONE);
                overlay.setVisibility(View.GONE);
            } else if (item.isUnlocked) {
                // Current unlocked level - blue, no lock
                card.setCardBackgroundColor(Color.parseColor("#2196F3"));
                card.setAlpha(1.0f);
                levelNumber.setTextColor(Color.WHITE);
                gridSize.setTextColor(Color.WHITE);
                lockIcon.setVisibility(View.GONE);
                checkIcon.setVisibility(View.GONE);
                saveIcon.setVisibility(item.hasSave ? View.VISIBLE : View.GONE);
                overlay.setVisibility(View.GONE);
            } else {
                // Locked level - blue with lock icon and overlay
                card.setCardBackgroundColor(Color.parseColor("#2196F3"));
                card.setAlpha(0.6f);
                levelNumber.setTextColor(Color.WHITE);
                gridSize.setTextColor(Color.WHITE);
                lockIcon.setVisibility(View.VISIBLE);
                checkIcon.setVisibility(View.GONE);
                saveIcon.setVisibility(View.GONE);
                overlay.setVisibility(View.VISIBLE);
            }

            card.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLevelClick(item);
                }
            });
        }
    }
}