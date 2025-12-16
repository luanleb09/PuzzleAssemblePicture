package com.example.puzzle_assemble_picture;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PowerUpAdapter extends RecyclerView.Adapter<PowerUpAdapter.ViewHolder> {

    private final ShopConfig.PowerUp[] powerUps;
    private final CoinManager coinManager;
    private final PowerUpsManager powerUpsManager;
    private final OnPowerUpClickListener listener;

    public interface OnPowerUpClickListener {
        void onPowerUpClick(ShopConfig.PowerUp powerUp);
    }

    public PowerUpAdapter(ShopConfig.PowerUp[] powerUps,
                          CoinManager coinManager,
                          PowerUpsManager powerUpsManager,
                          OnPowerUpClickListener listener) {
        this.powerUps = powerUps;
        this.coinManager = coinManager;
        this.powerUpsManager = powerUpsManager;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shop_powerup, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShopConfig.PowerUp powerUp = powerUps[position];

        // Set icon
        holder.iconText.setText(powerUp.icon);

        // Set title and description
        holder.titleText.setText(powerUp.name);
        holder.descriptionText.setText(powerUp.description);

        // Set price
        holder.priceText.setText(String.valueOf(powerUp.coinPrice));

        // ✅ Show badge with remaining count
        PowerUpsManager.PowerUpType type = getPowerUpType(powerUp.id);
        if (type != null) {
            int remaining = powerUpsManager.getRemainingUses(type);
            if (remaining > 0) {
                holder.badgeCount.setVisibility(View.VISIBLE);
                holder.badgeCount.setText(String.valueOf(remaining));
            } else {
                holder.badgeCount.setVisibility(View.GONE);
            }
        } else {
            holder.badgeCount.setVisibility(View.GONE);
        }

        // ✅ Enable/disable button based on coin balance
        boolean canAfford = coinManager.canAfford(powerUp.coinPrice);
        holder.buyButton.setEnabled(canAfford);

        if (canAfford) {
            holder.buyButton.setText("Buy");
            holder.buyButton.setAlpha(1.0f);
        } else {
            holder.buyButton.setText("Not Enough");
            holder.buyButton.setAlpha(0.5f);
        }

        // ✅ Click listener
        holder.buyButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPowerUpClick(powerUp);
            }
        });
    }

    @Override
    public int getItemCount() {
        return powerUps != null ? powerUps.length : 0;
    }

    /**
     * ✅ Map shop ID to PowerUpType
     */
    private PowerUpsManager.PowerUpType getPowerUpType(String id) {
        switch (id) {
            case "auto_solve_pack":
                return PowerUpsManager.PowerUpType.AUTO_SOLVE;
            case "shuffle_pack":
                return PowerUpsManager.PowerUpType.SHUFFLE;
            case "solve_corners":
                return PowerUpsManager.PowerUpType.SOLVE_CORNERS;
            case "solve_edges":
                return PowerUpsManager.PowerUpType.SOLVE_EDGES;
            case "reveal_preview":
                return PowerUpsManager.PowerUpType.REVEAL_PREVIEW;
            default:
                return null;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView iconText;
        TextView badgeCount;
        TextView titleText;
        TextView descriptionText;
        TextView priceText;
        Button buyButton;

        ViewHolder(View itemView) {
            super(itemView);
            iconText = itemView.findViewById(R.id.iconText);
            badgeCount = itemView.findViewById(R.id.badgeCount);
            titleText = itemView.findViewById(R.id.titleText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            priceText = itemView.findViewById(R.id.priceText);
            buyButton = itemView.findViewById(R.id.buyButton);
        }
    }
}