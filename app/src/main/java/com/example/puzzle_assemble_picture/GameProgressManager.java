package com.example.puzzle_assemble_picture;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class GameProgressManager {
    private static final String TAG = "GameProgressManager";
    private static final String PREFS_NAME = "PuzzleProgress";
    private static final String KEY_GALLERY_PIECES = "gallery_pieces";
    private static final String KEY_GAME_STATE = "game_state_";
    private static final String KEY_CURRENT_LEVEL = "current_level_";
    private static final String KEY_COMPLETED_LEVELS = "completed_levels_";
    private static final String KEY_ACHIEVEMENTS = "achievements";

    // Achievement constants
    public static final int PIECES_PER_ACHIEVEMENT = 10;
    public static final int TOTAL_ACHIEVEMENTS = 10;

    // Game constants
    public static final int MAX_LEVEL = 300;
    public static final int UNLOCK_NORMAL_AT = 20;
    public static final int UNLOCK_HARD_AT = 20;
    public static final int UNLOCK_INSANE_AT = 20;

    private SharedPreferences prefs;
    private Gson gson;

    public GameProgressManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // ===== LEVEL PROGRESS METHODS (PER MODE) =====

    /**
     * Get current level for a specific mode
     */
    public int getCurrentLevel(String mode) {
        return prefs.getInt(KEY_CURRENT_LEVEL + mode, 1);
    }

    /**
     * Set current level for a specific mode
     */
    public void setCurrentLevel(String mode, int level) {
        prefs.edit().putInt(KEY_CURRENT_LEVEL + mode, level).apply();
    }

    /**
     * Check if a level is completed in a specific mode
     */
    public boolean isLevelCompleted(String mode, int level) {
        return prefs.getBoolean(KEY_COMPLETED_LEVELS + mode + "_" + level, false);
    }

    /**
     * Mark a level as completed and unlock next level
     */
    public void markLevelCompleted(String mode, int currentLevel) {
        SharedPreferences.Editor editor = prefs.edit();

        // Mark level as completed
        editor.putBoolean(KEY_COMPLETED_LEVELS + mode + "_" + currentLevel, true);

        // ✅ Auto unlock next level
        int nextLevel = currentLevel + 1;
        if (nextLevel <= MAX_LEVEL) {
            int savedLevel = getCurrentLevel(mode);
            if (nextLevel > savedLevel) {
                editor.putInt(KEY_CURRENT_LEVEL + mode, nextLevel);
                Log.d(TAG, "✅ Unlocked level " + nextLevel + " in " + mode + " mode");
            }
        }

        editor.apply();
        Log.d(TAG, "✅ Level " + currentLevel + " completed in " + mode + " mode");
    }

    /**
     * Get total completed levels in a specific mode
     */
    public int getCompletedLevelsInMode(String mode) {
        int count = 0;
        for (int level = 1; level <= MAX_LEVEL; level++) {
            if (isLevelCompleted(mode, level)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get completion percentage for a specific mode
     */
    public float getModeCompletionPercentage(String mode) {
        int completed = getCompletedLevelsInMode(mode);
        return (completed * 100f) / MAX_LEVEL;
    }

    /**
     * Get total completed levels across ALL modes
     */
    public int getTotalCompletedLevelsAllModes() {
        int total = 0;
        String[] allModes = {GameMode.MODE_EASY, GameMode.MODE_NORMAL,
                GameMode.MODE_HARD, GameMode.MODE_INSANE};

        for (String mode : allModes) {
            total += getCompletedLevelsInMode(mode);
        }

        return total;
    }

    /**
     * Get overall completion percentage (all modes)
     */
    public float getOverallCompletionPercentage() {
        int totalCompleted = getTotalCompletedLevelsAllModes();
        int totalLevels = MAX_LEVEL * 4; // 4 modes
        return (totalCompleted * 100f) / totalLevels;
    }

    // ===== MODE UNLOCK METHODS =====

    /**
     * Check if a mode is unlocked
     */
    public boolean isModeUnlocked(String mode) {
        switch (mode) {
            case GameMode.MODE_EASY:
                return true; // Easy always unlocked

            case GameMode.MODE_NORMAL:
                return isLevelCompleted(GameMode.MODE_EASY, UNLOCK_NORMAL_AT);

            case GameMode.MODE_HARD:
                return isLevelCompleted(GameMode.MODE_NORMAL, UNLOCK_HARD_AT);

            case GameMode.MODE_INSANE:
                return isLevelCompleted(GameMode.MODE_HARD, UNLOCK_INSANE_AT);

            default:
                return false;
        }
    }

    /**
     * Get unlock message when completing a level
     */
    public String getUnlockMessage(String mode, int currentLevel) {
        if (mode.equals(GameMode.MODE_EASY)) {
            if (currentLevel == UNLOCK_NORMAL_AT) {
                return "🎉 Normal Mode Unlocked!";
            }
        } else if (mode.equals(GameMode.MODE_NORMAL)) {
            if (currentLevel == UNLOCK_HARD_AT) {
                return "🎉 Hard Mode Unlocked!";
            }
        } else if (mode.equals(GameMode.MODE_HARD)) {
            if (currentLevel == UNLOCK_INSANE_AT) {
                return "🎉 Insane Mode Unlocked!";
            }
        }
        return null;
    }

    // ===== GRID SIZE =====

    /**
     * Get grid size based on level
     */
    public int getGridSizeForLevel(int level) {
        // Bắt đầu từ 5x5, tăng dần
        if (level <= 5) {
            return 5;  // Level 1-5: 5×5 (25 pieces)
        } else if (level <= 10) {
            return 6;  // Level 6-10: 6×6 (36 pieces)
        } else if (level <= 15) {
            return 7;  // Level 11-15: 7×7 (49 pieces)
        } else if (level <= 20) {
            return 8;  // Level 16-20: 8×8 (64 pieces)
        } else if (level <= 25) {
            return 9;  // Level 21-25: 9×9 (81 pieces)
        } else if (level <= 30) {
            return 10; // Level 26-30: 10×10 (100 pieces)
        } else {
            return 11; // Level 31+: 11×11 (121 pieces)
        }
    }

    // ===== GALLERY METHODS =====

    /**
     * Add a piece to gallery
     */
    public void addGalleryPiece(int pieceIndex) {
        String currentPieces = prefs.getString(KEY_GALLERY_PIECES, "");
        if (!currentPieces.contains("," + pieceIndex + ",")) {
            currentPieces += pieceIndex + ",";
            prefs.edit().putString(KEY_GALLERY_PIECES, currentPieces).apply();

            // Auto check for new achievements
            Integer newAchievement = checkAndUnlockAchievement();
            if (newAchievement != null) {
                Log.d(TAG, "New achievement unlocked: " + getAchievementName(newAchievement));
            }
        }
    }

    /**
     * Check if a gallery piece is unlocked
     */
    public boolean isGalleryPieceUnlocked(int pieceIndex) {
        String pieces = prefs.getString(KEY_GALLERY_PIECES, "");
        return pieces.contains("," + pieceIndex + ",");
    }

    /**
     * Get count of unlocked gallery pieces
     */
    public int getUnlockedPiecesCount() {
        String pieces = prefs.getString(KEY_GALLERY_PIECES, "");
        if (pieces.isEmpty()) return 0;

        String[] pieceArray = pieces.split(",");
        int count = 0;
        for (String piece : pieceArray) {
            if (!piece.isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get list of unlocked gallery pieces
     */
    public List<Integer> getGalleryPieces() {
        List<Integer> pieceList = new ArrayList<>();
        String pieces = prefs.getString(KEY_GALLERY_PIECES, "");

        if (!pieces.isEmpty()) {
            String[] pieceArray = pieces.split(",");
            for (String piece : pieceArray) {
                if (!piece.isEmpty()) {
                    try {
                        pieceList.add(Integer.parseInt(piece));
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid piece index: " + piece);
                    }
                }
            }
        }

        return pieceList;
    }

    /**
     * Clear all gallery pieces
     */
    public void clearAllGalleryPieces() {
        prefs.edit().remove(KEY_GALLERY_PIECES).apply();
    }

    // ===== ACHIEVEMENT METHODS =====

    /**
     * Check if achievement is unlocked
     */
    public boolean isAchievementUnlocked(int achievementIndex) {
        int requiredPieces = (achievementIndex + 1) * PIECES_PER_ACHIEVEMENT;
        int unlockedPieces = getUnlockedPiecesCount();
        return unlockedPieces >= requiredPieces;
    }

    /**
     * Get list of unlocked achievements
     */
    public List<Integer> getUnlockedAchievements() {
        List<Integer> unlockedList = new ArrayList<>();
        for (int i = 0; i < TOTAL_ACHIEVEMENTS; i++) {
            if (isAchievementUnlocked(i)) {
                unlockedList.add(i);
            }
        }
        return unlockedList;
    }

    /**
     * Get total number of achievements
     */
    public int getTotalAchievements() {
        return TOTAL_ACHIEVEMENTS;
    }

    /**
     * Get number of unlocked achievements
     */
    public int getUnlockedAchievementsCount() {
        return getUnlockedAchievements().size();
    }

    /**
     * Get achievement progress (0-100%)
     */
    public int getAchievementProgress(int achievementIndex) {
        int startPiece = achievementIndex * PIECES_PER_ACHIEVEMENT;
        int endPiece = startPiece + PIECES_PER_ACHIEVEMENT - 1;

        int unlockedInRange = 0;
        for (int i = startPiece; i <= endPiece; i++) {
            if (isGalleryPieceUnlocked(i)) {
                unlockedInRange++;
            }
        }

        return (unlockedInRange * 100) / PIECES_PER_ACHIEVEMENT;
    }

    /**
     * Get achievement name
     */
    public String getAchievementName(int achievementIndex) {
        switch (achievementIndex) {
            case 0: return "Beginner";
            case 1: return "Novice";
            case 2: return "Apprentice";
            case 3: return "Intermediate";
            case 4: return "Advanced";
            case 5: return "Expert";
            case 6: return "Master";
            case 7: return "Grandmaster";
            case 8: return "Legend";
            case 9: return "Ultimate Champion";
            default: return "Achievement " + (achievementIndex + 1);
        }
    }

    /**
     * Get achievement description
     */
    public String getAchievementDescription(int achievementIndex) {
        int requiredPieces = (achievementIndex + 1) * PIECES_PER_ACHIEVEMENT;
        return "Collect " + requiredPieces + " gallery pieces";
    }

    /**
     * Get achievement emoji
     */
    public String getAchievementEmoji(int achievementIndex) {
        switch (achievementIndex) {
            case 0: return "🌱";
            case 1: return "🌿";
            case 2: return "🌳";
            case 3: return "⭐";
            case 4: return "🌟";
            case 5: return "💫";
            case 6: return "🏆";
            case 7: return "👑";
            case 8: return "💎";
            case 9: return "🔥";
            default: return "🎯";
        }
    }

    /**
     * Check and unlock achievement if conditions met
     * Returns newly unlocked achievement index or null
     */
    public Integer checkAndUnlockAchievement() {
        int unlockedCount = getUnlockedPiecesCount();

        for (int i = 0; i < TOTAL_ACHIEVEMENTS; i++) {
            int requiredPieces = (i + 1) * PIECES_PER_ACHIEVEMENT;

            if (unlockedCount >= requiredPieces) {
                String key = KEY_ACHIEVEMENTS + "_" + i;
                if (!prefs.getBoolean(key, false)) {
                    prefs.edit().putBoolean(key, true).apply();
                    return i;
                }
            }
        }

        return null;
    }

    /**
     * Reset all achievements
     */
    public void resetAchievements() {
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < TOTAL_ACHIEVEMENTS; i++) {
            editor.remove(KEY_ACHIEVEMENTS + "_" + i);
        }
        editor.apply();
    }

    // ===== GAME STATE (SAVE/LOAD) =====

    /**
     * Save game state for a specific mode and level
     */
    public void saveGameState(String mode, int level, GameSaveData saveData) {
        String key = KEY_GAME_STATE + mode + "_" + level;
        String json = gson.toJson(saveData);
        prefs.edit().putString(key, json).apply();
        Log.d(TAG, "Game state saved for " + mode + " level " + level);
    }

    /**
     * Load game state for a specific mode and level
     */
    public GameSaveData loadGameState(String mode, int level) {
        String key = KEY_GAME_STATE + mode + "_" + level;
        String json = prefs.getString(key, null);
        if (json != null) {
            return gson.fromJson(json, GameSaveData.class);
        }
        return null;
    }

    /**
     * Check if saved game exists
     */
    public boolean hasSavedGame(String mode, int level) {
        String key = KEY_GAME_STATE + mode + "_" + level;
        return prefs.contains(key);
    }

    /**
     * Clear saved game state
     */
    public void clearGameState(String mode, int level) {
        String key = KEY_GAME_STATE + mode + "_" + level;
        prefs.edit().remove(key).apply();
        Log.d(TAG, "Game state cleared for " + mode + " level " + level);
    }

    /**
     * Clear all saved games
     */
    public void clearAllSavedGames() {
        SharedPreferences.Editor editor = prefs.edit();
        String[] allModes = {GameMode.MODE_EASY, GameMode.MODE_NORMAL,
                GameMode.MODE_HARD, GameMode.MODE_INSANE};

        for (String mode : allModes) {
            for (int level = 1; level <= MAX_LEVEL; level++) {
                String key = KEY_GAME_STATE + mode + "_" + level;
                editor.remove(key);
            }
        }

        editor.apply();
        Log.d(TAG, "All saved games cleared");
    }

    // ===== RESET/DEBUG METHODS =====

    /**
     * Reset all progress (for testing)
     */
    public void resetAllProgress() {
        prefs.edit().clear().apply();
        Log.d(TAG, "All progress reset");
    }

    /**
     * Reset progress for a specific mode
     */
    public void resetModeProgress(String mode) {
        SharedPreferences.Editor editor = prefs.edit();

        // Clear current level
        editor.remove(KEY_CURRENT_LEVEL + mode);

        // Clear all completed levels
        for (int level = 1; level <= MAX_LEVEL; level++) {
            editor.remove(KEY_COMPLETED_LEVELS + mode + "_" + level);
            editor.remove(KEY_GAME_STATE + mode + "_" + level);
        }

        editor.apply();
        Log.d(TAG, "Progress reset for " + mode + " mode");
    }
}