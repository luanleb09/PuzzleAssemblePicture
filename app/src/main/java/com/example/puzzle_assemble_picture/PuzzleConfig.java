package com.example.puzzle_assemble_picture;

public class PuzzleConfig {
    public int gridSize;
    public boolean showSample;
    public boolean autoLockCorrectPieces;
    public boolean canSeparateConnectedPieces;
    public boolean autoConnectCorrectPieces;
    public boolean dimLockedPieces;

    public PuzzleConfig() {
        this.gridSize = 3;
        this.showSample = true;
        this.autoLockCorrectPieces = true;
        this.canSeparateConnectedPieces = false;
        this.autoConnectCorrectPieces = true;
        this.dimLockedPieces = true;
    }
}