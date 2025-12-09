package com.example.puzzle_assemble_picture;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PowerUpAdapter extends RecyclerView.Adapter<PowerUpAdapter.ViewHolder> {

    private ShopConfig.PowerUp[] powerUps;
    private CoinManager coinManager;
    private OnPurchaseListener listener;

    public interface OnPurchaseListener {
        void onPurchase(ShopConfig.PowerUp powerUp);
    }

    public PowerUpAdapter(ShopConfig.PowerUp[] powerUps, CoinManager coinManager, OnPurchaseListener listener) {
        this.powerUps = powerUps;
        this.coinManager = coinManager;
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

        holder.iconText.setText(powerUp.icon);
        holder.nameText.setText(powerUp.name);
        holder.descriptionText.setText(powerUp.description);
        holder.buyButton.setText("💰 " + powerUp.coinPrice);

        boolean canAfford = coinManager.canAfford(powerUp.coinPrice);
        holder.buyButton.setEnabled(canAfford);
        holder.buyButton.setAlpha(canAfford ? 1.0f : 0.5f);

        holder.buyButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPurchase(powerUp);
            }
        });
    }

    @Override
    public int getItemCount() {
        return powerUps.length;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView iconText, nameText, descriptionText;
        Button buyButton;

        ViewHolder(View itemView) {
            super(itemView);
            iconText = itemView.findViewById(R.id.powerUpIcon);
            nameText = itemView.findViewById(R.id.powerUpName);
            descriptionText = itemView.findViewById(R.id.powerUpDescription);
            buyButton = itemView.findViewById(R.id.btnBuyPowerUp);
        }
    }
}