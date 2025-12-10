package com.example.puzzle_assemble_picture;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
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
import android.app.Dialog;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import android.view.ViewGroup;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity";

    private PuzzleView puzzleView;
    private ImageView sampleImageView;
    private TextView progressText;
    private TextView levelText;
    private Button checkButton;
    private Button saveButton;
    private Button hintButton;
    private TextView levelCompleteText;
    private View completionOverlay;
    private View glowOverlay;
    private nl.dionsegijn.konfetti.xml.KonfettiView konfettiView;

    private int currentLevel;
    private int gridSize;
    private String gameMode;
    private MediaPlayer successSound;
    private MediaPlayer clickSound;
    private MediaPlayer celebrationSound;
    private MediaPlayer confettiSound;
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

    private TextView coinCountGameText;
    private CoinManager coinManager;

    private Handler handler = new Handler(Looper.getMainLooper());

    private InterstitialAdManager interstitialAdManager;
    private boolean isShowingAd = false; // Prevent finish during ad
    private boolean isLevelCompleted = false;
    private String currentMode;
    private DailyRewardManager dailyRewardManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "=== GameActivity onCreate START ===");

        interstitialAdManager = new InterstitialAdManager(this);

        try {
            setContentView(R.layout.activity_game);

            currentLevel = getIntent().getIntExtra("LEVEL", 1);
            gameMode = getIntent().getStringExtra("MODE");

            currentMode = gameMode; // Assign to instance variable for use in listeners

            progressManager = new GameProgressManager(this);
            imageLoader = new PuzzleImageLoader(this);

            if (gameMode == null || gameMode.isEmpty()) {
                gameMode = GameMode.MODE_EASY;
            }

            gridSize = progressManager.getGridSizeForLevel(currentLevel);

            // Initialize views
            puzzleView = findViewById(R.id.puzzleView);
            sampleImageView = findViewById(R.id.sampleImageView);
            progressText = findViewById(R.id.progressText);
            levelText = findViewById(R.id.levelText);
            checkButton = findViewById(R.id.checkButton);
            saveButton = findViewById(R.id.saveButton);
            hintButton = findViewById(R.id.hintButton);
            levelCompleteText = findViewById(R.id.levelCompleteText);
            completionOverlay = findViewById(R.id.completionOverlay);
            glowOverlay = findViewById(R.id.glowOverlay);
            konfettiView = findViewById(R.id.konfettiView);

            initSounds();

            levelText.setText("Level " + currentLevel + " (" + gridSize + "x" + gridSize + ")");

            if (progressManager.hasSavedGame(gameMode, currentLevel)) {
                showLoadGameDialog();
            } else {
                setupNewGame();
            }

            checkButton.setOnClickListener(v -> checkProgress());
            saveButton.setOnClickListener(v -> saveGame());
            hintButton.setOnClickListener(v -> showHint());
            findViewById(R.id.backButton).setOnClickListener(v -> showExitDialog());

            // Shop button
            Button shopButton = findViewById(R.id.shopButton);
            if (shopButton != null) {
                shopButton.setOnClickListener(v -> {
                    Intent intent = new Intent(GameActivity.this, ShopActivity.class);
                    startActivity(intent);
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ ERROR in onCreate: ", e);
            Toast.makeText(this, "Error loading game: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }

        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        fullscreenOverlay = findViewById(R.id.fullscreenOverlay);
        fullscreenImageView = findViewById(R.id.fullscreenImageView);

        sampleImageView.setOnClickListener(v -> {
            if (currentPuzzleBitmap != null) {
                showFullscreenImage();
            }
        });

        fullscreenOverlay.setOnClickListener(v -> hideFullscreenImage());

        powerUpsManager = new PowerUpsManager(this);

        autoSolveButton = findViewById(R.id.autoSolveButton);
        shuffleButton = findViewById(R.id.shuffleButton);
        progressBar = findViewById(R.id.progressBar);
        lockedCountText = findViewById(R.id.lockedCountText);
        remainingCountText = findViewById(R.id.remainingCountText);
        streakCountText = findViewById(R.id.streakCountText);

        coinCountGameText = findViewById(R.id.coinCountGameText);
        coinManager = new CoinManager(this);
        updateCoinDisplay();

        autoSolveButton.setOnClickListener(v -> useAutoSolve());
        shuffleButton.setOnClickListener(v -> useShuffle());

        updatePowerUpButtons();

        dailyRewardManager = new DailyRewardManager(this);
//        updateButtonStates();

    }

//    private void updateButtonStates() {
//        Button autoSolveBtn = findViewById(R.id.autoSolveButton);
//        Button shuffleBtn = findViewById(R.id.shuffleButton);
//
//        int remainingAutoSolve = dailyRewardManager.getRemainingAutoSolveCount();
//        int remainingShuffle = dailyRewardManager.getRemainingShuffleCount();
//
//        autoSolveBtn.setText("🎯 Auto (" + remainingAutoSolve + "/" +
//                DailyRewardManager.MAX_AUTO_SOLVE_PER_DAY + ")");
//        shuffleBtn.setText("🔀 Shuffle (" + remainingShuffle + "/" +
//                DailyRewardManager.MAX_SHUFFLE_PER_DAY + ")");
//
//        autoSolveBtn.setEnabled(remainingAutoSolve > 0);
//        shuffleBtn.setEnabled(remainingShuffle > 0);
//    }

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
        recycleBitmap();

        if (imageLoader.needsDownload(currentLevel)) {
            showDownloadDialog();
        }

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

                PuzzleConfig config = createConfigForMode(gameMode);
                config.gridSize = gridSize;

                if (config.showSample) {
                    sampleImageView.setVisibility(View.VISIBLE);
                    sampleImageView.setImageBitmap(currentPuzzleBitmap);
                } else {
                    sampleImageView.setVisibility(View.GONE);
                }

                puzzleView.getViewTreeObserver().addOnGlobalLayoutListener(
                        new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                puzzleView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                                if (puzzleView.getWidth() == 0 || puzzleView.getHeight() == 0) {
                                    finish();
                                    return;
                                }

                                try {
                                    puzzleView.initPuzzle(currentPuzzleBitmap, config, createPuzzleListener());
                                    updateProgress();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error initializing puzzle", e);
                                    finish();
                                }
                            }
                        }
                );
            }

            @Override
            public void onError(String error) {
                dismissDownloadDialog();
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
        GameSaveData saveData = progressManager.loadGameState(gameMode, currentLevel);
        if (saveData == null) {
            setupNewGame();
            return;
        }

        recycleBitmap();

        if (imageLoader.needsDownload(currentLevel)) {
            showDownloadDialog();
        }

        imageLoader.loadLevelImage(currentLevel, new PuzzleImageLoader.ImageLoadCallback() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                dismissDownloadDialog();

                if (bitmap == null) {
                    finish();
                    return;
                }

                currentPuzzleBitmap = bitmap;

                PuzzleConfig config = createConfigForMode(gameMode);
                config.gridSize = gridSize;

                if (config.showSample) {
                    sampleImageView.setVisibility(View.VISIBLE);
                    sampleImageView.setImageBitmap(currentPuzzleBitmap);
                } else {
                    sampleImageView.setVisibility(View.GONE);
                }

                puzzleView.getViewTreeObserver().addOnGlobalLayoutListener(
                        new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                puzzleView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                                try {
                                    puzzleView.initPuzzle(currentPuzzleBitmap, config, createPuzzleListener());
                                    puzzleView.loadGameState(saveData);
                                    updateProgress();
                                    Toast.makeText(GameActivity.this, "Game loaded!", Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Log.e(TAG, "Error loading saved game", e);
                                    finish();
                                }
                            }
                        }
                );
            }

            @Override
            public void onError(String error) {
                dismissDownloadDialog();
                finish();
            }

            @Override
            public void onDownloadProgress(int progress) {
                updateDownloadProgress(progress);
            }
        });
    }

    private void openShop() {
        Intent intent = new Intent(this, ShopActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update coin display when returning from shop
        updateCoinDisplay();
        updatePowerUpButtons();
    }

    private void showFullImage(int pieceId) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_image);

        ImageView fullImageView = dialog.findViewById(R.id.fullImageView);
        ImageView closeButton = dialog.findViewById(R.id.closeButton);

        int imageResId = getResources().getIdentifier(
                "level_" + pieceId,
                "drawable",
                getPackageName()
        );

        if (imageResId != 0) {
            Glide.with(this)
                    .load(imageResId)
                    .fitCenter()
                    .into(fullImageView);
        }

        closeButton.setOnClickListener(v -> dialog.dismiss());
        fullImageView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
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
                playCelebrationSound();
                if (isLevelCompleted) return;
                isLevelCompleted = true;

                // Mark level as completed
                progressManager.markLevelCompleted(currentMode, currentLevel);

                // Unlock gallery piece
                int pieceId = currentLevel - 1;
                progressManager.unlockGalleryPiece(pieceId);

                // Award coins
                int reward = CoinManager.getRewardForLevel(currentMode);
                coinManager.addCoins(reward);
                updateCoinDisplay();

                // Get unlock message
                String unlockMessage = progressManager.getUnlockMessage(currentMode, currentLevel);

                // Show completion animation first
                showCompletionAnimation();

                // Then show tap to continue overlay
                handler.postDelayed(() -> {
                    showTapToContinueOverlay(unlockMessage);
                }, 5000);
            }

            @Override
            public void onProgressChanged() {
                updateStats();
            }
        };
    }

    private void showCompletionDialog() {
        cancelAllCompletionAnimations();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("🎉 Level Complete!");
        int correctPieces = puzzleView.getCorrectPiecesCount();
        int totalPieces = gridSize * gridSize;

        String message = "Congratulations!\n\n" +
                "✓ Correct pieces: " + correctPieces + "/" + totalPieces + "\n" +
                "🎯 Mode: " + getModeDisplayName(currentMode) + "\n" +
                "📊 Level: " + currentLevel;

        // Add coin reward info
        int reward = CoinManager.getRewardForLevel(currentMode);
        message += "\n💰 Coins earned: " + reward;

        builder.setMessage(message);
        builder.setCancelable(false);

        int nextLevel = currentLevel + 1;

        if (nextLevel <= GameProgressManager.MAX_LEVEL) {
            builder.setPositiveButton("Next Level", (dialog, which) -> {
                onLevelCompleted();
            });
            builder.setNeutralButton("Level Select", (dialog, which) -> {
                finish();
            });
        } else {
            builder.setPositiveButton("OK", (dialog, which) -> {
                finish();
            });
        }

        builder.show();
    }

    private void showTapToContinueOverlay(String unlockMessage) {
        // Tạo overlay view
        View overlayView = getLayoutInflater().inflate(R.layout.overlay_tap_to_continue, null);

        TextView messageText = overlayView.findViewById(R.id.tapToContinueText);
        if (unlockMessage != null) {
            messageText.setText(unlockMessage + "\n\nTap to continue to next level");
        } else {
            messageText.setText("Tap to continue to next level");
        }

        // Add overlay to root view
        ViewGroup rootView = findViewById(android.R.id.content);
        rootView.addView(overlayView);

        // ✅ THÊM: Fade in animation cho overlay
        overlayView.setAlpha(0f);
        overlayView.animate()
                .alpha(1f)
                .setDuration(500)
                .start();

        // Click anywhere to continue
        overlayView.setOnClickListener(v -> {
            // ✅ Fade out animation
            overlayView.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        rootView.removeView(overlayView);
                        puzzleView.hideCompletionImage();

                        // Show completion dialog
                        showCompletionDialog();
                    })
                    .start();
        });

        TextView fingerIcon = overlayView.findViewById(R.id.fingerIcon);
        if (fingerIcon != null) {
            android.view.animation.AlphaAnimation blink = new android.view.animation.AlphaAnimation(0.3f, 1.0f);
            blink.setDuration(800);
            blink.setRepeatCount(android.view.animation.Animation.INFINITE);
            blink.setRepeatMode(android.view.animation.Animation.REVERSE);
            fingerIcon.startAnimation(blink);
        }
    }

    private void cancelAllCompletionAnimations() {
        // Cancel all pending handlers
        handler.removeCallbacksAndMessages(null);

        // Stop celebration effects
        if (konfettiView != null) {
            konfettiView.stopGracefully();
        }

        // Stop scale animation
        if (levelCompleteText != null) {
            levelCompleteText.clearAnimation();
        }
    }

    /**
     * Show completion with FULL CELEBRATION EFFECTS
     */
    private void showCompletionAnimation() {
        // === GIAI ĐOẠN 1: Show Full Image (KHÔNG có animation) ===
        completionOverlay.setVisibility(View.VISIBLE);
        completionOverlay.setAlpha(1f);

        // ✅ ẨN tất cả effects ban đầu
        if (konfettiView != null) konfettiView.setVisibility(View.INVISIBLE);
        if (glowOverlay != null) glowOverlay.setVisibility(View.INVISIBLE);
        if (levelCompleteText != null) levelCompleteText.setVisibility(View.INVISIBLE);


        // Fade in overlay
        completionOverlay.animate()
                .alpha(1f)
                .setDuration(1000)
                .withEndAction(() -> {
                    // Full image đã hiển thị, CHỜ 3 giây trước khi start animation
                    handler.postDelayed(() -> {
                        startCelebrationEffects();
                    }, 3000); // ← CHỜ 3 giây để xem full image

                    // Sau 6 giây (3s xem + 3s animation), navigate
//                    handler.postDelayed(() -> {
//                        onLevelCompleted();
//                    }, 6000); // ← Tổng 6 giây
                })
                .start();
    }

    /**
     * Start all celebration effects combo
     */
    private void startCelebrationEffects() {
        // ✅ Show effects overlay
        if (konfettiView != null) konfettiView.setVisibility(View.VISIBLE);
        if (levelCompleteText != null) levelCompleteText.setVisibility(View.VISIBLE);

        // 1. Play celebration sound
        playCelebrationSound();

        // 2. Start confetti
        startKonfettiEffect();

        // 3. Glow effect
//        startGlowEffect();

        // 4. Scale bounce animation
//        startScaleBounceEffect();

        // 5. Blink level complete text
        handler.postDelayed(() -> {
            blinkLevelCompleteText();
        }, 1000);

        // 6. Haptic feedback pattern
        playVictoryVibration();
    }

    /**
     * Konfetti effect - confetti falling from top
     */
    private void startKonfettiEffect() {
        if (konfettiView == null) return;

        // Play confetti pop sound
        playConfettiSound();

        // Create party configuration
        nl.dionsegijn.konfetti.core.emitter.EmitterConfig emitterConfig =
                new nl.dionsegijn.konfetti.core.emitter.Emitter(300, java.util.concurrent.TimeUnit.MILLISECONDS).max(100);

        nl.dionsegijn.konfetti.core.Party party = new nl.dionsegijn.konfetti.core.PartyFactory(emitterConfig)
                .spread(360)
                .shapes(java.util.Arrays.asList(
                        nl.dionsegijn.konfetti.core.models.Shape.Square.INSTANCE,
                        nl.dionsegijn.konfetti.core.models.Shape.Circle.INSTANCE
                ))
                .colors(java.util.Arrays.asList(
                        0xFFFFD700, // Gold
                        0xFFFF6B6B, // Red
                        0xFF4ECDC4, // Cyan
                        0xFF45B7D1, // Blue
                        0xFFFFA07A, // Orange
                        0xFF98D8C8  // Green
                ))
                .setSpeedBetween(0f, 30f)
                .position(new nl.dionsegijn.konfetti.core.Position.Relative(0.0, 0.0)
                        .between(new nl.dionsegijn.konfetti.core.Position.Relative(1.0, 0.0)))
                .build();

        konfettiView.start(party);

        // Second burst after 800ms
        handler.postDelayed(() -> {
            konfettiView.start(party);
        }, 800);
    }

    /**
     * Glow effect - pulsating glow around image
     */
    private void startGlowEffect() {
        if (glowOverlay == null) return;

        // Set glow color (gold)

        // ✅ Làm mờ hơn nhiều: 0x40 → 0x10 (chỉ 6% opacity)
        glowOverlay.setBackgroundColor(0x10FFD700); // Vàng cực nhẹ
        glowOverlay.setVisibility(View.VISIBLE);

        // Animate glow pulsing
        android.animation.ObjectAnimator glowAnim = android.animation.ObjectAnimator.ofFloat(
                glowOverlay, "alpha", 0f, 0.6f, 0f, 0.6f, 0f
        );
        glowAnim.setDuration(2000);
        glowAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        glowAnim.start();
    }

    /**
     * Scale bounce effect - image bounces with overshoot
     */
    private void startScaleBounceEffect() {
        View puzzleContainer = findViewById(R.id.completionPuzzleView);
        if (puzzleContainer == null) return;

        puzzleContainer.setScaleX(0.8f);
        puzzleContainer.setScaleY(0.8f);

        puzzleContainer.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(400)
                .setInterpolator(new android.view.animation.OvershootInterpolator(2.0f))
                .withEndAction(() -> {
                    puzzleContainer.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(250)
                            .start();
                })
                .start();
    }

    /**
     * Victory vibration pattern
     */
    private void playVictoryVibration() {
        if (!SettingsActivity.isVibrationEnabled(this)) return;

        android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Pattern: short-short-long-short-long
                long[] pattern = {0, 100, 50, 100, 50, 300, 50, 100, 50, 400};
                android.os.VibrationEffect effect = android.os.VibrationEffect.createWaveform(pattern, -1);
                vibrator.vibrate(effect);
            } else {
                vibrator.vibrate(500);
            }
        }
    }

    /**
     * Blink "LEVEL COMPLETE" text (1 second, 2 blinks)
     */
    private void blinkLevelCompleteText() {
        levelCompleteText.setVisibility(View.VISIBLE);
        levelCompleteText.setAlpha(1f);

        AlphaAnimation blink = new AlphaAnimation(0.2f, 1.0f);
        blink.setDuration(400);
        blink.setRepeatCount(2); // 2 full blinks
        blink.setRepeatMode(Animation.REVERSE);

        levelCompleteText.startAnimation(blink);
    }

    /**
     * Auto navigate to next level or mode select
     */
    private void onLevelCompleted() {
        // ✅ MARK COMPLETED NGAY KHI HOÀN THÀNH
        progressManager.markLevelCompleted(gameMode, currentLevel);
        progressManager.clearGameState(gameMode, currentLevel);
        progressManager.addGalleryPiece(currentLevel - 1);

        // ✅ THÊM: Auto-download pack cho level tiếp theo
        PreDownloadManager preDownloadManager = new PreDownloadManager(this);
        preDownloadManager.autoDownloadNextPack(currentLevel);

        // ✅ REWARD COINS - Rút gọn toast message
        int reward = CoinManager.getRewardForLevel(gameMode);
        coinManager.addCoins(reward);

        // Toast gọn hơn với formatted coin
        Toast.makeText(this,
                "🪙 " + CoinManager.formatRewardDisplay(reward) + " coins!",
                Toast.LENGTH_SHORT).show();

        updateCoinDisplay(); // ← Cập nhật UI

        int nextLevel = currentLevel + 1;

        if (nextLevel > GameProgressManager.MAX_LEVEL) {
            // Last level completed
            String message = "🎉 Congratulations!\nYou've completed all levels in " + getModeDisplayName(gameMode) + " mode!";

            String unlockMessage = progressManager.getUnlockMessage(gameMode, currentLevel);
            if (unlockMessage != null) {
                message += "\n\n" + unlockMessage;
            }

            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            interstitialAdManager.onLevelCompleted(this, new InterstitialAdManager.AdCallback() {
                @Override
                public void onAdShown() {
                    Log.d(TAG, "Interstitial ad shown");
                }

                @Override
                public void onAdDismissed() {
                    handler.postDelayed(() -> {
                        finish();
                    }, 2000);
                }

                @Override
                public void onAdFailedToShow() {
                    handler.postDelayed(() -> {
                        finish();
                    }, 2000);
                }
            });

        } else {
            // ✅ FIX: Đảm bảo level tiếp theo được unlock trước khi navigate
            interstitialAdManager.onLevelCompleted(this, new InterstitialAdManager.AdCallback() {
                @Override
                public void onAdShown() {
                    Log.d(TAG, "Interstitial ad shown");
                }

                public void onAdDismissed() {
                    Intent intent = new Intent(GameActivity.this, GameActivity.class);
                    intent.putExtra("LEVEL", nextLevel);
                    intent.putExtra("MODE", gameMode);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }

                @Override
                public void onAdFailedToShow() {
                    Intent intent = new Intent(GameActivity.this, GameActivity.class);
                    intent.putExtra("LEVEL", nextLevel);
                    intent.putExtra("MODE", gameMode);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            });
        }
    }

    private String getModeDisplayName(String mode) {
        switch (mode) {
            case GameMode.MODE_EASY: return "Easy";
            case GameMode.MODE_NORMAL: return "Normal";
            case GameMode.MODE_HARD: return "Hard";
            case GameMode.MODE_INSANE: return "Insane";
            default: return mode;
        }
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
        if (puzzleView == null || !puzzleView.isInitialized()) {
            Log.d(TAG, "Puzzle not initialized yet, skip save");
            return;
        }

        try {
            GameSaveData saveData = puzzleView.getSaveData();
            if (saveData == null) {
                Log.e(TAG, "getSaveData returned null");
                return;
            }

            saveData.level = currentLevel;
            progressManager.saveGameState(gameMode, currentLevel, saveData);
            Toast.makeText(this, "💾 Game saved!", Toast.LENGTH_SHORT).show();
            playClickSound();
        } catch (Exception e) {
            Log.e(TAG, "Error saving game", e);
        }
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

    private void playCelebrationSound() {
        if (SettingsActivity.isSoundEnabled(this) && celebrationSound != null) {
            try {
                float volume = SettingsActivity.getSoundVolumeFloat(this);
                celebrationSound.setVolume(volume, volume);
                celebrationSound.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void playConfettiSound() {
        if (SettingsActivity.isSoundEnabled(this) && confettiSound != null) {
            try {
                float volume = SettingsActivity.getSoundVolumeFloat(this);
                confettiSound.setVolume(volume, volume);
                confettiSound.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void recycleBitmap() {
        if (currentPuzzleBitmap != null && !currentPuzzleBitmap.isRecycled()) {
            currentPuzzleBitmap.recycle();
            currentPuzzleBitmap = null;
        }
    }

    private void initSounds() {
        try {
            if (SettingsActivity.isSoundEnabled(this)) {
                successSound = MediaPlayer.create(this, R.raw.success_sound);
                clickSound = MediaPlayer.create(this, R.raw.click_sound);

                // Try to load celebration sounds (may not exist yet)
                try {
                    celebrationSound = MediaPlayer.create(this, R.raw.success_sound); // Use existing for now
                    confettiSound = MediaPlayer.create(this, R.raw.click_sound); // Use existing for now
                } catch (Exception e) {
                    Log.d(TAG, "Celebration sounds not found, using defaults");
                }

                if (successSound != null) {
                    float volume = SettingsActivity.getSoundVolumeFloat(this);
                    successSound.setVolume(volume, volume);
                }
                if (clickSound != null) {
                    float volume = SettingsActivity.getSoundVolumeFloat(this);
                    clickSound.setVolume(volume, volume);
                }
                if (celebrationSound != null) {
                    float volume = SettingsActivity.getSoundVolumeFloat(this);
                    celebrationSound.setVolume(volume, volume);
                }
                if (confettiSound != null) {
                    float volume = SettingsActivity.getSoundVolumeFloat(this);
                    confettiSound.setVolume(volume, volume);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Sound initialization failed", e);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Don't auto-save if showing ad or puzzle completed
        if (!isShowingAd &&
                SettingsActivity.isAutoSaveEnabled(this) &&
                puzzleView != null &&
                puzzleView.isInitialized() &&  // ← THÊM CHECK NÀY
                !puzzleView.isPuzzleCompleted()) {
            saveGame();
        }
    }

    private void useAutoSolve() {
        if (isShowingAd) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        powerUpsManager.usePowerUp(PowerUpsManager.PowerUpType.AUTO_SOLVE, new PowerUpsManager.PowerUpCallback() {
            @Override
            public void onSuccess() {
                isShowingAd = true;

                boolean solved = puzzleView.autoSolveOnePiece();

                handler.postDelayed(() -> {
                    isShowingAd = false;

                    if (solved) {
                        if (!puzzleView.isPuzzleCompleted()) {
                            Toast.makeText(GameActivity.this, "✨ One piece auto-solved!", Toast.LENGTH_SHORT).show();
                            updateStats();
                            updatePowerUpButtons();
                            updateCoinDisplay(); // ← Cập nhật coin sau khi spend
                        }
                    } else {
                        Toast.makeText(GameActivity.this, "No pieces to auto-solve!", Toast.LENGTH_SHORT).show();
                    }
                }, 1500);
            }

            @Override
            public void onFailed(String reason) {
                isShowingAd = false;
                Toast.makeText(GameActivity.this, reason, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void useShuffle() {
        if (isShowingAd) {
            Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        powerUpsManager.usePowerUp(PowerUpsManager.PowerUpType.SHUFFLE, new PowerUpsManager.PowerUpCallback() {
            @Override
            public void onSuccess() {
                isShowingAd = true;

                boolean shuffled = puzzleView.shuffleRemainingPieces();

                handler.postDelayed(() -> {
                    isShowingAd = false;

                    if (shuffled) {
                        Toast.makeText(GameActivity.this, "🔀 Pieces shuffled!", Toast.LENGTH_SHORT).show();
                        currentStreak = 0;
                        updateStats();
                        updatePowerUpButtons();
                        updateCoinDisplay(); // ← Cập nhật coin sau khi spend
                    } else {
                        Toast.makeText(GameActivity.this, "No pieces to shuffle!", Toast.LENGTH_SHORT).show();
                    }
                }, 1500);
            }

            @Override
            public void onFailed(String reason) {
                isShowingAd = false;
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

        int progress = (correctPieces * 100) / totalPieces;
        progressBar.setProgress(progress);

        lockedCountText.setText(String.valueOf(lockedPieces));
        remainingCountText.setText(String.valueOf(remainingPieces));
        streakCountText.setText(String.valueOf(currentStreak));

        progressText.setText(correctPieces + "/" + totalPieces + " (" + progress + "%)");
    }

    private void updateCoinDisplay() {
        if (coinCountGameText != null && coinManager != null) {
            coinCountGameText.setText(coinManager.getFormattedCoinsWithIcon());
        }
    }

    private PuzzlePiece getLastMovedPiece() {
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacksAndMessages(null);
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
        if (celebrationSound != null) {
            celebrationSound.release();
            celebrationSound = null;
        }
        if (confettiSound != null) {
            confettiSound.release();
            confettiSound = null;
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
    }

    private void showFullscreenImage() {
        if (currentPuzzleBitmap != null) {
            fullscreenImageView.setImageBitmap(currentPuzzleBitmap);
            fullscreenOverlay.setVisibility(View.VISIBLE);

            fullscreenOverlay.setAlpha(0f);
            fullscreenOverlay.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        }
    }

    private void hideFullscreenImage() {
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
        if (fullscreenOverlay.getVisibility() == View.VISIBLE) {
            hideFullscreenImage();
        } else {
            showExitDialog();
        }
    }
}