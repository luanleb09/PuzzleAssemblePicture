package com.example.puzzle_assemble_picture;

/**
 * ✅ Game Configuration
 * Contains all game constants and costs
 */
public class GameConfig {

    // ===== POWER-UP COSTS (in coins) =====

    /**
     * 🎯 Auto-Solve: Automatically place one random piece correctly
     * Cost: 60 coins
     */
    public static final int COST_AUTO_SOLVE = 60;

    /**
     * 🔀 Shuffle: Shuffle all remaining pieces
     * Cost: 40 coins
     */
    public static final int COST_SHUFFLE = 40;

    /**
     * 📐 Solve Corners: Automatically place all 4 corner pieces
     * Cost: 120 coins (more expensive because solves 4 pieces)
     */
    public static final int COST_SOLVE_CORNERS = 120;

    /**
     * 🔲 Solve Edges: Automatically place all edge pieces
     * Cost: 160 coins (most expensive because solves many pieces)
     */
    public static final int COST_SOLVE_EDGES = 160;

    /**
     * 👁️ Reveal Preview: Show full image for 10 seconds (INSANE mode only)
     * Cost: 60 coins
     */
    public static final int COST_REVEAL_PREVIEW = 60;

    // ===== LEVEL REWARDS (coins earned per level) =====

    public static final int REWARD_EASY = 20;
    public static final int REWARD_NORMAL = 50;
    public static final int REWARD_HARD = 100;
    public static final int REWARD_INSANE = 200;

    // ===== GAME SETTINGS =====

    public static final int MAX_LEVEL = 100;
    public static final int PIECES_PER_ROW_EASY = 3;
    public static final int PIECES_PER_ROW_NORMAL = 4;
    public static final int PIECES_PER_ROW_HARD = 5;
    public static final int PIECES_PER_ROW_INSANE = 6;

    public static final int COINS_PER_LEVEL_EASY = 30;
    public static final int COINS_PER_LEVEL_NORMAL = 50;
    public static final int COINS_PER_LEVEL_HARD = 70;
    public static final int COINS_PER_LEVEL_INSANE = 100;
    public static final int INITIAL_COINS = 0;

    // ===== AD SETTINGS =====

    /**
     * Frequency of interstitial ads (every N levels)
     */
    public static final int INTERSTITIAL_AD_FREQUENCY = 3;

    /**
     * Test rewarded ad unit ID (replace with production ID)
     */
    public static final String REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917";

    /**
     * Test interstitial ad unit ID (replace with production ID)
     */
    public static final String INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712";
    public static final String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";

    // ===== SHOP ITEMS =====

    public static final int SHOP_COINS_SMALL = 100;
    public static final int SHOP_COINS_MEDIUM = 500;
    public static final int SHOP_COINS_LARGE = 1000;
    public static final int SHOP_COINS_MEGA = 5000;


    // Private constructor to prevent instantiation
    private GameConfig() {
        throw new AssertionError("Cannot instantiate GameConfig");
    }
}