package com.example.puzzle_assemble_picture;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";

    private PuzzleView puzzleView;
    private ImageView sampleImageView;
    private TextView progressText;
    private TextView levelText;
    private FloatingActionButton checkButton;
    private FloatingActionButton saveButton;
    private Button hintButton;

    private int currentLevel;
    private int gridSize;
    private String gameMode;
    private MediaPlayer successSound;
    private MediaPlayer clickSound;
    private GameProgressManager progressManager;
    private Bitmap currentPuzzleBitmap;

    private final int[] puzzleImages = {
            R.drawable.puzzle_sample,
            R.drawable.puzzle_1,
            R.drawable.puzzle_2,
            R.drawable.puzzle_3,
            R.drawable.puzzle_4,
            R.drawable.puzzle_5
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "=== GameActivity onCreate START ===");

        try {
            setContentView(R.layout.activity_game);
            Log.d(TAG, "✓ Layout loaded");

            // Get intent data
            currentLevel = getIntent().getIntExtra("LEVEL", 1);
            gameMode = getIntent().getStringExtra("MODE");

            Log.d(TAG, "✓ Intent data - Level: " + currentLevel + ", Mode: " + gameMode);

            progressManager = new GameProgressManager(this);
            Log.d(TAG, "✓ ProgressManager created");

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

            // Initialize sounds (commented out if not available)
//            try {
//                successSound = MediaPlayer.create(this, R.raw.success_sound);
//                clickSound = MediaPlayer.create(this, R.raw.click_sound);
//            } catch (Exception e) {
//                Log.e(TAG, "Sound initialization failed", e);
//            }

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

        int imageIndex = (currentLevel - 1) % puzzleImages.length;
        Log.d(TAG, "Loading image index: " + imageIndex);

        // Decode with options
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), puzzleImages[imageIndex], options);

        int imageWidth = options.outWidth;
        int imageHeight = options.outHeight;
        int maxSize = 1200;

        Log.d(TAG, "Original image size: " + imageWidth + "x" + imageHeight);

        if (imageWidth > maxSize || imageHeight > maxSize) {
            options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize);
            Log.d(TAG, "Scaling down with inSampleSize: " + options.inSampleSize);
        }

        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        currentPuzzleBitmap = BitmapFactory.decodeResource(getResources(), puzzleImages[imageIndex], options);

        if (currentPuzzleBitmap == null) {
            Log.e(TAG, "Failed to decode bitmap!");
            Toast.makeText(this, "Error loading puzzle image!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Bitmap loaded: " + currentPuzzleBitmap.getWidth() + "x" + currentPuzzleBitmap.getHeight());

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

        int imageIndex = (currentLevel - 1) % puzzleImages.length;
        Log.d(TAG, "Loading image index: " + imageIndex);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), puzzleImages[imageIndex], options);

        int imageWidth = options.outWidth;
        int imageHeight = options.outHeight;
        int maxSize = 1200;

        Log.d(TAG, "Original image size: " + imageWidth + "x" + imageHeight);

        if (imageWidth > maxSize || imageHeight > maxSize) {
            options.inSampleSize = calculateInSampleSize(options, maxSize, maxSize);
            Log.d(TAG, "Scaling down with inSampleSize: " + options.inSampleSize);
        }

        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        currentPuzzleBitmap = BitmapFactory.decodeResource(getResources(), puzzleImages[imageIndex], options);

        if (currentPuzzleBitmap == null) {
            Log.e(TAG, "Failed to decode bitmap!");
            Toast.makeText(this, "Error loading puzzle image!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Log.d(TAG, "Bitmap loaded: " + currentPuzzleBitmap.getWidth() + "x" + currentPuzzleBitmap.getHeight());

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

        GameSaveData finalSaveData = saveData;
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
                            puzzleView.loadGameState(finalSaveData);
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
                updateProgress();
            }

            @Override
            public void onPuzzleCompleted() {
                playSuccessSound();
                onLevelCompleted();
            }

            @Override
            public void onProgressChanged() {
                updateProgress();
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
                // Restart activity with next level
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

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (SettingsActivity.isAutoSaveEnabled(this) && !puzzleView.isPuzzleCompleted()) {
            saveGame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy called");

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
    }

    @Override
    public void onBackPressed() {
        showExitDialog();
    }
    private void initSounds() {
        try {
            // Chỉ init nếu sound được bật
            if (SettingsActivity.isSoundEnabled(this)) {
                successSound = MediaPlayer.create(this, R.raw.success_sound);
                clickSound = MediaPlayer.create(this, R.raw.click_sound);

                // Set volume theo settings
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
}