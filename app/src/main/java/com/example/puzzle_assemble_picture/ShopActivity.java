package com.example.puzzle_assemble_picture;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.tabs.TabLayout;

public class ShopActivity extends AppCompatActivity {

    private CoinManager coinManager;
    private DailyRewardManager dailyRewardManager;
    private TextView coinBalanceText;
    private RecyclerView powerUpsRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_shop);

        coinManager = new CoinManager(this);
        dailyRewardManager = new DailyRewardManager(this);

        coinBalanceText = findViewById(R.id.coinBalanceText);
        powerUpsRecyclerView = findViewById(R.id.powerUpsRecyclerView);

        findViewById(R.id.btnCloseShop).setOnClickListener(v -> finish());

        updateCoinBalance();
        setupPowerUps();
    }

    private void updateCoinBalance() {
        coinBalanceText.setText(String.valueOf(coinManager.getCoinBalance()));
    }

    private void setupPowerUps() {
        powerUpsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        PowerUpAdapter adapter = new PowerUpAdapter(
                ShopConfig.POWER_UPS,
                coinManager,
                (powerUp) -> {
                    handlePowerUpPurchase(powerUp);
                }
        );

        powerUpsRecyclerView.setAdapter(adapter);
    }

    private void handlePowerUpPurchase(ShopConfig.PowerUp powerUp) {
        if (!coinManager.canAfford(powerUp.coinPrice)) {
            Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean success = false;

        switch (powerUp.id) {
            case "auto_solve_pack":
                // Add 3 auto-solves (implement in DailyRewardManager)
                success = coinManager.spendCoins(powerUp.coinPrice, powerUp.name);
                if (success) {
                    // TODO: Add method to DailyRewardManager
                    Toast.makeText(this, "Purchased! +3 Auto-Solves", Toast.LENGTH_SHORT).show();
                }
                break;

            case "shuffle_pack":
                success = coinManager.spendCoins(powerUp.coinPrice, powerUp.name);
                if (success) {
                    Toast.makeText(this, "Purchased! +5 Shuffles", Toast.LENGTH_SHORT).show();
                }
                break;

            case "hint":
                success = coinManager.spendCoins(powerUp.coinPrice, powerUp.name);
                if (success) {
                    Toast.makeText(this, "Hint purchased! Use in game", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show();
        }

        if (success) {
            updateCoinBalance();
        }
    }
}