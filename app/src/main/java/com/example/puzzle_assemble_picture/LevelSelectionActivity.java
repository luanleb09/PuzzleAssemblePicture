package com.example.puzzle_assemble_picture;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class LevelSelectionActivity extends AppCompatActivity {

    private RecyclerView levelRecyclerView;
    private TextView titleText;
    private GameProgressManager progressManager;
    private String selectedMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_selection);

        progressManager = new GameProgressManager(this);
        selectedMode = getIntent().getStringExtra("MODE");

        titleText = findViewById(R.id.titleText);
        levelRecyclerView = findViewById(R.id.levelRecyclerView);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        String modeDisplayName = getModeDisplayName(selectedMode);
        titleText.setText(modeDisplayName + " - Select Level");

        levelRecyclerView.setLayoutManager(new GridLayoutManager(this, 5));

        // UPDATED: Tạo level items dựa trên mode
        List<LevelItem> levelItems = createLevelItems();

        LevelItemAdapter adapter = new LevelItemAdapter(levelItems, this::onLevelSelected);
        levelRecyclerView.setAdapter(adapter);
    }

    private String getModeDisplayName(String mode) {
        switch (mode) {
            case GameMode.MODE_EASY: return "🟢 Easy Mode";
            case GameMode.MODE_NORMAL: return "🟡 Normal Mode";
            case GameMode.MODE_HARD: return "🔵 Hard Mode";
            case GameMode.MODE_INSANE: return "🔴 Insane Mode";
            default: return "Select Level";
        }
    }

    private List<LevelItem> createLevelItems() {
        List<LevelItem> items = new ArrayList<>();

        // UPDATED: Lấy current level của MODE hiện tại
        int currentLevel = progressManager.getCurrentLevel(selectedMode);

        for (int i = 1; i <= GameProgressManager.MAX_LEVEL; i++) {
            // UPDATED: Check completed theo MODE
            boolean isCompleted = progressManager.isLevelCompleted(selectedMode, i);
            boolean isUnlocked = i <= currentLevel;
            boolean hasSave = progressManager.hasSavedGame(selectedMode, i);
            int gridSize = progressManager.getGridSizeForLevel(i);

            items.add(new LevelItem(i, isCompleted, isUnlocked, hasSave, gridSize));
        }

        return items;
    }

    private void onLevelSelected(LevelItem item) {
        if (!item.isUnlocked) {
            android.widget.Toast.makeText(this,
                    "Complete Level " + (item.levelNumber - 1) + " to unlock!",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // Start game với level và mode đã chọn
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("LEVEL", item.levelNumber);
        intent.putExtra("MODE", selectedMode);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    public static class LevelItem {
        public int levelNumber;
        public boolean isCompleted;
        public boolean isUnlocked;
        public boolean hasSave;
        public int gridSize;

        public LevelItem(int levelNumber, boolean isCompleted, boolean isUnlocked, boolean hasSave, int gridSize) {
            this.levelNumber = levelNumber;
            this.isCompleted = isCompleted;
            this.isUnlocked = isUnlocked;
            this.hasSave = hasSave;
            this.gridSize = gridSize;
        }
    }
}