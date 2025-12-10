package com.example.puzzle_assemble_picture;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.ads.AdView;

public class LevelSelectionActivity extends AppCompatActivity {

    private RecyclerView levelRecyclerView;
    private TextView titleText;
    private TextView coinCountText;
    private GameProgressManager progressManager;
    private PuzzleImageLoader imageLoader;
    private CoinManager coinManager;
    private String selectedMode;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_level_selection);

            progressManager = new GameProgressManager(this);
            imageLoader = new PuzzleImageLoader(this);
            coinManager = new CoinManager(this);
            selectedMode = getIntent().getStringExtra("MODE");

            titleText = findViewById(R.id.titleText);
            coinCountText = findViewById(R.id.coinCountText);
            levelRecyclerView = findViewById(R.id.levelRecyclerView);

            findViewById(R.id.backButton).setOnClickListener(v -> finish());

            String modeDisplayName = getModeDisplayName(selectedMode);
            titleText.setText(modeDisplayName + " - Select Level");

            // Update coin display
            updateCoinDisplay();

            levelRecyclerView.setLayoutManager(new GridLayoutManager(this, 5));

            List<LevelItem> levelItems = createLevelItems();

            LevelItemAdapter adapter = new LevelItemAdapter(levelItems, this::onLevelSelected);
            levelRecyclerView.setAdapter(adapter);

            AdMobHelper.initialize(this);
            adView = findViewById(R.id.adView);
            AdMobHelper.loadBannerAd(adView);

        } catch (Exception e) {
            Log.e("LevelSelection", "Error in onCreate", e);
            Toast.makeText(this, "Error loading levels", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateCoinDisplay() {
        if (coinCountText != null) {
            coinCountText.setText(coinManager.getFormattedCoinsWithIcon());
        }
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
        int currentLevel = progressManager.getCurrentLevel(selectedMode);

        for (int i = 1; i <= GameProgressManager.MAX_LEVEL; i++) {
            boolean isCompleted = progressManager.isLevelCompleted(selectedMode, i);
            boolean isUnlocked = i <= currentLevel;
            boolean hasSave = progressManager.hasSavedGame(selectedMode, i);
            int gridSize = progressManager.getGridSizeForLevel(i);

            // Kiểm tra xem level có cần download không
            boolean needsDownload = imageLoader.needsDownload(i);

            items.add(new LevelItem(i, isCompleted, isUnlocked, hasSave, gridSize, needsDownload));
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

        // Kiểm tra và download asset pack nếu cần
        if (item.needsDownload) {
            showDownloadDialog(item);
        } else {
            startGame(item.levelNumber);
        }
    }

    private void showDownloadDialog(LevelItem item) {
        // Tạo dialog hiển thị progress download
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setTitle("Downloading Level " + item.levelNumber);
        progressDialog.setMessage("Preparing puzzle images...");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setMax(100);
        progressDialog.show();

        imageLoader.loadLevelImage(item.levelNumber, new PuzzleImageLoader.ImageLoadCallback() {
            @Override
            public void onSuccess(android.graphics.Bitmap bitmap) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    startGame(item.levelNumber);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    android.widget.Toast.makeText(LevelSelectionActivity.this,
                            "Download failed: " + error,
                            android.widget.Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onDownloadProgress(int progress) {
                runOnUiThread(() -> {
                    progressDialog.setProgress(progress);
                    progressDialog.setMessage("Downloading... " + progress + "%");
                });
            }
        });
    }

    private void startGame(int levelNumber) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("LEVEL", levelNumber);
        intent.putExtra("MODE", selectedMode);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Update coin display when returning from GameActivity
        updateCoinDisplay();

        // Refresh level list khi quay lại
        new Thread(() -> {
            List<LevelItem> levelItems = createLevelItems();

            runOnUiThread(() -> {
                LevelItemAdapter adapter = new LevelItemAdapter(levelItems, this::onLevelSelected);
                levelRecyclerView.setAdapter(adapter);
            });
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageLoader != null) {
            imageLoader.cancelDownloads();
        }
        AdMobHelper.destroyAd(adView);
    }

    public static class LevelItem {
        public int levelNumber;
        public boolean isCompleted;
        public boolean isUnlocked;
        public boolean hasSave;
        public int gridSize;
        public boolean needsDownload;

        public LevelItem(int levelNumber, boolean isCompleted, boolean isUnlocked,
                         boolean hasSave, int gridSize, boolean needsDownload) {
            this.levelNumber = levelNumber;
            this.isCompleted = isCompleted;
            this.isUnlocked = isUnlocked;
            this.hasSave = hasSave;
            this.gridSize = gridSize;
            this.needsDownload = needsDownload;
        }
    }
}