package com.example.puzzle_assemble_picture;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class GalleryPieceAdapter extends RecyclerView.Adapter<GalleryPieceAdapter.ViewHolder> {

    private final List<GalleryActivity.GalleryPieceItem> items;

    public GalleryPieceAdapter(List<GalleryActivity.GalleryPieceItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery_piece, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GalleryActivity.GalleryPieceItem item = items.get(position);

        if (item.unlocked) {
            holder.card.setCardBackgroundColor(Color.parseColor("#6750A4"));
            holder.card.setAlpha(1.0f);
        } else {
            holder.card.setCardBackgroundColor(Color.parseColor("#E0E0E0"));
            holder.card.setAlpha(0.5f);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;

        ViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.pieceCard);
        }
    }
}