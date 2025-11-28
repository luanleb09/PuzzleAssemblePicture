package com.example.puzzle_assemble_picture;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
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
    private GameProgressManager progressManager;
    private AdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Initialize AdMob ASYNC - không block UI thread
        new Thread(() -> {
            AdMobHelper.initialize(MainActivity.this);

            // Load banner sau khi init xong
            runOnUiThread(() -> {
                adView = findViewById(R.id.adView);
                if (adView != null) {
                    AdMobHelper.loadBannerAd(adView);
                }
            });
        }).start();

        progressManager = new GameProgressManager(this);

        // Initialize views
        modeRecyclerView = findViewById(R.id.modeRecyclerView);
        btnGallery = findViewById(R.id.btnGallery);
        btnAchievements = findViewById(R.id.btnAchievements);
        btnSettings = findViewById(R.id.btnSettings);
        progressText = findViewById(R.id.progressText);

        // Setup RecyclerView
        modeRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        List<GameMode> modes = createModeList();
        ModeSelectAdapter adapter = new ModeSelectAdapter(modes, this::onModeSelected);
        modeRecyclerView.setAdapter(adapter);

        // Update progress text
        updateProgressText();

        // Setup button listeners
        btnGallery.setOnClickListener(v -> openGallery());
        btnAchievements.setOnClickListener(v -> openAchievements());
        btnSettings.setOnClickListener(v -> openSettings());
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
        // Refresh mode list and progress when returning to main screen
        List<GameMode> modes = createModeList();
        ModeSelectAdapter adapter = new ModeSelectAdapter(modes, this::onModeSelected);
        modeRecyclerView.setAdapter(adapter);
        updateProgressText();
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
                false, // Easy is always unlocked
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

        // Open level selection for this mode
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
        int totalLevels = GameProgressManager.MAX_LEVEL * 4; // 4 modes
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

    private void openAchievements() {
        Intent intent = new Intent(this, AchievementActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
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