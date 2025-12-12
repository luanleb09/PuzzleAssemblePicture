package com.example.puzzle_assemble_picture;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class GalleryPieceAdapter extends RecyclerView.Adapter<GalleryPieceAdapter.ViewHolder> {

    private final List<GalleryActivity.GalleryItem> items;
    private final Context context;
    private final OnPieceClickListener listener;

    public interface OnPieceClickListener {
        void onPieceClick(GalleryActivity.GalleryItem item);
    }

    public GalleryPieceAdapter(List<GalleryActivity.GalleryItem> items, Context context, OnPieceClickListener listener) {
        this.items = items;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // ✅ USE SIMPLE IMAGE VIEW
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                300 // Fixed height
        ));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setPadding(8, 8, 8, 8);

        return new ViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GalleryActivity.GalleryItem item = items.get(position);

        if (item.isUnlocked) {
            // Show unlocked image
            if (item.imageResId != 0) {
                Glide.with(context)
                        .load(item.imageResId)
                        .centerCrop()
                        .into(holder.imageView);
            } else {
                holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery);
            }

            holder.itemView.setAlpha(1.0f);
        } else {
            // ✅ FIX: Use Android built-in icon
            holder.imageView.setImageResource(android.R.drawable.ic_lock_idle_lock);
            holder.itemView.setAlpha(0.5f);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPieceClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView; // ✅ itemView is ImageView
        }
    }
}