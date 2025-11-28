package com.example.puzzle_assemble_picture;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";
    // MAX_IMAGE_SIZE đã được định nghĩa trong PuzzleImageLoader

    private PuzzleView puzzleView;
    private ImageView sampleImageView;
    private TextView progressText;
    private TextView levelText;
    private Button checkButton;
    private Button saveButton;
    private Button hintButton;

    private int currentLevel;
    private int gridSize;
    private String gameMode;
    private MediaPlayer successSound;
    private MediaPlayer clickSound;
    private GameProgressManager progressManager;
    private PuzzleImageLoader imageLoader;
    private Bitmap currentPuzzleBitmap;
    private android.app.ProgressDialog downloadDialog;

    private AdView adView;
    private View fullscreenOverlay;
    private ImageView fullscreenImageView;

    private PowerUpsManager powerUpsManager;
    private Button autoSolveButton;
    private Button shuffleButton;
    private ProgressBar progressBar;
    private TextView lockedCountText;
    private TextView remainingCountText;
    private TextView streakCountText;
    private int currentStreak = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "=== GameActivity onCreate START ===");

        // Initialize AdMob
        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "AdMob initialized");
        });

        try {
            setContentView(R.layout.activity_game);
            Log.d(TAG, "✓ Layout loaded");

            // Get intent data
            currentLevel = getIntent().getIntExtra("LEVEL", 1);
            gameMode = getIntent().getStringExtra("MODE");

            Log.d(TAG, "✓ Intent data - Level: " + currentLevel + ", Mode: " + gameMode);

            progressManager = new GameProgressManager(this);
            imageLoader = new PuzzleImageLoader(this);
            Log.d(TAG, "✓ ProgressManager & ImageLoader created");

            // Default to EASY if no mode specified
            if (gameMode == null || gameMode.isEmpty()) {
                gameMode = GameMode.MODE_EASY;
                Log.d(TAG, "✓ Mode defaulted to: " + gameMode);
            }

            gridSize = progressManager.getGridSizeForLevel(currentLevel);
            Log.d(TAG, "✓ Grid size: " + gridSize);

            // Initialize views
            puzzleView = findViewById(R.id.puzzleView);
            sampleImageView = findViewById(R.id.sampleImageView);
            progressText = findViewById(R.id.progressText);
            levelText = findViewById(R.id.levelText);
            checkButton = findViewById(R.id.checkButton);
            saveButton = findViewById(R.id.saveButton);
            hintButton = findViewById(R.id.hintButton);

            Log.d(TAG, "✓ All views initialized");

            initSounds();

            levelText.setText("Level " + currentLevel + " (" + gridSize + "x" + gridSize + ")");

            // Check for saved game
            if (progressManager.hasSavedGame(gameMode, currentLevel)) {
                Log.d(TAG, "✓ Has saved game - showing dialog");
                showLoadGameDialog();
            } else {
                Log.d(TAG, "✓ No saved game - setup new");
                setupNewGame();
            }

            // Setup button listeners
            checkButton.setOnClickListener(v -> checkProgress());
            saveButton.setOnClickListener(v -> saveGame());
            hintButton.setOnClickListener(v -> showHint());
            findViewById(R.id.backButton).setOnClickListener(v -> showExitDialog());

            Log.d(TAG, "=== GameActivity onCreate COMPLETE ===");

        } catch (Exception e) {
            Log.e(TAG, "❌ ERROR in onCreate: ", e);
            e.printStackTrace();
            Toast.makeText(this, "Error loading game: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }


        // Setup AdMob Banner
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        // Setup fullscreen overlay
        fullscreenOverlay = findViewById(R.id.fullscreenOverlay);
        fullscreenImageView = findViewById(R.id.fullscreenImageView);

        // Click sample image to show fullscreen
        sampleImageView.setOnClickListener(v -> {
            if (currentPuzzleBitmap != null) {
                showFullscreenImage();
            }
        });

        // Click overlay to close
        fullscreenOverlay.setOnClickListener(v -> hideFullscreenImage());

        // Initialize Power-ups Manager
        powerUpsManager = new PowerUpsManager(this);

    // Initialize new views
        autoSolveButton = findViewById(R.id.autoSolveButton);
        shuffleButton = findViewById(R.id.shuffleButton);
        progressBar = findViewById(R.id.progressBar);
        lockedCountText = findViewById(R.id.lockedCountText);
        remainingCountText = findViewById(R.id.remainingCountText);
        streakCountText = findViewById(R.id.streakCountText);

    // Setup power-up buttons
        autoSolveButton.setOnClickListener(v -> useAutoSolve());
        shuffleButton.setOnClickListener(v -> useShuffle());

    // Update button texts với số lượt
        updatePowerUpButtons();

    }

    private void showLoadGameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Continue Game?");
        builder.setMessage("You have a saved game for this level. Would you like to continue?");
        builder.setPositiveButton("Continue", (dialog, which) -> loadSavedGame());
        builder.setNegativeButton("Start New", (dialog, which) -> {
            progressManager.clearGameState(gameMode, currentLevel);
            setupNewGame();
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void setupNewGame() {
        Log.d(TAG, "setupNewGame - Level: " + currentLevel + ", Mode: " + gameMode);

        // Clean up old bitmap
        recycleBitmap();

        // Hiển thị download dialog nếu cần
        if (imageLoader.needsDownload(currentLevel)) {
            showDownloadDialog();
        }

        // Load image từ assets hoặc asset pack
        imageLoader.loadLevelImage(currentLevel, new PuzzleImageLoader.ImageLoadCallback() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                dismissDownloadDialog();

                if (bitmap == null) {
                    Toast.makeText(GameActivity.this, "Error: Bitmap is null!", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                currentPuzzleBitmap = bitmap;
                Log.d(TAG, "Bitmap loaded: " + bitmap.getWidth() + "x" + bitmap.getHeight());

                PuzzleConfig config = createConfigForMode(gameMode);
                config.gridSize = gridSize;

                if (config.showSample) {
                    sampleImageView.setVisibility(View.VISIBLE);
                    sampleImageView.setImageBitmap(currentPuzzleBitmap);
                    Log.d(TAG, "Sample image visible");
                } else {
                    sampleImageView.setVisibility(View.GONE);
                    Log.d(TAG, "Sample image hidden");
                }

                // Wait for view to be laid out
                puzzleView.getViewTreeObserver().addOnGlobalLayoutListener(
                        new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                puzzleView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                                Log.d(TAG, "PuzzleView size: " + puzzleView.getWidth() + "x" + puzzleView.getHeight());

                                if (puzzleView.getWidth() == 0 || puzzleView.getHeight() == 0) {
                                    Log.e(TAG, "PuzzleView has zero dimensions!");
                                    Toast.makeText(GameActivity.this, "Error: View not ready", Toast.LENGTH_SHORT).show();
                                    finish();
                                    return;
                                }

                                try {
                                    puzzleView.initPuzzle(currentPuzzleBitmap, config, createPuzzleListener());
                                    updateProgress();
                                    Log.d(TAG, "Puzzle initialized successfully");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error initializing puzzle", e);
                                    Toast.makeText(GameActivity.this, "Error setting up puzzle: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            }
                        }
                );
            }

            @Override
            public void onError(String error) {
                dismissDownloadDialog();
                Log.e(TAG, "Failed to load image: " + error);
                Toast.makeText(GameActivity.this, "Failed to load puzzle image: " + error, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onDownloadProgress(int progress) {
                updateDownloadProgress(progress);
            }
        });
    }

    private void loadSavedGame() {
        Log.d(TAG, "loadSavedGame - Level: " + currentLevel);

        GameSaveData saveData = progressManager.loadGameState(gameMode, currentLevel);
        if (saveData == null) {
            Log.d(TAG, "No saved game found, starting new game");
            setupNewGame();
            return;
        }

        Log.d(TAG, "Saved game found, loading...");

        recycleBitmap();

        // Hiển thị download dialog nếu cần
        if (imageLoader.needsDownload(currentLevel)) {
            showDownloadDialog();
        }

        // Load image
        imageLoader.loadLevelImage(currentLevel, new PuzzleImageLoader.ImageLoadCallback() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                dismissDownloadDialog();

                if (bitmap == null) {
                    Toast.makeText(GameActivity.this, "Error: Bitmap is null!", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                currentPuzzleBitmap = bitmap;
                Log.d(TAG, "Bitmap loaded: " + bitmap.getWidth() + "x" + bitmap.getHeight());

                PuzzleConfig config = createConfigForMode(gameMode);
                config.gridSize = gridSize;

                if (config.showSample) {
                    sampleImageView.setVisibility(View.VISIBLE);
                    sampleImageView.setImageBitmap(currentPuzzleBitmap);
                    Log.d(TAG, "Sample image visible");
                } else {
                    sampleImageView.setVisibility(View.GONE);
                    Log.d(TAG, "Sample image hidden");
                }

                puzzleView.getViewTreeObserver().addOnGlobalLayoutListener(
                        new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                puzzleView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                                Log.d(TAG, "PuzzleView size: " + puzzleView.getWidth() + "x" + puzzleView.getHeight());

                                if (puzzleView.getWidth() == 0 || puzzleView.getHeight() == 0) {
                                    Log.e(TAG, "PuzzleView has zero dimensions!");
                                    Toast.makeText(GameActivity.this, "Error: View not ready", Toast.LENGTH_SHORT).show();
                                    finish();
                                    return;
                                }

                                try {
                                    puzzleView.initPuzzle(currentPuzzleBitmap, config, createPuzzleListener());
                                    puzzleView.loadGameState(saveData);
                                    updateProgress();
                                    Log.d(TAG, "Saved game loaded successfully");
                                    Toast.makeText(GameActivity.this, "Game loaded!", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error loading saved game", e);
                                    Toast.makeText(GameActivity.this, "Error loading saved game: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    finish();
                                }
                            }
                        }
                );
            }

            @Override
            public void onError(String error) {
                dismissDownloadDialog();
                Log.e(TAG, "Failed to load image: " + error);
                Toast.makeText(GameActivity.this, "Failed to load puzzle image: " + error, Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onDownloadProgress(int progress) {
                updateDownloadProgress(progress);
            }
        });
    }

    private void showDownloadDialog() {
        if (downloadDialog == null) {
            downloadDialog = new android.app.ProgressDialog(this);
            downloadDialog.setTitle("Downloading Level " + currentLevel);
            downloadDialog.setMessage("Preparing puzzle image...");
            downloadDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
            downloadDialog.setCancelable(false);
            downloadDialog.setMax(100);
        }
        downloadDialog.show();
    }

    private void updateDownloadProgress(int progress) {
        if (downloadDialog != null && downloadDialog.isShowing()) {
            downloadDialog.setProgress(progress);
            downloadDialog.setMessage("Downloading... " + progress + "%");
        }
    }

    private void dismissDownloadDialog() {
        if (downloadDialog != null && downloadDialog.isShowing()) {
            downloadDialog.dismiss();
        }
    }

    private PuzzleConfig createConfigForMode(String mode) {
        PuzzleConfig config = new PuzzleConfig();

        switch (mode) {
            case GameMode.MODE_EASY:
                config.showSample = true;
                config.autoLockCorrectPieces = true;
                config.canSeparateConnectedPieces = false;
                config.autoConnectCorrectPieces = true;
                config.dimLockedPieces = true;
                break;

            case GameMode.MODE_NORMAL:
                config.showSample = true;
                config.autoLockCorrectPieces = false;
                config.canSeparateConnectedPieces = false;
                config.autoConnectCorrectPieces = true;
                config.dimLockedPieces = false;
                break;

            case GameMode.MODE_HARD:
                config.showSample = false;
                config.autoLockCorrectPieces = false;
                config.canSeparateConnectedPieces = true;
                config.autoConnectCorrectPieces = true;
                config.dimLockedPieces = false;
                break;

            case GameMode.MODE_INSANE:
                config.showSample = false;
                config.autoLockCorrectPieces = false;
                config.canSeparateConnectedPieces = true;
                config.autoConnectCorrectPieces = false;
                config.dimLockedPieces = false;
                break;
        }

        return config;
    }

    private PuzzleView.PuzzleListener createPuzzleListener() {
        return new PuzzleView.PuzzleListener() {
            @Override
            public void onPieceConnected() {
                playClickSound();

                // Increase streak khi piece đúng vị trí
                PuzzlePiece lastMovedPiece = getLastMovedPiece();
                if (lastMovedPiece != null && lastMovedPiece.isLocked()) {
                    currentStreak++;
                    if (currentStreak > 1) {
                        Toast.makeText(GameActivity.this,
                                "🔥 Streak: " + currentStreak + "!",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    currentStreak = 0;
                }

                updateStats();
            }

            @Override
            public void onPuzzleCompleted() {
                playSuccessSound();
                onLevelCompleted();
            }

            @Override
            public void onProgressChanged() {
                updateStats();
            }
        };
    }

    private void onLevelCompleted() {
        progressManager.markLevelCompleted(gameMode, currentLevel);
        progressManager.clearGameState(gameMode, currentLevel);
        progressManager.addGalleryPiece(currentLevel - 1);

        String unlockMessage = progressManager.getUnlockMessage(gameMode, currentLevel);
        showCompletionDialog(unlockMessage);
    }

    private void updateProgress() {
        int correctPieces = puzzleView.getCorrectPiecesCount();
        int totalPieces = gridSize * gridSize;
        progressText.setText(correctPieces + "/" + totalPieces + " correct");
        updateStats();
    }

    private void checkProgress() {
        int correctPieces = puzzleView.getCorrectPiecesCount();
        int totalPieces = gridSize * gridSize;

        String message = "✓ Correct pieces: " + correctPieces + "/" + totalPieces;

        if (correctPieces == totalPieces) {
            message = "🎉 Perfect! All pieces are correct!";
        } else if (correctPieces > totalPieces * 0.7) {
            message += "\n💪 You're almost there!";
        } else if (correctPieces > totalPieces * 0.4) {
            message += "\n👍 Good progress!";
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void saveGame() {
        GameSaveData saveData = puzzleView.getSaveData();
        saveData.level = currentLevel;
        progressManager.saveGameState(gameMode, currentLevel, saveData);
        Toast.makeText(this, "💾 Game saved!", Toast.LENGTH_SHORT).show();
        playClickSound();
    }

    private void showHint() {
        String hint = getHintForMode(gameMode, gridSize);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("💡 Hint");
        builder.setMessage(hint);
        builder.setPositiveButton("Got it!", null);
        builder.show();
    }

    private String getHintForMode(String mode, int size) {
        String sizeHint = "";
        if (size >= 7) {
            sizeHint = "\n\n🔍 Tip: Start with corner pieces!";
        }

        switch (mode) {
            case GameMode.MODE_EASY:
                return "💡 Easy Mode Tips:\n\n" +
                        "• Look at the sample image in the corner\n" +
                        "• Pieces auto-connect when placed correctly\n" +
                        "• Correct pieces lock in place and dim\n" +
                        "• Swap pieces to arrange them!" + sizeHint;

            case GameMode.MODE_NORMAL:
                return "💡 Normal Mode Tips:\n\n" +
                        "• Use the sample image as reference\n" +
                        "• Pieces connect automatically\n" +
                        "• Find pieces that should be adjacent\n" +
                        "• Group similar patterns!" + sizeHint;

            case GameMode.MODE_HARD:
                return "💡 Hard Mode Tips:\n\n" +
                        "• No sample - rely on your memory!\n" +
                        "• Pieces can be separated\n" +
                        "• Look for edge pieces first\n" +
                        "• Pay attention to colors and patterns!" + sizeHint;

            case GameMode.MODE_INSANE:
                return "💡 Insane Mode Tips:\n\n" +
                        "• No sample, no auto-connect!\n" +
                        "• Place each piece carefully\n" +
                        "• Take your time and focus\n" +
                        "• Ultimate challenge!" + sizeHint;

            default:
                return "Good luck!";
        }
    }

    private void showCompletionDialog(String unlockMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎉 Level Complete!");

        String message = "You completed Level " + currentLevel + "!\n\n";
        message += "🧩 Gallery piece unlocked!\n";

        if (unlockMessage != null) {
            message += "\n" + unlockMessage;
        }

        builder.setMessage(message);
        builder.setPositiveButton("Next Level", (dialog, which) -> {
            int nextLevel = currentLevel + 1;
            if (nextLevel <= GameProgressManager.MAX_LEVEL) {
                Intent intent = getIntent();
                intent.putExtra("LEVEL", nextLevel);
                intent.putExtra("MODE", gameMode);
                finish();
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            } else {
                Toast.makeText(this, "🎉 You've completed all levels in this mode!", Toast.LENGTH_LONG).show();
                finish();
            }
        });
        builder.setNegativeButton("Main Menu", (dialog, which) -> finish());
        builder.setCancelable(false);
        builder.show();
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit Level?");
        builder.setMessage("Don't forget to save your progress before leaving!");
        builder.setPositiveButton("Exit", (dialog, which) -> finish());
        builder.setNegativeButton("Cancel", null);
        builder.setNeutralButton("Save & Exit", (dialog, which) -> {
            saveGame();
            finish();
        });
        builder.show();
    }

    private void playClickSound() {
        if (SettingsActivity.isSoundEnabled(this) && clickSound != null) {
            try {
                if (clickSound.isPlaying()) clickSound.seekTo(0);
                float volume = SettingsActivity.getSoundVolumeFloat(this);
                clickSound.setVolume(volume, volume);
                clickSound.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void playSuccessSound() {
        if (SettingsActivity.isSoundEnabled(this) && successSound != null) {
            try {
                float volume = SettingsActivity.getSoundVolumeFloat(this);
                successSound.setVolume(volume, volume);
                successSound.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void recycleBitmap() {
        if (currentPuzzleBitmap != null && !currentPuzzleBitmap.isRecycled()) {
            currentPuzzleBitmap.recycle();
            currentPuzzleBitmap = null;
            Log.d(TAG, "Bitmap recycled");
        }
    }

    private void initSounds() {
        try {
            if (SettingsActivity.isSoundEnabled(this)) {
                successSound = MediaPlayer.create(this, R.raw.success_sound);
                clickSound = MediaPlayer.create(this, R.raw.click_sound);

                if (successSound != null) {
                    float volume = SettingsActivity.getSoundVolumeFloat(this);
                    successSound.setVolume(volume, volume);
                }
                if (clickSound != null) {
                    float volume = SettingsActivity.getSoundVolumeFloat(this);
                    clickSound.setVolume(volume, volume);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Sound initialization failed", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (SettingsActivity.isAutoSaveEnabled(this) && !puzzleView.isPuzzleCompleted()) {
            saveGame();
        }
    }

    private void useAutoSolve() {
        powerUpsManager.usePowerUp(PowerUpsManager.PowerUpType.AUTO_SOLVE, new PowerUpsManager.PowerUpCallback() {
            @Override
            public void onSuccess() {
                // Execute auto-solve
                boolean solved = puzzleView.autoSolveOnePiece();

                if (solved) {
                    Toast.makeText(GameActivity.this, "✨ One piece auto-solved!", Toast.LENGTH_SHORT).show();
                    updateStats();
                    updatePowerUpButtons();

                    // Check completion
                    if (puzzleView.isPuzzleCompleted()) {
                        onLevelCompleted();
                    }
                } else {
                    Toast.makeText(GameActivity.this, "No pieces to auto-solve!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailed(String reason) {
                Toast.makeText(GameActivity.this, reason, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void useShuffle() {
        powerUpsManager.usePowerUp(PowerUpsManager.PowerUpType.SHUFFLE, new PowerUpsManager.PowerUpCallback() {
            @Override
            public void onSuccess() {
                // Execute shuffle
                boolean shuffled = puzzleView.shuffleRemainingPieces();

                if (shuffled) {
                    Toast.makeText(GameActivity.this, "🔀 Pieces shuffled!", Toast.LENGTH_SHORT).show();
                    currentStreak = 0; // Reset streak khi shuffle
                    updateStats();
                    updatePowerUpButtons();
                } else {
                    Toast.makeText(GameActivity.this, "No pieces to shuffle!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailed(String reason) {
                Toast.makeText(GameActivity.this, reason, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePowerUpButtons() {
        int autoSolveRemaining = powerUpsManager.getRemainingUses(PowerUpsManager.PowerUpType.AUTO_SOLVE);
        int shuffleRemaining = powerUpsManager.getRemainingUses(PowerUpsManager.PowerUpType.SHUFFLE);

        if (autoSolveRemaining > 0) {
            autoSolveButton.setText("🎯 Auto-Solve (" + autoSolveRemaining + ")");
            autoSolveButton.setEnabled(true);
            autoSolveButton.setAlpha(1.0f);
        } else {
            autoSolveButton.setText("🎯 Auto-Solve (📺 Watch Ad)");
            autoSolveButton.setEnabled(true);
            autoSolveButton.setAlpha(0.8f);
        }

        if (shuffleRemaining > 0) {
            shuffleButton.setText("🔀 Shuffle (" + shuffleRemaining + ")");
            shuffleButton.setEnabled(true);
            shuffleButton.setAlpha(1.0f);
        } else {
            shuffleButton.setText("🔀 Shuffle (📺 Watch Ad)");
            shuffleButton.setEnabled(true);
            shuffleButton.setAlpha(0.8f);
        }
    }

    private void updateStats() {
        int totalPieces = gridSize * gridSize;
        int correctPieces = puzzleView.getCorrectPiecesCount();
        int lockedPieces = puzzleView.getLockedPiecesCount();
        int remainingPieces = puzzleView.getRemainingPiecesCount();

        // Update progress bar
        int progress = (correctPieces * 100) / totalPieces;
        progressBar.setProgress(progress);

        // Update counts
        lockedCountText.setText(String.valueOf(lockedPieces));
        remainingCountText.setText(String.valueOf(remainingPieces));
        streakCountText.setText(String.valueOf(currentStreak));

        // Update main progress text
        progressText.setText(correctPieces + "/" + totalPieces + " (" + progress + "%)");
    }

    private PuzzlePiece getLastMovedPiece() {
        // Simplified - just return null for now
        // PuzzleView cần track last moved piece nếu muốn chính xác
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy called");

        dismissDownloadDialog();
        recycleBitmap();

        if (successSound != null) {
            successSound.release();
            successSound = null;
        }
        if (clickSound != null) {
            clickSound.release();
            clickSound = null;
        }

        if (puzzleView != null) {
            try {
                puzzleView.cleanup();
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up PuzzleView", e);
            }
        }

        if (imageLoader != null) {
            imageLoader.cancelDownloads();
        }

        if (adView != null) {
            adView.destroy();
        }

        powerUpsManager = null;
        super.onDestroy();
    }

    private void showFullscreenImage() {
        if (currentPuzzleBitmap != null) {
            fullscreenImageView.setImageBitmap(currentPuzzleBitmap);
            fullscreenOverlay.setVisibility(View.VISIBLE);

            // Animate fade in
            fullscreenOverlay.setAlpha(0f);
            fullscreenOverlay.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        }
    }

    private void hideFullscreenImage() {
        // Animate fade out
        fullscreenOverlay.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    fullscreenOverlay.setVisibility(View.GONE);
                    fullscreenOverlay.setAlpha(1f);
                })
                .start();
    }

    @Override
    public void onBackPressed() {
        // Nếu đang hiện fullscreen image, đóng nó
        if (fullscreenOverlay.getVisibility() == View.VISIBLE) {
            hideFullscreenImage();
        } else {
            // Nếu không, show exit dialog như bình thường
            showExitDialog();
        }
    }
}