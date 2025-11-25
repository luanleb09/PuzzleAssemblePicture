package com.example.puzzle_assemble_picture;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {

    private RecyclerView galleryRecyclerView;
    private RecyclerView achievementsRecyclerView;
    private TextView galleryTitle;
    private TextView achievementsTitle;
    private GameProgressManager progressManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        progressManager = new GameProgressManager(this);

        galleryTitle = findViewById(R.id.galleryTitle);
        achievementsTitle = findViewById(R.id.achievementsTitle);
        galleryRecyclerView = findViewById(R.id.galleryRecyclerView);
        achievementsRecyclerView = findViewById(R.id.achievementsRecyclerView);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        galleryRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        List<Integer> galleryPieces = progressManager.getGalleryPieces();
        int totalPieces = GameProgressManager.MAX_LEVEL;

        List<GalleryPieceItem> pieceItems = new ArrayList<>();
        for (int i = 0; i < totalPieces; i++) {
            boolean unlocked = galleryPieces.contains(i);
            pieceItems.add(new GalleryPieceItem(i, unlocked));
        }

        GalleryPieceAdapter pieceAdapter = new GalleryPieceAdapter(pieceItems);
        galleryRecyclerView.setAdapter(pieceAdapter);

        achievementsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        List<Integer> unlockedAchievements = progressManager.getUnlockedAchievements();
        int totalAchievements = progressManager.getTotalAchievements();

        List<AchievementItem> achievementItems = new ArrayList<>();
        for (int i = 0; i < totalAchievements; i++) {
            boolean unlocked = unlockedAchievements.contains(i);
            int startPiece = i * GameProgressManager.PIECES_PER_ACHIEVEMENT;
            int endPiece = startPiece + GameProgressManager.PIECES_PER_ACHIEVEMENT - 1;
            achievementItems.add(new AchievementItem(i, unlocked, startPiece, endPiece));
        }

        AchievementAdapter achievementAdapter = new AchievementAdapter(achievementItems, progressManager);
        achievementsRecyclerView.setAdapter(achievementAdapter);

        galleryTitle.setText(String.format("Gallery Pieces: %d/%d", galleryPieces.size(), totalPieces));
        achievementsTitle.setText(String.format("Achievements: %d/%d", unlockedAchievements.size(), totalAchievements));
    }

    public static class GalleryPieceItem {
        public int pieceId;
        public boolean unlocked;

        public GalleryPieceItem(int pieceId, boolean unlocked) {
            this.pieceId = pieceId;
            this.unlocked = unlocked;
        }
    }

    public static class AchievementItem {
        public int achievementId;
        public boolean unlocked;
        public int startPiece;
        public int endPiece;

        public AchievementItem(int achievementId, boolean unlocked, int startPiece, int endPiece) {
            this.achievementId = achievementId;
            this.unlocked = unlocked;
            this.startPiece = startPiece;
            this.endPiece = endPiece;
        }
    }
}