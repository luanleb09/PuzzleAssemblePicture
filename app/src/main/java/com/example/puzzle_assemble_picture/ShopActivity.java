package com.example.puzzle_assemble_picture;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ShopActivity extends AppCompatActivity {

    private static final String TAG = "ShopActivity";

    private CoinManager coinManager;
    private PowerUpsManager powerUpsManager;
    private TextView coinBalanceText;
    private RecyclerView powerUpsRecyclerView;
    private PowerUpAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_shop);

            coinManager = new CoinManager(this);
            powerUpsManager = new PowerUpsManager(this);

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
                coinBalanceText.setText(coinManager.getFormattedCoins());
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

            if (ShopConfig.POWER_UPS == null || ShopConfig.POWER_UPS.length == 0) {
                Log.w(TAG, "⚠️ ShopConfig.POWER_UPS is empty");
                Toast.makeText(this, "Shop is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Pass powerUpsManager to adapter
            adapter = new PowerUpAdapter(
                    ShopConfig.POWER_UPS,
                    coinManager,
                    powerUpsManager,
                    this::showPurchaseConfirmation
            );

            powerUpsRecyclerView.setAdapter(adapter);

            Log.d(TAG, "✅ Power-ups adapter set with " + ShopConfig.POWER_UPS.length + " items");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up power-ups", e);
            Toast.makeText(this, "Error loading items", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * ✅ NEW: Show confirmation dialog before purchase
     */
    private void showPurchaseConfirmation(ShopConfig.PowerUp powerUp) {
        if (powerUp == null) {
            Toast.makeText(this, "Invalid item", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if power-up is implemented
        if (!ShopConfig.isPowerUpImplemented(powerUp.id)) {
            Toast.makeText(this, "🔜 Coming soon! Stay tuned for updates", Toast.LENGTH_LONG).show();
            return;
        }

        // Check if user can afford
        if (!coinManager.canAfford(powerUp.coinPrice)) {
            Toast.makeText(this,
                    "Not enough coins! Need " + powerUp.coinPrice + " 💰",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Show confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Purchase " + powerUp.name + "?");

        String message = powerUp.description + "\n\n" +
                "Cost: 💰 " + powerUp.coinPrice + " coins\n" +
                "Your balance: 💰 " + coinManager.getCoins() + " coins\n\n" +
                "Confirm purchase?";

        builder.setMessage(message);

        builder.setPositiveButton("✅ Buy", (dialog, which) -> {
            handlePowerUpPurchase(powerUp);
        });

        builder.setNegativeButton("❌ Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.show();
    }

    /**
     * ✅ Handle actual purchase after confirmation
     */
    private void handlePowerUpPurchase(ShopConfig.PowerUp powerUp) {
        try {
            boolean success = false;
            String successMessage = "";

            switch (powerUp.id) {
                case "auto_solve_pack":
                    success = coinManager.spendCoins(powerUp.coinPrice);
                    if (success) {
                        powerUpsManager.addUses(PowerUpsManager.PowerUpType.AUTO_SOLVE, 3);
                        successMessage = "✨ Purchased! +3 Auto-Solves";
                    }
                    break;

                case "shuffle_pack":
                    success = coinManager.spendCoins(powerUp.coinPrice);
                    if (success) {
                        powerUpsManager.addUses(PowerUpsManager.PowerUpType.SHUFFLE, 5);
                        successMessage = "🔀 Purchased! +5 Shuffles";
                    }
                    break;

                case "solve_corners":
                    success = coinManager.spendCoins(powerUp.coinPrice);
                    if (success) {
                        powerUpsManager.addUses(PowerUpsManager.PowerUpType.SOLVE_CORNERS, 3);
                        successMessage = "📐 Purchased! +3 Corner Solvers";
                    }
                    break;

                case "solve_edges":
                    success = coinManager.spendCoins(powerUp.coinPrice);
                    if (success) {
                        powerUpsManager.addUses(PowerUpsManager.PowerUpType.SOLVE_EDGES, 2);
                        successMessage = "🔲 Purchased! +2 Edge Solvers";
                    }
                    break;

                case "reveal_preview":
                    success = coinManager.spendCoins(powerUp.coinPrice);
                    if (success) {
                        powerUpsManager.addUses(PowerUpsManager.PowerUpType.REVEAL_PREVIEW, 3);
                        successMessage = "👁️ Purchased! +3 Preview Reveals (Insane mode only)";
                    }
                    break;

                default:
                    Toast.makeText(this, "Unknown item", Toast.LENGTH_SHORT).show();
                    return;
            }

            if (success) {
                Toast.makeText(this, successMessage, Toast.LENGTH_LONG).show();
                updateCoinBalance();

                // ✅ Refresh adapter to update badge counts and button states
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            } else {
                Toast.makeText(this, "Purchase failed!", Toast.LENGTH_SHORT).show();
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

        // ✅ Refresh adapter when returning from game
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}