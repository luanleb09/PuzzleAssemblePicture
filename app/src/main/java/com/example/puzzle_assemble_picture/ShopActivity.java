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
    private TextView coinBalanceText;
    private RecyclerView powerUpsRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            // ✅ THAY ĐỔI: Sử dụng activity_shop.xml thay vì dialog_shop.xml
            setContentView(R.layout.activity_shop);

            coinManager = new CoinManager(this);
            dailyRewardManager = new DailyRewardManager(this);

            coinBalanceText = findViewById(R.id.coinBalanceText);
            powerUpsRecyclerView = findViewById(R.id.powerUpsRecyclerView);

            // ✅ THÊM: Null check cho buttons
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

            // ✅ KIỂM TRA: ShopConfig.POWER_UPS có tồn tại không
            if (ShopConfig.POWER_UPS == null || ShopConfig.POWER_UPS.isEmpty()) {
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

            Log.d(TAG, "✅ Power-ups adapter set successfully");

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

            // ✅ CHECK: Is this power-up implemented?
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

                // ✅ FUTURE: Add more cases when implemented
                case "hint":
                case "unlock_corners":
                case "unlock_edges":
                case "reveal_preview":
                case "time_freeze":
                case "double_coins":
                    Toast.makeText(this, "🔜 Coming soon in next update!", Toast.LENGTH_SHORT).show();
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
}