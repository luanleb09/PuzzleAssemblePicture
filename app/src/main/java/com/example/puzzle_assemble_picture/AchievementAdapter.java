package com.example.puzzle_assemble_picture;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.ViewHolder> {

    private final List<AchievementItem> items;
    private final GameProgressManager progressManager;

    public AchievementAdapter(List<AchievementItem> items, GameProgressManager progressManager) {
        this.items = items;
        this.progressManager = progressManager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false); // ✅ USE SIMPLE LAYOUT
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AchievementItem item = items.get(position);

        // ✅ SIMPLE: Chỉ hiển thị text
        int progress = getProgressForAchievement(item);
        boolean isUnlocked = (progress >= item.targetValue);

        String status = isUnlocked ? "✅" : "🔒";
        String text = status + " " + item.icon + " " + item.title + "\n" +
                item.description + " (" + progress + "/" + item.targetValue + ")";

        holder.textView.setText(text);
        holder.itemView.setAlpha(isUnlocked ? 1.0f : 0.6f);
    }

    private int getProgressForAchievement(AchievementItem item) {
        switch (item.id) {
            case "first_win":
                return progressManager.getTotalCompletedLevelsAllModes() > 0 ? 1 : 0;
            case "complete_10":
                return Math.min(progressManager.getTotalCompletedLevelsAllModes(), 10);
            case "complete_50":
                return Math.min(progressManager.getTotalCompletedLevelsAllModes(), 50);
            case "complete_100":
                return Math.min(progressManager.getTotalCompletedLevelsAllModes(), 100);
            case "master_easy":
                // ✅ FIX: Use correct method
                return getCompletedForMode(GameMode.MODE_EASY);
            case "master_normal":
                return getCompletedForMode(GameMode.MODE_NORMAL);
            case "master_hard":
                return getCompletedForMode(GameMode.MODE_HARD);
            case "master_insane":
                return getCompletedForMode(GameMode.MODE_INSANE);
            default:
                return 0;
        }
    }

    /**
     * ✅ HELPER: Get completed count for a specific mode
     */
    private int getCompletedForMode(String mode) {
        int count = 0;
        for (int level = 1; level <= GameProgressManager.MAX_LEVEL; level++) {
            if (progressManager.isLevelCompleted(mode, level)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1); // ✅ USE ANDROID ID
        }
    }

    /**
     * AchievementItem data class
     */
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