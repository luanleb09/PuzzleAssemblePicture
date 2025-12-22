package com.example.puzzle_assemble_picture;

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

        holder.levelNumber.setText(String.valueOf(item.levelNumber));
        holder.gridSize.setText(item.gridSize + "×" + item.gridSize);

        // Reset visibility
        holder.lockIcon.setVisibility(View.GONE);
        holder.checkIcon.setVisibility(View.GONE);
        holder.saveIcon.setVisibility(View.GONE);
        holder.downloadIcon.setVisibility(View.GONE);
        holder.overlay.setVisibility(View.GONE);

        // Xử lý trạng thái level
        if (!item.isUnlocked) {
            // Level bị khóa
            holder.cardView.setCardBackgroundColor(0xFF424242);
            holder.levelNumber.setTextColor(0xFF757575);
            holder.gridSize.setTextColor(0xFF757575);
//            holder.lockIcon.setVisibility(View.VISIBLE);
//            holder.overlay.setVisibility(View.VISIBLE);
            holder.lockIcon.setVisibility(View.GONE);
            holder.overlay.setVisibility(View.GONE);

        } else if (item.isCompleted) {
            // Level đã hoàn thành
            holder.cardView.setCardBackgroundColor(0xFF4CAF50);
            holder.levelNumber.setTextColor(0xFFFFFFFF);
            holder.gridSize.setTextColor(0xFFE8F5E9);
//            holder.checkIcon.setVisibility(View.VISIBLE);
            holder.checkIcon.setVisibility(View.GONE);

            // Hiển thị save icon nếu có
            if (item.hasSave) {
                holder.saveIcon.setVisibility(View.VISIBLE);
            }

        } else {
            // Level đang chơi (unlocked nhưng chưa hoàn thành)
            holder.cardView.setCardBackgroundColor(0xFF2196F3);
            holder.levelNumber.setTextColor(0xFFFFFFFF);
            holder.gridSize.setTextColor(0xFFBBDEFB);

            // Hiển thị save icon nếu có
            if (item.hasSave) {
                holder.saveIcon.setVisibility(View.VISIBLE);
            }

            // THÊM: Hiển thị download icon nếu cần
            if (item.needsDownload) {
                holder.downloadIcon.setVisibility(View.VISIBLE);
            }
        }

        holder.cardView.setOnClickListener(v -> listener.onLevelClick(item));
    }

//    @Override
//    public void onBindViewHolder(@NonNull LevelViewHolder holder, int position) {
//        LevelItem item = items.get(position);
//
//        holder.levelNumber.setText(String.valueOf(item.levelNumber));
//        holder.gridSize.setText(item.gridSize + "x" + item.gridSize);
//
//        // Show/hide icons
//        holder.completedIcon.setVisibility(item.isCompleted ? View.VISIBLE : View.GONE);
//        holder.savedIcon.setVisibility(item.hasSave ? View.VISIBLE : View.GONE);
//        holder.downloadIcon.setVisibility(item.needsDownload ? View.VISIBLE : View.GONE);
//
//        // Lock overlay
//        holder.lockOverlay.setVisibility(item.isUnlocked ? View.GONE : View.VISIBLE);
//
//        // Click listener
//        holder.itemView.setOnClickListener(v -> {
//            if (listener != null) {
//                listener.onLevelClick(item);
//            }
//        });
//
//        // ✅ Add ripple/press effect even with custom background
//        holder.itemView.setClickable(true);
//        holder.itemView.setFocusable(true);
//
//        // Optional: Add scale animation on press
//        holder.itemView.setOnTouchListener((v, event) -> {
//            switch (event.getAction()) {
//                case android.view.MotionEvent.ACTION_DOWN:
//                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
//                    break;
//                case android.view.MotionEvent.ACTION_UP:
//                case android.view.MotionEvent.ACTION_CANCEL:
//                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
//                    break;
//            }
//            return false;
//        });
//    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView levelNumber;
        TextView gridSize;
        ImageView lockIcon;
        ImageView checkIcon;
        ImageView saveIcon;
        ImageView downloadIcon;
        View overlay;

        ViewHolder(View view) {
            super(view);
            cardView = view.findViewById(R.id.levelCard);
            levelNumber = view.findViewById(R.id.levelNumber);
            gridSize = view.findViewById(R.id.gridSize);
            lockIcon = view.findViewById(R.id.lockIcon);
            checkIcon = view.findViewById(R.id.checkIcon);
            saveIcon = view.findViewById(R.id.saveIcon);
            downloadIcon = view.findViewById(R.id.downloadIcon);
            overlay = view.findViewById(R.id.overlay);
        }
    }
}