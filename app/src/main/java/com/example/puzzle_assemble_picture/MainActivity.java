package com.example.puzzle_assemble_picture;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.ads.AdView;

public class MainActivity extends AppCompatActivity {

    private RecyclerView modeRecyclerView;
    private Button btnGallery;
    private Button btnAchievements;
    private Button btnSettings;
    private TextView progressText;

    private TextView coinCountText;
    private CoinManager coinManager;
    private GameProgressManager progressManager;
    private AdView adView;
    private DailyRewardManager dailyRewardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ AdMob already initialized in LoadingActivity, just load banner
        adView = findViewById(R.id.adView);
        if (adView != null) {
            new Thread(() -> {
                runOnUiThread(() -> AdMobHelper.loadBannerAd(adView));
            }).start();
        }

        progressManager = new GameProgressManager(this);

        // Initialize views
        modeRecyclerView = findViewById(R.id.modeRecyclerView);
        btnGallery = findViewById(R.id.btnGallery);
        btnSettings = findViewById(R.id.btnSettings);
        progressText = findViewById(R.id.progressText);

        coinCountText = findViewById(R.id.coinCountText);
        coinManager = new CoinManager(this);
        updateCoinDisplay();

        // ✅ OPTIMIZE: Setup RecyclerView with performance tweaks
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        modeRecyclerView.setLayoutManager(layoutManager);
        modeRecyclerView.setHasFixedSize(true);
        modeRecyclerView.setItemViewCacheSize(4); // Only 4 modes

        List<GameMode> modes = createModeList();
        ModeSelectAdapter adapter = new ModeSelectAdapter(modes, this::onModeSelected);
        modeRecyclerView.setAdapter(adapter);

        // Update progress text
        updateProgressText();

        // Setup button listeners
        btnGallery.setOnClickListener(v -> openGallery());
        btnSettings.setOnClickListener(v -> openSettings());

        dailyRewardManager = new DailyRewardManager(this);
        setupBottomButtons();
    }

    private void showDailyCheckInDialog() {
        DailyCheckInDialog dialog = new DailyCheckInDialog(
                this,
                dailyRewardManager,
                (reward, streak) -> {
                    // Handle reward (add coins, etc.)
                    Toast.makeText(this, "Earned " + reward + " coins!", Toast.LENGTH_SHORT).show();
                    // Update UI
                }
        );
        dialog.show();
    }

    private void setupBottomButtons() {
        // Daily Check-in Button
        Button dailyCheckInBtn = findViewById(R.id.btnDailyCheckIn);
        dailyCheckInBtn.setOnClickListener(v -> showDailyCheckInDialog());
        dailyCheckInBtn.setOnLongClickListener(v -> {
            showTooltip("Daily Check-in - Get free coins every day!");
            return true;
        });

        // Highlight if can check in today
        if (dailyRewardManager.canCheckInToday()) {
            // Add a subtle badge/indicator
            dailyCheckInBtn.setAlpha(1.0f);
        } else {
            dailyCheckInBtn.setAlpha(0.7f);
        }

        // Shop Button
        Button shopBtn = findViewById(R.id.btnShop);
        shopBtn.setOnClickListener(v -> openShop());
        shopBtn.setOnLongClickListener(v -> {
            showTooltip("Shop - Buy power-ups with coins!");
            return true;
        });

        // Gallery Button
        Button galleryBtn = findViewById(R.id.btnGallery);
        galleryBtn.setOnClickListener(v -> openGallery());
        galleryBtn.setOnLongClickListener(v -> {
            showTooltip("Gallery - View completed puzzle images!");
            return true;
        });

        // Settings Button
        Button settingsBtn = findViewById(R.id.btnSettings);
        settingsBtn.setOnClickListener(v -> openSettings());
        settingsBtn.setOnLongClickListener(v -> {
            showTooltip("Settings - Adjust game preferences!");
            return true;
        });
    }

    private void showTooltip(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // ✅ OPTIMIZE: Only refresh if needed (don't recreate adapter every time)
        if (modeRecyclerView.getAdapter() != null) {
            modeRecyclerView.getAdapter().notifyDataSetChanged();
        }

        updateProgressText();
        updateCoinDisplay();

        if (adView != null) {
            adView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }

    private List<GameMode> createModeList() {
        List<GameMode> modes = new ArrayList<>();

        modes.add(new GameMode(
                GameMode.MODE_EASY,
                "🟢 Easy Mode",
                R.drawable.mode_easy,
                false,
                1
        ));

        modes.add(new GameMode(
                GameMode.MODE_NORMAL,
                "🟡 Normal Mode",
                R.drawable.mode_normal,
                !progressManager.isModeUnlocked(GameMode.MODE_NORMAL),
                GameProgressManager.UNLOCK_NORMAL_AT
        ));

        modes.add(new GameMode(
                GameMode.MODE_HARD,
                "🔵 Hard Mode",
                R.drawable.mode_hard,
                !progressManager.isModeUnlocked(GameMode.MODE_HARD),
                GameProgressManager.UNLOCK_HARD_AT
        ));

        modes.add(new GameMode(
                GameMode.MODE_INSANE,
                "🔴 Insane Mode",
                R.drawable.mode_superhard,
                !progressManager.isModeUnlocked(GameMode.MODE_INSANE),
                GameProgressManager.UNLOCK_INSANE_AT
        ));

        return modes;
    }

    private void onModeSelected(GameMode mode) {
        if (mode.isLocked()) {
            showLockedModeDialog(mode);
            return;
        }

        // ✅ FAST: Direct navigation without preparation
        Intent intent = new Intent(this, LevelSelectionActivity.class);
        intent.putExtra("MODE", mode.getModeType());
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void showLockedModeDialog(GameMode mode) {
        String unlockRequirement = getUnlockRequirement(mode.getModeType());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🔒 " + mode.getDisplayName() + " Locked");
        builder.setMessage(mode.getDescription() + "\n\n" + unlockRequirement);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private String getUnlockRequirement(String mode) {
        switch (mode) {
            case GameMode.MODE_NORMAL:
                return "Complete Level " + GameProgressManager.UNLOCK_NORMAL_AT + " in Easy Mode to unlock!";
            case GameMode.MODE_HARD:
                return "Complete Level " + GameProgressManager.UNLOCK_HARD_AT + " in Normal Mode to unlock!";
            case GameMode.MODE_INSANE:
                return "Complete Level " + GameProgressManager.UNLOCK_INSANE_AT + " in Hard Mode to unlock!";
            default:
                return "";
        }
    }

    private void updateProgressText() {
        int totalCompleted = progressManager.getTotalCompletedLevelsAllModes();
        int totalLevels = GameProgressManager.MAX_LEVEL * 4;
        float completion = progressManager.getOverallCompletionPercentage();

        int galleryPieces = progressManager.getUnlockedPiecesCount();
        int totalPieces = 100;

        String progressInfo = String.format(
                "Progress: %d/%d levels (%.1f%%)\nGallery: %d/%d pieces",
                totalCompleted, totalLevels, completion, galleryPieces, totalPieces
        );

        progressText.setText(progressInfo);
    }

    private void openGallery() {
        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void openShop() {
        Intent intent = new Intent(this, ShopActivity.class);
        startActivity(intent);
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void updateCoinDisplay() {
        if (coinCountText != null && coinManager != null) {
            coinCountText.setText(String.valueOf(coinManager.getCoins()));
        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit Game?");
        builder.setMessage("Are you sure you want to exit?");
        builder.setPositiveButton("Exit", (dialog, which) -> {
            super.onBackPressed();
            finish();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}