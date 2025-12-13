package com.example.puzzle_assemble_picture;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ShopActivity extends AppCompatActivity {

    private static final String TAG = "ShopActivity";

    private CoinManager coinManager;
    private DailyRewardManager dailyRewardManager;
    private PowerUpsManager powerUpsManager; // ✅ FIX: Khai báo variable
    private TextView coinBalanceText;
    private RecyclerView powerUpsRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_shop);

            coinManager = new CoinManager(this);
            dailyRewardManager = new DailyRewardManager(this);
            powerUpsManager = new PowerUpsManager(this); // ✅ FIX: Initialize

            coinBalanceText = findViewById(R.id.coinBalanceText);
            powerUpsRecyclerView = findViewById(R.id.powerUpsRecyclerView);

            if (findViewById(R.id.btnCloseShop) != null) {
                findViewById(R.id.btnCloseShop).setOnClickListener(v -> finish());
            }

            updateCoinBalance();
            setupPowerUps();

            Log.d(TAG, "✅ ShopActivity created successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Error in ShopActivity onCreate", e);
            Toast.makeText(this, "Error loading shop: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateCoinBalance() {
        try {
            if (coinBalanceText != null && coinManager != null) {
                coinBalanceText.setText(String.valueOf(coinManager.getCoins()));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating coin balance", e);
        }
    }

    private void setupPowerUps() {
        try {
            if (powerUpsRecyclerView == null) {
                Log.e(TAG, "powerUpsRecyclerView is null!");
                return;
            }

            powerUpsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

            // ✅ FIX: Array check - use .length thay vì .isEmpty()
            if (ShopConfig.POWER_UPS == null || ShopConfig.POWER_UPS.length == 0) {
                Log.w(TAG, "⚠️ ShopConfig.POWER_UPS is empty");
                Toast.makeText(this, "Shop is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            PowerUpAdapter adapter = new PowerUpAdapter(
                    ShopConfig.POWER_UPS,
                    coinManager,
                    this::handlePowerUpPurchase
            );

            powerUpsRecyclerView.setAdapter(adapter);

            Log.d(TAG, "✅ Power-ups adapter set with " + ShopConfig.POWER_UPS.length + " items");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up power-ups", e);
            Toast.makeText(this, "Error loading items", Toast.LENGTH_SHORT).show();
        }
    }

    private void handlePowerUpPurchase(ShopConfig.PowerUp powerUp) {
        try {
            if (powerUp == null) {
                Toast.makeText(this, "Invalid item", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if power-up is implemented
            if (!ShopConfig.isPowerUpImplemented(powerUp.id)) {
                Toast.makeText(this, "🔜 Coming soon! Stay tuned for updates", Toast.LENGTH_LONG).show();
                return;
            }

            if (!coinManager.canAfford(powerUp.coinPrice)) {
                Toast.makeText(this, "Not enough coins! Need " + powerUp.coinPrice + " 💰", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success = false;

            switch (powerUp.id) {
                case "auto_solve_pack":
                    success = coinManager.spendCoins(powerUp.coinPrice);
                    if (success) {
                        // ✅ FIX: powerUpsManager đã được khai báo và init
                        powerUpsManager.addUses(PowerUpsManager.PowerUpType.AUTO_SOLVE, 3);
                        Toast.makeText(this, "✨ Purchased! +3 Auto-Solves", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case "shuffle_pack":
                    success = coinManager.spendCoins(powerUp.coinPrice);
                    if (success) {
                        powerUpsManager.addUses(PowerUpsManager.PowerUpType.SHUFFLE, 5);
                        Toast.makeText(this, "🔀 Purchased! +5 Shuffles", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case "solve_corners":
                    success = coinManager.spendCoins(powerUp.coinPrice);
                    if (success) {
                        powerUpsManager.addUses(PowerUpsManager.PowerUpType.SOLVE_CORNERS, 3);
                        Toast.makeText(this, "📐 Purchased! +3 Corner Solvers", Toast.LENGTH_SHORT).show();
                    }
                    break;

                case "solve_edges":
                    success = coinManager.spendCoins(powerUp.coinPrice);
                    if (success) {
                        powerUpsManager.addUses(PowerUpsManager.PowerUpType.SOLVE_EDGES, 2);
                        Toast.makeText(this, "🔲 Purchased! +2 Edge Solvers", Toast.LENGTH_SHORT).show();
                    }
                    break;

                // ✅ NEW: Reveal Preview
                case "reveal_preview":
                    success = coinManager.spendCoins(powerUp.coinPrice);
                    if (success) {
                        powerUpsManager.addUses(PowerUpsManager.PowerUpType.REVEAL_PREVIEW, 3);
                        Toast.makeText(this, "👁️ Purchased! +3 Preview Reveals (Insane mode only)", Toast.LENGTH_LONG).show();
                    }
                    break;

                default:
                    Toast.makeText(this, "Unknown item", Toast.LENGTH_SHORT).show();
            }

            if (success) {
                updateCoinBalance();

                // Refresh adapter to update canAfford status
                if (powerUpsRecyclerView != null && powerUpsRecyclerView.getAdapter() != null) {
                    powerUpsRecyclerView.getAdapter().notifyDataSetChanged();
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error handling purchase", e);
            Toast.makeText(this, "Purchase failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCoinBalance();

        // Refresh adapter khi quay lại từ game
        if (powerUpsRecyclerView != null && powerUpsRecyclerView.getAdapter() != null) {
            powerUpsRecyclerView.getAdapter().notifyDataSetChanged();
        }
    }
}