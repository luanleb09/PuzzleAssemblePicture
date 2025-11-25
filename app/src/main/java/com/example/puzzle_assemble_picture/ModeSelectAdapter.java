package com.example.puzzle_assemble_picture;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ModeSelectAdapter extends RecyclerView.Adapter<ModeSelectAdapter.ModeViewHolder> {

    private final List<GameMode> modes;
    private final OnModeClickListener listener;

    public interface OnModeClickListener {
        void onModeClick(GameMode mode);
    }

    public ModeSelectAdapter(List<GameMode> modes, OnModeClickListener listener) {
        this.modes = modes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ModeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mode, parent, false);
        return new ModeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ModeViewHolder holder, int position) {
        GameMode mode = modes.get(position);
        holder.bind(mode, listener);
    }

    @Override
    public int getItemCount() {
        return modes.size();
    }

    static class ModeViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView modeCard;
        private final ImageView modeImage;
        private final TextView modeTitle;
        private final View lockedOverlay;
        private final ImageView lockIcon;

        public ModeViewHolder(@NonNull View itemView) {
            super(itemView);
            modeCard = itemView.findViewById(R.id.modeCard);
            modeImage = itemView.findViewById(R.id.modeImage);
            modeTitle = itemView.findViewById(R.id.modeTitle);
            lockedOverlay = itemView.findViewById(R.id.lockedOverlay);
            lockIcon = itemView.findViewById(R.id.lockIcon);
        }

        public void bind(GameMode mode, OnModeClickListener listener) {
            modeImage.setImageResource(mode.getIconResource());
            modeTitle.setText(mode.getDisplayName());

            if (mode.isLocked()) {
                lockedOverlay.setVisibility(View.VISIBLE);
                lockIcon.setVisibility(View.VISIBLE);
                modeCard.setAlpha(0.6f);
                modeCard.setClickable(true);
            } else {
                lockedOverlay.setVisibility(View.GONE);
                lockIcon.setVisibility(View.GONE);
                modeCard.setAlpha(1.0f);
                modeCard.setClickable(true);
            }

            modeCard.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onModeClick(mode);
                }
            });

            modeCard.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        modeCard.setCardElevation(16f);
                        break;
                    case android.view.MotionEvent.ACTION_UP:
                    case android.view.MotionEvent.ACTION_CANCEL:
                        modeCard.setCardElevation(8f);
                        break;
                }
                return false;
            });
        }
    }
}