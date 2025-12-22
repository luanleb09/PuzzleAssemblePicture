package com.example.puzzle_assemble_picture;

import android.graphics.Bitmap;
import java.util.ArrayList;
import java.util.List;

public class PuzzlePiece {
    private final Bitmap bitmap;
    private final int correctRow;
    private final int correctCol;
    private final int width;
    private final int height;
    private boolean isLocked;
    private final List<PuzzlePiece> connectedPieces;

    public PuzzlePiece(Bitmap bitmap, int correctRow, int correctCol, int width, int height) {
        this.bitmap = bitmap;
        this.correctRow = correctRow;
        this.correctCol = correctCol;
        this.width = width;
        this.height = height;
        this.isLocked = false;
        this.connectedPieces = new ArrayList<>();
    }

    public void addConnectedPiece(PuzzlePiece piece) {
        if (!connectedPieces.contains(piece)) {
            connectedPieces.add(piece);
        }
    }

    public void removeConnectedPiece(PuzzlePiece piece) {
        connectedPieces.remove(piece);
    }

    public List<PuzzlePiece> getConnectedPieces() {
        return connectedPieces;
    }

    // Getters and setters
    public Bitmap getBitmap() {
        return bitmap;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getCorrectRow() {
        return correctRow;
    }

    public int getCorrectCol() {
        return correctCol;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        this.isLocked = locked;
    }
}