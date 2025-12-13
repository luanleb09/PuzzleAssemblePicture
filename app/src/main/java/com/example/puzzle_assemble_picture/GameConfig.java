package com.example.puzzle_assemble_picture;

import android.content.Context;
import android.content.SharedPreferences;

public class GameConfig {
    // Coin rewards per level completion
    public static final int COINS_PER_LEVEL_EASY = 10;
    public static final int COINS_PER_LEVEL_NORMAL = 15;
    public static final int COINS_PER_LEVEL_HARD = 20;
    public static final int COINS_PER_LEVEL_INSANE = 30;

    // Power-up costs
    public static final int COST_AUTO_SOLVE = 50;
    public static final int COST_SHUFFLE = 30;
    public static final int COST_SOLVE_CORNERS = 200;
    public static final int COST_SOLVE_EDGES = 500;
    public static final int COST_REVEAL_PREVIEW = 100;

    public static final int PRICE_AUTO_SOLVE = 50;
    public static final int PRICE_SHUFFLE = 30;
    public static final int PRICE_HINT = 20;

    // Initial coins
    public static final int INITIAL_COINS = 100;
}
