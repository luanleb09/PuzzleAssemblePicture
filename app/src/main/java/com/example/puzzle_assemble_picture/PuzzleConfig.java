package com.example.puzzle_assemble_picture;

public class PuzzleConfig {
    public int gridSize;
    public boolean showSample;
    public boolean autoLockCorrectPieces;
    public boolean canSeparateConnectedPieces;
    public boolean autoConnectCorrectPieces;
    public boolean dimLockedPieces;

    // ✅ THÊM: Insane mode features
    public boolean enableRotation = false;
    public boolean enableTimer = false;
    public boolean enableMistakeLimit = false;
    public int timeLimitSeconds = 300; // 5 minutes default
    public int maxMistakes = 10;

    public PuzzleConfig() {
        this.gridSize = 3;
        this.showSample = true;
        this.autoLockCorrectPieces = true;
        this.canSeparateConnectedPieces = false;
        this.autoConnectCorrectPieces = true;
        this.dimLockedPieces = true;
    }
}