package com.example.puzzle_assemble_picture;

public class ShopConfig {

    // ===== COINS PACKAGES =====
    public static class CoinsPackage {
        public String id;
        public String name;
        public int amount;
        public int price; // Real money price in cents (USD)
        public String icon;
        public boolean isPopular;

        public CoinsPackage(String id, String name, int amount, int price, String icon, boolean isPopular) {
            this.id = id;
            this.name = name;
            this.amount = amount;
            this.price = price;
            this.icon = icon;
            this.isPopular = isPopular;
        }
    }

    // ===== POWER-UPS =====
    public static class PowerUp {
        public String id;
        public String name;
        public int coinPrice;
        public String description;
        public String icon;

        public PowerUp(String id, String name, int coinPrice, String description, String icon) {
            this.id = id;
            this.name = name;
            this.coinPrice = coinPrice;
            this.description = description;
            this.icon = icon;
        }
    }

    // ===== COINS PACKAGES CATALOG =====
    public static final CoinsPackage[] COINS_PACKAGES = {
            new CoinsPackage("coins_small", "Small Bag", 100, 99, "💰", false),
            new CoinsPackage("coins_medium", "Medium Bag", 500, 499, "💎", true),
            new CoinsPackage("coins_large", "Large Bag", 1200, 999, "👑", false),
            new CoinsPackage("coins_huge", "Huge Bag", 2500, 1999, "🏆", false),
            new CoinsPackage("coins_mega", "Mega Bag", 5500, 3999, "⭐", false),
            new CoinsPackage("coins_ultimate", "Ultimate Bag", 12000, 7999, "🔥", false)
    };

    // ===== POWER-UPS CATALOG =====
    public static final PowerUp[] POWER_UPS = {
            // ✅ Essential Power-ups (được sử dụng trong game)
            new PowerUp(
                    "auto_solve_pack",
                    "🎯 Auto-Solve Pack",
                    50,
                    "Add 3 auto-solves to help complete puzzles",
                    "🎯"
            ),
            new PowerUp(
                    "shuffle_pack",
                    "🔀 Shuffle Pack",
                    30,
                    "Add 5 shuffles to rearrange remaining pieces",
                    "🔀"
            ),

            // ✅ Additional Power-ups (coming soon features)
            new PowerUp(
                    "solve_corners",
                    "📐 Solve 4 Pieces Corners",
                    200,
                    "Auto-place all 4 corner pieces instantly",
                    "📐"
            ),
            new PowerUp(
                    "solve_edges",
                    "🔲 Solve All Edges",
                    400,
                    "Auto-place all edge pieces instantly",
                    "🔲"
            ),
            new PowerUp(
                    "reveal_preview",
                    "👁️ Reveal Preview",
                    80,
                    "Show full image for 10 seconds (use for Insane mode)",
                    "👁️"
            ),
    };

    // ===== IN-APP PURCHASE IDs (for Google Play) =====
    public static final String IAP_COINS_SMALL = "coins_100";
    public static final String IAP_COINS_MEDIUM = "coins_500";
    public static final String IAP_COINS_LARGE = "coins_1200";
    public static final String IAP_COINS_HUGE = "coins_2500";
    public static final String IAP_COINS_MEGA = "coins_5500";
    public static final String IAP_COINS_ULTIMATE = "coins_12000";

    // ===== PRICES & REWARDS =====

    // Daily bonus
    public static final int DAILY_LOGIN_BONUS = 10;

    // Level completion rewards (per mode)
    public static final int REWARD_EASY = 5;
    public static final int REWARD_NORMAL = 10;
    public static final int REWARD_HARD = 15;
    public static final int REWARD_INSANE = 25;

    // Achievement rewards
    public static final int REWARD_FIRST_WIN = 50;
    public static final int REWARD_10_WINS = 100;
    public static final int REWARD_50_WINS = 250;
    public static final int REWARD_100_WINS = 500;

    // Power-up prices (helper getters - for future dynamic pricing)
    public static int getAutoSolvePackPrice() { return 50; }
    public static int getShufflePackPrice() { return 30; }
    public static int getHintPrice() { return 20; }
    public static int getUnlockCornersPrice() { return 40; }
    public static int getUnlockEdgesPrice() { return 60; }
    public static int getTimeFreezePrice() { return 35; }
    public static int getDoubleCoinsPrice() { return 100; }

    // ===== HELPER METHODS =====

    /**
     * Get coins package by ID
     */
    public static CoinsPackage getCoinsPackageById(String id) {
        if (id == null || COINS_PACKAGES == null) return null;

        for (CoinsPackage pkg : COINS_PACKAGES) {
            if (pkg != null && id.equals(pkg.id)) {
                return pkg;
            }
        }
        return null;
    }

    /**
     * Get power-up by ID
     */
    public static PowerUp getPowerUpById(String id) {
        if (id == null || POWER_UPS == null) return null;

        for (PowerUp powerUp : POWER_UPS) {
            if (powerUp != null && id.equals(powerUp.id)) {
                return powerUp;
            }
        }
        return null;
    }

    /**
     * Format price for display (e.g., 99 cents -> "$0.99")
     */
    public static String formatPrice(int priceInCents) {
        double dollars = priceInCents / 100.0;
        return String.format("$%.2f", dollars);
    }

    /**
     * Check if power-up is currently implemented in game
     */
    public static boolean isPowerUpImplemented(String id) {
        switch (id) {
            case "auto_solve_pack":
            case "shuffle_pack":
            case "auto_solve_pack":
            case "shuffle_pack":
            case "reveal_preview":
                return true; // ✅ Already working
            default:
                return false; // 🔜 Coming soon
        }
    }
}